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
package org.ow2.proactive.iaas.cloudstack;

import org.ow2.proactive.iaas.IaasInstance;
import org.ow2.proactive.resourcemanager.exception.RMException;
import org.ow2.proactive.resourcemanager.nodesource.common.Configurable;
import org.ow2.proactive.resourcemanager.nodesource.infrastructure.InfrastructureManager;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.log4j.Logger;
import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.core.util.ProActiveCounter;

/**
 *
 * Cloudstack Infrastructure
 *
 * @author The ProActive Team
 *
 */
public class CloudStackInfrastructure extends InfrastructureManager {

    private static final Logger logger = Logger.getLogger(CloudStackInfrastructure.class);

    private static final int TEN_MINUTES_TIMEOUT = 60000 * 10;
    private static final String NODE_NAME_FORMAT = "%s-node-%d";

    private Hashtable<String, IaasInstance> nodeNameToInstance = new Hashtable<String, IaasInstance>();

    @Configurable(description = "The maximum number of instances that this policy can start.")
    protected Integer maxNbOfInstances;

    @Configurable(description = "The URL where the Cloudstack Rest API is located.")
    protected String apiUrl = "http://localhost:8080/client/api";

    @Configurable(description = "The user's api key to query Cloudstack API.")
    protected String apiKey = "dQEdbQVukQYkzGl9O_sG5qknip0mnXBtPfVBaJMiZd5LbwNuf3HTNi8hfxzLcXm32auykyoHuV_PIkak2kLeuA";

    @Configurable(description = "The user's secret key to compute the signature of Cloudstack API queries.")
    protected String secretKey = "VV_w_yDEqST8ovh0mkQpDh8nXEzyMBsW0wFyCEhjneZazHIX8IcNCAgsjGF3p2ZzeVqyxYT6vwWJm6TSv5tdoQ";

    @Configurable(description = "The identifier of the service offering used for the instances.")
    protected String serviceOfferingId = "4fe8b730-f227-4693-8b5e-bf384c566853";

    @Configurable(description = "The identifier of the template used for the instances.")
    protected String templateId = "d93961c7-c8bf-4f36-b592-4c5f4ff7a780";

    @Configurable(description = "The identifier of the zone used for the instances.")
    protected String zoneId = "ff2169df-f439-4694-817c-31babf50df9f";

    @Configurable(description = "The location of the Resource Manager where new instances will register.")
    protected String rmAddress = "192.168.56.1";

    @Override
    protected void configure(Object... parameters) {
        maxNbOfInstances = Integer.parseInt(parameters[0].toString());
        apiUrl = (String) parameters[1];
        apiKey = (String) parameters[2];
        secretKey = (String) parameters[3];
        serviceOfferingId = (String) parameters[4];
        templateId = (String) parameters[5];
        zoneId = (String) parameters[6];
        rmAddress = (String) parameters[7];
    }

    @Override
    public void acquireNode() {
        if (!allowedToCreateMoreInstance()) {
            logger.info(String.format("Can not create more instance, limit reached (%d on %d).", nodeNameToInstance.size(), maxNbOfInstances));
            return;
        }

        CloudStackAPI api = new CloudStackAPI(apiUrl, apiKey, secretKey);

        String nodeSourceName = this.nodeSource.getName();
        String nodeName = String.format(NODE_NAME_FORMAT, nodeSourceName, ProActiveCounter.getUniqID());

        Map<String, String> args = new HashMap<String, String>();
        args.put(CloudStackAPI.CloudStackAPIConstants.InstanceParameters.TEMPLATE, templateId);
        args.put(CloudStackAPI.CloudStackAPIConstants.InstanceParameters.ZONE, zoneId);
        args.put(CloudStackAPI.CloudStackAPIConstants.InstanceParameters.SERVICE_OFFERING, serviceOfferingId);
        args.put(CloudStackAPI.CloudStackAPIConstants.InstanceParameters.NAME, nodeName);
        args.put(CloudStackAPI.CloudStackAPIConstants.InstanceParameters.USER_DATA, String.format("%s\n%s\n%s", rmAddress, nodeSourceName, nodeName));

        try {
            String nodeUrl = this.addDeployingNode(nodeName, "", "Deploying Cloudstack node ", TEN_MINUTES_TIMEOUT);

            IaasInstance instance = api.startInstance(args);
            nodeNameToInstance.put(nodeName, instance);
            logger.info("New Cloudstack instance started " + nodeUrl);

        } catch (Exception e) {
            logger.error("Failed to start Cloudstack instance", e);
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

        CloudStackAPI api = new CloudStackAPI(apiUrl, apiKey, secretKey);
        try {
            api.stopInstance(instance);
            nodeNameToInstance.remove(nodeName);
        } catch (Exception e) {
            throw new RMException("Failed to remove Cloudstack node", e);
        }
    }

    @Override
    protected void notifyAcquiredNode(Node node) throws RMException {
        logger.info("Cloudstack node has been acquired " + node.getNodeInformation().getName());
    }

    // required by PluginDescriptor#PluginDescriptor()
    @SuppressWarnings("unused")
    public String getDescription() {
        return "Handles nodes from Cloudstack.";
    }
}
