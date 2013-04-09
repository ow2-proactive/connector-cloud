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

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import javax.management.remote.JMXServiceURL;

import org.ow2.proactive.authentication.crypto.Credentials;
import org.ow2.proactive.iaas.monitoring.FormattedSigarMBeanClientHost;
import org.ow2.proactive.iaas.monitoring.OSMonitoringException;


public class JmxUtils {

    public static final String getHostName(String jmxServiceUrl) {
        try {
            return new JMXServiceURL(jmxServiceUrl).getHost();
        } catch (MalformedURLException e) {
            // TODO: Consider throwing a checked exception.
            throw new IllegalArgumentException(e);
        }
    }

    public static Map<String, Object> getSigarProperties(
            String jmxurl, Map<String, Object> jmxenv) 
                    throws OSMonitoringException {
        FormattedSigarMBeanClientHost a;
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            a = new FormattedSigarMBeanClientHost(jmxurl, jmxenv);
        } catch (MalformedURLException e) {
            throw new OSMonitoringException(e);
        } catch (IOException e) {
            throw new OSMonitoringException(e);
        }
        
        map = a.getPropertyMap();
        try {
            a.disconnect();
        } catch (IOException e) {
            // Ignore it.
        }
        return map;
    }

    // non-instantiable
    private JmxUtils() {
    }

}
