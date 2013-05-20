package org.ow2.proactive.iaas.vcloud.tasks;

import java.io.Serializable;

import org.ow2.proactive.iaas.IaasExecutable;
import org.ow2.proactive.iaas.vcloud.VCloudAPI;
import org.ow2.proactive.scheduler.common.task.TaskResult;


public class ComposeVapp extends IaasExecutable {

    @Override
    public Serializable execute(TaskResult... results) throws Throwable {
        VCloudAPI api = null;

        try {
            System.out.println("[ComposeVapp task] Composing Vapp "+ "...");
            api = (VCloudAPI) createApi(args);

            String vdcName = args.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.VDC_NAME);
            String vappTemplateId = args.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.INSTANCE_ID);
            String vappName = args.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.INSTANCE_NAME);

            return api.cloneVapp(vdcName, vappTemplateId, vappName);
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            if(api !=null) {
                api.disconnect();
            }
        }

        return "Failed";
    }
}
