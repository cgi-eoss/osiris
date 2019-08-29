package com.cgi.eoss.osiris.harvesters.ftp;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.cgi.eoss.osiris.harvesters.HarvestersTestConfig;
import com.cgi.eoss.osiris.model.DownloaderCredentials;
import com.cgi.eoss.osiris.persistence.service.DownloaderCredentialsDataService;
import com.cgi.eoss.osiris.persistence.service.RpcCredentialsService;
import com.cgi.eoss.osiris.queues.service.OsirisQueueService;
import com.cgi.eoss.osiris.rpc.CredentialsServiceGrpc;
import com.cgi.eoss.osiris.rpc.FtpJobSpec;
import com.cgi.eoss.osiris.rpc.FtpJobStarted;
import com.cgi.eoss.osiris.rpc.FtpJobStopped;
import com.cgi.eoss.osiris.rpc.Job;
import com.cgi.eoss.osiris.rpc.JobFtpFileAvailable;
import com.cgi.eoss.osiris.rpc.NoMoreJobFtpFilesAvailable;
import com.cgi.eoss.osiris.rpc.OsirisServerClient;
import com.cgi.eoss.osiris.rpc.StopFtpJob;
import com.cgi.eoss.osiris.rpc.catalogue.CatalogueServiceGrpc;
import com.cgi.eoss.osiris.rpc.worker.JobError;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.OffsetDateTime;
import java.util.Date;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { HarvestersTestConfig.class })
@TestPropertySource("classpath:test-harvesters.properties")
public class TestFtpDispatcher {

	@Autowired
	OsirisQueueService queueService;

	private FakeFtpServer fakeFtpServer;

	@MockBean
    private OsirisServerClient osirisServerClient;
	
	@Mock
    private DownloaderCredentialsDataService credentialsDataService;

	private Server server;
	
	@Before
	public void setUp() throws Exception {
		buildFakeFtpServer();
		
		InProcessServerBuilder inProcessServerBuilder = InProcessServerBuilder.forName(getClass().getName()).directExecutor();
        InProcessChannelBuilder channelBuilder = InProcessChannelBuilder.forName(getClass().getName()).directExecutor();
        RpcCredentialsService rpcCredentialsService = new RpcCredentialsService(credentialsDataService);
        inProcessServerBuilder.addService(rpcCredentialsService);
        server = inProcessServerBuilder.build().start();

        CredentialsServiceGrpc.CredentialsServiceBlockingStub credentialsService = CredentialsServiceGrpc.newBlockingStub(channelBuilder.build());
        when(osirisServerClient.credentialsServiceBlockingStub()).thenReturn(credentialsService);

        CatalogueServiceGrpc.CatalogueServiceBlockingStub catalogueService = CatalogueServiceGrpc.newBlockingStub(channelBuilder.build());
        when(osirisServerClient.catalogueServiceBlockingStub()).thenReturn(catalogueService);
        when(credentialsDataService.getByHost(any())).thenReturn(DownloaderCredentials.basicBuilder()
                .username("ftpuser")
                .password("ftppass")
                .build());
	}

	private void buildFakeFtpServer() throws Exception {
		fakeFtpServer = new FakeFtpServer();
		fakeFtpServer.setServerControlPort(0);
		fakeFtpServer.addUserAccount(new UserAccount("ftpuser", "ftppass", "/"));
		org.mockftpserver.fake.filesystem.FileSystem fileSystem = new UnixFakeFileSystem();
		fileSystem.add(new DirectoryEntry("/test_ftp_root"));
		fileSystem.add(new DirectoryEntry("/out1"));
		FileEntry testFile = new FileEntry("/test_ftp_root/out1/file1.txt", "contents");
		testFile.setLastModified(Date.from(OffsetDateTime.now().minusDays(20).toInstant()));
        FileEntry testFile2 = new FileEntry("/test_ftp_root/out1/file2.txt", "contents2");
        testFile2.setLastModified(Date.from(OffsetDateTime.now().minusDays(10).toInstant()));
        fileSystem.add(testFile);
		fileSystem.add(testFile2);
		fakeFtpServer.setFileSystem(fileSystem);
		fakeFtpServer.start();
	}

	@After
	public void tearDown() {
		server.shutdown();
	}

