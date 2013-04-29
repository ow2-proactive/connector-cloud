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
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.InputStream;
import java.util.Properties;
import java.io.FileInputStream;
import org.ow2.proactive.iaas.vcloud.monitoring.VimServiceClient;
import org.ow2.proactive.iaas.vcloud.monitoring.ViServiceClientException;


public class VimServiceClientTest {
    static final String[] VM_EXPECTED_KEYS_CPU = { "cpu.cores", "cpu.usage", "cpu.frequency" };
    static final String[] HOST_EXPECTED_KEYS_CPU = { "cpu.cores", "cpu.usage", "cpu.frequency" };

    static final String[] VM_EXPECTED_KEYS_MEMORY = { "memory.total", "memory.free",
            "memory.actualfree" };
    static final String[] HOST_EXPECTED_KEYS_MEMORY = { "memory.total", "memory.free",
            "memory.actualfree" };

    static final String[] VM_EXPECTED_KEYS_STORAGE = { "storage.total", "storage.used", };
    static final String[] HOST_EXPECTED_KEYS_STORAGE = { "storage.total", "storage.used" };

    static final String[] VM_EXPECTED_KEYS_NETWORK = { "network.count", "network.tx",
            "network.speed", "network.rx", "network.0.tx", "network.0.rx", "network.0.speed" };
    static final String[] HOST_EXPECTED_KEYS_NETWORK = { "network.count", "network.tx",
            "network.speed", "network.rx", "network.0.tx", "network.0.rx", "network.0.speed" };

    static final String[] VM_EXPECTED_KEYS_MISC = { "host", "status" };
    static final String[] HOST_EXPECTED_KEYS_MISC = { "site", "status" };

    static final String TEST_CONFIG_FILENAME = "tests/test.properties";

    static final String URL_KEY = "vmware.url";
    static final String USER_KEY = "vmware.user";
    static final String PASS_KEY = "vmware.pass";
    static final String RUN_SERVICE_CLINET_TEST_KEY = "vmware.runtest";
    static final String MAX_NOT_CONTAINED_KEYS_KEY = "vmware.maximum_not_contained_keys";

    private static int keysNotContainedMaximum = 0;

    private static VimServiceClient v;
    private static boolean testCorrectlyConfigured = false;

    private static Map<String, Object> allhosts;
    private static Map<String, Object> allvms;

    @Before
    public void setUp() throws ViServiceClientException {
        Properties prop = new Properties();
        try {
            InputStream in = new FileInputStream(new File(TEST_CONFIG_FILENAME));
            prop.load(in);
            in.close();
        } catch (Exception e) {
            System.out
                    .println("Error loading the file '" + TEST_CONFIG_FILENAME + "'. Test will be skipped.");
            return;
        }

        v = new VimServiceClient();
        if (prop.isEmpty() == false) {
            String url = prop.getProperty(URL_KEY);
            String user = prop.getProperty(USER_KEY);
            String pass = prop.getProperty(PASS_KEY);
            String runtest = prop.getProperty(RUN_SERVICE_CLINET_TEST_KEY);

            try {
                keysNotContainedMaximum = Integer.parseInt(prop.getProperty(MAX_NOT_CONTAINED_KEYS_KEY));
            } catch (Exception e) {
                // Ignore, use default value.
            }

            if (runtest == null || runtest.equals("false")) {
                System.out.println("Skipping test (to run tests, set propertly the key '" +
                    RUN_SERVICE_CLINET_TEST_KEY + "' in file '" + TEST_CONFIG_FILENAME + "').");
                return;
            }

            if (url == null || user == null || pass == null) {
                System.out.println("Error of the content of the file '" + TEST_CONFIG_FILENAME +
                    "'. Test will be skipped.");
                return;
            }

            v.initialize(url, user, pass);
            testCorrectlyConfigured = true;

            if (testCorrectlyConfigured == false)
                return;
        }

        if (allhosts == null) {
            System.out.println("Getting all Host properties (this will only happen once)...\n");
            allhosts = new HashMap<String, Object>();
            for (String hostid : v.getHosts()) {
                System.out.println("Getting properties of host: " + hostid);
                Map<String, String> props = v.getHostProperties(hostid);

                allhosts.put(hostid, props);
            }
            System.out.println("Host properties obtained.\n");
        } else {
            System.out.println("Host properties already obtained. ");
        }

        if (allvms == null) {
            System.out.println("Getting all VM properties (this will only happen once)...\n");
            allvms = new HashMap<String, Object>();
            for (String vmid : v.getVMs()) {
                System.out.println("Getting properties of VM: " + vmid);
                Map<String, String> props = v.getVMProperties(vmid);
                allvms.put(vmid, props);
            }
            System.out.println("VM properties obtained.\n");
        } else {
            System.out.println("VM properties already obtained. ");
        }
    }

