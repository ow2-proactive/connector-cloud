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

    // Masks to get specific properties.
    public static final int MASK_CPU = 0x0001;
    public static final int MASK_MEMORY = 0x0002;
    public static final int MASK_PROCESS = 0x0004;
    public static final int MASK_STORAGE = 0x0008;
    public static final int MASK_STATUS = 0x0010;
    public static final int MASK_PFLAGS = 0x0020;
    public static final int MASK_SIGAR = 0x0040;
    public static final int MASK_NETWORK = 0x0080;
    public static final int MASK_VMPROC = 0x0100;
    public static final int MASK_ALL = 0xFFFF;

    private static final Logger logger = Logger.getLogger(FormattedSigarMBeanClient.class);

    private String serviceurl;
    private JMXConnector connector;

    public FormattedSigarMBeanClient(String url, Map<String, Object> jmxenv) throws MalformedURLException,
            IOException {
        this.serviceurl = url;
        connector = JMXConnectorFactory.connect(new JMXServiceURL(serviceurl), jmxenv);
    }

    public FormattedSigarMBeanClient(JMXConnector connector) {
        this.connector = connector;
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

        if ((mask & MASK_CPU) == MASK_CPU) {
            addCpuCoresProperty(propertyMap);
            addCpuFrequencyProperty(propertyMap);
            addCpuUsageProperty(propertyMap);
        }

        if ((mask & MASK_MEMORY) == MASK_MEMORY)
            addMemoryProperties(propertyMap);

        if ((mask & MASK_NETWORK) == MASK_NETWORK)
            addNetworkProperties(propertyMap);

        if ((mask & MASK_PROCESS) == MASK_PROCESS)
            addProcessProperties(propertyMap);

        if ((mask & MASK_STORAGE) == MASK_STORAGE)
            addStorageProperties(propertyMap);

        if ((mask & MASK_STATUS) == MASK_STATUS)
            addStatusProperties(propertyMap);

        if ((mask & MASK_PFLAGS) == MASK_PFLAGS)
            addPFlagsProperties(propertyMap);

        if ((mask & MASK_SIGAR) == MASK_SIGAR)
            addSigarProperties(propertyMap);

        if ((mask & MASK_VMPROC) == MASK_VMPROC)
            addVMProcessesProperties(propertyMap);

        return propertyMap;
    }

    private void addVMProcessesProperties(Map<String, Object> properties) {
        try {
            Map<String, Object> props = VMPLister.getVMPsAsMap((Object) connector);
            properties.putAll(props);
        } catch (Exception e) {
            logger.error("Error getting some properties.", e);
        }
    }

    private void addCpuCoresProperty(Map<String, Object> properties) {
        try {
            Object a = getJMXSigarAttribute("sigar:Type=Cpu", "TotalCores");
            properties.put(IaasConst.P_COMMON_CPU_CORES.get(), (Integer) a);
        } catch (Exception e) {
            logger.error("Error getting some properties.", e);
        }
    }

    private void addCpuFrequencyProperty(Map<String, Object> properties) {
        try {
            Object a = getJMXSigarAttribute("sigar:Type=Cpu", "Mhz");
            int fmhz = (Integer) a;
            float fghz = (float) fmhz / 1000;
            properties.put(IaasConst.P_COMMON_CPU_FREQUENCY.get(), fghz);
        } catch (Exception e) {
            logger.error("Error getting some properties.", e);
        }
    }

    private void addCpuUsageProperty(Map<String, Object> properties) {
        try {
            Double a = (Double) getJMXSigarAttribute("sigar:Type=CpuUsage", "Idle");
            float usage = (float) (1.0 - a);
            properties.put(IaasConst.P_COMMON_CPU_USAGE.get(), usage);
        } catch (Exception e) {
            logger.error("Error getting some properties.", e);
        }
    }

    private void addMemoryProperties(Map<String, Object> properties) {
        try {
            Long total = (Long) getJMXSigarAttribute("sigar:Type=Mem", "Total");
            Long free = (Long) getJMXSigarAttribute("sigar:Type=Mem", "Free");
            Long actualFree = (Long) getJMXSigarAttribute("sigar:Type=Mem", "ActualFree");

            properties.put(IaasConst.P_COMMON_MEM_TOTAL.get(), total);
            properties.put(IaasConst.P_COMMON_MEM_FREE.get(), free);
            properties.put(IaasConst.P_COMMON_MEM_ACTUAL_FREE.get(), actualFree);
        } catch (Exception e) {
            logger.error("Error getting some properties.", e);
        }
    }

    private void addNetworkProperties(Map<String, Object> properties) {
        Set<ObjectName> mbeans;

        try {
            mbeans = connector.getMBeanServerConnection().queryNames(null, null);
        } catch (IOException e) {
            logger.error("Error getting some properties.", e);
            return;
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

                    properties.put(IaasConst.P_COMMON_NET_NAME.get(counter), name);
                    properties.put(IaasConst.P_COMMON_NET_TX.get(counter), tx);
                    properties.put(IaasConst.P_COMMON_NET_RX.get(counter), rx);
                    properties.put(IaasConst.P_COMMON_NET_MAC.get(counter), mac);
                    properties.put(IaasConst.P_COMMON_NET_SPEED.get(counter), (speed < 0 ? 0 : speed));
                    properties.put(IaasConst.P_COMMON_NET_IP.get(counter), ip);

                    ttx += tx;
                    trx += rx;
                    counter++;
                } catch (Exception e) {
                    logger.error("Error getting some properties.", e);
                }
            }
        }
        properties.put(IaasConst.P_COMMON_NET_COUNT_TOTAL.get(), counter);
        properties.put(IaasConst.P_COMMON_NET_TX_TOTAL.get(), ttx);
        properties.put(IaasConst.P_COMMON_NET_RX_TOTAL.get(), trx);
    }

    private void addStorageProperties(Map<String, Object> properties) {
        Set<ObjectName> mbeans;

        try {
            mbeans = connector.getMBeanServerConnection().queryNames(null, null);
        } catch (IOException e) {
            logger.error("Error getting some properties.", e);
            return;
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

                    properties.put(IaasConst.P_COMMON_STORAGE_NAME.get(counter), name);
                    properties.put(IaasConst.P_COMMON_STORAGE_TOTAL.get(counter), total);
                    properties.put(IaasConst.P_COMMON_STORAGE_USED.get(counter), used);
                    ttotal += total;
                    tused += used;
                    counter++;
                } catch (Exception e) {
                    logger.error("Error getting some properties.", e);
                }
            }
        }
        properties.put(IaasConst.P_COMMON_STORAGE_COUNT_TOTAL.get(), counter);
        properties.put(IaasConst.P_COMMON_STORAGE_TOTAL_TOTAL.get(), ttotal);
        properties.put(IaasConst.P_COMMON_STORAGE_USED_TOTAL.get(), tused);
    }

    private void addProcessProperties(Map<String, Object> properties) {
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
            properties.put(IaasConst.P_COMMON_SYSTEM_PROCESS.get(), ps.substring(0, ps.length() - 1));
            String ps3 = process3.toString();
            properties.put(IaasConst.P_COMMON_SYSTEM_PROCESS3.get(), ps3.substring(0, ps3.length() - 1));
        } catch (Exception e) {
            logger.error("Error getting some properties.", e);
        }
    }

    private void addStatusProperties(Map<String, Object> properties) {
        try {
            if (!properties.isEmpty()) {
                properties.put(IaasConst.P_COMMON_STATUS.get(), "up");
            }
        } catch (Exception e) {
            logger.error("Error getting some properties.", e);
        }
    }

    private void addSigarProperties(Map<String, Object> properties) {
        try {
            if (!properties.isEmpty()) {
                properties.put(IaasConst.P_SIGAR_JMX_URL.get(), this.serviceurl);
            }
        } catch (Exception e) {
            logger.error("Error getting some properties.", e);
        }
    }

    private void addPFlagsProperties(Map<String, Object> properties) {
        try {
            Map<String, String> pflags = (Map<String, String>) getJMXSigarAttribute("sigar:Type=PFlags",
                    "PFlags");
            for (String key : pflags.keySet()) {
                properties.put(IaasConst.P_COMMON_PFLAGS.get(key), pflags.get(key));
            }
        } catch (Exception e) {
            logger.error("Error getting some properties.", e);
        }
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
