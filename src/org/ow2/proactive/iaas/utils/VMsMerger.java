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

import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map.Entry;
import org.apache.log4j.Logger;
import org.ow2.proactive.iaas.monitoring.IaasConst;

import com.google.common.collect.Maps;


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
    public static Map<String, String> getExtraVMPropertiesFromVMRMNodes(String vmId,
            Map<String, String> vmProperties, Map<String, Object> sigarsMap) {

        // We assume sigar information is available. 
        List<String> vmMacs = getMacs(vmProperties);

        if (vmMacs.isEmpty()) {
            logger.warn("No MACs found in VM " + vmId + ": " + vmProperties);
            return Collections.<String,String>emptyMap();
        }

        for (Entry<String, Object> sigar : sigarsMap.entrySet()) {
            String mac;
            int i = 0;
            Map<String, Object> props = (Map<String, Object>) sigar.getValue();

            while ((mac = (String) props.get(IaasConst.P_COMMON_NET_MAC.get(i++))) != null) {
                if (vmMacs.contains(mac.toUpperCase())) {
                    return Utils.convertToStringMap(props);
                }
            }
        }
        
        logger.warn("Could not match VM " + vmId + " with any VM RMNode...");
        return Collections.<String,String>emptyMap();
    }

    /**
     * The goal is to add to the scare information of a VM coming from the IaaS API some information like 
     * process memory usage and cpu usage as listed in the host where this VM is hosted (a kvm process for instance). 
     * @param vmId 
     * @param vmProperties 
     * @param hostsMap
     * @return the set of new extra properties of the VM.
     */
    public static Map<String, String> getExtraVMPropertiesFromHostRMNodes(String vmId,
            Map<String, String> vmProperties, Map<String, Object> hostsMap) {

        for (Entry<String, Object> host : hostsMap.entrySet()) {
            Map<String, Object> hostProps = (Map<String, Object>) host.getValue();
            if (hostProps.containsKey(IaasConst.P_HOST_VM_ID.get(vmId))) {
                Map<String, String> ret = new HashMap<String, String>();
                ret.putAll(getVMPropertiesFromHostMap(vmId, hostProps));
                ret.put(IaasConst.P_VM_HOST.get(), host.getKey());
                return ret;
            }
        }
        logger.warn("Could not match " + vmId + " with any host RMNode...");
        return Collections.<String,String>emptyMap();
    }

    private static Map<String, String> getVMPropertiesFromHostMap(String vmId, Map<String, Object> props) {
        Map<String, String> output = new HashMap<String, String>();

        String prefix = IaasConst.P_HOST_VM_PREFIX.get(vmId);
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
        while ((mac = vmProperties.get(IaasConst.P_COMMON_NET_MAC.get(i++))) != null) {
            output.add(mac.toUpperCase());
        }
        return output;
    }

}
