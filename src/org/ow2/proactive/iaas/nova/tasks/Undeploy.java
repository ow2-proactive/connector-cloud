package org.ow2.proactive.iaas.nova.tasks;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.ow2.proactive.iaas.IaasExecutable;
import org.ow2.proactive.iaas.IaasInstance;
import org.ow2.proactive.iaas.vcloud.VCloudAPI;
import org.ow2.proactive.iaas.vcloud.VCloudAPI.VCloudAPIConstants;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scripting.PropertyUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


public class Undeploy extends IaasExecutable {

    public static final String INSTANCE_ID_KEY = "uuid";


    @Override
    public Serializable execute(TaskResult... results) throws Throwable {
        String instanceId;

        instanceId = getInstanceId(args, results);

        createApi(args).stopInstance(new IaasInstance(instanceId));
        return "DONE";
    }

    private String getInstanceId(Map<String, String> args, TaskResult[] results) throws Throwable {
        String instanceId;

        instanceId = args.get(INSTANCE_ID_KEY);
        if (instanceId != null && !instanceId.isEmpty())
            return instanceId;

        instanceId = getInstanceIdFromOcciServer(args);
        if (instanceId != null && !instanceId.isEmpty())
            return instanceId;

        if (results.length > 0)
            return (String) results[0].value();

        throw new IllegalArgumentException("VM uuid not provided.");
    }

    public String getInstanceIdFromOcciServer(Map<String, String> args) throws Throwable {

        String endpoint = args.get("occi_endpoint").toString();
        String category = args.get("category").toString();
        String occi_core_id = args.get("occi.core.id").toString();
        String attributeKey = args.get("attribute").toString();
        String occi_url = endpoint + category + "/" + occi_core_id;

        String fullUrl = occi_url + "?attribute=" + attributeKey;
        HttpGet get = new HttpGet(fullUrl);
        HttpResponse response = new DefaultHttpClient().execute(get);
        String attributeValue = EntityUtils.toString(response.getEntity());

        return attributeValue;
    }


    @Override
    public void init(Map<String, Serializable> args) throws Exception {
        super.init(args);
    }

}
