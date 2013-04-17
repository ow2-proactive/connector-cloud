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
import org.ow2.proactive.iaas.utils.JmxUtils;
import org.ow2.proactive.iaas.utils.VMsMerger;
import org.ow2.proactive.iaas.IaaSMonitoringApi;
import org.ow2.proactive.iaas.monitoring.vmprocesses.VMPLister;


public class IaaSMonitoringServiceLoader extends IaaSMonitoringService {

    /** Logger. */
    private static final Logger logger = Logger.getLogger(IaaSMonitoringServiceLoader.class);

    public IaaSMonitoringServiceLoader(IaaSMonitoringApi iaaSMonitoringApi)
            throws IaaSMonitoringServiceException {
        super(iaaSMonitoringApi);
    }
    
    @Override
    /**
     * Get the list of host IDs from:
     * - Infrastructure API.
     * - JMX table of registered hosts.
     * @return the list of host Ids in the infrastructure.
     */
    public String[] getHosts() throws IaaSMonitoringServiceException {
        logger.debug("Retrieving list of hosts from IaaS node source: " + nsname);
        HashSet<String> hosts = new HashSet<String>();
        if (useApi)
            try {
                hosts.addAll(Arrays.asList(iaaSMonitoringApi.getHosts())); // From API.
            } catch (Exception e) {
                logger.error("Cannot retrieve the list of hosts from API.", e);
            }

        if (credentialsSigar != null)
            try {
                hosts.addAll(jmxSupportedHosts.keySet()); // From RM.
            } catch (Exception e) {
                logger.error("Cannot retrieve the list of hosts from JMX table.", e);
            }

        return hosts.toArray(new String[] {});
    }

    @Override
    /**
     * Get the list of Ids of each VM in the infrastructure.
     * It collects the VMs as listed by these sources:
     * - The method getVM(host) for each host listed by getHosts(), 
     * - All the VMs registered in the JMX table for VMs.
     * @return the list of VM Ids in the infrastructure.
     */
    public String[] getVMs() throws IaaSMonitoringServiceException {
        logger.debug("Retrieving list of VMs from IaaS node source: " + nsname);
        HashSet<String> vms = new HashSet<String>();

        String[] hosts = getHosts();
        for (String hostid : hosts) {
            try {
                Set<String> vmsAtHost = new HashSet<String>(Arrays.asList(getVMs(hostid)));
                vms.addAll(vmsAtHost);
            } catch (Exception e) {
                logger.warn("Cannot retrieve the list of VMs from host: " + hostid, e);
            }
        }

        if (credentialsSigar != null)
            vms.addAll(jmxSupportedVMs.keySet()); // From RM.

        return vms.toArray(new String[] {});
    }

    @Override
    /**
     * Get the list of VMs in the host provided.
     * The list of VMs is obtained from the following sources:
     * - Infrastructure API.
     * - Parsing of processes running in the host.
     * NOTE: we assume that the RMNode running in each VM has the same name as the ID of the VM
     * as shown by the infrastructure API and the id identified by means of the VM process running
     * in the host. This way, every VM listed in jmxSupportedVMs will be already included in the 
     * VMProcesses list.
     */
    public String[] getVMs(String hostId) throws IaaSMonitoringServiceException {
        logger.debug("Retrieving list of VMs in host " + hostId + " from IaaS node source " + nsname);

        Set<String> vms = new HashSet<String>();

        if (useApi)
            try {
                List<String> fromApi = Arrays.asList(iaaSMonitoringApi.getVMs(hostId));
                vms.addAll(fromApi);
            } catch (Exception e) {
                logger.warn("Cannot retrieve the list of VMs from API of the host: " + hostId, e);
            }

        if (useVMProcesses)
            try {
                List<String> fromHostProc = Arrays.asList(getVMsFromHostProcesses(hostId));
                vms.addAll(fromHostProc);
            } catch (Exception e) {
                logger.warn("Cannot retrieve the list of VMs of the host: " + hostId, e);
            }

        return (new ArrayList<String>(vms)).toArray(new String[] {});
    }

