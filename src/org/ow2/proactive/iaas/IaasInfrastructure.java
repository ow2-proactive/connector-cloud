/*
 *  *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2011 INRIA/University of
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
 *  * $$PROACTIVE_INITIAL_DEV$$
 */
package org.ow2.proactive.iaas;

import java.io.File;
import java.security.KeyException;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;

import javax.management.MBeanRegistrationException;

import org.apache.log4j.Logger;
import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.core.util.ProActiveCounter;
import org.ow2.proactive.iaas.monitoring.IaaSMonitoringService;
import org.ow2.proactive.iaas.monitoring.IaaSMonitoringServiceException;
import org.ow2.proactive.iaas.monitoring.MBeanExposer;
import org.ow2.proactive.iaas.monitoring.NodeType;
import org.ow2.proactive.jmx.naming.JMXTransportProtocol;
import org.ow2.proactive.resourcemanager.exception.RMException;
import org.ow2.proactive.resourcemanager.nodesource.common.Configurable;
import org.ow2.proactive.resourcemanager.nodesource.infrastructure.InfrastructureManager;
import org.ow2.proactive.resourcemanager.utils.RMNodeStarter;


public abstract class IaasInfrastructure extends InfrastructureManager {

    @Configurable(description = "The maximum number of instances that infrastructure can start.")
    protected Integer maxNbOfInstances;

    @Configurable(description = "The URL where Rest API is located.")
    protected String iaasApiUrl = "";

    protected static final Logger logger = Logger.getLogger(IaasInfrastructure.class);
    protected Hashtable<String, IaasInstance> nodeNameToInstance = new Hashtable<String, IaasInstance>();

    protected static final int TEN_MINUTES_TIMEOUT = 60000 * 10;
    protected static final String NODE_NAME_FORMAT = "%s-node-%d";
    protected static final Map<String, Object> USE_CONFIGURED_VALUES = Collections.emptyMap();

    protected abstract IaasApi getAPI();

    protected abstract Map<String, String> getInstanceParams(String nodeName, String nodeSourceName,
            Map<String, ?> nodeConfiguration);
    
    protected IaaSMonitoringService iaaSMonitoringService;
    protected MBeanExposer mbeanExposer;
    
    public IaasInfrastructure() {
    }

    protected void configure(Object... parameters) {
        maxNbOfInstances = Integer.parseInt(parameters[0].toString());
        iaasApiUrl = parameters[1].toString();
        startIaaSMonitoringService(parameters);
    }

    @Override
    public void acquireNode() {
        acquireNode(USE_CONFIGURED_VALUES);
    }

    @Override
    public void acquireNodes(int n, Map<String, ?> nodeConfiguration) {
        for (int i = 0; i < n; i++) {
            acquireNode(nodeConfiguration);
        }
    }

    private void acquireNode(Map<String, ?> nodeConfiguration) {
        if (!allowedToCreateMoreInstance()) {
            logger.info(String.format("Can not create more instance, limit reached (%d on %d).",
                    nodeNameToInstance.size(), maxNbOfInstances));
            return;
        }

        IaasApi api = getAPI();

        logger.debug("Starting a " + api.getName() + " instance wih parameters " + nodeConfiguration);

        String nodeSourceName = this.nodeSource.getName();
        String nodeName = String.format(NODE_NAME_FORMAT, nodeSourceName, ProActiveCounter.getUniqID());

        String nodeUrl = this.addDeployingNode(nodeName, "", "Deploying " + api.getName() + " node ",
                TEN_MINUTES_TIMEOUT);
        try {
            IaasInstance instance = api.startInstance(getInstanceParams(nodeName, nodeSourceName,
                    nodeConfiguration));
            nodeNameToInstance.put(nodeName, instance);
            logger.info("New " + api.getName() + " instance started " + nodeUrl);
        } catch (Exception e) {
            this.declareDeployingNodeLost(nodeUrl,
                    "Failed to start " + api.getName() + " instance: " + e.getMessage());
            logger.error("Failed to start " + api.getName() + " instance", e);
        } finally {
        	try {
        		api.disconnect();
        	} catch(Exception e) {
        		logger.warn("Could not disconnect from the API.", e);
        	}
        }
    }

    @Override
    public void acquireAllNodes() {
        while (allowedToCreateMoreInstance()) {
            acquireNode();
        }
    }

    private boolean allowedToCreateMoreInstance() {
        return nodeNameToInstance.size() < maxNbOfInstances;
    }

