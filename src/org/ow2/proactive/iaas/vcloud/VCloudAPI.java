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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

import javax.security.sasl.AuthenticationException;

import org.apache.log4j.Logger;
import org.ow2.proactive.iaas.IaasApi;
import org.ow2.proactive.iaas.IaasInstance;

import com.vmware.vcloud.api.rest.schema.InstantiateVAppTemplateParamsType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.sdk.Organization;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.Vapp;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.Vdc;
import com.vmware.vcloud.sdk.constants.Version;


public class VCloudAPI implements IaasApi {

    private static final Logger logger = Logger.getLogger(VCloudAPI.class);

    private static Map<Integer, VCloudAPI> instances;

    private Map<IaasInstance, Vapp> vapps;
    private long created;
    private VcloudClient vcd;
    private Organization org;
    private Vdc vdc;
    private URI endpoint;

    /////
    // VCLOUD FACTORY
    /////
    public static IaasApi getVCloudAPI(Map<String, String> args) throws URISyntaxException,
            AuthenticationException {
        return getVCloudAPI(args.get(VCloudAPIConstants.ApiParameters.USER_NAME),
                args.get(VCloudAPIConstants.ApiParameters.PASSWORD),
                args.get(VCloudAPIConstants.ApiParameters.ORGANIZATION_NAME),
                new URI(args.get(VCloudAPIConstants.ApiParameters.API_URL)),
                args.get(VCloudAPIConstants.ApiParameters.DEFAULT_VDC_NAME));
    }

    public static synchronized VCloudAPI getVCloudAPI(String username, String password,
            String organizationName, URI endpoint, String vdcName) throws AuthenticationException {
        if (instances == null) {
            instances = new HashMap<Integer, VCloudAPI>();
        }
        int hash = (username + password + organizationName).hashCode();
        VCloudAPI instance = instances.get(hash);
        if (instance == null) {
            try {
                instances.remove(hash);
                instance = new VCloudAPI(username, password, organizationName, endpoint, vdcName);
            } catch (Throwable t) {
                throw new AuthenticationException("Authentication failed to " + endpoint, t);
            }
            instances.put(hash, instance);
        }
        return instance;
    }

    public VCloudAPI(String username, String password, String organizationName, URI endpoint, String vdcName)
            throws IOException, KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException,
            KeyStoreException, VCloudException {
        this.vapps = new HashMap<IaasInstance, Vapp>();
        this.created = System.currentTimeMillis();
        this.endpoint = endpoint;
        authenticate(username, password, organizationName, vdcName);
    }

    public void authenticate(String username, String password, String organizationName, String vdcName)
            throws VCloudException, KeyManagementException, UnrecoverableKeyException,
            NoSuchAlgorithmException, KeyStoreException {
        VcloudClient.setLogLevel(Level.OFF);
        vcd = new VcloudClient(this.endpoint.toString(), Version.V5_1);
        vcd.registerScheme("https", 443, FakeSSLSocketFactory.getInstance());
        vcd.login(username + "@" + organizationName, password);

        org = Organization.getOrganizationByReference(vcd, vcd.getOrgRefsByName().get(organizationName));
        vdc = Vdc.getVdcByReference(vcd, org.getVdcRefsByName().get(vdcName));

    }

    @Override
    public IaasInstance startInstance(Map<String, String> arguments) throws Exception {
        String instanceName = arguments.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.INSTANCE_NAME);
//        String orgName = arguments.get(VCloudAPI.VCloudAPIConstants.ApiParameters.ORGANIZATION_NAME);
        String templateName = arguments.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.TEMPLATE_NAME);

        InstantiateVAppTemplateParamsType instVappTemplParamsType = new InstantiateVAppTemplateParamsType();
        instVappTemplParamsType.setName(instanceName);
        instVappTemplParamsType.setSource(getVappTemplateByName(templateName));
        //        instVappTemplParamsType.setInstantiationParams(instantiationParamsType);

        Vapp vapp = vdc.instantiateVappTemplate(instVappTemplParamsType);
//        int retries = 100;
//        while (!vapp.isDeployed() && retries >= 0) {
//            Thread.sleep(10000);
//            retries--;
//        }
//        if(!vapp.isDeployed()) {
//            return null;
//        }
//        vapp.powerOn();
        IaasInstance iaasInstance = new IaasInstance(vapp.getReference().getId());
        vapps.put(iaasInstance, vapp);

        return iaasInstance;
    }

    @Override
    public void stopInstance(IaasInstance instance) throws Exception {
        vapps.get(instance).delete();
    }

    @Override
    public boolean isInstanceStarted(IaasInstance instance) throws Exception {
        return vapps.get(instance).isDeployed();
    }

    @Override
    public String getName() {
        return "VMWare vCloud";
    }

    public class VCloudAPIConstants {
        public class ApiParameters {
            static final String API_URL = "apiurl";
            static final String USER_NAME = "username";
            static final String PASSWORD = "password";
            static final String ORGANIZATION_NAME = "organizationName";
            static final String DEFAULT_VDC_NAME = "vdcName";
        }

        public class InstanceParameters {
            public static final String INSTANCE_NAME = "instanceName";
            public static final String TEMPLATE_NAME = "templateName";
            public static final String VDC_NAME = "vdcName";
        }
    }

    private ReferenceType getVappTemplateByName(String templateName) {
        Iterator<ReferenceType> it = org.getVdcRefs().iterator();
        while (it.hasNext()) {
            try {
                ReferenceType vdcRef = it.next();
                Vdc vdc = Vdc.getVdcByReference(vcd, vdcRef);
                Collection<ReferenceType> set = vdc.getVappTemplateRefsByName(templateName);
                if (set.size() > 0) {
                    return set.iterator().next();
                }

            } catch (Throwable e) {
                System.err.println("Failed...");
            }
        }
        return null;
    }
}
