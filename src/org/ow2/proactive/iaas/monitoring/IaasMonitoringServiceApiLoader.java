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
import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import org.ow2.proactive.iaas.utils.Utils;
import org.ow2.proactive.iaas.IaasMonitoringApi;

public class IaasMonitoringServiceApiLoader implements IaasMonitoringChainable {

    /**
     * Flag in the options that tells if the API of the infrastructure provider will be used.
     */
    public static final String USE_API_FLAG = "useApi";
    
    /** 
     * Logger. 
     */
    private static final Logger logger = Logger.getLogger(IaasMonitoringServiceApiLoader.class);

    /**
     * Monitoring API.
     */
    private IaasMonitoringApi iaaSMonitoringApi;

    /**
     * Use the Infrastructure API to get monitoring information.
     */
    protected Boolean useApi = false;

    /**
     * Name of the Node Source being monitored.
     */
    protected String nsname;

    IaasMonitoringServiceApiLoader(IaasMonitoringApi iaaSMonitoringApi) throws IaasMonitoringException {
        this.iaaSMonitoringApi = iaaSMonitoringApi;
    }

    @Override
    public void configure(String nsName, String options) {
        Boolean useApi = Utils.isPresentInParameters(USE_API_FLAG, options);

        logger.info(String.format("Monitoring params for APILoader: useApi='%b'", useApi));

        this.nsname = nsName;

        // Use API monitoring?
        this.useApi = useApi;
    }

    @Override
    public void registerNode(String nodeId, String jmxUrl, NodeType type) {
        // Ignore.
    }

    @Override
    public void unregisterNode(String nodeId, NodeType type) {
        // Ignore.
    }

    @Override
    public String[] getHosts() throws IaasMonitoringException {
        logger.debug("[" + nsname + "]" + "Retrieving list of hosts for IaaS node source: " + nsname);
        HashSet<String> hosts = new HashSet<String>();
        if (useApi)
            try {
                hosts.addAll(Arrays.asList(iaaSMonitoringApi.getHosts())); // From API.
            } catch (Exception e) {
                logger.error("[" + nsname + "]" + "Cannot retrieve the list of hosts from API.", e);
            }

        return hosts.toArray(new String[] {});
    }

    @Override
    public String[] getVMs() throws IaasMonitoringException {
        logger.debug("[" + nsname + "]" + "Retrieving list of VMs from IaaS node source: " + nsname);
        HashSet<String> vms = new HashSet<String>();

        String[] hosts = getHosts();
        for (String hostid : hosts) {
            try {
                Set<String> vmsAtHost = new HashSet<String>(Arrays.asList(getVMs(hostid)));
                vms.addAll(vmsAtHost);
            } catch (Exception e) {
                logger.warn("[" + nsname + "]" + "Cannot retrieve the list of VMs from host: " + hostid, e);
            }
        }

        return vms.toArray(new String[] {});
    }

    @Override
    public String[] getVMs(String hostId) throws IaasMonitoringException {
        logger.debug("[" + nsname + "]" + "Retrieving list of VMs in host " + hostId +
            " from IaaS node source " + nsname);

        Set<String> vms = new HashSet<String>();

        if (useApi)
            try {
                List<String> fromApi = Arrays.asList(iaaSMonitoringApi.getVMs(hostId));
                vms.addAll(fromApi);
            } catch (Exception e) {
                logger.warn("[" + nsname + "]" + "Cannot retrieve the list of VMs from API of the host: " +
                    hostId, e);
            }

        return (new ArrayList<String>(vms)).toArray(new String[] {});
    }

    @Override
    public Map<String, String> getHostProperties(String hostId) throws IaasMonitoringException {
        logger.debug("[" + nsname + "]" + "Retrieving properties from host " + hostId +
            " from IaaS node source " + nsname);
        Map<String, String> properties = new HashMap<String, String>();

        if (useApi)
            try {
                Map<String, String> apiprops = iaaSMonitoringApi.getHostProperties(hostId);
                properties.putAll(apiprops);
            } catch (Exception e) {
                logger.warn("[" + nsname + "]" + "Cannot retrieve IaaS API properties from host: " + hostId,
                        e);
            }

        return properties;
    }

    @Override
    public Map<String, String> getVMProperties(String vmId) throws IaasMonitoringException {
        logger.debug("[" + nsname + "]" + "Retrieving properties for VM " + vmId);
        Map<String, String> properties = new HashMap<String, String>();

        if (useApi)
            try {
                Map<String, String> apiprops = iaaSMonitoringApi.getVMProperties(vmId);
                properties.putAll(apiprops);
            } catch (Exception e) {
                logger.warn("[" + nsname + "]" + "Cannot retrieve IaaS API properties for VM: " + vmId, e);
            }

        return properties;
    }

    @Override
    public Map<String, Object> getVendorDetails() throws IaasMonitoringException {
        logger.debug("[" + nsname + "] Retrieving provider details.");
        Map<String, Object> properties = new HashMap<String, Object>();

        if (useApi)
            try {
                Map<String, Object> apiprops = iaaSMonitoringApi.getVendorDetails();
                properties.putAll(apiprops);
            } catch (Exception e) {
                logger.warn("[" + nsname + "]" + "Cannot retrieve IaaS API properties. ", e);
            }

        return properties;
    }

    public void shutDown() {
    }
}
