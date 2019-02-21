package com.cgi.eoss.osiris.io.download;

import com.cgi.eoss.osiris.io.ServiceIoException;
import com.cgi.eoss.osiris.rpc.Credentials;
import com.cgi.eoss.osiris.rpc.GetCredentialsParams;
import com.cgi.eoss.osiris.rpc.OsirisServerClient;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;
import org.jooq.lambda.Unchecked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Log4j2
public class FtpDownloader implements Downloader {
    private static final int CONNECT_TIMEOUT = 2000;
    private static final String FTPS_SCHEME = "ftps";

    private final OsirisServerClient osirisServerClient;
    private final DownloaderFacade downloaderFacade;

    @Autowired
    public FtpDownloader(OsirisServerClient osirisServerClient, DownloaderFacade downloaderFacade) {
        this.osirisServerClient = osirisServerClient;
        this.downloaderFacade = downloaderFacade;
    }

    @PostConstruct
    public void postConstruct() {
        downloaderFacade.registerDownloader(this);
    }

    @PreDestroy
    public void preDestroy() {
        downloaderFacade.unregisterDownloader(this);
    }

    @Override
    public Set<String> getProtocols() {
        return ImmutableSet.of("ftp", "ftps");
    }

    @Override
    public Path download(Path targetDir, URI uri) throws IOException {
        LOG.info("Downloading: {}", uri);

        FTPClient ftpClient = FTPS_SCHEME.equals(uri.getScheme()) ? new FTPSClient() : new FTPClient();
        ftpClient.setConnectTimeout(CONNECT_TIMEOUT);

        try {
            if (uri.getPort() == -1) {
                ftpClient.connect(uri.getHost());
            } else {
                ftpClient.connect(uri.getHost(), uri.getPort());
            }
            Credentials creds = osirisServerClient.credentialsServiceBlockingStub().getCredentials(GetCredentialsParams.newBuilder().setHost(uri.getHost()).build());
            if (creds.getType() == Credentials.Type.BASIC) {
                ftpClient.login(creds.getUsername(), creds.getPassword());
            }
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            String rootDir = uri.getPath();
            FTPFile[] ftpFile = ftpClient.listFiles(rootDir);
            if (ftpFile[0].isDirectory()) {
                downloadRecursive(ftpClient, rootDir, "", targetDir);
                return targetDir;
            } else {
                Path outputFile = targetDir.resolve(Paths.get(rootDir).getFileName().toString());
                downloadFile(ftpClient, rootDir, outputFile);
                return outputFile;
            }
        } finally {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
        }
    }

    private void downloadRecursive(FTPClient ftpClient, String rootDir, String currentRelativeDir, Path targetDir) throws IOException {
        String currentFtpDir = appendSegmentToPath(rootDir, currentRelativeDir);
        Path currentTargetDir = Files.createDirectories(targetDir.resolve(currentRelativeDir));

        FTPFile[] currentFiles = ftpClient.listFiles(currentFtpDir);

        Map<Boolean, List<FTPFile>> currentPathEntries = Arrays.stream(currentFiles)
                .filter(f -> !f.getName().equals(".") && !f.getName().equals(".."))
                .collect(Collectors.partitioningBy(FTPFile::isDirectory));

        currentPathEntries.get(true).forEach(Unchecked.consumer(f -> downloadRecursive(ftpClient, rootDir, appendSegmentToPath(currentRelativeDir, f.getName()), targetDir)));
        currentPathEntries.get(false).forEach(Unchecked.consumer(f -> downloadFile(ftpClient, appendSegmentToPath(appendSegmentToPath(rootDir, currentRelativeDir), f.getName()), currentTargetDir.resolve(f.getName()))));
    }

    private void downloadFile(FTPClient ftpClient, String ftpPath, Path targetFile) throws IOException {
        try (OutputStream os = new BufferedOutputStream(Files.newOutputStream(targetFile))) {
            boolean success = ftpClient.retrieveFile(ftpPath, os);
            if (!success) {
                ServiceIoException cause = new ServiceIoException("FTP download error: " + ftpClient.getReplyString());
                throw new ServiceIoException("Failed to download via FTP: " + ftpClient.getPassiveHost() + ftpPath, cause);
            }
            LOG.info("Successfully downloaded via FTP: {}", targetFile);
        }
    }

    private String appendSegmentToPath(String path, String segment) {
        if (Strings.isNullOrEmpty(path)) {
            return segment;
        }
        if (Strings.isNullOrEmpty(segment)) {
            return path;
        }
        if (path.charAt(path.length() - 1) == '/' || segment.charAt(0) == '/') {
            return path + segment;
        }
        return path + "/" + segment;
    }
}
