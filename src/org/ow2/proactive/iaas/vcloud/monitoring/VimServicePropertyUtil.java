package org.ow2.proactive.iaas.vcloud.monitoring;

import java.util.Map;

public class VimServicePropertyUtil {

	private static class VM {
		public static void standardize(Map<String, String> propertyMap) {

		}
	}

	private static class HOST {
		public static void standardize(Map<String, String> propertyMap) {
			standardizeCommonProperties(propertyMap);
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

		}
	}

	private static void standardizeCommonProperties(
			Map<String, String> propertyMap) {
		// adjust value
		replaceUsagePropertyIfPresent(VimServiceConstants.PROP_CPU_USAGE,
				"cpu.usage", propertyMap);
	}

	private static void replaceFrequencyPropertyIfPresent(String oldKey,
			String newKey, Map<String, String> propertyMap) {
		String frequency = propertyMap.remove(oldKey);
		if (frequency != null) {
			frequency = Long.toString(Long.parseLong(frequency) / (long) 1000);
			propertyMap.put(newKey, frequency);
		}
	}

	private static void replaceUsagePropertyIfPresent(String oldKey,
			String newKey, Map<String, String> propertyMap) {
		String usage = propertyMap.remove(oldKey);
		if (usage != null) {
			usage = Float.toString(Float.parseFloat(usage) / (float) 10000);
			propertyMap.put(newKey, usage);
		}
	}

	private static void replaceMemoryTotalIfPresent(String oldKey,
			String newKey, Map<String, String> propertyMap) {
		String totalMemory = propertyMap.remove(oldKey);
		if (totalMemory != null) {

		}

	}

	public static void updateKeys(Map<String, String> propertyMap) {
		replaceKeyIfPresent(VimServiceConstants.PROP_HOST_CPU_CORES,
				"cpu.cores", propertyMap);

		replaceKeyIfPresent(VimServiceConstants.PROP_CPU_USAGE, "cpu.usage",
				propertyMap);

		replaceKeyIfPresent(VimServiceConstants.PROP_HOST_MEMORY_TOTAL,
				"memory.total", propertyMap);
		replaceKeyIfPresent(VimServiceConstants.PROP_MEM_USAGE, "memory.usage",
				propertyMap);

		replaceKeyIfPresent(VimServiceConstants.PROP_NET_RX_RATE,
				"network.0.rx", propertyMap);
		replaceKeyIfPresent(VimServiceConstants.PROP_NET_TX_RATE,
				"network.0.tx", propertyMap);
		replaceKeyIfPresent(VimServiceConstants.PROP_HOST_NETWORK_COUNT,
				"network.count", propertyMap);

		// VM
		replaceKeyIfPresent(VimServiceConstants.PROP_VM_CPU_CORES, "cpu.cores",
				propertyMap);
		replaceKeyIfPresent(VimServiceConstants.PROP_VM_MEMEORY_TOTAL,
				"memory.total", propertyMap);
	}

	private static void replaceKeyIfPresent(String oldKey, String newKey,
			Map<String, String> propertyMap) {
		if (propertyMap.containsKey(oldKey)) {
			propertyMap.put(newKey, propertyMap.remove(oldKey));
		}
	}

	// non-instantiable
	private VimServicePropertyUtil() {
	}

}
