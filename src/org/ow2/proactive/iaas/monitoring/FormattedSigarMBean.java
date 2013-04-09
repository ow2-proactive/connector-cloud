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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.ProcMem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;
import org.hyperic.sigar.cmd.Ps;

public class FormattedSigarMBean implements NodeAgentMXBean {

    private static final Logger logger = Logger
            .getLogger(FormattedSigarMBean.class);

    private final SigarProxy sigarProxy = new Sigar();

    public FormattedSigarMBean() {
    }
    
    public Map<String, String> getPropertyMap() {
        Map<String, String> propertyMap = new HashMap<String,String>();
        try {
            addCpuCoresProperty(propertyMap, getSigarProxy());
            addCpuFrequencyProperty(propertyMap, getSigarProxy());
            addCpuUsageProperty(propertyMap, getSigarProxy());
            addMemoryProperties(propertyMap, getSigarProxy());
            addNetworkProperties(propertyMap, getSigarProxy());
            addProcessProperties(propertyMap, getSigarProxy());
            addStorageProperties(propertyMap, getSigarProxy());
        } catch (SigarException se) {
            logger.error("", se);
        }
        return propertyMap;
        
    }

    
    private SigarProxy getSigarProxy() {
        return sigarProxy;
    }
    
    private void addCpuCoresProperty(Map<String, String> properties,
            SigarProxy sigar) throws SigarException {
        int cpuCores = sigar.getCpuInfoList()[0].getTotalCores();
        properties.put("cpu.cores", Integer.toString(cpuCores));
    }

    private void addCpuFrequencyProperty(Map<String, String> properties,
            SigarProxy sigar) throws SigarException {
        int fmhz = sigar.getCpuInfoList()[0].getMhz();
        float fghz = (float) fmhz / 1000;
        properties.put("cpu.frequency", Float.toString(fghz));
    }

    private void addCpuUsageProperty(Map<String, String> properties,
            SigarProxy sigar) throws SigarException {
        CpuPerc cpuPerc = sigar.getCpuPerc();
        float usage = (float) (1 - cpuPerc.getIdle());
        properties.put("cpu.usage", Float.toString(usage));
    }

    private void addMemoryProperties(Map<String,String> properties,
            SigarProxy sigar) throws SigarException {
        Mem mem = sigar.getMem();
        long total = mem.getTotal();
        properties.put("memory.total", Long.toString(total));
        long free = mem.getFree();
        properties.put("memory.free", Long.toString(free));
        long actualFree = mem.getActualFree();
        properties.put("memory.actualfree", Long.toString(actualFree));
    }

    private void addNetworkProperties(Map<String, String> properties,
            SigarProxy sigar) throws SigarException {
        int count = sigar.getNetInterfaceList().length;
        properties.put("network.count", Integer.toString(count));
        NetInterfaceConfig config = sigar.getNetInterfaceConfig();
        NetInterfaceStat stat = sigar.getNetInterfaceStat(config.getName());
        properties.put("network.0.tx", Long.toString(stat.getTxBytes()));
        properties.put("network.0.rx", Long.toString(stat.getRxBytes()));
    }

    private void addStorageProperties(Map<String, String> properties,
            SigarProxy sigar) throws SigarException {
        FileSystem[] fileSystemList = sigar.getFileSystemList();
        long total = 0, used = 0;
        for (FileSystem fs : fileSystemList) {
            if (FileSystem.TYPE_LOCAL_DISK == fs.getType()) {
                FileSystemUsage fsu = sigar.getFileSystemUsage(fs.getDirName());
                total += fsu.getTotal();
                used += fsu.getUsed();
            }
        }
        // TODO: Add network file systems
        properties.put("storage.used", Long.toString(used));
        properties.put("storage.total", Long.toString(total));
    }

    private void addProcessProperties(Map<String, String> properties,
            SigarProxy sigar) throws SigarException {
        long[] procList = sigar.getProcList();
        StringBuilder process = new StringBuilder();
        StringBuilder process3 = new StringBuilder();
        for (long pid : procList) {
            List<String> info = (List<String>) Ps.getInfo(sigar, pid);
            String procName = info.get(info.size() - 1);
            process.append(info.get(0)).append(';').append(procName)
                    .append(',');
            ProcCpu procCpu = sigar.getProcCpu(pid);
            long procCpuTotal = procCpu.getTotal();
            ProcMem procMem = sigar.getProcMem(pid);
            long procMemSize = procMem.getSize();
            process3.append(info.get(0)).append(';').append(procCpuTotal)
                    .append(';').append(procMemSize).append(',');
        }
        String ps = process.toString();
        properties.put("system.process", ps.substring(0, ps.length() - 1));
        String ps3 = process3.toString();
        properties.put("system.process.3", ps3.substring(0, ps3.length() - 1));
    }
    
}
