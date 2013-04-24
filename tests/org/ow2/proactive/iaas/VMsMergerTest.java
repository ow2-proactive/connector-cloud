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
import org.junit.After;
import java.util.HashMap;
import org.ow2.proactive.iaas.utils.VMsMerger;


public class VMsMergerTest {

    @Test
    public void macResolution() throws Exception {
        // Scenario. 
        // The user requests getVMProperties('vmidx').
        // Some VM information obtained from IaaS Api (VM id 'vmidx') .
        // VM has an RMNode.
        // Hosts contain RMNode, from its processes lists the same VM can be
        // identified so its VM information is extended.

        // There is a VM. 
        // Each VM has some properties coming from either IaaS Api or from 
        // Sigar monitoring (so an RMNode is running in the VM).
        String vmId;
        Map<String, String> currentVMProperties;

        // There are many hosts.
        // Each host has a map with host properties, with keys vm.vmid.prop 
        // containing properties identified through listing the host processes. 
        Map<String, Object> hostsMap;

        // This host is where the VM is running.
        Map<String, Object> host1 = new HashMap<String, Object>();
        host1.put("host.cpu.usage", "0.1");
        host1.put("host.memory.usage", "200");
        host1.put("vm.vmid1.mac", "00:00:00:00:00:00");
        host1.put("vm.vmid1.shouldNOTbeadded", "notToBeAdded");
        host1.put("vm.vmidx.mac", "00:00:00:00:00:0a"); // Right VM info.
        host1.put("vm.vmidx.property.that.should.be.added", "toBeAdded1"); // Property to add.
        host1.put("vm.vmidx.property.that.should.be.also.added", "toBeAdded2"); // Property to add.

        // This host is NOT where the VM is running.
        Map<String, Object> host2 = new HashMap<String, Object>();
        host2.put("host.cpu.usage", "0.1");
        host2.put("host.memory.usage", "200");
        host2.put("vm.vmid2.mac", "00:00:00:00:00:02");
        host2.put("vm.vmid2.property.that.should.NOT.be.added", "toBeAdded1");
        host2.put("vm.vmid3.mac", "00:00:00:00:00:03");
        host2.put("vm.vmid3.property.that.should.NOT.be.added", "toBeAdded1");

        hostsMap = new HashMap<String, Object>();

        hostsMap.put("host1", host1);
        hostsMap.put("host2", host2);

        Map<String, Object> sigarsMap;

        // This host is where the VM is running.
        Map<String, Object> sigar1 = new HashMap<String, Object>();
        sigar1.put("host.cpu.usage", "0.1tobeAdded");
        sigar1.put("host.memory.usage", "200tobeAdded");
        sigar1.put("network.count", "2");
        sigar1.put("network.0.mac", "00:00:00:00:00:00");
        sigar1.put("network.1.mac", "00:00:00:00:00:0a"); // right MAC

        // This host is NOT where the VM is running.
        Map<String, Object> sigar2 = new HashMap<String, Object>();
        sigar2.put("host.cpu.usage", "0.1");
        sigar2.put("host.memory.usage", "200");
        sigar2.put("network.count", "2");
        sigar2.put("network.0.mac", "00:00:00:00:00:0b");
        sigar2.put("network.1.mac", "00:00:00:00:00:0c");

        sigarsMap = new HashMap<String, Object>();

        sigarsMap.put("nodeSigar1", sigar1);
        sigarsMap.put("nodeSigar2", sigar2);

        vmId = "vmidx";

        currentVMProperties = new HashMap<String, String>();

        // MAC addresses of the VM obtained through Sigar (RMNode running on it).
        currentVMProperties.put("vm.cpu.usage", "0.2");
        currentVMProperties.put("network.count", "2");
        currentVMProperties.put("network.0.mac", "00:00:00:00:00:0A"); // coming from the VMProcess.
        currentVMProperties.put("network.1.mac", "00:00:00:00:00:0D"); // coming from the VMProcess.

        Map<String, String> ex = VMsMerger.getExtraVMPropertiesUsingMac(vmId, currentVMProperties, hostsMap,
                sigarsMap);
        System.out.println(ex);

        assertTrue(ex.size() == 3);
        assertTrue(ex.get("property.that.should.be.added").equals("toBeAdded1"));
        assertTrue(ex.get("property.that.should.be.also.added").equals("toBeAdded2"));
    }

    @Test
    public void noSigarTest() throws Exception {
        // Scenario. 
        // VM information obtained from IaaS Api (VM id 'vmidx') .
        // VM has no RMNode.
        // Hosts contain RMNode, from its processes lists the same VM can be
        // identified so its VM information is extended.

        // There is a VM. 
        // Each VM has some properties coming from either IaaS Api or from 
        // Sigar monitoring (so an RMNode is running in the VM).
        String vmId;
        Map<String, String> currentVMProperties;

        // There are many hosts.
        // Each host has a map with host properties, with keys vm.vmid.prop 
        // containing properties identified through listing the host processes. 
        Map<String, Object> hostsMap;

        vmId = "vmidx";

        currentVMProperties = new HashMap<String, String>();

        // There is only this information available coming from the IaaS API 
        // (since in the VM there is no RMNode).
        currentVMProperties.put("status", "on");

        // This host is where the VM is running.
        Map<String, Object> host1 = new HashMap<String, Object>();
        host1.put("host.cpu.usage", "0.1");
        host1.put("host.memory.usage", "200");
        host1.put("vm.vmid1.mac", "00:00:00:00:00:00");
        host1.put("vm.vmid1.shouldNOTbeadded", "notToBeAdded");
        host1.put("vm.vmidx.mac", "00:00:00:00:00:0a"); // Right VM info.
        host1.put("vm.vmidx.property.that.should.be.added", "toBeAdded1"); // Property to add.
        host1.put("vm.vmidx.property.that.should.be.also.added", "toBeAdded2"); // Property to add.

        // This host is NOT where the VM is running.
        Map<String, Object> host2 = new HashMap<String, Object>();
        host2.put("host.cpu.usage", "0.1");
        host2.put("host.memory.usage", "200");
        host2.put("vm.vmid2.mac", "00:00:00:00:00:02");
        host2.put("vm.vmid2.property.that.should.NOT.be.added", "toBeAdded1");
        host2.put("vm.vmid3.mac", "00:00:00:00:00:03");
        host2.put("vm.vmid3.property.that.should.NOT.be.added", "toBeAdded1");

        hostsMap = new HashMap<String, Object>();

        hostsMap.put("host1", host1);
        hostsMap.put("host2", host2);

        Map<String, String> ex = VMsMerger.getExtraVMPropertiesByVMId(vmId, currentVMProperties, hostsMap);
        System.out.println(ex);

        assertTrue(ex.size() == 3);
        assertTrue(ex.get("property.that.should.be.added").equals("toBeAdded1"));
        assertTrue(ex.get("property.that.should.be.also.added").equals("toBeAdded2"));
    }

    private void assertTrue(boolean b) throws Exception {
        if (!b) {
            throw new Exception("Test failed.");
        }
    }
    
}
