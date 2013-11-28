package org.ow2.proactive.iaas.vcloud.tasks;

import java.io.Serializable;

import org.ow2.proactive.iaas.IaasExecutable;
import org.ow2.proactive.iaas.vcloud.VCloudAPI;
import org.ow2.proactive.scheduler.common.task.TaskResult;


public class CustomizeVM extends IaasExecutable {

    private static final String SCRIPT_PARAMETERS = "script";

    @Override
    public Serializable execute(TaskResult... results) throws Throwable {
        VCloudAPI api = null;

        try {
            System.out.println("[CustomizeVM task] Customizing VM " + "...");
            api = (VCloudAPI) createApi(args);

            String vmId = (String) results[0].value();
            String scriptFromArgs = args.get(SCRIPT_PARAMETERS);
            String script = scriptFromArgs == null ? System.getProperty(SCRIPT_PARAMETERS) : "";

            System.out.println("[CustomizeVM task] Using script: " + script);

            api.customizeVM(vmId, script);
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            if (api != null) {
                api.disconnect();
            }
        }
        return null;
    }
}
