package org.ow2.proactive.iaas.nova;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.sasl.AuthenticationException;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.ow2.proactive.iaas.IaasMonitoringApi;
import org.ow2.proactive.iaas.IaasApi;
import org.ow2.proactive.iaas.IaasApiFactory;
import org.ow2.proactive.iaas.IaasInstance;
import org.ow2.proactive.iaas.monitoring.IaasMonitoringException;

import com.jayway.jsonpath.JsonPath;


public class NovaAPI implements IaasApi, IaasMonitoringApi {

    private static final Logger logger = Logger.getLogger(NovaAPI.class);

    private static Map<Integer, NovaAPI> instances;

    private static String host_prop_filter_criteria = "$.host[*].resource[?(@.project == '($FILTER_TOKEN$)')]";

    private long created;
    private HttpClient httpClient;
    private URI endpoint;
    private String sessionId;
    private String novaUri;

    // ////////////////////////
    // NOVA FACTORY
    // ////////////////////////

    public static IaasApi getNovaAPI(Map<String, String> args) throws URISyntaxException,
            AuthenticationException {
        return getNovaAPI(args.get(NovaAPIConstants.ApiParameters.USER_NAME),
                args.get(NovaAPIConstants.ApiParameters.PASSWORD),
                args.get(NovaAPIConstants.ApiParameters.TENANT_NAME),
                new URI(args.get(NovaAPIConstants.ApiParameters.API_URL))
        /* null */);
    }

    public static synchronized NovaAPI getNovaAPI(String username, String password, String tenantName,
            URI endpoint) throws AuthenticationException {
        if (instances == null) {
            instances = new HashMap<Integer, NovaAPI>();
        }
        int hash = (username + password + tenantName).hashCode();
        NovaAPI instance = instances.get(hash);
        if (instance == null || !isValid(instance.created)) {
            try {
                instances.remove(hash);
                instance = new NovaAPI(username, password, tenantName, endpoint);
            } catch (Throwable t) {
                throw new AuthenticationException("Failed to authenticate to " + endpoint, t);
            }
            instances.put(hash, instance);
        }
        return instance;
    }

    /**
     * SessionId provided by OpenStack are valid for 24 hours. So we have to
     * check is the cached one is still valid.
     * 
     * @param created
     * @return
     */
    private static boolean isValid(long created) {
        final int ALMOST_ONE_DAY = 1000 * (3600 * 24 - 10); // in ms, 1 day
                                                            // minus 10 seconds

        return (System.currentTimeMillis() - created < ALMOST_ONE_DAY);
    }

    private NovaAPI(String username, String password, String tenantName, URI endpoint) throws IOException {
        logger.debug("Initializing Nova API for Nova infrastructure...");
        logger.debug(String.format("username='%s', tenant='%s', endpoint='%s'", username, tenantName,
                endpoint));
        this.created = System.currentTimeMillis();
        this.endpoint = endpoint;
        this.httpClient = new DefaultHttpClient();

        authenticate(username, password, tenantName);
    }

    public void test() throws Exception {
        String outp;
        outp = filter("servers/detail", "$.servers[*].status");
        System.out.println("STATUS = " + outp);

        // It lists all the hosts with all the services: conductor, compute,
        // cert, network, scheduler, consoleauth.
        outp = filter("os-hosts", "$.hosts[*]");
        System.out.println("HOSTS = " + outp);

        outp = filter("os-hosts", "$.hosts[?(@.service=='compute')].host_name");
        System.out.println("HOSTS COMPUTE = " + outp);
        // outp = api.filter("servers/detail", "$.servers[*].status");
        // System.out.println("STATUS = " + outp);
    }

    // ////////////////////////
    // NOVA COMMANDS
    // ////////////////////////

    private void authenticate(String username, String password, String tenant) throws IOException {
        // Retrieve a token id to list tenants
        JSONObject jsonCreds = new JSONObject();
        jsonCreds.put(NovaAPIConstants.ApiParameters.USER_NAME, username);
        jsonCreds.put(NovaAPIConstants.ApiParameters.PASSWORD, password);
        JSONObject jsonAuth = new JSONObject();
        jsonAuth.put(NovaAPIConstants.ApiParameters.PASSWORD_CREDENTIALS, jsonCreds);
        jsonAuth.put(NovaAPIConstants.ApiParameters.TENANT_NAME, tenant);
        JSONObject jsonReq = new JSONObject();
        jsonReq.put(NovaAPIConstants.ApiParameters.AUTH, jsonAuth);

        // Here we cannot use post() yet because sessionId is not set
        HttpPost post = new HttpPost(endpoint + "/tokens");
        post.addHeader("Content-type", "application/json");
        post.setEntity(new StringEntity(jsonReq.toString(), "UTF-8"));

        HttpResponse response = httpClient.execute(post);
        String entity = EntityUtils.toString(response.getEntity());

        logger.debug(entity);

        // Retrieve useful information from this response
        sessionId = JsonPath.read(entity, "$.access.token.id");
        try {
            novaUri = JsonPath.read(entity,
                    "$.access.serviceCatalog[?(@.type=='compute')].endpoints[0].publicURL");
            logger.info("Compute url is " + novaUri);
        } catch (RuntimeException ex) {
            throw new RuntimeException("Cannot parse service catalog - check your tenant name");
        }
    }

