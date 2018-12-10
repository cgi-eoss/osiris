package com.cgi.eoss.osiris.persistence.service;

import com.cgi.eoss.osiris.model.DownloaderCredentials;

public interface DownloaderCredentialsDataService extends
        OsirisEntityDataService<DownloaderCredentials> {
    /**
     * @param host The hostname for which credentials are required.
     * @return The credentials required to download from the given host, or null if no credentials were found for the
     * host.
     */
    DownloaderCredentials getByHost(String host);
}
