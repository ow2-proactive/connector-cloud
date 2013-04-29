/*
 * ################################################################
 *
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
 *  Contributor(s): ActiveEon Team - http://www.activeeon.com
 *
 * ################################################################
 * $$ACTIVEEON_CONTRIBUTOR$$
 */
package org.ow2.proactive.iaas;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.objectweb.proactive.api.PAActiveObject;
import org.objectweb.proactive.api.PAFuture;
import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.ProActiveTimeoutException;
import org.objectweb.proactive.core.config.CentralPAPropertyRepository;
import org.objectweb.proactive.core.node.Node;
import org.objectweb.proactive.core.node.NodeException;
import org.objectweb.proactive.core.node.NodeFactory;
import org.objectweb.proactive.core.process.JVMProcessImpl;
import org.objectweb.proactive.core.util.ProActiveInet;
import org.ow2.proactive.authentication.crypto.CredData;
import org.ow2.proactive.authentication.crypto.Credentials;
import org.ow2.proactive.resourcemanager.RMFactory;
import org.ow2.proactive.resourcemanager.authentication.RMAuthentication;
import org.ow2.proactive.resourcemanager.common.event.RMEventType;
import org.ow2.proactive.resourcemanager.common.event.RMNodeEvent;
import org.ow2.proactive.resourcemanager.core.properties.PAResourceManagerProperties;
import org.ow2.proactive.resourcemanager.exception.RMException;
import org.ow2.proactive.resourcemanager.frontend.RMConnection;
import org.ow2.proactive.resourcemanager.frontend.ResourceManager;
import org.ow2.proactive.resourcemanager.nodesource.NodeSource;
import org.ow2.proactive.resourcemanager.nodesource.infrastructure.DefaultInfrastructureManager;
import org.ow2.proactive.resourcemanager.nodesource.infrastructure.LocalInfrastructure;
import org.ow2.proactive.resourcemanager.nodesource.policy.StaticPolicy;
import org.ow2.proactive.utils.FileToBytesConverter;
import org.ow2.tests.ProActiveSetup;

import functionaltests.RMTHelper;
import functionaltests.RMTStarter;
import functionaltests.common.CommonTUtils;
import functionaltests.common.InputStreamReaderThread;
import functionaltests.monitor.RMMonitorEventReceiver;
import functionaltests.monitor.RMMonitorsHandler;


/**
 *
 * Static helpers for Resource Manager functional tests.
 * It provides waiters methods that check correct event dispatching.
 *
 * @author ProActive team
 *
 */
public class RMTHelperIaas extends RMTHelper {


    private static RMTHelperIaas defaultInstanceIaas = new RMTHelperIaas();

