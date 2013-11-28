package org.ow2.proactive.iaas;

import org.ow2.proactive.scheduler.common.task.executable.JavaExecutable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for operations on Iaas, responsible for selecting the Iaas implementation from a parameter.
 *
 * You can use by setting the {@link #providerName} parameter.
 */
public abstract class IaasExecutable extends JavaExecutable {

    /**
     * See {@link IaasApiFactory.IaasProvider} for accepted values
     */
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
