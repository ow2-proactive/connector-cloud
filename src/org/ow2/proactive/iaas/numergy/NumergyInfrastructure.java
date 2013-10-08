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
package org.ow2.proactive.iaas.numergy;

import net.minidev.json.JSONObject;
import org.apache.log4j.Logger;
import org.ow2.proactive.iaas.IaasApi;
import org.ow2.proactive.iaas.IaasInfrastructure;
import org.ow2.proactive.iaas.IaasPolicy;
import org.ow2.proactive.resourcemanager.nodesource.common.Configurable;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.ow2.proactive.iaas.metadata.MetadataHttpClient.*;

/**
 * An infrastructure that creates nodes in Numergy.
 */
public class NumergyInfrastructure extends IaasInfrastructure {

    private static final Logger logger = Logger.getLogger(NumergyInfrastructure.class);

    @Configurable(description = "Access key for Numergy API.")
    protected String accessKey;
    @Configurable(description = "Secret key for Numergy API.")
    protected String secretKey;
    @Configurable(description = "Tenant (project) ID for Numergy API.")
    protected String tenantId;
    @Configurable(description = "An ID of an existing VM instance to duplicate.")
    protected String baseInstanceId;
    @Configurable(description = "URL of the metadata server.")
    protected String metadataServerUrl;
    @Configurable(credential = true, description = "Absolute path of the credential file (node to RM)")
    protected String credentials = "";
    @Configurable(description = "IP address of the ProActive router (PAMR for instance) if any.")
    protected String routerAddress;
    @Configurable(description = "Port number of the ProActive router (PAMR for instance) if any.")
    protected String routerPort;
    @Configurable(description = "Protocol of the ProActive router (PAMR for instance) if any.")
    protected String protocol;


    @Override
    protected void configure(Object... parameters) {

        super.configure(parameters);
        int offset = IaasInfrastructure.NB_OF_BASE_PARAMETERS;

        accessKey = (String) parameters[offset + 0];
        secretKey = (String) parameters[offset + 1];
        tenantId = (String) parameters[offset + 2];
        baseInstanceId = (String) parameters[offset + 3];
        metadataServerUrl = (String) parameters[offset + 4];
        credentials = new String((byte[]) parameters[offset + 5]);
        routerAddress = (String) parameters[offset + 6];
        routerPort = (String) parameters[offset + 7];
        protocol = (String) parameters[offset + 8];

        DEPLOYING_NODES_TIMEOUT_MS = 1000 * 60 * 50;

    }

    @Override
    protected IaasApi getAPI() {
        NumergyAPI api;

        try {
            api = NumergyAPI.getNumergyAPI(
                    accessKey, secretKey, tenantId,
                    new URI(iaasApiUrl),
                    new URI(metadataServerUrl));
        } catch (Exception e) {
            throw new RuntimeException("Cannot create API client", e);
        }

        return api;
    }

    @Override
    protected Map<String, String> getInstanceParams(String nodeName, String nodeSourceName,
                                                    Map<String, ?> nodeConfiguration) {

        Map<String, String> args = new HashMap<String, String>();

        args.put(NumergyAPI.NumergyAPIConstants.InstanceParameters.NAME, nodeName);

        args.put(NumergyAPI.NumergyAPIConstants.InstanceParameters.INSTANCE_REF,
                 getFromNodeConfigurationOrDefault(nodeConfiguration, IaasPolicy.GenericInformation.IMAGE_ID, baseInstanceId));

        JSONObject metadata = generateMetadataJson(nodeName, nodeSourceName, nodeConfiguration);

        args.put(NumergyAPI.NumergyAPIConstants.InstanceParameters.META_DATA, metadata.toJSONString());

        logger.debug("VM parameters: " + args);

        return args;
    }

    private JSONObject generateMetadataJson(String nodeName, String nodeSourceName, Map<String, ?> nodeConfiguration) {
        String token = getFromNodeConfigurationOrDefault(nodeConfiguration, IaasPolicy.GenericInformation.TOKEN, "");
        JSONObject json = new JSONObject();
        json.put(RM_URL, rmUrl);
        json.put(CREDENTIALS, credentials);
        json.put(PROTOCOL, protocol);
        json.put(ROUTER_ADDRESS, routerAddress);
        json.put(ROUTER_PORT, routerPort);
        json.put(NODE_SOURCE_NAME, nodeSourceName);
        json.put(NODE_NAME, nodeName);
        json.put(TOKEN, token);
        return json;
    }

}
