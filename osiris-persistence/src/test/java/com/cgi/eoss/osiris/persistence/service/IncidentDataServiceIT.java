package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.Collection;
import com.cgi.eoss.osiris.model.Incident;
import com.cgi.eoss.osiris.model.IncidentType;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.persistence.PersistenceConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.time.Instant;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PersistenceConfig.class})
@TestPropertySource("classpath:test-persistence.properties")
@Transactional
public class IncidentDataServiceIT {
    
	@Autowired
	private UserDataService userDataService;
	
	@Autowired
	private IncidentTypeDataService incidentTypeDataService;
	
	@Autowired
	private IncidentDataService incidentDataService;
	
	@Autowired
	private CollectionDataService collectionDataService;
	
    @Test
    public void test() throws Exception {
        User owner = new User("owner-uid");
        userDataService.save(owner);
        IncidentType incidentType = new IncidentType(owner, "test", "test description", null);
        incidentTypeDataService.save(incidentType);
        Incident incident = new Incident(owner, incidentType, "test", "test description", "test aoi", Instant.now(), Instant.now());
        incident = incidentDataService.save(incident);
        assertThat(incident, is(notNullValue()));
        Collection collection = new Collection("test", owner);
        collection.setIdentifier("test");
        collectionDataService.save(collection);
        incident.getCollections().add(collection);
        incident = incidentDataService.save(incident);
        assertThat(incident, is(notNullValue()));
        assertThat(incident.getCollections().size(), is(1));
    }

}