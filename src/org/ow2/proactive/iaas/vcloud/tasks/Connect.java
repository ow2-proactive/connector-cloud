package org.ow2.proactive.iaas.vcloud.tasks;

import java.io.Serializable;

import org.ow2.proactive.iaas.IaasApi;
import org.ow2.proactive.iaas.IaasExecutable;
import org.ow2.proactive.scheduler.common.task.TaskResult;

public class Connect extends IaasExecutable {

    @Override
    public Serializable execute(TaskResult... results) throws Throwable {
        IaasApi api = createApi(args);
        return (Serializable) api; //((VCloudAPI) api).getVCloudClient();
    }

}
