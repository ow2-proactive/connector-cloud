package org.ow2.proactive.iaas;

import org.ow2.proactive.scheduler.common.task.executable.JavaExecutable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public abstract class IaasExecutable extends JavaExecutable {

    @SuppressWarnings("unused") // will be written by #init()
    private String providerName;

    protected Map<String, String> args = new HashMap<String, String>();

    @Override
    public void init(Map<String, Serializable> args) throws Exception {
        super.init(args);

        for (Map.Entry<String, Serializable> entry : args.entrySet()) {
            this.args.put(entry.getKey(), entry.getValue().toString());
        }

    }

    protected IaasApi createApi(Map<String, String> args) throws Exception {
        return IaasApiFactory.create(providerName, args);
    }
}
