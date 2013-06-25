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
import org.ow2.proactive.iaas.IaasMonitoringApi;


public class IaasMonitoringService implements IaasMonitoringServiceMBean, IaasNodesListener {

    /** Logger. */
    private static final Logger logger = Logger.getLogger(IaasMonitoringService.class);

    /**
     * Loader for monitoring information.
     */
    protected IaasMonitoringServiceCacher loader;
    
    /**
     * Name of the Node Source being monitored.
     */
    protected String nsname;

    /**
     * Constructor.
     * @param iaaSMonitoringApi
     * @throws IaasMonitoringException
     */
    public IaasMonitoringService(IaasMonitoringApi iaaSMonitoringApi) throws IaasMonitoringException {
        try {
            loader = new IaasMonitoringServiceCacher(new IaasMonitoringServiceLoader(iaaSMonitoringApi));
        } catch (Exception e) {
            logger.error("Cannot instantiate IasSClientApi:", e);
            throw new IaasMonitoringException(e);
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
    public String[] getHosts() throws IaasMonitoringException {
        return loader.getHosts();
    }

    @Override
    public String[] getVMs() throws IaasMonitoringException {
        return loader.getVMs();
    }

    @Override
    public String[] getVMs(String hostId) throws IaasMonitoringException {
        return loader.getVMs(hostId);
    }

    @Override
    public Map<String, String> getHostProperties(String hostId) throws IaasMonitoringException {
        return loader.getHostProperties(hostId);
    }

    @Override
    public Map<String, String> getVMProperties(String vmId) throws IaasMonitoringException {
        return loader.getVMProperties(vmId);
    }

    @Override
    public Map<String, Object> getSummary() throws IaasMonitoringException {

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
                    } catch (IaasMonitoringException e) {
                        // Ignore it.
                    }
                }
                hostinfo.put(IaasConst.P_HOST_VMS.get(), vmsinfo);

                summary.put(host, hostinfo);

            } catch (IaasMonitoringException e) {
                // Ignore it.
            }
        }

        return summary;
    }

    @Override
    public Map<String, Object> getHostsSummary() throws IaasMonitoringException {
        Map<String, Object> summary = new HashMap<String, Object>();
        String[] hosts = this.getHosts();
        for (String host : hosts) {
            // Put host properties.
            try {
                Map<String, Object> hostinfo = Utils.convertToObjectMap(getHostProperties(host));
                summary.put(host, hostinfo);
            } catch (IaasMonitoringException e) {
                // Ignore it.
            }
        }
        return summary;
    }

    @Override
    public Map<String, Object> getVMsSummary() throws IaasMonitoringException {
        Map<String, Object> summary = new HashMap<String, Object>();
        String[] vms = this.getVMs();
        for (String vm : vms) {
            // Put vm properties.
            try {
                Map<String, String> vminfo = getVMProperties(vm);
                summary.put(vm, vminfo);
            } catch (IaasMonitoringException e) {
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
