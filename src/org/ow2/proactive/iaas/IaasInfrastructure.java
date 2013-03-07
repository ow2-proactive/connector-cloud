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

import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;

import org.apache.log4j.Logger;
import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.core.util.ProActiveCounter;
import org.ow2.proactive.resourcemanager.exception.RMException;
import org.ow2.proactive.resourcemanager.nodesource.common.Configurable;
import org.ow2.proactive.resourcemanager.nodesource.infrastructure.InfrastructureManager;


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

    public IaasInfrastructure() {
    }

    protected void configure(Object... parameters) {
        maxNbOfInstances = Integer.parseInt(parameters[0].toString());
        iaasApiUrl = parameters[1].toString();
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
        }
    }

    @Override
    protected void notifyAcquiredNode(Node node) throws RMException {
        logger.info("Node has been acquired " + node.getNodeInformation().getName());
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
}
