package com.cgi.eoss.osiris.harvesters.ftp;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.cgi.eoss.osiris.harvesters.HarvestersConfig;
import com.cgi.eoss.osiris.model.DownloaderCredentials;
import com.cgi.eoss.osiris.persistence.service.DownloaderCredentialsDataService;
import com.cgi.eoss.osiris.persistence.service.RpcCredentialsService;
import com.cgi.eoss.osiris.rpc.CredentialsServiceGrpc;
import com.cgi.eoss.osiris.rpc.OsirisServerClient;
import com.cgi.eoss.osiris.rpc.catalogue.CatalogueServiceGrpc;
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

import java.io.IOException;
import java.net.URI;
import java.util.List;

import javax.security.auth.login.FailedLoginException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {HarvestersConfig.class})
@TestPropertySource("classpath:test-harvesters.properties")
public class TestFtpHarvesterService {
	
	@Autowired
	private FtpHarvesterService ftpHarvesterService;
	
	private FakeFtpServer fakeFtpServer;
	
	@Mock
    private DownloaderCredentialsDataService credentialsDataService;
	
	@MockBean
    private OsirisServerClient osirisServerClient;
	
	private Server server;
	
	@Before
	public void setUp() throws Exception {
		fakeFtpServer = buildFakeFtpServer();
        fakeFtpServer.start();
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
	
	private FakeFtpServer buildFakeFtpServer() throws Exception {
        FakeFtpServer ftpServer = new FakeFtpServer();
        ftpServer.setServerControlPort(0);
        ftpServer.addUserAccount(new UserAccount("ftpuser", "ftppass", "/"));
        org.mockftpserver.fake.filesystem.FileSystem fileSystem = new UnixFakeFileSystem();
        fileSystem.add(new DirectoryEntry("/test_ftp_root"));
        fileSystem.add(new DirectoryEntry("/out1"));
        FileEntry testFile = new FileEntry("/test_ftp_root/out1/file1.txt", "contents");
        FileEntry testFile2 = new FileEntry("/test_ftp_root/out1/file2.txt", "contents2");
        fileSystem.add(testFile);
        fileSystem.add(testFile2);
        ftpServer.setFileSystem(fileSystem);
        return ftpServer;
    }
	
	@After
	public void tearDown() {
		server.shutdown();
	}
	
	@Test
	public void testHarvestFiles() throws FailedLoginException, IOException {
		URI ftpRootUri = URI.create("ftp://127.0.0.1:" + fakeFtpServer.getServerControlPort() + "/test_ftp_root");
		List<FileItem> results = ftpHarvesterService.harvestFiles(ftpRootUri, null);
		assertThat(results.size(), is(2)); 
	}
	

}
