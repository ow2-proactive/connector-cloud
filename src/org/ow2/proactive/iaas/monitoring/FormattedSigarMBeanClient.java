package org.ow2.proactive.iaas.monitoring;
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.*;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;


public class FormattedSigarMBeanClient {
    private static final String[] nic_attr = new String[] { "Name", "TxBytes", "RxBytes" };

    private JMXConnector connector;
    private MBeanServerConnection connection;
    private Map<String, Object> environment;

    // Testing.
    public static void main(String[] args) {
        FormattedSigarMBeanClient a = new FormattedSigarMBeanClient();
        Map<String, Object> env = new HashMap<String, Object>();
        a.initialize("service:jmx:rmi:///jndi/rmi://127.0.1.1:55555/vmware", env);

        Map<String, Object> res = a.getAllProperties();
        a.close();
        System.out.println(res);
    }

    public void initialize(String jmxUrl, Map<String, Object> environment) {
        setEnvironment(environment);
        initialize(jmxUrl);
    }

    public void initialize(String jmxUrl) {
        try {
            if (connector != null) {
                close();
            }
            connector = JMXConnectorFactory.connect(new JMXServiceURL(jmxUrl), environment);
            connection = connector.getMBeanServerConnection();

        } catch (Exception e) {
            close();
            throw new RuntimeException(e);
        }

    }

    public void setEnvironment(Map<String, Object> environment) {
        this.environment = environment;
    }

    public void close() {
        try {
            if (connector != null) {
                connector.close();
            }
        } catch (IOException e) {
        }
    }

    public Map<String, Object> getAllProperties() {
        Map<String, Object> properties = new HashMap<String, Object>();
        addCpuCoresProperty(properties);
        addCpuFrequencyProperty(properties);
        addCpuUsageProperty(properties);
        addMemoryProperties(properties);
        addNetworkProperties(properties);
        return properties;
    }

    private void addCpuCoresProperty(Map<String, Object> properties) {
        properties.put("cpu.cores", getNodeAttribute("sigar:Type=Cpu", "TotalCores"));
    }

    private void addCpuFrequencyProperty(Map<String, Object> properties) {
        int fmhz = (Integer) getNodeAttribute("sigar:Type=Cpu", "Mhz");
        float fghz = (float) fmhz / 1000;
        properties.put("cpu.frequency", fghz);
    }

    private void addCpuUsageProperty(Map<String, Object> properties) {
        double idle = (Double) getNodeAttribute("sigar:Type=CpuUsage", "Idle");
        float usage = (float) (1.0 - idle);
        properties.put("cpu.usage", usage);
    }

    private void addMemoryProperties(Map<String, Object> properties) {
        properties.put("memory.total", getNodeAttribute("sigar:Type=Mem", "Total"));
        properties.put("memory.free", getNodeAttribute("sigar:Type=Mem", "Free"));
        properties.put("memory.actualfree", getNodeAttribute("sigar:Type=Mem", "ActualFree"));
    }

    private void addNetworkProperties(Map<String, Object> properties) {
        Set<ObjectName> nics = getNodeMbeans("sigar:Type=NetInterface,*", null);
        int index = 0;
        for (ObjectName name : nics) {
            Object[] attributeList = getNodeAttributes(name, nic_attr);
            properties.put("network." + index + ".name", getAttributeValue(attributeList[0]));
            properties.put("network." + index + ".tx", getAttributeValue(attributeList[1]));
            properties.put("network." + index + ".rx", getAttributeValue(attributeList[2]));
            index++;
        }
    }

    private Set<ObjectName> getNodeMbeans(String name, QueryExp query) {
        try {
            return connection.queryNames(new ObjectName(name), query);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object getNodeAttribute(String name, String attribute) {
        try {
            return connection.getAttribute(new ObjectName(name), attribute);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object[] getNodeAttributes(ObjectName name, String[] attributeNameList) {
        try {
            AttributeList attributes = connection.getAttributes(name, attributeNameList);
            return attributes.toArray(new Object[attributes.size()]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object getAttributeValue(Object attribute) {
        return ((Attribute) attribute).getValue();
    }
}
