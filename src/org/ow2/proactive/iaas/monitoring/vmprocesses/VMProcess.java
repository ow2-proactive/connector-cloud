package org.ow2.proactive.iaas.monitoring.vmprocesses;

import java.util.HashMap;
import java.util.Map;

public class VMProcess {
    
	public static final String PROP_ID="id";

	private Map<String, Object> props;
	
	public VMProcess() {
		props = new HashMap<String, Object>();
	}
	
	public void setProperty(String key, Object value){
		props.put(key, value);
	}
	
	public Object getProperty(String key){
		return props.get(key);
	}
	
	public Map<String, Object> getProperties(){
		return props;
	}
	
	public String toString(){
		return props.toString();
	}
	
	
}
