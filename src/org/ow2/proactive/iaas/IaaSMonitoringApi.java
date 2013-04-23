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

package org.ow2.proactive.iaas;

import java.util.Map;

public interface IaaSMonitoringApi {
	
    /**
     * Get a list of all the hosts/hypervisors of this Infrastructure.
     * @return list of host ids.
     * @throws Exception
     */
    public String[] getHosts() throws Exception;

    /**
     * Get a list of all the VMs of this Infrastructure.
     * @return list of VM ids.
     * @throws Exception
     */
    public String[] getVMs() throws Exception;

    /**
     * Get a list of all the VMs of this host.
     * @param hostId
     * @return list of VM ids.
     * @throws Exception
     */
    public String[] getVMs(String hostId) throws Exception;

    /**
     * Get a map of all the properties of the host.
     * @param hostId
     * @return the list of properties.
     * @throws Exception
     */
    public Map<String, String> getHostProperties(String hostId) throws Exception;

    /**
     * Get a map of all the properties of the VM.
     * @param vmId
     * @return the list of properties.
     * @throws Exception
     */
    public Map<String, String> getVMProperties(String vmId) throws Exception;
    
    /**
     * Get a map with information regarding the specific cloud services provider.
     * This method returns information provider-dependent. 
     * @return the map.
     * @throws Exception
     */
    public Map<String, Object> getVendorDetails() throws Exception; 

}
