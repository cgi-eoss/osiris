package com.cgi.eoss.osiris.harvesters.ftp;

import com.cgi.eoss.osiris.rpc.Credentials;
import com.cgi.eoss.osiris.rpc.GetCredentialsParams;
import com.cgi.eoss.osiris.rpc.OsirisServerClient;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.security.auth.login.FailedLoginException;

@Component
@Log4j2
public class FtpHarvesterServiceImpl implements FtpHarvesterService{
    
	
	private OsirisServerClient osirisServerClient;
	private Set<String> excludedExtensions;

	@Autowired
	public FtpHarvesterServiceImpl(OsirisServerClient osirisServerClient, @Value("#{'${osiris.harvesters.excludedExtensions:.temp}'.split(',')}") Set<String> excludedExtensions) {
		this.osirisServerClient = osirisServerClient;
		this.excludedExtensions = excludedExtensions;
	}
	
	@Override
    public List<FileItem> harvestFiles(URI ftpRootUri, Instant start) throws IOException, FailedLoginException {
    	FTPClient ftpClient = getFtpClient(ftpRootUri);
    	String ftpRoot = ftpRootUri.getPath();
        if(!ftpRoot.endsWith("/")) {
            ftpRoot = ftpRoot + "/";
        }
        List<FileItem> files = harvestDirectory(ftpClient, ftpRootUri, ftpRoot, start);
        ftpClient.disconnect();
        return files;
    }
    
    private List<FileItem> harvestDirectory(FTPClient ftpClient, URI ftpRootUri, String directory, Instant start) throws IOException {
        boolean cwd = ftpClient.changeWorkingDirectory(directory);
        if (!cwd) {
            throw new IOException("Cannot change FTP directory");
        }
        
        FTPFile[] ftpFiles;
        
        if (ftpClient.listHelp().contains("MLSD")) {
        	ftpFiles = ftpClient.mlistDir(null, new TimeAndExtensionFtpFileFilter(start, excludedExtensions));
        }
        else {
        	ftpFiles = ftpClient.listFiles(null, new TimeAndExtensionFtpFileFilter(start, excludedExtensions));
        }
        
        List<FileItem> result = new ArrayList<>();
        for (FTPFile ftpFile: ftpFiles) {
        	if (ftpFile.isDirectory()){
                result.addAll(harvestDirectory(ftpClient, ftpRootUri, directory + ftpFile.getName() + "/", start));
            }
        	else if (ftpFile.isFile()) {
            	result.add(buildFileItem(ftpRootUri, directory, ftpFile));
            }
        }
        
        return result;
    }
    
    
    private FileItem buildFileItem(URI ftpUri, String directory, FTPFile ftpFile) {
    	return new FileItem(buildFileUri(ftpUri, directory + ftpFile.getName()).toString(), ftpFile.getTimestamp().toInstant());
   }
    
    private URI buildFileUri(URI ftpUri, String fullFilePath) {
    	StringBuilder builder = new StringBuilder();
    	builder.append(String.format("%s://%s", ftpUri.getScheme(), ftpUri.getHost()));
    	if(ftpUri.getPort() != -1) {
    		builder.append(String.format(":%d", ftpUri.getPort()));
        }
    	builder.append(fullFilePath);
    	return URI.create(builder.toString());
   }

	private FTPClient getFtpClient(URI ftpUri) throws IOException, FailedLoginException {
		FTPClient ftpClient = "ftps".equals(ftpUri.getScheme()) ? new FTPSClient() : new FTPClient();
        if(ftpUri.getPort() != -1) {
        	ftpClient.connect(ftpUri.getHost(), ftpUri.getPort());
        }
        else {
        	ftpClient.connect(ftpUri.getHost());
        }
        Map<String, String> params = URLEncodedUtils.parse(ftpUri, "UTF-8").stream().collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
        String timezone = params.getOrDefault("timezone", "UTC");
        
        FTPClientConfig conf = new FTPClientConfig();
        conf.setServerTimeZoneId(timezone);
        ftpClient.configure(conf);
        
        Credentials creds = osirisServerClient.credentialsServiceBlockingStub().getCredentials(GetCredentialsParams.newBuilder().setHost(ftpUri.getHost()).build());
        if (creds.getType() != Credentials.Type.BASIC || !ftpClient.login(creds.getUsername(), creds.getPassword())) {
        	throw new FailedLoginException();
        }
        ftpClient.enterLocalPassiveMode();
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        return ftpClient; 
    }

    @Override
    public FtpFileMeta getFile(URI fileUri) throws IOException, FailedLoginException {
        LOG.info("Received request for file {}", fileUri);
    	
        Path fullFilePath = Paths.get(fileUri.getPath());
        LOG.debug("Full path {}", fullFilePath.toString());
    	Path fileName = fullFilePath.getFileName();
    	LOG.debug("File name {}", fileName.toString());
    	Path workDir = fullFilePath.getParent();
    	LOG.debug("Work dir {}", workDir.toString());
    	FTPClient ftpClient = getFtpClient(fileUri);
        boolean changeWD = ftpClient.changeWorkingDirectory(workDir.toString());
        if (!changeWD) {
            throw new IOException("Cannot change working directory");
        }
        FTPFile[] ftpFiles = ftpClient.listFiles(fileName.toString());
        if (ftpFiles.length == 0) {
            throw new FileNotFoundException("Cannot find file");
        }
        FTPFile file = ftpFiles[0];
        return FtpFileMeta.builder()
        		.fileName(file.getName())
        		.fileSize(file.getSize())
        		.fileInputStream(ftpClient.retrieveFileStream(fileName.toString()))
        		.build();
    }


    @Override
    public void deleteFile(URI fileUri) throws IOException, FailedLoginException {
    	FTPClient ftpClient = getFtpClient(fileUri);
    	Path fullFilePath = Paths.get(fileUri.getPath());
        Path fileName = fullFilePath.getFileName();
        Path workDir = fullFilePath.getParent();
        boolean changeWD = ftpClient.changeWorkingDirectory(workDir.toString());
        if (!changeWD) {
            throw new IOException("Cannot change working directory");
        }
        FTPFile[] ftpFiles = ftpClient.listFiles(fileName.toString());
        if (ftpFiles.length == 0) {
            throw new FileNotFoundException ("Cannot find file");
        }
        ftpClient.deleteFile(fileName.toString());
        ftpClient.disconnect();
        LOG.info("Deleted file {}", fileUri);
    	
    }
    

}
