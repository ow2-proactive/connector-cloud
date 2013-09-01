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

package org.ow2.proactive.iaas.monitoring;

import org.apache.log4j.Logger;
import org.ow2.proactive.resourcemanager.core.jmx.RMJMXHelper;

import javax.management.*;
import java.util.Arrays;
import java.util.List;

public class MBeanExposer {

    private static final Logger logger = Logger.getLogger(MBeanExposer.class);

    public static final String RM_MBEAN_DOMAIN_NAME = "ProActiveResourceManager";
    public static final String IAASMONITORING_MBEAN_NAME = RM_MBEAN_DOMAIN_NAME + ":name=IaasMonitoring";

    private ObjectName name;
    private MBeanServer server;


    public void registerAsMBean(String nsname, Object mbean) throws MBeanRegistrationException {

        registerAsMBean(nsname, mbean, false);

    }

    public MBeanServer getServer() {
        return server;
    }

    public void registerAsMBean(String nsname, Object mbean, boolean anyServer)
            throws MBeanRegistrationException {

        server = getRMMBeanServer(anyServer);
        String mbeanname = IAASMONITORING_MBEAN_NAME + "-" + nsname;
        try {
            name = new ObjectName(mbeanname);
            ObjectInstance instance = server.registerMBean(mbean, name);
            logger.debug("JMX MBean " + name.getCanonicalName() + " registered successfully: " + instance.toString());
        } catch (MalformedObjectNameException e) {
            throw new MBeanRegistrationException(e, "The mbean name '" + mbeanname + "' is not valid.");
        } catch (InstanceAlreadyExistsException e) {
            throw new MBeanRegistrationException(e, "The instance of the MBean '" + mbeanname + "' already exists.");
        } catch (NotCompliantMBeanException e) {
            throw new RuntimeException("Wrong MBean.", e);
        }
    }

    public ObjectName getName() {

        return name;

    }

    private MBeanServer getRMMBeanServer(boolean anyServer) throws MBeanRegistrationException {
        MBeanServer mbs = null;

        if (anyServer)
            return MBeanServerFactory.createMBeanServer();

        List<MBeanServer> mbss = RMJMXHelper.findMBeanServer(null);
        for (MBeanServer s : mbss) {
            List<String> domains = Arrays.asList(s.getDomains());
            if (domains.contains(RM_MBEAN_DOMAIN_NAME)) {
                mbs = s;
            }
        }

        if (mbs == null) {
            throw new MBeanRegistrationException(null, "Could not find the right MBeanServer.");
        }

        return mbs;

    }

    public void stop() throws MBeanRegistrationException, InstanceNotFoundException {

        if (server != null)
            server.unregisterMBean(name);

    }

}
