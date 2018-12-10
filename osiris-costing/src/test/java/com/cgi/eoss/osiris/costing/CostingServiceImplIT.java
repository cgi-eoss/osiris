package com.cgi.eoss.osiris.costing;

import com.cgi.eoss.osiris.model.CostingExpression;
import com.cgi.eoss.osiris.model.DataSource;
import com.cgi.eoss.osiris.model.OsirisFile;
import com.cgi.eoss.osiris.model.OsirisService;
import com.cgi.eoss.osiris.model.Job;
import com.cgi.eoss.osiris.model.JobConfig;
import com.cgi.eoss.osiris.model.User;
import com.cgi.eoss.osiris.model.WalletTransaction;
import com.cgi.eoss.osiris.persistence.service.CostingExpressionDataService;
import com.cgi.eoss.osiris.persistence.service.UserDataService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {CostingConfig.class})
@TestPropertySource("classpath:test-costing.properties")
@Transactional
public class CostingServiceImplIT {

    @Autowired
    private CostingService costingService;

    @Autowired
    private CostingExpressionDataService costingExpressionDataService;

    @Autowired
    private UserDataService userDataService;

    @Test
    public void estimateJobCost() throws Exception {
        OsirisService service = new OsirisService("osirisService", null, "dockerTag");
        service.setId(1L);
        JobConfig jobConfig = new JobConfig();
        jobConfig.setId(1L);
        jobConfig.setService(service);

        int defaultCost = costingService.estimateJobCost(jobConfig);
        assertThat(defaultCost, is(1));

        CostingExpression costingExpression = CostingExpression.builder()
                .type(CostingExpression.Type.SERVICE)
                .associatedId(1L)
                .costExpression("1")
                .estimatedCostExpression("service.name.length()")
                .build();
        costingExpressionDataService.save(costingExpression);

        int cost = costingService.estimateJobCost(jobConfig);
        assertThat(cost, is(service.getName().length()));
    }

    @Test
    public void estimateDownloadCost() throws Exception {
        DataSource dataSource = new DataSource();
        dataSource.setId(1L);
        dataSource.setName("OSIRIS DataSource");
        OsirisFile osirisFile = new OsirisFile(URI.create("osiris:///foo"), UUID.randomUUID());
        osirisFile.setId(1L);
        osirisFile.setDataSource(dataSource);
        osirisFile.setFilesize(585L);

        int defaultCost = costingService.estimateDownloadCost(osirisFile);
        assertThat(defaultCost, is(1));

        CostingExpression costingExpression = CostingExpression.builder()
                .type(CostingExpression.Type.DOWNLOAD)
                .associatedId(1L)
                .costExpression("1")
                .estimatedCostExpression("T(Math).round(2 * T(Math).ceil(filesize/100.0))")
                .build();
        costingExpressionDataService.save(costingExpression);

        int cost = costingService.estimateDownloadCost(osirisFile);
        assertThat(cost, is(2 * (int) Math.ceil((double) osirisFile.getFilesize() / 100)));
        assertThat(cost, is(12));
    }

    @Test
    public void chargeForJob() throws Exception {
        User owner = new User("owner-uid");
        userDataService.save(owner);

        int startingBalance = owner.getWallet().getBalance();

        OsirisService service = new OsirisService("osirisService", null, "dockerTag");
        service.setId(1L);
        JobConfig jobConfig = new JobConfig();
        jobConfig.setId(1L);
        jobConfig.setService(service);
        Job job = new Job(jobConfig, "jobId", owner);

        costingService.chargeForJob(owner.getWallet(), job);
        assertThat(owner.getWallet().getBalance(), is(startingBalance - 1));

        CostingExpression costingExpression = CostingExpression.builder()
                .type(CostingExpression.Type.SERVICE)
                .associatedId(1L)
                .costExpression("config.service.name.length()")
                .estimatedCostExpression("1")
                .build();
        costingExpressionDataService.save(costingExpression);

        costingService.chargeForJob(owner.getWallet(), job);
        assertThat(owner.getWallet().getBalance(), is(startingBalance - 1 - service.getName().length()));

        List<WalletTransaction> transactions = owner.getWallet().getTransactions();
        assertThat(transactions.size(), is(3));
        assertThat(transactions.get(1).getBalanceChange(), is(-1));
        assertThat(transactions.get(2).getBalanceChange(), is(-service.getName().length()));
    }

    @Test
    public void chargeForDownload() throws Exception {
        User owner = new User("owner-uid");
        userDataService.save(owner);

        int startingBalance = owner.getWallet().getBalance();

        DataSource dataSource = new DataSource();
        dataSource.setId(1L);
        dataSource.setName("OSIRIS DataSource");
        OsirisFile osirisFile = new OsirisFile(URI.create("osiris:///foo"), UUID.randomUUID());
        osirisFile.setId(1L);
        osirisFile.setDataSource(dataSource);
        osirisFile.setFilesize(329L);

        costingService.chargeForDownload(owner.getWallet(), osirisFile);
        assertThat(owner.getWallet().getBalance(), is(startingBalance - 1));

        CostingExpression costingExpression = CostingExpression.builder()
                .type(CostingExpression.Type.DOWNLOAD)
                .associatedId(1L)
                .costExpression("T(Math).round(2 * T(Math).ceil(filesize/100.0))")
                .build();
        costingExpressionDataService.save(costingExpression);

        costingService.chargeForDownload(owner.getWallet(), osirisFile);
        assertThat(owner.getWallet().getBalance(), is(startingBalance - 1 - (2 * (int) Math.ceil(osirisFile.getFilesize() / 100.0))));

        List<WalletTransaction> transactions = owner.getWallet().getTransactions();
        assertThat(transactions.size(), is(3));
        assertThat(transactions.get(1).getBalanceChange(), is(-1));
        assertThat(transactions.get(2).getBalanceChange(), is(-(2 * (int) Math.ceil(osirisFile.getFilesize() / 100.0))));
    }

}