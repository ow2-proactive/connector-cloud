package org.ow2.proactive.iaas.vcloud.tasks;

import java.io.Serializable;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scheduler.common.task.executable.JavaExecutable;


public class OcciUpdate extends JavaExecutable {

    private String occi_endpoint = "http://10.1.244.17:8182/compute/";
    private String occi_core_id;
    private String vdcName;

    private String vappId;
    private String ip;

    @Override
    public void init(Map<String, Serializable> args) throws Exception {
        super.init(args);
        occi_endpoint = args.get("occi_endpoint").toString();
        occi_core_id = args.get("occi.core.id").toString();
        vdcName = args.get("vdcName").toString();
    }

    @Override
    public Serializable execute(TaskResult... results) throws Throwable {  
        System.out.println("OcciUpdate TaskResult " + results[0]);
        vappId = results[0].toString().split(",")[0];
        ip = results[0].toString().split(",")[1];

        HttpPut put = new HttpPut(occi_endpoint + occi_core_id);
        String attributes = "occi.compute.state=\"active\"";
        attributes += ",occi.compute.vendor.vmpath=\"VCLOUD/" + vdcName + "/" + vappId + "\"";
        attributes += ",occi.networkinterface.address=\"" + ip + "\"";
        put.addHeader("X-OCCI-Attribute", attributes);
        HttpResponse response = new DefaultHttpClient().execute(put);

        return response.toString();
    }

}