    @Test
    public void getProviderDetails() throws Exception {
        if (testCorrectlyConfigured == false)
            return;

        System.out.println("Vendor details: " + v.getVendorDetails());
    }

    @Test
    public void getHosts() throws Exception {
        if (testCorrectlyConfigured == false)
            return;

        System.out.println("Hosts: " + Arrays.asList(v.getHosts()));
    }

    @Test
    public void getVMs() throws Exception {
        if (testCorrectlyConfigured == false)
            return;

        System.out.println("VMs: " + Arrays.asList(v.getVMs()));
    }

    @Test
    public void getVMsPerHost() throws Exception {
        if (testCorrectlyConfigured == false)
            return;

        for (String hostid : v.getHosts()) {
            System.out.println("Checking host: " + hostid);
            System.out.println("VMs at " + hostid + ": " + Arrays.asList(v.getVMs(hostid)));
        }
    }

    @Test
    public void getHostProperties_cpuProperties() throws Exception {
        if (testCorrectlyConfigured == false)
            return;

        for (String hostid : allhosts.keySet()) {
            System.out.println("Checking host: " + hostid);
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) allhosts.get(hostid);
            checkMapProperties("Host", map, HOST_EXPECTED_KEYS_CPU);
        }
    }

    @Test
    public void getVMProperties_cpuProperties() throws Exception {
        if (testCorrectlyConfigured == false)
            return;

        for (String vmid : allvms.keySet()) {
            System.out.println("Checking VM: " + vmid);
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) allvms.get(vmid);
            checkMapProperties("VM", map, VM_EXPECTED_KEYS_CPU);
        }
    }

    @Test
    public void getHostProperties_memoryProperties() throws Exception {
        if (testCorrectlyConfigured == false)
            return;

        for (String hostid : allhosts.keySet()) {
            System.out.println("Checking host: " + hostid);
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) allhosts.get(hostid);
            checkMapProperties("Host", map, HOST_EXPECTED_KEYS_MEMORY);
        }
    }

    @Test
    public void getVMProperties_memoryProperties() throws Exception {
        if (testCorrectlyConfigured == false)
            return;

        for (String vmid : allvms.keySet()) {
            System.out.println("Checking VM: " + vmid);
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) allvms.get(vmid);
            checkMapProperties("VM", map, VM_EXPECTED_KEYS_MEMORY);
        }
    }

    @Test
    public void getHostProperties_storageProperties() throws Exception {
        if (testCorrectlyConfigured == false)
            return;

        for (String hostid : allhosts.keySet()) {
            System.out.println("Checking host: " + hostid);
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) allhosts.get(hostid);
            checkMapProperties("Host", map, HOST_EXPECTED_KEYS_STORAGE);
        }
    }

    @Test
    public void getVMProperties_storageProperties() throws Exception {
        if (testCorrectlyConfigured == false)
            return;

        for (String vmid : allvms.keySet()) {
            System.out.println("Checking VM: " + vmid);
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) allvms.get(vmid);
            checkMapProperties("VM", map, VM_EXPECTED_KEYS_STORAGE);
        }
    }

    @Test
    public void getHostProperties_networkProperties() throws Exception {
        if (testCorrectlyConfigured == false)
            return;

        for (String hostid : allhosts.keySet()) {
            System.out.println("Checking host: " + hostid);
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) allhosts.get(hostid);
            checkMapProperties("Host", map, HOST_EXPECTED_KEYS_NETWORK);
        }
    }

    @Test
    public void getVMProperties_networkProperties() throws Exception {
        if (testCorrectlyConfigured == false)
            return;

        for (String vmid : allvms.keySet()) {
            System.out.println("Checking VM: " + vmid);
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) allvms.get(vmid);
            checkMapProperties("VM", map, VM_EXPECTED_KEYS_NETWORK);
        }
    }

    @Test
    public void getHostProperties_miscProperties() throws Exception {
        if (testCorrectlyConfigured == false)
            return;

        for (String hostid : allhosts.keySet()) {
            System.out.println("Checking host: " + hostid);
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) allhosts.get(hostid);
            checkMapProperties("Host", map, HOST_EXPECTED_KEYS_MISC);
        }
    }

    @Test
    public void getVMProperties_miscProperties() throws Exception {
        if (testCorrectlyConfigured == false)
            return;

        for (String vmid : allvms.keySet()) {
            System.out.println("Checking VM: " + vmid);
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) allvms.get(vmid);
            checkMapProperties("VM", map, VM_EXPECTED_KEYS_MISC);
        }
    }

    private void checkMapProperties(String entityType, Map<String, String> map, String[] expectedKeys)
            throws Exception {
        List<String> notContained = new ArrayList<String>();
        for (String key : expectedKeys)
            if (map.containsKey(key) == false)
                notContained.add(key);

        if (notContained.size() > 0) {
            StringBuffer str = new StringBuffer();
            str.append("\nSome keys were not found in a " + entityType + " properties map: " + map);
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
            v.close();
    }

}
