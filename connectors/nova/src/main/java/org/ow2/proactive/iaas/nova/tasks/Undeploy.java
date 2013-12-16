package org.ow2.proactive.iaas.nova.tasks;

import net.minidev.json.JSONObject;
import org.ow2.proactive.iaas.IaasApi;
import org.ow2.proactive.iaas.IaasExecutable;
import org.ow2.proactive.iaas.IaasInstance;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import java.io.Serializable;
import java.util.Map;

public class Undeploy extends IaasExecutable {

    public static final String INSTANCE_ID_KEY = "occi.compute.vendor.uuid";

    @Override
    public Serializable execute(TaskResult... results) throws Throwable {
        IaasApi api = null;

        try {
            api = createApi(args);
            String instanceId = getInstanceId(args, results);
            api.stopInstance(new IaasInstance(instanceId));
            return getJsonString("deleted");
        } catch (Throwable e) {
            throw e;
        } finally {
            if (api != null)
                api.disconnect();
        }
    }

    private String getJsonString(String status) {
        JSONObject o = new JSONObject();
        o.put("occi.compute.state", status);
        return o.toString();
    }

    private String getInstanceId(Map<String, String> args, TaskResult[] results) throws Throwable {
        String instanceId;

        instanceId = args.get(INSTANCE_ID_KEY);
        if (instanceId != null && !instanceId.isEmpty())
            return instanceId;

        if (results.length > 0)
            return (String) results[0].value();

        throw new IllegalArgumentException("VM uuid not provided.");
    }

}
