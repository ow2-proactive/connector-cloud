package org.ow2.proactive.iaas.vcloud.tasks;

import java.io.Serializable;

import org.ow2.proactive.iaas.IaasExecutable;
import org.ow2.proactive.iaas.vcloud.VCloudAPI;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scripting.PropertyUtils;


public class ConfigureVM extends IaasExecutable {

    @Override
    public Serializable execute(TaskResult... results) throws Throwable {
        VCloudAPI api = null;
        try {
            api = (VCloudAPI) createApi(args);
            String vappId = args.get("vappid").split("/")[2];
            System.out.println("[ConfigureVM task] Configuring vApp " + vappId + "...");

            int cpu = Integer.valueOf(args.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.CORES));
            int memoryGB = Integer.valueOf(args.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.MEMORY));
            int diskGB = Integer.valueOf(args.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.STORAGE));

            api.configureVM(vappId, cpu, memoryGB * 1024, diskGB * 1024);

            System.out.println("[ConfigureVM task] vApp configured");
            PropertyUtils.propagateProperty("vcloud.vapp.id");
            return "done";

        } catch (Throwable e) {
            e.printStackTrace();
            System.setProperty("error.description", e.getMessage());
            PropertyUtils.propagateProperty("error.description");
            return e.getMessage();
        } finally {
            if(api !=null) {
                api.disconnect();
            }
        }
    }

}
