package org.ow2.proactive.iaas.vcloud.tasks;

import java.io.Serializable;
import java.util.HashMap;

import org.ow2.proactive.iaas.IaasExecutable;
import org.ow2.proactive.iaas.vcloud.VCloudAPI;
import org.ow2.proactive.scheduler.common.task.TaskResult;


public class Undeploy extends IaasExecutable {

    @Override
    public Serializable execute(TaskResult... results) throws Throwable {
        HashMap<String, String> occiAttributes = new HashMap<String, String>();
        VCloudAPI api = null;
        try {
            api = (VCloudAPI) createApi(args);
            String vappId = args.get("vappid").split("/")[2];
            System.out.println("[Undeploy task] Undeploying " + vappId + "...");

            api.undeployInstance(vappId);
            occiAttributes.put("action.state", "done");

        } catch (Throwable e) {
            e.printStackTrace();
            occiAttributes.put("action.state", "error");
            occiAttributes.put("occi.compute.error.code", "1");
            occiAttributes.put("occi.compute.error.description", e.getMessage());
        } finally {
            if(api !=null) {
                api.disconnect();
            }
        }
        return occiAttributes;
    }

}
