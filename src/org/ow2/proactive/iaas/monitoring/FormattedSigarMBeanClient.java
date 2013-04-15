package org.ow2.proactive.iaas.monitoring;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import org.apache.log4j.Logger;
import org.ow2.proactive.iaas.monitoring.vmprocesses.VMPLister;

public class FormattedSigarMBeanClient {

    private static final Logger logger = Logger.getLogger(FormattedSigarMBeanClient.class);
    
	private String serviceurl;
	private JMXConnector connector;
	
	public FormattedSigarMBeanClient(String url, Map<String, Object> jmxenv) 
	        throws MalformedURLException, IOException {
		this.serviceurl = url;
        connector = JMXConnectorFactory.connect(new JMXServiceURL(serviceurl), jmxenv);
	}
	
	public void disconnect() throws IOException{
		if (connector != null){
	        connector.close();
		}
	}
	
	
    public Map<String, Object> getPropertyMap() {
        Map<String, Object> propertyMap = new HashMap<String, Object>();
        try {
            addCpuCoresProperty(propertyMap);
            addCpuFrequencyProperty(propertyMap);
            addCpuUsageProperty(propertyMap);
            addMemoryProperties(propertyMap);
            addNetworkProperties(propertyMap);
            addProcessProperties(propertyMap);
            addStorageProperties(propertyMap);
            addVMProcessesProperties(propertyMap);
        } catch (Exception se) {
            logger.error("Error getting some properties.", se);
        }
        return propertyMap;
    }
		
    private void addVMProcessesProperties(Map<String, Object> properties) 
    		throws IaaSMonitoringException {
        Map<String, Object> props = VMPLister.getVMPsAsMap((Object)connector);
        properties.putAll(props);
    }
    
    private void addCpuCoresProperty(Map<String, Object> properties) throws IaaSMonitoringException{
		Object a = getJMXSigarAttribute("sigar:Type=Cpu", "TotalCores");
        properties.put("cpu.cores", (Integer)a);
    }

    private void addCpuFrequencyProperty(Map<String, Object> properties) throws IaaSMonitoringException {
		Object a = getJMXSigarAttribute("sigar:Type=Cpu", "Mhz");
        int fmhz = (Integer) a;
        float fghz = (float) fmhz / 1000;
        properties.put("cpu.frequency", fghz);
    }

    private void addCpuUsageProperty(Map<String, Object> properties) throws IaaSMonitoringException {
		Double a = (Double)getJMXSigarAttribute("sigar:Type=CpuUsage", "Idle");
        float usage = (float) (1.0 - a);
        properties.put("cpu.usage", usage);
    }

    private void addMemoryProperties(Map<String, Object> properties) throws IaaSMonitoringException {
		Long total = (Long)getJMXSigarAttribute("sigar:Type=Mem", "Total");
		Long free = (Long)getJMXSigarAttribute("sigar:Type=Mem", "Free");
		Long actualFree = (Long)getJMXSigarAttribute("sigar:Type=Mem", "ActualFree");
		
        properties.put("memory.total", total);
        properties.put("memory.free", free);
        properties.put("memory.actualfree", actualFree);
    }

    private void addNetworkProperties(Map<String, Object> properties) throws IaaSMonitoringException {
    	Set<ObjectName> mbeans;
    	
    	try{
	        mbeans = connector.getMBeanServerConnection().queryNames(null, null); 
    	}catch(IOException e){
			throw new IaaSMonitoringException(e);
    	}
    	
        int counter = 0;
        Long ttx = 0L;
        Long trx = 0L;
        for (ObjectName on: mbeans){
	        if (on.getCanonicalName().contains("Type=NetInterface")){
				try {
			        Object name = connector.getMBeanServerConnection().getAttribute(on, "Name");
			        Long tx = (Long)connector.getMBeanServerConnection().getAttribute(on, "TxBytes");
			        Long rx = (Long)connector.getMBeanServerConnection().getAttribute(on, "RxBytes");
			        
			        properties.put("network."+counter+".name", name);
			        properties.put("network."+counter+".tx", tx);
			        properties.put("network."+counter+".rx", rx);
			        ttx += tx;
			        trx += rx;
			        counter++;
				} catch (AttributeNotFoundException e) {
					throw new IaaSMonitoringException(e);
				} catch (InstanceNotFoundException e) {
					throw new IaaSMonitoringException(e);
				} catch (MBeanException e) {
					throw new IaaSMonitoringException(e);
				} catch (ReflectionException e) {
					throw new IaaSMonitoringException(e);
				} catch (IOException e) {
					throw new IaaSMonitoringException(e);
				}
	        }
        }
        properties.put("network.count", counter);
        properties.put("network.tx", ttx);
        properties.put("network.rx", trx);
    }

