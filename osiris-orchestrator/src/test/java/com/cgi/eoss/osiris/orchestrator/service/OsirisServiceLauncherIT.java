package com.cgi.eoss.osiris.orchestrator.service;

import com.cgi.eoss.osiris.orchestrator.OrchestratorConfig;
import com.cgi.eoss.osiris.orchestrator.OrchestratorTestConfig;
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

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {OrchestratorConfig.class, OrchestratorTestConfig.class})
@TestPropertySource("classpath:test-orchestrator.properties")
@Transactional
public class OsirisServiceLauncherIT {

    @Autowired
    private OsirisServiceLauncher osirisServiceLauncher;

    @Test
    public void test() {
        assertThat(osirisServiceLauncher, is(notNullValue()));
    }

}