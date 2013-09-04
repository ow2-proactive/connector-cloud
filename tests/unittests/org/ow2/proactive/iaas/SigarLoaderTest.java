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

import com.google.common.collect.Lists;
import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ow2.proactive.iaas.monitoring.*;
import org.ow2.proactive.iaas.utils.JmxUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import static org.ow2.proactive.iaas.monitoring.IaasConst.*;
import static org.ow2.proactive.iaas.monitoring.IaasMonitoringServiceSigarLoader.*;
import static org.ow2.proactive.iaas.utils.Utils.KEY_VALUE_SEP;
import static org.ow2.proactive.iaas.utils.Utils.OPTIONS_SEP;


public class SigarLoaderTest {

    static final int HOSTN = 2;
    static final String HOST1_ID = "host1-id";
    static final String HOST1_URL = "host1-url";
    static final String HOST2_ID = "host2-id";
    static final String HOST2_URL = "host2-url";
    static final int VMN = 4;
    static final String VM1_ID = "vm1-id";
    static final String VM1_NODE = "vm1-node";
    static final String VM1_URL = "service:vm1-url";
    static final String VM1_MAC = "vm1-mac";
    static final String VM1_PROP_VM_SIGAR = "vm1-prop";
    static final String VM2_ID = "vm2-id";
    static final String VM2_NODE = "vm2-node";
    static final String VM2_URL = "service:vm2-url";
    static final String VM2_MAC = "vm2-mac";
    static final String VM2_PROP_VM_SIGAR = "vm2-prop";
    static final String VM3_ID = "vm3-id";
    static final String VM3_NODE = "vm3-node";
    static final String VM3_URL = "service:vm3-url";
    static final String VM3_MAC = "vm3-mac";
    static final String VM3_PROP_VM_SIGAR = "vm3-prop";
    static final String VM4_ID = "vm4-id";
    static final String VM4_NODE = "vm4-node";
    static final String VM4_URL = "service:vm4-url";
    static final String VM4_MAC = "vm4-mac";
    static final String VM4_PROP_VM_SIGAR = "vm4-prop";
    private static File monitHosts;

    @BeforeClass
    public static void start() throws Exception {

        Map<String, String> hostsfile = new HashMap<String, String>();
        hostsfile.put(HOST1_ID, HOST1_URL);
        hostsfile.put(HOST2_ID, HOST2_URL);

        monitHosts = createMonitoringHostsFile(hostsfile);

        JmxUtils.setMBeanClient(SigarLoaderTestClientHelper.class);

    }

    /**
     * Create monitor service.
     *
     * @return standard monitor.
     * @throws IaasMonitoringException
     */
    private static IaasMonitoringChainable getMonitorUsingHosts(boolean skipRegisterNodes) throws IaasMonitoringException {
        IaasMonitoringChainable monit = IaasMonitoringServiceFactory.getMonitoringService(null, "nsname",
                IaasMonitoringServiceFactory.SKIP_CACHE_FLAG + OPTIONS_SEP + USE_SIGAR_FLAG + OPTIONS_SEP +
                        USE_RMNODE_ON_HOST_FLAG + OPTIONS_SEP + USE_RMNODE_ON_VM_FLAG + OPTIONS_SEP +
                        HOSTSFILE_FLAG + KEY_VALUE_SEP + monitHosts.getAbsolutePath());
        if (!skipRegisterNodes)
            registerAllFakeVmNodes(monit, true);
        return monit;
    }

    /**
     * Create monitor service (without use of RmNodes on Hosts).
     *
     * @return standard monitor.
     * @throws IaasMonitoringException
     */
    private static IaasMonitoringChainable getMonitorWithoutUsingHosts(boolean skipRegisterNodes) throws IaasMonitoringException {
        IaasMonitoringChainable monit = IaasMonitoringServiceFactory.getMonitoringService(null, "nsname",
                IaasMonitoringServiceFactory.SKIP_CACHE_FLAG + OPTIONS_SEP + USE_SIGAR_FLAG + OPTIONS_SEP +
                        OPTIONS_SEP + USE_RMNODE_ON_VM_FLAG);
        if (!skipRegisterNodes)
            registerAllFakeVmNodes(monit, false);
        return monit;
    }

