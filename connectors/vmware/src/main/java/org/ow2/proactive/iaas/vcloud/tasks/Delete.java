package org.ow2.proactive.iaas.vcloud.tasks;

import java.io.Serializable;

import org.ow2.proactive.iaas.IaasExecutable;
import org.ow2.proactive.iaas.vcloud.VCloudAPI;
import org.ow2.proactive.scheduler.common.task.TaskResult;


public class Delete extends IaasExecutable {

    @Override
    public Serializable execute(TaskResult... results) throws Throwable {
        VCloudAPI api = (VCloudAPI) createApi(args);
        String instanceId = args.get("vappid").split("/")[2];

        System.out.println("[Delete task] Deleting " + instanceId + "...");
        api.deleteInstance(instanceId);
        api.disconnect();

        return null;
    }
}
