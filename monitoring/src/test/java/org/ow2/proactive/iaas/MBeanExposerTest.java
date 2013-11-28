package org.ow2.proactive.iaas;/*
 *  
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2011 INRIA/University of
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
 *  * $$PROACTIVE_INITIAL_DEV$$
 */

import junit.framework.Assert;
import org.apache.commons.collections.CollectionUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ow2.proactive.iaas.monitoring.*;
import org.ow2.proactive.iaas.testutils.MBeanServerHelper;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MBeanExposerTest {

    private static String nsname = "NODESOURCENAME";
    private static MBeanServerHelper serverHelper;

    private static final String[] HOSTS;
    private static final String[] VMS;

    private static final Map<String, String> HOST1PROPS;
    private static final Map<String, String> VM1PROPS;
    private static final Map<String, String> VM2PROPS;

    static {
        HOSTS = new String[]{"host1"};
        VMS = new String[]{"vm1", "vm2"};
        HOST1PROPS = new HashMap<String, String>();
        HOST1PROPS.put("name", "host1");
        VM1PROPS = new HashMap<String, String>();
        VM1PROPS.put("name", "vm1");
        VM2PROPS = new HashMap<String, String>();
        VM2PROPS.put("name", "vm2");
    }


    @BeforeClass
    public static void beforeClass() throws Exception {

        IaasMonitoringService service = new IaasMonitoringService(nsname);
        IaasMonitoringChainable chain = mock(IaasMonitoringChainable.class);

        when(chain.getHosts()).thenReturn(HOSTS);
        when(chain.getHostProperties("host1")).thenReturn(HOST1PROPS);
        when(chain.getVMs()).thenReturn(VMS);
        when(chain.getVMs("host1")).thenReturn(VMS);
        when(chain.getVMProperties("vm1")).thenReturn(VM1PROPS);
        when(chain.getVMProperties("vm2")).thenReturn(VM2PROPS);

        service.configure(chain);

        MBeanExposer exposer = new MBeanExposer();
        exposer.registerAsMBean(nsname, new DynamicIaasMonitoringMBean(service), true);

        MBeanServer server = exposer.getServer();

        serverHelper = new MBeanServerHelper();
        serverHelper.createJMXServer(server, "serverName");

    }

    @Test
    public void getSummaries_Test() throws Exception {

        IaasMonitoringApiExtended proxy = getMBeanProxy();

        proxy.getSummary();
        proxy.getHostsSummary();
        proxy.getVMsSummary();

    }

    @Test
    public void getLists_Test() throws Exception {

        IaasMonitoringApiExtended proxy = getMBeanProxy();

        Assert.assertTrue(CollectionUtils.isEqualCollection(Arrays.asList(HOSTS), Arrays.asList(proxy.getHosts())));
        Assert.assertTrue(CollectionUtils.isEqualCollection(Arrays.asList(VMS), Arrays.asList(proxy.getVMs())));

    }

    @Test
    public void getProperties_Test() throws Exception {

        IaasMonitoringApiExtended proxy = getMBeanProxy();

        Assert.assertEquals(HOST1PROPS, proxy.getHostProperties("host1"));
        Assert.assertEquals(VM1PROPS, proxy.getVMProperties("vm1"));
        Assert.assertEquals(VM2PROPS, proxy.getVMProperties("vm2"));

    }

    @Test
    public void getNewAttributes_Test() throws Exception {

        MBeanServerConnection proxy = getMBeanServerConnection();

        Object host1 = proxy.getAttribute(getDefaultObjectName(), DynamicIaasMonitoringMBean.HOST_PREFIX + "host1");
        Assert.assertEquals(HOST1PROPS, host1);

        Object vm1 = proxy.getAttribute(getDefaultObjectName(), DynamicIaasMonitoringMBean.VM_PREFIX + "vm1");
        Assert.assertEquals(VM1PROPS, vm1);

        Object vm2 = proxy.getAttribute(getDefaultObjectName(), DynamicIaasMonitoringMBean.VM_PREFIX + "vm2");
        Assert.assertEquals(VM2PROPS, vm2);

    }

    private IaasMonitoringApiExtended getMBeanProxy() throws IOException, MalformedObjectNameException {

        JMXConnector jmxc = JMXConnectorFactory.connect(serverHelper.getAddress(), null);
        return JMX.newMBeanProxy(jmxc.getMBeanServerConnection(),
                getDefaultObjectName(), IaasMonitoringApiExtended.class, true);

    }

    private MBeanServerConnection getMBeanServerConnection() throws IOException, MalformedObjectNameException {

        JMXConnector jmxc = JMXConnectorFactory.connect(serverHelper.getAddress(), null);
        return jmxc.getMBeanServerConnection();

    }

    private ObjectName getDefaultObjectName() throws MalformedObjectNameException {
        return new ObjectName("ProActiveResourceManager:name=IaasMonitoring-" + nsname);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        serverHelper.stopJMXServer();
    }


}
