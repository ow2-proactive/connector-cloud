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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.ow2.proactive.iaas.monitoring.IaasConst;
import org.ow2.proactive.iaas.testsutils.IaasFuncTConfig;
import org.ow2.proactive.iaas.vcloud.monitoring.VimServiceClient;


@Ignore
public class VimServiceClientTest {
    static final String[] VM_EXPECTED_KEYS_CPU = {IaasConst.P_COMMON_CPU_CORES.toString(),
            IaasConst.P_COMMON_CPU_USAGE.toString(), IaasConst.P_COMMON_CPU_FREQUENCY.toString()};
    static final String[] HOST_EXPECTED_KEYS_CPU = VM_EXPECTED_KEYS_CPU;

    static final String[] VM_EXPECTED_KEYS_MEMORY = {IaasConst.P_COMMON_MEM_TOTAL.toString(),
            IaasConst.P_COMMON_MEM_FREE.toString(), IaasConst.P_COMMON_MEM_ACTUAL_FREE.toString()};
    static final String[] HOST_EXPECTED_KEYS_MEMORY = VM_EXPECTED_KEYS_MEMORY;

    static final String[] VM_EXPECTED_KEYS_STORAGE = {IaasConst.P_COMMON_STORAGE_TOTAL_TOTAL.toString(),
            IaasConst.P_COMMON_STORAGE_USED_TOTAL.toString()};
    static final String[] HOST_EXPECTED_KEYS_STORAGE = VM_EXPECTED_KEYS_STORAGE;

    static final String[] VM_EXPECTED_KEYS_NETWORK = {IaasConst.P_COMMON_NET_COUNT_TOTAL.toString(),
            IaasConst.P_COMMON_NET_TX_TOTAL.toString(), IaasConst.P_COMMON_NET_SPEED_TOTAL.toString(),
            IaasConst.P_COMMON_NET_RX_TOTAL.toString(), IaasConst.P_COMMON_NET_RX.toString(0),
            IaasConst.P_COMMON_NET_TX.toString(0), IaasConst.P_COMMON_NET_SPEED.toString(0)};
    static final String[] HOST_EXPECTED_KEYS_NETWORK = VM_EXPECTED_KEYS_NETWORK;

    static final String[] VM_EXPECTED_KEYS_MISC = {IaasConst.P_VM_HOST.toString(), IaasConst.P_COMMON_STATUS.toString()};
    static final String[] HOST_EXPECTED_KEYS_MISC = {IaasConst.P_HOST_SITE.toString(), IaasConst.P_COMMON_STATUS.toString()};

    static final String URL_KEY = "vmware.url";
    static final String USER_KEY = "vmware.user";
    static final String PASS_KEY = "vmware.pass";
    static final String ONLY_ONE_HOST_KEY = "vmware.test.onlyonehost";
    static final String ONLY_ONE_VM_KEY = "vmware.test.onlyonevm";
    static final String ONLY_ACTIVE_VMS_KEY = "vmware.test.onlyactivevms";
    static final String MAX_NOT_CONTAINED_KEYS_KEY = "vmware.test.maximum_not_contained_keys";

    private static int keysNotContainedMaximum = 0;

    private static VimServiceClient v = new VimServiceClient();
    private static boolean testCorrectlyConfigured = false;

    private static Map<String, Object> allhosts;
    private static Map<String, Object> allvms;

    private static String[] hosts;
    private static String[] vms;
    private static Map<String, Object> vendorDetails;

    @Before
    public void setUp() throws Exception {
        IaasFuncTConfig prop = IaasFuncTConfig.getInstance();
        if (prop.isEmpty() == false) {
            String url = prop.getProperty(URL_KEY);
            String user = prop.getProperty(USER_KEY);
            String pass = prop.getProperty(PASS_KEY);

            try {
                keysNotContainedMaximum = Integer.parseInt(prop.getProperty(MAX_NOT_CONTAINED_KEYS_KEY));
            } catch (Exception e) {
                // Ignore, use default value.
            }

            if (url == null || user == null || pass == null) {
                System.out
                        .println("Error of the content of the tests configuration file. Test will be skipped.");
                return;
            }

            v.initialize(url, user, pass);
            testCorrectlyConfigured = true;

            if (testCorrectlyConfigured == false)
                return;
        }

        Boolean useOnlyOneHost = Boolean.valueOf(prop.getProperty(ONLY_ONE_HOST_KEY));
        Boolean useOnlyOneVm = Boolean.valueOf(prop.getProperty(ONLY_ONE_VM_KEY));
        Boolean useOnlyActiveVms = Boolean.valueOf(prop.getProperty(ONLY_ACTIVE_VMS_KEY));

        if (allhosts == null) {
            System.out.println("Getting all Host properties (this will only happen once)...\n");
            allhosts = new HashMap<String, Object>();
            for (String hostid : v.getHosts()) {
                System.out.println("Getting properties of host: " + hostid);
                Map<String, String> props = v.getHostProperties(hostid);

                allhosts.put(hostid, props);
                if (useOnlyOneHost)
                    break;
            }
            System.out.println("Host properties obtained.\n");
        }

        if (allvms == null) {
            System.out.println("Getting all VM properties (this will only happen once)...\n");
            allvms = new HashMap<String, Object>();
            for (String vmid : v.getVMs()) {
                System.out.println("Getting properties of VM: " + vmid);
                Map<String, String> props = v.getVMProperties(vmid);
                if (useOnlyActiveVms) {
                    if ("up".equals(props.get("status"))) {
                        allvms.put(vmid, props);
                    } else {
                        System.out.println("   Discarding because is down.");
                    }
                } else {
                    allvms.put(vmid, props);
                }
                if (useOnlyOneVm)
                    if (!allvms.isEmpty())
                        break;
            }
            System.out.println("VM properties obtained.\n");
        }

        if (vms == null) {
            vms = v.getVMs();
        }

        if (hosts == null) {
            hosts = v.getHosts();
        }

        if (vendorDetails == null) {
            vendorDetails = v.getVendorDetails();
        }
    }

