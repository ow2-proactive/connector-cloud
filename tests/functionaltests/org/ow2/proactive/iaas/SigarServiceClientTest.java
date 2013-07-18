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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import org.apache.commons.io.FileUtils;
import org.hyperic.sigar.PFlags;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.ow2.proactive.iaas.utils.JmxUtils;
import org.ow2.proactive.iaas.utils.Utils;
import org.ow2.proactive.iaas.monitoring.SigarClient;
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
public class SigarServiceClientTest {

    /**
     * Set of keys that are expected to be present in the response. 
     */
    static final String[] VM_EXPECTED_KEYS_CPU = VimServiceClientTest.VM_EXPECTED_KEYS_CPU;
    static final String[] HOST_EXPECTED_KEYS_CPU = VimServiceClientTest.HOST_EXPECTED_KEYS_CPU;

    static final String[] VM_EXPECTED_KEYS_MEMORY = VimServiceClientTest.VM_EXPECTED_KEYS_MEMORY;
    static final String[] HOST_EXPECTED_KEYS_MEMORY = VimServiceClientTest.HOST_EXPECTED_KEYS_MEMORY;

    static final String[] VM_EXPECTED_KEYS_STORAGE = VimServiceClientTest.VM_EXPECTED_KEYS_STORAGE;
    static final String[] HOST_EXPECTED_KEYS_STORAGE = VimServiceClientTest.HOST_EXPECTED_KEYS_STORAGE;

    static final String[] VM_EXPECTED_KEYS_NETWORK = VimServiceClientTest.VM_EXPECTED_KEYS_NETWORK;
    static final String[] HOST_EXPECTED_KEYS_NETWORK = VimServiceClientTest.HOST_EXPECTED_KEYS_NETWORK;

    static final String[] VM_EXPECTED_KEYS_MISC = VimServiceClientTest.VM_EXPECTED_KEYS_MISC;
    static final String[] HOST_EXPECTED_KEYS_MISC = VimServiceClientTest.HOST_EXPECTED_KEYS_MISC;

    static final String MAX_NOT_CONTAINED_KEYS_KEY = "sigar.maximum_not_contained_keys";

    /**
     * Maximum allowed amount of keys not present (per test) before considered the 
     * test as failed.
     */
    private static int keysNotContainedMaximum = 0;

    /**
     * Url of the RM.
     */
    private static String rmurl;

    /**
     * Properties obtained from querying the RMNode (using Sigar's MBeans)
     */
    private static Map<String, String> rmNodeProps;

    /**
     * Flag to tell if the test was correctly configured and can continue.
     */
    private static boolean testConfigured = false;

    /**
     * PFlags test attributes.
     */
    private static String pflagName;
    private static String pflagValue;
    private static TemporaryFolder tmpFold;

    @BeforeClass
    public static void startRM() throws Exception {

        // Initialize some environment variables needed for tests.

        // INITIALIZATION FOR PFLAGS TEST
        // PFlags files will be stored in a temporary folder.
        tmpFold = new TemporaryFolder();
        tmpFold.create();
        String tmpFoldPath = tmpFold.getRoot().getAbsolutePath();

        // The PFlag name is as follows.
        pflagName = "tomcat-process";
        File f = tmpFold.newFile(pflagName);
        f.deleteOnExit();

        // The PFlag value is as follows.
        pflagValue = "pid:3030,port:8081,prop:" + (new Random()).nextInt(1024);
        FileUtils.writeStringToFile(f, pflagValue);
        // END OF INITIALIZATION FOR PFLAGS TEST

        // Get some test parameters.
        String k = IaasFuncTConfig.getInstance().getProperty(MAX_NOT_CONTAINED_KEYS_KEY);
        System.out.println(k);
        try {
            keysNotContainedMaximum = Integer.parseInt(k);
        } catch (Exception e) {
            // Ignore, use default.
        }

        // Start the RM. 

        try {
            System.out.println("Starting RM (make sure no other RM is running)...");
            Map<String, String> rmNodeJvmArgs = new HashMap<String, String>();
            rmNodeJvmArgs.put(PFlags.PFLAGS_DIR_KEY, tmpFoldPath);
            rmurl = IaasFuncTHelper.startResourceManager(rmNodeJvmArgs);
            testConfigured = true;
        } catch (Exception e) {
            System.out.println("Error while launching the RM...");
            e.printStackTrace();
            testConfigured = false;
            rmurl = "<error>";
        }

        System.out.println("Connecting and login to existing RM to '" + rmurl + "'...");

        RMAuthentication auth = RMConnection.join(rmurl);
        Credentials c = IaasFuncTHelper.getRmCredentials();
        ResourceManager rm = auth.login(c);

        System.out.println("Done.");

        RMInitialState state = rm.getMonitoring().getState();
        List<RMNodeEvent> events = state.getNodesEvents();
        String jmxurl = null;
        for (RMNodeEvent r : events) { // Only one RMNode's JMX URL needed.
            jmxurl = r.getDefaultJMXUrl();
            if (jmxurl != null)
                break;
        }

        System.out.println("RMNode JMX URL: " + jmxurl);

        Map<String, Object> jmxenv = JmxUtils.getROJmxEnv(c);
        rmNodeProps = Utils.convertToStringMap(JmxUtils.getSigarProperties(jmxurl, jmxenv,
                SigarClient.MASK_ALL));
        System.out.println("Map properties: '\n" + rmNodeProps + "\n'");

        StringBuffer str = new StringBuffer();
        for (Entry<String, String> entry : rmNodeProps.entrySet()) {
            str.append("\n - " + entry.getKey() + " - " + entry.getValue());
        }

        System.out.println("Map keys: '\n" + str.toString() + "\n'");

        System.out.println("Disconnecting...");
        rm.disconnect();
        System.out.println("Done.");
    }

