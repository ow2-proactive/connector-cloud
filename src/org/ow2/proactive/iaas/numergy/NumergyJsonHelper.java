package org.ow2.proactive.iaas.numergy;

import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONObject;


public class NumergyJsonHelper {

    public static JSONObject buildAuthenticationJson(String accessKey, String secretKey, String tenantId) {
        JSONObject jsonCreds = new JSONObject();
        jsonCreds.put(NumergyAPI.NumergyAPIConstants.ApiParameters.ACCESS_KEY, accessKey);
        jsonCreds.put(NumergyAPI.NumergyAPIConstants.ApiParameters.SECRET_KEY, secretKey);
        JSONObject jsonAuth = new JSONObject();
        jsonAuth.put(NumergyAPI.NumergyAPIConstants.ApiParameters.PASSWORD_CREDENTIALS, jsonCreds);
        jsonAuth.put(NumergyAPI.NumergyAPIConstants.ApiParameters.TENANT_ID, tenantId);
        JSONObject jsonReq = new JSONObject();
        jsonReq.put(NumergyAPI.NumergyAPIConstants.ApiParameters.AUTH, jsonAuth);
        return jsonReq;
    }

    public static JSONObject buildStartServerJson() {
        JSONObject jReq = new JSONObject();
        jReq.put("os-start", null);
        return jReq;
    }

    public static JSONObject buildDuplicateJson(String name) {
        JSONObject jServer = new JSONObject();
        jServer.put("password_delivery", "API");
        jServer.put("name", name);
        JSONObject jReq = new JSONObject();
        jReq.put("duplicate", jServer);
        return jReq;
    }

    public static String getServerIdFromJson(String entity) {
        try {
            return JsonPath.read(entity, "$.server.id");
        } catch (InvalidPathException e) {
            throw new InvalidPathException("Could not obtain server ID: " + entity, e);
        }
    }

    public static String getComputeUriFromJson(String entity) {
        String compute;
        try {
            compute = JsonPath.read(entity, "$.access.serviceCatalog[?(@.type=='compute')].endpoints[0].publicURL");
        } catch (InvalidPathException ex) {
            throw new InvalidPathException("Cannot obtain novaUri from: " + entity);
        }
        return compute;
    }

    public static String getSessionIdFromJson(String entity) {
        try {
            return JsonPath.read(entity, "$.access.token.id");
        } catch (InvalidPathException e) {
            throw new InvalidPathException("Could not obtain token ID: " + entity, e);
        }
    }

    public static String getServerStatusFromJson(String instanceId, String entity) {
        String status;
        try {
            status = JsonPath.read(entity, "$.server.status");
        } catch (InvalidPathException e) {
            throw new InvalidPathException("Could not get status of server: " + instanceId + " : " + entity, e);
        }
        return status;
    }

    public static String getRequestIdFromJson(String entity) {
        try {
            return JsonPath.read(entity, "$.request_id");
        } catch (InvalidPathException e) {
            throw new InvalidPathException("Could not obtain request ID: " + entity, e);
        }
    }

    public static String getServerPrivateIpFromJson(String entity) {
        try {
            return JsonPath.read(entity, "$.server.addresses.private[0].addr");
        } catch (InvalidPathException e) {
            throw new InvalidPathException("Could not obtain IP address: " + entity, e);
        }
    }

}
