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

    @Configurable(description = "User name for vCloud.")
    protected String userName;
    @Configurable(description = "User password for vCloud.")
    protected String password;
    @Configurable(description = "Organization name for vCloud.")
    protected String orgName;
    @Configurable(description = "Virtual DataCenter (VDC) name.")
    protected String vdcName;
    @Configurable(credential = true, description = "Absolute path of the credential file")
    protected File rmCredentialsPath;
    protected String credentials = "";

    @Override
    protected void configure(Object... parameters) {
        super.configure(parameters);

        userName = (String) parameters[2];
        password = (String) parameters[3];
        orgName = (String) parameters[4];
        vdcName = (String) parameters[5];
        credentials = new String((byte[]) parameters[6]);
    }

    @Override
    protected IaasApi getAPI() {
        IaasApi api;
        try {
            api = VCloudAPI.getVCloudAPI(userName, password, orgName, new URI(iaasApiUrl), vdcName);
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
