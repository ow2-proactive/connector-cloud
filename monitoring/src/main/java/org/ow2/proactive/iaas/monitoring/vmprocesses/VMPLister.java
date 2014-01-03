package org.ow2.proactive.iaas.monitoring.vmprocesses;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;

import org.apache.log4j.Logger;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;
import org.ow2.proactive.iaas.monitoring.IaasConst;
import org.ow2.proactive.iaas.monitoring.IaasMonitoringException;
import org.ow2.proactive.iaas.utils.Utils;


public class VMPLister {

    private static final Logger logger = Logger.getLogger(VMPLister.class);

    public static final String VMS_KEY = "vms";
    public static final String VMS_SEPARATOR = ";";

    public static List<VMProcess> getRemoteVMPs(JMXConnector connector) throws IaasMonitoringException {

        List<VMProcess> vmps = new ArrayList<VMProcess>();

        try {
            ObjectName oname = new ObjectName("sigar:Type=Processes");
            Object a = connector.getMBeanServerConnection().getAttribute(oname, "Processes");
            CompositeData[] bb = (CompositeData[]) a;
            for (CompositeData c : bb) {
                Integer pid = (Integer) c.get("pid");
                String desc = (String) c.get("description");
                String mem = (String) c.get("memSize");
                String cpu = (String) c.get("cpuPerc");
                String[] vargs = (String[]) c.get("commandline");

                String args = Utils.argsToString(vargs);
                VMPPattern pat = VMPPattern.whatVMPPatternMatches(args);
                if (pat != null) { // Valid VM process.
                    VMProcess proc = pat.getVMP(pid, args);

                    proc.setProperty(IaasConst.P_COMMON_CPU_USAGE.toString(), cpu);
                    proc.setProperty(IaasConst.P_VM_VMUSAGE.toString(), mem);

                    vmps.add(proc);
                }
            }
        } catch (AttributeNotFoundException e) {
            throw new IaasMonitoringException(e);
        } catch (InstanceNotFoundException e) {
            throw new IaasMonitoringException(e);
        } catch (MBeanException e) {
            throw new IaasMonitoringException(e);
        } catch (ReflectionException e) {
            throw new IaasMonitoringException(e);
        } catch (IOException e) {
            throw new IaasMonitoringException(e);
        } catch (MalformedObjectNameException e) {
            throw new IaasMonitoringException(e);
        }

        return vmps;
    }

    public static List<VMProcess> getLocalVMPs(SigarProxy sigar) throws IaasMonitoringException {

        List<VMProcess> vmps = new ArrayList<VMProcess>();
        long[] listpids;

        try {
            listpids = sigar.getProcList();
        } catch (SigarException e) {
            throw new IaasMonitoringException(e);
        }

        for (long pid : listpids) {

            try {
                String args = Utils.argsToString(sigar.getProcArgs(pid));
                VMPPattern pat = VMPPattern.whatVMPPatternMatches(args);
                if (pat != null) {
                    VMProcess proc = pat.getVMP(pid, args);

                    // http://www.hyperic.com/support/docs/sigar/
                    proc.setProperty(IaasConst.P_COMMON_CPU_USAGE.toString(), new Double(sigar.getProcCpu(pid).getPercent()));
                    proc.setProperty(IaasConst.P_VM_VMUSAGE.toString(), new Double(sigar.getProcMem(pid).getSize()));
                    proc.setProperty(IaasConst.P_VM_HOST.toString(), sigar.getNetInfo().getHostName());
                    vmps.add(proc);
                }
            } catch (SigarException e) {
                // Ignore it. Probably the process in the list of pids has already finished.
            }
        }
        return vmps;
    }

    public static Map<String, Object> getVMPsAsMap(Object connector) throws IaasMonitoringException {
        Map<String, Object> output = new HashMap<String, Object>();
        List<String> vmslist = new ArrayList<String>();

        List<VMProcess> vmps;
        if (connector instanceof SigarProxy) {
            vmps = getLocalVMPs((SigarProxy) connector);
        } else if (connector instanceof JMXConnector) {
            vmps = getRemoteVMPs((JMXConnector) connector);
        } else {
            throw new IaasMonitoringException("Invalid type of connector for monitoring.");
        }

        for (VMProcess vmp : vmps) {
            String vmid = vmp.getProperty(IaasConst.P_COMMON_ID.toString()).toString();
            if (vmid != null) {
                vmslist.add(vmid);
                Map<String, Object> props = vmp.getProperties();
                for (String key : props.keySet()) {
                    output.put(IaasConst.P_HOST_VM_PROP.toString(vmid, key), props.get(key));
                }
            }
        }

        logger.debug("VM processes found: " + vmslist);
        if (!vmslist.isEmpty()) {
            output.put(VMS_KEY, Utils.argsToString(vmslist, VMS_SEPARATOR));
        }

        return output;
    }

}
