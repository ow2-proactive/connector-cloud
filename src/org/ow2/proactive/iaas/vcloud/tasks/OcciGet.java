package org.ow2.proactive.iaas.vcloud.tasks;

import java.io.Serializable;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scheduler.common.task.executable.JavaExecutable;
import org.ow2.proactive.scripting.PropertyUtils;


public class OcciGet extends JavaExecutable {

    private String occi_url;
    private String attributeKey;

    @Override
    public void init(Map<String, Serializable> args) throws Exception {
        super.init(args);
        String endpoint = args.get("occi_endpoint").toString();
        String category = args.get("category").toString();
        String occi_core_id = args.get("occi.core.id").toString();
        attributeKey = args.get("attribute").toString();
        occi_url = endpoint + category + "/" + occi_core_id;
    }

    @Override
    public Serializable execute(TaskResult... results) throws Throwable {
        String fullUrl = occi_url + "?attribute=" + attributeKey;
        HttpGet get = new HttpGet(fullUrl);
        HttpResponse response = new DefaultHttpClient().execute(get);
        String attributeValue = EntityUtils.toString(response.getEntity());

        System.setProperty(attributeKey, attributeValue);
        PropertyUtils.propagateProperty(attributeKey);

        System.out.println("Retrieved from " + fullUrl + "  --> " + attributeValue);

        return "done";
    }

}
