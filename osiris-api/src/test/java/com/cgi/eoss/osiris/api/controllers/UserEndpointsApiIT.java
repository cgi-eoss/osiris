package com.cgi.eoss.osiris.api.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.cgi.eoss.osiris.api.ApiConfig;
import com.cgi.eoss.osiris.api.ApiTestConfig;
import com.cgi.eoss.osiris.model.Role;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.model.UserEndpoint;
import com.cgi.eoss.osiris.persistence.service.UserDataService;
import com.cgi.eoss.osiris.persistence.service.UserEndpointDataService;
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
public class UserEndpointsApiIT {

    @Autowired
    private UserEndpointDataService dataService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private MockMvc mockMvc;

    private User osirisUser1;
    private User osirisUser2;
    private User osirisAdmin;

    private UserEndpoint userEndpoint1;
    private UserEndpoint userEndpoint2;
    private UserEndpoint userEndpoint3;

    @Before
    public void setUp() {
        osirisUser1 = new User("osiris-user1");
        osirisUser1.setRole(Role.USER);
        osirisUser2 = new User("osiris-user2");
        osirisUser2.setRole(Role.USER);
        osirisAdmin = new User("osiris-admin");
        osirisAdmin.setRole(Role.ADMIN);

        userDataService.save(ImmutableSet.of(osirisUser1, osirisUser2, osirisAdmin));

        userEndpoint1 = new UserEndpoint("externalSite", "http://externalSite.com");

        userEndpoint1.setOwner(osirisUser1);

        userEndpoint2 = new UserEndpoint("externalHttpsSite", "https://externalSite.com");

        userEndpoint2.setOwner(osirisUser2);

        userEndpoint3 = new UserEndpoint("anotherWebsite", "http://anotherWebsite.com");

        userEndpoint3.setOwner(osirisAdmin);

        dataService.save(ImmutableSet.of(userEndpoint1, userEndpoint2, userEndpoint3));
    }

    @After
    public void tearDown() {
        dataService.deleteAll();
    }

    @Test
    public void testFindByType() throws Exception {
        mockMvc.perform(get("/api/userEndpoints/")
                .header("REMOTE_USER", osirisUser1.getName())).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.userEndpoints").isArray())
                .andExpect(jsonPath("$._embedded.userEndpoints.length()").value(1))
                .andExpect(jsonPath("$._embedded.userEndpoints[0].name").value("externalSite"));

        mockMvc.perform(get("/api/userEndpoints/")
                .header("REMOTE_USER", osirisUser2.getName())).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.userEndpoints").isArray())
                .andExpect(jsonPath("$._embedded.userEndpoints.length()").value(1))
                .andExpect(jsonPath("$._embedded.userEndpoints[0].name").value("externalHttpsSite"));

        mockMvc.perform(get("/api/userEndpoints/")
                .header("REMOTE_USER", osirisAdmin.getName())).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.userEndpoints").isArray())
                .andExpect(jsonPath("$._embedded.userEndpoints.length()").value(3));

    }


}
