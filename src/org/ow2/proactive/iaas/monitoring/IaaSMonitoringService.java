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
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.util.Properties;
import org.apache.log4j.Logger;
import java.io.FileInputStream;
import java.security.KeyException;
import java.io.FileNotFoundException;
import org.ow2.proactive.iaas.utils.Utils;
import org.ow2.proactive.iaas.IaaSMonitoringApi;
import org.ow2.proactive.authentication.crypto.Credentials;


public abstract class IaaSMonitoringService implements IaaSMonitoringServiceMBean, IaaSNodesListener {

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
     * Monitoring API.
     */
    protected IaaSMonitoringApi iaaSMonitoringApi;

    /**
     * Credentials to get connected to the RMNodes (information obtained from JMX Sigar MBeans).
     */
    protected Credentials credentialsSigar = null;

    /**
     * Use the Infrastructure API to get monitoring information.
     */
    protected Boolean useApi = false;

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
     * Constructor.
     * @param iaaSMonitoringApi
     * @throws IaaSMonitoringServiceException
     */
    public IaaSMonitoringService(IaaSMonitoringApi iaaSMonitoringApi) throws IaaSMonitoringServiceException {
        try {
            this.iaaSMonitoringApi = iaaSMonitoringApi;
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
        Boolean useApi = Utils.isPresentInParameters("useApi", options);
        Boolean showVMProcessesOnHost = Utils.isPresentInParameters("showVMProcessesOnHost", options);
        String credentialsPath = Utils.getValueFromParameters("cred", options);
        String hostsFile = Utils.getValueFromParameters("hostsfile", options);
        Boolean useRMNodeOnHost = Utils.isPresentInParameters("useRMNodeOnHost", options);
        Boolean useRMNodeOnVM = Utils.isPresentInParameters("useRMNodeOnVM", options);

        logger.debug(String.format(
                "Monitoring params: useRMNodeOnHost='%s', useRMNodeOnVM='%s', sigarCred='%s',"
                    + " hostsfile='%s', useApi='%b', showVMProcessesOnHost='%b'", useRMNodeOnHost,
                useRMNodeOnVM, credentialsPath, hostsFile, useApi, showVMProcessesOnHost));

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

        // Use API monitoring?
        this.useApi = useApi;

        // Show VM Processes information when getting host properties?
        this.showVMProcessesOnHost = showVMProcessesOnHost;

        // Assume RMNodes are running on hosts.
        this.useRMNodeOnHost = useRMNodeOnHost;

        // Assume RMNodes are running on VMs.
        this.useRMNodeOnVM = useRMNodeOnVM;

        // Set up hosts file path.
        if (hostsFile != null) {
            setHosts(hostsFile);
        } else {
            logger.warn("Hosts monitoring file not provided.");
        }
    }

    private void setHosts(String filepath) {
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
    public abstract String[] getHosts() throws IaaSMonitoringServiceException;

    @Override
    public abstract String[] getVMs() throws IaaSMonitoringServiceException;

    @Override
    public abstract String[] getVMs(String hostId) throws IaaSMonitoringServiceException;

    @Override
    public abstract Map<String, String> getHostProperties(String hostId)
            throws IaaSMonitoringServiceException;

    @Override
    public abstract Map<String, String> getVMProperties(String vmId) throws IaaSMonitoringServiceException;

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

    public void shutDown() {
    }
}
