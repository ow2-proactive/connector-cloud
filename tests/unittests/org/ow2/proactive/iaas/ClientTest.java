package org.ow2.proactive.iaas;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;

import static org.ow2.proactive.iaas.monitoring.IaasConst.*;
import static org.ow2.proactive.iaas.SigarLoaderTest.*;

import org.junit.Ignore;
import org.ow2.proactive.iaas.monitoring.MonitoringClient;
import org.ow2.proactive.iaas.monitoring.vmprocesses.VMPLister;


@Ignore
public class ClientTest implements MonitoringClient {

    private static Map<String, Map<String, Object>> results;
    static {
        restore();
    }

    /**
     * In host1 -> vm1 and vm2
     * In host2 -> vm3 and vm4
     */
    public static void restore() {
        results = new HashMap<String, Map<String, Object>>();

        {
            Map<String, Object> host1 = new HashMap<String, Object>();
            results.put(HOST1_URL, host1);
            host1.put(P_COMMON_ID.get(), HOST1_ID);
            host1.put(P_HOST_VM_ID.get(VM1_ID), VM1_ID);
            host1.put(P_HOST_VM_MAC.get(VM1_ID, 0), VM1_MAC);
            host1.put(P_HOST_VM_ID.get(VM2_ID), VM2_ID);
            host1.put(P_HOST_VM_MAC.get(VM2_ID, 0), VM2_MAC);
            host1.put(VMPLister.VMS_KEY, VM1_ID + VMPLister.VMS_SEPARATOR + VM2_ID);
        }

        {
            Map<String, Object> host2 = new HashMap<String, Object>();
            results.put(HOST2_URL, host2);
            host2.put(P_COMMON_ID.get(), HOST2_ID);
            host2.put(P_HOST_VM_ID.get(VM3_ID), VM3_ID);
            host2.put(P_HOST_VM_MAC.get(VM3_ID, 0), VM3_MAC);
            host2.put(P_HOST_VM_ID.get(VM4_ID), VM4_ID);
            host2.put(P_HOST_VM_MAC.get(VM4_ID, 0), VM4_MAC);
            host2.put(VMPLister.VMS_KEY, VM3_ID + VMPLister.VMS_SEPARATOR + VM4_ID);
        }

        {
            Map<String, Object> vm1 = new HashMap<String, Object>();
            results.put(VM1_URL, vm1);
            vm1.put(P_COMMON_ID.get(), VM1_ID);
            vm1.put(P_TEST_PROP_FROM_VM_SIGAR.get(), VM1_PROP_VM_SIGAR);
            vm1.put(P_COMMON_NET_MAC.get(0), VM1_MAC);
            vm1.put(P_SIGAR_JMX_URL.get(), VM1_URL);
        }

        {
            Map<String, Object> vm2 = new HashMap<String, Object>();
            results.put(VM2_URL, vm2);
            vm2.put(P_COMMON_ID.get(), VM2_ID);
            vm2.put(P_TEST_PROP_FROM_VM_SIGAR.get(), VM2_PROP_VM_SIGAR);
            vm2.put(P_COMMON_NET_MAC.get(0), VM2_MAC);
            vm2.put(P_SIGAR_JMX_URL.get(), VM2_URL);
        }

        {
            Map<String, Object> vm3 = new HashMap<String, Object>();
            results.put(VM3_URL, vm3);
            vm3.put(P_COMMON_ID.get(), VM3_ID);
            vm3.put(P_COMMON_NET_MAC.get(0), VM3_MAC);
            vm3.put(P_SIGAR_JMX_URL.get(), VM3_URL);
        }

        {
            Map<String, Object> vm4 = new HashMap<String, Object>();
            results.put(VM4_URL, vm4);
            vm4.put(P_COMMON_ID.get(), VM4_ID);
            vm4.put(P_COMMON_NET_MAC.get(0), VM4_MAC);
            vm4.put(P_SIGAR_JMX_URL.get(), VM4_URL);
        }
    }

    private String target;

    public static Map<String, Map<String, Object>> getResults() {
        return results;
    }

    @Override
    public void configure(String target, Map<String, Object> env) throws IOException {
        this.target = target;
    }

    @Override
    public Map<String, Object> getPropertyMap(int mask) throws IOException {
        return results.get(target);
    }

    @Override
    public void disconnect() throws IOException {
        // Do nothing.
    }
    
    public static Map<String, Object> getSigarFailureMap(String jmxurl) {
        Map<String, Object> r = new HashMap<String, Object>();
        r.put(P_DEBUG_NUMBER_OF_ERRORS.get(), 4);
        r.put(P_SIGAR_JMX_URL.get(), jmxurl);
        return r;
    }
}