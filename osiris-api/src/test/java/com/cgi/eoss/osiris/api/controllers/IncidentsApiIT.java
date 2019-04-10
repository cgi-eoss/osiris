package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.api.ApiConfig;
import com.cgi.eoss.osiris.api.ApiTestConfig;
import com.cgi.eoss.osiris.model.Collection;
import com.cgi.eoss.osiris.model.Incident;
import com.cgi.eoss.osiris.model.IncidentProcessing;
import com.cgi.eoss.osiris.model.IncidentProcessingTemplate;
import com.cgi.eoss.osiris.model.IncidentType;
import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.Role;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.persistence.dao.CollectionDao;
import com.cgi.eoss.osiris.persistence.dao.IncidentProcessingDao;
import com.cgi.eoss.osiris.persistence.dao.IncidentProcessingTemplateDao;
import com.cgi.eoss.osiris.persistence.dao.OsirisServiceDao;
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

    private static final JsonPath SELF_HREF_JSONPATH = JsonPath.compile("$._links.self.href");

    @Autowired
    private UserDataService userDataService;
    @Autowired
    private IncidentTypeDataService incidentTypeDataService;
    @Autowired
    private IncidentDataService incidentDataService;
    @Autowired
    private IncidentProcessingDao processingDao;
    @Autowired
    private IncidentProcessingTemplateDao templateDao;
    @Autowired
    private CollectionDao collectionDao;
    @Autowired
    private OsirisServiceDao serviceDao;

    @Autowired
    private MockMvc mockMvc;

    private Collection collectionSingle;
    private Collection collectionBoth;

    private IncidentProcessingTemplate template1;
    private IncidentProcessingTemplate template2;
    private IncidentProcessingTemplate templateBoth;

    private OsirisService service;

    private IncidentProcessing processing1;
    private IncidentProcessing processing2;
    private IncidentProcessing processingBoth;

    private IncidentType incidentType1;
    private IncidentType incidentType2;

    private Incident incident1;
    private Incident incident2;

    private User osirisUser;
    private User osirisAdmin;

    @Before
    public void setUp() throws Exception {
        //user
        osirisUser = new User("osiris-user");
        osirisUser.setRole(Role.USER);
        osirisAdmin = new User("osiris-admin");
        osirisAdmin.setRole(Role.ADMIN);
        userDataService.save(ImmutableSet.of(osirisUser, osirisAdmin));

        //osiris service
        service = new OsirisService();
        service.setOwner(osirisAdmin);
        service.setDockerTag("dockerTag");
        service.setName("Osiris Service");
        serviceDao.save(ImmutableSet.of(service));

        //incident type
        incidentType1 = new IncidentType(osirisAdmin, "Incident Type 1", "First incident type.", "someIconId");
        incidentType2 = new IncidentType(osirisUser, "Incident Type 2", "Second incident type.", "someOtherIconId");
        incidentTypeDataService.save(ImmutableSet.of(incidentType1, incidentType2));

        //incident
        incident1 = new Incident(osirisAdmin, incidentType1, "Incident 1", "First Incident",
                "someAoi", Instant.now(), Instant.now().plus(365, ChronoUnit.DAYS));
        incident2 = new Incident(osirisUser, incidentType2, "Incident 2", "Second Incident",
                "someOtherAoi", Instant.EPOCH, Instant.EPOCH.plus(1, ChronoUnit.DAYS));
        incidentDataService.save(ImmutableSet.of(incident1, incident2));

        //incident processing template
        template1 = new IncidentProcessingTemplate();
        template1.setOwner(osirisUser);
        template1.setTitle("Template 1");
        template1.setIncidentType(incidentType1);
        template1.setService(service);
        template2 = new IncidentProcessingTemplate();
        template2.setOwner(osirisUser);
        template2.setTitle("Template 2");
        template2.setIncidentType(incidentType2);
        template2.setService(service);
        templateBoth = new IncidentProcessingTemplate();
        templateBoth.setOwner(osirisUser);
        templateBoth.setTitle("Template Both");
        templateBoth.setIncidentType(incidentType1);
        templateBoth.setService(service);
        templateDao.save(ImmutableSet.of(template1, template2, templateBoth));

        //collection
        collectionSingle = new Collection();
        collectionSingle.setName("Collection Single");
        collectionSingle.setOwner(osirisUser);
        collectionSingle.setIdentifier("single");
        collectionBoth = new Collection();
        collectionBoth.setName("Collection Both");
        collectionBoth.setOwner(osirisAdmin);
        collectionBoth.setIdentifier("Both");
        collectionDao.save(ImmutableSet.of(collectionSingle, collectionBoth));

        //incident processing
        processing1 = new IncidentProcessing();
        processing1.setCollection(collectionSingle);
        processing1.setTemplate(template1);
        processing1.setIncident(incident1);
        processing1.setOwner(osirisAdmin);
        processing2 = new IncidentProcessing();
        processing2.setCollection(collectionBoth);
        processing2.setTemplate(template2);
        processing2.setIncident(incident2);
        processing2.setOwner(osirisUser);
        processingBoth = new IncidentProcessing();
        processingBoth.setCollection(collectionBoth);
        processingBoth.setTemplate(templateBoth);
        processingBoth.setIncident(incident1);
        processingBoth.setOwner(osirisAdmin);
        processingDao.save(ImmutableSet.of(processing1, processing2, processingBoth));
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
    public void testFindByOwner() throws Exception {
        mockMvc.perform(get("/api/incidentTypes/search/findByOwner?owner=" + userUri(osirisAdmin))
                .header("REMOTE_USER", osirisAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.incidentTypes.size()").value(1));

        mockMvc.perform(get("/api/incidents/search/findByOwner")
                .header("REMOTE_USER", osirisAdmin.getName())
                .param("owner", userUri(osirisAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.incidents.size()").value(1));
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
                        "\",\"startDate\":\"2018-01-01T00:00:00.000Z\",\"endDate\":\"2018-01-10T00:00:00.000Z\"}")
                .header("REMOTE_USER", osirisAdmin.getName()))
                .andExpect(status().isCreated());

    }

    @Test
    public void testCreateNewIncidentType() throws Exception {
        mockMvc.perform(post("/api/incidentTypes/")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"New incident\",\"description\":\"some description\",\"iconId\":\"someIconId\"}")
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
                .andExpect(jsonPath("$._embedded.incidents[0].type.title").value(incidentType1.getTitle()))
                .andExpect(jsonPath("$._embedded.incidents[0].aoi").value(incident1.getAoi()))
                .andExpect(jsonPath("$._embedded.incidents[0].startDate").value(incident1.getStartDate().toString()))
                .andExpect(jsonPath("$._embedded.incidents[0].endDate").value(incident1.getEndDate().toString()))
                .andExpect(jsonPath("$._embedded.incidents[0]._links.self.href").value(endsWith("/incidents/" + incident1.getId())));
    }

    @Test
    public void testFindIncidentByFilter() throws Exception {
        mockMvc.perform(get("/api/incidents/search/findByFilterOnly").header("REMOTE_USER", osirisAdmin.getName()).param("filter", "Incident"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.incidents.size()").value(2));

        mockMvc.perform(get("/api/incidents/search/findByFilterOnly").header("REMOTE_USER", osirisAdmin.getName()).param("filter", "second"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.incidents.size()").value(1))
                .andExpect(jsonPath("$._embedded.incidents[0].title").value(incident2.getTitle()));

        mockMvc.perform(get("/api/incidents/search/findByFilterAndOwner").header("REMOTE_USER", osirisAdmin.getName()).param("filter", "Inc").param("owner", userUri(osirisAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.incidents.size()").value(1))
                .andExpect(jsonPath("$._embedded.incidents[0].title").value(incident1.getTitle()));

        String incidentType1Url = SELF_HREF_JSONPATH.read(mockMvc.perform(
                get("/api/incidentTypes/" + incidentType1.getId()).header("REMOTE_USER", osirisAdmin.getName()))
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(get("/api/incidents/search/findByFilterOnly").header("REMOTE_USER", osirisAdmin.getName()).param("filter", "Incident").param("incidentType", incidentType1Url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.incidents.size()").value(1))
                .andExpect(jsonPath("$._embedded.incidents[0].title").value(incident1.getTitle()));
    }

    @Test
    public void testFindIncidentByDateRange() throws Exception {
        String urlTemplate = "/api/incidents/search/findByDateRange";
        String name = "REMOTE_USER";
        String startDate = "startDate";
        String endDate = "endDate";
        // search before date (ignoring the epoch incident)
        mockMvc.perform(get(urlTemplate).header(name, osirisAdmin.getName()).param(startDate, Instant.now().minus(10, ChronoUnit.DAYS).toString()).param(endDate, Instant.now().minus(9, ChronoUnit.DAYS).toString()))
                .andExpect(jsonPath("$._embedded.incidents.size()").value(0));
        // search after date
        mockMvc.perform(get(urlTemplate).header(name, osirisAdmin.getName()).param(startDate, Instant.now().plus(380, ChronoUnit.DAYS).toString()).param(endDate, Instant.now().plus(400, ChronoUnit.DAYS).toString()))
                .andExpect(jsonPath("$._embedded.incidents.size()").value(0));
        // search partially within
        mockMvc.perform(get(urlTemplate).header(name, osirisAdmin.getName()).param(startDate, Instant.now().minus(10, ChronoUnit.DAYS).toString()).param(endDate, Instant.now().plus(10, ChronoUnit.DAYS).toString()))
                .andExpect(jsonPath("$._embedded.incidents.size()").value(1));
        // search fully within
        mockMvc.perform(get(urlTemplate).header(name, osirisAdmin.getName()).param(startDate, Instant.now().plus(10, ChronoUnit.DAYS).toString()).param(endDate, Instant.now().plus(20, ChronoUnit.DAYS).toString()))
                .andExpect(jsonPath("$._embedded.incidents.size()").value(1));
        // search encompassingly
        mockMvc.perform(get(urlTemplate).header(name, osirisAdmin.getName()).param(startDate, Instant.now().minus(10, ChronoUnit.DAYS).toString()).param(endDate, Instant.now().plus(380, ChronoUnit.DAYS).toString()))
                .andExpect(jsonPath("$._embedded.incidents.size()").value(1));
        // search exactly matching
        mockMvc.perform(get(urlTemplate).header(name, osirisAdmin.getName()).param(startDate, Instant.EPOCH.toString()).param(endDate, Instant.EPOCH.plus(1, ChronoUnit.DAYS).toString()))
                .andExpect(jsonPath("$._embedded.incidents.size()").value(1));
        // search for both of the defined incidents ( both partially within )
        mockMvc.perform(get(urlTemplate).header(name, osirisAdmin.getName()).param(startDate, Instant.EPOCH.plus(1, ChronoUnit.HOURS).toString()).param(endDate, Instant.now().plus(20, ChronoUnit.DAYS).toString()))
                .andExpect(jsonPath("$._embedded.incidents.size()").value(2));
    }

    @Test
    public void testFindIncidentByCollection() throws Exception {
        String urlTemplate = "/api/incidents/search/findByCollection";
        String name = "REMOTE_USER";
        String collection = "collection";

        // search by collection related to a single incident
        mockMvc.perform(get(urlTemplate).header(name, osirisAdmin.getName()).param(collection, collectionUri(collectionSingle)))
                .andExpect(jsonPath("$._embedded.incidents.size()").value(1));
        // search by collection related to a two incidents
        mockMvc.perform(get(urlTemplate).header(name, osirisAdmin.getName()).param(collection, collectionUri(collectionBoth)))
                .andExpect(jsonPath("$._embedded.incidents.size()").value(2));
        // search by a malformed collection uri
        mockMvc.perform(get(urlTemplate).header(name, osirisAdmin.getName()).param(collection, "malformed"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void testFindIncidentTypeByFilter() throws Exception {
        mockMvc.perform(get("/api/incidentTypes/search/findByFilterOnly").header("REMOTE_USER", osirisAdmin.getName()).param("filter", "Incident"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.incidentTypes.size()").value(2));

        mockMvc.perform(get("/api/incidentTypes/search/findByFilterOnly").header("REMOTE_USER", osirisAdmin.getName()).param("filter", "second"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.incidentTypes.size()").value(1))
                .andExpect(jsonPath("$._embedded.incidentTypes[0].title").value(incidentType2.getTitle()));

        mockMvc.perform(get("/api/incidentTypes/search/findByFilterAndOwner").header("REMOTE_USER", osirisAdmin.getName()).param("filter", "Inc").param("owner", userUri(osirisAdmin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.incidentTypes.size()").value(1))
                .andExpect(jsonPath("$._embedded.incidentTypes[0].title").value(incidentType1.getTitle()));
    }

    private String userUri(User user) throws Exception {
        String jsonResult = mockMvc.perform(
                get("/api/users/" + user.getId()).header("REMOTE_USER", osirisAdmin.getName()))
                .andReturn().getResponse().getContentAsString();
        return SELF_HREF_JSONPATH.read(jsonResult);
    }

    private String collectionUri(Collection collection) throws Exception {
        String jsonResult = mockMvc.perform(
                get("/api/collections/" + collection.getId()).header("REMOTE_USER", osirisAdmin.getName()))
                .andReturn().getResponse().getContentAsString();
        return SELF_HREF_JSONPATH.read(jsonResult);
    }
}