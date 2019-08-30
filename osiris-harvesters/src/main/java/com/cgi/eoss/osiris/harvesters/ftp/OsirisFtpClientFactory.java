package com.cgi.eoss.osiris.harvesters.ftp;

import com.cgi.eoss.osiris.rpc.Credentials;
import com.cgi.eoss.osiris.rpc.GetCredentialsParams;
import com.cgi.eoss.osiris.rpc.OsirisServerClient;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

import javax.security.auth.login.FailedLoginException;

public class OsirisFtpClientFactory implements PooledObjectFactory<FTPClient> {

	private URI ftpUri;
	private OsirisServerClient osirisServerClient;

    public OsirisFtpClientFactory(URI ftpUri, OsirisServerClient osirisServerClient) {
        this.ftpUri = ftpUri;
        this.osirisServerClient = osirisServerClient;
    }

    @Override
    public PooledObject<FTPClient> makeObject() throws Exception {
    	FTPClient ftpClient = "ftps".equals(ftpUri.getScheme()) ? new FTPSClient() : new FTPClient();
        try {
        	configureFtpClient(ftpClient, ftpUri);
        } catch (IOException | FailedLoginException e) {
            try {
            	if (ftpClient.isConnected()) {
            		ftpClient.disconnect();
                }
            } catch (IOException e1) {
                throw new FtpPoolException(e);
            }
            throw new FtpPoolException(e);
        }
        return new DefaultPooledObject<>(ftpClient);
    }
    
    private FTPClient configureFtpClient(FTPClient ftpClient, URI ftpUri) throws IOException, FailedLoginException {
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
    public void destroyObject(PooledObject<FTPClient> p) throws Exception {
        final FTPClient object = p.getObject();
        if(object.isConnected()){
            try {
                object.disconnect();
            } catch (IOException e) {
                throw new FtpPoolException(e);
            }
        }
    }

    @Override
    public boolean validateObject(PooledObject<FTPClient> p) {
        final FTPClient object = p.getObject();
        try {
            return object.isConnected() && object.isAvailable() &&  object.getStatus().length()>=0;
        }catch (Exception e) {
            return false;
        }
    }

    @Override
    public void activateObject(PooledObject<FTPClient> p) throws Exception {
       

    }

    @Override
    public void passivateObject(PooledObject<FTPClient> p) throws Exception {

    }
}
