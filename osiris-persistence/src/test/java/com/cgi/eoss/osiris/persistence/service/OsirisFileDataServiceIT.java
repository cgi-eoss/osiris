package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.persistence.PersistenceConfig;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {PersistenceConfig.class})
@TestPropertySource("classpath:test-persistence.properties")
@Transactional
public class OsirisFileDataServiceIT {
    @Autowired
    private OsirisFileDataService dataService;
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

        dataService.save(ImmutableSet.of(osirisFile, osirisFile2));

        assertThat(dataService.getAll(), is(ImmutableList.of(osirisFile, osirisFile2)));
        assertThat(dataService.getById(osirisFile.getId()), is(osirisFile));
        assertThat(dataService.getByIds(ImmutableSet.of(osirisFile.getId())), is(ImmutableList.of(osirisFile)));
        assertThat(dataService.isUniqueAndValid(new OsirisFile(URI.create("osiris://osirisFile"), UUID.randomUUID())), is(false));
        assertThat(dataService.isUniqueAndValid(new OsirisFile(URI.create("osiris://newUri"), osirisFile.getRestoId())), is(false));
        assertThat(dataService.isUniqueAndValid(new OsirisFile(URI.create("osiris://newUri"), UUID.randomUUID())), is(true));

        assertThat(dataService.findByOwner(owner), is(ImmutableList.of(osirisFile, osirisFile2)));
        assertThat(dataService.findByOwner(owner2), is(ImmutableList.of()));
        assertThat(dataService.getByRestoId(osirisFile.getRestoId()), is(osirisFile));
        assertThat(dataService.getByRestoId(osirisFile2.getRestoId()), is(osirisFile2));
    }

}