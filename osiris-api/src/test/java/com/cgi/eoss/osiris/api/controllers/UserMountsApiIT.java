package com.cgi.eoss.osiris.api.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.cgi.eoss.osiris.api.ApiConfig;
import com.cgi.eoss.osiris.api.ApiTestConfig;
import com.cgi.eoss.osiris.model.Role;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.model.UserMount;
import com.cgi.eoss.osiris.model.UserMount.MountType;
import com.cgi.eoss.osiris.persistence.service.UserDataService;
import com.cgi.eoss.osiris.persistence.service.UserMountDataService;
import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class UserMountsApiIT {

    @Autowired
    private UserMountDataService dataService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private MockMvc mockMvc;

    private User osirisUser;
    private User osirisAdmin;

    private UserMount userMount1;
    private UserMount userMount2;
    private UserMount userMount3;

    @Before
    public void setUp() {
        osirisUser = new User("osiris-user");
        osirisUser.setRole(Role.USER);
        osirisAdmin = new User("osiris-admin");
        osirisAdmin.setRole(Role.ADMIN);

        userDataService.save(ImmutableSet.of(osirisUser, osirisAdmin));

        userMount1 = new UserMount("mount1", "/data/mount1", MountType.RO);

        userMount1.setOwner(osirisUser);

        userMount2 = new UserMount("mount2", "/data/mount2", MountType.RW);

        userMount2.setOwner(osirisUser);

        userMount3 = new UserMount("mount3", "/data/mount3", MountType.RO);

        userMount3.setOwner(osirisAdmin);

        dataService.save(ImmutableSet.of(userMount1, userMount2, userMount3));
    }

    @After
    public void tearDown() {
        dataService.deleteAll();
    }

    @Test
    public void testFindByType() throws Exception {
        mockMvc.perform(get("/api/userMounts/")
                .header("REMOTE_USER", osirisUser.getName())).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.userMounts").isArray())
                .andExpect(jsonPath("$._embedded.userMounts.length()").value(2))
                .andExpect(jsonPath("$._embedded.userMounts[0].name").value("mount1"));

        mockMvc.perform(get("/api/userMounts/")
                .header("REMOTE_USER", "nonExistingUser")).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.userMounts").isArray())
                .andExpect(jsonPath("$._embedded.userMounts.length()").value(0));

        mockMvc.perform(get("/api/userMounts/")
                .header("REMOTE_USER", osirisAdmin.getName())).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.userMounts").isArray())
                .andExpect(jsonPath("$._embedded.userMounts.length()").value(3));

    }


}
