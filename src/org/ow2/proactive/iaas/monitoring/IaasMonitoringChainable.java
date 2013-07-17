package org.ow2.proactive.iaas.monitoring;

import org.ow2.proactive.iaas.IaasMonitoringApi;

public interface IaasMonitoringChainable extends IaasMonitoringApi, IaasNodesListener {
    /**
     * Configure the block.
     */
    public void configure(String nsname, String options);
    
    /**
     * ShutDown the block.
     */
    public void shutDown();
}
