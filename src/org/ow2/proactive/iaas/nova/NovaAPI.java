package org.ow2.proactive.iaas.nova;



import org.ow2.proactive.iaas.IaasApi;
import org.ow2.proactive.iaas.IaasVM;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.security.sasl.AuthenticationException;

import net.minidev.json.JSONObject;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.jayway.jsonpath.JsonPath;


public class NovaAPI implements IaasApi {

    private static Map<Integer, NovaAPI> instances;

    private long created;
    private HttpClient httpClient;
    private URI endpoint;
    private String sessionId;
    private String novaUri;

    // ////////////////////////
    // NOVA FACTORY
    // ////////////////////////

    public static IaasApi getNovaAPI(Map<String, String> args) throws URISyntaxException, AuthenticationException {
        return getNovaAPI(args.get("username"),
                args.get("password"),
                args.get("tenantName"),
                new URI(args.get("endpoint")));
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
        final int ALMOST_ONE_DAY = 1000 * (3600 * 24 - 10); // in ms, 1 day minus 10 seconds

        return (System.currentTimeMillis() - created < ALMOST_ONE_DAY);
    }

    private NovaAPI(String username, String password, String tenantName, URI endpoint) throws IOException {
        this.created = System.currentTimeMillis();
        this.endpoint = endpoint;
        this.httpClient = new DefaultHttpClient();
        authenticate(username, password, tenantName);
    }

    // ////////////////////////
    // NOVA COMMANDS
    // ////////////////////////

    private void authenticate(String username, String password, String tenant) throws IOException {
        // Retrieve a token id to list tenants
        JSONObject jsonCreds = new JSONObject();
        jsonCreds.put("username", username);
        jsonCreds.put("password", password);
        JSONObject jsonAuth = new JSONObject();
        jsonAuth.put("passwordCredentials", jsonCreds);
        jsonAuth.put("tenantName", tenant);
        JSONObject jsonReq = new JSONObject();
        jsonReq.put("auth", jsonAuth);

        // Here we cannot use post() yet because sessionId is not set
        HttpPost post = new HttpPost(endpoint);
        post.addHeader("Content-type", "application/json");
        post.setEntity(new StringEntity(jsonReq.toString(), "UTF-8"));

        HttpResponse response = httpClient.execute(post);
        String entity = EntityUtils.toString(response.getEntity());

        System.out.println(entity);
        
        // Retrieve useful information from this response
        sessionId = JsonPath.read(entity, "$.access.token.id");
        novaUri = JsonPath.read(entity, "$.access.serviceCatalog[?(@.name=='Compute')].endpoints[0].publicURL");
   
    
    
    }
    
    
    
    public void listAvailableImages() throws ClientProtocolException, IOException{
    	
        //JSONObject jReq = new JSONObject();
        //jReq.put("server", jServer);
    	System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        HttpResponse response = get("/servers/detail");
        String entity = EntityUtils.toString(response.getEntity());

        System.out.println(entity);
        
        
        
        System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
         response = get("/flavors/detail");
         entity = EntityUtils.toString(response.getEntity());

        System.out.println(entity);
        
    }
    
    
    

    public String createServer(String name, String imageRef, String flavorRef, Map<String, String> metadata)
            throws ClientProtocolException, IOException {

        JSONObject jMetadata = new JSONObject();
        jMetadata.putAll(metadata);
        JSONObject jServer = new JSONObject();
        jServer.put("name", name);
        jServer.put("imageRef", imageRef);
        jServer.put("flavorRef", flavorRef);
        jServer.put("metadata", jMetadata);
        JSONObject jReq = new JSONObject();
        jReq.put("server", jServer);

        HttpResponse response = post("/servers", jReq);
        String entity = EntityUtils.toString(response.getEntity());

        return entity;
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
    public IaasVM startVm(Map<String, String> arguments) throws Exception {
        return new IaasVM(createServer(
                arguments.get("name"),
                arguments.get("imageRef"),
                arguments.get("flavorRef"),
                null) // TODO
        );
    }

    @Override
    public void stopVm(IaasVM vm) throws Exception {
        deleteServer(vm.getVmId());
    }

    @Override
    public boolean isVmStarted(IaasVM vm) throws Exception {
        throw new UnsupportedOperationException("not implemented");
    }
}
