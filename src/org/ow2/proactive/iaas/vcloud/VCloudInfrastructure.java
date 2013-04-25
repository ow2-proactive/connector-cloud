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
    @Configurable(description = "Virtual Organization name.")
    protected String orgName;
    @Configurable(credential = true, description = "Absolute path of the credential file")
    protected File rmCredentialsPath;
    protected String credentials;
    
    @Configurable(description = "Virtual Application template name.")
    protected String templateName;
    @Configurable(description = "Virtual Machine instance name.")
    String instanceName;
    @Configurable(description = "Virtual Data Center name.")
    protected String vdcName;
    
    @Configurable(description = "Vitural Center Metadata Service URL.")
    protected String vimServiceUrl;
    @Configurable(description = "Vitural Center Metadata Service username.")
    protected String vimServiceUsername;
    @Configurable(description = "Vitural Center Metadata Service password.")
    protected String vimServicePassword;

    @Override
    protected void configure(Object... parameters) {
        super.configure(parameters);

        int offset = IaasInfrastructure.NB_OF_BASE_PARAMETERS;
        login = (String) parameters[offset + 0];
        password = (String) parameters[offset + 1];
        orgName = (String) parameters[offset + 2];
        
        Object credentialsObj = parameters[offset + 3];
        if (credentialsObj instanceof byte[]) {
        	credentials = new String((byte[]) credentialsObj);
        } else {
        	credentials = (String) credentialsObj;
        }
        
		USE_CONFIGURED_VALUES.put(VCloudAPIConstants.InstanceParameters.RM_URL,
				rmUrl);

		USE_CONFIGURED_VALUES.put(
				VCloudAPIConstants.InstanceParameters.RM_CRED_VAL, credentials);
		
		templateName = IaaSParamUtil
				.getParameterValue(
						VCloudAPIConstants.InstanceParameters.TEMPLATE_NAME,
						parameters);
		if (templateName != null) {
			USE_CONFIGURED_VALUES.put(
					VCloudAPIConstants.InstanceParameters.TEMPLATE_NAME,
					templateName);
		}
		instanceName = IaaSParamUtil
				.getParameterValue(
						VCloudAPIConstants.InstanceParameters.INSTANCE_NAME,
						parameters);
		if (instanceName != null) {
			USE_CONFIGURED_VALUES.put(
					VCloudAPIConstants.InstanceParameters.INSTANCE_NAME,
					instanceName);
		}
		vdcName = IaaSParamUtil.getParameterValue(
				VCloudAPIConstants.InstanceParameters.VDC_NAME, parameters);
		if (orgName != null) {
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
            api = VCloudAPI.getVCloudAPI(login, password, new URI(iaasApiUrl), orgName);
        	} else {
				api = VCloudAPI.getVCloudAPI(login, password, new URI(
						iaasApiUrl), orgName, vimServiceUrl,
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
