package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.api.ApiConfig;
import com.cgi.eoss.osiris.api.ApiTestConfig;
import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.Role;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.persistence.service.ServiceDataService;
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
public class PublishingRequestsApiIT {
    @Autowired
    private UserDataService userDataService;

    @Autowired
    private ServiceDataService serviceDataService;

    private OsirisService service1;
    private OsirisService service2;
    private OsirisService service3;

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

        userDataService.save(ImmutableSet.of(osirisGuest, osirisUser, osirisAdmin));

        service1 = new OsirisService("service-1", osirisAdmin, "dockerTag");
        service1.setStatus(OsirisService.Status.AVAILABLE);
        service2 = new OsirisService("service-2", osirisUser, "dockerTag");
        service2.setStatus(OsirisService.Status.IN_DEVELOPMENT);
        service3 = new OsirisService("service-3", osirisGuest, "dockerTag");
        service3.setStatus(OsirisService.Status.IN_DEVELOPMENT);
        serviceDataService.save(ImmutableSet.of(service1, service2, service3));
    }

    @Test
    public void testRequestPublishService() throws Exception {
        mockMvc.perform(post("/api/publishingRequests/requestPublishService/" + service1.getId()).header("REMOTE_USER", osirisUser.getName()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/publishingRequests/requestPublishService/" + service2.getId()).header("REMOTE_USER", osirisUser.getName()))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/publishingRequests/requestPublishService/" + service3.getId()).header("REMOTE_USER", osirisUser.getName()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/publishingRequests/requestPublishService/" + service2.getId()).header("REMOTE_USER", osirisUser.getName()))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testGet() throws Exception {
        String svc2Url = mockMvc.perform(post("/api/publishingRequests/requestPublishService/" + service2.getId()).header("REMOTE_USER", osirisUser.getName()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        String svc3Url = mockMvc.perform(post("/api/publishingRequests/requestPublishService/" + service3.getId()).header("REMOTE_USER", osirisGuest.getName()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(svc2Url).header("REMOTE_USER", osirisUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$._links.associated.href").value(endsWith("/api/services/" + service2.getId() + "{?projection}")));
        mockMvc.perform(get(svc2Url).header("REMOTE_USER", osirisGuest.getName()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(svc2Url).header("REMOTE_USER", osirisAdmin.getName()))
                .andExpect(status().isOk());

        mockMvc.perform(get(svc3Url).header("REMOTE_USER", osirisGuest.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$._links.associated.href").value(endsWith("/api/services/" + service3.getId() + "{?projection}")));
        mockMvc.perform(get(svc3Url).header("REMOTE_USER", osirisUser.getName()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/publishingRequests").header("REMOTE_USER", osirisUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.publishingRequests.length()").value(1));
        mockMvc.perform(get("/api/publishingRequests").header("REMOTE_USER", osirisGuest.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.publishingRequests.length()").value(1));
        mockMvc.perform(get("/api/publishingRequests").header("REMOTE_USER", osirisAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.publishingRequests.length()").value(2));
    }

    @Test
    public void testFindByStatusAndPublish() throws Exception {
        String svc2Url = mockMvc.perform(post("/api/publishingRequests/requestPublishService/" + service2.getId()).header("REMOTE_USER", osirisUser.getName()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        String svc3Url = mockMvc.perform(post("/api/publishingRequests/requestPublishService/" + service3.getId()).header("REMOTE_USER", osirisGuest.getName()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get("/api/publishingRequests/search/findByStatus?status=REQUESTED").header("REMOTE_USER", osirisUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.publishingRequests.length()").value(1));
        mockMvc.perform(get("/api/publishingRequests/search/findByStatus?status=REQUESTED").header("REMOTE_USER", osirisGuest.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.publishingRequests.length()").value(1));
        mockMvc.perform(get("/api/publishingRequests/search/findByStatus?status=REQUESTED").header("REMOTE_USER", osirisAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.publishingRequests.length()").value(2));

        mockMvc.perform(post("/api/contentAuthority/services/publish/" + service2.getId()).header("REMOTE_USER", osirisAdmin.getName()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/publishingRequests/search/findByStatus?status=REQUESTED,NEEDS_INFO,REJECTED").header("REMOTE_USER", osirisUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.publishingRequests.length()").value(0));
        mockMvc.perform(get("/api/publishingRequests/search/findByStatus?status=GRANTED").header("REMOTE_USER", osirisUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.publishingRequests.length()").value(1));
        mockMvc.perform(get("/api/publishingRequests/search/findByStatus?status=REQUESTED").header("REMOTE_USER", osirisAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.publishingRequests.length()").value(1));
    }

}
