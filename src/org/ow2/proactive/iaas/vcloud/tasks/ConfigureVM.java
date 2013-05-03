package org.ow2.proactive.iaas.vcloud.tasks;

import java.io.Serializable;

import org.ow2.proactive.iaas.IaasExecutable;
import org.ow2.proactive.iaas.vcloud.VCloudAPI;
import org.ow2.proactive.scheduler.common.task.TaskResult;


public class ConfigureVM extends IaasExecutable {

    @Override
    public Serializable execute(TaskResult... results) throws Throwable {
        VCloudAPI api = (VCloudAPI) createApi(args);
        String instanceID = results[0].toString();
        int cpu = Integer.valueOf(args.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.CORES));
        int memoryMB = Integer.valueOf(args.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.MEMORY));
        int diskGB = Integer.valueOf(args.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.STORAGE));
        api.configureVM(instanceID, cpu, memoryMB, diskGB);

        return results[0];
    }

}
