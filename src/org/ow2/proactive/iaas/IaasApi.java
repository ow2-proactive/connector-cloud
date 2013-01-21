/*
 *  *
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
package org.ow2.proactive.iaas;

import java.util.Map;

/**
 * Implementations are targeted at specific Iaas implementation, for instance Cloudstack, Openstack, Eucalyptus, ...
 */
public interface IaasApi {

    /**
     * Starts a new instance on a Iaas.
     *
     * Should be synchronous, i.e wait until instance is started and ready.
     *
     * @param arguments generic arguments that will be interpreted by the called API
     * @return an identifier of the new instance
     * @throws Exception anything can happen from the misconfiguration to the API not reachable
     */
    IaasInstance startInstance(Map<String, String> arguments) throws Exception;

    /**
     * Stop a running instance on a Iaas.
     *
     * Should be synchronous, i.e wait until instance is started and ready.
     *
     * @param instance an identifier of the instance to stop
     * @throws Exception anything can happen from the misconfiguration to the API not reachable
     */
    void stopInstance(IaasInstance instance) throws Exception;

    /**
     * A utility method to check if an instance is running.
     *
     * @param instance an identifier of the target instance
     * @return true if instance is running, false otherwise
     * @throws Exception anything can happen from the misconfiguration to the API not reachable
     */
    boolean isInstanceStarted(IaasInstance instance) throws Exception;
}
