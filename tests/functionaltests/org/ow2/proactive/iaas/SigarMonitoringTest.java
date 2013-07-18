/*
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
 *  Contributor(s):
 *
 *  * $$PROACTIVE_INITIAL_DEV$$
 */

package org.ow2.proactive.iaas;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.hyperic.sigar.PFlags;
import org.hyperic.sigar.Sigar;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.ow2.proactive.iaas.utils.JmxUtils;
import org.ow2.proactive.iaas.utils.Utils;
import org.ow2.proactive.iaas.monitoring.SigarClient;
import org.ow2.proactive.iaas.monitoring.IaasConst;
import org.ow2.proactive.iaas.monitoring.IaasMonitoringChainable;
import org.ow2.proactive.iaas.monitoring.IaasMonitoringService;
import org.ow2.proactive.iaas.monitoring.IaasMonitoringServiceApiLoader;
import org.ow2.proactive.iaas.monitoring.IaasMonitoringServiceCacher;
import org.ow2.proactive.iaas.monitoring.IaasMonitoringServiceFactory;
import org.ow2.proactive.iaas.monitoring.IaasMonitoringServiceSigarLoader;
import org.ow2.proactive.iaas.monitoring.NodeType;
import org.ow2.proactive.iaas.testsutils.IaasFuncTConfig;
import org.ow2.proactive.iaas.testsutils.IaasFuncTHelper;
import org.ow2.proactive.authentication.crypto.Credentials;
import org.ow2.proactive.resourcemanager.frontend.RMConnection;
import org.ow2.proactive.resourcemanager.frontend.ResourceManager;
import org.ow2.proactive.resourcemanager.common.event.RMInitialState;
import org.ow2.proactive.resourcemanager.common.event.RMNodeEvent;
import org.ow2.proactive.resourcemanager.authentication.RMAuthentication;


/**
 * These tests check for the presence of certain monitoring information as it 
 * is retrieved from a sigar mbean in an RMNode.
 * @author mjost
 */
public class SigarMonitoringTest {

    static final String HOST_NAME = "host1";
    static final String VM_NAME = "vm1";
    static final String VM_INSTANCE_NAME = "instance-00000000";

    /**
     * Url of the RM.
     */
    private static String rmurl;

    /**
     * Jmx Url .
     */
    private static String jmxUrl;
    private static File monitHosts;

    @BeforeClass
    public static void startRM() throws Exception {

        System.out.println("Starting RM (make sure no other RM is running)...");
        Map<String, String> rmNodeJvmArgs = new HashMap<String, String>();
        rmurl = IaasFuncTHelper.startResourceManager(rmNodeJvmArgs);

        System.out.println("Connecting and login to existing RM to '" + rmurl + "'...");

        RMAuthentication auth = RMConnection.join(rmurl);
        Credentials c = IaasFuncTHelper.getRmCredentials();
        ResourceManager rm = auth.login(c);

        System.out.println("Done.");

        RMInitialState state = rm.getMonitoring().getState();
        List<RMNodeEvent> events = state.getNodesEvents();
        for (RMNodeEvent r : events) { // Only one RMNode's JMX URL needed.
            jmxUrl = r.getDefaultJMXUrl();
            if (jmxUrl != null)
                break;
        }

        System.out.println("RMNode JMX URL: " + jmxUrl);

        monitHosts = createMonitoringHostsFile(HOST_NAME, jmxUrl);

        rm.disconnect();
    }

