package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.api.ApiConfig;
import com.cgi.eoss.osiris.api.ApiTestConfig;
import com.cgi.eoss.osiris.model.Incident;
import com.cgi.eoss.osiris.model.IncidentType;
import com.cgi.eoss.osiris.model.Role;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.persistence.service.IncidentDataService;
import com.cgi.eoss.osiris.persistence.service.IncidentTypeDataService;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class IncidentsApiIT {

    private static final JsonPath USER_HREF_JSONPATH = JsonPath.compile("$._links.self.href");

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private IncidentTypeDataService incidentTypeDataService;

    @Autowired
    private IncidentDataService incidentDataService;

    @Autowired
    private MockMvc mockMvc;

    private IncidentType incidentType1;
    private IncidentType incidentType2;

    private Incident incident1;
    private Incident incident2;

    private User osirisUser;
    private User osirisAdmin;

    @Before
    public void setUp() throws Exception {
        osirisUser = new User("osiris-user");
        osirisUser.setRole(Role.USER);
        osirisAdmin = new User("osiris-admin");
        osirisAdmin.setRole(Role.ADMIN);
        userDataService.save(ImmutableSet.of(osirisUser, osirisAdmin));

        incidentType1 = new IncidentType(osirisAdmin, "Incident Type 1", "First incident type.", "someIconId");
        incidentType2 = new IncidentType(osirisUser, "Incident Type 2", "Second incident type.", "someOtherIconId");

        incidentTypeDataService.save(ImmutableSet.of(incidentType1, incidentType2));

        incident1 = new Incident(osirisAdmin, incidentType1, "Incident 1", "First Incident",
                "someAoi", Instant.now(), Instant.now().plus(365, ChronoUnit.DAYS));
        incident2 = new Incident(osirisUser, incidentType2, "Incident 2", "Second Incident",
                "someOtherAoi", Instant.EPOCH, Instant.EPOCH.plus(1, ChronoUnit.DAYS));

        incidentDataService.save(ImmutableSet.of(incident1, incident2));
    }

    @After
    public void tearDown() throws Exception {
        incidentDataService.deleteAll();
        incidentTypeDataService.deleteAll();
    }

    @Test
    public void testGetIncidentType() throws Exception {
        mockMvc.perform(get("/api/incidentTypes/" + incidentType1.getId()).header("REMOTE_USER", osirisAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Incident Type 1"))
                .andExpect(jsonPath("$.description").value("First incident type."))
                .andExpect(jsonPath("$.iconId").value("someIconId"))
                .andExpect(jsonPath("$._links.self.href").value(endsWith("/incidentTypes/" + incidentType1.getId())));

        mockMvc.perform(get("/api/incidentTypes/" + incidentType2.getId()).header("REMOTE_USER", osirisUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Incident Type 2"))
                .andExpect(jsonPath("$.description").value("Second incident type."))
                .andExpect(jsonPath("$.iconId").value("someOtherIconId"))
                .andExpect(jsonPath("$._links.self.href").value(endsWith("/incidentTypes/" + incidentType2.getId())));
    }

    @Test
    public void testGetIncident() throws Exception {
        mockMvc.perform(get("/api/incidents/" + incident1.getId()).header("REMOTE_USER", osirisAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(incident1.getTitle()))
                .andExpect(jsonPath("$.description").value(incident1.getDescription()))
                .andExpect(jsonPath("$.aoi").value(incident1.getAoi()))
                .andExpect(jsonPath("$.startDate").value(incident1.getStartDate().toString()))
                .andExpect(jsonPath("$.endDate").value(incident1.getEndDate().toString()))
                .andExpect(jsonPath("$._links.self.href").value(endsWith("/incidents/" + incident1.getId())));

        mockMvc.perform(get("/api/incidents/" + incident2.getId()).header("REMOTE_USER", osirisUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(incident2.getTitle()))
                .andExpect(jsonPath("$.description").value(incident2.getDescription()))
                .andExpect(jsonPath("$.aoi").value(incident2.getAoi()))
                .andExpect(jsonPath("$.startDate").value(incident2.getStartDate().toString()))
                .andExpect(jsonPath("$.endDate").value(incident2.getEndDate().toString()))
                .andExpect(jsonPath("$._links.self.href").value(endsWith("/incidents/" + incident2.getId())));
    }

    @Test
    public void testIncidentAccessDeniedForNonOwner() throws Exception {
        mockMvc.perform(get("/api/incidentTypes/" + incidentType1.getId()).header("REMOTE_USER", osirisUser.getName()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/incidents/" + incident1.getId()).header("REMOTE_USER", osirisUser.getName()))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testCreateNewIncident() throws Exception {
        String incidentType1Url = JsonPath.compile("$._links.self.href")
                .read(mockMvc.perform(get("/api/incidentTypes/" + incidentType1.getId()).header("REMOTE_USER", osirisAdmin.getName()))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());

        mockMvc.perform(post("/api/incidents/")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"New incident\",\"aoi\":\"newAoi\",\"type\":\"" + incidentType1Url +
                        "\",\"startDate\":\"2018-01-01T00:00:00.000Z\",\"endDate\":\"2018-01-10T00:00:00.000Z\",\"owner\":\""
                        + userUri(osirisAdmin) + "\"}")
                .header("REMOTE_USER", osirisAdmin.getName()))
                .andExpect(status().isCreated());

    }

    @Test
    public void testFindIncidentByType() throws Exception {
        String incidentType1Url = JsonPath.compile("$._links.self.href")
                .read(mockMvc.perform(get("/api/incidentTypes/" + incidentType1.getId()).header("REMOTE_USER", osirisAdmin.getName()))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());

        mockMvc.perform(get("/api/incidents/search/findByType").header("REMOTE_USER", osirisAdmin.getName()).param("type", incidentType1Url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.incidents.size()").value(1))
                .andExpect(jsonPath("$._embedded.incidents[0].title").value(incident1.getTitle()))
                .andExpect(jsonPath("$._embedded.incidents[0].description").value(incident1.getDescription()))
                .andExpect(jsonPath("$._embedded.incidents[0].aoi").value(incident1.getAoi()))
                .andExpect(jsonPath("$._embedded.incidents[0].startDate").value(incident1.getStartDate().toString()))
                .andExpect(jsonPath("$._embedded.incidents[0].endDate").value(incident1.getEndDate().toString()))
                .andExpect(jsonPath("$._embedded.incidents[0]._links.self.href").value(endsWith("/incidents/" + incident1.getId())));
    }

    private String userUri(User user) throws Exception {
        String jsonResult = mockMvc.perform(
                get("/api/users/" + user.getId()).header("REMOTE_USER", osirisAdmin.getName()))
                .andReturn().getResponse().getContentAsString();
        return USER_HREF_JSONPATH.read(jsonResult);
    }
}