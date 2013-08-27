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


import static org.ow2.proactive.iaas.monitoring.IaasConst.*;

public class IaasMonitoringServiceSigarLoader implements IaasMonitoringChainable {

    /**
     * Flags in the options.
     */
    public static final String USE_SIGAR_FLAG = "useSigar";
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
     * List saving all JMX urls for VMs.
     */
    protected List<String> jmxSupportedVMs = new ArrayList<String>();

    /**
     * Use sigar monitoring.
     */
    protected Boolean useSigar = false;
    /**
     * Credentials to get connected to the RMNodes (information obtained from JMX Sigar MBeans).
     */
    protected Credentials credentialsSigar = null;

    /**
     * Show VM Processes information when getting host properties.
     */
    protected Boolean showVMProcessesOnHost = false;

    /**
     * Assume RMNodes are running on hosts.
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
    private Map<String, String> vmId2SigarJmxUrlCache = new HashMap<String, String>();
    private Integer vmId2SigarJmxUrlCacheMisses = 0;

    /**
     * Cache map containing vmid -> hostid (host where the vm is running).
     */
    private Map<String, String> vmId2hostIdCache = new HashMap<String, String>();
    private Integer vmId2hostIdCacheMisses = 0;

    /**
     * Constants to filter sigar properties.
     */
    private static int MASK_ALL = SigarClient.MASK_ALL;
    private static int MASK_VMPROC = SigarClient.MASK_VMPROC;
    private static int MASK_ALL_BUT_VMPROC = MASK_ALL & ~MASK_VMPROC;

    public static final Integer ERRORS_ALLOWED = 3;

    IaasMonitoringServiceSigarLoader() throws IaasMonitoringException {
    }

    @Override
    public void configure(String nsName, String options) throws IaasMonitoringException {
        Boolean useSigar = Utils.isPresentInParameters(USE_SIGAR_FLAG, options);
        Boolean showVMProcessesOnHost = Utils.isPresentInParameters(SHOW_VMPROCESSES_ON_HOST_FLAG, options);
        String credentialsPath = Utils.getValueFromParameters(CREDENTIALS_FLAG, options);
        String hostsFile = Utils.getValueFromParameters(HOSTSFILE_FLAG, options);
        Boolean useRMNodeOnHost = Utils.isPresentInParameters(USE_RMNODE_ON_HOST_FLAG, options);
        Boolean useRMNodeOnVM = Utils.isPresentInParameters(USE_RMNODE_ON_VM_FLAG, options);

        logger.info(String.format(
                "Monitoring params for SigarLoader: useRMNodeOnHost='%s', useRMNodeOnVM='%s', sigarCred='%s',"
                        + " hostsfile='%s', showVMProcessesOnHost='%b', useSigar='%b'", useRMNodeOnHost,
                useRMNodeOnVM, credentialsPath, hostsFile, showVMProcessesOnHost, useSigar));

        // Use Sigar monitoring? 
        // Set up credentials file path.
        if (useSigar && credentialsPath != null) {
            try {
                setCredentials(new File(credentialsPath));
            } catch (KeyException e) {
                throw new IaasMonitoringException("Credentials file did not load successfully: " +
                        credentialsPath, e);
            }
        } else {
            logger.warn("No credentials file for monitoring will be used.");
        }

        this.nsname = nsName;

        // Use Sigar monitoring.
        this.useSigar = useSigar;

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
        if (!useSigar)
            return;

        logger.info("Registered node '" + nodeId + "' with jmxUrl '" + jmxUrl + "' of type '" + type + "'.");
        if (type.equals(NodeType.HOST)) {
            jmxSupportedHosts.put(nodeId, jmxUrl);
        } else if (type.equals(NodeType.VM)) {
            jmxSupportedVMs.add(jmxUrl);
        } else {
            logger.error("Node '" + nodeId + "' has an incorrect type. Its JMX URL will not be registered.");
        }
    }

    @Override
    public void unregisterNode(String nodeId, String jmxUrl, NodeType type) {
        if (!useSigar)
            return;

        if (type.equals(NodeType.HOST)) {
            jmxSupportedHosts.remove(nodeId);
        } else if (type.equals(NodeType.VM)) {
            jmxSupportedVMs.remove(jmxUrl);
        } else {
            throw new RuntimeException("Invalid node type.");
        }

        logger.info("Unregistered node '" + nodeId + "' type '" + type + "'.");
    }

    @Override
    public String[] getHosts() throws IaasMonitoringException {
        logger.debug("[" + nsname + "]" + "Retrieving list of hosts...");
        HashSet<String> hosts = new HashSet<String>();

        if (useSigar)
            try {
                hosts.addAll(jmxSupportedHosts.keySet()); // From RM.
            } catch (Exception e) {
                logger.error("[" + nsname + "]" + "Cannot retrieve the list of hosts from JMX table.", e);
            }

        return hosts.toArray(new String[]{});
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

        return vms.toArray(new String[]{});
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

        return (new ArrayList<String>(vms)).toArray(new String[]{});
    }

