package org.ow2.proactive.iaas.vcloud.tasks;

import java.io.Serializable;

import org.ow2.proactive.iaas.IaasExecutable;
import org.ow2.proactive.iaas.vcloud.VCloudAPI;
import org.ow2.proactive.scheduler.common.task.TaskResult;


public class Copy extends IaasExecutable {

    @Override
    public Serializable execute(TaskResult... results) throws Throwable {
        VCloudAPI api = null;

        try {
            System.out.println("[Copy task] Copying VM inside Vapp "+ "...");
            api = (VCloudAPI) createApi(args);

            String vdcName = args.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.VDC_NAME);
            String vappName = args.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.INSTANCE_NAME);
            String from = args.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.TEMPLATE_NAME);
            String to = args.get("newInstanceName") + getIterationIndex();
            return api.copy(vdcName, vappName, from, to);
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            if(api !=null) {
                api.disconnect();
            }
        }

        return null;
    }
}
