package org.ow2.proactive.iaas.numergy;

import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import com.rabbitmq.tools.json.JSONReader;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.log4j.Logger;
import org.ow2.proactive.iaas.IaasApi;
import org.ow2.proactive.iaas.IaasApiFactory;
import org.ow2.proactive.iaas.IaasInstance;
import org.ow2.proactive.iaas.metadata.MetadataHttpClient;

import javax.security.sasl.AuthenticationException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;


public class NumergyAPI implements IaasApi {

    private static final Logger logger = Logger.getLogger(NumergyAPI.class);

    private static Map<Integer, NumergyAPI> instances;

    private NumergyInstancesStarter instancesStarter;
    NumergyHttpClient numergyClient;
    MetadataHttpClient metadataClient;

    // ////////////////////////
    // NUMERGY FACTORY
    // ////////////////////////

    public static IaasApi getNumergyAPI(Map<String, String> args) throws URISyntaxException,
            AuthenticationException {
        return getNumergyAPI(
                args.get(NumergyAPIConstants.ApiParameters.ACCESS_KEY),
                args.get(NumergyAPIConstants.ApiParameters.SECRET_KEY),
                args.get(NumergyAPIConstants.ApiParameters.TENANT_ID),
                new URI(args.get(NumergyAPIConstants.ApiParameters.API_URL)),
                new URI(args.get(NumergyAPIConstants.METADATA_SERVER_URL))
        );
    }

    public static synchronized NumergyAPI getNumergyAPI(String accessKey, String secretKey, String tenantId,
                                                        URI endpoint, URI metadataServer) throws AuthenticationException {
        if (instances == null)
            instances = new HashMap<Integer, NumergyAPI>();

        int hash = (accessKey + secretKey + tenantId).hashCode();
        NumergyAPI instance = instances.get(hash);
        if (instance == null) {
            try {
                instance = new NumergyAPI(accessKey, secretKey, tenantId, endpoint, metadataServer);
            } catch (Throwable t) {
                throw new AuthenticationException("Failed to create API client", t);
            }
            instances.put(hash, instance);
        }
        return instance;
    }

    private NumergyAPI(String accessKey, String secretKey, String tenantId, URI apiEndpoint, URI metadataServerEndpoint) throws IOException {

        logger.debug("Creating new Numergy API.");

        this.numergyClient = new NumergyHttpClient(accessKey, secretKey, tenantId, apiEndpoint);
        this.metadataClient = new MetadataHttpClient(metadataServerEndpoint);
        this.instancesStarter = new NumergyInstancesStarter(this);

    }

    protected String startCreatedServer(String serverId) throws IOException {

        JSONObject jReq = NumergyJsonHelper.buildStartServerJson();
        String entity = numergyClient.executePost("/servers/" + serverId + "/action", jReq);
        try {
            return NumergyJsonHelper.getRequestIdFromJson(entity);
        } catch (InvalidPathException e) {
            throw new RuntimeException("Could not start instance " + serverId + " : " + entity);
        }
    }

    protected void updateMetadataInfo(String serverId, JSONObject metadata) throws IOException {
        String entity = numergyClient.executeGet("/servers/" + serverId);
        String ip;
        try {
            ip = NumergyJsonHelper.getServerPrivateIpFromJson(entity);
        } catch (InvalidPathException e) {
            throw new RuntimeException("Could get server private IP: " + serverId + ", " + entity, e);
        }
        metadataClient.putMetadata(ip, metadata);
    }

    protected String instanceStatus(String instanceId) throws Exception {
        String entity = numergyClient.executeGet("/servers/" + instanceId);
        return NumergyJsonHelper.getServerStatusFromJson(instanceId, entity);
    }


    // ////////////////////////
    // PRIVATE
    // ////////////////////////

    private String duplicateServer(String originInstanceId, String name) throws IOException {

        logger.debug(String.format("Duplicating VM [instance='%s', name='%s']",
                                   originInstanceId, name));

        JSONObject jReq = NumergyJsonHelper.buildDuplicateJson(name);

        String entity = numergyClient.executePost("/servers/" + originInstanceId + "/action", jReq);

        String serverId;
        try {
            serverId = NumergyJsonHelper.getServerIdFromJson(entity);
        } catch (Exception e) {
            try {
                serverId = getServerIdFromWorkaround(name);
            } catch (Exception j) {
                throw new RuntimeException("Could not create server: " + name + ", " + entity, j);
            }
        }

        return serverId;
    }

    private String getServerIdFromWorkaround(String name) throws IOException {

        /* The Numergy API is buggy and returns an error for server
        duplication invokations, even if duplication was successful.
        This (hopefully temporal) workaround fixes the issue. */

        String entity = numergyClient.executeGet("/servers");

        JSONArray results;
        try {
            results = JsonPath.read(entity, "$.servers[?(@.name=='" + name + "')].id");
        } catch (InvalidPathException e) {
            throw new RuntimeException("Could not get ID of new server: " + name + " : " + entity, e);
        }

        if (results.size() < 1)
            throw new RuntimeException("No VM found: " + entity);

        if (results.size() > 1)
            logger.warn("Unexpected number of VMs: " + results.toJSONString());

        return results.get(results.size() - 1).toString();
    }

    // ////////////////////////
    // API METHODS
    // ////////////////////////

    @Override
    public IaasInstance startInstance(Map<String, String> arguments) throws Exception {
        String instanceId = duplicateServer(
                arguments.get(NumergyAPIConstants.InstanceParameters.INSTANCE_REF),
                arguments.get(NumergyAPIConstants.InstanceParameters.NAME));

        String metadataStr = arguments.get(NumergyAPIConstants.InstanceParameters.META_DATA);
        Object obj = (new JSONReader()).read(metadataStr);
        JSONObject metadataJson = new JSONObject(((Map) obj));

        instancesStarter.addServerToWatchAndStart(instanceId, metadataJson);

        return new IaasInstance(instanceId);
    }

    @Override
    public void stopInstance(IaasInstance instance) throws Exception {
        String serverId = instance.getInstanceId();
        String entity = numergyClient.executeDelete("/servers/" + serverId);
        try {
            NumergyJsonHelper.getRequestIdFromJson(entity);
        } catch (InvalidPathException e) {
            throw new RuntimeException("Could not delete instance " + serverId + " : " + entity, e);
        }
    }

    @Override
    public boolean isInstanceStarted(IaasInstance instance) throws Exception {
        return "ACTIVE".equals(instanceStatus(instance.getInstanceId()));
    }

    @Override
    public String getName() {
        return IaasApiFactory.IaasProvider.NUMERGY.name();
    }

    @Override
    public void disconnect() throws Exception {
    }

    // ////////////////////////
    // CONSTANTS
    // ////////////////////////

    public class NumergyAPIConstants {

        static final String METADATA_SERVER_URL = "metadataServer";

        public class ApiParameters {
            static final String API_URL = "apiurl";
            static final String AUTH = "auth";
            static final String ACCESS_KEY = "accessKey";
            static final String SECRET_KEY = "secretKey";
            static final String PASSWORD_CREDENTIALS = "apiAccessKeyCredentials";
            static final String TENANT_ID = "tenantId";
        }

        public class InstanceParameters {
            public static final String INSTANCE_REF = "instanceRef";
            public static final String META_DATA = "metadata";
            public static final String NAME = "name";
        }
    }
}