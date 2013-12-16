package org.ow2.proactive.iaas.vcloud.tasks;

import java.io.Serializable;

import org.ow2.proactive.iaas.IaasExecutable;
import org.ow2.proactive.iaas.vcloud.VCloudAPI;
import org.ow2.proactive.iaas.vcloud.VCloudAPI.VCloudAPIConstants;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scripting.PropertyUtils;


public class ConfigurePassword extends IaasExecutable {

    @Override
    public Serializable execute(TaskResult... results) throws Throwable {
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
            System.out.println("[ConfigurePassword task] Setting password for vApp " + vappId + "...");

            String vmPassword = args.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.PASSWORD);
            if (vmPassword != null) {
                if (!vmPassword.isEmpty()) {
                    api.setPassword(vappId, vmPassword);
                    System.setProperty("occi.compute.password", vmPassword);
                    PropertyUtils.propagateProperty("occi.compute.password");
                } else {
                    api.generatePassword(vappId);
                }
            }

            return "done";

        } catch (Throwable e) {
            e.printStackTrace();
            System.setProperty("error.description", e.getMessage());
            return e.getMessage();
        } finally {
            if (api != null) {
                api.disconnect();
            }
        }
    }
}
