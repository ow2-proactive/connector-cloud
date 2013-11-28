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

package org.ow2.proactive.iaas.monitoring;

public enum IaasConst {

    P_COMMON_ID("id"),
    P_COMMON_CPU_USAGE("cpu.usage"),
    P_COMMON_CPU_CORES("cpu.cores"),
    P_COMMON_CPU_FREQUENCY("cpu.frequency"),

    P_COMMON_MEM_TOTAL("memory.total"),
    P_COMMON_MEM_FREE("memory.free"),
    P_COMMON_MEM_ACTUAL_FREE("memory.actualfree"),

    P_COMMON_NET_NAME("network.%x%.name"),
    P_COMMON_NET_MAC("network.%x%.mac"),
    P_COMMON_NET_IP("network.%x%.ip"),
    P_COMMON_NET_SPEED("network.%x%.speed"),
    P_COMMON_NET_TX("network.%x%.tx"),
    P_COMMON_NET_RX("network.%x%.rx"),
    P_COMMON_NET_TX_TOTAL("network.tx"),
    P_COMMON_NET_RX_TOTAL("network.rx"),
    P_COMMON_NET_SPEED_TOTAL("network.speed"),
    P_COMMON_NET_COUNT_TOTAL("network.count"),

    P_COMMON_STORAGE_NAME("storage.%x%.name"),
    P_COMMON_STORAGE_TOTAL("storage.%x%.total"),
    P_COMMON_STORAGE_USED("storage.%x%.used"),

    P_COMMON_STORAGE_COUNT_TOTAL("storage.count"),
    P_COMMON_STORAGE_TOTAL_TOTAL("storage.total"),
    P_COMMON_STORAGE_USED_TOTAL("storage.used"),

    P_COMMON_SYSTEM_PROCESS("system.process"),
    P_COMMON_SYSTEM_PROCESS3("system.process.3"),

    P_COMMON_STATUS("status"),

    P_COMMON_PFLAGS("pflags.%x%"),


    P_VM_VMUSAGE("memory.virtualmemorybytes"),
    P_VM_HOST("host"),

    P_HOST_SITE("site"),
    P_HOST_VMS("vmsinfo"),
    P_HOST_VM_PREFIX("vm.%x%."),
    P_HOST_VM_PROP("vm.%x%.%y%"),
    P_HOST_VM_ID("vm.%x%.id"),
    P_HOST_VM_MAC("vm.%x%.network.%y%.mac"),

    P_SIGAR_JMX_URL("proactive.sigar.jmx.url"),

    /**
     * DEBUG constants.
     */
    P_DEBUG_SIGAR_USED("debug.sigar.used"),
    P_DEBUG_NUMBER_OF_ERRORS("debug.sigar.errors"),

    /**
     * Test constants.
     */

    P_TEST_PROP_FROM_API("property.from.api"),
    P_TEST_PROP_FROM_HOST_PROC("property.from.host.processes"),
    P_TEST_PROP_FROM_VM_SIGAR("property.from.vm.sigar");

    protected String pattern;

    private IaasConst(String pattern) {
        this.pattern = pattern;
    }

    public String toString() {

        if (pattern.contains("%x%"))
            throw new IllegalArgumentException("This pattern requires one or more arguments.");

        return pattern;
    }

    public String toString(Object id) {
        if (pattern.contains("%y%"))
            throw new IllegalArgumentException("This pattern requires more than one argument.");

        return pattern.replace("%x%", String.valueOf(id));
    }

    public String toString(Object id1, Object id2) {
        return pattern.replace("%x%", String.valueOf(id1)).replace("%y%", String.valueOf(id2));
    }

}
