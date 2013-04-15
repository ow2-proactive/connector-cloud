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
package org.ow2.proactive.iaas.nova;

import java.io.File;
import java.net.URI;
import java.util.Map;
import java.util.HashMap;
import org.apache.log4j.Logger;
import org.ow2.proactive.iaas.IaasApi;
import org.ow2.proactive.iaas.IaasPolicy;
import org.ow2.proactive.iaas.IaasInstance;
import org.ow2.proactive.iaas.IaasInfrastructure;
import org.ow2.proactive.utils.FileToBytesConverter;
import org.objectweb.proactive.core.util.ProActiveCounter;
import org.ow2.proactive.iaas.nova.NovaAPI.NovaAPIConstants;
import org.ow2.proactive.resourcemanager.nodesource.common.Configurable;
import org.ow2.proactive.resourcemanager.utils.RMNodeStarter.OperatingSystem;

/**
 * An infrastructure that creates nodes in openstack using NOVA API.
 * It also adds monitoring features.
 */
public class NovaInfrastructureMon extends IaasInfrastructure {

    private static final Logger logger = Logger.getLogger(NovaInfrastructureMon.class);

    /**
     * Configuration parameters.
     */
    @Configurable(description = "[VM] User name for OpenStack.")
    protected String userName;
    
    @Configurable(description = "[VM] User password for OpenStack.")
    protected String password;
    
    @Configurable(description = "[VM] Tenant (project) name for OpenStack.")
    protected String tenantName;
    
    @Configurable(description = "[VM] An id of existing image.")
    protected String imageRef;
    
    @Configurable(description = "[VM] An id of existing flavor type (1 - m1.tiny, 2 - m1.small, etc).")
    protected String flavorRef;
    
    @Configurable(credential = true, description = "[VM] Path to local RM credential file.")
    protected String credentialvm = "";
    
    
    @Configurable(description = "")
    protected String notUsed1;
    
    
    @Configurable(description = "")
    protected String notUsed2;
    
    
    @Configurable(description = "")
    protected String notUsed3;
    
    
    @Configurable(description = "")
    protected String notUsed4;
    
    @Configurable(description = "")
    protected String notUsed5;
    
    
    @Configurable(description = "")
    protected String notUsed6;
    
    /**
     * The type of the OS on the Hosts.
     */
    protected final OperatingSystem HOST_OS = OperatingSystem.getOperatingSystem("Linux");
    
    /** 
     * After this timeout expired\nthe node is considered to be lost [ms]
     */
    protected final int DEFAULT_NODE_TIMEOUT = 60 * 1000;
    
    /**
     * Shutdown flag
     */
    protected boolean shutdown = false;
    
    @Override
    protected void configure(Object... parameters) {
        super.configure(parameters);
        
        int offset = IaasInfrastructure.NB_OF_BASE_PARAMETERS;
        userName = (String) parameters[offset + 0];
        password = (String) parameters[offset + 1];
        tenantName = (String) parameters[offset + 2];
        imageRef = (String) parameters[offset + 3];
        flavorRef = (String) parameters[offset + 4];
        credentialvm = new String((byte[]) parameters[offset + 5]);
    }

