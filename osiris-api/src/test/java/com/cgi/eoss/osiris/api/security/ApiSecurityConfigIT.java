package com.cgi.eoss.osiris.api.security;

import com.cgi.eoss.osiris.api.ApiConfig;
import com.cgi.eoss.osiris.api.ApiTestConfig;
import com.cgi.eoss.osiris.api.controllers.AclsApi;
import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.Group;
import com.cgi.eoss.osiris.model.Role;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.persistence.service.GroupDataService;
import com.cgi.eoss.osiris.persistence.service.ServiceDataService;
import com.cgi.eoss.osiris.persistence.service.UserDataService;
import com.cgi.eoss.osiris.security.OsirisPermission;
import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.google.common.collect.ImmutableSet;
import com.jayway.jsonpath.JsonPath;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class ApiSecurityConfigIT {

    @Autowired
    private ServiceDataService serviceDataService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private GroupDataService groupDataService;

    @Autowired
    private MutableAclService aclService;

    @Autowired
    private AclsApi aclsApi;

    @Autowired
    private MockMvc mockMvc;

    private User alice;
    private User bob;
    private User chuck;
    private User osirisAdmin;

    private Group alpha;
    private Group beta;

    private OsirisService service1;
    private OsirisService service2;
    private OsirisService service3;

    @Before
    public void setUp() {
        alice = new User("alice");
        alice.setRole(Role.USER);
        bob = new User("bob");
        bob.setRole(Role.USER);
        chuck = new User("chuck");
        chuck.setRole(Role.GUEST);
        osirisAdmin = new User("osiris-admin");
        osirisAdmin.setRole(Role.ADMIN);

        userDataService.save(ImmutableSet.of(alice, bob, chuck, osirisAdmin));

        alpha = new Group("alpha", osirisAdmin);
        alpha.setMembers(ImmutableSet.of(alice, bob));
        beta = new Group("beta", osirisAdmin);
        beta.setMembers(ImmutableSet.of(alice));

        groupDataService.save(ImmutableSet.of(alpha, beta));

        service1 = new OsirisService("service-1", osirisAdmin, "dockerTag");
        service1.setStatus(OsirisService.Status.AVAILABLE);
        service2 = new OsirisService("service-2", osirisAdmin, "dockerTag");
        service2.setStatus(OsirisService.Status.IN_DEVELOPMENT);
        service3 = new OsirisService("service-3", osirisAdmin, "dockerTag");
        service3.setStatus(OsirisService.Status.IN_DEVELOPMENT);
        serviceDataService.save(ImmutableSet.of(service1, service2, service3));
    }

    @After
    public void tearDown() {
        serviceDataService.getAll().stream()
                .map(OsirisService::getId)
                .map(id -> new ObjectIdentityImpl(OsirisService.class, id))
                .forEach(oid -> aclService.deleteAcl(oid, true));
        serviceDataService.deleteAll();
    }

    @Test
    public void testUserDetails() throws Exception {
        mockMvc.perform(get("/api/currentUser").header("REMOTE_USER", alice.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(alice.getId()))
                .andExpect(jsonPath("$.name").value(alice.getName()))
                .andExpect(jsonPath("$.email").doesNotExist());

        // Make sure the SSO header updates the email attribute
        assertThat(alice.getEmail(), is(nullValue()));
        String aliceEmail = "alice@example.com";
        mockMvc.perform(get("/api/currentUser").header("REMOTE_USER", alice.getName()).header("REMOTE_EMAIL", aliceEmail))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(alice.getId()))
                .andExpect(jsonPath("$.name").value(alice.getName()))
                .andExpect(jsonPath("$.email").value(aliceEmail));
        assertThat(userDataService.getByName(alice.getName()).getEmail(), is(aliceEmail));
    }

    @Test
    public void testRoleAccess() throws Exception {
        getServiceFromApi(service1.getId(), alice).andExpect(status().isOk());
        getServiceFromApi(service1.getId(), bob).andExpect(status().isOk());
        getServiceFromApi(service1.getId(), chuck).andExpect(status().isOk());
        getServiceFromApi(service1.getId(), osirisAdmin).andExpect(status().isOk());

        getServiceFromApi(service2.getId(), alice).andExpect(status().isForbidden());
        getServiceFromApi(service2.getId(), bob).andExpect(status().isForbidden());
        getServiceFromApi(service2.getId(), chuck).andExpect(status().isForbidden());
        getServiceFromApi(service2.getId(), osirisAdmin).andExpect(status().isOk());

        getServiceFromApi(service3.getId(), alice).andExpect(status().isForbidden());
        getServiceFromApi(service3.getId(), bob).andExpect(status().isForbidden());
        getServiceFromApi(service3.getId(), chuck).andExpect(status().isForbidden());
        getServiceFromApi(service3.getId(), osirisAdmin).andExpect(status().isOk());
    }

    @Test
    public void testUserAclAccess() throws Exception {
        // Grant read access for service-1 to alice - but unused as it is already AVAILABLE
        createReadAce(service1.getId(), alice.getName());
        // Grant read access for service-2 to bob
        createReadAce(service2.getId(), bob.getName());
        // Grant read access for service-3 to alice and bob
        createReadAce(service3.getId(), alice.getName());
        createReadAce(service3.getId(), bob.getName());

        getServiceFromApi(service1.getId(), alice).andExpect(status().isOk());
        getServiceFromApi(service1.getId(), bob).andExpect(status().isOk());
        getServiceFromApi(service1.getId(), chuck).andExpect(status().isOk());
        getServiceFromApi(service1.getId(), osirisAdmin).andExpect(status().isOk());

        getServiceFromApi(service2.getId(), alice).andExpect(status().isForbidden());
        getServiceFromApi(service2.getId(), bob).andExpect(status().isOk());
        getServiceFromApi(service2.getId(), chuck).andExpect(status().isForbidden());
        getServiceFromApi(service2.getId(), osirisAdmin).andExpect(status().isOk());

        getServiceFromApi(service3.getId(), alice).andExpect(status().isOk());
        getServiceFromApi(service3.getId(), bob).andExpect(status().isOk());
        getServiceFromApi(service3.getId(), chuck).andExpect(status().isForbidden());
        getServiceFromApi(service3.getId(), osirisAdmin).andExpect(status().isOk());
    }

    @Test
    public void testGroupAclAccess() throws Exception {
        // Verify groups can read themselves
        getGroupFromApi(alpha.getId(), alice).andExpect(status().isOk());
        getGroupFromApi(alpha.getId(), bob).andExpect(status().isOk());
        getGroupFromApi(beta.getId(), alice).andExpect(status().isOk());
        getGroupFromApi(beta.getId(), bob).andExpect(status().isForbidden());

        // Grant read access for service-2 to alpha (alice & bob)
        createGroupReadAce(service2.getId(), alpha);
        // Grant read access for service-3 to beta (alice)
        createGroupReadAce(service3.getId(), beta);

        getServiceFromApi(service1.getId(), alice).andExpect(status().isOk());
        getServiceFromApi(service1.getId(), bob).andExpect(status().isOk());
        getServiceFromApi(service1.getId(), chuck).andExpect(status().isOk());
        getServiceFromApi(service1.getId(), osirisAdmin).andExpect(status().isOk());

        getServiceFromApi(service2.getId(), alice).andExpect(status().isOk());
        getServiceFromApi(service2.getId(), bob).andExpect(status().isOk());
        getServiceFromApi(service2.getId(), chuck).andExpect(status().isForbidden());
        getServiceFromApi(service2.getId(), osirisAdmin).andExpect(status().isOk());

        getServiceFromApi(service3.getId(), alice).andExpect(status().isOk());
        getServiceFromApi(service3.getId(), bob).andExpect(status().isForbidden());
        getServiceFromApi(service3.getId(), chuck).andExpect(status().isForbidden());
        getServiceFromApi(service3.getId(), osirisAdmin).andExpect(status().isOk());
    }

    @Test
    public void testUserGrantedAuthorities() throws Exception {
        mockMvc.perform(get("/api/currentUser/grantedAuthorities").header("REMOTE_USER", alice.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(containsInAnyOrder(authoritiesToStrings(alpha, beta, OsirisPermission.PUBLIC, Role.USER))));
        mockMvc.perform(get("/api/currentUser/grantedAuthorities").header("REMOTE_USER", bob.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(containsInAnyOrder(authoritiesToStrings(alpha, OsirisPermission.PUBLIC, Role.USER))));
        MockHttpSession chuckSession = (MockHttpSession) mockMvc.perform(get("/api/currentUser/grantedAuthorities").header("REMOTE_USER", chuck.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(containsInAnyOrder(authoritiesToStrings(OsirisPermission.PUBLIC, Role.GUEST))))
                .andReturn().getRequest().getSession();

        mockMvc.perform(patch("/api/groups/"+beta.getId()).content("{\"members\":[\"" +
                getUserUrl(alice) + getUserUrl(chuck) + "\"]}").header("REMOTE_USER", osirisAdmin.getName()))
                .andExpect(status().isNoContent());

        // Prove the existing session is updated
        mockMvc.perform(get("/api/currentUser/grantedAuthorities").header("REMOTE_USER", chuck.getName()).session(chuckSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(containsInAnyOrder(authoritiesToStrings(beta, OsirisPermission.PUBLIC, Role.GUEST))));
    }

    private String getUserUrl(User user) throws Exception {
        String jsonResult = mockMvc.perform(
                get("/api/users/" + user.getId()).header("REMOTE_USER", osirisAdmin.getName()))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.compile("$._links.self.href").read(jsonResult);
    }

    private String[] authoritiesToStrings(GrantedAuthority... authorities) {
        return Arrays.stream(authorities)
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()).toArray(new String[]{});
    }

    private ResultActions getServiceFromApi(Long serviceId, User user) throws Exception {
        return mockMvc.perform(get("/api/services/" + serviceId).header("REMOTE_USER", user.getName()));
    }

    private ResultActions getGroupFromApi(Long groupId, User user) throws Exception {
        return mockMvc.perform(get("/api/groups/" + groupId).header("REMOTE_USER", user.getName()));
    }

    private void createReadAce(Long id, String principal) {
        ObjectIdentity oi = new ObjectIdentityImpl(OsirisService.class, id);
        createAce(oi, new PrincipalSid(principal), BasePermission.READ);
    }

    private void createGroupReadAce(Long id, GrantedAuthority group) {
        ObjectIdentity oi = new ObjectIdentityImpl(OsirisService.class, id);
        createAce(oi, new GrantedAuthoritySid(group), BasePermission.READ);
    }

    private void createAce(ObjectIdentity oi, Sid sid, Permission p) {
        SecurityContextHolder.getContext().setAuthentication(OsirisSecurityService.PUBLIC_AUTHENTICATION);

        MutableAcl acl;
        try {
            acl = (MutableAcl) aclService.readAclById(oi);
        } catch (NotFoundException nfe) {
            acl = aclService.createAcl(oi);
        }

        acl.insertAce(acl.getEntries().size(), p, sid, true);
        aclService.updateAcl(acl);
    }

}