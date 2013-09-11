package org.ow2.proactive.iaas.nova.tasks;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.ow2.proactive.iaas.IaasExecutable;
import org.ow2.proactive.iaas.IaasInstance;
import org.ow2.proactive.iaas.nova.NovaAPI;
import org.ow2.proactive.iaas.vcloud.VCloudAPI;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scripting.PropertyUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


public class Deploy extends IaasExecutable {

    @Override
    public Serializable execute(TaskResult... results) throws Throwable {
        NovaAPI api = null;
        String instanceId = null;

        try {
            api = (NovaAPI) createApi(args);
            instanceId = startInstance(api);
        } catch (Throwable e) {
            return e;
        } finally {
            if (api != null) {
                api.disconnect();
            }
        }

        HttpResponse resp = updateOcciServer(args, instanceId);
        System.out.println("Update result: " + resp.getStatusLine().getStatusCode() + ":" + resp.getStatusLine().getReasonPhrase());
        return instanceId;
    }

    private String startInstance(NovaAPI api) throws Exception {
        IaasInstance instance = api.startInstance(args);
        return instance.getInstanceId();
    }

    public HttpResponse updateOcciServer(Map<String, String> args, String instanceId) throws IOException {

        String endpoint = args.get("occi_endpoint").toString();
        String category = args.get("category").toString();
        String occi_core_id = args.get("occi.core.id").toString();
        String occi_url = endpoint + category + "/" + occi_core_id;

        System.out.println("Updating " + occi_url);

        Map<String, String> attributeMap = new HashMap<String, String>();
        attributeMap.put("uuid", instanceId);
        HttpPut put = new HttpPut(occi_url);
        String attributes = "";
        String comma = "";
        for (String key : attributeMap.keySet()) {
            attributes += comma + key + "=" + attributeMap.get(key);
            comma = ",";
        }

        put.addHeader("X-OCCI-Attribute", attributes);
        return new DefaultHttpClient().execute(put);
    }


}