    private static void registerAllFakeVmNodes(IaasMonitoringChainable monit, boolean usingHosts) {
        if (usingHosts) {
            monit.registerNode(VM1_ID, VM1_URL, NodeType.VM);
            monit.registerNode(VM2_ID, VM2_URL, NodeType.VM);
            monit.registerNode(VM3_ID, VM3_URL, NodeType.VM);
            monit.registerNode(VM4_ID, VM4_URL, NodeType.VM);
        } else {
            monit.registerNode(VM1_URL, VM1_URL, NodeType.VM);
            monit.registerNode(VM2_URL, VM2_URL, NodeType.VM);
            monit.registerNode(VM3_URL, VM3_URL, NodeType.VM);
            monit.registerNode(VM4_URL, VM4_URL, NodeType.VM);
        }

    }

    private static Integer getVm2UrlCacheMisses(IaasMonitoringChainable monit) {
        IaasMonitoringServiceSigarLoader monitC = (IaasMonitoringServiceSigarLoader) monit;
        return monitC.getVmId2SigarJmxUrlCacheMisses();
    }

    private static Integer getVm2HostCacheMisses(IaasMonitoringChainable monit) {
        IaasMonitoringServiceSigarLoader monitC = (IaasMonitoringServiceSigarLoader) monit;
        return monitC.getVmId2hostIdCacheMisses();
    }

    @AfterClass
    public static void shutdown() throws Exception {
    }

