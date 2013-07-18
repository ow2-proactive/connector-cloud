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

package org.ow2.proactive.iaas.utils;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import org.apache.log4j.Logger;
import java.net.MalformedURLException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.JMXConnectorFactory;
import org.ow2.proactive.authentication.crypto.Credentials;
import org.ow2.proactive.iaas.monitoring.IaasMonitoringException;
import org.ow2.proactive.iaas.monitoring.SigarClient;
import org.ow2.proactive.iaas.monitoring.MonitoringClient;


public class JmxUtils {

    /** 
     * Logger. 
     */
    private static final Logger logger = Logger.getLogger(JmxUtils.class);

    /**
     * Class to be used as monitoring info getter.
     */
    private static Class mbeanClient = MonitoringClient.class;

    static {
        mbeanClient = SigarClient.class;
    }

    public static final String getHostName(String jmxServiceUrl) {
        try {
            return new JMXServiceURL(jmxServiceUrl).getHost();
        } catch (MalformedURLException e) {
            // TODO: Consider throwing a checked exception.
            throw new IllegalArgumentException(e);
        }
    }

    public static void setMBeanClient(Class<MonitoringClient> clz) {
        JmxUtils.mbeanClient = clz;
    }

    public static Map<String, Object> getSigarProperties(String jmxurl, Map<String, Object> jmxenv, int mask)
            throws IaasMonitoringException {

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) Collections.EMPTY_MAP;

        MonitoringClient a = null;
        try {
            a = (MonitoringClient) Class.forName(mbeanClient.getName()).newInstance();
        } catch (InstantiationException e1) {
            logger.error(e1);
            return map;
        } catch (IllegalAccessException e1) {
            logger.error(e1);
            return map;
        } catch (ClassNotFoundException e1) {
            logger.error(e1);
            return map;
        }

        try {
            a.configure(jmxurl, jmxenv);
        } catch (MalformedURLException e) {
            throw new IaasMonitoringException(e);
        } catch (IOException e) {
            throw new IaasMonitoringException(e);
        }

        try {
            map = a.getPropertyMap(mask);
        } catch (IOException e1) {
            logger.warn(e1);
            return map;
        } finally {
            try {
                a.disconnect();
            } catch (IOException e) {
                // Ignore it.
            }
        }

        return map;
    }

    public static Map<String, Object> getROJmxEnv(Credentials credentials) {
        Map<String, Object> env = new HashMap<String, Object>();
        env.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES, "org.ow2.proactive.jmx.provider");
        Object[] obj = new Object[] { "", credentials };
        env.put(JMXConnector.CREDENTIALS, obj);
        return env;
    }

    // non-instantiable
    private JmxUtils() {
    }

}
