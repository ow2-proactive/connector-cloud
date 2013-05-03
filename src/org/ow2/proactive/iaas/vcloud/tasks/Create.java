package org.ow2.proactive.iaas.vcloud.tasks;

import java.io.Serializable;

import org.ow2.proactive.iaas.IaasExecutable;
import org.ow2.proactive.iaas.IaasInstance;
import org.ow2.proactive.iaas.vcloud.VCloudAPI;
import org.ow2.proactive.scheduler.common.task.TaskResult;


public class Create extends IaasExecutable {

    @Override
    public Serializable execute(TaskResult... results) throws Throwable {
        VCloudAPI api = (VCloudAPI) createApi(args);// (VCloudAPI) results[0];
        IaasInstance instance = api.createInstance(args);
        System.setProperty("vcloud.instance.id", instance.getInstanceId());
        return instance.getInstanceId();
    }

}