    @Test
    public void getProviderDetails() throws Exception {
        if (testCorrectlyConfigured == false)
            return;

        System.out.println("Vendor details: " + vendorDetails);
    }

    @Test
    public void getHosts() throws Exception {
        if (testCorrectlyConfigured == false)
            return;

        System.out.println("Hosts: " + Arrays.asList(hosts));
    }

    @Test
    public void getVMs() throws Exception {
        if (testCorrectlyConfigured == false)
            return;

        System.out.println("VMs: " + Arrays.asList(vms));
    }

    @Test
    public void getHostProperties_cpuProperties() throws Exception {
        if (testCorrectlyConfigured == false)
            return;

        for (String hostid : allhosts.keySet()) {
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) allhosts.get(hostid);
            checkMapProperties(hostid, "Host", map, HOST_EXPECTED_KEYS_CPU);
        }
    }

    @Test
    public void getVMProperties_cpuProperties() throws Exception {
        if (testCorrectlyConfigured == false)
            return;

        for (String vmid : allvms.keySet()) {
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) allvms.get(vmid);
            checkMapProperties(vmid, "VM", map, VM_EXPECTED_KEYS_CPU);
        }
    }

    @Test
    public void getHostProperties_memoryProperties() throws Exception {
        if (testCorrectlyConfigured == false)
            return;

        for (String hostid : allhosts.keySet()) {
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) allhosts.get(hostid);
            checkMapProperties(hostid, "Host", map, HOST_EXPECTED_KEYS_MEMORY);
        }
    }

    @Test
    public void getVMProperties_memoryProperties() throws Exception {
        if (testCorrectlyConfigured == false)
            return;

        for (String vmid : allvms.keySet()) {
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) allvms.get(vmid);
            checkMapProperties(vmid, "VM", map, VM_EXPECTED_KEYS_MEMORY);
        }
    }

    @Test
    public void getHostProperties_storageProperties() throws Exception {
        if (testCorrectlyConfigured == false)
            return;

        for (String hostid : allhosts.keySet()) {
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) allhosts.get(hostid);
            checkMapProperties(hostid, "Host", map, HOST_EXPECTED_KEYS_STORAGE);
        }
    }

    @Test
    public void getVMProperties_storageProperties() throws Exception {
        if (testCorrectlyConfigured == false)
            return;

        for (String vmid : allvms.keySet()) {
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) allvms.get(vmid);
            checkMapProperties(vmid, "VM", map, VM_EXPECTED_KEYS_STORAGE);
        }
    }

    @Test
    public void getHostProperties_networkProperties() throws Exception {
        if (testCorrectlyConfigured == false)
            return;

        for (String hostid : allhosts.keySet()) {
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) allhosts.get(hostid);
            checkMapProperties(hostid, "Host", map, HOST_EXPECTED_KEYS_NETWORK);
        }
    }

    @Test
    public void getVMProperties_networkProperties() throws Exception {
        if (testCorrectlyConfigured == false)
            return;

        for (String vmid : allvms.keySet()) {
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) allvms.get(vmid);
            checkMapProperties(vmid, "VM", map, VM_EXPECTED_KEYS_NETWORK);
        }
    }

    @Test
    public void getHostProperties_miscProperties() throws Exception {
        if (testCorrectlyConfigured == false)
            return;

        for (String hostid : allhosts.keySet()) {
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) allhosts.get(hostid);
            checkMapProperties(hostid, "Host", map, HOST_EXPECTED_KEYS_MISC);
        }
    }

    @Test
    public void getVMProperties_miscProperties() throws Exception {
        if (testCorrectlyConfigured == false)
            return;

        for (String vmid : allvms.keySet()) {
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) allvms.get(vmid);
            checkMapProperties(vmid, "VM", map, VM_EXPECTED_KEYS_MISC);
        }
    }

    private void checkMapProperties(String id, String entityType, Map<String, String> map,
                                    String[] expectedKeys) throws Exception {
        List<String> notContained = new ArrayList<String>();
        for (String key : expectedKeys)
            if (map.containsKey(key) == false)
                notContained.add(key);

        if (notContained.size() > 0) {
            StringBuffer str = new StringBuffer();
            str.append("\nSome keys were not found in a " + entityType + " " + id + " properties map: " + map);
            for (String key : notContained) {
                str.append("\n   KEY: '" + key + "' not present;");
            }
            if (notContained.size() > keysNotContainedMaximum) {
                System.out.println("TEST FAILED: " + str.toString());
                throw new Exception(str.toString());
            } else {
                System.out.println("TEST OK, BUT SOME KEYS MISSING: " + str.toString());
            }
        }
    }

    @After
    public void disconnect() throws Exception {
        if (v != null)
            v.disconnect();
    }

}
