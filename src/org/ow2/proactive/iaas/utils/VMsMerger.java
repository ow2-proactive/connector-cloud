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
package org.ow2.proactive.iaas.utils;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map.Entry;
import org.apache.log4j.Logger;

public class VMsMerger {

    private static final Logger logger = Logger.getLogger(VMsMerger.class);

    /**
     * The goal is to add to the current VMProperties (obtained from IaaS API) and VMProcesses 
     * some properties obtained from Sigar agents. But first we need to find out what Sigar agent 
     * really matches the already provided information. 
     * @param vmId 
     * @param vmProperties 
     * @param sigarsMap
     * @return the set of new extra properties of the VM.
     */
    public static Map<String, String> getExtraVMPropertiesUsingMac(String vmId,
            Map<String, String> vmProperties, Map<String, Object> sigarsMap) {
        Map<String, String> output = new HashMap<String, String>();

        // We assume sigar information is available. 
        List<String> vmMacs = getMacs(vmProperties);

        if (vmMacs.size() > 0) {
            logger.debug("MACs of the current VM: " + vmMacs);
        } else {
            logger.warn("No MACs found in VM " + vmId + ": " + vmProperties);
        }

        if (vmMacs.isEmpty())
            return output;

        logger.debug("Analysing sigars...");
        for (Entry<String, Object> sigar : sigarsMap.entrySet()) {
            logger.debug("   Sigar: " + sigar.getKey());
            Map<String, Object> props = (Map<String, Object>) sigar.getValue();
            for (String key : props.keySet()) {
                logger.debug("      Key: " + key);
                if (isVMMacKey(key)) {
                    String mac = props.get(key).toString().toUpperCase();
                    if (vmMacs.contains(mac)) {
                        logger.debug("         Found!");
                        //return getProperties(vmId, key, props);
                        return Utils.convertToStringMap(props);
                    }
                } else {
                    logger.debug("                  No mac key (network.x.mac) " + key + "...");
                }
            }
        }
        return output;
    }

    /**
     * The goal is to add to the scare information of a VM coming from the IaaS API some information like 
     * process memory usage and cpu usage as listed in the host where this VM is hosted (a kvm process for instance). 
     * @param vmId 
     * @param vmProperties 
     * @param hostsMap
     * @return the set of new extra properties of the VM.
     */
    public static Map<String, String> getExtraVMPropertiesByVMId(String vmId,
            Map<String, String> vmProperties, Map<String, Object> hostsMap) {
        Map<String, String> output = new HashMap<String, String>();

        logger.debug("VM '" + vmId + "': extending basic properties...");
        logger.debug("Analysing hosts...");
        for (String hostid : hostsMap.keySet()) {
            logger.debug("   Host: " + hostid);
            Map<String, Object> hostProps = (Map<String, Object>) hostsMap.get(hostid);
            for (String key : hostProps.keySet()) {
                logger.debug("      Key: " + key);
                if (isVMKey(key, vmId)) {
                    logger.debug("         Found!");
                    return getVMPropertiesFromHostMap(vmId, key, hostProps);
                } else {
                    logger.debug("                  Skipping property " + key + "...");
                }
            }
        }
        return output;
    }

    private static boolean isVMKey(String key, String vmId) {
        return (key.startsWith("vm." + vmId + "."));
    }

    private static boolean isVMMacKey(String key) {
        return (key.startsWith("network.") && key.endsWith(".mac"));
    }

    private static Map<String, String> getVMPropertiesFromHostMap(String vmId, String key,
            Map<String, Object> props) {
        Map<String, String> output = new HashMap<String, String>();

        String prefix = "vm." + vmId + ".";
        for (String k : props.keySet()) {
            if (k.startsWith(prefix)) {
                try {
                    output.put(k.substring(prefix.length()), props.get(k).toString());
                } catch (Exception e) {
                    // Ignore it.
                }
            }
        }

        return output;
    }

    private static List<String> getMacs(Map<String, String> vmProperties) {
        List<String> output = new ArrayList<String>();
        String mac = null;
        int i = 0;
        while ((mac = vmProperties.get("network." + i++ + ".mac")) != null) {
            output.add(mac.toUpperCase());
        }
        return output;
    }

}
