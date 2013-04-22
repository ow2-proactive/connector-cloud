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
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import java.io.InputStream;
import java.util.Properties;
import org.ow2.proactive.iaas.vcloud.monitoring.VimServiceClient;


public class VimServiceClientTest {
    private static final String[] VM_EXPECTED_KEYS_CPU = { "cpu.cores", "cpu.usage", "cpu.frequency" };
    private static final String[] HOST_EXPECTED_KEYS_CPU = { "cpu.cores", "cpu.usage", "cpu.frequency" };

    private static final String[] VM_EXPECTED_KEYS_MEMORY = { "memory.total", "memory.free",
            "memory.actualfree" };
    private static final String[] HOST_EXPECTED_KEYS_MEMORY = { "memory.total", "memory.free",
            "memory.actualfree" };

    private static final String[] VM_EXPECTED_KEYS_STORAGE = { "storage.total", "storage.used", };
    private static final String[] HOST_EXPECTED_KEYS_STORAGE = { "storage.total", "storage.used" };

    private static final String[] VM_EXPECTED_KEYS_NETWORK = { "network.count", "network.tx",
            "network.speed", "network.rx", "network.0.tx", "network.0.rx", "network.0.speed" };
    private static final String[] HOST_EXPECTED_KEYS_NETWORK = { "network.count", "network.tx",
            "network.speed", "network.rx", "network.0.tx", "network.0.rx", "network.0.speed" };

    private static final String[] VM_EXPECTED_KEYS_STATUS = { "host", "status" };
    private static final String[] HOST_EXPECTED_KEYS_STATUS = { "site", "status" };

    private VimServiceClient v;
    private static final String URL_KEY = "vmware.url";
    private static final String USER_KEY = "vmware.user";
    private static final String PASS_KEY = "vmware.pass";

    @Before
    public void setUp() throws Exception {
        Properties prop = new Properties();
        InputStream in = getClass().getResourceAsStream("test.properties");
        prop.load(in);
        in.close();

        v = new VimServiceClient();
        v.initialize(prop.getProperty(URL_KEY), prop.getProperty(USER_KEY), prop.getProperty(PASS_KEY));
    }

    @Test
    public void getHosts() throws Exception {
        v.getHosts();
    }

    @Test
    public void getVMs() throws Exception {
        v.getVMs();
    }

    @Test
    public void getVMsPerHost() throws Exception {
        for (String hostid : v.getHosts())
            v.getVMs(hostid);

    }

    @Test
    public void getHostProperties_cpuProperties() throws Exception {
        for (String hostid : v.getHosts())
            checkMapProperties("Host", v.getHostProperties(hostid), HOST_EXPECTED_KEYS_CPU);
    }

    @Test
    public void getVMProperties_cpuProperties() throws Exception {
        for (String vmid : v.getVMs())
            checkMapProperties("VM", v.getVMProperties(vmid), VM_EXPECTED_KEYS_CPU);
    }

    @Test
    public void getHostProperties_memoryProperties() throws Exception {
        for (String hostid : v.getHosts())
            checkMapProperties("Host", v.getHostProperties(hostid), HOST_EXPECTED_KEYS_MEMORY);
    }

    @Test
    public void getVMProperties_memoryProperties() throws Exception {
        for (String vmid : v.getVMs())
            checkMapProperties("VM", v.getVMProperties(vmid), VM_EXPECTED_KEYS_MEMORY);
    }

    @Test
    public void getHostProperties_storageProperties() throws Exception {
        for (String hostid : v.getHosts())
            checkMapProperties("Host", v.getHostProperties(hostid), HOST_EXPECTED_KEYS_STORAGE);
    }

    @Test
    public void getVMProperties_storageProperties() throws Exception {
        for (String vmid : v.getVMs())
            checkMapProperties("VM", v.getVMProperties(vmid), VM_EXPECTED_KEYS_STORAGE);
    }

    @Test
    public void getHostProperties_networkProperties() throws Exception {
        for (String hostid : v.getHosts())
            checkMapProperties("Host", v.getHostProperties(hostid), HOST_EXPECTED_KEYS_NETWORK);
    }

    @Test
    public void getVMProperties_networkProperties() throws Exception {
        for (String vmid : v.getVMs())
            checkMapProperties("VM", v.getVMProperties(vmid), VM_EXPECTED_KEYS_NETWORK);
    }

    @Test
    public void getHostProperties_statusProperties() throws Exception {
        for (String hostid : v.getHosts())
            checkMapProperties("Host", v.getHostProperties(hostid), HOST_EXPECTED_KEYS_STATUS);

    }

    @Test
    public void getVMProperties_statusProperties() throws Exception {
        for (String vmid : v.getVMs())
            checkMapProperties("VM", v.getVMProperties(vmid), VM_EXPECTED_KEYS_STATUS);

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
            System.out.println("TEST FAILED" + str.toString());
            throw new Exception(str.toString());
        } 
    }

    @After
    public void disconnect() throws Exception {
        v.close();
    }

}
