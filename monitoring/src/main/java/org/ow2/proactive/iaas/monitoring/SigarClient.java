package org.ow2.proactive.iaas.monitoring;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.io.IOException;

import org.apache.log4j.Logger;

import javax.management.ObjectName;
import javax.management.MBeanException;
import javax.management.remote.JMXConnector;
import javax.management.ReflectionException;
import javax.management.remote.JMXServiceURL;
import javax.management.openmbean.CompositeData;
import javax.management.InstanceNotFoundException;
import javax.management.AttributeNotFoundException;
import javax.management.remote.JMXConnectorFactory;
import javax.management.MalformedObjectNameException;

import org.ow2.proactive.iaas.monitoring.vmprocesses.VMPLister;

import static org.ow2.proactive.iaas.monitoring.IaasConst.*;


public class SigarClient implements MonitoringClient {

    private static final int NO_ERROR = 0;
    private static final int ERROR = 1;

    private static final Logger logger = Logger.getLogger(SigarClient.class);

    private String serviceurl;
    private JMXConnector connector;

    public SigarClient() {
    }

    public void configure(String target, Map<String, Object> env) throws IOException {
        this.serviceurl = target;
        connector = JMXConnectorFactory.connect(new JMXServiceURL(serviceurl), env);
    }

    public void disconnect() throws IOException {
        if (connector != null) {
            connector.close();
        }
    }

    public Map<String, Object> getPropertyMap(int mask) {

        // Can use: 
        //          getPropertyMap(MASK_CPU | MASK_MEMORY);
        // or also: 
        //          getPropertyMap(MASK_ALL);

        // TODO optimize addVMProcessesProperties to use processes already
        //      obtained 

        Map<String, Object> propertyMap = new HashMap<String, Object>();

        int errors = 0;

        if ((mask & MASK_CPU) == MASK_CPU) {
            errors += addCpuCoresProperty(propertyMap);
            errors += addCpuFrequencyProperty(propertyMap);
            errors += addCpuUsageProperty(propertyMap);
        }

        if ((mask & MASK_MEMORY) == MASK_MEMORY)
            errors += addMemoryProperties(propertyMap);

        if ((mask & MASK_NETWORK) == MASK_NETWORK)
            errors += addNetworkProperties(propertyMap);

        if ((mask & MASK_PROCESS) == MASK_PROCESS)
            errors += addProcessProperties(propertyMap);

        if ((mask & MASK_STORAGE) == MASK_STORAGE)
            errors += addStorageProperties(propertyMap);

        if ((mask & MASK_STATUS) == MASK_STATUS)
            errors += addStatusProperties(propertyMap);

        if ((mask & MASK_PFLAGS) == MASK_PFLAGS)
            errors += addPFlagsProperties(propertyMap);

        if ((mask & MASK_SIGAR) == MASK_SIGAR)
            errors += addSigarProperties(propertyMap);

        if ((mask & MASK_VMPROC) == MASK_VMPROC)
            errors += addVMProcessesProperties(propertyMap);

        addDebugProperties(propertyMap, errors);

        return propertyMap;
    }

    private void addDebugProperties(Map<String, Object> properties, int errors) {
        properties.put(P_DEBUG_SIGAR_USED.toString(), "yes");
        properties.put(P_DEBUG_NUMBER_OF_ERRORS.toString(), (Integer) errors);
    }

    private int addVMProcessesProperties(Map<String, Object> properties) {
        try {
            Map<String, Object> props = VMPLister.getVMPsAsMap((Object) connector);
            properties.putAll(props);
        } catch (Exception e) {
            logger.error("Error getting some properties.", e);
            return ERROR;
        }
        return NO_ERROR;
    }

    private int addCpuCoresProperty(Map<String, Object> properties) {
        try {
            Object a = getJMXSigarAttribute("sigar:Type=Cpu", "TotalCores");
            properties.put(P_COMMON_CPU_CORES.toString(), (Integer) a);
        } catch (Exception e) {
            logger.error("Error getting some properties.", e);
            return ERROR;
        }
        return NO_ERROR;
    }

    private int addCpuFrequencyProperty(Map<String, Object> properties) {
        try {
            Object a = getJMXSigarAttribute("sigar:Type=Cpu", "Mhz");
            int fmhz = (Integer) a;
            float fghz = (float) fmhz / 1000;
            properties.put(P_COMMON_CPU_FREQUENCY.toString(), fghz);
        } catch (Exception e) {
            logger.error("Error getting some properties.", e);
            return ERROR;
        }
        return NO_ERROR;
    }

