package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.api.ApiConfig;
import com.cgi.eoss.osiris.api.ApiTestConfig;
import com.cgi.eoss.osiris.model.Role;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.persistence.service.UserDataService;
import com.google.common.collect.ImmutableSet;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class CurrentUserApiIT {

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private MockMvc mockMvc;

    private User osirisUser;
    private User osirisAdmin;

    @Before
    public void setUp() {
        osirisUser = new User("osiris-user");
        osirisUser.setRole(Role.USER);
        osirisAdmin = new User("osiris-admin");
        osirisAdmin.setRole(Role.ADMIN);

        userDataService.save(ImmutableSet.of(osirisUser, osirisAdmin));
    }

    @Test
    public void currentUser() throws Exception {
        mockMvc.perform(get("/api/currentUser").header("REMOTE_USER", osirisUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(osirisUser.getId()))
                .andExpect(jsonPath("$.name").value(osirisUser.getName()));

        mockMvc.perform(get("/api/currentUser").header("REMOTE_USER", osirisAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(osirisAdmin.getId()))
                .andExpect(jsonPath("$.name").value(osirisAdmin.getName()));
    }

}