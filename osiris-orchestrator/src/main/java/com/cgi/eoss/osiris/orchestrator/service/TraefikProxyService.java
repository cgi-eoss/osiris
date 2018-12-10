package com.cgi.eoss.osiris.orchestrator.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.Acl;
import org.springframework.stereotype.Service;

import com.cgi.eoss.osiris.model.Group;
import com.cgi.eoss.osiris.model.Job;
import com.cgi.eoss.osiris.model.Job.Status;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.persistence.service.GroupDataService;
import com.cgi.eoss.osiris.persistence.service.JobDataService;
import com.cgi.eoss.osiris.security.OsirisPermission;
import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
@ConditionalOnProperty(value = "osiris.orchestrator.proxy.traefik.enabled", havingValue = "true", matchIfMissing = false)
@Log4j2
public class TraefikProxyService implements DynamicProxyService{
	
	private static final Collector<AccessControlEntry, ?, OsirisPermission> SPRING_OSIRIS_ACL_SET_COLLECTOR =
            Collectors.collectingAndThen(Collectors.mapping(AccessControlEntry::getPermission, Collectors.toSet()), OsirisPermission::getOsirisPermission);

	private static final long PROXY_UPDATE_PERIOD_MS = 10* 60 * 1000L;
	
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	
	private final JobDataService jobDataService;
	
	private final OsirisSecurityService securityService;
	
	private final GroupDataService groupDataService;
	
	private HttpUrl traefikUrl;
	
	private String traefikUser;
	
	private String traefikPassword;

	private OkHttpClient httpClient;

	private String baseUrl;
	
	private String guiUrlPrefix;

	private boolean enableSSOHeaders;
	
	private String usernameSSOHeader;

	@Autowired
	public TraefikProxyService(@Value("${osiris.orchestrator.traefik.url:}") String traefikUrlString, 
			@Value("${osiris.orchestrator.traefik.user:}") String traefikUser, 
			@Value("${osiris.orchestrator.traefik.password:}") String traefikPassword, 
			@Value("${osiris.orchestrator.gui.baseUrl:}") String baseUrl,
			@Value("${osiris.orchestrator.gui.urlPrefix:/gui/}") String guiUrlPrefix,
			@Value("${osiris.orchestrator.traefik.enableSSOHeaders:true}") boolean enableSSOHeaders,
			@Value("${osiris.api.security.username-request-header:REMOTE_USER}") String usernameSSOHeader,
			JobDataService jobDataService,
			GroupDataService groupDataService,
			OsirisSecurityService securityService) {
		this.jobDataService = jobDataService;
		this.groupDataService = groupDataService;
		this.securityService = securityService;
		this.httpClient = new OkHttpClient.Builder().build();
		traefikUrl = HttpUrl.parse(traefikUrlString);
		this.traefikUser = traefikUser;
		this.traefikPassword = traefikPassword;
		this.baseUrl = baseUrl;
		this.guiUrlPrefix = guiUrlPrefix;
		this.enableSSOHeaders = enableSSOHeaders;
		this.usernameSSOHeader = usernameSSOHeader;
	}
	
	public boolean supportsProxyRoute() {
		 return true;
	 }
	 
	 public String getProxyRoute(com.cgi.eoss.osiris.rpc.Job job) {
		 return guiUrlPrefix + job.getId();
	 }
	
	@Override
	public ReverseProxyEntry getProxyEntry(com.cgi.eoss.osiris.rpc.Job job, String host, int port) {
		return new ReverseProxyEntry(baseUrl + guiUrlPrefix + job.getId() + "/", "http://" + host + ":" + port);
	}
	

	@Override
	public void update() {
		try {
			updateTraefik();
		}
		catch (Exception e) {
			LOG.error("Error updating traefik", e);
		}
	}