    private int addCpuUsageProperty(Map<String, Object> properties) {
        try {
            Double a = (Double) getJMXSigarAttribute("sigar:Type=CpuUsage", "Idle");
            float usage = (float) (1.0 - a);
            properties.put(P_COMMON_CPU_USAGE.toString(), usage);
        } catch (Exception e) {
            logger.error("Error getting some properties.", e);
            return ERROR;
        }
        return NO_ERROR;
    }

    private int addMemoryProperties(Map<String, Object> properties) {
        try {
            Long total = (Long) getJMXSigarAttribute("sigar:Type=Mem", "Total");
            Long free = (Long) getJMXSigarAttribute("sigar:Type=Mem", "Free");
            Long actualFree = (Long) getJMXSigarAttribute("sigar:Type=Mem", "ActualFree");

            properties.put(P_COMMON_MEM_TOTAL.toString(), total);
            properties.put(P_COMMON_MEM_FREE.toString(), free);
            properties.put(P_COMMON_MEM_ACTUAL_FREE.toString(), actualFree);
        } catch (Exception e) {
            logger.error("Error getting some properties.", e);
            return ERROR;
        }
        return NO_ERROR;
    }

    private int addNetworkProperties(Map<String, Object> properties) {
        Set<ObjectName> mbeans;

        try {
            mbeans = connector.getMBeanServerConnection().queryNames(null, null);
        } catch (IOException e) {
            logger.error("Error getting some properties.", e);
            return ERROR;
        }

        int counter = 0;
        Long ttx = 0L;
        Long trx = 0L;
        for (ObjectName on : mbeans) {
            if (on.getCanonicalName().contains("Type=NetInterface")) {
                try {
                    Object name = connector.getMBeanServerConnection().getAttribute(on, "Name");
                    Long tx = (Long) connector.getMBeanServerConnection().getAttribute(on, "TxBytes");
                    Long rx = (Long) connector.getMBeanServerConnection().getAttribute(on, "RxBytes");
                    String mac = (String) connector.getMBeanServerConnection().getAttribute(on, "Hwaddr");
                    Long speed = (Long) connector.getMBeanServerConnection().getAttribute(on, "Speed");
                    String ip = (String) connector.getMBeanServerConnection().getAttribute(on, "Address");

                    properties.put(P_COMMON_NET_NAME.toString(counter), name);
                    properties.put(P_COMMON_NET_TX.toString(counter), tx);
                    properties.put(P_COMMON_NET_RX.toString(counter), rx);
                    properties.put(P_COMMON_NET_MAC.toString(counter), mac);
                    properties.put(P_COMMON_NET_SPEED.toString(counter), (speed < 0 ? 0 : speed));
                    properties.put(P_COMMON_NET_IP.toString(counter), ip);

                    ttx += tx;
                    trx += rx;
                    counter++;
                } catch (Exception e) {
                    logger.error("Error getting some properties.", e);
                    return ERROR;
                }
            }
        }
        properties.put(P_COMMON_NET_COUNT_TOTAL.toString(), counter);
        properties.put(P_COMMON_NET_TX_TOTAL.toString(), ttx);
        properties.put(P_COMMON_NET_RX_TOTAL.toString(), trx);
        return NO_ERROR;
    }

    private int addStorageProperties(Map<String, Object> properties) {
        Set<ObjectName> mbeans;

        try {
            mbeans = connector.getMBeanServerConnection().queryNames(null, null);
        } catch (IOException e) {
            logger.error("Error getting some properties.", e);
            return ERROR;
        }

        int counter = 0;
        Long ttotal = 0L;
        Long tused = 0L;
        for (ObjectName on : mbeans) {
            if (on.getCanonicalName().contains("Type=FileSystem")) {
                try {
                    Object name = connector.getMBeanServerConnection().getAttribute(on, "DirName");
                    Long total = (Long) connector.getMBeanServerConnection().getAttribute(on, "Total");
                    Long used = (Long) connector.getMBeanServerConnection().getAttribute(on, "Used");

                    properties.put(P_COMMON_STORAGE_NAME.toString(counter), name);
                    properties.put(P_COMMON_STORAGE_TOTAL.toString(counter), total);
                    properties.put(P_COMMON_STORAGE_USED.toString(counter), used);
                    ttotal += total;
                    tused += used;
                    counter++;
                } catch (Exception e) {
                    logger.error("Error getting some properties.", e);
                    return ERROR;
                }
            }
        }
        properties.put(P_COMMON_STORAGE_COUNT_TOTAL.toString(), counter);
        properties.put(P_COMMON_STORAGE_TOTAL_TOTAL.toString(), ttotal);
        properties.put(P_COMMON_STORAGE_USED_TOTAL.toString(), tused);
        return NO_ERROR;
    }

