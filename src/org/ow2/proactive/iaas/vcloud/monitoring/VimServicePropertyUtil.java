package org.ow2.proactive.iaas.vcloud.monitoring;

import java.text.NumberFormat;
import java.util.Map;

public class VimServicePropertyUtil {

	public static class VM {
		public static void standardize(Map<String, String> propertyMap) {
			standardizeCommonProperties(propertyMap);
			replaceKeyIfPresent(VimServiceConstants.PROP_VM_CPU_CORES,
					"cpu.cores", propertyMap);
			replaceKeyIfPresent(VimServiceConstants.PROP_VM_MEMEORY_TOTAL,
					"mem.total", propertyMap);
			replaceKeyIfPresent(VimServiceConstants.PROP_VM_STORAGE_COMMITTED,
					"storage.used", propertyMap);
			replaceStorageUnCommitted(
					VimServiceConstants.PROP_VM_STORAGE_UNCOMMITTED,
					"storage.total", propertyMap);
		}
	}

	public static class HOST {
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
		replaceUsagePropertyIfPresent(VimServiceConstants.PROP_MEM_USAGE,
				"mem.usage", propertyMap);
		replaceKeyIfPresent(VimServiceConstants.PROP_NET_RX_RATE,
				"network.0.rx", propertyMap);
		replaceKeyIfPresent(VimServiceConstants.PROP_NET_TX_RATE,
				"network.0.tx", propertyMap);
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

	private static void replaceUsagePropertyIfPresent(String oldKey,
			String newKey, Map<String, String> propertyMap) {
		String usage = propertyMap.remove(oldKey);
		if (usage != null) {
			NumberFormat nf = NumberFormat.getInstance();
			nf.setMaximumFractionDigits(4);
			nf.setGroupingUsed(false);
			usage = nf.format(Float.parseFloat(usage) / 10000);
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

	// non-instantiable
	private VimServicePropertyUtil() {
	}

}
