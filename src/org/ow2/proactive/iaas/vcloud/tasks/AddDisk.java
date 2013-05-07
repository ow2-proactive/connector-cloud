package org.ow2.proactive.iaas.vcloud.tasks;

import java.io.Serializable;
import java.util.HashMap;

import org.ow2.proactive.iaas.IaasExecutable;
import org.ow2.proactive.iaas.vcloud.VCloudAPI;
import org.ow2.proactive.scheduler.common.task.TaskResult;


public class AddDisk extends IaasExecutable {

    @Override
    public Serializable execute(TaskResult... results) throws Throwable {
        HashMap<String, String> occiAttributes = new HashMap<String, String>();
        VCloudAPI api = null;

        try {
            api = (VCloudAPI) createApi(args);
            String instanceId = args.get("vappid").split("/")[2];
            System.out.println("[AddDisk task] Adding disk to " + instanceId + "...");
            int storageGB = Integer.valueOf(args.get("occi.storage.size"));

            api.addVMDisk(instanceId, storageGB * 1024, "/dev/vda");

        } catch (Throwable e) {
            e.printStackTrace();

            occiAttributes.put("action.state", "error");
            occiAttributes.put("occi.compute.error.code", "1");
            occiAttributes.put("occi.compute.error.description", e.getMessage());
        } finally {
            if(api !=null) {
                api.disconnect();
            }
        }

        return occiAttributes;
    }
}
