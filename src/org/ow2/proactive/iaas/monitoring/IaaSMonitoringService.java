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
import java.io.ByteArrayInputStream;
import org.ow2.proactive.iaas.utils.Utils;
import org.ow2.proactive.iaas.utils.JmxUtils;
import org.ow2.proactive.iaas.IaaSMonitoringApi;
import org.ow2.proactive.authentication.crypto.Credentials;

public class IaaSMonitoringService implements
        IaaSMonitoringServiceMBean, IaaSNodesListener {

    public static final String PROP_PA_SIGAR_JMX_URL = "proactive.sigar.jmx.url";
    
    private static final Logger logger = Logger.getLogger(IaaSMonitoringService.class);

    private Map<String, String> jmxSupportedNodes = new HashMap<String, String>();
    private IaaSMonitoringApi iaaSMonitoringApi;
    private Credentials credentials;
    
    public IaaSMonitoringService(IaaSMonitoringApi iaaSMonitoringApi, String credentials)
            throws IaaSMonitoringServiceException {
        try {
            this.iaaSMonitoringApi = iaaSMonitoringApi;
            this.credentials = Credentials.getCredentials(
                    new ByteArrayInputStream(credentials.getBytes()));
        } catch (Exception e) {
            logger.error("Cannot instantiate IasSClientApi:", e);
            throw new IaaSMonitoringServiceException(e);
        }
    }

    @Override
    public void registerNode(String nodeId, String jmxUrl, NodeType type) {
        logger.info("Registered node '" + nodeId + "' with jmxUrl '" + jmxUrl + "' of type '" + type + "'.");
        jmxSupportedNodes.put(nodeId, jmxUrl);
    }

    @Override
    public void unregisterNode(String nodeId) {
        jmxSupportedNodes.remove(nodeId);
        logger.info("Unregistered node '" + nodeId + "'.");
    }

    @Override
    public String[] getHosts() throws IaaSMonitoringServiceException {
        try {
            return iaaSMonitoringApi.getHosts();
        } catch (Exception e) {
            logger.error("Cannot retrieve the list of hosts.", e);
            throw new IaaSMonitoringServiceException(e);
        }
    }

    @Override
    public Map<String, String> getHostProperties(String hostId)
            throws IaaSMonitoringServiceException {
        try {
            Map<String, String> properties = iaaSMonitoringApi
                    .getHostProperties(hostId);
            
            if (jmxSupportedNodes.containsKey(hostId)) {
            	String jmxurl = jmxSupportedNodes.get(hostId);
                properties.put(PROP_PA_SIGAR_JMX_URL, jmxurl);
                Map<String, Object> jmxenv = JmxUtils.getROJmxEnv(credentials);
                Map<String, String> sigarProps = queryProps(jmxurl, jmxenv);
                properties.putAll(sigarProps);
            } else {
                logger.info("No RMNode running on the host '" + hostId + "'.");
            }
            return properties;
        } catch (Exception e) {
            throw new IaaSMonitoringServiceException(e);
        }

    }

    @Override
    public String[] getVMs() throws IaaSMonitoringServiceException {
        try {
            return iaaSMonitoringApi.getVMs();
        } catch (Exception e) {
            logger.error("Cannot retrieve the list of VMs.", e);
            throw new IaaSMonitoringServiceException(e);
        }

    }

    @Override
    public String[] getVMs(String hostId) throws IaaSMonitoringServiceException {
        try {
            return iaaSMonitoringApi.getVMs(hostId);
        } catch (Exception e) {
            logger.error("Cannot retrieve the list of VMs of the host: "
                    + hostId, e);
            throw new IaaSMonitoringServiceException(e);
        }
    }

    @Override
    public Map<String, String> getVMProperties(String vmId)
            throws IaaSMonitoringServiceException {
        try {
            Map<String, String> properties = iaaSMonitoringApi
                    .getVMProperties(vmId);
            
            if (jmxSupportedNodes.containsKey(vmId)) {
            	String jmxurl = jmxSupportedNodes.get(vmId);
                properties.put(PROP_PA_SIGAR_JMX_URL, jmxurl);
                Map<String, Object> jmxenv = JmxUtils.getROJmxEnv(credentials);
                Map<String, String> sigarProps = queryProps(jmxurl, jmxenv);
                properties.putAll(sigarProps);
            } else {
                logger.info("No RMNode running on the VM '" + vmId + "'.");
            }
            return properties;
        } catch (Exception e) {
            logger.error("Cannot retrieve properties of the VM: " + vmId, e);
            throw new IaaSMonitoringServiceException(e);
        }
    }

    @Override
	public Map<String, Object> getSummary() throws IaaSMonitoringServiceException {
		try {
		    
        	Map<String, Object> summary = new HashMap<String, Object>();
        	
        	String[] hosts = this.getHosts();
        	for (String host: hosts) {
        		// Put host properties.
        		Map<String, Object> hostinfo = Utils.convertToObjectMap(getHostProperties(host));
        		
        		// Put a list of VMs with their properties.
        		Map<String, Object> vmsinfo = new HashMap<String, Object>();
        		String[] vms = this.getVMs();
        		for (String vm: vms) {
        			vmsinfo.put(vm, this.getVMProperties(vm));
        		}
    			hostinfo.put("vmsinfo", vmsinfo);	
    			
    			summary.put(host,  hostinfo);
        	}
    	
			return summary;
		} catch (Exception e) {
			throw new IaaSMonitoringServiceException(e);
		}
	}
	
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
