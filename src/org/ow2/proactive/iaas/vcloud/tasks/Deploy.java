package org.ow2.proactive.iaas.vcloud.tasks;

import java.io.Serializable;

import org.ow2.proactive.iaas.IaasExecutable;
import org.ow2.proactive.iaas.vcloud.VCloudAPI;
import org.ow2.proactive.scheduler.common.task.TaskResult;


public class Deploy extends IaasExecutable {

    @Override
    public Serializable execute(TaskResult... results) throws Throwable {
        String vappId = results[0].toString();
        System.out.println("results[0].vappId=" + vappId + ", vcloud.instance.id=" +
            System.getProperty("vcloud.instance.id"));

        VCloudAPI api = (VCloudAPI) createApi(args);
        args.put(VCloudAPI.VCloudAPIConstants.InstanceParameters.INSTANCE_ID, results[0].toString());
        String ipAddress = api.deployInstance(args);
        System.setProperty("vcloud.instance.ipaddress", ipAddress);

        return vappId + "," + ipAddress;
    }

}
