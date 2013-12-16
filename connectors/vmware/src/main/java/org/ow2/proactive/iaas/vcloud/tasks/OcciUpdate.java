package org.ow2.proactive.iaas.vcloud.tasks;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scheduler.common.task.executable.JavaExecutable;


public class OcciUpdate extends JavaExecutable {

    private String occi_url;

    @Override
    public void init(Map<String, Serializable> args) throws Exception {
        super.init(args);
        String endpoint = args.get("occi_endpoint").toString();
        String category = args.get("category").toString();
        String occi_core_id = args.get("occi.core.id").toString();
        occi_url = endpoint + category + "/" + occi_core_id;
    }

    @Override
    public Serializable execute(TaskResult... results) throws Throwable {
        HashMap<String, String> attributeMap = null;
        if (results.length == 0) {
            attributeMap = new HashMap<String, String>();
            attributeMap.put("action.state", "done");
        } else {
            System.out.println("OcciUpdate TaskResult " + results[0]);
            ByteArrayInputStream in = new ByteArrayInputStream(results[0].getSerializedValue());
            ObjectInputStream is = new ObjectInputStream(in);
            attributeMap = (HashMap<String, String>) is.readObject();
        }
        HttpPut put = new HttpPut(occi_url);
        String attributes = "";
        String comma = "";
        for (String key : attributeMap.keySet()) {
            attributes += comma + key + "=" + attributeMap.get(key);
            comma = ",";
        }

        put.addHeader("X-OCCI-Attribute", attributes);
        System.out.println("Updating " + occi_url);
        HttpResponse response = new DefaultHttpClient().execute(put);

        return response.toString();
    }

}
