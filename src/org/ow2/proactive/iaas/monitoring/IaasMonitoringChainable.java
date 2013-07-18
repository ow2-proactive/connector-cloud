package org.ow2.proactive.iaas.monitoring;

import org.ow2.proactive.iaas.IaasMonitoringApi;


/**
 * Asking for a cpu.usage might involve going through several levels of 
 * monitoring, for instance, asking to the API of the infrastructure (VMWare), or 
 * using the Sigar MBean exposed by the RMNode running on the target node.
 * 
 *                                                         +-->   [  api  ] 
 * get("cpu.usage") <-> [ cacher ] <-> [ monit merger ] <--|  
 *                                                         +-->   [ sigar ]
 * 
 * Every element on this chain should implement this interface.
 */
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
