package com.cgi.eoss.osiris.scheduledjobs;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.cgi.eoss.osiris.scheduledjobs.service.ScheduledJobService;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ScheduledJobsConfig.class})
@TestPropertySource("classpath:test-scheduled-jobs.properties")
@SpringBootTest
public class ScheduledJobsServiceIT {
    
    @Autowired
    private ScheduledJobService scheduledJobService;
    
    @MockBean
    private CalleeStub calleeStub;
    
    @After
    public void tearDown() {
        scheduledJobService.unscheduleJob("test", "test");
        scheduledJobService.deleteJob("test", "test");
    }
    
    @Test
    public void test() throws InterruptedException {
        Map<String, Object> jobContext = new HashMap<>();
        jobContext.put("testData", "testData");
        scheduledJobService.scheduleJobEveryNSeconds(TestScheduledJob.class, "test", "test", jobContext, 1, 2);
        verify(calleeStub, timeout(10000).times(2)).callMe();
    }
    
    @Test
    public void testPersistent() throws InterruptedException {
        Map<String, Object> jobContext = new HashMap<>();
        jobContext.put("testData", "testData");
        scheduledJobService.scheduleJobEveryNSeconds(TestPersistentScheduledJob.class, "test", "test", jobContext, 1, 2);
        ArgumentCaptor<Integer> argument = ArgumentCaptor.forClass(Integer.class);
        verify(calleeStub, timeout(10000)).callMe(argument.capture());
        assertEquals(2, argument.getValue().intValue());
    }

    
   
}