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
package org.ow2.proactive.iaas.vcloud;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.ow2.proactive.iaas.IaasApi;
import org.ow2.proactive.iaas.IaasInfrastructure;
import org.ow2.proactive.resourcemanager.nodesource.common.Configurable;


public class VCloudInfrastructure extends IaasInfrastructure {
    private static final Logger logger = Logger.getLogger(VCloudInfrastructure.class);

    @Configurable(description = "Login (format: user@organization)")
    protected String login;
    @Configurable(description = "Password")
    protected String password;
    @Configurable(description = "Virtual DataCenter (VDC) name.")
    protected String vdcName;
    @Configurable(credential = true, description = "Absolute path of the credential file")
    protected File rmCredentialsPath;
    protected String credentials = "";

    @Override
    protected void configure(Object... parameters) {
        super.configure(parameters);

        login = (String) parameters[2];
        password = (String) parameters[3];
        vdcName = (String) parameters[4];
        credentials = new String((byte[]) parameters[6]);
    }

    @Override
    protected IaasApi getAPI() {
        IaasApi api;
        try {
            api = VCloudAPI.getVCloudAPI(login, password, new URI(iaasApiUrl), vdcName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return api;
    }

    @Override
    protected Map<String, String> getInstanceParams(String nodeName, String nodeSourceName,
            Map<String, ?> nodeConfiguration) {
        Map<String, String> args = new HashMap<String, String>();
        args.put(VCloudAPI.VCloudAPIConstants.InstanceParameters.TEMPLATE_NAME,
                (String) nodeConfiguration.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.TEMPLATE_NAME));
        args.put(VCloudAPI.VCloudAPIConstants.InstanceParameters.INSTANCE_NAME, nodeName);
        args.put(VCloudAPI.VCloudAPIConstants.InstanceParameters.VDC_NAME,
                (String) nodeConfiguration.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.VDC_NAME));
        return args;
    }
}