    public void listAvailableImages() throws ClientProtocolException, IOException {

        // JSONObject jReq = new JSONObject();
        // jReq.put("server", jServer);

        HttpResponse response = get("/servers/detail");
        String entity = EntityUtils.toString(response.getEntity());
        logger.debug(entity);

        response = get("/flavors/detail");
        entity = EntityUtils.toString(response.getEntity());
        logger.debug(entity);

    }

    public String createServer(String name, String imageRef, String flavorRef, String userData,
            Map<String, String> metaData) throws ClientProtocolException, IOException {

        JSONObject jsonMetaData = new JSONObject();
        jsonMetaData.putAll(metaData);

        JSONObject jServer = new JSONObject();
        jServer.put(NovaAPIConstants.InstanceParameters.NAME, name);
        jServer.put(NovaAPIConstants.InstanceParameters.IMAGE_REF, imageRef);
        jServer.put(NovaAPIConstants.InstanceParameters.FLAVOR_REF, flavorRef);
        jServer.put(NovaAPIConstants.InstanceParameters.META_DATA, jsonMetaData);
        jServer.put(NovaAPIConstants.InstanceParameters.USER_DATA, userData);

        JSONObject jReq = new JSONObject();
        jReq.put("server", jServer);

        logger.debug(jReq.toJSONString());

        HttpResponse response = post("/servers", jReq);
        if (response.getStatusLine().getStatusCode() != 200) {
            // TODO throw an error 
        }
        String entity = EntityUtils.toString(response.getEntity());
        logger.debug(entity);

        String serverId = JsonPath.read(entity, "$.server.id");
        return serverId;
    }

    public boolean rebootServer(String serverId, String method) throws ClientProtocolException, IOException {
        JSONObject jReboot = new JSONObject();
        jReboot.put("type", method.toUpperCase());
        JSONObject jReq = new JSONObject();
        jReq.put("reboot", jReboot);

        HttpResponse response = post("/servers/" + serverId + "/action", jReq);
        response.getEntity().consumeContent();
        return response.getStatusLine().getStatusCode() == 202;
    }

    public boolean deleteServer(String serverId) throws ClientProtocolException, IOException {
        HttpResponse response = delete("/servers/" + serverId);
        logger.debug(response.getEntity());
        return response.getStatusLine().getStatusCode() == 204;
    }

    public String filter(String ressources, String criteria) throws ClientProtocolException, IOException {
        HttpResponse response = get("/" + ressources);
        String entity = EntityUtils.toString(response.getEntity());
        return JsonPath.read(entity, criteria).toString();
    }

    // ////////////////////////
    // GENERATE HTTP REQUESTS
    // ////////////////////////

    private HttpResponse post(String path, JSONObject content) throws ClientProtocolException, IOException {
        HttpPost post = new HttpPost(novaUri + path);
        post.addHeader("X-Auth-Token", sessionId);
        post.addHeader("Content-type", "application/json");
        post.setEntity(new StringEntity(content.toString(), "UTF-8"));

        return httpClient.execute(post);
    }

    private HttpResponse get(String path) throws ClientProtocolException, IOException {
        HttpGet get = new HttpGet(novaUri + path);
        get.addHeader("X-Auth-Token", sessionId);

        return httpClient.execute(get);
    }

    private HttpResponse delete(String path) throws ClientProtocolException, IOException {
        HttpDelete del = new HttpDelete(novaUri + path);
        del.addHeader("X-Auth-Token", sessionId);

        return httpClient.execute(del);
    }

    @Override
    public IaasInstance startInstance(Map<String, String> arguments) throws Exception {

        Map<String, String> metaData = Collections.emptyMap();

        String userData = "";
        if (arguments.containsKey(NovaAPIConstants.InstanceParameters.USER_DATA)) {
            userData = arguments.get(NovaAPIConstants.InstanceParameters.USER_DATA);
            userData = new String(Base64.encodeBase64(userData.getBytes()));
        }

        return new IaasInstance(createServer(arguments.get(NovaAPIConstants.InstanceParameters.NAME),
                arguments.get(NovaAPIConstants.InstanceParameters.IMAGE_REF),
                arguments.get(NovaAPIConstants.InstanceParameters.FLAVOR_REF), userData, metaData));
    }

