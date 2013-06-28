package org.ow2.proactive.iaas.vcloud.tasks;

import static org.ow2.proactive.iaas.vcloud.VCloudAPI.VCloudAPIConstants.ApiParameters.ORGANIZATION_NAME;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.ow2.proactive.iaas.IaasExecutable;
import org.ow2.proactive.iaas.vcloud.VCloudAPI;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scripting.PropertyUtils;


public class Undeploy extends IaasExecutable {
    
    @Override
    public void init(Map<String, Serializable> args) throws Exception {
        super.init(args);
        String vmPath = System.getProperty("occi.compute.vendor.vmpath");
        if (vmPath != null && vmPath.length() != 0) {
            args.put(ORGANIZATION_NAME, vmPath.split("/")[1]);
        }
    }

    @Override
    public Serializable execute(TaskResult... results) throws Throwable {
        HashMap<String, String> occiAttributes = new HashMap<String, String>();
        VCloudAPI api = null;
        try {
            api = (VCloudAPI) createApi(args);
            String vappId = null;
            if (args.get("vappid") != null) {
                vappId = args.get("vappid").split("/")[2];
            } else if( System.getProperty("occi.compute.vendor.vmpath") != null ) {
                vappId = System.getProperty("occi.compute.vendor.vmpath").split("/")[2];
                PropertyUtils.propagateProperty("occi.compute.vendor.vmpath");
            } else {
                vappId = System.getProperty("vcloud.vapp.id");
                PropertyUtils.propagateProperty("vcloud.vapp.id");
            }

            System.out.println("[Undeploy task] Undeploying " + vappId + "...");

            api.undeployInstance(vappId);
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
