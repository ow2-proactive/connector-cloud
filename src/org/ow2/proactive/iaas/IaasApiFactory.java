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

import org.ow2.proactive.iaas.cloudstack.CloudStackAPI;
import org.ow2.proactive.iaas.eucalyptus.EucalyptusConnector;
import org.ow2.proactive.iaas.nova.NovaAPI;
import org.ow2.proactive.iaas.openstack.OpenStackAPI;
import org.ow2.proactive.iaas.vcloud.VCloudAPI;

import java.util.Map;

import static org.ow2.proactive.iaas.IaasApiFactory.IaasProvider.CLOUDSTACK;
import static org.ow2.proactive.iaas.IaasApiFactory.IaasProvider.EUCALYPTUS;
import static org.ow2.proactive.iaas.IaasApiFactory.IaasProvider.NOVA;
import static org.ow2.proactive.iaas.IaasApiFactory.IaasProvider.OPENSTACK;
import static org.ow2.proactive.iaas.IaasApiFactory.IaasProvider.VCLOUD;

public class IaasApiFactory {
    public enum IaasProvider {
        CLOUDSTACK, NOVA, OPENSTACK, EUCALYPTUS, VCLOUD
    }

    public static IaasApi create(String providerName, Map<String, String> args) throws Exception {
        if (CLOUDSTACK.name().equals(providerName)) {
            return new CloudStackAPI(args);
        } else if (NOVA.name().equals(providerName)) {
            return NovaAPI.getNovaAPI(args);
        } else if (OPENSTACK.name().equals(providerName)) {
            return OpenStackAPI.getOpenStackAPI(args);
        } else if (EUCALYPTUS.name().equals(providerName)) {
            return new EucalyptusConnector(args);
        } else if (VCLOUD.name().equals(providerName)) {
            return VCloudAPI.getVCloudAPI(args);
        }
        throw new IllegalArgumentException("Unknown Iaas provider : " + providerName);
    }
}
