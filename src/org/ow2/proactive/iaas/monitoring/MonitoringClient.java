/*
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

package org.ow2.proactive.iaas.monitoring;

import java.io.IOException;
import java.util.Map;


public interface MonitoringClient {

    /**
     * Return the properties as key:value for a given entity (host X, or VM Y).
     * @param mask to be used to choose among all possible properties to be obtained.
     * @return map with all required properties.
     */
    public Map<String, Object> getPropertyMap(int mask) throws IOException;

    /**
     * Configure the client, connect it to target, etc.
     * @param target normally an url.
     * @param env map with all extra properties needed, like authentication.
     * @throws IOException
     */
    public void configure(String target, Map<String, Object> env) throws IOException;

    /**
     * Disconnect from the monitoring target.
     */
    public void disconnect() throws IOException;
}
