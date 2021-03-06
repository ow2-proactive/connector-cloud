package org.ow2.proactive.iaas.vcloud.tasks;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import org.ow2.proactive.iaas.IaasExecutable;
import org.ow2.proactive.iaas.vcloud.VCloudAPI;
import org.ow2.proactive.iaas.vcloud.VCloudAPI.VCloudAPIConstants;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scripting.PropertyUtils;


public class Deploy extends IaasExecutable {

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
            } else if (System.getProperty("vcloud.vapp.id") != null) {
                vappId = System.getProperty("vcloud.vapp.id");
                PropertyUtils.propagateProperty("vcloud.vapp.id");
            } else {
                vappId = (String) results[0].value();
            }
            String vdcName = args.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.VDC_NAME);
            System.out.println("[Deploy task] Deploying vApp " + vappId + "...");

            //            String vmPassword = args.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.PASSWORD);
            //            if (vmPassword != null) {
            //                if (!vmPassword.isEmpty()) {
            //                    api.setPassword(vappId, vmPassword);
            //                } else {
            //                    api.generatePassword(vappId);
            //                }
            //            }

            String ipAddress = api.deployInstance(vappId);

            String vmPassword = System.getProperty("occi.compute.password");
            if (vmPassword == null) {
                vmPassword = api.getPassword(vappId);
            }

            List<String> vmIDs = api.getVmId(vappId);
            String vmId = vmIDs.get(0);

            System.setProperty("vcloud.instance.ipaddress", ipAddress);
            PropertyUtils.propagateProperty("vcloud.instance.ipaddress");
            System.out.println("[Deploy task] vApp deployed on " + ipAddress);

            occiAttributes.put("occi.compute.state", "active");
            occiAttributes.put("action.state", "done");
            occiAttributes.put("occi.compute.error.code", "0");
            occiAttributes.put("occi.networkinterface.address", ipAddress);
            occiAttributes.put("occi.compute.vendor.vmpath", "VCLOUD/" + vdcName + "/" + vappId + "/" + vmId);
            occiAttributes.put("occi.compute.password", vmPassword);

        } catch (Throwable e) {
            e.printStackTrace();

            occiAttributes.put("occi.compute.state", "inactive");
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