	@Test
	public void testFtpJob() {
		Job job = Job.newBuilder().setId("test").setIntJobId("1").setServiceId("test").build();
		FtpJobSpec ftpJobSpec = FtpJobSpec.newBuilder()
				.setFtpRootUri("ftp://127.0.0.1:" + fakeFtpServer.getServerControlPort() + "/test_ftp_root").setJob(job)
				.build();
		queueService.sendObject(OsirisQueueService.ftpJobQueueName, ftpJobSpec);
		Object resp = queueService.receiveSelectedObject(OsirisQueueService.ftpJobUpdatesQueueName, "messageType = 'FTPFileAvailable'");
		assertThat(resp instanceof JobFtpFileAvailable, is(true));
		resp = queueService.receiveSelectedObject(OsirisQueueService.ftpJobUpdatesQueueName, "messageType = 'FTPFileAvailable'");
		assertThat(resp instanceof JobFtpFileAvailable, is(true));
		StopFtpJob stopFtpJob = StopFtpJob.newBuilder().setJob(job).build();
		queueService.sendObject(OsirisQueueService.ftpJobQueueName, stopFtpJob);
		resp = queueService.receiveSelectedObject(OsirisQueueService.ftpJobUpdatesQueueName,
				"messageType IS NULL OR messageType <> 'FTPFileAvailable'");
		assertThat(resp instanceof FtpJobStarted, is(true));
		resp = queueService.receiveSelectedObject(OsirisQueueService.ftpJobUpdatesQueueName,
				"messageType IS NULL OR messageType <> 'FTPFileAvailable'");
		assertThat(resp instanceof FtpJobStopped, is(true));
	}

	@Test
	public void testFtpJobErrorOnInexistentPath() {
		Job job = Job.newBuilder().setId("test").setIntJobId("1").setServiceId("test").build();
		FtpJobSpec ftpJobSpec = FtpJobSpec.newBuilder()
				.setFtpRootUri("ftp://127.0.0.1:" + fakeFtpServer.getServerControlPort() + "/inexistent").setJob(job)
				.build();
		queueService.sendObject(OsirisQueueService.ftpJobQueueName, ftpJobSpec);
		Object resp = queueService.receiveSelectedObject(OsirisQueueService.ftpJobUpdatesQueueName,
				"messageType IS NULL OR messageType <> 'FTPFileAvailable'");
		assertThat(resp instanceof FtpJobStarted, is(true));
		resp = queueService.receiveSelectedObject(OsirisQueueService.ftpJobUpdatesQueueName,
				"messageType IS NULL OR messageType <> 'FTPFileAvailable'");
		assertThat(resp instanceof JobError, is(true));
		StopFtpJob stopFtpJob = StopFtpJob.newBuilder().setJob(job).build();
		queueService.sendObject(OsirisQueueService.ftpJobQueueName, stopFtpJob);
		resp = queueService.receiveSelectedObject(OsirisQueueService.ftpJobUpdatesQueueName,
				"messageType IS NULL OR messageType <> 'FTPFileAvailable'");
		assertThat(resp instanceof JobError, is(true));
		resp = queueService.receiveSelectedObject(OsirisQueueService.ftpJobUpdatesQueueName,
				"messageType IS NULL OR messageType <> 'FTPFileAvailable'");
		assertThat(resp instanceof FtpJobStopped, is(true));
		resp = queueService.receiveSelectedObject(OsirisQueueService.ftpJobUpdatesQueueName, "messageType = 'FTPFileAvailable'");
		assertThat(resp instanceof NoMoreJobFtpFilesAvailable, is(true));
	}

	@Test
	public void testFtpJobStop() {
		Job job = Job.newBuilder().setId("test").setIntJobId("1").setServiceId("test").build();
		FtpJobSpec ftpJobSpec = FtpJobSpec.newBuilder()
				.setFtpRootUri("ftp://127.0.0.1:" + fakeFtpServer.getServerControlPort() + "/test_ftp_root").setJob(job)
				.build();
		queueService.sendObject(OsirisQueueService.ftpJobQueueName, ftpJobSpec);
		Object resp = queueService.receiveSelectedObject(OsirisQueueService.ftpJobUpdatesQueueName, "messageType = 'FTPFileAvailable'");
		assertThat(resp instanceof JobFtpFileAvailable, is(true));
		resp = queueService.receiveSelectedObject(OsirisQueueService.ftpJobUpdatesQueueName, "messageType = 'FTPFileAvailable'");
		assertThat(resp instanceof JobFtpFileAvailable, is(true));
		StopFtpJob stopFtpJob = StopFtpJob.newBuilder().setJob(job).build();
		queueService.sendObject(OsirisQueueService.ftpJobQueueName, stopFtpJob);
		resp = queueService.receiveSelectedObject(OsirisQueueService.ftpJobUpdatesQueueName,
				"messageType IS NULL OR messageType <> 'FTPFileAvailable'");
		assertThat(resp instanceof FtpJobStarted, is(true));
		resp = queueService.receiveSelectedObject(OsirisQueueService.ftpJobUpdatesQueueName,
				"messageType IS NULL OR messageType <> 'FTPFileAvailable'");
		assertThat(resp instanceof FtpJobStopped, is(true));
		resp = queueService.receiveSelectedObject(OsirisQueueService.ftpJobUpdatesQueueName, "messageType = 'FTPFileAvailable'");
		assertThat(resp instanceof NoMoreJobFtpFilesAvailable, is(true));
	}

}
