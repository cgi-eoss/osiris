package com.cgi.eoss.osiris.api.controllers;

import com.cgi.eoss.osiris.api.ApiConfig;
import com.cgi.eoss.osiris.api.ApiTestConfig;
import com.cgi.eoss.osiris.catalogue.CatalogueService;
import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.Role;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.persistence.service.OsirisFileDataService;
import com.cgi.eoss.osiris.persistence.service.UserDataService;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import com.jayway.jsonpath.JsonPath;
import okhttp3.HttpUrl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {ApiConfig.class, ApiTestConfig.class})
@AutoConfigureMockMvc
@TestPropertySource("classpath:test-api.properties")
@Transactional
public class OsirisFilesApiIT {

    @Autowired
    private UserDataService userDataService;

    @Autowired
    private OsirisFileDataService fileDataService;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CatalogueService catalogueService;

    private OsirisFile testFile1;
    private OsirisFile testFile2;

    private User osirisUser;
    private User osirisAdmin;

    @Before
    public void setUp() throws Exception {
        osirisUser = new User("osiris-user");
        osirisUser.setRole(Role.USER);
        osirisAdmin = new User("osiris-admin");
        osirisAdmin.setRole(Role.ADMIN);
        userDataService.save(ImmutableSet.of(osirisUser, osirisAdmin));

        UUID fileUuid = UUID.randomUUID();
        testFile1 = new OsirisFile(URI.create("osiris://refData/2/testFile1"), fileUuid);
        testFile1.setOwner(osirisAdmin);
        testFile1.setFilename("testFile1");
        testFile1.setType(OsirisFile.Type.REFERENCE_DATA);

        UUID file2Uuid = UUID.randomUUID();
        testFile2 = new OsirisFile(URI.create("osiris://outputProduct/job1/testFile2"), file2Uuid);
        testFile2.setOwner(osirisAdmin);
        testFile2.setFilename("testFile2");
        testFile2.setType(OsirisFile.Type.OUTPUT_PRODUCT);

        fileDataService.save(ImmutableSet.of(testFile1, testFile2));
    }

    @After
    public void tearDown() throws Exception {
        fileDataService.deleteAll();
    }

