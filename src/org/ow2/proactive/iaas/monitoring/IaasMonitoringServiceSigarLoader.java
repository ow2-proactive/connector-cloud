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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
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
import org.ow2.proactive.authentication.crypto.Credentials;
import org.ow2.proactive.iaas.monitoring.vmprocesses.VMPLister;


public class IaasMonitoringServiceSigarLoader implements IaasMonitoringChainable {

    /**
     * Flags in the options.
     */
    public static final String SHOW_VMPROCESSES_ON_HOST_FLAG = "showVMProcessesOnHost";
    public static final String CREDENTIALS_FLAG = "cred";
    public static final String HOSTSFILE_FLAG = "hostsfile";
    public static final String USE_RMNODE_ON_HOST_FLAG = "useRMNodeOnHost";
    public static final String USE_RMNODE_ON_VM_FLAG = "useRMNodeOnVM";

    /** 
     * Logger. 
     */
    private static final Logger logger = Logger.getLogger(IaasMonitoringServiceSigarLoader.class);

    /** 
     * Map to save JMX urls per host.
     * Each JMX url is used to exploit monitoring information that can
     * be obtained by using the Sigar MBean available and running in the host.
     */
    protected Map<String, String> jmxSupportedHosts = new HashMap<String, String>();

    /** 
     * Map to save JMX urls per VM.
     */
    protected Map<String, String> jmxSupportedVMs = new HashMap<String, String>();

    /**
     * Credentials to get connected to the RMNodes (information obtained from JMX Sigar MBeans).
     */
    protected Credentials credentialsSigar = null;

    /**
     * Show VM Processes information when getting host properties.
     */
    protected Boolean showVMProcessesOnHost = false;

    /**
     *  Assume RMNodes are running on hosts.
     */
    protected Boolean useRMNodeOnHost = false;

    /**
     * Assume RMNodes are running on VMs.
     */
    protected Boolean useRMNodeOnVM = false;

    /**
     * Name of the Node Source being monitored.
     */
    protected String nsname;

    /**
     * Cache map containing vmid -> RMNode jmxurl.
     */
    private Map<String, String> vmId2SigarJmxUrl = new HashMap<String, String>();

    /**
     * Cache map containing vmid -> hostid (host where the vm is running).
     */
    private Map<String, String> vmId2hostId = new HashMap<String, String>();

    /** 
     * Constants to filter sigar properties.
     */
    private static int MASK_ALL = FormattedSigarMBeanClient.MASK_ALL;
    private static int MASK_VMPROC = FormattedSigarMBeanClient.MASK_VMPROC;
    private static int MASK_ALL_BUT_VMPROC = MASK_ALL & ~MASK_VMPROC;

    IaasMonitoringServiceSigarLoader() throws IaasMonitoringException {
    }

    @Override
    public void configure(String nsName, String options) {
        Boolean showVMProcessesOnHost = Utils.isPresentInParameters(SHOW_VMPROCESSES_ON_HOST_FLAG, options);
        String credentialsPath = Utils.getValueFromParameters(CREDENTIALS_FLAG, options);
        String hostsFile = Utils.getValueFromParameters(HOSTSFILE_FLAG, options);
        Boolean useRMNodeOnHost = Utils.isPresentInParameters(USE_RMNODE_ON_HOST_FLAG, options);
        Boolean useRMNodeOnVM = Utils.isPresentInParameters(USE_RMNODE_ON_VM_FLAG, options);

        logger.info(String.format(
                "Monitoring params for SigarLoader: useRMNodeOnHost='%s', useRMNodeOnVM='%s', sigarCred='%s',"
                    + " hostsfile='%s', showVMProcessesOnHost='%b'", useRMNodeOnHost, useRMNodeOnVM,
                credentialsPath, hostsFile, showVMProcessesOnHost));

        // Use Sigar monitoring? 
        // Set up credentials file path.
        if (credentialsPath != null) {
            try {
                setCredentials(new File(credentialsPath));
            } catch (KeyException e) {
                logger.warn("Credentials file did not load successfully. No JMX Sigar monitoring will take place.");
            }
        } else {
            logger.warn("Credentials file not provided. No JMX Sigar monitoring will take place.");
        }

        this.nsname = nsName;

        // Show VM Processes information when getting host properties?
        this.showVMProcessesOnHost = showVMProcessesOnHost;

        // Assume RMNodes are running on hosts.
        this.useRMNodeOnHost = useRMNodeOnHost;

        // Assume RMNodes are running on VMs.
        this.useRMNodeOnVM = useRMNodeOnVM;

        // Set up hosts file path.
        if (hostsFile != null) {
            setHostsToMonitor(hostsFile);
        } else {
            logger.warn("Hosts monitoring file not provided.");
        }
    }

