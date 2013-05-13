package org.ow2.proactive.iaas.vcloud.tasks;

import java.io.Serializable;
import java.util.HashMap;

import org.ow2.proactive.iaas.IaasExecutable;
import org.ow2.proactive.iaas.vcloud.VCloudAPI;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scripting.PropertyUtils;


public class Snapshot extends IaasExecutable {

    @Override
    public Serializable execute(TaskResult... results) throws Throwable {
        HashMap<String, String> occiAttributes = new HashMap<String, String>();
        VCloudAPI api = null;
        String vappId = null;
        try {
            api = (VCloudAPI) createApi(args);
            if (args.get("vappid") != null) {
                vappId = args.get("vappid").split("/")[2];
            } else if (System.getProperty("occi.compute.vendor.vmpath") != null) {
                vappId = System.getProperty("occi.compute.vendor.vmpath").split("/")[2];
                PropertyUtils.propagateProperty("occi.compute.vendor.vmpath");
            } else {
                vappId = System.getProperty("vcloud.vapp.id");
                PropertyUtils.propagateProperty("vcloud.vapp.id");
            }
            String name = "";
            String description = "";
            boolean memory = false;
            boolean quiesce = true;

            System.out.println("[Snapshot task] Creating snapshot on " + vappId + "...");

            api.snapshotInstance(vappId, name, description, memory, quiesce);
            occiAttributes.put("action.state", "done");

        } catch (Throwable e) {
            e.printStackTrace();
            occiAttributes.put("action.state", "error");
            occiAttributes.put("occi.compute.error.code", "1");
            occiAttributes.put("occi.compute.error.description", e.getMessage());
        } finally {
            if (api != null) {
                api.disconnect();
            }
        }

        return occiAttributes;
    }

}