    @Override
    public void stopInstance(IaasInstance instance) throws Exception {
        deleteServer(instance.getInstanceId());
    }

    @Override
    public boolean isInstanceStarted(IaasInstance instance) throws Exception {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public String getName() {
        return IaasApiFactory.IaasProvider.NOVA.name();
    }

    public class NovaAPIConstants {
        public class ApiParameters {
            static final String API_URL = "apiurl";
            static final String AUTH = "auth";
            static final String USER_NAME = "username";
            static final String PASSWORD = "password";
            static final String PASSWORD_CREDENTIALS = "passwordCredentials";
            static final String TENANT_NAME = "tenantName";
        }

        public class InstanceParameters {
            public static final String NAME = "name";
            public static final String TENANT_NAME = "";
            public static final String IMAGE_REF = "imageRef";
            public static final String FLAVOR_REF = "flavorRef";
            public static final String META_DATA = "metadata";
            public static final String USER_DATA = "user_data";
        }
    }

    @Override
    public void disconnect() throws Exception {
    }

    /*
     * IaaSMonitoringApi implementation for Nova
     */

    @Override
    public String[] getHosts() throws IaasMonitoringException {
        try {
            HttpResponse response = get("/os-hosts");
            String entity = EntityUtils.toString(response.getEntity());
            JSONArray array = JsonPath.read(entity, "$.hosts[?(@.service=='compute')].host_name");
            String[] stringArray = Arrays.copyOf(array.toArray(), array.size(), String[].class);
            return stringArray;
        } catch (ClientProtocolException e) {
            throw new IaasMonitoringException(e);
        } catch (IOException e) {
            throw new IaasMonitoringException(e);
        }
    }

    @Override
    public String[] getVMs() throws IaasMonitoringException {
        try {
            HttpResponse response = get("/servers");
            String entity = EntityUtils.toString(response.getEntity());
            JSONArray array = JsonPath.read(entity, "$.servers[*].name");
            String[] stringArray = Arrays.copyOf(array.toArray(), array.size(), String[].class);
            return stringArray;
        } catch (ClientProtocolException e) {
            throw new IaasMonitoringException(e);
        } catch (IOException e) {
            throw new IaasMonitoringException(e);
        }
    }

    @Override
    public String[] getVMs(String hostId) throws IaasMonitoringException {
        try {
            HttpResponse response = get("/servers?host=" + hostId);
            String entity = EntityUtils.toString(response.getEntity());
            JSONArray array = JsonPath.read(entity, "$.servers[*].name");
            String[] stringArray = Arrays.copyOf(array.toArray(), array.size(), String[].class);
            return stringArray;
        } catch (ClientProtocolException e) {
            throw new IaasMonitoringException(e);
        } catch (IOException e) {
            throw new IaasMonitoringException(e);
        }
    }

    @Override
    public Map<String, String> getHostProperties(String hostId) throws IaasMonitoringException {
        try {
            HttpResponse response = get("/os-hosts/" + hostId);
            String entity = EntityUtils.toString(response.getEntity());
            Map<String, String> properties = new HashMap<String, String>();
            addProperties(entity, "total", properties);
            addProperties(entity, "used_now", properties);
            addProperties(entity, "used_max", properties);
            return properties;
        } catch (ClientProtocolException e) {
            throw new IaasMonitoringException(e);
        } catch (IOException e) {
            throw new IaasMonitoringException(e);
        }
    }

    @Override
    public Map<String, String> getVMProperties(String vmId) throws IaasMonitoringException {
        try {
            HttpResponse response = get("/servers/detail?name=" + vmId);
            String entity = EntityUtils.toString(response.getEntity());
            JSONObject resource = (JSONObject) ((List<Object>) JsonPath.read(entity, "$.servers[*]")).get(0);
            Map<String, String> properties = new HashMap<String, String>();
            properties.put("status", "" + resource.get("status"));
            return properties;
        } catch (ClientProtocolException e) {
            throw new IaasMonitoringException(e);
        } catch (IOException e) {
            throw new IaasMonitoringException(e);
        }
    }

    private void addProperties(String entity, String filterToken, Map<String, String> properties) {
        JSONObject resource = (JSONObject) ((List<Object>) JsonPath.read(entity,
                host_prop_filter_criteria.replace("$FILTER_TOKEN$", filterToken))).get(0);
        properties.put("cpu.cores." + filterToken, "" + resource.get("cpu"));
        properties.put("memory." + filterToken, "" + resource.get("memory_mb"));
        properties.put("storage." + filterToken, "" + resource.get("disk_gb"));
    }

    @Override
    public Map<String, Object> getVendorDetails() throws IaasMonitoringException {
        return (Map<String, Object>) Collections.EMPTY_MAP;
    }

}
