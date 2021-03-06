package org.ow2.proactive.iaas.vcloud.tasks;

import java.io.Serializable;
import java.util.HashMap;

import org.ow2.proactive.iaas.IaasExecutable;
import org.ow2.proactive.iaas.vcloud.VCloudAPI;
import org.ow2.proactive.iaas.vcloud.VCloudAPI.VCloudAPIConstants;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scripting.PropertyUtils;


public class Undeploy extends IaasExecutable {

//    @Override
//    public void init(Map<String, Serializable> args) throws Exception {
//        super.init(args);
//        String vmPath = System.getProperty("occi.compute.vendor.vmpath");
//        if (vmPath != null && vmPath.length() != 0) {
//            //            System.out.println("UNDEPLOY -- " + vmPath + " vmPath.split[1]=" + vmPath.split("/")[0
//            args.put("providerName", vmPath.split("/")[0]);
//        } else {
//            System.out.println("UNDEPLOY -- VmPath is not set !");
//        }
//    }

    @Override
    public Serializable execute(TaskResult... results) throws Throwable {
        HashMap<String, String> occiAttributes = new HashMap<String, String>();
        VCloudAPI api = null;
        try {
            if (System.getProperty("occi.compute.organization.name") != null) {
                args.put(VCloudAPIConstants.ApiParameters.ORGANIZATION_NAME,
                        System.getProperty("occi.compute.organization.name"));
                PropertyUtils.propagateProperty("occi.compute.organization.name");
            }
            api = (VCloudAPI) createApi(args);
            String vappId = null;
            if (args.get("vappid") != null) {
                vappId = args.get("vappid").split("/")[2];
            } else if (System.getProperty("occi.compute.vendor.vmpath") != null) {
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
