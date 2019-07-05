package com.cgi.eoss.osiris.harvesters.ftp;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.List;

import javax.security.auth.login.FailedLoginException;

public interface FtpHarvesterService {

    List<String> harvestFiles(URI ftpRootUri, Instant start) throws IOException, FailedLoginException;
  
    FtpFileMeta getFile(URI fileUri) throws IOException, FailedLoginException;

    void deleteFile(URI fileUri) throws IOException, FailedLoginException;
}
