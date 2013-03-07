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
package org.ow2.proactive.iaas.cloudstack;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.ow2.proactive.iaas.IaasApi;
import org.ow2.proactive.iaas.IaasInfrastructure;
import org.ow2.proactive.iaas.IaasPolicy;
import org.ow2.proactive.resourcemanager.nodesource.common.Configurable;

/**
 *
 * An infrastructure spawning Cloudstack nodes.
 *
 * <p>
 *     Either use the parameters specified when created or parameters given by the policy.
 * </p>
 *
 * @see CloudStackInfrastructure
 *
 * @author The ProActive Team
 *
 */
public class CloudStackInfrastructure extends IaasInfrastructure {

    private static final Logger logger = Logger.getLogger(CloudStackInfrastructure.class);

    @Configurable(description = "The user's api key to query Cloudstack API.")
    protected String apiKey = "";

    @Configurable(description = "The user's secret key to compute the signature of Cloudstack API queries.")
    protected String secretKey = "";

    @Configurable(description = "The identifier of the default service offering used for the instances. The constant DEFAULT will refer to it.")
    protected String defaultServiceOfferingId = "";
    @Configurable(description = "The identifier of the small service offering used for the instances. The constant SMALL will refer to it.")
    protected String smallServiceOfferingId = "";
    @Configurable(description = "The identifier of the medium service offering used for the instances. The constant MEDIUM will refer to it.")
    protected String mediumServiceOfferingId = "";
    @Configurable(description = "The identifier of the large service offering used for the instances. The constant LARGE will refer to it.")
    protected String largeServiceOfferingId = "";

    @Configurable(description = "The identifier of the template used for the instances.")
    protected String templateId = "";

    @Configurable(description = "The identifier of the zone used for the instances.")
    protected String zoneId = "";

    @Configurable(description = "The location of the Resource Manager where new instances will register.")
    protected String rmAddress = "";

    
    @Override
    protected void configure(Object... parameters) {
        super.configure(parameters);
        
        apiKey = (String) parameters[2];
        secretKey = (String) parameters[3];
        defaultServiceOfferingId = (String) parameters[4];
        smallServiceOfferingId = (String) parameters[5];
        mediumServiceOfferingId = (String) parameters[6];
        largeServiceOfferingId = (String) parameters[7];
        templateId = (String) parameters[8];
        zoneId = (String) parameters[9];
        rmAddress = (String) parameters[10];
    }

    @Override
    protected Map<String, String> getInstanceParams(String nodeName, String nodeSourceName, Map<String, ?> nodeConfiguration) {
        
        Map<String, String> args = new HashMap<String, String>();
        args.put(CloudStackAPI.CloudStackAPIConstants.InstanceParameters.TEMPLATE,
                getFromNodeConfigurationOrDefault(nodeConfiguration, IaasPolicy.GenericInformation.IMAGE_ID, templateId));
        args.put(CloudStackAPI.CloudStackAPIConstants.InstanceParameters.ZONE,
                zoneId);
        args.put(CloudStackAPI.CloudStackAPIConstants.InstanceParameters.SERVICE_OFFERING,
                resolveServiceOffering(getFromNodeConfigurationOrDefault(nodeConfiguration, IaasPolicy.GenericInformation.INSTANCE_TYPE, defaultServiceOfferingId)));
        args.put(CloudStackAPI.CloudStackAPIConstants.InstanceParameters.NAME,
                nodeName);
        args.put(CloudStackAPI.CloudStackAPIConstants.InstanceParameters.USER_DATA,
                String.format("%s\n%s\n%s\n%s", rmAddress, nodeSourceName, nodeName,
                        getFromNodeConfigurationOrDefault(nodeConfiguration, IaasPolicy.GenericInformation.TOKEN, "")));
        return args;
    }

    @Override
    protected IaasApi getAPI() {
        return new CloudStackAPI(iaasApiUrl, apiKey, secretKey);
    }
    
    private String resolveServiceOffering(String serviceOfferingValue) {
        if (IaasPolicy.GenericInformation.InstanceType.SMALL.name().equals(serviceOfferingValue)) {
            return smallServiceOfferingId;
        } else if (IaasPolicy.GenericInformation.InstanceType.MEDIUM.name().equals(serviceOfferingValue)) {
            return mediumServiceOfferingId;
        } else if (IaasPolicy.GenericInformation.InstanceType.LARGE.name().equals(serviceOfferingValue)) {
            return largeServiceOfferingId;
        } else if (IaasPolicy.GenericInformation.InstanceType.DEFAULT.name().equals(serviceOfferingValue)) {
            return defaultServiceOfferingId;
        }
        logger.debug("Instance type is not a constant, using the parameter value as serviceofferingid");
        return serviceOfferingValue;
    }

}