    public static RMTHelperIaas getDefaultInstanceIaas() {
        return defaultInstanceIaas;
    }
    /**
     * Start the RM using a forked JVM
     *
     * @param configurationFile the RM's configuration file to use (default is functionalTSchedulerProperties.ini)
     *          null to use the default one.
     * @throws Exception if an error occurs.
     */
    public String startRM(String configurationFile, int rmiPort, String... jvmArgs) throws Exception {
        if (configurationFile == null) {
            configurationFile = new File(functionalTestRMProperties.toURI()).getAbsolutePath();
        }
        PAResourceManagerProperties.loadProperties(configurationFile);
        
        PAResourceManagerProperties.RM_NODE_NAME.updateProperty("node");
        System.out.println("Name: " + PAResourceManagerProperties.RM_NODE_NAME.getValueAsString());
        System.out.println("Name: " + PAResourceManagerProperties.RM_NODE_NAME.getValueAsString());
        
        
        List<String> commandLine = new ArrayList<String>();
        commandLine.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        commandLine.add("-Djava.security.manager");
        commandLine.add(CentralPAPropertyRepository.PA_HOME.getCmdLine() +
            CentralPAPropertyRepository.PA_HOME.getValue());
        commandLine.add(CentralPAPropertyRepository.JAVA_SECURITY_POLICY.getCmdLine() +
            CentralPAPropertyRepository.JAVA_SECURITY_POLICY.getValue());
        commandLine.add(CentralPAPropertyRepository.LOG4J.getCmdLine() +
            CentralPAPropertyRepository.LOG4J.getValue());
        commandLine.add(PAResourceManagerProperties.RM_HOME.getCmdLine() +
            PAResourceManagerProperties.RM_HOME.getValueAsString());

        String home = PAResourceManagerProperties.RM_HOME.getValueAsString();
        String distLib = home + File.separator + "dist" + File.separator + "lib" + File.separator;
        StringBuilder classpath = new StringBuilder();
        for (String name : new String[] { "ProActive_tests.jar", "ProActive_SRM-common.jar",
                "ProActive_ResourceManager.jar", "ProActive.jar", "jruby-engine.jar", "jython-engine.jar" }) {
            classpath.append(distLib).append(name).append(File.pathSeparator);
        }
        classpath.append(home + File.separator + "classes" + File.separator + "schedulerTests");
        classpath.append(File.pathSeparator);
        classpath.append(home + File.separator + "classes" + File.separator + "resource-managerTests");

        commandLine.add("-cp");
        commandLine.add(classpath.toString());
        commandLine.add(CentralPAPropertyRepository.PA_TEST.getCmdLine() + "true");
        commandLine.add(CentralPAPropertyRepository.PA_RMI_PORT.getCmdLine() + rmiPort);
        for (String jvmArg : jvmArgs) {
            commandLine.add(jvmArg);
        }
        commandLine.add(RMTStarter.class.getName());
        commandLine.add(configurationFile);
        System.out.println("Starting RM process: " + commandLine);

        ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
        processBuilder.redirectErrorStream(true);
        rmProcess = processBuilder.start();

        InputStreamReaderThread outputReader = new InputStreamReaderThread(rmProcess.getInputStream(),
            "[RM VM output]: ");
        outputReader.start();

        String url = "rmi://localhost:" + rmiPort + "/";

        System.out.println("Waiting for the RM using URL: " + url);
        auth = RMConnection.waitAndJoin(url);
        return url;
    }

    /**
     * Stop the Resource Manager if exists.
     * @throws Exception
     * @throws ProActiveException
     */
    public void killRM() throws Exception {
        if (rmProcess != null) {
            rmProcess.destroy();
            rmProcess.waitFor();
            rmProcess = null;

            // sometimes RM_NODE object isn't removed from the RMI registry after JVM with RM is killed (SCHEDULING-1498)
            CommonTUtils.cleanupRMActiveObjectRegistry();
        }
        reset();
    }

    /**
     * Resets the RMTHelper
     */
    public void reset() throws Exception {
        auth = null;
        resourceManager = null;
        eventReceiver = null;
    }

    /**
     * Wait for an event regarding Scheduler state : started, resumed, stopped...
     * If a corresponding event has been already thrown by scheduler, returns immediately,
     * otherwise wait for reception of the corresponding event.
     * @param event awaited event.
     */
    public void waitForRMStateEvent(RMEventType event) {
        try {
            waitForRMStateEvent(event, 0);
        } catch (ProActiveTimeoutException e) {
            //unreachable block, 0 means infinite, no timeout
            //log sthing ?
        }
    }

    /**
     * Wait for an event regarding RM state : started, resumed, stopped...
     * If a corresponding event has been already thrown by scheduler, returns immediately,
     * otherwise wait for reception of the corresponding event.
     * @param eventType awaited event.
     * @param timeout in milliseconds
     * @throws ProActiveTimeoutException if timeout is reached
     */
    public void waitForRMStateEvent(RMEventType eventType, long timeout) throws ProActiveTimeoutException {
        getMonitorsHandler().waitForRMStateEvent(eventType, timeout);
    }

