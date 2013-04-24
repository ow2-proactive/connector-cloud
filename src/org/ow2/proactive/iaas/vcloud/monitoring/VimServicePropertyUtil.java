package org.ow2.proactive.iaas.vcloud.monitoring;

import java.text.DecimalFormat;
import java.util.Map;

public class VimServicePropertyUtil {
	
	public static class VM {
		public static void standardize(Map<String, String> propertyMap) {
			replaceKeyIfPresent(VimServiceConstants.PROP_VM_PARENT, "host",
					propertyMap);
			replaceKeyIfPresent(VimServiceConstants.PROP_VM_CPU_CORES,
					"cpu.cores", propertyMap);
			
			replaceVmMemoryTotalPropertyIfPresent(VimServiceConstants.PROP_VM_MEMEORY_TOTAL,
					"memory.total", propertyMap);
			
			replaceKeyIfPresent(VimServiceConstants.PROP_VM_STORAGE_COMMITTED,
					"storage.used", propertyMap);
			replaceStorageUnCommitted(
					VimServiceConstants.PROP_VM_STORAGE_UNCOMMITTED,
					"storage.total", propertyMap);
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
			replaceKeyIfPresent(VimServiceConstants.PROP_HOST_MEMORY_TOTAL,
					"memory.total", propertyMap);
			replaceKeyIfPresent(VimServiceConstants.PROP_HOST_NETWORK_COUNT,
					"network.count", propertyMap);
			replaceKeyIfPresent(VimServiceConstants.PROP_HOST_SITE, "site",
					propertyMap);
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
			// convert from Mb to Kb.
			propertyMap
					.put(newKey, Long.toString(Long.parseLong(total) * 1024));
		}
	}

	private static void replaceUsagePropertyIfPresent(String oldKey,
			String newKey, Map<String, String> propertyMap) {
		String usage = propertyMap.remove(oldKey);
		if (usage != null) {
			DecimalFormat df = new DecimalFormat("#.#");
			df.setMaximumFractionDigits(4);
			df.setGroupingUsed(false);
			usage = df.format(Float.parseFloat(usage) / 10000);
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

	private static void setFreeMemoryProperty(Map<String, String> propertyMap) {
		String total = propertyMap.get("memory.total");
		String usage = propertyMap.get("memory.usage");
		if (total != null && usage != null) {
			float free = Long.parseLong(total)
					* (1 - Float.parseFloat(usage) / 100);
			propertyMap.put("memory.free", Long.toString((long) free));
		}
	}

	// non-instantiable
	private VimServicePropertyUtil() {
	}

}