    @Test
    public void testGet() throws Exception {
        mockMvc.perform(get("/api/osirisFiles/" + testFile1.getId()).header("REMOTE_USER", osirisAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").value(endsWith("/osirisFiles/" + testFile1.getId())))
                .andExpect(jsonPath("$._links.download.href").value(endsWith("/osirisFiles/" + testFile1.getId() + "/dl")));

        mockMvc.perform(get("/api/osirisFiles/search/findByType?type=REFERENCE_DATA").header("REMOTE_USER", osirisAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.osirisFiles").isArray())
                .andExpect(jsonPath("$._embedded.osirisFiles.length()").value(1))
                .andExpect(jsonPath("$._embedded.osirisFiles[0].filename").value("testFile1"));

        // Results are filtered by ACL
        mockMvc.perform(get("/api/osirisFiles/search/findByType?type=REFERENCE_DATA").header("REMOTE_USER", osirisUser.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.osirisFiles").isArray())
                .andExpect(jsonPath("$._embedded.osirisFiles.length()").value(0));

        mockMvc.perform(get("/api/osirisFiles/search/findByType?type=OUTPUT_PRODUCT").header("REMOTE_USER", osirisAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.osirisFiles").isArray())
                .andExpect(jsonPath("$._embedded.osirisFiles.length()").value(1))
                .andExpect(jsonPath("$._embedded.osirisFiles[0].filename").value("testFile2"));
    }

    @Test
    public void testGetWithProjection() throws Exception {
        when(catalogueService.getWmsUrl(testFile1.getType(), testFile1.getUri())).thenReturn(HttpUrl.parse("http://example.com/wms"));

        mockMvc.perform(get("/api/osirisFiles/" + testFile1.getId() + "?projection=detailedOsirisFile").header("REMOTE_USER", osirisAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").value(endsWith("/osirisFiles/" + testFile1.getId())))
                .andExpect(jsonPath("$._links.download.href").value(endsWith("/osirisFiles/" + testFile1.getId() + "/dl")))
                .andExpect(jsonPath("$._links.wms.href").value("http://example.com/wms"))
                .andExpect(jsonPath("$._links.osiris.href").value(testFile1.getUri().toASCIIString()));

        mockMvc.perform(get("/api/osirisFiles/" + testFile1.getId() + "?projection=shortOsirisFile").header("REMOTE_USER", osirisAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.self.href").value(endsWith("/osirisFiles/" + testFile1.getId())))
                .andExpect(jsonPath("$._links.download.href").value(endsWith("/osirisFiles/" + testFile1.getId() + "/dl")))
                .andExpect(jsonPath("$._links.wms").doesNotExist())
                .andExpect(jsonPath("$._links.osiris").doesNotExist());
    }

    @Test
    public void testFindByOwner() throws Exception {
        String osirisUserUrl = JsonPath.compile("$._links.self.href").read(
                mockMvc.perform(get("/api/users/" + osirisUser.getId()).header("REMOTE_USER", osirisAdmin.getName())).andReturn().getResponse().getContentAsString()
        );
        String osirisAdminUrl = JsonPath.compile("$._links.self.href").read(
                mockMvc.perform(get("/api/users/" + osirisAdmin.getId()).header("REMOTE_USER", osirisAdmin.getName())).andReturn().getResponse().getContentAsString()
        );

        mockMvc.perform(get("/api/osirisFiles/search/findByOwner?owner="+osirisUserUrl).header("REMOTE_USER", osirisAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.osirisFiles").isArray())
                .andExpect(jsonPath("$._embedded.osirisFiles.length()").value(0));

        mockMvc.perform(get("/api/osirisFiles/search/findByOwner?owner="+osirisAdminUrl).header("REMOTE_USER", osirisAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.osirisFiles").isArray())
                .andExpect(jsonPath("$._embedded.osirisFiles.length()").value(2));
    }

    @Test
    public void testFindByNotOwner() throws Exception {
        String osirisUserUrl = JsonPath.compile("$._links.self.href").read(
                mockMvc.perform(get("/api/users/" + osirisUser.getId()).header("REMOTE_USER", osirisAdmin.getName())).andReturn().getResponse().getContentAsString()
        );
        String osirisAdminUrl = JsonPath.compile("$._links.self.href").read(
                mockMvc.perform(get("/api/users/" + osirisAdmin.getId()).header("REMOTE_USER", osirisAdmin.getName())).andReturn().getResponse().getContentAsString()
        );

        mockMvc.perform(get("/api/osirisFiles/search/findByNotOwner?owner="+osirisUserUrl).header("REMOTE_USER", osirisAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.osirisFiles").isArray())
                .andExpect(jsonPath("$._embedded.osirisFiles.length()").value(2));

        mockMvc.perform(get("/api/osirisFiles/search/findByNotOwner?owner="+osirisAdminUrl).header("REMOTE_USER", osirisAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._embedded.osirisFiles").isArray())
                .andExpect(jsonPath("$._embedded.osirisFiles.length()").value(0));
    }

    @Test
    public void testSaveRefData() throws Exception {
        Resource fileResource = new ClassPathResource("/testFile1", OsirisFilesApiIT.class);
        MockMultipartFile uploadFile = new MockMultipartFile("file", "testFile1", "text/plain", fileResource.getInputStream());

        when(catalogueService.ingestReferenceData(any(), any())).thenReturn(testFile1);
        mockMvc.perform(fileUpload("/api/osirisFiles/refData").file(uploadFile).header("REMOTE_USER", osirisUser.getName()).param("fileType", "OTHER"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$._links.self.href").value(endsWith("/osirisFiles/" + testFile1.getId())))
                .andExpect(jsonPath("$._links.download.href").value(endsWith("/osirisFiles/" + testFile1.getId() + "/dl")));
        verify(catalogueService).ingestReferenceData(any(), any());
    }

    @Test
    public void testDownloadFile() throws Exception {
        Resource response = new ClassPathResource("/testFile1", OsirisFilesApiIT.class);
        when(catalogueService.getAsResource(testFile1)).thenReturn(response);

        mockMvc.perform(get("/api/osirisFiles/" + testFile1.getId() + "/dl").header("REMOTE_USER", osirisUser.getName()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/osirisFiles/" + testFile1.getId() + "/dl").header("REMOTE_USER", osirisAdmin.getName()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"testFile1\""))
                .andExpect(content().bytes(ByteStreams.toByteArray(response.getInputStream())));
    }

}