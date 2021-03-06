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

import org.apache.log4j.Logger;
import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.core.util.ProActiveCounter;
import org.ow2.proactive.iaas.monitoring.*;
import org.ow2.proactive.jmx.naming.JMXTransportProtocol;
import org.ow2.proactive.resourcemanager.exception.RMException;
import org.ow2.proactive.resourcemanager.nodesource.NodeSource;
import org.ow2.proactive.resourcemanager.nodesource.common.Configurable;
import org.ow2.proactive.resourcemanager.nodesource.infrastructure.InfrastructureManager;
import org.ow2.proactive.resourcemanager.utils.RMNodeStarter;

import javax.management.MBeanRegistrationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;


public abstract class IaasInfrastructure extends InfrastructureManager {

    @Configurable(description = "The maximum number of instances that infrastructure can start.")
    protected Integer maxNbOfInstances;

    @Configurable(description = "The URL where Rest API is located.")
    protected String iaasApiUrl = "";

    @Configurable(description = "Monitoring options (use 'monitoringDisabled' if not required).")
    protected String monitoringOptions = "";

    protected static final int NB_OF_BASE_PARAMETERS = 3;

    protected static final Logger logger = Logger.getLogger(IaasInfrastructure.class);
    protected Hashtable<String, IaasInstance> nodeNameToInstance = new Hashtable<String, IaasInstance>();

    protected static final String NODE_NAME_FORMAT = "%s-node-%d";
    protected static final Map<String, Object> USE_CONFIGURED_VALUES = new HashMap<String, Object>();

    protected long DEPLOYING_NODES_TIMEOUT_MS = 60000 * 10;

    protected abstract IaasApi getAPI();

    protected abstract Map<String, String> getInstanceParams(String nodeName, String nodeSourceName,
                                                             Map<String, ?> nodeConfiguration);

    protected IaasMonitoringService iaaSMonitoringService;
    protected MBeanExposer mbeanExposer;

    public IaasInfrastructure() {
    }

    protected void configure(Object... parameters) {
        maxNbOfInstances = Integer.parseInt(parameters[0].toString());
        iaasApiUrl = parameters[1].toString();
        monitoringOptions = parameters[2].toString();
    }

    public void setNodeSource(NodeSource ns) {
        super.setNodeSource(ns);
        startIaaSMonitoringService(monitoringOptions);
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
                                               DEPLOYING_NODES_TIMEOUT_MS);

        try {
            Map<String, String> instanceParams = getInstanceParams(nodeName, nodeSourceName,
                                                                   nodeConfiguration);
            IaasInstance instance = api.startInstance(instanceParams);
            nodeNameToInstance.put(nodeName, instance);
            logger.info("New " + api.getName() + " instance started " + nodeUrl);
        } catch (Exception e) {
            this.declareDeployingNodeLost(nodeUrl,
                                          "Failed to start " + api.getName() + " instance: " + e.getMessage());
            logger.error("Failed to start " + api.getName() + " instance", e);
        } finally {
            try {
                api.disconnect();
            } catch (Exception e) {
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
            } catch (Exception e) {
                logger.warn("Could not disconnect from the API.", e);
            }
        }
        unregisterWithIaaSMonitoringService(node, NodeType.VM);
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

    private void startIaaSMonitoringService(String options) {

        String nodeSourceName = nodeSource.getName();

        if (isIaaSMonitoringServiceEnabled(options)) {

            logger.info("Monitoring of insfrastructure '" + nodeSourceName + "': enabled.");

            try {
                IaasMonitoringService monitService = new IaasMonitoringService(nodeSourceName);

                monitService.configure((IaasMonitoringApi) getAPI(), options);

                MBeanExposer exp = new MBeanExposer();
                exp.registerAsMBean(nodeSourceName, new DynamicIaasMonitoringMBean(monitService));

                iaaSMonitoringService = monitService;
                mbeanExposer = exp;

            } catch (MBeanRegistrationException e) {
                logger.error("Could not register IaaS Monitoring MBean.", e);
            } catch (IOException e) {
                logger.error("Could not initialize IaaS Monitoring MBean.", e);
            } catch (IaasMonitoringException e) {
                logger.error("Cannot initialize IaaSMonitoringService MBean:", e);
            }
        } else {
            logger.info("Monitoring of insfrastructure '" + nodeSourceName + "': disabled.");
        }
    }

    private boolean isIaaSMonitoringServiceEnabled(String options) {
        if (isPresentInParameters("monitoringEnabled", options)) {
            return true;
        } else if (isPresentInParameters("monitoringDisabled", options)) {
            return false;
        } else {
            throw new RuntimeException("Wrong monitoring options. "
                                               + "Should at least specify 'monitoringEnabled' or 'monitoringDisabled'.");
        }
    }

    private boolean isPresentInParameters(String flag, String options) {
        if (options.contains(flag)) {
            return true;
        }
        return false;
    }

    /*
     * Register a PANode with Infrastructure Monitoring Service, if enabled.
     */
    private void registerWithIaaSMonitoringService(Node node) {
        if (iaaSMonitoringService != null) {

            String nodeid = node.getNodeInformation().getName();

            try {
                String jmxurlro = node.getProperty(RMNodeStarter.JMX_URL + JMXTransportProtocol.RO);
                iaaSMonitoringService.registerNode(nodeid, jmxurlro, NodeType.VM);
            } catch (ProActiveException e) {
                logger.error("Error while trying to register node.", e);
            }
        }
    }

    /*
     * Unregister the specified PANode from Infrastructure Monitoring Service, if the service is
     * enabled.
     */
    private void unregisterWithIaaSMonitoringService(Node node, NodeType type) {
        if (iaaSMonitoringService != null) {

            String nodeid = node.getNodeInformation().getName();
            String jmxurlro = null;

            try {
                jmxurlro = node.getProperty(RMNodeStarter.JMX_URL + JMXTransportProtocol.RO);
                iaaSMonitoringService.unregisterNode(nodeid, jmxurlro, type);
            } catch (ProActiveException e) {
                logger.error("Error while trying to unregister node.", e);
            }
        }
    }

    @Override
    public void shutDown() {
        if (mbeanExposer != null) {
            try {
                mbeanExposer.stop();
            } catch (Exception e) {
                logger.warn("Error while stopping mbeanExposer.", e);
            }
        }

        if (iaaSMonitoringService != null) {
            try {
                iaaSMonitoringService.shutDown();
            } catch (Exception e) {
                logger.warn("Error while shutting down the monitoring service.", e);
            }
        }
    }
}
