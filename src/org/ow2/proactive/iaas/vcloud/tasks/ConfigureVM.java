package org.ow2.proactive.iaas.vcloud.tasks;

import java.io.Serializable;

import org.ow2.proactive.iaas.IaasExecutable;
import org.ow2.proactive.iaas.vcloud.VCloudAPI;
import org.ow2.proactive.scheduler.common.task.TaskResult;


public class ConfigureVM extends IaasExecutable {

    @Override
    public Serializable execute(TaskResult... results) throws Throwable {
        try {
            VCloudAPI api = (VCloudAPI) createApi(args);
            String instanceID = results[0].toString();
            int cpu = 1;
            int memoryMB = 1024;
            int diskGB = 3;
            api.configureVM(instanceID, cpu, memoryMB, diskGB);
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage() + " <-- " + e.getCause().getMessage());
        }

        return results[0];
    }

}