    /**
     * Wait for an event regarding node sources: created, removed....
     * If a corresponding event has been already thrown by RM, returns immediately,
     * otherwise wait for reception of the corresponding event.
     * @param nodeSourceEvent awaited event.
     * @param nodeSourceName corresponding node source name for which an event is awaited.
     */
    public void waitForNodeSourceEvent(RMEventType nodeSourceEvent, String nodeSourceName) {
        try {
            waitForNodeSourceEvent(nodeSourceEvent, nodeSourceName, 0);
        } catch (ProActiveTimeoutException e) {
            //unreachable block, 0 means infinite, no timeout
            //log sthing ?
        }
    }

    /**
     * Wait for an event regarding node sources: created, removed....
     * If a corresponding event has been already thrown by RM, returns immediately,
     * otherwise wait for reception of the corresponding event.
     * @param eventType awaited event.
     * @param nodeSourceName corresponding node source name for which an event is awaited.
     * @param timeout in milliseconds
     * @throws ProActiveTimeoutException if timeout is reached
     */
    public void waitForNodeSourceEvent(RMEventType eventType, String nodeSourceName, long timeout)
            throws ProActiveTimeoutException {
        getMonitorsHandler().waitForNodesourceEvent(eventType, nodeSourceName, timeout);
    }

    /**
     * Wait for an event on a specific node : created, removed....
     * If a corresponding event has been already thrown by RM, returns immediately,
     * otherwise wait for reception of the corresponding event.
     * @param nodeEvent awaited event.
     * @param nodeUrl Url's of the node for which a new state is awaited.
     * @return RMNodeEvent object received by event receiver.
     */
    public RMNodeEvent waitForNodeEvent(RMEventType nodeEvent, String nodeUrl) {
        try {
            return waitForNodeEvent(nodeEvent, nodeUrl, 0);
        } catch (ProActiveTimeoutException e) {
            //unreachable block, 0 means infinite, no timeout
            //log string ?
            return null;
        }
    }

    /**
     * Wait for an event on a specific node : created, removed....
     * If a corresponding event has been already thrown by RM, returns immediately,
     * otherwise wait for reception of the corresponding event.
     * @param eventType awaited event.
     * @param nodeUrl Url's of the node for which a new state is awaited
     * @param timeout in milliseconds
     * @return RMNodeEvent object received by event receiver.
     * @throws ProActiveTimeoutException if timeout is reached
     */
    public RMNodeEvent waitForNodeEvent(RMEventType eventType, String nodeUrl, long timeout)
            throws ProActiveTimeoutException {
        return getMonitorsHandler().waitForNodeEvent(eventType, nodeUrl, timeout);
    }

    /**
     * Wait for an event on any node: added, removed....
     * If a corresponding event has been already thrown by RM, returns immediately,
     * otherwise wait for reception of the corresponding event.
     * @param eventType awaited event.
     * @return RMNodeEvent object received by event receiver.
     */
    public RMNodeEvent waitForAnyNodeEvent(RMEventType eventType) {
        try {
            return waitForAnyNodeEvent(eventType, 0);
        } catch (ProActiveTimeoutException e) {
            //unreachable block, 0 means infinite, no timeout
            //log sthing ?
            return null;
        }
    }

    /**
     * Kills the node with specified url
     * @param url of the node
     * @throws NodeException if node cannot be looked up
     */
    public void killNode(String url) throws NodeException {
        Node node = NodeFactory.getNode(url);
        try {
            node.getProActiveRuntime().killRT(false);
        } catch (Exception e) {
        }
    }

    /**
     * Wait for an event on any node: added, removed....
     * If a corresponding event has been already thrown by RM, returns immediately,
     * otherwise wait for reception of the corresponding event.
     * @param eventType awaited event.
     * @param timeout in milliseconds
     * @return RMNodeEvent object received by event receiver.
     * @throws ProActiveTimeoutException if timeout is reached
     */
    public RMNodeEvent waitForAnyNodeEvent(RMEventType eventType, long timeout)
            throws ProActiveTimeoutException {
        return getMonitorsHandler().waitForAnyNodeEvent(eventType, timeout);
    }

