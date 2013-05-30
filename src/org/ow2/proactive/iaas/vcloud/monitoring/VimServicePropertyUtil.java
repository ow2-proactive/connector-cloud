package org.ow2.proactive.iaas.vcloud.monitoring;

import static org.ow2.proactive.iaas.vcloud.monitoring.VimServiceConstants.PROP_HOST_MEMORY_TOTAL;
import static org.ow2.proactive.iaas.vcloud.monitoring.VimServiceConstants.PROP_HOST_MEM_USED;
import static org.ow2.proactive.iaas.vcloud.monitoring.VimServiceConstants.PROP_MEM_USAGE;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VimServicePropertyUtil {

    public static class VM {
        public static void standardize(Map<String, String> propertyMap) {
            replaceKeyIfPresent(VimServiceConstants.PROP_VM_PARENT, "host",
                    propertyMap);
            replaceKeyIfPresent(VimServiceConstants.PROP_VM_CPU_CORES,
                    "cpu.cores", propertyMap);

            // adjust value, MHz -> GHz
            replaceFrequencyPropertyIfPresent(
                    VimServiceConstants.PROP_VM_CPU_FREQUENCY, "cpu.frequency",
                    propertyMap);

            replaceVmMemoryTotalPropertyIfPresent(
                    VimServiceConstants.PROP_VM_MEMEORY_TOTAL, "memory.total",
                    propertyMap);

            /*
            replaceKeyIfPresent(VimServiceConstants.PROP_VM_STORAGE_COMMITTED,
                    "storage.used", propertyMap);
            replaceStorageUnCommitted(
                    VimServiceConstants.PROP_VM_STORAGE_UNCOMMITTED,
                    "storage.total", propertyMap);
            */
            setVmStorageUsageProperties(propertyMap);
            
            replaceKeyIfPresent(VimServiceConstants.PROP_VM_NETWORK,
                    "network.count", propertyMap);
            standardizeCommonProperties(propertyMap);
        }
    }

    public static class HOST {
        public static void standardize(Map<String, String> propertyMap) {
            replaceKeyIfPresent(VimServiceConstants.PROP_HOST_CPU_CORES,
                    "cpu.cores", propertyMap);
            // adjust value, MHz -> GHz
            replaceFrequencyPropertyIfPresent(
                    VimServiceConstants.PROP_HOST_CPU_FREQUENCY,
                    "cpu.frequency", propertyMap);
//            replaceKeyIfPresent(VimServiceConstants.PROP_HOST_MEMORY_TOTAL,
//                    "memory.total", propertyMap);
            setHostMemoryUsageProperties(propertyMap);
            replaceKeyIfPresent(VimServiceConstants.PROP_HOST_NETWORK_COUNT,
                    "network.count", propertyMap);
            replaceKeyIfPresent(VimServiceConstants.PROP_HOST_SITE, "site",
                    propertyMap);
            setHostStorageUsageProperties(propertyMap);
            standardizeCommonProperties(propertyMap);
        }
    }

    private static void standardizeCommonProperties(
            Map<String, String> propertyMap) {
        // adjust value
        replaceUsagePropertyIfPresent(VimServiceConstants.PROP_CPU_USAGE,
                "cpu.usage", propertyMap);
        replaceUsagePropertyIfPresent(VimServiceConstants.PROP_MEM_USAGE,
                "memory.usage", propertyMap);
        setFreeMemoryProperty(propertyMap);
        replaceKeyIfPresent(VimServiceConstants.PROP_NET_RX_RATE, "network.rx",
                propertyMap);
        replaceKeyIfPresent(VimServiceConstants.PROP_NET_TX_RATE, "network.tx",
                propertyMap);
        replaceKeyIfPresent(VimServiceConstants.PROP_NET_USAGE,
                "network.speed", propertyMap);
        replaceStatus(VimServiceConstants.PROP_STATE, "status", propertyMap);
    }

    private static void replaceFrequencyPropertyIfPresent(String oldKey,
            String newKey, Map<String, String> propertyMap) {
        String frequency = propertyMap.remove(oldKey);
        if (frequency != null) {
            frequency = Float
                    .toString((float) Long.parseLong(frequency) / 1000);
            propertyMap.put(newKey, frequency);
        }
    }

    private static void replaceVmMemoryTotalPropertyIfPresent(String oldKey,
            String newKey, Map<String, String> propertyMap) {
        String total = propertyMap.remove(oldKey);
        if (total != null) {
            // convert from Mb to bytes.
            propertyMap
                    .put(newKey, Long.toString(Long.parseLong(total) * 1024 * 1024));
        }
    }

    private static void replaceUsagePropertyIfPresent(String oldKey,
            String newKey, Map<String, String> propertyMap) {
        String usage = propertyMap.remove(oldKey);
        if (usage != null) {
            DecimalFormat format = new DecimalFormat();
            format.setMaximumFractionDigits(4);
            format.setGroupingUsed(false);
            DecimalFormatSymbols custom = new DecimalFormatSymbols();
            custom.setDecimalSeparator('.');
            format.setDecimalFormatSymbols(custom);
            usage = format.format(Float.parseFloat(usage) / 10000);
            propertyMap.put(newKey, usage);
        }
    }

    private static void replaceStorageUnCommitted(String oldKey, String newKey,
            Map<String, String> propertyMap) {
        String storageUncommitted = propertyMap.remove(oldKey);
        if (storageUncommitted != null) {
            String storageCommitted = propertyMap.get("storage.used");
            if (storageCommitted != null) {
                String total = Long.toString(Long.parseLong(storageUncommitted)
                        + Long.parseLong(storageCommitted));
                propertyMap.put(newKey, total);
            }
        }
    }

    private static void replaceStatus(String oldKey, String newKey,
            Map<String, String> propertyMap) {
        String status = propertyMap.remove(oldKey);
        if (status != null) {
            status = ("POWERED_ON".equalsIgnoreCase(status)) ? "up" : "down";
            propertyMap.put(newKey, status);
        }
    }

    private static void replaceKeyIfPresent(String oldKey, String newKey,
            Map<String, String> propertyMap) {
        if (propertyMap.containsKey(oldKey)) {
            propertyMap.put(newKey, propertyMap.remove(oldKey));
        }
    }
    
    private static void setHostMemoryUsageProperties(
            Map<String, String> propertyMap) {
        long capacity = -1, used = -1;
        if (propertyMap.containsKey(PROP_HOST_MEMORY_TOTAL)) {
            capacity = Long.valueOf(propertyMap.get(PROP_HOST_MEMORY_TOTAL));
            propertyMap.put("memory.total", String.valueOf(capacity));
            propertyMap.remove(PROP_HOST_MEMORY_TOTAL);
        }
        if (propertyMap.containsKey(PROP_HOST_MEM_USED)) {
            used = Long.valueOf(propertyMap.get(PROP_HOST_MEM_USED)) * 1024 * 1024;
            propertyMap.put("memory.used", String.valueOf(used));
            if (capacity != -1) {
                String free = String.valueOf(capacity - used);
                propertyMap.put("memory.free", free);
                // FIXME:
                propertyMap.put("memory.actualfree", free);
                propertyMap.remove(PROP_HOST_MEM_USED);
            }
        }
        
        propertyMap.remove(PROP_MEM_USAGE);
    }

    private static void setFreeMemoryProperty(Map<String, String> propertyMap) {
        String total = propertyMap.get("memory.total");
        String usage = propertyMap.get("memory.usage");
        if (total != null && usage != null) {
            float free = Long.parseLong(total)
                    * (1 - Float.parseFloat(usage) / 100);
            propertyMap.put("memory.free", Long.toString((long) free));
            propertyMap.put("memory.actualfree", Long.toString((long) free));
        }
    }

    private static void setVmStorageUsageProperties(
            Map<String, String> propertyMap) {
        long total = 0;
        long free = 0;
        long used = 0;
        List<String> vmDiskInfoKeys = new ArrayList<String>();
        for (String key : propertyMap.keySet()) {
            if (key.startsWith("vm.disk.")) {
                vmDiskInfoKeys.add(key);
                long value = Long.valueOf(propertyMap.get(key));
                if (key.endsWith(".total")) {
                    total += value;
                } else if (key.endsWith(".free")) {
                    free += value;
                } else if (key.endsWith(".used")) {
                    used += value;
                } else {
                    // FIXME: consider throwing an exception
                }
            }
        }
        for (String key : vmDiskInfoKeys) {
            propertyMap.remove(key);
        }
        propertyMap.put("storage.total", String.valueOf(total));
        propertyMap.put("storage.free", String.valueOf(free));
        propertyMap.put("storage.used", String.valueOf(used));
    }
     
    private static void setHostStorageUsageProperties(Map<String,String> propertyMap) {
        List<String> hostDataStoreKeys = new ArrayList<String>();
        long capacity = -1, free = -1;
        for (String key: propertyMap.keySet()) {
            if (key.startsWith("host.datastore.")) {
                hostDataStoreKeys.add(key);
                if (key.endsWith(".total")) {
                    capacity = Long.parseLong(propertyMap.get(key));
                } else if (key.endsWith(".free")) {
                    free = Long.parseLong(propertyMap.get(key));
                }
                if (capacity != -1 && free != -1 ) {
                    break;
                }
            }
        }
        free = (free == -1 ) ? 0 : free;
        long used = capacity - free;
        propertyMap.put("storage.total", String.valueOf(capacity));
        propertyMap
                .put("storage.free", String.valueOf(free));
        propertyMap.put("storage.used", String.valueOf(used));
        for (String key : hostDataStoreKeys) {
            propertyMap.remove(key);
        }
    }

    // non-instantiable
    private VimServicePropertyUtil() {
    }

}