    private static File createMonitoringHostsFile(Map<String, String> file) {
        File temp = null;
        try {
            temp = File.createTempFile("monit", ".cnf");
            FileWriter fw = new FileWriter(temp.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            for (Entry<String, String> entry : file.entrySet()) {
                bw.write(entry.getKey() + "=" + entry.getValue() + "\n");
            }
            bw.close();
        } catch (IOException e) {
            Assert.fail("Failed to create temporary file: " + e.getMessage());
        }
        return temp;
    }

    @Test
    public void getHostProperties_Test() throws Exception {
        IaasMonitoringChainable monit = getMonitorUsingHosts(false);

        // Check outputs...
        String[] hosts = monit.getHosts();
        Assert.assertTrue(hosts.length == HOSTN);
        Assert.assertTrue(Lists.newArrayList(hosts).contains(HOST1_ID));
        Assert.assertTrue(Lists.newArrayList(hosts).contains(HOST2_ID));
    }

    @Test
    public void getVMList_Test() throws Exception {
        IaasMonitoringChainable monit = getMonitorUsingHosts(false);

        // Check outputs...
        String[] vms = monit.getVMs();
        Assert.assertTrue(vms.length == VMN);
        Assert.assertTrue(Lists.newArrayList(vms).contains(VM1_ID));
        Assert.assertTrue(Lists.newArrayList(vms).contains(VM2_ID));
        Assert.assertTrue(Lists.newArrayList(vms).contains(VM3_ID));
        Assert.assertTrue(Lists.newArrayList(vms).contains(VM4_ID));
    }

    @Test
    public void getVMProperties_Test() throws Exception {
        IaasMonitoringChainable monit = getMonitorUsingHosts(true);

        Map<String, String> vm1;
        Map<String, String> vm2;

        {
            // Test vm1
            monit.registerNode(VM1_NODE, VM1_URL, NodeType.VM); // VM registered.

            vm1 = monit.getVMProperties(VM1_ID);
            Assert.assertTrue(vm1 != null);
            Assert.assertTrue(vm1.containsKey(P_TEST_PROP_FROM_VM_SIGAR.toString()));
            Assert.assertTrue(VM1_PROP_VM_SIGAR.equals(vm1.get(P_TEST_PROP_FROM_VM_SIGAR.toString())));

            monit.unregisterNode(VM1_NODE, VM1_URL, NodeType.VM); // VM unregistered.
        }

        {
            // Test vm2 (VM RMNode not registered yet)
            vm2 = monit.getVMProperties(VM2_ID);
            Assert.assertTrue(vm2 != null);
            Assert.assertTrue(vm2.containsKey(P_TEST_PROP_FROM_VM_SIGAR.toString()) == false);
        }

        {
            // Test vm2 (registered)
            monit.registerNode(VM2_NODE, VM2_URL, NodeType.VM); // VM registered.

            vm2 = monit.getVMProperties(VM2_ID);
            Assert.assertTrue(vm2 != null);
            Assert.assertTrue(vm2.containsKey(P_TEST_PROP_FROM_VM_SIGAR.toString()));
            Assert.assertTrue(VM2_PROP_VM_SIGAR.equals(vm2.get(P_TEST_PROP_FROM_VM_SIGAR.toString())));

            monit.unregisterNode(VM2_NODE, VM2_URL, NodeType.VM); // VM unregistered.
        }

    }

    @Test
    public void getVMPropertiesSameRMNodeName_Test() throws Exception {

        IaasMonitoringChainable monit = getMonitorUsingHosts(true);

        Map<String, String> vm1;
        Map<String, String> vm2;

        final String name = VM1_NODE + "same";

        // Two vms with the same RMNode name (should not happen but it might)
        monit.registerNode(name, VM1_URL, NodeType.VM);
        monit.registerNode(name, VM2_URL, NodeType.VM);

        vm1 = monit.getVMProperties(VM1_ID);
        vm2 = monit.getVMProperties(VM2_ID);

        Assert.assertTrue(vm1 != null);
        Assert.assertTrue(vm2 != null);

        Assert.assertTrue(vm1.containsKey(P_TEST_PROP_FROM_VM_SIGAR.toString()));
        Assert.assertTrue(vm2.containsKey(P_TEST_PROP_FROM_VM_SIGAR.toString()));

        Assert.assertTrue(VM1_PROP_VM_SIGAR.equals(vm1.get(P_TEST_PROP_FROM_VM_SIGAR.toString())));
        Assert.assertTrue(VM2_PROP_VM_SIGAR.equals(vm2.get(P_TEST_PROP_FROM_VM_SIGAR.toString())));

    }

    @Test
    public void getVMPropertiesVMDisconnectionAndReconnection_Test() throws Exception {

        IaasMonitoringChainable monit = getMonitorUsingHosts(true);

        Map<String, String> vmOriginal;
        Map<String, String> vmReconnected;
        Map<String, String> vmReReconnected;

        monit.registerNode(VM1_NODE, VM1_URL, NodeType.VM);

        vmOriginal = monit.getVMProperties(VM1_ID);

        Assert.assertTrue(getVm2UrlCacheMisses(monit) == 1);    // url for vm RMNode not found initially
        Assert.assertTrue(getVm2HostCacheMisses(monit) == 1);   // host where vm is not found initially

        makeVm1DisconnectAndReconnectWithDifferentUrl(monit);

        vmReconnected = monit.getVMProperties(VM1_ID);

        Assert.assertTrue(getVm2UrlCacheMisses(monit) == 2);    // url found but it is not valid (failures while connecting to it)
        Assert.assertTrue(getVm2HostCacheMisses(monit) == 1);   // host should not change

        vmReReconnected = monit.getVMProperties(VM1_ID);

        Assert.assertTrue(getVm2UrlCacheMisses(monit) == 2);    // no cache miss since last check
        Assert.assertTrue(getVm2HostCacheMisses(monit) == 1);   // host should not change

        // Asserts...

        Assert.assertTrue(vmOriginal != null);
        Assert.assertTrue(vmReconnected != null);
        Assert.assertTrue(vmReReconnected != null);

        Assert.assertTrue(vmOriginal.containsKey(P_COMMON_ID.toString()));
        Assert.assertTrue(vmReconnected.containsKey(P_COMMON_ID.toString()));
        Assert.assertTrue(vmReReconnected.containsKey(P_COMMON_ID.toString()));

        Assert.assertEquals(vmOriginal.get(P_COMMON_ID.toString()), vmReconnected.get(P_COMMON_ID.toString()));
        Assert.assertEquals(vmOriginal.get(P_COMMON_ID.toString()), vmReReconnected.get(P_COMMON_ID.toString()));

        Assert.assertTrue(vmOriginal.containsKey(P_SIGAR_JMX_URL.toString()));
        Assert.assertTrue(vmReconnected.containsKey(P_SIGAR_JMX_URL.toString()));
        Assert.assertTrue(vmReReconnected.containsKey(P_SIGAR_JMX_URL.toString()));
        Assert.assertFalse(vmOriginal.get(P_SIGAR_JMX_URL.toString()).equals(vmReconnected.get(P_SIGAR_JMX_URL.toString())));
        Assert.assertFalse(vmOriginal.get(P_SIGAR_JMX_URL.toString()).equals(vmReReconnected.get(P_SIGAR_JMX_URL.toString())));
        Assert.assertTrue(vmReconnected.get(P_SIGAR_JMX_URL.toString()).equals(vmReconnected.get(P_SIGAR_JMX_URL.toString())));

        SigarLoaderTestClientHelper.restoreDefaultProperties(); // Restore original status.

    }

    private void makeVm1DisconnectAndReconnectWithDifferentUrl(IaasMonitoringChainable monit) {

        Map<String, Map<String, Object>> allProps = SigarLoaderTestClientHelper.getAllPropsMap();

        Map<String, Object> propsWithFailure = SigarLoaderTestClientHelper.getSigarMapSimulatingFailures(VM1_URL);

        Map<String, Object> originalVm1Props = allProps.remove(VM1_URL);   // all info regarding vm1 is removed

        allProps.put(VM1_URL, propsWithFailure);                           // instead a map with connection failures data is put

        String NEW_VM1_URL = VM1_URL + "NEW";

        originalVm1Props.put(P_SIGAR_JMX_URL.toString(), NEW_VM1_URL);     // the original info regarding vm1 is added into a new url (RMNode reconnected)
        allProps.put(NEW_VM1_URL, originalVm1Props);                       // Different URL with same VM1 original data.

        monit.registerNode(VM1_NODE, NEW_VM1_URL, NodeType.VM);

    }

    @Test
    public void getVMPropertiesVMCache_Test() throws Exception {

        IaasMonitoringChainable monit = getMonitorUsingHosts(true);
        Map<String, String> vm1;
        monit.registerNode(VM1_NODE, VM1_URL, NodeType.VM);

        vm1 = monit.getVMProperties(VM1_ID);
        Assert.assertTrue(vm1 != null);

        vm1 = monit.getVMProperties(VM1_ID);
        Assert.assertTrue(vm1 != null);

    }

    @Test
    public void getVMList_NoHost_Test() throws Exception {
        IaasMonitoringChainable monit = getMonitorWithoutUsingHosts(false);

        String[] vms = monit.getVMs();
        Assert.assertTrue(vms.length == VMN);
        Assert.assertTrue(Lists.newArrayList(vms).contains(VM1_URL));
        Assert.assertTrue(Lists.newArrayList(vms).contains(VM2_URL));
        Assert.assertTrue(Lists.newArrayList(vms).contains(VM3_URL));
        Assert.assertTrue(Lists.newArrayList(vms).contains(VM4_URL));
    }

    @Test
    public void getVMProperties_NoHost_Test() throws Exception {
        IaasMonitoringChainable monit = getMonitorWithoutUsingHosts(false);

        String[] vms = monit.getVMs();
        Assert.assertTrue(vms.length == VMN);
        Assert.assertTrue(Lists.newArrayList(vms).contains(VM1_URL));
        Map<String, String> vmProps = monit.getVMProperties(VM1_URL);
        Assert.assertTrue(vmProps.containsKey(P_COMMON_NET_MAC.toString(0)));
        System.out.println(vmProps.get(P_COMMON_NET_MAC.toString(0)));
        Assert.assertTrue(vmProps.get(P_COMMON_NET_MAC.toString(0)).equals(VM1_MAC));

    }

    @Test
    public void getHostsList_NoHost_Test() throws Exception {
        IaasMonitoringChainable monit = getMonitorWithoutUsingHosts(false);

        String[] hosts = monit.getHosts();
        Assert.assertTrue(hosts.length == 0);

    }

}