package org.ow2.proactive.iaas.vcloud.tasks;

import java.io.Serializable;

import org.ow2.proactive.iaas.IaasExecutable;
import org.ow2.proactive.iaas.vcloud.VCloudAPI;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scripting.PropertyUtils;


public class ConfigureNetwork extends IaasExecutable {

    @Override
    public Serializable execute(TaskResult... results) throws Throwable {
        VCloudAPI api = null;
        try {
            api = (VCloudAPI) createApi(args);
            String vappId = args.get("vappid").split("/")[2];
            String vdcName = args.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.VDC_NAME);
            System.out.println("[ConfigureNetwork task] Configuring vApp [" + vappId + "] with network [" +
                vdcName + "]...");

            api.configureNetwork(vappId, vdcName);
            PropertyUtils.propagateProperty("vcloud.vapp.id");
            return "done";

        } catch (Throwable e) {
            e.printStackTrace();
            System.setProperty("error.description", e.getMessage());
            return e.getMessage();
        } finally {
            if(api !=null) {
                api.disconnect();
            }
        }
    }

}
