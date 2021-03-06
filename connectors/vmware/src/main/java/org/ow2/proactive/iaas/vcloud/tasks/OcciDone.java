package org.ow2.proactive.iaas.vcloud.tasks;

import java.io.Serializable;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scheduler.common.task.executable.JavaExecutable;


public class OcciDone extends JavaExecutable {

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
        HttpPut put = new HttpPut(occi_url);
        put.addHeader("X-OCCI-Attribute", "action.state=done");
        System.out.println("Updating " + occi_url);
        HttpResponse response = new DefaultHttpClient().execute(put);

        return response.toString();
    }

}
