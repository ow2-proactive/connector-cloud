package org.ow2.proactive.iaas.monitoring.vmprocesses;

import java.io.IOException;
import java.util.ArrayList;
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
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;
import org.ow2.proactive.iaas.monitoring.OSMonitoringException;
import org.ow2.proactive.iaas.utils.Utils;

public class VMPLister {

    
	public static List<VMProcess> getRemoteVMPs(
			JMXConnector connector) throws OSMonitoringException{ 
		
		List<VMProcess> vmps = new ArrayList<VMProcess>();
		
		
		try {
	    	ObjectName oname = new ObjectName("sigar:Type=Processes");
	    	Object a = connector.getMBeanServerConnection().getAttribute(oname, "Processes");
	    	CompositeData[] bb = (CompositeData[]) a;
	    	for (CompositeData c: bb){
	    		Integer pid = (Integer)c.get("pid");
	    		String desc = (String)c.get("description");
	    		String mem = (String)c.get("memSize");
	    		String cpu = (String)c.get("cpuPerc");
	    		String[] vargs = (String[])c.get("commandline");
	    		
				String args = Utils.argsToString(vargs);
				VMPPattern pat = VMPPattern.whatVMPPatternMatches(args);
				if (pat != null){
					VMProcess proc = pat.getVMP(pid, args);
					
					proc.setProperty("cpu.usage.perc", cpu);
					proc.setProperty("memory.virtualmemorybytes", mem);
					
					vmps.add(proc);
				}
	    	}
		} catch (AttributeNotFoundException e) {
			throw new OSMonitoringException(e);
		} catch (InstanceNotFoundException e) {
			throw new OSMonitoringException(e);
		} catch (MBeanException e) {
			throw new OSMonitoringException(e);
		} catch (ReflectionException e) {
			throw new OSMonitoringException(e);
		} catch (IOException e) {
			throw new OSMonitoringException(e);
		} catch (MalformedObjectNameException e) {
			throw new OSMonitoringException(e);
		} 
    
    
		return vmps;
	}
	
	public static List<VMProcess> getLocalVMPs(
			SigarProxy sigar) throws OSMonitoringException{ 
		
		List<VMProcess> vmps = new ArrayList<VMProcess>();
		long[] listpids;
		
		try{
			listpids = sigar.getProcList();
		}catch(SigarException e){
			throw new OSMonitoringException(e);
		}
		
		for (long pid: listpids){
			
			try{
				String args = Utils.argsToString(sigar.getProcArgs(pid));
				VMPPattern pat = VMPPattern.whatVMPPatternMatches(args);
				if (pat != null){
					VMProcess proc = pat.getVMP(pid, args);
					
					// http://www.hyperic.com/support/docs/sigar/
					proc.setProperty("cpu.usage.perc", new Double(sigar.getProcCpu(pid).getPercent()));
					proc.setProperty("memory.virtualmemorybytes", new Double(sigar.getProcMem(pid).getSize()));
					proc.setProperty("host", sigar.getNetInfo().getHostName());
					
					vmps.add(proc);
				}
			}catch(SigarException e){
				throw new OSMonitoringException(e);
			}
		}
		return vmps;
	}

	public static Map<String, Object> getVMPsAsMap(
			Object connector) throws OSMonitoringException{ 
		Map<String, Object> output = new HashMap<String, Object>();
		List<String> vmslist = new ArrayList<String>();
		
		List<VMProcess> vmps;
		if (connector instanceof SigarProxy){
			vmps = getLocalVMPs((SigarProxy)connector);
		} else if (connector instanceof JMXConnector){
			vmps = getRemoteVMPs((JMXConnector)connector);
		} else {
			throw new OSMonitoringException();
		}
		
		for (VMProcess vmp: vmps){
			String vmid = vmp.getProperty("id").toString();
			if (vmid != null){
				vmslist.add(vmid);
				Map<String, Object> props = vmp.getProperties();
				for(String key: props.keySet()){
					output.put("vm." + vmid + "." + key, props.get(key));
				}
			}
		}
		output.put("vms", Utils.argsToString(vmslist, ";"));
		return output;
	}
	

}