    private int addProcessProperties(Map<String, Object> properties) {
        StringBuilder process = new StringBuilder();
        StringBuilder process3 = new StringBuilder();

        try {
            ObjectName oname = new ObjectName("sigar:Type=Processes");
            Object a = connector.getMBeanServerConnection().getAttribute(oname, "Processes");
            CompositeData[] bb = (CompositeData[]) a;
            for (CompositeData c : bb) {
                Integer pid = (Integer) c.get("pid");
                String desc = (String) c.get("description");
                String mem = (String) c.get("memSize");
                String cpu = (String) c.get("cpuPerc");
                //String[] args = (String[])c.get("commandline");
                // TODO serialize correctly, name and desc. may include ',' and ';' characters.
                process.append(pid).append(';').append(desc).append(',');
                process3.append(pid).append(';').append(desc).append(',').append(cpu).append(';').append(mem)
                        .append(',');
            }
            String ps = process.toString();
            properties.put(P_COMMON_SYSTEM_PROCESS.toString(), ps.substring(0, ps.length() - 1));
            String ps3 = process3.toString();
            properties.put(P_COMMON_SYSTEM_PROCESS3.toString(), ps3.substring(0, ps3.length() - 1));
        } catch (Exception e) {
            logger.error("Error getting some properties.", e);
            return ERROR;
        }
        return NO_ERROR;
    }

    private int addStatusProperties(Map<String, Object> properties) {
        try {
            if (!properties.isEmpty()) {
                properties.put(P_COMMON_STATUS.toString(), "up");
            }
        } catch (Exception e) {
            logger.error("Error getting some properties.", e);
            return ERROR;
        }
        return NO_ERROR;
    }

    private int addSigarProperties(Map<String, Object> properties) {
        try {
            if (!properties.isEmpty()) {
                properties.put(P_SIGAR_JMX_URL.toString(), this.serviceurl);
            }
        } catch (Exception e) {
            logger.error("Error getting some properties.", e);
            return ERROR;
        }
        return NO_ERROR;
    }

    private int addPFlagsProperties(Map<String, Object> properties) {
        try {
            Map<String, String> pflags = (Map<String, String>) getJMXSigarAttribute("sigar:Type=PFlags",
                    "PFlags");
            for (String key : pflags.keySet()) {
                properties.put(P_COMMON_PFLAGS.toString(key), pflags.get(key));
            }
        } catch (Exception e) {
            logger.error("Error getting some properties.", e);
            return ERROR;
        }
        return NO_ERROR;
    }

    public Object getJMXSigarAttribute(String objname, String attribute) throws IaasMonitoringException {
        Object a = null;
        try {
            ObjectName name = new ObjectName(objname);
            a = connector.getMBeanServerConnection().getAttribute(name, attribute);
        } catch (MalformedObjectNameException e) {
            throw new IaasMonitoringException(dump(objname, attribute), e);
        } catch (AttributeNotFoundException e) {
            throw new IaasMonitoringException(dump(objname, attribute), e);
        } catch (InstanceNotFoundException e) {
            throw new IaasMonitoringException(dump(objname, attribute), e);
        } catch (MBeanException e) {
            throw new IaasMonitoringException(dump(objname, attribute), e);
        } catch (ReflectionException e) {
            throw new IaasMonitoringException(dump(objname, attribute), e);
        } catch (IOException e) {
            throw new IaasMonitoringException(dump(objname, attribute), e);
        }

        if (a == null) {
            throw new IaasMonitoringException("Failed to get attribute" + dump(objname, attribute));
        }

        return a;
    }

    private String dump(String objname, String att) {
        return " (obj '" + objname + "' att '" + att + "')";
    }
}
