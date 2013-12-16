package org.ow2.proactive.iaas.nova.tasks;

import net.minidev.json.JSONObject;
import org.ow2.proactive.iaas.IaasApi;
import org.ow2.proactive.iaas.IaasExecutable;
import org.ow2.proactive.iaas.IaasInstance;
import org.ow2.proactive.iaas.nova.NovaAPI;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import java.io.Serializable;


public class Deploy extends IaasExecutable {

    @Override
    public Serializable execute(TaskResult... results) throws Throwable {
        IaasApi api = null;
        try {
            api = createApi(args);
            String instanceId = startInstance(api);
            return getJsonString(instanceId);
        } catch (Throwable e) {
            throw e;
        } finally {
            if (api != null)
                api.disconnect();
        }
    }

    private String getJsonString(String instanceId) {
        JSONObject o = new JSONObject();
        o.put("occi.compute.vendor.uuid", instanceId);
        return o.toString();
    }

    private String startInstance(IaasApi api) throws Exception {
        IaasInstance instance = api.startInstance(args);
        return instance.getInstanceId();
    }

}
