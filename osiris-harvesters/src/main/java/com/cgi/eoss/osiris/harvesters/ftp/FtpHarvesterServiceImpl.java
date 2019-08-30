package com.cgi.eoss.osiris.harvesters.ftp;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@Log4j2
public class FtpHarvesterServiceImpl implements FtpHarvesterService{
    
	
	private Set<String> excludedExtensions;
	private FtpClientPools ftpClientPools;
	
	@Autowired
	public FtpHarvesterServiceImpl(FtpClientPools ftpClientPools, @Value("#{'${osiris.harvesters.excludedExtensions:.temp}'.split(',')}") Set<String> excludedExtensions) {
		this.excludedExtensions = excludedExtensions;
		this.ftpClientPools = ftpClientPools;
	}
	
	@Override
    public List<FileItem> harvestFiles(URI ftpRootUri, Instant start) throws FtpHarvesterException {
    	FTPClient ftpClient = ftpClientPools.getFtpClient(ftpRootUri);
    	LOG.debug("Got client from pool - is connected: {}", ftpClient.isConnected());
    	String ftpRoot = ftpRootUri.getPath();
        if(!ftpRoot.endsWith("/")) {
            ftpRoot = ftpRoot + "/";
        }
		try {
			 return harvestDirectory(ftpClient, ftpRootUri, ftpRoot, start);
		} catch (IOException e) {
			throw new FtpHarvesterException(e);
		}
		finally {
			ftpClientPools.releaseFtpClient(ftpRootUri, ftpClient);
		}
       
    }
    
    private List<FileItem> harvestDirectory(FTPClient ftpClient, URI ftpRootUri, String directory, Instant start) throws IOException {
    	 LOG.debug("Changing ftp directory: {}", directory);
     	boolean cwd = ftpClient.changeWorkingDirectory(directory);
        if (!cwd) {
        	LOG.error("Could not change root directory: {}", directory);
        	throw new IOException("Cannot change FTP directory");
        }
       	LOG.trace("Reply code and message: {} {} ", ftpClient.getReplyCode(), ftpClient.getReplyString());
        FTPFile[] ftpFiles;
        LOG.debug("Listing ftp directory: {}", directory);
        String helpString = ftpClient.listHelp();
        LOG.debug("Help: {}", helpString);
        if (helpString.contains("MLSD")) {
    		ftpFiles = ftpClient.mlistDir(null, new TimeAndExtensionFtpFileFilter(start, excludedExtensions));
        }
        else {
        	ftpFiles = ftpClient.listFiles(null, new TimeAndExtensionFtpFileFilter(start, excludedExtensions));
        }
    	LOG.trace("Reply code and message: {} {} ", ftpClient.getReplyCode(), ftpClient.getReplyString());
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

	

    @Override
    public FtpFileMeta getFile(URI fileUri) throws FtpHarvesterException {
    	FTPClient ftpClient = ftpClientPools.getFtpClient(fileUri);
		try {
			return getFileFromFtp(ftpClient, fileUri);
		} catch (IOException e) {
			ftpClientPools.releaseFtpClient(fileUri, ftpClient);
			throw new FtpHarvesterException(e);
		}
    }

	private FtpFileMeta getFileFromFtp(FTPClient ftpClient, URI fileUri) throws IOException, FtpHarvesterException {
		LOG.info("Received request for file {}", fileUri);
    	
        Path fullFilePath = Paths.get(fileUri.getPath());
        LOG.debug("Full path {}", fullFilePath.toString());
    	Path fileName = fullFilePath.getFileName();
    	LOG.debug("File name {}", fileName.toString());
    	Path workDir = fullFilePath.getParent();
    	LOG.debug("Work dir {}", workDir.toString());
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
        		.fileInputStream(new FilterInputStream(ftpClient.retrieveFileStream(fileName.toString())) {
        			@Override
        			public void close() throws IOException {
        				ftpClient.completePendingCommand();
        				ftpClientPools.releaseFtpClient(fileUri, ftpClient);
        			}
        		})
        		.build();
	}


	@Override
    public void deleteFile(URI fileUri) throws FtpHarvesterException {
		FTPClient ftpClient = ftpClientPools.getFtpClient(fileUri);
		try {
			deleteFileFromFtp(ftpClient, fileUri);
		} catch (IOException e) {
			throw new FtpHarvesterException(e);
		}
		finally {
			ftpClientPools.releaseFtpClient(fileUri, ftpClient);
		}
	}
    
	private void deleteFileFromFtp(FTPClient ftpClient, URI fileUri) throws IOException {
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
        
        LOG.info("Deleted file {}", fileUri);
    	
    }
    

}