    //-------------------------------------------------------------//
    //private methods
    //-------------------------------------------------------------//

    private void initEventReceiver() throws Exception {
        RMMonitorsHandler mHandler = getMonitorsHandler();
        if (eventReceiver == null) {
            /** create event receiver then turnActive to avoid deepCopy of MonitorsHandler object
             *  (shared instance between event receiver and static helpers).
            */
            System.out.println("Initializing new event receiver");
            RMMonitorEventReceiver passiveEventReceiver = new RMMonitorEventReceiver(mHandler);
            eventReceiver = (RMMonitorEventReceiver) PAActiveObject.turnActive(passiveEventReceiver);
            PAFuture.waitFor(resourceManager.getMonitoring().addRMEventListener(eventReceiver));
        }
    }

    /**
     * Gets the connected ResourceManager interface.
     */
    public ResourceManager getResourceManager() throws Exception {
        return getResourceManager(null, defaultUserName, defaultUserPassword);
    }

    /**
     * Idem than getResourceManager but allow to specify a propertyFile
     * @return the resource manager
     * @throws Exception
     */
    public ResourceManager getResourceManager(String propertyFile, String user, String pass) throws Exception {

        if (user == null)
            user = defaultUserName;
        if (pass == null)
            pass = defaultUserPassword;

        if (resourceManager == null || !user.equals(connectedUserName)) {

            if (resourceManager != null) {
                System.out.println("Disconnecting user " + connectedUserName + " from the resource manager");
                try {
                    resourceManager.getMonitoring().removeRMEventListener();
                    resourceManager.disconnect().getBooleanValue();

                    eventReceiver = null;
                    resourceManager = null;
                } catch (RuntimeException ex) {
                    ex.printStackTrace();
                }
            }

            String rmUrl = System.getProperty("url");

            if (rmUrl != null && !rmUrl.equals("${url}")) {
                // joining the existing RM
                try {
                    System.out.println("Checking if there is existing RM on " + rmUrl);
                    auth = RMConnection.join(rmUrl);
                    System.out.println("Connected to the existing RM on " + rmUrl);
                    authentificate(user, pass);
                    initEventReceiver();
                } catch (RMException ex) {
                    System.out.println("Cannot connect to the RM on " + rmUrl + ": " + ex.getMessage());
                    throw ex;
                }
            } else {
                if (auth == null) {
                    // creating a new RM and default node source
                    startRM(propertyFile, CentralPAPropertyRepository.PA_RMI_PORT.getValue(), "-cp", "lib/scheduling/*", "-Djava.security.policy=/media/mjost/linweb/build/dist/cloud_service_provider_conectors/tests/security.java.policy-server");
                }
                authentificate(user, pass);
                initEventReceiver();
            }
            System.out.println("RMTHelper is connected");
        }
        return resourceManager;
    }

    private void authentificate(String user, String pass) throws Exception {
        connectedUserName = user;
        connectedUserPassword = pass;
        connectedUserCreds = Credentials.createCredentials(
                new CredData(CredData.parseLogin(user), CredData.parseDomain(connectedUserName), pass),
                auth.getPublicKey());

        System.out.println("Authentificating as user " + user);
        resourceManager = auth.login(connectedUserCreds);
    }

    public RMMonitorsHandler getMonitorsHandler() {
        if (monitorsHandler == null) {
            monitorsHandler = new RMMonitorsHandler();
        }
        return monitorsHandler;
    }

    public RMMonitorEventReceiver getEventReceiver() {
        return eventReceiver;
    }

    public RMAuthentication getRMAuth() throws Exception {
        if (auth == null) {
            getResourceManager();
        }
        return auth;
    }
}
