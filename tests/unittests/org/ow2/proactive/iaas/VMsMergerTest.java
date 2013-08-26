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

import java.util.Map;

import org.junit.Test;

import java.util.HashMap;

import junit.framework.Assert;

import org.ow2.proactive.iaas.utils.VMsMerger;


public class VMsMergerTest {

    @Test
    public void macResolution() throws Exception {
        // Scenario. 
        // The user requests getVMProperties('vmidx').
        // A physical hosts (hyp) contain an RMNode, from its processes lists the same VM can be
        // identified so its VM information is extended.

        // There is a VM. 
        // Each VM has some properties coming from Sigar monitoring (so an RMNode is running in the VM).
        String vmId;
        Map<String, String> currentVMProperties;

        // There are many hosts.
        // Each host has a map with host properties, with keys vm.vmid.prop 
        // containing properties identified through listing the host processes. 
        Map<String, Object> hostsMap;

        // This host is where the VM is running.
        Map<String, Object> host1 = new HashMap<String, Object>();
        host1.put("host1.cpu.usage", "0.1");
        host1.put("host1.memory.usage", "200");
        host1.put("vm.vmid1.id", "vmid1");
        host1.put("vm.vmid1.network.0.mac", "00:00:00:00:00:00");
        host1.put("vm.vmid1.noadd1", "noadd1");
        host1.put("vm.vmidx.id", "vmidx");
        host1.put("vm.vmidx.network.0.mac", "00:00:00:00:00:0a"); // Right VM info.
        host1.put("vm.vmidx.toadd1", "toadd1"); // Property to add.
        host1.put("vm.vmidx.toadd2", "toadd2"); // Property to add.

        // This host is NOT where the VM is running.
        Map<String, Object> host2 = new HashMap<String, Object>();
        host2.put("host2.cpu.usage", "0.1");
        host2.put("host2.memory.usage", "200");
        host2.put("vm.vmid2.id", "vmid2");
        host2.put("vm.vmid2.network.0.mac", "00:00:00:00:00:02");
        host2.put("vm.vmid2.noadd1", "noadd1");
        host2.put("vm.vmid3.id", "vmid3");
        host2.put("vm.vmid3.network.0.mac", "00:00:00:00:00:03");
        host2.put("vm.vmid3.noadd1", "noadd1");

        hostsMap = new HashMap<String, Object>();

        hostsMap.put("host1", host1);
        hostsMap.put("host2", host2);

        Map<String, Object> sigarsMap;

        // This node is where the VM is running.
        Map<String, Object> sigar1 = new HashMap<String, Object>();
        sigar1.put("toadd3", "toadd3");
        sigar1.put("toadd4", "toadd4");
        sigar1.put("network.0.mac", "00:00:00:00:00:00");
        sigar1.put("network.1.mac", "00:00:00:00:00:0a"); // right MAC

        // This host is NOT where the VM is running.
        Map<String, Object> sigar2 = new HashMap<String, Object>();
        sigar2.put("noadd1", "noadd1");
        sigar2.put("network.0.mac", "00:00:00:00:00:0b");
        sigar2.put("network.1.mac", "00:00:00:00:00:0c");

        sigarsMap = new HashMap<String, Object>();

        sigarsMap.put("sigar1", sigar1);
        sigarsMap.put("sigar2", sigar2);

        vmId = "vmidx";

        currentVMProperties = new HashMap<String, String>();

        Map<String, String> fromVMP = VMsMerger.getExtraVMPropsFromHostProps(vmId, hostsMap);
        currentVMProperties.putAll(fromVMP);
        Map<String, String> fromSigar = VMsMerger.getExtraVMPropertiesFromVMRMNodes(vmId, currentVMProperties,
                sigarsMap);
        currentVMProperties.putAll(fromSigar);

        currentVMProperties.putAll(fromVMP);
        currentVMProperties.putAll(fromSigar);

        System.out.println("Result: " + currentVMProperties);

        Assert.assertTrue(fromVMP.get("toadd1").equals("toadd1"));
        Assert.assertTrue(fromVMP.get("toadd2").equals("toadd2"));
        Assert.assertTrue(fromSigar.get("toadd3").equals("toadd3"));
        Assert.assertTrue(fromSigar.get("toadd4").equals("toadd4"));
    }

}