    private static File createMonitoringHostsFile(String nodename, String jmxUrl) {
        File temp = null;
        try {
            temp = File.createTempFile("monit", ".cnf");
            FileWriter fw = new FileWriter(temp.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(nodename + "=" + jmxUrl + "\n");
            bw.close();
        } catch (IOException e) {
            Assert.fail("Failed to create temporary file: " + e.getMessage());
        }
        return temp;
    }

    @Test
    public void getHostRelatedProperties() throws Exception {
        // Create monitor service.
        IaasMonitoringChainable monit = IaasMonitoringServiceFactory.getMonitoringService(
                null,
                "nsname",
                IaasMonitoringServiceFactory.SKIP_CACHE_FLAG + Utils.OPTIONS_SEP +
                    IaasMonitoringServiceSigarLoader.CREDENTIALS_FLAG + Utils.KEY_VALUE_SEP +
                    IaasFuncTHelper.getRmHome() + "/config/authentication/rm.cred" + Utils.OPTIONS_SEP +
                    IaasMonitoringServiceSigarLoader.USE_RMNODE_ON_HOST_FLAG + Utils.OPTIONS_SEP +
                    IaasMonitoringServiceSigarLoader.HOSTSFILE_FLAG + Utils.KEY_VALUE_SEP +
                    monitHosts.getAbsolutePath());

        // Check outputs...
        String[] hosts = monit.getHosts();
        for (String host : hosts) {
            System.out.println(" - " + host);
        }
        Assert.assertTrue(hosts.length > 0 && hosts[0].equals(HOST_NAME));

        Map<String, String> props = monit.getHostProperties(HOST_NAME);
        for (Entry<String, String> entry : props.entrySet()) {
            System.out.println(entry.getKey() + ":" + entry.getValue());
        }
        Assert.assertTrue(props.containsKey(IaasConst.P_COMMON_CPU_CORES.get()));
        Assert.assertTrue(props.containsKey(IaasConst.P_COMMON_MEM_TOTAL.get()));
        Assert.assertTrue(props.containsKey(IaasConst.P_COMMON_STORAGE_TOTAL_TOTAL.get()));

    }

    @Test
    public void getVMsOnHost() throws Exception {
        VMProcessTest.startSecondJVM(VMProcessHelperTest.class);
        Thread.sleep(100);

        // Create monitor service.
        IaasMonitoringChainable monit = IaasMonitoringServiceFactory.getMonitoringService(
                null,
                "nsname",
                IaasMonitoringServiceFactory.SKIP_CACHE_FLAG + Utils.OPTIONS_SEP +
                    IaasMonitoringServiceSigarLoader.CREDENTIALS_FLAG + Utils.KEY_VALUE_SEP +
                    IaasFuncTHelper.getRmHome() + "/config/authentication/rm.cred" + Utils.OPTIONS_SEP +
                    IaasMonitoringServiceSigarLoader.USE_RMNODE_ON_HOST_FLAG + Utils.OPTIONS_SEP +
                    IaasMonitoringServiceSigarLoader.SHOW_VMPROCESSES_ON_HOST_FLAG + Utils.OPTIONS_SEP +
                    IaasMonitoringServiceSigarLoader.HOSTSFILE_FLAG + Utils.KEY_VALUE_SEP +
                    monitHosts.getAbsolutePath());

        // Check outputs...
        Map<String, String> props = monit.getHostProperties(HOST_NAME);
        for (Entry<String, String> entry : props.entrySet()) {
            System.out.println(entry.getKey() + ":" + entry.getValue());
        }
        Assert.assertTrue(props.containsKey(IaasConst.P_HOST_VM_ID.get(VMProcessTest.INSTANCE)));

    }

    @Test
    public void getVMPropertiesOnHost() throws Exception {
        String macAddress = new Sigar().getNetInterfaceConfig().getHwaddr();
        String cline = VMProcessTest.KVM_COMMAND_LINE_SAMPLE.replace(VMProcessTest.MAC, macAddress).replace(VMProcessTest.INSTANCE, VM_INSTANCE_NAME);

        VMProcessTest.startSecondJVM(VMProcessHelperTest.class, cline);
        Thread.sleep(100);

        // Create monitor service.
        IaasMonitoringChainable monit = IaasMonitoringServiceFactory.getMonitoringService(
                null,
                "nsname",
                IaasMonitoringServiceFactory.SKIP_CACHE_FLAG + Utils.OPTIONS_SEP +
                    IaasMonitoringServiceSigarLoader.CREDENTIALS_FLAG + Utils.KEY_VALUE_SEP +
                    IaasFuncTHelper.getRmHome() + "/config/authentication/rm.cred" + Utils.OPTIONS_SEP +
                    IaasMonitoringServiceSigarLoader.USE_RMNODE_ON_HOST_FLAG + Utils.OPTIONS_SEP +
                    IaasMonitoringServiceSigarLoader.USE_RMNODE_ON_VM_FLAG + Utils.OPTIONS_SEP +
                    IaasMonitoringServiceSigarLoader.SHOW_VMPROCESSES_ON_HOST_FLAG + Utils.OPTIONS_SEP +
                    IaasMonitoringServiceSigarLoader.HOSTSFILE_FLAG + Utils.KEY_VALUE_SEP +
                    monitHosts.getAbsolutePath());

        monit.registerNode(VM_NAME, jmxUrl, NodeType.VM);

        // Check outputs...
        Map<String, String> hprops = monit.getHostProperties(HOST_NAME);
        for (Entry<String, String> entry : hprops.entrySet()) {
            System.out.println(entry.getKey() + ":" + entry.getValue());
        }
        Assert.assertTrue(hprops.containsKey(IaasConst.P_HOST_VM_ID.get(VM_INSTANCE_NAME)));
        
        Map<String, String> vprops = monit.getVMProperties(VM_INSTANCE_NAME);
        for (Entry<String, String> entry : vprops.entrySet()) {
            System.out.println(entry.getKey() + ":" + entry.getValue());
        }
        Assert.assertTrue(vprops.containsKey(IaasConst.P_COMMON_CPU_CORES.get()));
        Assert.assertTrue(vprops.containsKey(IaasConst.P_COMMON_STORAGE_COUNT_TOTAL.get()));

    }

    @AfterClass
    public static void shutdownRM() throws Exception {
        System.out.println("RM not needed anymore. Shutting it down...");
        rmurl = null;
        IaasFuncTHelper.stopRm();
    }
}
