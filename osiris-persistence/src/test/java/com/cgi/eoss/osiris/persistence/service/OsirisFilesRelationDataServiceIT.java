package com.cgi.eoss.osiris.persistence.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.net.URI;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.OsirisFilesRelation;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.persistence.PersistenceConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PersistenceConfig.class})
@TestPropertySource("classpath:test-persistence.properties")
@Transactional
public class OsirisFilesRelationDataServiceIT {
    @Autowired
    private OsirisFileDataService fileDataService;
    
    @Autowired
    private OsirisFilesRelationDataService dataService;
    
    @Autowired
    private UserDataService userService;

    @Test
    public void test() throws Exception {
        User owner = new User("owner-uid");
        User owner2 = new User("owner-uid2");
        userService.save(ImmutableSet.of(owner, owner2));

        OsirisFile osirisFile = new OsirisFile();
        osirisFile.setUri(URI.create("osiris://osirisFile"));
        osirisFile.setRestoId(UUID.randomUUID());
        osirisFile.setOwner(owner);

        OsirisFile osirisFile2 = new OsirisFile();
        osirisFile2.setUri(URI.create("osiris://osirisFile2"));
        osirisFile2.setRestoId(UUID.randomUUID());
        osirisFile2.setOwner(owner);

        OsirisFile osirisFile3 = new OsirisFile();
        osirisFile3.setUri(URI.create("osiris://osirisFile3"));
        osirisFile3.setRestoId(UUID.randomUUID());
        osirisFile3.setOwner(owner);
        
        fileDataService.save(ImmutableSet.of(osirisFile, osirisFile2, osirisFile3));

        //Save a relation between osirisFile and osirisFile2
        OsirisFilesRelation relation = new OsirisFilesRelation(osirisFile, osirisFile2, OsirisFilesRelation.Type.VISUALIZATION_OF);
        dataService.save(relation);
        
        //Save a relation between osirisFile and osirisFile2
        OsirisFilesRelation relation1 = new OsirisFilesRelation(osirisFile, osirisFile3, OsirisFilesRelation.Type.VISUALIZATION_OF);
        
        dataService.save(relation1);
        
        //Retrieve files associated with osirisFile
        Set<OsirisFilesRelation> related = dataService.findBySourceFileAndType(osirisFile, OsirisFilesRelation.Type.VISUALIZATION_OF);
        assertThat(related.stream().map(r -> r.getTargetFile()).collect(Collectors.toList()), is(ImmutableList.of(osirisFile2, osirisFile3)));
        
        //Remove the source file
        osirisFile = fileDataService.getByRestoId(osirisFile.getRestoId());
        fileDataService.delete(osirisFile);
        
        assertThat(fileDataService.getAll(), is(ImmutableList.of(osirisFile2, osirisFile3)));
        
        related = dataService.findBySourceFileAndType(osirisFile, OsirisFilesRelation.Type.VISUALIZATION_OF);
        
        assertThat(related.size(), is(0));
    }

}