    private void setHostsToMonitor(String filepath) {
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(filepath));
        } catch (FileNotFoundException e) {
            logger.error("Could not find hosts monitoring file: " + filepath);
        } catch (IOException e) {
            logger.error("Could not load hosts monitoring file: " + filepath);
        }

        for (Object name : prop.keySet()) {
            this.registerNode(name.toString(), prop.getProperty(name.toString()), NodeType.HOST);
        }
    }

    private void setCredentials(File fcredentials) throws KeyException {
        if (fcredentials != null) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(fcredentials);
                credentialsSigar = Credentials.getCredentials(fis);
                logger.info("Monitoring credentials set correctly.");
            } catch (FileNotFoundException e) {
                throw new KeyException(e);
            } catch (KeyException e) {
                throw new KeyException(e);
            } catch (IOException e) {
                throw new KeyException(e);
            } finally {
                if (fis != null)
                    try {
                        fis.close();
                    } catch (IOException e) {
                        // Ignore it.
                    }
            }
        }
    }

    @Override
    public void registerNode(String nodeId, String jmxUrl, NodeType type) {
        if (credentialsSigar == null)
            return;

        logger.info("Registered node '" + nodeId + "' with jmxUrl '" + jmxUrl + "' of type '" + type + "'.");
        if (type.equals(NodeType.HOST)) {
            jmxSupportedHosts.put(nodeId, jmxUrl);
        } else if (type.equals(NodeType.VM)) {
            jmxSupportedVMs.put(nodeId, jmxUrl);
        } else {
            logger.error("Node '" + nodeId + "' has an incorrect type. Its JMX URL will not be registered.");
        }
    }

    @Override
    public void unregisterNode(String nodeId, NodeType type) {
        if (credentialsSigar == null)
            return;

        if (type.equals(NodeType.HOST)) {
            jmxSupportedHosts.remove(nodeId);
        } else if (type.equals(NodeType.VM)) {
            jmxSupportedVMs.remove(nodeId);
        } else {
            throw new RuntimeException("Invalid node type.");
        }

        logger.info("Unregistered node '" + nodeId + "' type '" + type + "'.");
    }

    @Override
    public String[] getHosts() throws IaasMonitoringException {
        logger.debug("[" + nsname + "]" + "Retrieving list of hosts...");
        HashSet<String> hosts = new HashSet<String>();

        if (credentialsSigar != null)
            try {
                hosts.addAll(jmxSupportedHosts.keySet()); // From RM.
            } catch (Exception e) {
                logger.error("[" + nsname + "]" + "Cannot retrieve the list of hosts from JMX table.", e);
            }

        return hosts.toArray(new String[] {});
    }

    @Override
    public String[] getVMs() throws IaasMonitoringException {
        logger.debug("[" + nsname + "]" + "Retrieving list of VMs...");
        HashSet<String> vms = new HashSet<String>();

        String[] hosts = getHosts();
        for (String hostid : hosts) {
            try {
                String[] vmsList = getVMs(hostid);

                Set<String> vmsAtHost = new HashSet<String>(Arrays.asList(vmsList));

                vms.addAll(vmsAtHost);
            } catch (Exception e) {
                logger.warn("[" + nsname + "]" + "Cannot retrieve the list of VMs from host: " + hostid, e);
            }
        }

        // VMs from the jmxSupportedVMs will not be listed.
        //if (credentialsSigar != null)
        //    vms.addAll(jmxSupportedVMs.keySet()); // From RM.

        return vms.toArray(new String[] {});
    }

    @Override
    public String[] getVMs(String hostId) throws IaasMonitoringException {
        logger.debug("[" + nsname + "]" + "Retrieving list of VMs in host '" + hostId + "'...");

        Set<String> vms = new HashSet<String>();

        if (useRMNodeOnHost)
            try {
                String[] vmsList = getVMProcesses(hostId);
                List<String> fromHostProc = Arrays.asList(vmsList);
                vms.addAll(fromHostProc);
            } catch (Exception e) {
                logger.warn("[" + nsname + "]" + "Cannot retrieve the list of VMs of the host: " + hostId, e);
            }

        return (new ArrayList<String>(vms)).toArray(new String[] {});
    }

    @Override
    public Map<String, String> getHostProperties(String hostId) throws IaasMonitoringException {
        logger.debug("[" + nsname + "]" + "Retrieving properties from host " + hostId + "'...");
        int mask = (showVMProcessesOnHost ? MASK_ALL : MASK_ALL_BUT_VMPROC);
        return getHostPropertiesSelective(hostId, mask);
    }

    @Override
    public Map<String, String> getVMProperties(String vmId) throws IaasMonitoringException {
        logger.debug("[" + nsname + "]" + "Retrieving properties for VM " + vmId);
        Map<String, String> properties = new HashMap<String, String>();

        if (useRMNodeOnHost) {
            try {

                Map<String, Object> hostSummary = getHostsMaps(vmId, vmId2hostId);
                Map<String, String> newProps;

                // Get VM properties coming from the Host processes about this VM.
                newProps = VMsMerger.getExtraVMProperties(vmId, properties, hostSummary);
                if (newProps.containsKey(IaasConst.P_VM_HOST.get())) {
                    String host = newProps.get(IaasConst.P_VM_HOST.get());
                    vmId2hostId.put(vmId, host);
                    logger.debug("Added cache for: vm " + vmId + " <-> host " + host + "...");
                } else {
                    logger.warn("Could not find host for: " + vmId + " (" + vmId2hostId + ")");
                }
                properties.putAll(newProps);

                /*
                 * Scenario: 1. Many VMs with Sigar(RMNodes) running on them. 2. Many VMs told by
                 * the IaaS API. 3. Which VM corresponds to which Sigar? -> Needed MAC matching (MAC
                 * address is obtainable from the parameters of the VM process, in the host).
                 */
                if (useRMNodeOnVM) {
                    // Get properties of all VMs (from Sigar of each) unless cache exists.
                    Map<String, Object> sigarsMap = getRMNodeVMsMaps(vmId, vmId2SigarJmxUrl);

                    newProps = VMsMerger.getExtraVMPropertiesFromVMRMNodes(vmId, properties, sigarsMap);

                    if (newProps.containsKey(IaasConst.P_SIGAR_JMX_URL.get())) {
                        String jmxurl = newProps.get(IaasConst.P_SIGAR_JMX_URL.get());
                        vmId2SigarJmxUrl.put(vmId, jmxurl);
                        logger.debug("Added cache for: vm " + vmId + " <-> jmxurl " + jmxurl + "...");
                    } else {
                        logger.warn("Could not find jmxurl for: " + vmId + " (" + vmId2SigarJmxUrl + ")");
                    }
                    
                    properties.putAll(newProps);
                }

            } catch (Exception e) {
                logger.warn("[" + nsname + "]" + "Cannot retrieve VMProcesses info for VM: " + vmId, e);
            }
        }

        return properties;
    }

    private String[] getVMProcesses(String hostId) throws IaasMonitoringException {
        Map<String, String> props = getHostPropertiesSelective(hostId, MASK_VMPROC);
        String vms = props.get(VMPLister.VMS_KEY);
        if (vms != null) {
            return vms.split(VMPLister.VMS_SEPARATOR);
        } else {
            return new String[] {};
        }
    }

    public Map<String, String> getHostPropertiesSelective(String hostId, int mask)
            throws IaasMonitoringException {

        Map<String, String> properties = new HashMap<String, String>();

        if (credentialsSigar != null)
            try {

                if (jmxSupportedHosts.containsKey(hostId)) {
                    String jmxurl = jmxSupportedHosts.get(hostId);
                    properties.put(IaasConst.P_SIGAR_JMX_URL.get(), jmxurl);
                    Map<String, Object> jmxenv = JmxUtils.getROJmxEnv(credentialsSigar);
                    Map<String, String> sigarProps = queryProps(jmxurl, jmxenv, mask);
                    properties.putAll(sigarProps);
                } else {
                    logger.debug("[" + nsname + "]" + "No RMNode running on the host '" + hostId + "'.");
                }
            } catch (Exception e) {
                logger.warn("[" + nsname + "]" + "Cannot retrieve Sigar properties from host: " + hostId, e);
            }

        return properties;
    }

    private Map<String, Object> getHostsMaps(String vmIdTarget, Map<String, String> cache)
            throws IaasMonitoringException {

        Map<String, Object> hostsMap = new HashMap<String, Object>();
        String host = null;

        /*
         * Cached hostid for this vmIdTarget?
         */
        host = cache.get(vmIdTarget);

        if (host != null) {
            try {
                Map<String, Object> hostinfo = Utils.convertToObjectMap(getHostProperties(host));
                hostsMap.put(host, hostinfo);
            } catch (IaasMonitoringException e) {
                logger.warn("Could not get RMNode monitoring info for host " + host, e);
            }
        } else {
            logger.info("vm<->host cache miss for VM " + vmIdTarget + ": " + host);
            /*
             * No cached hostid for this vmIdTarget. Retrieve all.
             */
            String[] hosts = this.getHosts();
            for (String h : hosts) {
                // Put host properties.
                try {
                    Map<String, Object> hostinfo = Utils.convertToObjectMap(getHostProperties(h));
                    hostsMap.put(h, hostinfo);
                } catch (IaasMonitoringException e) {
                    logger.warn("Could not get RMNode monitoring info for host " + host, e);
                }
            }
        }

        return hostsMap;
    }

    /**
     * Get Sigar maps of all VMs (maps with properties of an RMNode).
     * This is optimized to retrieve only one VM set of properties if there is already
     * a relation vmIdTarget<->jmxurl (that can be found using mac resolution only).
     * @param vmIdTarget
     * @param cache
     * @return the map with node:properties
     */
    private Map<String, Object> getRMNodeVMsMaps(String vmIdTarget, Map<String, String> cache) {
        Map<String, Object> sigarsMap = new HashMap<String, Object>();
        String jmxurl = null;

        /*
         * Cached Sigar jmxurl for this vmIdTarget.
         */
        jmxurl = cache.get(vmIdTarget);

        if (jmxurl != null) {
            Map<String, Object> jmxenv = JmxUtils.getROJmxEnv(credentialsSigar);
            sigarsMap.put(vmIdTarget, queryProps(jmxurl, jmxenv, MASK_ALL_BUT_VMPROC));
        } else {
            /*
             * No cached Sigar jmxurl for this vmIdTarget. Retrieve all.
             */
            logger.info("vm<->jmxurl cache miss for VM " + vmIdTarget + ": " + jmxurl);
            for (String sigar : jmxSupportedVMs.keySet()) {
                String j = jmxSupportedVMs.get(sigar);
                logger.debug("Querying props of: " + sigar + " at " + j);
                Map<String, String> sigarProps = null;
                Map<String, Object> jmxenv = JmxUtils.getROJmxEnv(credentialsSigar);
                sigarProps = queryProps(j, jmxenv, MASK_ALL_BUT_VMPROC);
                logger.debug("Properties: " + sigarProps);
                if (sigarProps != null) {
                    sigarsMap.put(sigar, sigarProps);
                } else {
                    logger.warn("Could not get properties for RMNode@VM " + sigar + ":" + j);
                }
            }
        }

        return sigarsMap;
    }

    /**
     * Query all the monitoring properties of a target RMNode using 
     * the remote Sigar MBean.
     * @param jmxurl URL of the RM JMX Server.
     * @param env Jmx initialization map.
     * @return a map of properties of the target RMNode.
     */
    private Map<String, String> queryProps(String jmxurl, Map<String, Object> env, int mask) {
        Map<String, Object> outp = new HashMap<String, Object>();
        try {
            outp = JmxUtils.getSigarProperties(jmxurl, env, mask);
        } catch (Exception e) {
            logger.warn("[" + nsname + "]" + "Could not get sigar properties for '" + jmxurl + "'.", e);
        }
        return Utils.convertToStringMap(outp);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> getVendorDetails() throws IaasMonitoringException {
        return (Map<String, Object>) Collections.EMPTY_MAP;
    }

    public void shutDown() {
    }
}