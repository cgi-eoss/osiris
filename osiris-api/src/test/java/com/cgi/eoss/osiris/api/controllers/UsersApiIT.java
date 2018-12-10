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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsIterableContainingInRelativeOrder.containsInRelativeOrder;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class UsersApiIT {

    @Autowired
    private UserDataService dataService;

    @Autowired
    private MockMvc mockMvc;

    private User osirisGuest;
    private User osirisUser;
    private User osirisAdmin;

    @Before
    public void setUp() {
        osirisGuest = new User("osiris-guest");
        osirisGuest.setRole(Role.GUEST);
        osirisUser = new User("osiris-user");
        osirisUser.setRole(Role.USER);
        osirisAdmin = new User("osiris-admin");
        osirisAdmin.setRole(Role.ADMIN);

        dataService.save(ImmutableSet.of(osirisGuest, osirisUser, osirisAdmin));
    }

    @Test
    public void testGetIndex() throws Exception {
        mockMvc.perform(get("/api/").header("REMOTE_USER", osirisUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.users").exists());
    }

    @Test
    public void testGet() throws Exception {
        User owner = dataService.save(new User("owner-uid"));
        User owner2 = dataService.save(new User("owner-uid2"));
        owner.setEmail("owner@example.com");
        owner2.setEmail("owner2@example.com");

        mockMvc.perform(get("/api/users").header("REMOTE_USER", osirisUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.users").isArray())
                .andExpect(jsonPath("$._embedded.users[?(@.id=="+owner2.getId()+")].name").value("owner-uid2"));

        mockMvc.perform(get("/api/users/" + owner.getId()).header("REMOTE_USER", "osiris-new-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("owner-uid"))
                .andExpect(jsonPath("$.email").value("owner@example.com"))
                .andExpect(jsonPath("$._links.self.href").value(endsWith("/users/" + owner.getId())));

        // The unknown user "osiris-new-user" was created automatically
        assertThat(dataService.getByName("osiris-new-user"), is(notNullValue()));
        assertThat(dataService.getByName("osiris-new-user").getRole(), is(Role.GUEST));
    }

    @Test
    public void testFindByFilter() throws Exception {
        User owner = dataService.save(new User("owner-uid"));
        User owner2 = dataService.save(new User("owner-uid2"));
        owner.setEmail("owner@example.com");
        owner2.setEmail("owner2@example.com");

        mockMvc.perform(get("/api/users/search/byFilter?filter=er2").header("REMOTE_USER", osirisUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.users").isArray())
                .andExpect(jsonPath("$._embedded.users.length()").value(1))
                .andExpect(jsonPath("$._embedded.users[0].name").value("owner-uid2"));

        mockMvc.perform(get("/api/users/search/byFilter?filter=uid&sort=name,asc").header("REMOTE_USER", osirisUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.users").isArray())
                .andExpect(jsonPath("$._embedded.users.length()").value(2))
                .andExpect(jsonPath("$._embedded.users[0].name").value("owner-uid"))
                .andExpect(jsonPath("$._embedded.users[1].name").value("owner-uid2"));
    }

    @Test
    public void testGetWithoutAuthRequestAttribute() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testCreate() throws Exception {
        mockMvc.perform(post("/api/users").header("REMOTE_USER", osirisAdmin.getName()).content("{\"name\": \"Osiris User\", \"email\":\"osiris.user@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/users/\\d+$")));
        mockMvc.perform(post("/api/users").header("REMOTE_USER", osirisAdmin.getName()).content("{\"name\": \"Osiris User 2\", \"email\":\"osiris.user.2@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/users/\\d+$")));

        User user = new User("Osiris User");
        User user2 = new User("Osiris User 2");
        user.setEmail("osiris.user@example.com");
        user2.setEmail("osiris.user.2@example.com");

        assertThat(dataService.getAll(), containsInRelativeOrder(user, user2));
    }

    @Test
    public void testUpdate() throws Exception {
        MvcResult result =
                mockMvc.perform(post("/api/users").header("REMOTE_USER", osirisAdmin.getName()).content("{\"name\": \"Osiris User\", \"email\":\"osiris.user@example.com\"}"))
                        .andExpect(status().isCreated())
                        .andExpect(header().string("Location", matchesPattern(".*/users/\\d+$")))
                        .andReturn();

        String location = result.getResponse().getHeader("Location");

        mockMvc.perform(put(location).header("REMOTE_USER", osirisAdmin.getName()).content("{\"name\": \"New Name\", \"email\":\"new.osiris.email@example.com\"}")).andExpect(
                status().isNoContent());

        User expected = new User("New Name");
        expected.setEmail("new.osiris.email@example.com");
        assertThat(dataService.getByName("New Name"), is(expected));
    }

    @Test
    public void testPartialUpdate() throws Exception {
        MvcResult result =
                mockMvc.perform(post("/api/users").header("REMOTE_USER", osirisAdmin.getName()).content("{\"name\": \"Osiris User\", \"email\":\"osiris.user@example.com\"}"))
                        .andExpect(status().isCreated())
                        .andExpect(header().string("Location", matchesPattern(".*/users/\\d+$")))
                        .andReturn();

        String location = result.getResponse().getHeader("Location");

        mockMvc.perform(patch(location).header("REMOTE_USER", osirisAdmin.getName()).content("{\"name\": \"New Name\"}"))
                .andExpect(status().isNoContent());

        User expected = new User("New Name");
        expected.setEmail("osiris.user@example.com");
        assertThat(dataService.getByName("New Name"), is(expected));
    }

    @Test
    public void testDelete() throws Exception {
        MvcResult result =
                mockMvc.perform(post("/api/users").header("REMOTE_USER", osirisAdmin.getName()).content("{\"name\": \"Osiris User\", \"email\":\"osiris.user@example.com\"}"))
                        .andExpect(status().isCreated())
                        .andExpect(header().string("Location", matchesPattern(".*/users/\\d+$")))
                        .andReturn();

        String location = result.getResponse().getHeader("Location");

        mockMvc.perform(delete(location).header("REMOTE_USER", osirisUser.getName()))
                .andExpect(status().isMethodNotAllowed());

        User expected = new User("Osiris User");
        expected.setEmail("osiris.user@example.com");
        assertThat(dataService.getByName("Osiris User"), is(notNullValue()));
    }

}