    @Override
    public Map<String, String> getHostProperties(String hostId) throws IaasMonitoringException {

        logger.debug("[" + nsname + "]" + " Retrieving properties from host '" + hostId + "'...");

        int mask = (showVMProcessesOnHost ? MASK_ALL : MASK_ALL_BUT_VMPROC);

        return getHostPropertiesByMask(hostId, mask);

    }

    @Override
    public Map<String, String> getVMProperties(String vmId) throws IaasMonitoringException {

        Map<String, String> vmProps = new HashMap<String, String>();

        logger.debug("[" + nsname + "]" + "Retrieving properties for VM " + vmId);

        if (useRMNodeOnHost) {

            try {

                addExtraPropsComingFromHostProps(vmId, vmProps);

                if (useRMNodeOnVM)
                    addExtraPropsComingFromRmNodeProps(vmId, vmProps);


            } catch (Exception e) {
                logger.warn("[" + nsname + "]" + "Cannot retrieve VM info for VM: " + vmId, e);
            }
        }

        return vmProps;
    }

    private void addExtraPropsComingFromRmNodeProps(String vmId, Map<String, String> vmProps) {

        Map<String, Object> rmNodeVMsMaps = getRMNodeVMsMaps(vmId);

        Map<String, String> vmPropsFromRmNode = VMsMerger.getExtraVMPropertiesFromVMRMNodes(vmId, vmProps, rmNodeVMsMaps);

        addCacheEntryToVmId2SigarJmxUrl(vmId, vmPropsFromRmNode);

        vmProps.putAll(vmPropsFromRmNode);

    }

    private void addCacheEntryToVmId2SigarJmxUrl(String vmId, Map<String, String> vmPropsFromRmNode) {

        String jmxurl = getJmxUrlFromProps(vmPropsFromRmNode);

        if (jmxurl != null) {

            vmId2SigarJmxUrlCache.put(vmId, jmxurl);
            logger.debug("Added cache for: vm " + vmId + " <-> jmxurl " + jmxurl + "...");

        } else {

            logger.warn("Could not find jmxurl for: " + vmId + " (" + vmId2SigarJmxUrlCache + ")");

        }

    }

    private void addExtraPropsComingFromHostProps(String vmId, Map<String, String> properties) throws IaasMonitoringException {

        Map<String, Object> hostSummary = getPropsOfHostHostingVmOrAllHostsProps(vmId);
        Map<String, String> vmPropsFromHost = VMsMerger.getExtraVMPropsFromHostProps(vmId, hostSummary);

        addCacheEntryTovmId2HostId(vmId, vmPropsFromHost);
        properties.putAll(vmPropsFromHost);

    }

    private String getJmxUrlFromProps(Map<String, String> props) {
        return props.get(IaasConst.P_SIGAR_JMX_URL.toString());
    }

    private void addCacheEntryTovmId2HostId(String vmId, Map<String, String> vmPropsFromHost) {

        String host = getHostNameFromVmProps(vmPropsFromHost);

        if (host != null) {
            vmId2hostIdCache.put(vmId, host);
            logger.debug("Added cache for: vm " + vmId + " <-> host " + host + "...");
        } else {
            logger.warn("Could not find host for: " + vmId + " (" + vmId2hostIdCache + ")");
        }
    }

    private String getHostNameFromVmProps(Map<String, String> vmPropsFromHost) {
        return vmPropsFromHost.get(IaasConst.P_VM_HOST.toString());
    }

    private String[] getVMProcesses(String hostId) throws IaasMonitoringException {

        Map<String, String> props = getHostPropertiesByMask(hostId, MASK_VMPROC);
        String vms = props.get(VMPLister.VMS_KEY);

        if (vms != null)
            return vms.split(VMPLister.VMS_SEPARATOR);
        else
            return new String[]{};

    }

    public Map<String, String> getHostPropertiesByMask(String hostId, int mask)
            throws IaasMonitoringException {

        Map<String, String> hostProps = new HashMap<String, String>();

        if (useSigar)

            try {

                String jmxurl = jmxSupportedHosts.get(hostId);

                if (jmxurl != null) {
                    hostProps.put(IaasConst.P_SIGAR_JMX_URL.toString(), jmxurl);
                    Map<String, String> rmNodeProps = retrieveRmNodeProps(jmxurl, mask);
                    hostProps.putAll(rmNodeProps);
                } else {
                    logger.debug("[" + nsname + "]" + "No RMNode running on the host '" + hostId + "'.");
                }

            } catch (Exception e) {
                logger.warn("[" + nsname + "]" + "Cannot retrieve RMNode properties from host: " + hostId, e);
            }

        return hostProps;

    }