    private String[] getVMsFromHostProcesses(String hostId) throws IaaSMonitoringServiceException {
        Map<String, String> props = getHostProperties(hostId);
        String vms = props.get(VMPLister.VMS_KEY);
        if (vms != null) {
            return vms.split(VMPLister.VMS_SEPARATOR);
        } else {
            return new String[] {};
        }
    }

    @Override
    public Map<String, String> getHostProperties(String hostId) throws IaaSMonitoringServiceException {
        logger.debug("Retrieving properties from host " + hostId + " from IaaS node source " + nsname);
        Map<String, String> properties = new HashMap<String, String>();

        if (useApi)
            try {
                Map<String, String> apiprops = iaaSMonitoringApi.getHostProperties(hostId);
                properties.putAll(apiprops);
            } catch (Exception e) {
                logger.warn("Cannot retrieve IaaS API properties from host: " + hostId, e);
            }

        if (credentialsSigar != null)
            try {

                if (jmxSupportedHosts.containsKey(hostId)) {
                    String jmxurl = jmxSupportedHosts.get(hostId);
                    properties.put(PROP_PA_SIGAR_JMX_URL, jmxurl);
                    if (resolveSigar) {
                        Map<String, Object> jmxenv = JmxUtils.getROJmxEnv(credentialsSigar);
                        Map<String, String> sigarProps = queryProps(jmxurl, jmxenv);
                        properties.putAll(sigarProps);
                    }
                } else {
                    logger.debug("No RMNode running on the host '" + hostId + "'.");
                }
            } catch (Exception e) {
                logger.warn("Cannot retrieve Sigar properties from host: " + hostId, e);
            }

        return properties;
    }

    @Override
    public Map<String, String> getVMProperties(String vmId) throws IaaSMonitoringServiceException {
        logger.debug("Retrieving properties from VM " + vmId + " in IaaS node source " + nsname);
        Map<String, String> properties = new HashMap<String, String>();

        if (useApi)
            try {
                Map<String, String> apiprops = iaaSMonitoringApi.getVMProperties(vmId);
                properties.putAll(apiprops);
            } catch (Exception e) {
                logger.warn("Cannot retrieve IaaS API properties from VM: " + vmId, e);
            }

        if (credentialsSigar != null)
            try {

                if (jmxSupportedVMs.containsKey(vmId)) {
                    String jmxurl = jmxSupportedVMs.get(vmId);
                    properties.put(PROP_PA_SIGAR_JMX_URL, jmxurl);
                    if (resolveSigar) {
                        Map<String, Object> jmxenv = JmxUtils.getROJmxEnv(credentialsSigar);
                        Map<String, String> sigarProps = queryProps(jmxurl, jmxenv);
                        properties.putAll(sigarProps);
                    }
                } else {
                    logger.info("No RMNode running on the VM '" + vmId + "'.");
                }

            } catch (Exception e) {
                logger.warn("Cannot retrieve Sigar properties from VM: " + vmId, e);
            }

        if (useVMProcesses) {
            try {
                Map<String, String> newProps = VMsMerger.enrichVMProperties(properties, getHostsSummary());
                properties.putAll(newProps);
            } catch (Exception e) {
                logger.warn("Cannot retrieve VMProcesses info for VM: " + vmId, e);
            }
        }

        return properties;
    }

    /**
     * Query all the monitoring properties of a target RMNode using 
     * the remote Sigar MBean.
     * @param jmxurl URL of the RM JMX Server.
     * @param env Jmx initialization map.
     * @return a map of properties of the target RMNode.
     */
    private Map<String, String> queryProps(String jmxurl, Map<String, Object> env) {
        Map<String, Object> outp = new HashMap<String, Object>();
        try {
            outp = JmxUtils.getSigarProperties(jmxurl, env, useVMProcesses);
        } catch (Exception e) {
            logger.warn("Could not get sigar properties from '" + jmxurl + "'.", e);
        }
        return Utils.convertToStringMap(outp);
    }

}
