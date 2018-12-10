package com.cgi.eoss.osiris.api.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
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
import com.cgi.eoss.osiris.api.ApiConfig;
import com.cgi.eoss.osiris.api.ApiTestConfig;
import com.cgi.eoss.osiris.model.Role;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.model.UserPreference;
import com.cgi.eoss.osiris.persistence.service.UserDataService;
import com.cgi.eoss.osiris.persistence.service.UserPreferenceDataService;
import com.google.common.collect.ImmutableSet;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class UserPreferencesApiIT {

    @Autowired
    private UserPreferenceDataService dataService;

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private MockMvc mockMvc;

    private User osirisUser;
    private User osirisAdmin;

    private UserPreference userPreference1;
    private UserPreference userPreference2;
    private UserPreference userPreference3;

    @Before
    public void setUp() {
        osirisUser = new User("osiris-user");
        osirisUser.setRole(Role.USER);
        osirisAdmin = new User("osiris-admin");
        osirisAdmin.setRole(Role.ADMIN);

        userDataService.save(ImmutableSet.of(osirisUser, osirisAdmin));

        userPreference1 = new UserPreference("Name1", "Type1", "Value1");

        userPreference1.setOwner(osirisUser);

        userPreference2 = new UserPreference("Name2", "Type2", "Value2");

        userPreference2.setOwner(osirisUser);

        userPreference3 = new UserPreference("Name3", "Type2", "Value3");

        userPreference3.setOwner(osirisUser);

        dataService.save(ImmutableSet.of(userPreference1, userPreference2, userPreference3));
    }

    @After
    public void tearDown() {
        dataService.deleteAll();
    }

    @Test
    public void testFindByType() throws Exception {
        mockMvc.perform(get("/api/userPreferences/search/findByType?type=Type1")
                .header("REMOTE_USER", osirisUser.getName())).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.userPreferences").isArray())
                .andExpect(jsonPath("$._embedded.userPreferences.length()").value(1))
                .andExpect(jsonPath("$._embedded.userPreferences[0].name").value("Name1"));

        mockMvc.perform(get("/api/userPreferences/search/findByType?type=Type2")
                .header("REMOTE_USER", osirisUser.getName())).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.userPreferences").isArray())
                .andExpect(jsonPath("$._embedded.userPreferences.length()").value(2));

        mockMvc.perform(get("/api/userPreferences/search/findByType?type=Type3")
                .header("REMOTE_USER", osirisUser.getName())).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.userPreferences").isArray())
                .andExpect(jsonPath("$._embedded.userPreferences.length()").value(0));

    }

    @Test
    public void testFindByName() throws Exception {
        mockMvc.perform(get("/api/userPreferences/search/findByName?name=Name1")
                .header("REMOTE_USER", osirisUser.getName())).andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.userPreferences").isArray())
                .andExpect(jsonPath("$._embedded.userPreferences.length()").value(1))
                .andExpect(jsonPath("$._embedded.userPreferences[0].name").value("Name1"));
    }

}
