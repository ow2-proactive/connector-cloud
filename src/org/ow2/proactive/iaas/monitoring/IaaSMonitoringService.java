/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2012 INRIA/University of
 *                 Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org or contact@activeeon.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; version 3 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 * ################################################################
 * %$ACTIVEEON_INITIAL_DEV$
 */

package org.ow2.proactive.iaas.monitoring;

import java.util.Map;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.ow2.proactive.iaas.utils.Utils;
import org.ow2.proactive.iaas.IaaSMonitoringApi;


public class IaaSMonitoringService implements IaaSMonitoringServiceMBean, IaaSNodesListener {

    /** 
     * Key used for referencing the URL of the Sigar MBean of the entity (host/vm). 
     */
    public static final String PROP_PA_SIGAR_JMX_URL = "proactive.sigar.jmx.url";

    /** 
     * Key that belongs to the host properties map. 
     * Its value contains information about all its VMs. 
     */
    protected static final String VMS_INFO_KEY = "vmsinfo";

    /** Logger. */
    private static final Logger logger = Logger.getLogger(IaaSMonitoringService.class);

    /**
     * Loader for monitoring information.
     */
    protected IaaSMonitoringServiceCacher loader;
    
    /**
     * Name of the Node Source being monitored.
     */
    protected String nsname;

    /**
     * Constructor.
     * @param iaaSMonitoringApi
     * @throws IaaSMonitoringServiceException
     */
    public IaaSMonitoringService(IaaSMonitoringApi iaaSMonitoringApi) throws IaaSMonitoringServiceException {
        try {
            loader = new IaaSMonitoringServiceCacher(new IaaSMonitoringServiceLoader(iaaSMonitoringApi));
        } catch (Exception e) {
            logger.error("Cannot instantiate IasSClientApi:", e);
            throw new IaaSMonitoringServiceException(e);
        }
    }

    /**
     * Configure the monitoring module.
     * @param nsName Node Source name. 
     * @param options 
     */
    public void configure(String nsName, String options) {
        this.nsname = nsName;
        loader.configure(nsName, options);
    }

    @Override
    public String[] getHosts() throws IaaSMonitoringServiceException {
        return loader.getHosts();
    }

    @Override
    public String[] getVMs() throws IaaSMonitoringServiceException {
        return loader.getVMs();
    }

    @Override
    public String[] getVMs(String hostId) throws IaaSMonitoringServiceException {
        return loader.getVMs(hostId);
    }

    @Override
    public Map<String, String> getHostProperties(String hostId) throws IaaSMonitoringServiceException {
        return loader.getHostProperties(hostId);
    }

    @Override
    public Map<String, String> getVMProperties(String vmId) throws IaaSMonitoringServiceException {
        return loader.getVMProperties(vmId);
    }

    @Override
    public Map<String, Object> getSummary() throws IaaSMonitoringServiceException {

        Map<String, Object> summary = new HashMap<String, Object>();

        String[] hosts = this.getHosts();
        for (String host : hosts) {
            // Put host properties.
            try {
                Map<String, Object> hostinfo = Utils.convertToObjectMap(getHostProperties(host));

                // Put a list of VMs with their properties.
                Map<String, Object> vmsinfo = new HashMap<String, Object>();
                String[] vms = this.getVMs(host);
                for (String vm : vms) {
                    try {
                        vmsinfo.put(vm, this.getVMProperties(vm));
                    } catch (IaaSMonitoringServiceException e) {
                        // Ignore it.
                    }
                }
                hostinfo.put(VMS_INFO_KEY, vmsinfo);

                summary.put(host, hostinfo);

            } catch (IaaSMonitoringServiceException e) {
                // Ignore it.
            }
        }

        return summary;
    }

    @Override
    public Map<String, Object> getHostsSummary() throws IaaSMonitoringServiceException {
        Map<String, Object> summary = new HashMap<String, Object>();
        String[] hosts = this.getHosts();
        for (String host : hosts) {
            // Put host properties.
            try {
                Map<String, Object> hostinfo = Utils.convertToObjectMap(getHostProperties(host));
                summary.put(host, hostinfo);
            } catch (IaaSMonitoringServiceException e) {
                // Ignore it.
            }
        }
        return summary;
    }

    @Override
    public Map<String, Object> getVMsSummary() throws IaaSMonitoringServiceException {
        Map<String, Object> summary = new HashMap<String, Object>();
        String[] vms = this.getVMs();
        for (String vm : vms) {
            // Put vm properties.
            try {
                Map<String, String> vminfo = getVMProperties(vm);
                summary.put(vm, vminfo);
            } catch (IaaSMonitoringServiceException e) {
                // Ignore it.
            }
        }
        return summary;
    }

    @Override
    public Map<String, Object> getVendorDetails() throws Exception {
        return loader.getVendorDetails();
    }

    @Override
    public void registerNode(String nodeid, String jmxurl, NodeType type) {
        loader.registerNode(nodeid, jmxurl, type);
    }

    @Override
    public void unregisterNode(String nodeid, NodeType type) {
        loader.unregisterNode(nodeid, type);
    }
    
    public void shutDown() {
        loader.shutDown();
    }
}
