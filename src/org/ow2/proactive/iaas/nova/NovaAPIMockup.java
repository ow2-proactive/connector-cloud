package org.ow2.proactive.iaas.nova;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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
import org.ow2.proactive.iaas.IaaSMonitoringApi;
import org.ow2.proactive.iaas.IaasApi;
import org.ow2.proactive.iaas.IaasApiFactory;
import org.ow2.proactive.iaas.IaasInstance;
import com.jayway.jsonpath.JsonPath;


public class NovaAPIMockup implements IaasApi, IaaSMonitoringApi  {

    private static final Logger logger = Logger.getLogger(NovaAPIMockup.class);
    private static Map<Integer, NovaAPIMockup> instances;

    public static IaasApi getNovaAPI(Map<String, String> args) throws URISyntaxException,
            AuthenticationException {
        return getNovaAPI(null, null, null, null);
    }

    public static synchronized NovaAPIMockup getNovaAPI(
    		String username, 
    		String password, 
    		String tenantName,
            URI endpoint) throws AuthenticationException {
        if (instances == null) {
            instances = new HashMap<Integer, NovaAPIMockup>();
        }
        int hash = (username + password + tenantName).hashCode();
        NovaAPIMockup instance = instances.get(hash);
        if (instance == null) {
            try {
                instances.remove(hash);
                instance = new NovaAPIMockup(username, password, tenantName, endpoint);
            } catch (Throwable t) {
                throw new AuthenticationException("Failed to authenticate to " + endpoint, t);
            }
            instances.put(hash, instance);
        }
        return instance;
    }

    private NovaAPIMockup(
    		String username, 
    		String password, 
    		String tenantName, 
    		URI endpoint) throws IOException {
    	logger.info("Initializing Nova API MOCKUP...");
    	logger.info(
    			String.format("username='%s', tenant='%s', endpoint='%s'", 
    					username, tenantName, endpoint));
    }

    public String createServer(String name, String imageRef, String flavorRef, String userData,
            Map<String, String> metaData) throws ClientProtocolException, IOException {
        return "" + new Random().nextInt();
    }

    public boolean rebootServer(String serverId, String method) throws ClientProtocolException, IOException {
        return true;
    }

    public boolean deleteServer(String serverId) throws ClientProtocolException, IOException {
        return true;
    }

    @Override
    public IaasInstance startInstance(Map<String, String> arguments) throws Exception {
    	logger.info("Start instance.");
        return new IaasInstance(new Random().nextInt() + "");
    }

    @Override
    public void stopInstance(IaasInstance instance) throws Exception {
    	logger.info("Stop instance.");
    }

    @Override
    public boolean isInstanceStarted(IaasInstance instance) throws Exception {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public String getName() {
    	logger.info("GetName instance.");
        return IaasApiFactory.IaasProvider.NOVA.name();
    }

	@Override
	public void disconnect() throws Exception {
	}

    @Override
    public Map<String, Object> getSummary() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getHosts() throws Exception {
        return new String[]{"host-1", "host-2"};
    }

    @Override
    public String[] getVMs() throws Exception {
        return new String[]{"host-1", "host-2"};
    }

    @Override
    public String[] getVMs(String hostId) throws Exception {
        return new String[]{"host-1", "host-2"};
    }

    @Override
    public Map<String, String> getHostProperties(String hostId) throws Exception {
        Map<String, String> prop = new HashMap<String, String> ();
        prop.put("cpu.usage", "0.1");
        prop.put("mem.usage", "0.1");
        return prop;
    }

    @Override
    public Map<String, String> getVMProperties(String vmId) throws Exception {
        Map<String, String> prop = new HashMap<String, String> ();
        prop.put("cpu.usage", "0.1");
        prop.put("mem.usage", "0.1");
        return prop;
    }

}