    private void addStorageProperties(Map<String, Object> properties) throws IaaSMonitoringException {
    	Set<ObjectName> mbeans;
    	
    	try{
	        mbeans = connector.getMBeanServerConnection().queryNames(null, null); 
    	}catch(IOException e){
			throw new IaaSMonitoringException(e);
    	}
    	
        int counter = 0;
        Long ttotal = 0L;
        Long tused = 0L;
        for (ObjectName on: mbeans){
	        if (on.getCanonicalName().contains("Type=FileSystem")){
				try {
			        Object name = connector.getMBeanServerConnection().getAttribute(on, "DirName");
			        Long total = (Long)connector.getMBeanServerConnection().getAttribute(on, "Total");
			        Long used = (Long)connector.getMBeanServerConnection().getAttribute(on, "Used");
			        
			        properties.put("storage."+counter+".name", name);
			        properties.put("storage."+counter+".total", total);
			        properties.put("storage."+counter+".used", used);
			        ttotal += total;
			        tused += used;
			        counter++;
				} catch (AttributeNotFoundException e) {
					throw new IaaSMonitoringException(e);
				} catch (InstanceNotFoundException e) {
					throw new IaaSMonitoringException(e);
				} catch (MBeanException e) {
					throw new IaaSMonitoringException(e);
				} catch (ReflectionException e) {
					throw new IaaSMonitoringException(e);
				} catch (IOException e) {
					throw new IaaSMonitoringException(e);
				}
	        }
        }
        properties.put("storage.count", counter);
        properties.put("storage.total", ttotal);
        properties.put("storage.used", tused);
    }

    private void addProcessProperties(Map<String, Object> properties) throws IaaSMonitoringException {
        StringBuilder process = new StringBuilder();
        StringBuilder process3 = new StringBuilder();
        
		try {
	    	ObjectName oname = new ObjectName("sigar:Type=Processes");
	    	Object a = connector.getMBeanServerConnection().getAttribute(oname, "Processes");
	    	CompositeData[] bb = (CompositeData[]) a;
	    	for (CompositeData c: bb){
	    		Integer pid = (Integer)c.get("pid");
	    		String desc = (String)c.get("description");
	    		String mem = (String)c.get("memSize");
	    		String cpu = (String)c.get("cpuPerc");
	    		//String[] args = (String[])c.get("commandline");
	    		// TODO serialize correctly, name and desc. may include ',' and ';' characters.
	            process.append(pid).append(';').append(desc).append(',');
	            process3.append(pid).append(';').append(desc).append(',').append(cpu).append(';').append(mem).append(',');
	    	}
	        String ps = process.toString();
	        properties.put("system.process", ps.substring(0, ps.length() - 1));
	        String ps3 = process3.toString();
	        properties.put("system.process.3", ps3.substring(0, ps3.length() - 1));
		} catch (AttributeNotFoundException e) {
			throw new IaaSMonitoringException(e);
		} catch (InstanceNotFoundException e) {
			throw new IaaSMonitoringException(e);
		} catch (MBeanException e) {
			throw new IaaSMonitoringException(e);
		} catch (ReflectionException e) {
			throw new IaaSMonitoringException(e);
		} catch (IOException e) {
			throw new IaaSMonitoringException(e);
		} catch (MalformedObjectNameException e) {
			throw new IaaSMonitoringException(e);
		} 
    }
    
	public Object getJMXSigarAttribute(String objname, String attribute) throws IaaSMonitoringException{
        Object a = null;
		try {
	        ObjectName name = new ObjectName(objname);
			a = connector.getMBeanServerConnection().getAttribute(name, attribute);
		} catch (MalformedObjectNameException e) {
		    logger.error(e);
		} catch (AttributeNotFoundException e) {
		    logger.error(e);
		} catch (InstanceNotFoundException e) {
		    logger.error(e);
		} catch (MBeanException e) {
		    logger.error(e);
		} catch (ReflectionException e) {
		    logger.error(e);
		} catch (IOException e) {
		    logger.error(e);
		}
	
		if (a == null){
			throw new IaaSMonitoringException();
		}
		return a;
	}
	
}
