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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedWriter;

import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import org.ow2.proactive.iaas.utils.JmxUtils;
import org.ow2.proactive.iaas.monitoring.NodeType;
import org.ow2.proactive.iaas.monitoring.IaasMonitoringException;
import org.ow2.proactive.iaas.monitoring.IaasMonitoringChainable;
import org.ow2.proactive.iaas.monitoring.IaasMonitoringServiceFactory;

import static org.ow2.proactive.iaas.utils.Utils.*;
import static org.ow2.proactive.iaas.monitoring.IaasMonitoringServiceSigarLoader.*;

import com.google.common.collect.Lists;

import junit.framework.Assert;

import static org.ow2.proactive.iaas.monitoring.IaasConst.*;


public class SigarLoaderTest {

    private static File monitHosts;

    static final int HOSTN = 2;
    static final String HOST1_ID = "host1-id";
    static final String HOST1_URL = "host1-url";
    static final String HOST2_ID = "host2-id";
    static final String HOST2_URL = "host2-url";

    static final int VMN = 4;
    static final String VM1_ID = "vm1-id";
    static final String VM1_URL = "vm1-url";
    static final String VM1_MAC = "vm1-mac";
    static final String VM1_CORES = "vm1-cores";

    static final String VM2_ID = "vm2-id";
    static final String VM2_URL = "vm2-url";
    static final String VM2_MAC = "vm2-mac";
    static final String VM2_CORES = "vm2-cores";

    static final String VM3_ID = "vm3-id";
    static final String VM3_URL = "vm3-url";
    static final String VM3_MAC = "vm3-mac";

    static final String VM4_ID = "vm4-id";
    static final String VM4_URL = "vm4-url";
    static final String VM4_MAC = "vm4-mac";

    @BeforeClass
    public static void start() throws Exception {

        Map<String, String> hostsfile = new HashMap<String, String>();
        hostsfile.put(HOST1_ID, HOST1_URL);
        hostsfile.put(HOST2_ID, HOST2_URL);

        monitHosts = createMonitoringHostsFile(hostsfile);

        JmxUtils.setMBeanClient(ClientTest.class);

    }

    /**
     * Create monitor service.
     * @return standard monitor.
     * @throws IaasMonitoringException
     */
    private static IaasMonitoringChainable getMonitor() throws IaasMonitoringException {
        IaasMonitoringChainable monit = IaasMonitoringServiceFactory.getMonitoringService(null, "nsname",
                IaasMonitoringServiceFactory.SKIP_CACHE_FLAG + OPTIONS_SEP + USE_SIGAR_FLAG + OPTIONS_SEP +
                    USE_RMNODE_ON_HOST_FLAG + OPTIONS_SEP + USE_RMNODE_ON_VM_FLAG + OPTIONS_SEP +
                    HOSTSFILE_FLAG + KEY_VALUE_SEP + monitHosts.getAbsolutePath());
        return monit;
    }

    @Test
    public void getHostProperties_Test() throws Exception {
        IaasMonitoringChainable monit = getMonitor();

        // Check outputs...
        String[] hosts = monit.getHosts();
        for (String host : hosts) {
            System.out.println(" - " + host);
        }
        Assert.assertTrue(hosts.length == HOSTN);
        Assert.assertTrue(Lists.newArrayList(hosts).contains(HOST1_ID));
        Assert.assertTrue(Lists.newArrayList(hosts).contains(HOST2_ID));
    }

    @Test
    public void getVMList_Test() throws Exception {
        IaasMonitoringChainable monit = getMonitor();

        // Check outputs...
        String[] vms = monit.getVMs();
        for (String vm : vms) {
            System.out.println(" - " + vm);
        }
        Assert.assertTrue(vms.length == VMN);
        Assert.assertTrue(Lists.newArrayList(vms).contains(VM1_ID));
        Assert.assertTrue(Lists.newArrayList(vms).contains(VM2_ID));
        Assert.assertTrue(Lists.newArrayList(vms).contains(VM3_ID));
        Assert.assertTrue(Lists.newArrayList(vms).contains(VM4_ID));
    }

    @Test
    public void getVMProperties_Test() throws Exception {
        IaasMonitoringChainable monit = getMonitor();

        Map<String, String> vm1;
        Map<String, String> vm2;

        {
            // Test vm1
            monit.registerNode(VM1_ID + "-panode", VM1_URL, NodeType.VM); // VM registered.

            vm1 = monit.getVMProperties(VM1_ID);
            Assert.assertTrue(vm1 != null);
            Assert.assertTrue(vm1.containsKey(P_COMMON_CPU_CORES.get()));
            Assert.assertTrue(VM1_CORES.equals(vm1.get(P_COMMON_CPU_CORES.get())));

            monit.unregisterNode(VM1_ID + "-panode", NodeType.VM); // VM unregistered.
        }

        {
            // Test vm2 (VM RMNode not registered yet)
            vm2 = monit.getVMProperties(VM2_ID);
            Assert.assertTrue(vm2 != null);
            Assert.assertTrue(vm2.containsKey(P_COMMON_CPU_CORES.get()) == false);
        }

        {
            // Test vm2 (registered)
            monit.registerNode(VM2_ID + "-panode", VM2_URL, NodeType.VM); // VM registered.

            vm2 = monit.getVMProperties(VM2_ID);
            Assert.assertTrue(vm2 != null);
            Assert.assertTrue(vm2.containsKey(P_COMMON_CPU_CORES.get()));
            Assert.assertTrue(VM2_CORES.equals(vm2.get(P_COMMON_CPU_CORES.get())));

            monit.unregisterNode(VM2_ID + "-panode", NodeType.VM); // VM unregistered.
        }

    }

    @Test
    @Ignore
    public void getVMPropertiesSameRMNodeName_Test() throws Exception {
        IaasMonitoringChainable monit = getMonitor();

        Map<String, String> vm1;
        Map<String, String> vm2;

        final String name = "panode";

        monit.registerNode(name, VM1_URL, NodeType.VM); 
        monit.registerNode(name, VM2_URL, NodeType.VM); 

        vm1 = monit.getVMProperties(VM1_ID);
        vm2 = monit.getVMProperties(VM2_ID);
        
        Assert.assertTrue(vm1 != null);
        Assert.assertTrue(vm2 != null);
        
        Assert.assertTrue(vm1.containsKey(P_COMMON_CPU_CORES.get()));
        Assert.assertTrue(vm2.containsKey(P_COMMON_CPU_CORES.get()));
        
        Assert.assertTrue(VM1_CORES.equals(vm1.get(P_COMMON_CPU_CORES.get())));
        Assert.assertTrue(VM2_CORES.equals(vm2.get(P_COMMON_CPU_CORES.get())));

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

}