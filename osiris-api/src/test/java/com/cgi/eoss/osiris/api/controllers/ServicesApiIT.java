package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.api.ApiConfig;
import com.cgi.eoss.osiris.api.ApiTestConfig;
import com.cgi.eoss.osiris.security.OsirisPermission;
import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.Role;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.persistence.service.ServiceDataService;
import com.cgi.eoss.osiris.persistence.service.UserDataService;
import com.google.common.collect.ImmutableSet;
import com.jayway.jsonpath.JsonPath;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class ServicesApiIT {

    private static final JsonPath USER_HREF_JSONPATH = JsonPath.compile("$._links.self.href");
    private static final JsonPath OBJ_ID_JSONPATH = JsonPath.compile("$.id");

    @Autowired
    private ServiceDataService dataService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private MutableAclService aclService;

    @Autowired
    private OsirisSecurityService securityService;

    @Autowired
    private MockMvc mockMvc;

    private User osirisGuest;
    private User osirisUser;
    private User osirisExpertUser;
    private User osirisContentAuthority;
    private User osirisAdmin;

    @Before
    public void setUp() {
        osirisGuest = new User("osiris-guest");
        osirisGuest.setRole(Role.GUEST);
        osirisUser = new User("osiris-user");
        osirisUser.setRole(Role.USER);
        osirisExpertUser = new User("osiris-expert-user");
        osirisExpertUser.setRole(Role.EXPERT_USER);
        osirisContentAuthority = new User("osiris-content-authority");
        osirisContentAuthority.setRole(Role.CONTENT_AUTHORITY);
        osirisAdmin = new User("osiris-admin");
        osirisAdmin.setRole(Role.ADMIN);

        userDataService.save(ImmutableSet.of(osirisGuest, osirisUser, osirisExpertUser, osirisContentAuthority, osirisAdmin));
    }

    @After
    public void tearDown() {
        dataService.deleteAll();
    }

    @Test
    public void testGetIndex() throws Exception {
        mockMvc.perform(get("/api/").header("REMOTE_USER", osirisUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.services").exists());
    }

    @Test
    public void testGet() throws Exception {
        OsirisService service = new OsirisService("service-1", osirisUser, "dockerTag");
        service.setStatus(OsirisService.Status.AVAILABLE);
        dataService.save(service);

        mockMvc.perform(get("/api/services").header("REMOTE_USER", osirisUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.services").isArray())
                .andExpect(jsonPath("$._embedded.services[0].id").value(service.getId()))
                .andExpect(jsonPath("$._embedded.services[0].name").value("service-1"))
                .andExpect(jsonPath("$._embedded.services[0].dockerTag").value("dockerTag"))
                .andExpect(jsonPath("$._embedded.services[0].owner.id").value(osirisUser.getId()))
                .andExpect(jsonPath("$._embedded.services[0].access.published").value(false))
                .andExpect(jsonPath("$._embedded.services[0].access.publishRequested").value(false))
                .andExpect(jsonPath("$._embedded.services[0].access.currentLevel").value("ADMIN"))
                .andExpect(jsonPath("$._embedded.services[0]._links.self.href").value(endsWith("/services/" + service.getId())))
                .andExpect(jsonPath("$._embedded.services[0]._links.owner.href").value(endsWith("/services/" + service.getId() + "/owner")));
    }

    @Test
    public void testGetFilter() throws Exception {
        OsirisService service = new OsirisService("service-1", osirisAdmin, "dockerTag");
        service.setStatus(OsirisService.Status.AVAILABLE);
        OsirisService service2 = new OsirisService("service-2", osirisAdmin, "dockerTag");
        service2.setStatus(OsirisService.Status.IN_DEVELOPMENT);
        OsirisService service3 = new OsirisService("service-3", osirisAdmin, "dockerTag");
        service3.setStatus(OsirisService.Status.IN_DEVELOPMENT);
        dataService.save(ImmutableSet.of(service, service2, service3));

        createAce(new ObjectIdentityImpl(OsirisService.class, service.getId()), new GrantedAuthoritySid(OsirisPermission.PUBLIC), BasePermission.READ);
        createReadAce(new ObjectIdentityImpl(OsirisService.class, service3.getId()), osirisExpertUser.getName());

        // service1 is returned as it is AVAILABLE
        // service2 is not returned as it is IN_DEVELOPMENT and not readable by the user
        // service3 is returned as the user has been granted read permission

        mockMvc.perform(get("/api/services").header("REMOTE_USER", osirisExpertUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.services").isArray())
                .andExpect(jsonPath("$._embedded.services.length()").value(2))
                .andExpect(jsonPath("$._embedded.services[0].id").value(service.getId()))
                .andExpect(jsonPath("$._embedded.services[0].access.published").value(true))
                .andExpect(jsonPath("$._embedded.services[0].access.currentLevel").value("READ"))
                .andExpect(jsonPath("$._embedded.services[1].id").value(service3.getId()))
                .andExpect(jsonPath("$._embedded.services[1].access.published").value(false))
                .andExpect(jsonPath("$._embedded.services[1].access.currentLevel").value("READ"));
    }

    @Test
    public void testCreateWithValidRole() throws Exception {
        mockMvc.perform(post("/api/services").header("REMOTE_USER", osirisAdmin.getName()).content("{\"name\": \"service-1\", \"dockerTag\": \"dockerTag\", \"owner\":\"" + userUri(osirisAdmin) + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/services/\\d+$")));
        mockMvc.perform(post("/api/services").header("REMOTE_USER", osirisContentAuthority.getName()).content("{\"name\": \"service-2\", \"dockerTag\": \"dockerTag\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/services/\\d+$")));

        assertThat(dataService.getByName("service-1"), is(notNullValue()));
        assertThat(dataService.getByName("service-1").getOwner(), is(osirisAdmin));
        assertThat(dataService.getByName("service-2"), is(notNullValue()));
        assertThat(dataService.getByName("service-2").getOwner(), is(osirisContentAuthority));
    }

    @Test
    public void testCreateWithInvalidRole() throws Exception {
        mockMvc.perform(post("/api/services").header("REMOTE_USER", osirisUser.getName()).content("{\"name\": \"service-1\", \"dockerTag\": \"dockerTag\", \"owner\":\"" + userUri(osirisUser) + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testWriteAccessControl() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/services").header("REMOTE_USER", osirisAdmin.getName()).content("{\"name\": \"service-1\", \"dockerTag\": \"dockerTag\", \"owner\":\"" + userUri(osirisAdmin) + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/services/\\d+$")))
                .andReturn();

        String serviceLocation = result.getResponse().getHeader("Location");

        // WARNING: The underlying object *is* modified by these calls, due to ORM state held in the mockMvc layer
        // This should not happen in production and must be verified in the full test harness

        mockMvc.perform(patch(serviceLocation).header("REMOTE_USER", osirisUser.getName()).content("{\"name\": \"service-1-user-updated\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch(serviceLocation).header("REMOTE_USER", osirisGuest.getName()).content("{\"name\": \"service-1-guest-updated\"}"))
                .andExpect(status().isForbidden());

        // Allow the user to write to the object
        createWriteAce(new ObjectIdentityImpl(OsirisService.class, getJsonObjectId(serviceLocation)), osirisUser.getName());

        mockMvc.perform(patch(serviceLocation).header("REMOTE_USER", osirisUser.getName()).content("{\"name\": \"service-1-user-updated\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch(serviceLocation).header("REMOTE_USER", osirisGuest.getName()).content("{\"name\": \"service-1-guest-updated\"}"))
                .andExpect(status().isForbidden());
    }

    private String userUri(User user) throws Exception {
        String jsonResult = mockMvc.perform(
                get("/api/users/" + user.getId()).header("REMOTE_USER", osirisAdmin.getName()))
                .andReturn().getResponse().getContentAsString();
        return USER_HREF_JSONPATH.read(jsonResult);
    }

    private Long getJsonObjectId(String location) throws Exception {
        String jsonResult = mockMvc.perform(get(location).header("REMOTE_USER", osirisAdmin.getName()))
                .andReturn().getResponse().getContentAsString();
        return ((Number) OBJ_ID_JSONPATH.read(jsonResult)).longValue();
    }

    private void createWriteAce(ObjectIdentity oi, String principal) {
        createReadAce(oi, principal);
        createAce(oi, new PrincipalSid(principal), BasePermission.WRITE);
    }

    private void createReadAce(ObjectIdentity oi, String principal) {
        createAce(oi, new PrincipalSid(principal), BasePermission.READ);
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