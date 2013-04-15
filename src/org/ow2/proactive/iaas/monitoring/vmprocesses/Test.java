package org.ow2.proactive.iaas.monitoring.vmprocesses;

import java.util.List;
import java.util.Map;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarProxy;
import org.ow2.proactive.iaas.monitoring.IaaSMonitoringException;


public class Test {
	
	public static void main(String[] args) throws IaaSMonitoringException{ 
		SigarProxy sigar = new Sigar();
		List<VMProcess> vmps = VMPLister.getLocalVMPs(sigar);
		if (vmps.size() != 0){
			for (VMProcess vmp: vmps){
				System.out.println("### ### Found VMP...");
				System.out.println("### ### Process information : " + vmp.toString());
			}
		}else{
			System.out.println("### ### No VMP found...");
		}
		
		Map<String, Object> map = VMPLister.getVMPsAsMap(sigar);
		if (map.keySet().size() != 0){
			for (String key: map.keySet()){
				System.out.println("### Key "+key+" : " + map.get(key));
			}
		}else{
			System.out.println("### ### No VMP found...");
		}
	}
	
}