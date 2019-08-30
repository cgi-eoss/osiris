package com.cgi.eoss.osiris.harvesters.ftp;

import java.net.URI;
import java.time.Instant;
import java.util.List;

public interface FtpHarvesterService {

    List<FileItem> harvestFiles(URI ftpRootUri, Instant start) throws FtpHarvesterException;
  
    FtpFileMeta getFile(URI fileUri) throws FtpHarvesterException;

    void deleteFile(URI fileUri) throws FtpHarvesterException;
}
