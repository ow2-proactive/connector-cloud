/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2012 INRIA/University of
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
 * ################################################################
 * %$ACTIVEEON_INITIAL_DEV$
 */

package org.ow2.proactive.iaas.vcloud.monitoring;

public class VimServiceConstants {

    // Static properties of ClusterComputeResource objects.
	public static final String PROP_CCR_HOST = "host";
	
	// Static properties which are common to both host systems and virtual
	// machines.
	public static final String PROP_NAME = "name";
	public static final String PROP_STATE = "summary.runtime.powerState";

	
	// Static properties of host systems.
	public static final String PROP_HOST_CPU_FREQUENCY = "summary.hardware.cpuMhz";
	public static final String PROP_HOST_CPU_CORES = "summary.hardware.numCpuCores";
	public static final String PROP_HOST_MEMORY_TOTAL = "summary.hardware.memorySize";
	public static final String PROP_HOST_MEM_USED = "summary.quickStats.overallMemoryUsage"; 
	public static final String PROP_HOST_NETWORK_COUNT = "summary.hardware.numNics";
	public static final String PROP_HOST_SYSTEM_IDENTIFICATION = "hardware.systemInfo.otherIdentifyingInfo";
	public static final String PROP_HOST_SITE = "parent";
	public static final String PROP_HOST_DS = "datastore";
	
	
	// Static properties of datastores
	public static final String PROP_DS_TYPE = "summary.type";
	public static final String PROP_DS_CAPACITY = "summary.capacity";
	public static final String PROP_DS_FREE_SPACE = "summary.freeSpace";

	// Static properties for VMs.
	public static final String PROP_VM_CPU_CORES = "summary.config.numCpu";
	public static final String PROP_VM_CPU_FREQUENCY = "summary.quickStats.staticCpuEntitlement";
	public static final String PROP_VM_MEMEORY_TOTAL = "summary.config.memorySizeMB";
	public static final String PROP_VM_STORAGE_COMMITTED = "summary.storage.committed";
	public static final String PROP_VM_STORAGE_UNCOMMITTED = "summary.storage.uncommitted";
	public static final String PROP_VM_NETWORK = "summary.config.numEthernetCards";
	public static final String PROP_VM_PARENT = "summary.runtime.host";
	public static final String PROP_VM_DISK = "guest.disk";

	/** Dynamic properties for both host systems and virtual machines. */

	// CPU usage as a percentage during the interval
	public static final String PROP_CPU_USAGE = "cpu.usage.AVERAGE";
	// Amount of memory that is actively used, as estimated by VMkernel based on
	// recently touched memory pages
	public static final String PROP_MEM_USAGE = "mem.active.AVERAGE";
	// Average amount of data received per second
	public static final String PROP_NET_RX_RATE = "net.bytesRx.AVERAGE";
	// Average amount of data transmitted per second
	public static final String PROP_NET_TX_RATE = "net.bytesTx.AVERAGE";
	// Network utilization (combined transmit-rates and receive-rates) during
	// the interval
	public static final String PROP_NET_USAGE = "net.usage.AVERAGE";

    public static final String[] HOST_STATIC_PROPERTIES = new String[] {
            PROP_HOST_CPU_CORES, PROP_HOST_CPU_FREQUENCY,
            PROP_HOST_MEMORY_TOTAL, PROP_HOST_MEM_USED,
            PROP_HOST_NETWORK_COUNT, PROP_HOST_SITE, PROP_STATE, PROP_HOST_DS };

	public static final String[] VM_STATIC_PROPERTIES = new String[] {
			PROP_VM_PARENT, PROP_VM_CPU_CORES, PROP_VM_CPU_FREQUENCY, PROP_VM_MEMEORY_TOTAL,
			PROP_VM_STORAGE_COMMITTED, PROP_VM_STORAGE_UNCOMMITTED,
			PROP_VM_NETWORK, PROP_STATE, PROP_VM_DISK};
    
    public static final String[] DS_STATIC_PROPERTIES = new String[] {
            PROP_DS_TYPE, PROP_DS_CAPACITY, PROP_DS_FREE_SPACE };

	public static final String[] DYNAMIC_PROPERTIES = new String[] {
			PROP_CPU_USAGE, PROP_MEM_USAGE, PROP_NET_RX_RATE, PROP_NET_TX_RATE,
			PROP_NET_USAGE };

	// non-instantiable
	private VimServiceConstants() {
	}

}