    @Override
    public void removeNode(Node node) throws RMException {
        String nodeName = node.getNodeInformation().getName();
        IaasInstance instance = nodeNameToInstance.get(nodeName);
        IaasApi api = getAPI();

        try {
            api.stopInstance(instance);
            nodeNameToInstance.remove(nodeName);
        } catch (Exception e) {
            throw new RMException("Failed to remove " + api.getName() + " node", e);
        } finally {
        	try {
        		api.disconnect();
        	} catch(Exception e) {
        		logger.warn("Could not disconnect from the API.", e);
        	}
        }
        unregisterWithIaaSMonitoringService(node);
    }

    @Override
    protected void notifyAcquiredNode(Node node) throws RMException {
        logger.info("Node has been acquired " + node.getNodeInformation().getName());
        registerWithIaaSMonitoringService(node);
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    

    // required by PluginDescriptor#PluginDescriptor()
    @SuppressWarnings("unused")
    public String getDescription() {
        return "Handles nodes from " + this.getClass().getSimpleName() + ".";
    }

    protected String getFromNodeConfigurationOrDefault(Map<String, ?> nodeConfiguration, String key,
            String defaultValue) {
        Object fromNodeConfiguration = nodeConfiguration.get(key);
        if (fromNodeConfiguration instanceof String && !((String) fromNodeConfiguration).isEmpty()) {
            return (String) fromNodeConfiguration;
        }
        return defaultValue;
    }
    
    private void startIaaSMonitoringService(Object... parameters) {
        if (isIaaSMonitoringServiceEnabled(parameters)) {
            String nodeSource = getNodeSourceName(parameters);
            String credentialsPath = getCredentialsPath(parameters);
            
            if (nodeSource == null) {
                throw new RuntimeException(
                        "Required paramater NodeSource not specified for IaaSMonitoringService, expected -ns <node-source-name>");
            }
            
            try {
                IaaSMonitoringService mbean = new IaaSMonitoringService(
                        (IaaSMonitoringApi) getAPI());
                
                if (credentialsPath != null) {
                    mbean.setCredentials(new File(credentialsPath));
                }
                
                MBeanExposer ep = new MBeanExposer();
                ep.registerMBeanLocally(nodeSource, mbean);
                
                iaaSMonitoringService = mbean;
                mbeanExposer = ep;

            } catch (MBeanRegistrationException e) {
                logger.error("Could not register IaaS Monitoring MBean.", e);
            } catch (IaaSMonitoringServiceException e) {
                logger.error("Cannot initialize IaaSMonitoringService MBean:", e);
            } catch (KeyException e) {
                logger.error("Problem while processing credentials file: ", e);
            }
        } else {
            logger.debug("Host monitoring: disabled (monitorHostDisabled).");
        }
    }
    
    private boolean isIaaSMonitoringServiceEnabled(Object... parameters) {
        for (Object param : parameters) {
            if ("monitorHostEnabled".equals(param.toString())) {
                return true;
            }
        }
        return false;
    }
    
    private String getCredentialsPath(Object... params) {
        return getValueFromParameters("-cred", params);
    }
    
    private String getNodeSourceName(Object... params) {
        return getValueFromParameters("-ns", params);
    }
    
    protected String getValueFromParameters(String flag, Object... params) {
        String value = null;
        for (int index = 0; index < params.length; index++) {
            if (flag.equals(params[index].toString())) {
                if (params.length < (index + 1)) {
                    value = params[index + 1].toString();
                }
                break;
            }
        }
        return value;
    }
    
    /*
     * Register a PANode with Infrastructure Monitoring Service, if enabled.
     */
    private void registerWithIaaSMonitoringService(Node node) {
        if (iaaSMonitoringService != null) {
            try {
                String jmxurlro = node.getProperty(RMNodeStarter.JMX_URL
                        + JMXTransportProtocol.RO);
                String token = node
                        .getProperty(RMNodeStarter.NODE_ACCESS_TOKEN);
                NodeType type = ("IAASHOST".equals(token)) ? NodeType.HOST
                        : NodeType.VM;
                
                logger.info("New node registered (NAME='" + 
                    node.getNodeInformation().getName() + 
                    "', TOKEN='" + token + "').");
                
                iaaSMonitoringService.registerNode(node.getNodeInformation()
                        .getName(), jmxurlro, type);

            } catch (ProActiveException e) {
                logger.error("Error while getting node properties.", e);
            }
        }
    }

    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    /*
     * Unregister the specified PANode from Infrastructure Monitoring Service,
     * if the service is enabled.
     */
    private void unregisterWithIaaSMonitoringService(Node node) {
        if (iaaSMonitoringService != null) {
            iaaSMonitoringService.unregisterNode(node.getNodeInformation()
                    .getName());
        }
    }
    
    
    
    @Override
    public void shutDown(){
    	if (mbeanExposer!=null) { 
    		try {
				mbeanExposer.stop();
			} catch (Exception e) {
				logger.warn("Error while stopping mbeanExposer.", e);
			}
    	}
    }
    
    
}
