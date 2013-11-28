package org.ow2.proactive.iaas.vcloud.tasks;

import java.io.Serializable;

import org.ow2.proactive.iaas.IaasExecutable;
import org.ow2.proactive.iaas.vcloud.VCloudAPI;
import org.ow2.proactive.scheduler.common.task.TaskResult;


public class DeleteVM extends IaasExecutable {

    @Override
    public Serializable execute(TaskResult... results) throws Throwable {
        VCloudAPI api = (VCloudAPI) createApi(args);

        String vdcName = args.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.VDC_NAME);
        String vappName = args.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.INSTANCE_NAME);
        String vmName = args.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.TEMPLATE_NAME);

        vmName += Integer.toString(Integer.parseInt(System.getProperty("latest")) + 1);
        System.out.println("[DeleteVM task] Deleting VM " + vmName + "...");
        api.deleteVM(vdcName, vappName, vmName);
        api.disconnect();

        return null;
    }
}
