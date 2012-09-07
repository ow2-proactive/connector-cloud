package org.ow2.proactive.testclient;

import org.ow2.proactive.occi.scheduler.actions.vm.VMStartAction;

public class ConnectorTester {
	
	
	
	public static void main(String[] args){
		
		
		VMStartAction a = new VMStartAction("demo", "demo", "whatever");
		
		a.execute();
		
	}
	
	
	

}