	private void updateTraefik() {
		List<Job> proxiedJobs = jobDataService.findByStatusAndGuiUrlNotNull(Status.RUNNING);
		TraefikProxyConfig pc = new TraefikProxyConfig();
		for (Job proxiedJob: proxiedJobs) {
			HttpUrl httpUrl = HttpUrl.parse(proxiedJob.getGuiUrl());
			String path = httpUrl.encodedPath();
			String httpEndpoint = proxiedJob.getGuiEndpoint();
			Map<String, String> route = new HashMap<>();
			if (proxiedJob.getConfig().getService().isStripProxyPath()) {
				route.put("rule", "PathPrefixStrip:" + path);
			}
			else {
				route.put("rule", "PathPrefix:" + path);
			}
			Map<String, Object> routes = new HashMap<>();
			routes.put("route-" + proxiedJob.getExtId(), route);
			if (enableSSOHeaders) {
				Map<String, String> routeAuth = new HashMap<>();
				Acl acl = securityService.getAcl(new ObjectIdentityImpl(Job.class, proxiedJob.getId()));
				Set<User> allowedUsers = new HashSet<>();
	            allowedUsers.add(proxiedJob.getOwner());
				Set<Entry<Group, OsirisPermission>> groupPermissions = acl.getEntries().stream()
	                    .filter(ace -> ace.getSid() instanceof GrantedAuthoritySid && ((GrantedAuthoritySid) ace.getSid()).getGrantedAuthority().startsWith("GROUP_"))
	                    .collect(Collectors.groupingBy(this::getGroup, SPRING_OSIRIS_ACL_SET_COLLECTOR))
	                    .entrySet();
	            for (Entry<Group, OsirisPermission> groupPermission: groupPermissions) {
	            	if (groupPermission.getValue().equals(OsirisPermission.ADMIN)) {
	            		allowedUsers.addAll(groupPermission.getKey().getMembers());
	            	}
	            }        
	            String allowedUsersString = allowedUsers.stream().map(u -> u.getName()).collect(Collectors.joining("|"));
				routeAuth.put("rule", "HeadersRegexp: " + usernameSSOHeader + "," + allowedUsersString);
				routes.put("route-" + proxiedJob.getExtId() + "-auth", routeAuth);
				
			}
			
			Map<String, Object> frontendDef = new HashMap<>();
			frontendDef.put("routes", routes);
			frontendDef.put("backend", "backend-" + proxiedJob.getExtId());
			frontendDef.put("passHostHeader", true);
			pc.getFrontends().put("frontend-" + proxiedJob.getExtId(), frontendDef);
			Map<String, String> server = new HashMap<>();
			server.put("url", httpEndpoint.toString());
			Map<String, Object> servers = new HashMap<>();
			servers.put("server-" + proxiedJob.getExtId(), server);
			Map<String, Object> backendDef = new HashMap<>();
			backendDef.put("servers", servers);
			pc.getBackends().put("backend-" + proxiedJob.getExtId(), backendDef);
			
		}
		
		String plainCreds = traefikUser + ":" + traefikPassword;
		byte[] plainCredsBytes = plainCreds.getBytes();
		byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
		String base64Creds = new String(base64CredsBytes);
		try {
		    RequestBody body = RequestBody.create(JSON, OBJECT_MAPPER.writeValueAsString(pc));
		    Request request = new Request.Builder()
		                .url(traefikUrl)
		                .put(body)
		                .addHeader("Authorization", "Basic " + base64Creds)
		                .build();
			Response response = httpClient.newCall(request).execute();
			if (response.isSuccessful() == false) {
				throw new RuntimeException("Unsuccessful response: " + response.message());
			}
			response.close();
		}
		catch (Exception e) {
			LOG.error("Error updating proxy: "  + e);
		}
	}
	
	private Group getGroup(AccessControlEntry ace) {
        return groupDataService.getById(Long.parseLong(((GrantedAuthoritySid) ace.getSid()).getGrantedAuthority().replaceFirst("^GROUP_", "")));
    }

	
	@Scheduled(fixedRate = PROXY_UPDATE_PERIOD_MS, initialDelay = 10000L)
	private void scheduledUpdate() {
		this.update();
		
		
	}
	
}
