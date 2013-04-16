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
import org.ow2.proactive.iaas.IaaSParamUtil;
import org.ow2.proactive.iaas.IaasApi;
import org.ow2.proactive.iaas.IaasInfrastructure;
import org.ow2.proactive.iaas.vcloud.VCloudAPI.VCloudAPIConstants;
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
    
    protected String vimServiceUrl;
    protected String vimServiceUsername;
    protected String vimServicePassword;
    
    protected File rmCredentialsPath;
    protected String credentials = "";

    @Override
    protected void configure(Object... parameters) {
        super.configure(parameters);

        int offset = IaasInfrastructure.NB_OF_BASE_PARAMETERS;
        login = (String) parameters[offset + 0];
        password = (String) parameters[offset + 1];
        vdcName = (String) parameters[offset + 2];
        credentials = (String) parameters[offset + 3];
        
		String templateName = IaaSParamUtil
				.getParameterValue(
						VCloudAPIConstants.InstanceParameters.TEMPLATE_NAME,
						parameters);
		if (templateName != null) {
			USE_CONFIGURED_VALUES.put(
					VCloudAPIConstants.InstanceParameters.TEMPLATE_NAME,
					templateName);
		}
		String instanceName = IaaSParamUtil
				.getParameterValue(
						VCloudAPIConstants.InstanceParameters.INSTANCE_NAME,
						parameters);
		if (instanceName != null) {
			USE_CONFIGURED_VALUES.put(
					VCloudAPIConstants.InstanceParameters.INSTANCE_NAME,
					instanceName);
		}
		String vdcName = IaaSParamUtil.getParameterValue(
				VCloudAPIConstants.InstanceParameters.VDC_NAME, parameters);
		if (vdcName != null) {
			USE_CONFIGURED_VALUES.put(
					VCloudAPIConstants.InstanceParameters.VDC_NAME, vdcName);
		}
		
		// VimService parameters
		vimServiceUrl = IaaSParamUtil.getParameterValue(
				VCloudAPIConstants.MonitoringParameters.URL, parameters);
		vimServiceUsername = IaaSParamUtil.getParameterValue(
				VCloudAPIConstants.MonitoringParameters.USERNAME, parameters);
		vimServicePassword = IaaSParamUtil.getParameterValue(
				VCloudAPIConstants.MonitoringParameters.PASSWORD, parameters);
				
    }

    @Override
    protected IaasApi getAPI() {
        IaasApi api;
        try {
        	if (vimServiceUrl == null) {
            api = VCloudAPI.getVCloudAPI(login, password, new URI(iaasApiUrl), vdcName);
        	} else {
				api = VCloudAPI.getVCloudAPI(login, password, new URI(
						iaasApiUrl), vdcName, vimServiceUrl,
						vimServiceUsername, vimServicePassword);
        	}
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
