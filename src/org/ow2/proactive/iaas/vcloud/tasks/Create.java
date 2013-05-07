package org.ow2.proactive.iaas.vcloud.tasks;

import java.io.Serializable;

import org.ow2.proactive.iaas.IaasExecutable;
import org.ow2.proactive.iaas.IaasInstance;
import org.ow2.proactive.iaas.vcloud.VCloudAPI;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scripting.PropertyUtils;


public class Create extends IaasExecutable {

    @Override
    public Serializable execute(TaskResult... results) throws Throwable {
        VCloudAPI api = null;
        try {
            api = (VCloudAPI) createApi(args);

            IaasInstance instance = api.createInstance(args);

            System.setProperty("vcloud.vapp.id", instance.getInstanceId());
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
