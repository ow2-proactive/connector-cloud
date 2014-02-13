package org.ow2.proactive.iaas.metadata;

import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
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
import java.util.Date;

public class MetadataHttpClient {

    private static final int CONNECTION_TIMEOUT = 8000;
    private static final int SO_TIMEOUT = 10000;

    public static final String RM_URL = "rm_url";
    public static final String PROTOCOL = "protocol";
    public static final String ROUTER_ADDRESS = "router_address";
    public static final String ROUTER_PORT = "router_port";
    public static final String CREDENTIALS = "credentials";
    public static final String NODE_SOURCE_NAME = "node_source_name";
    public static final String NODE_NAME = "node_name";
    public static final String TOKEN = "token";

    private static final Logger logger = Logger.getLogger(MetadataHttpClient.class);

    private HttpClient httpClient;
    private URI endpoint;

    public MetadataHttpClient(URI endpoint) throws IOException {
        this.endpoint = endpoint;
        this.httpClient = buildHttpClient();
        checkServerIsUp();
    }

    private void checkServerIsUp() throws IOException {
        JSONObject json = new JSONObject();
        json.put("connected", new Date());
        putMetadata("0.0.0.0", json);
        String connected = getMetadata("0.0.0.0", "connected");
        logger.debug("Connected to metadata server: " + connected);
    }

    public String getMetadata(String ipAddress) throws IOException {
        return executeGet("/server/metadata/" + encode(ipAddress));
    }

    public String getMetadata(String ipAddress, String attribute) throws IOException {
        String entity = getMetadata(ipAddress);
        String value;
        try {
            value = JsonPath.read(entity, "$._source." + attribute);
        } catch (InvalidPathException e) {
            throw new RuntimeException(
                    "Could not get metadata value: " + attribute + " : " + entity, e);
        }
        return value;
    }

    public String putMetadata(String ipAddress, JSONObject content) throws IOException {
        logger.debug(String.format("Putting metadata: '%s' '%s'", ipAddress, content.toJSONString()));
        return executePost("/server/metadata/" + encode(ipAddress), content);
    }

    private synchronized String executePost(String path, JSONObject content) throws IOException {
        HttpPost post = new HttpPost(endpoint.resolve(path));
        post.addHeader("Content-type", "application/json");
        post.setEntity(new StringEntity(content.toJSONString(), "UTF-8"));
        HttpResponse response = httpClient.execute(post);
        String entity = EntityUtils.toString(response.getEntity());
        EntityUtils.consume(response.getEntity());
        return entity;
    }

    private synchronized String executeGet(String path) throws IOException {
        HttpGet get = new HttpGet(endpoint.resolve(path).toString());
        HttpResponse response = httpClient.execute(get);
        String entity = EntityUtils.toString(response.getEntity());
        EntityUtils.consume(response.getEntity());
        return entity;
    }

    private String encode(String ipAddress) {
        return ipAddress.replaceAll("\\.", "_");
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
