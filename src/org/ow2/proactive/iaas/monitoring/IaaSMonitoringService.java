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
import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.io.BufferedReader;
import org.apache.log4j.Logger;
import java.io.FileInputStream;
import java.security.KeyException;
import java.io.FileNotFoundException;
import org.ow2.proactive.iaas.utils.Utils;
import org.ow2.proactive.iaas.utils.JmxUtils;
import org.ow2.proactive.iaas.IaaSMonitoringApi;
import org.ow2.proactive.authentication.crypto.Credentials;
import org.ow2.proactive.iaas.monitoring.vmprocesses.VMPLister;

public class IaaSMonitoringService implements
        IaaSMonitoringServiceMBean, IaaSNodesListener {

    /** 
     * Key used for referencing the URL of the Sigar MBean of the entity (host/vm). 
     */
    public static final String PROP_PA_SIGAR_JMX_URL = "proactive.sigar.jmx.url";
    
    /** 
     * Key that belongs to the host properties map. 
     * Its value contains information about all its VMs. 
     */
    public static final String VMS_INFO_KEY = "vmsinfo";
    
    /** Logger. */
    private static final Logger logger = Logger.getLogger(IaaSMonitoringService.class);
    
    /** 
     * Map to save JMX urls per host.
     * Each JMX url is used to exploit monitoring information that can
     * be obtained by using the Sigar MBean available and running in the host.
     */
    private Map<String, String> jmxSupportedHosts = new HashMap<String, String>();
    
    /** 
     * Map to save JMX urls per VM.
     */
    private Map<String, String> jmxSupportedVMs = new HashMap<String, String>();
    
    /**
     * Monitoring API.
     */
    private IaaSMonitoringApi iaaSMonitoringApi;
    
    /**
     * Credentials to get connected to the RMNodes (information obtained from JMX Sigar MBeans).
     */
    private Credentials credentials = null;
    

    public IaaSMonitoringService(IaaSMonitoringApi iaaSMonitoringApi)
            throws IaaSMonitoringServiceException {
        try {
            this.iaaSMonitoringApi = iaaSMonitoringApi;
        } catch (Exception e) {
            logger.error("Cannot instantiate IasSClientApi:", e);
            throw new IaaSMonitoringServiceException(e);
        }
    }
    
    public void setHosts(String filepath) throws IaaSMonitoringServiceException {
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(filepath));
        } catch (FileNotFoundException e) {
            throw new IaaSMonitoringServiceException(
                    "Could not load hosts monitoring file: " + filepath, e);
        }
        
        String line;
        try {
            while ((line = br.readLine()) != null) {
                if (line.trim().startsWith("#")) // comment
                    continue;
                
                String[] nameAndUrl = line.trim().split("="); // host=jmxurl
                if (nameAndUrl.length!=2)
                    continue;
                
                this.registerNode(nameAndUrl[0], nameAndUrl[1], NodeType.HOST);
            }
        } catch (IOException e) {
            throw new IaaSMonitoringServiceException("Error reading hosts monitoring file.", e);
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                logger.warn("Could not close properly the hosts monitoring file.", e);
            }
        }
    }
    
    public void setCredentials(File fcredentials) throws KeyException {
        if (fcredentials != null) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(fcredentials); 
                credentials = Credentials.getCredentials(fis);
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
    /**
     * Register a node in either the host or the VM JMX table. 
     * This allows to keep track of the monitorable entities (either host or vm).
     * @param nodeId id of the node, must be the same as the VM-id provided by the IaaS API.
     * @param jmxUrl jmx url of the RMNode running in the entity (host or vm).
     * @param type of the entity, either host or vm.
     */
    public void registerNode(String nodeId, String jmxUrl, NodeType type) {
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
    public void unregisterNode(String nodeId) {
        // Since they are both RMNodes, they can't have the same name.
        jmxSupportedHosts.remove(nodeId);
        jmxSupportedVMs.remove(nodeId);
        logger.info("Unregistered node '" + nodeId + "'.");
    }

    @Override
    /**
     * Get the list of host IDs from:
     * - Infrastructure API.
     * - JMX table of registered hosts.
     * @return the list of host Ids in the infrastructure.
     */
    public String[] getHosts() throws IaaSMonitoringServiceException {
        HashSet<String> hosts = new HashSet<String>();
        try {
            hosts.addAll(Arrays.asList(iaaSMonitoringApi.getHosts()));  // From API.
        } catch (Exception e) {
            logger.error("Cannot retrieve the list of hosts from API.", e);
        }
        
        try {
            hosts.addAll(jmxSupportedHosts.keySet());                   // From RM.
        } catch (Exception e) {
            logger.error("Cannot retrieve the list of hosts from JMX table.", e);
        }
        
        return hosts.toArray(new String[]{});
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
        HashSet<String> vms = new HashSet<String>();
        
        String[] hosts = getHosts();
        for (String hostid: hosts){
            try {
                Set<String> vmsAtHost = new HashSet<String>(Arrays.asList(getVMs(hostid)));
                vms.addAll(vmsAtHost);
            } catch (Exception e) {
                logger.warn("Cannot retrieve the list of VMs from host: " + hostid, e);
            }
        }
        
        vms.addAll(jmxSupportedVMs.keySet());                       // From RM.

        return vms.toArray(new String[]{});
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
        
        Set<String> vms = new HashSet<String> ();
        
        try {
            List<String> fromApi = Arrays.asList(iaaSMonitoringApi.getVMs(hostId));
            vms.addAll(fromApi);
        } catch (Exception e) {
            logger.warn("Cannot retrieve the list of VMs from API of the host: " + hostId, e);
        }
        
        try {
            List<String> fromHostProc = Arrays.asList(getVMsFromHostProcesses(hostId));
            vms.addAll(fromHostProc);
        } catch (Exception e) {
            logger.warn("Cannot retrieve the list of VMs of the host: " + hostId, e);
        }
        
        return (new ArrayList<String>(vms)).toArray(new String[]{});
    }

    /*
    private String[] getAllVMsFromHostProcesses() throws IaaSMonitoringServiceException {
        Set<String> vms = new HashSet<String>();
        
        for (String host: jmxSupportedHosts.keySet()){
            String jmxurl = jmxSupportedHosts.get(host);
            Map<String, Object> jmxenv = JmxUtils.getROJmxEnv(credentials);
            Map<String, String> sigarProps = queryProps(jmxurl, jmxenv);
            String v = sigarProps.get(VMPLister.VMS_KEY);
            if (v != null) {
                String[] split = v.split(VMPLister.VMS_SEPARATOR);
                vms.addAll(Arrays.asList(split));
            } 
        }
        
        return (new ArrayList<String>(vms)).toArray(new String[]{});
    }
    */
    
    private String[] getVMsFromHostProcesses(String hostId) throws IaaSMonitoringServiceException {
        Map<String, String> props = getHostProperties(hostId);
        String vms = props.get(VMPLister.VMS_KEY);
        if (vms != null) {
            return vms.split(VMPLister.VMS_SEPARATOR);
        } else {
            return new String[]{};
        }
    }
    
    @Override
    public Map<String, String> getHostProperties(String hostId)
            throws IaaSMonitoringServiceException {
        
        Map<String, String> properties = new HashMap<String, String>();
        
        try {
            Map<String, String> apiprops = iaaSMonitoringApi
                    .getHostProperties(hostId);
            properties.putAll(apiprops);
        } catch (Exception e) {
            logger.warn("Cannot retrieve IaaS API properties from host: " + 
                    hostId, e);
        }
        
        try {
            if (credentials == null) {
                return properties;
            }
            
            if (jmxSupportedHosts.containsKey(hostId)) {
            	String jmxurl = jmxSupportedHosts.get(hostId);
                properties.put(PROP_PA_SIGAR_JMX_URL, jmxurl);
                Map<String, Object> jmxenv = JmxUtils.getROJmxEnv(credentials);
                Map<String, String> sigarProps = queryProps(jmxurl, jmxenv);
                properties.putAll(sigarProps);
            } else {
                logger.debug("No RMNode running on the host '" + hostId + "'.");
            }
            
        } catch (Exception e) {
            logger.warn("Cannot retrieve Sigar properties from host: " + hostId, e);
        }

        if (properties.isEmpty()) {
            throw new IaaSMonitoringServiceException("No property found for host: " + hostId);
        }
        
        return properties;
    }
    
    @Override
    public Map<String, String> getVMProperties(String vmId)
            throws IaaSMonitoringServiceException {
        
        Map<String, String> properties = new HashMap<String, String>();
        
        try {
            Map<String, String> apiprops = iaaSMonitoringApi
                    .getVMProperties(vmId);
            properties.putAll(apiprops);
        } catch (Exception e) {
            logger.warn("Cannot retrieve IaaS API properties from VM: " + 
                    vmId, e);
        }
        
        try {
            if (credentials == null) {
                return properties;
            }
            
            if (jmxSupportedVMs.containsKey(vmId)) {
            	String jmxurl = jmxSupportedVMs.get(vmId);
                properties.put(PROP_PA_SIGAR_JMX_URL, jmxurl);
                Map<String, Object> jmxenv = JmxUtils.getROJmxEnv(credentials);
                Map<String, String> sigarProps = queryProps(jmxurl, jmxenv);
                properties.putAll(sigarProps);
            } else {
                logger.info("No RMNode running on the VM '" + vmId + "'.");
            }
            
        } catch (Exception e) {
            logger.warn("Cannot retrieve Sigar properties from VM: " + vmId, e);
        }
        
        if (properties.isEmpty()){
            throw new IaaSMonitoringServiceException("No property found for VM: " + vmId);
        }
        
        return properties;
    }

    @Override
	public Map<String, Object> getSummary() throws IaaSMonitoringServiceException {
		    
    	Map<String, Object> summary = new HashMap<String, Object>();
    	
    	String[] hosts = this.getHosts();
    	for (String host: hosts) {
    		// Put host properties.
    	    try {
        		Map<String, Object> hostinfo = Utils.convertToObjectMap(getHostProperties(host));
        		
        		// Put a list of VMs with their properties.
        		Map<String, Object> vmsinfo = new HashMap<String, Object>();
        		String[] vms = this.getVMs(host);
        		for (String vm: vms) {
        		    try {
            			vmsinfo.put(vm, this.getVMProperties(vm));
        		    } catch(IaaSMonitoringServiceException e) {
        		        // Ignore it.
        		    }
        		}
    			hostinfo.put(VMS_INFO_KEY, vmsinfo);	
    			
    			summary.put(host, hostinfo);
    			
    	    } catch(IaaSMonitoringServiceException e) {
    	        // Ignore it.
    	    }
    	}
	
		return summary;
	}
	
    /**
     * Query all the monitoring properties of a target RMNode using 
     * the remote Sigar MBean.
     * @param jmxurl URL of the RM JMX Server.
     * @param env Jmx initialization map.
     * @return a map of properties of the target RMNode.
     */
	private Map<String, String> queryProps(String jmxurl, Map<String, Object> env){
		Map<String, Object> outp = new HashMap<String, Object>();
		try {
			outp = JmxUtils.getSigarProperties(jmxurl, env);
		} catch (Exception e) {
			logger.warn("Could not get sigar properties from '" + jmxurl + "'.", e);
		}
		return Utils.convertToStringMap(outp);
	}

}