    private Map<String, Object> getPropsOfHostHostingVmOrAllHostsProps(String vmIdTarget)
            throws IaasMonitoringException {

        Map<String, Object> hostsMap = new HashMap<String, Object>();
        String host = null;

        /*
         * Cached hostid for this vmIdTarget?
         */
        host = vmId2hostIdCache.get(vmIdTarget);

        if (host != null) {

            try {
                Map<String, Object> hostinfo = Utils.convertToObjectMap(getHostProperties(host));
                hostsMap.put(host, hostinfo);
            } catch (IaasMonitoringException e) {
                logger.warn("Could not get RMNode monitoring info for host " + host, e);
            }

        } else {

            logger.info("vm<->host cache miss for VM " + vmIdTarget + ": " + host);
            vmId2hostIdCacheMisses++;

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
     *
     * @param vmIdTarget
     * @return the map with node:properties
     */
    private Map<String, Object> getRMNodeVMsMaps(String vmIdTarget) {

        Map<String, Object> rmNodesProps = new HashMap<String, Object>();
        Boolean needToReScanAllRmNodes;

        String cachedJmxUrl = vmId2SigarJmxUrlCache.get(vmIdTarget);

        if (cachedJmxUrl != null) {

            Map<String, String> rmNodeProps = retrieveRmNodeProps(cachedJmxUrl, MASK_ALL_BUT_VMPROC);

            if (areRmNodePropsInvalid(rmNodeProps)) {
                logger.info("Invalid RMNode props (too many fails), node reconnected? " + vmIdTarget + ": " + cachedJmxUrl);
                needToReScanAllRmNodes = true;
            } else {
                rmNodesProps.put(vmIdTarget, rmNodeProps);
                needToReScanAllRmNodes = false;
            }

        } else {

            logger.info("vm<->jmxurl cache miss for VM " + vmIdTarget);
            needToReScanAllRmNodes = true;

        }

        if (needToReScanAllRmNodes) {

            notifyCacheMissForVmId2SigarJmxUrlCache(vmIdTarget);

            getAllRmNodesProps(rmNodesProps);

        }

        return rmNodesProps;
    }

    private void notifyCacheMissForVmId2SigarJmxUrlCache(String vmIdTarget) {
        vmId2SigarJmxUrlCacheMisses++;
        vmId2SigarJmxUrlCache.remove(vmIdTarget);
    }

    private boolean areRmNodePropsInvalid(Map<String, String> rmNodeProps) {
        Integer errorsWhileRetrieving = getNumberOfErrorsWhileGettingProps(rmNodeProps);
        return rmNodeProps.isEmpty() || errorsWhileRetrieving > ERRORS_ALLOWED;
    }

    private void getAllRmNodesProps(Map<String, Object> sigarsMap) {
        for (String j : jmxSupportedVMs) {
            logger.debug("Querying props of sigar VM at: " + j);
            Map<String, String> sigarProps = null;
            sigarProps = retrieveRmNodeProps(j, MASK_ALL_BUT_VMPROC);
            logger.debug("Properties: " + sigarProps);
            if (sigarProps != null) {
                sigarsMap.put(j, sigarProps);
            } else {
                logger.warn("Could not get properties for RMNode@VM:" + j);
            }
        }
    }

    private Integer getNumberOfErrorsWhileGettingProps(Map<String, String> p) {

        String errors = p.get(P_DEBUG_NUMBER_OF_ERRORS.toString());

        if (errors == null)
            throw new IllegalStateException("Field " + P_DEBUG_NUMBER_OF_ERRORS.toString() + " in props is null, and it should never be null: " + p);

        Integer nErrors;

        try {
            nErrors = Integer.parseInt(errors);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Field " + P_DEBUG_NUMBER_OF_ERRORS.toString() + " contains an invalid integer: " + p);
        }

        return nErrors;

    }

    /**
     * Query all the monitoring properties of a target RMNode using
     * the remote Sigar MBean.
     *
     * @param jmxurl URL of the RM JMX Server.
     * @return a map of properties of the target RMNode.
     */
    private Map<String, String> retrieveRmNodeProps(String jmxurl, int mask) {
        Map<String, Object> outp = new HashMap<String, Object>();
        try {
            Map<String, Object> env = JmxUtils.getRoJmxEnv(credentialsSigar);
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

    public Integer getVmId2SigarJmxUrlCacheMisses() {
        return vmId2SigarJmxUrlCacheMisses;
    }

    public Integer getVmId2hostIdCacheMisses() {
        return vmId2hostIdCacheMisses;
    }
}
