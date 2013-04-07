package org.ow2.proactive.iaas.vcloud.tasks;

import java.io.Serializable;

import org.ow2.proactive.iaas.IaasExecutable;
import org.ow2.proactive.iaas.vcloud.VCloudAPI;
import org.ow2.proactive.scheduler.common.task.TaskResult;

public class Snapshot extends IaasExecutable {

    @Override
    public Serializable execute(TaskResult... results) throws Throwable {
        VCloudAPI api = (VCloudAPI) createApi(args);
        String instanceID = args.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.INSTANCE_ID);
        String name = "";
        String description = "";
        boolean memory = false;
        boolean quiesce = true;
        api.snapshotInstance(api.getIaasInstance(instanceID), name, description, memory, quiesce);

        return null;
    }

}
