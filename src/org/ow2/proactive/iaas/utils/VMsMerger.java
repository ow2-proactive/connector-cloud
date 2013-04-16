package org.ow2.proactive.iaas.utils;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;


public class VMsMerger {

    private static final Logger logger = Logger.getLogger(VMsMerger.class);
    
    /**
     * The goal is to add to the current VMProperties some properties obtained from the host processes
     * information like cpu usage, memory, etc. The problem is to find the right VMProcess (in all
     * the hosts) that matches the current VM.
     * @param vmProperties 
     * @param hostsMap
     * @return the set of new extra properties of the VM.
     */
    public static Map<String, String> enrichVMProperties(Map<String, String> vmProperties,
            Map<String, Object> hostsMap) {
        Map<String, String> output = new HashMap<String, String>();
        List<String> vmMacs = getMacs(vmProperties);

        logger.debug("MACs of the current VM: " + vmMacs);
        
        if (vmMacs.isEmpty())
            return output;
        
        logger.debug("Analysing hosts...");
        for (String hostid : hostsMap.keySet()) {
            logger.debug("   Host: " + hostid);
            Map<String, Object> props = (Map<String, Object>) hostsMap.get(hostid);
            for (String key : props.keySet()) {
                logger.debug("      Key: " + key);
                if (isMacKey(key)) {
                    String mac = props.get(key).toString().toUpperCase();
                    if (vmMacs.contains(mac)) {
                        logger.debug("         Found!");
                        return getProperties(key, props);
                    } 
                } else {
                    logger.debug("                  Skipping property " + key + "...");
                }
            }
        }
        return output;
    }

    private static boolean isMacKey(String key) {
        return (key.startsWith("vm.") && key.endsWith(".mac"));
    }

    private static Map<String, String> getProperties(String key, Map<String, Object> props) {
        Map<String, String> output = new HashMap<String, String>();

        String vmid = getVMIdFromKey(key);
        if (vmid == null) {
            throw new RuntimeException("Something bad here.");
        }

        logger.debug("VM Id identified: " + vmid);

        String prefix = "vm." + vmid;
        for (String k : props.keySet()) {
            if (k.startsWith(prefix)) {
                try {
                    output.put(k.substring(prefix.length() + 1), props.get(k).toString());
                } catch (Exception e) {
                    // Ignore it.
                }
            }
        }

        return output;
    }

    private static List<String> getMacs(Map<String, String> vmProperties) {
        List<String> output = new ArrayList<String>();
        String count = vmProperties.get("network.count");
        if (count != null) {
            int n = Integer.parseInt(count);
            for (int i = 0; i < n; i++) {
                String mac = vmProperties.get("network." + i + ".mac");
                output.add(mac.toUpperCase());
            }
        }
        return output;
    }

    private static String getVMIdFromKey(String key) {
        String regex = "vm\\.([\\w-]+)\\.mac";
        String vmid = null;
        Pattern strMatch = Pattern.compile(regex);
        Matcher m = strMatch.matcher(key);

        if (m.find()) {
            try {
                vmid = m.group(1);
            } catch (IndexOutOfBoundsException e) {
                // Ignore it.
            }
        }
        return vmid;
    }

    public static void main(String[] args) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("network.count", "2");
        map.put("network.0.mac", "aa");
        map.put("network.1.mac", "bb");
        Map<String, Object> hostsMap = new HashMap<String, Object>();
        Map<String, Object> host1 = new HashMap<String, Object>();
        host1.put("unkey", "unvalue");
        host1.put("unkey2", "unvalue2");
        host1.put("vm.vmid1.mac", "aaaa");
        host1.put("vm.vmid1.shouldNOTbeadded", "xxca");
        host1.put("vm.vmid2.mac", "aa");
        host1.put("vm.vmid2.shouldbeadded", "xxca");
        host1.put("vm.vmid2.shouldbeadded1", "xxca1");
        Map<String, Object> host2 = new HashMap<String, Object>();
        host2.put("unkey", "unvalue");
        host2.put("unkey2", "unvalue2");
        host2.put("vm.vmid1.mac", "aaaa");
        host2.put("vm.vmid2.mac", "ba");
        hostsMap.put("host1", host1);
        hostsMap.put("host2", host2);

        System.out.println(enrichVMProperties(map, hostsMap));
    }
}
