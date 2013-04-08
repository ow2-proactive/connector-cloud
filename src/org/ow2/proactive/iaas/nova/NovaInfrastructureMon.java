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
import java.util.HashMap;
import java.util.Map;
import javax.management.MBeanRegistrationException;
import org.apache.log4j.Logger;
import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.node.Node;
import org.ow2.proactive.iaas.IaaSMonitoringApi;
import org.ow2.proactive.iaas.IaasApi;
import org.ow2.proactive.iaas.IaasInfrastructure;
import org.ow2.proactive.iaas.IaasPolicy;
import org.ow2.proactive.iaas.monitoring.IaaSMonitoringService;
import org.ow2.proactive.iaas.monitoring.IaaSMonitoringServiceException;
import org.ow2.proactive.iaas.monitoring.MBeanExposer;
import org.ow2.proactive.iaas.monitoring.NodeType;
import org.ow2.proactive.iaas.nova.NovaAPI.NovaAPIConstants;
import org.ow2.proactive.jmx.naming.JMXTransportProtocol;
import org.ow2.proactive.resourcemanager.exception.RMException;
import org.ow2.proactive.resourcemanager.nodesource.common.Configurable;
import org.ow2.proactive.resourcemanager.utils.RMNodeStarter;
import org.ow2.proactive.resourcemanager.utils.RMNodeStarter.OperatingSystem;
import org.ow2.proactive.utils.FileToBytesConverter;


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
    
    @Configurable(credential = true, description = "[VM] Path to credential file (in VM) so VM registers to RM.")
    protected String credentialvm = "";
    
    @Configurable(description = "[HOSTS] Tells whether to monitor hosts or not. If set to " + 
		    "'monitorHostEnabled' a JMX monitoring service will be started to monitor the infrastrucutre.")
    protected boolean monitorHosts = false;
    
    @Configurable(description = "Node Source name (to be fixed, at configure level NSName is not set).")
    protected String nodeSourceName;
    
    
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
    
    /**
     * Exposer of IaaS monitoring mbean.
     */
    private MBeanExposer mbeanexposer;
    
    /**
     * IaaS monitoring service.
     */
    private IaaSMonitoringService monitoringService;
    
    @Override
    protected void configure(Object... parameters) {
        super.configure(parameters);
        
    	logger.debug("Configuring Nova infrastructure...");
        
        userName = (String) parameters[2];
        password = (String) parameters[3];
        tenantName = (String) parameters[4];
        imageRef = (String) parameters[5];
        flavorRef = (String) parameters[6];
        credentialvm = new String((byte[]) parameters[7]);
        
        if (parameters[8] != null && parameters[8].toString().equals("monitorHostEnabled")){
        	monitorHosts = true;
	        nodeSourceName = parameters[9].toString();
	        
        	try{
	
				IaaSMonitoringService m = new IaaSMonitoringService((IaaSMonitoringApi) getAPI());	
				MBeanExposer e =  new MBeanExposer();
				e.registerMBeanLocally(nodeSourceName, m);
			
				mbeanexposer = e;
				monitoringService = m;    

        	} catch (MBeanRegistrationException e) {
        		logger.error("Could not register IaaS Monitoring MBean.", e);
        	} catch (IaaSMonitoringServiceException e) {
				logger.error("Could not initialize IaaS monitoring service.", e);
			}
        } else {
        	logger.debug("Host monitoring: disabled (monitorHostDisabled).");
        }
    }

    @Override
    protected IaasApi getAPI() {
        IaasApi api;
        try {
            api = NovaAPI.getNovaAPI(userName, password, tenantName, new URI(iaasApiUrl));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return api;
    }
    
//    @Override
//    protected IaasApi getAPI() {
//        IaasApi api;
//        try {
//        	logger.error("SET TO THE REAL NOVA API.");
//        	// TODO SET TO THE REAL NOVA API.
//        	
//            api = NovaAPIMockup.getNovaAPI(
//            		userName, 
//            		password, 
//            		tenantName, 
//            		new URI(iaasApiUrl));
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//        return api;
//    }

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
    
    @Override
    public void shutDown(){
    	if (mbeanexposer!=null) { 
    		try {
				mbeanexposer.stop();
			} catch (Exception e) {
				logger.warn("Error while stopping mbeanexposer.", e);
			}
    	}
    }
    
    @Override
    protected void notifyAcquiredNode(Node node) throws RMException {
    	super.notifyAcquiredNode(node);
    	
    	if (monitoringService == null){
    		return;
    	}
    	
    	try {
            String jmxurlrmi = node.getProperty(
            		RMNodeStarter.JMX_URL + JMXTransportProtocol.RMI);
            String token = node.getProperty(
            		RMNodeStarter.NODE_ACCESS_TOKEN);
            
            NodeType type = ("IAASHOST".equals(token)?NodeType.HOST:NodeType.VM);
            monitoringService.registerNode(
            		node.getNodeInformation().getName(), 
            		jmxurlrmi, 
            		type);
		} catch (ProActiveException e) {
			logger.error("Error while getting node properties.", e);
		}
    }
    
    @Override
    public void removeNode(Node node) throws RMException{
    	super.removeNode(node);
    	
    	if (monitoringService == null){
    		return;
    	}
    	
        monitoringService.unregisterNode(node.getNodeInformation().getName());
    }
}
