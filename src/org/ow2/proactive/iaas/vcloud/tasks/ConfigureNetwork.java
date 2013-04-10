package org.ow2.proactive.iaas.vcloud.tasks;

import java.io.Serializable;

import org.ow2.proactive.iaas.IaasExecutable;
import org.ow2.proactive.iaas.vcloud.VCloudAPI;
import org.ow2.proactive.scheduler.common.task.TaskResult;


public class ConfigureNetwork extends IaasExecutable {

    @Override
    public Serializable execute(TaskResult... results) throws Throwable {
        try {
            VCloudAPI api = (VCloudAPI) createApi(args);
            args.put(VCloudAPI.VCloudAPIConstants.InstanceParameters.INSTANCE_ID, results[0].toString());
            api.configureNetwork(args);
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage() + " <-- " + e.getCause().getMessage());
        }

        return results[0];
    }

}
