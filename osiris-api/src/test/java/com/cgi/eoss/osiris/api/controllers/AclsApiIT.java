package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.api.ApiConfig;
import com.cgi.eoss.osiris.api.ApiTestConfig;
import com.cgi.eoss.osiris.security.OsirisPermission;
import com.cgi.eoss.osiris.security.OsirisSecurityService;
import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.Group;
import com.cgi.eoss.osiris.model.Role;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.persistence.service.GroupDataService;
import com.cgi.eoss.osiris.persistence.service.ServiceDataService;
import com.cgi.eoss.osiris.persistence.service.UserDataService;
import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class AclsApiIT {

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private GroupDataService groupDataService;

    @Autowired
    private ServiceDataService serviceDataService;

    @Autowired
    private MutableAclService aclService;

    @Autowired
    private MockMvc mockMvc;

    private User osirisUser;
    private User osirisAdmin;
    private Group defaultGroup;
    private OsirisService service1;

    @Before
    public void setUp() {
        osirisUser = new User("osiris-user");
        osirisUser.setRole(Role.USER);
        osirisAdmin = new User("osiris-admin");
        osirisAdmin.setRole(Role.ADMIN);
        userDataService.save(ImmutableSet.of(osirisUser, osirisAdmin));

        defaultGroup = new Group("defaultGroup", osirisAdmin);
        defaultGroup.setMembers(ImmutableSet.of(osirisUser));
        groupDataService.save(defaultGroup);

        service1 = new OsirisService("service-1", osirisAdmin, "dockerTag");
        service1.setStatus(OsirisService.Status.AVAILABLE);
        serviceDataService.save(service1);
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
    public void testGetServiceAcl() throws Exception {
        mockMvc.perform(get("/api/acls/service/" + service1.getId()).header("REMOTE_USER", osirisAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entityId").value(service1.getId()))
                .andExpect(jsonPath("$.permissions").isEmpty());
        mockMvc.perform(get("/api/acls/service/" + service1.getId()).header("REMOTE_USER", osirisUser.getName()))
                .andExpect(status().isForbidden());

        // Grant ADMIN to osirisUser
        MutableAcl acl = getAcl(new ObjectIdentityImpl(OsirisService.class, service1.getId()));
        OsirisPermission.ADMIN.getAclPermissions()
                .forEach(p -> acl.insertAce(acl.getEntries().size(), p, new GrantedAuthoritySid(defaultGroup), true));
        aclService.updateAcl(acl);

        mockMvc.perform(get("/api/acls/service/" + service1.getId()).header("REMOTE_USER", osirisAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entityId").value(service1.getId()))
                .andExpect(jsonPath("$.permissions[0].group.name").value(defaultGroup.getName()))
                .andExpect(jsonPath("$.permissions[0].permission").value("ADMIN"));
        // And now the user can read the ACL
        mockMvc.perform(get("/api/acls/service/" + service1.getId()).header("REMOTE_USER", osirisUser.getName()))
                .andExpect(status().isOk());
    }

    @Test
    public void testSetServiceAcl() throws Exception {
        mockMvc.perform(get("/api/acls/service/" + service1.getId()).header("REMOTE_USER", osirisAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entityId").value(service1.getId()))
                .andExpect(jsonPath("$.permissions").isEmpty());

        // Grant ADMIN to osirisUser by HTTP POST
        mockMvc.perform(post("/api/acls/service/" + service1.getId()).header("REMOTE_USER", osirisAdmin.getName()).contentType(MediaType.APPLICATION_JSON_UTF8)
                .content("{\"entityId\":" + service1.getId() + ", \"permissions\":[{\"group\":{\"id\":" + defaultGroup.getId() + ",\"name\":\"" + defaultGroup.getName() + "\"},\"permission\":\"ADMIN\"}]}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/acls/service/" + service1.getId()).header("REMOTE_USER", osirisAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entityId").value(service1.getId()))
                .andExpect(jsonPath("$.permissions[0].group.name").value(defaultGroup.getName()))
                .andExpect(jsonPath("$.permissions[0].permission").value("ADMIN"));
        // And now the user can read the ACL
        mockMvc.perform(get("/api/acls/service/" + service1.getId()).header("REMOTE_USER", osirisUser.getName()))
                .andExpect(status().isOk());
    }

    private MutableAcl getAcl(ObjectIdentity objectIdentity) {
        SecurityContextHolder.getContext().setAuthentication(OsirisSecurityService.PUBLIC_AUTHENTICATION);
        try {
            return (MutableAcl) aclService.readAclById(objectIdentity);
        } catch (NotFoundException nfe) {
            return aclService.createAcl(objectIdentity);
        }
    }

}