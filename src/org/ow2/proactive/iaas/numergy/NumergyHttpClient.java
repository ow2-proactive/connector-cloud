package org.ow2.proactive.iaas.numergy;

import net.minidev.json.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.URI;

public class NumergyHttpClient {

    private static final int CONNECTION_TIMEOUT = 8000;
    private static final int SO_TIMEOUT = 10000;

    private static final Logger logger = Logger.getLogger(NumergyHttpClient.class);

    private long created;
    private HttpClient httpClient;
    private URI endpoint;
    private String sessionId;
    private String computeUri;

    private String accessKey;
    private String secretKey;
    private String tenantId;

    public NumergyHttpClient(String accessKey, String secretKey, String tenantId, URI endpoint) throws IOException {
        this.endpoint = endpoint;
        this.created = System.currentTimeMillis();
        this.httpClient = buildHttpClient();

        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.tenantId = tenantId;

        renewSessionId();
    }

    private void renewSessionId() throws IOException {
        String entity = executeAuthenticationPost(accessKey, secretKey, tenantId);
        this.sessionId = NumergyJsonHelper.getSessionIdFromJson(entity);
        this.computeUri = getComputeUri();
    }

    private String getComputeUri() {
        return endpoint + "/" + tenantId;
    }

    private boolean isValid() {
        final int ALMOST_ONE_HOUR = 1000 * (3600 - 10);
        return (System.currentTimeMillis() - created < ALMOST_ONE_HOUR);
    }

    private void reconnectIfNeeded() {
        if (!isValid())
            try {
                renewSessionId();
            } catch (IOException e) {
                logger.warn("Could not renew SessionId after it expired.", e);
            }
    }

    public synchronized String executePost(String path, JSONObject content) throws IOException {

        reconnectIfNeeded();

        HttpPost post = new HttpPost(computeUri + path);
        post.addHeader("X-Auth-Token", sessionId);
        post.addHeader("Content-type", "application/json");
        post.setEntity(new StringEntity(content.toJSONString(), "UTF-8"));
        HttpResponse response = httpClient.execute(post);
        String entity = EntityUtils.toString(response.getEntity());
        EntityUtils.consume(response.getEntity());
        return entity;
    }

    public synchronized String executeGet(String path) throws IOException {

        reconnectIfNeeded();

        HttpGet get = new HttpGet(computeUri + path);
        get.addHeader("X-Auth-Token", sessionId);
        HttpResponse response = httpClient.execute(get);
        String entity = EntityUtils.toString(response.getEntity());
        EntityUtils.consume(response.getEntity());
        return entity;
    }

    public synchronized String executeDelete(String path) throws IOException {

        reconnectIfNeeded();

        HttpDelete del = new HttpDelete(computeUri + path);
        del.addHeader("X-Auth-Token", sessionId);
        HttpResponse response = httpClient.execute(del);
        String entity = EntityUtils.toString(response.getEntity());
        EntityUtils.consume(response.getEntity());
        return entity;
    }

    private String executeAuthenticationPost(String accessKey, String secretKey, String tenantId)
            throws IOException {
        JSONObject jsonReq = NumergyJsonHelper.buildAuthenticationJson(accessKey, secretKey, tenantId);
        HttpPost post = new HttpPost(endpoint + "/tokens");
        post.addHeader("Content-type", "application/json");
        post.setEntity(new StringEntity(jsonReq.toJSONString(), "UTF-8"));
        HttpResponse response = httpClient.execute(post);
        String entity = EntityUtils.toString(response.getEntity());
        return entity;
    }

    private HttpClient buildHttpClient() {
        HttpClient client = new DefaultHttpClient();
        ClientConnectionManager mgr = client.getConnectionManager();
        HttpParams params = client.getParams();
        HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, SO_TIMEOUT);
        ClientConnectionManager cm = new ThreadSafeClientConnManager(params, mgr.getSchemeRegistry());
        return new DefaultHttpClient(cm, params);
    }

}