    @Override
    protected IaasApi getAPI() {
        IaasApi api;
        try {
            logger.error("MMM: put the right NOVA API, not this MOCKUP...");
            // TODO To replace here.
            api = NovaAPIMockup.getNovaAPI(userName, password, tenantName, new URI(iaasApiUrl));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return api;
    }
    
    @Override
    /**
     *  This method is overwritten to avoid setting the flag usingDeployingNodes=true 
     *  that prevents the user to add manually RMNodes later.
     */
    public void acquireNode() {
        acquireNode(USE_CONFIGURED_VALUES);
    }
    
    @Override
    /**
     *  This method is overwritten to avoid setting the flag usingDeployingNodes=true 
     *  that prevents the user to add manually RMNodes later.
     */
    public void acquireNodes(int n, Map<String, ?> nodeConfiguration) {
        for (int i = 0; i < n; i++) {
            acquireNode(nodeConfiguration);
        }
    }

    private void acquireNode(Map<String, ?> nodeConfiguration) {
        IaasApi api = getAPI();

        logger.debug("Starting a '" + api.getName() + "' instance wih parameters: " + nodeConfiguration);

        String nodeSourceName = this.nodeSource.getName();
        String nodeName = String.format(NODE_NAME_FORMAT, nodeSourceName, ProActiveCounter.getUniqID());

        /* 
         * This line below needs to be commented, so the usingDeployedNodes flag in 
         * InfrastrucutreManager is not set and it is possible to register RMNodes 
         * without having them going through the process of "node deploying". 
         */
        //String nodeUrl = this.addDeployingNode(nodeName, "", "Deploying " + api.getName() + 
        //      " node ", TEN_MINUTES_TIMEOUT);
        try {
            IaasInstance instance = api.startInstance(
                    getInstanceParams(nodeName, nodeSourceName, nodeConfiguration));
            
            nodeNameToInstance.put(nodeName, instance);
            
            logger.info("Waiting for " + api.getName() + " instance to start...");
            
        } catch (Exception e) {
            logger.error("Failed to start " + api.getName() + " instance.", e);
        } finally {
        	try {
        		api.disconnect();
        	} catch(Exception e) {
        		logger.warn("Could not disconnect from the API.", e);
        	}
        }
    }
    
    @Override
    protected Map<String, String> getInstanceParams(String nodeName, String nodeSourceName,
            Map<String, ?> nodeConfiguration) {

        URI scriptPath;
        String script;
        try {
            scriptPath = NovaInfrastructureMon.class.getResource("start-proactive-node").toURI();
            script = new String(FileToBytesConverter.convertFileToByteArray(new File(scriptPath)));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
        script = script.replace("$rm_url", rmUrl);
        script = script.replace("$credentials", credentialvm);
        script = script.replace("$node_source_name", nodeSourceName);
        script = script.replace("$node_name", nodeName);
        script = script
                .replace(
                        "$token",
                        getFromNodeConfigurationOrDefault(nodeConfiguration,
                                IaasPolicy.GenericInformation.TOKEN, ""));

        logger.debug(script);

        Map<String, String> args = new HashMap<String, String>();
        args.put(NovaAPIConstants.InstanceParameters.NAME, nodeName);

        args.put(
                NovaAPIConstants.InstanceParameters.IMAGE_REF,
                getFromNodeConfigurationOrDefault(nodeConfiguration, IaasPolicy.GenericInformation.IMAGE_ID,
                        imageRef));
        String flavorId = flavorRef;
        if (nodeConfiguration.containsKey(IaasPolicy.GenericInformation.INSTANCE_TYPE)) {
            flavorId = instanceTypeToFlavorId(nodeConfiguration.get(
                    IaasPolicy.GenericInformation.INSTANCE_TYPE).toString());
        }
        args.put(NovaAPIConstants.InstanceParameters.FLAVOR_REF, flavorId);
        args.put(NovaAPIConstants.InstanceParameters.USER_DATA, script);

        return args;
    }

    private String instanceTypeToFlavorId(String instanceType) {

        if (IaasPolicy.GenericInformation.InstanceType.SMALL.name().equals(instanceType)) {
            return "2";
        } else if (IaasPolicy.GenericInformation.InstanceType.MEDIUM.name().equals(instanceType)) {
            return "3";
        } else if (IaasPolicy.GenericInformation.InstanceType.LARGE.name().equals(instanceType)) {
            return "4";
        } else if (IaasPolicy.GenericInformation.InstanceType.DEFAULT.name().equals(instanceType)) {
            return "1"; //tiny
        }
        logger.debug("Invalid instance type specified - using SMALL instances");
        return "2";
    }
     
}