    @Test
    public void getHostProperties_cpuProperties() throws Exception {
        if (testConfigured == false)
            return;

        checkMapProperties("Host", rmNodeProps, HOST_EXPECTED_KEYS_CPU);
    }

    @Test
    public void getVMProperties_cpuProperties() throws Exception {
        if (testConfigured == false)
            return;

        checkMapProperties("VM", rmNodeProps, VM_EXPECTED_KEYS_CPU);
    }

    @Test
    public void getHostProperties_memoryProperties() throws Exception {
        if (testConfigured == false)
            return;

        checkMapProperties("Host", rmNodeProps, HOST_EXPECTED_KEYS_MEMORY);
    }

    @Test
    public void getVMProperties_memoryProperties() throws Exception {
        if (testConfigured == false)
            return;

        checkMapProperties("VM", rmNodeProps, VM_EXPECTED_KEYS_MEMORY);
    }

    @Test
    public void getHostProperties_storageProperties() throws Exception {
        if (testConfigured == false)
            return;

        checkMapProperties("Host", rmNodeProps, HOST_EXPECTED_KEYS_STORAGE);
    }

    @Test
    public void getVMProperties_storageProperties() throws Exception {
        if (testConfigured == false)
            return;

        checkMapProperties("VM", rmNodeProps, VM_EXPECTED_KEYS_STORAGE);
    }

    @Test
    public void getHostProperties_networkProperties() throws Exception {
        if (testConfigured == false)
            return;

        checkMapProperties("Host", rmNodeProps, HOST_EXPECTED_KEYS_NETWORK);
    }

    @Test
    public void getVMProperties_networkProperties() throws Exception {
        if (testConfigured == false)
            return;

        checkMapProperties("VM", rmNodeProps, VM_EXPECTED_KEYS_NETWORK);
    }

    @Test
    public void getHostProperties_miscProperties() throws Exception {
        if (testConfigured == false)
            return;

        checkMapProperties("Host", rmNodeProps, HOST_EXPECTED_KEYS_MISC);
    }

    @Test
    public void getVMProperties_miscProperties() throws Exception {
        if (testConfigured == false)
            return;

        checkMapProperties("VM", rmNodeProps, VM_EXPECTED_KEYS_MISC);
    }

    @Test
    public void getPFlagsProperties() throws Exception {
        if (testConfigured == false)
            return;

        String pflagKey = "pflags." + pflagName;
        if (rmNodeProps.containsKey(pflagKey)) {
            if (rmNodeProps.get(pflagKey).equals(pflagValue)) {
                return;
            } else {
                String str = "TEST FAILED: key '" + pflagKey + "' with invalid value.";
                throw new Exception(str);
            }
        } else {
            String str = "TEST FAILED: key '" + pflagKey + "' not present.";
            throw new Exception(str);
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
            str.append("\nSome keys were not found in a " + entityType + ".");
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

    @AfterClass
    public static void shutdownRM() throws Exception {
        System.out.println("RM not needed anymore. Shutting it down...");
        rmurl = null;
        IaasFuncTHelper.stopRm();
        tmpFold.delete();
    }
}
