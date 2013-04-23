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
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.security.sasl.AuthenticationException;
import javax.xml.bind.JAXBElement;

import org.apache.log4j.Logger;
import org.ow2.proactive.iaas.IaaSMonitoringApi;
import org.ow2.proactive.iaas.IaasApi;
import org.ow2.proactive.iaas.IaasInstance;
import org.ow2.proactive.iaas.vcloud.monitoring.VimServiceClient;

import com.vmware.vcloud.api.rest.schema.FirewallRuleProtocols;
import com.vmware.vcloud.api.rest.schema.FirewallRuleType;
import com.vmware.vcloud.api.rest.schema.FirewallServiceType;
import com.vmware.vcloud.api.rest.schema.InstantiateVAppTemplateParamsType;
import com.vmware.vcloud.api.rest.schema.InstantiationParamsType;
import com.vmware.vcloud.api.rest.schema.NatRuleType;
import com.vmware.vcloud.api.rest.schema.NatServiceType;
import com.vmware.vcloud.api.rest.schema.NatVmRuleType;
import com.vmware.vcloud.api.rest.schema.NetworkConfigSectionType;
import com.vmware.vcloud.api.rest.schema.NetworkConfigurationType;
import com.vmware.vcloud.api.rest.schema.NetworkFeaturesType;
import com.vmware.vcloud.api.rest.schema.NetworkServiceType;
import com.vmware.vcloud.api.rest.schema.ObjectFactory;
import com.vmware.vcloud.api.rest.schema.RecomposeVAppParamsType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.api.rest.schema.VAppNetworkConfigurationType;
import com.vmware.vcloud.api.rest.schema.ovf.MsgType;
import com.vmware.vcloud.api.rest.schema.ovf.SectionType;
import com.vmware.vcloud.sdk.Organization;
import com.vmware.vcloud.sdk.Task;
import com.vmware.vcloud.sdk.VCloudException;
import com.vmware.vcloud.sdk.VM;
import com.vmware.vcloud.sdk.Vapp;
import com.vmware.vcloud.sdk.VappTemplate;
import com.vmware.vcloud.sdk.VcloudClient;
import com.vmware.vcloud.sdk.Vdc;
import com.vmware.vcloud.sdk.admin.AdminOrganization;
import com.vmware.vcloud.sdk.constants.FenceModeValuesType;
import com.vmware.vcloud.sdk.constants.FirewallPolicyType;
import com.vmware.vcloud.sdk.constants.NatPolicyType;
import com.vmware.vcloud.sdk.constants.NatTypeType;
import com.vmware.vcloud.sdk.constants.Version;


public class VCloudAPI implements IaasApi, IaaSMonitoringApi {

    private static final Logger logger = Logger.getLogger(VCloudAPI.class);

    private static Map<Integer, VCloudAPI> instances;

    private Map<String, IaasInstance> iaasInstances;
    private Map<IaasInstance, Vapp> vapps;
    private long created;
    private VcloudClient vCloudClient;
    private Organization org;
    private URI endpoint;

    private VimServiceClient vimServiceClient;

    // ///
    // VCLOUD FACTORY
    // ///
	public static IaasApi getVCloudAPI(Map<String, String> args)
			throws URISyntaxException, AuthenticationException {
		VCloudAPI vCloudAPI = getVCloudAPI(
				args.get(VCloudAPIConstants.ApiParameters.USER_NAME),
				args.get(VCloudAPIConstants.ApiParameters.PASSWORD), new URI(
						args.get(VCloudAPIConstants.ApiParameters.API_URL)),
				args.get(VCloudAPIConstants.ApiParameters.ORGANIZATION_NAME));
		vCloudAPI.initializeMonitoringService(
				args.get(VCloudAPIConstants.MonitoringParameters.URL),
				args.get(VCloudAPIConstants.MonitoringParameters.USERNAME),
				args.get(VCloudAPIConstants.MonitoringParameters.PASSWORD));
		return vCloudAPI;
	}

	public static synchronized VCloudAPI getVCloudAPI(String login,
			String password, URI endpoint, String orgName)
			throws AuthenticationException {
		if (instances == null) {
            instances = new HashMap<Integer, VCloudAPI>();
        }
        int hash = (login + password).hashCode();
        VCloudAPI instance = instances.get(hash);
        if (instance == null) {
            try {
				instances.remove(hash);
				instance = new VCloudAPI(login, password, endpoint, orgName);
			} catch (Throwable t) {
				throw new AuthenticationException("Authentication failed to "
						+ endpoint, t);
			}
			instances.put(hash, instance);
		}
		return instance;
	}
	
	public static synchronized VCloudAPI getVCloudAPI(String login,
			String password, URI endpoint, String orgName,
			String vimServiceUrl, String vimServiceUsername,
			String vimServicePassword) throws AuthenticationException {
		if (instances == null) {
			instances = new HashMap<Integer, VCloudAPI>();
		}
		int hash = (login + password).hashCode();
		VCloudAPI instance = instances.get(hash);
		if (instance == null) {
			try {
				instances.remove(hash);
				instance = new VCloudAPI(login, password, endpoint, orgName,
						vimServiceUrl, vimServiceUsername, vimServicePassword);
			} catch (Throwable t) {
				throw new AuthenticationException("Authentication failed to "
						+ endpoint, t);
			}
			instances.put(hash, instance);
		}
		return instance;
	}
	
	public VCloudAPI(String login, String password, URI endpoint,
			String orgName, String vimServiceUrl, String vimServiceUsername,
			String vimServicePassword) throws IOException,
			KeyManagementException, UnrecoverableKeyException,
			NoSuchAlgorithmException, KeyStoreException, VCloudException {
		this.iaasInstances = new HashMap<String, IaasInstance>();
		this.vapps = new HashMap<IaasInstance, Vapp>();
		this.created = System.currentTimeMillis();
		this.endpoint = endpoint;
		authenticate(login, password, orgName);
		initializeMonitoringService(vimServiceUrl, vimServiceUsername, vimServicePassword);
	}

	public VCloudAPI(String login, String password, URI endpoint, String orgName)
			throws IOException, KeyManagementException,
			UnrecoverableKeyException, NoSuchAlgorithmException,
			KeyStoreException, VCloudException {
		this.iaasInstances = new HashMap<String, IaasInstance>();
		this.vapps = new HashMap<IaasInstance, Vapp>();
		this.created = System.currentTimeMillis();
		this.endpoint = endpoint;
		authenticate(login, password, orgName);
	}

	public void authenticate(String login, String password, String orgName)
			throws VCloudException, KeyManagementException,
			UnrecoverableKeyException, NoSuchAlgorithmException,
			KeyStoreException {
		VcloudClient.setLogLevel(Level.OFF);
		vCloudClient = new VcloudClient(this.endpoint.toString(), Version.V5_1);
		vCloudClient.registerScheme("https", 443,
				FakeSSLSocketFactory.getInstance());
		vCloudClient.login(login, password);
		org = Organization.getOrganizationByReference(vCloudClient,
				vCloudClient.getOrgRefsByName().get(orgName));

		logger.info("Authentication success for " + login);
	}

	@Override
	public IaasInstance startInstance(Map<String, String> arguments)
			throws Exception {
		IaasInstance iaasInstance = createInstance(arguments);
		arguments.put(VCloudAPIConstants.InstanceParameters.INSTANCE_ID,
				iaasInstance.getInstanceId());
		configureNetwork(arguments);
		deployInstance(arguments);
		return iaasInstance;
	}

	public IaasInstance createInstance(Map<String, String> arguments)
			throws Exception {
		String templateName = arguments
				.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.TEMPLATE_NAME);
		String instanceName = arguments
				.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.INSTANCE_NAME);
		String vdcName = arguments
				.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.VDC_NAME);

		Vdc vdc = Vdc.getVdcByReference(vCloudClient, org.getVdcRefsByName()
				.get(vdcName));

		InstantiateVAppTemplateParamsType instVappTemplParamsType = new InstantiateVAppTemplateParamsType();
		instVappTemplParamsType.setName(instanceName);
		instVappTemplParamsType.setSource(getVAppTemplate(templateName)
				.getReference());
		Vapp vapp = vdc.instantiateVappTemplate(instVappTemplParamsType);

		vapp.getTasks().get(0).waitForTask(0);

		vapp = Vapp.getVappByReference(vCloudClient, vapp.getReference());

		String instanceID = vapp.getReference().getId();

		IaasInstance iaasInstance = new IaasInstance(instanceID);
		iaasInstances.put(instanceID, iaasInstance);
		vapps.put(iaasInstance, vapp);

		logger.info("[" + instanceID + "] Instanciated '" + templateName
				+ "' as '" + instanceName + "' on " + vdcName + " ");

		return iaasInstance;
	}

	public void configureNetwork(Map<String, String> arguments)
			throws Exception {
		String instanceID = arguments
				.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.INSTANCE_ID);
		String vdcName = arguments
				.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.VDC_NAME);

		Vapp vapp = Vapp.getVappById(vCloudClient, instanceID);
		Vdc vdc = Vdc.getVdcByReference(vCloudClient, org.getVdcRefsByName()
				.get(vdcName));

		String vappNet = vapp.getNetworkNames().iterator().next();
		VM vm = vapp.getChildrenVms().get(0);
		String id = vm.getResource().getVAppScopedLocalId();

		NetworkConfigSectionType networkConfigSectionType = buildNetworkConfigSection(
				vdc, id, vappNet);

		InstantiationParamsType instantiationParamsType = new InstantiationParamsType();
		List<JAXBElement<? extends SectionType>> section = instantiationParamsType
				.getSection();
		section.add(new ObjectFactory()
				.createNetworkConfigSection(networkConfigSectionType));

		RecomposeVAppParamsType recomposeVappParamsType = new RecomposeVAppParamsType();
		recomposeVappParamsType.setName(vapp.getReference().getName());
		recomposeVappParamsType.setInstantiationParams(instantiationParamsType);

		Task recomposeVapp = vapp.recomposeVapp(recomposeVappParamsType);
		recomposeVapp.waitForTask(0);

		logger.info("[" + instanceID + "] vApp network reconfigured");
	}

	public String deployInstance(Map<String, String> arguments)
			throws Exception {
		String instanceID = arguments
				.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.INSTANCE_ID);

		Vapp vapp = Vapp.getVappById(vCloudClient, instanceID);
		vapp.deploy(true, 0, false).waitForTask(0);

		vapp = Vapp.getVappByReference(vCloudClient, vapp.getReference());
		VAppNetworkConfigurationType vAppNetworkConfigType = vapp
				.getNetworkConfigSection().getNetworkConfig().iterator().next();
		NetworkConfigurationType networkConfig = vAppNetworkConfigType
				.getConfiguration();
		String ip = networkConfig.getRouterInfo().getExternalIp();

		logger.info("[" + instanceID + "] vApp deployed on " + ip);

		return ip;
	}

	@Override
	public void stopInstance(IaasInstance instance) throws Exception {
		vapps.get(instance).shutdown();
	}

	public void deleteInstance(IaasInstance instance) throws Exception {
		vapps.get(instance).delete();
	}

	public void rebootInstance(IaasInstance instance) throws Exception {
		vapps.get(instance).reboot();
	}

	public void snapshotInstance(IaasInstance instance, String name,
			String description, boolean memory, boolean quiesce)
			throws Exception {
		vapps.get(instance).createSnapshot(name, description, memory, quiesce);
	}

	@Override
	public boolean isInstanceStarted(IaasInstance instance) throws Exception {
		return vapps.get(instance).isDeployed();
	}

	@Override
	public String getName() {
		return "VMWare vCloud";
	}

	public IaasInstance getIaasInstance(String instanceID) {
		return iaasInstances.get(instanceID);
	}

	public class VCloudAPIConstants {
		public class ApiParameters {
			static final String API_URL = "apiurl";
			static final String USER_NAME = "username";
			static final String PASSWORD = "password";
			static final String ORGANIZATION_NAME = "organizationName";
		}

		public class InstanceParameters {
			public static final String INSTANCE_NAME = "instanceName";
			public static final String INSTANCE_ID = "instanceID";
			public static final String TEMPLATE_NAME = "templateName";
			public static final String VDC_NAME = "vdcName";
		}

		public class MonitoringParameters {
			public static final String URL = "vim.service.url";
			public static final String USERNAME = "vim.service.username";
			public static final String PASSWORD = "vim.service.password";
		}
	}

	private VappTemplate getVAppTemplate(String vappTemplateName)
			throws VCloudException {
		logger.debug("Searching vApp Template named : " + vappTemplateName);
		Iterator<ReferenceType> itOrg = vCloudClient.getOrgRefs().iterator();
		while (itOrg.hasNext()) {
			VappTemplate vappTemplate = getVAppTemplate(itOrg.next(),
					vappTemplateName);
			if (vappTemplate != null) {
				return vappTemplate;
			}
		}
		throw new VCloudException("vApp Template '" + vappTemplateName
				+ "' not found.");
	}

	private VappTemplate getVAppTemplate(ReferenceType orgRef,
			String vappTemplateName) throws VCloudException {
		logger.debug("Looking for a vApp Template : " + vappTemplateName);
		Organization org = Organization.getOrganizationByReference(
				vCloudClient, orgRef);
		Iterator<ReferenceType> itVdc = org.getVdcRefs().iterator();
		while (itVdc.hasNext()) {
			Vdc vdc = Vdc.getVdcByReference(vCloudClient, itVdc.next());
			Iterator<ReferenceType> itTpl = vdc.getVappTemplateRefs()
					.iterator();
			while (itTpl.hasNext()) {
				ReferenceType vappTemplateRef = itTpl.next();
				if (vappTemplateRef.getName().equals(vappTemplateName)) {
					logger.debug("vApp Template Found : "
							+ vappTemplateRef.getName() + " ["
							+ org.getReference().getName() + "/"
							+ vdc.getReference().getName() + "]");
					return VappTemplate.getVappTemplateByReference(
							vCloudClient, vappTemplateRef);
				}
			}
		}
		return null;
	}

	private NetworkConfigSectionType buildNetworkConfigSection(Vdc vdc,
			String vmId, String networkName) {
		logger.debug("[" + vmId + "] Build network configuration");
		Collection<ReferenceType> availableNetworkRefs = vdc
				.getAvailableNetworkRefs();

		NetworkConfigurationType networkConfigurationType = new NetworkConfigurationType();
		networkConfigurationType.setParentNetwork(availableNetworkRefs
				.iterator().next());
		networkConfigurationType.setFenceMode(FenceModeValuesType.NATROUTED
				.value());

		NetworkFeaturesType features = new NetworkFeaturesType();
		List<JAXBElement<? extends NetworkServiceType>> networkService = features
				.getNetworkService();

		// Setup NAT Port-forwarding
		logger.debug("[" + vmId + "] Setup NAT Port Forwarding");
		NatServiceType natServiceType = new NatServiceType();
		natServiceType.setIsEnabled(true);
		natServiceType.setNatType(NatTypeType.PORTFORWARDING.value());
		natServiceType.setPolicy(NatPolicyType.ALLOWTRAFFIC.value());

		addNatRule(natServiceType.getNatRule(), "SSH", "TCP", 22, 22, vmId);
		addNatRule(natServiceType.getNatRule(), "RDP", "TCP", 3389, 3389, vmId);

		JAXBElement<NetworkServiceType> networkServiceType = new ObjectFactory()
				.createNetworkService(natServiceType);
		networkService.add(networkServiceType);

		// Setup Firewall
		logger.debug("[" + vmId + "] Setup Firewall");
		FirewallServiceType firewallServiceType = new FirewallServiceType();
		firewallServiceType.setIsEnabled(true);
		firewallServiceType.setDefaultAction(FirewallPolicyType.DROP.value());
		firewallServiceType.setLogDefaultAction(false);

		List<FirewallRuleType> fwRules = firewallServiceType.getFirewallRule();
		addFirewallRule(fwRules, "PING", "ICMP", "Any", "Any", "Any");
		addFirewallRule(fwRules, "SSH", "TCP", "Any", "Any", "22");
		addFirewallRule(fwRules, "RDP", "TCP", "Any", "Any", "3389");
		addFirewallRule(fwRules, "In-Out", "ANY", "internal", "external", "Any");

		JAXBElement<FirewallServiceType> firewall = (new ObjectFactory())
                .createFirewallService(firewallServiceType);
        networkService.add(firewall);

        networkConfigurationType.setFeatures(features);

        VAppNetworkConfigurationType vAppNetworkConfigurationType = new VAppNetworkConfigurationType();
        vAppNetworkConfigurationType.setConfiguration(networkConfigurationType);

        vAppNetworkConfigurationType.setNetworkName(networkName);

        NetworkConfigSectionType networkConfigSectionType = new NetworkConfigSectionType();
		MsgType networkMsgType = new MsgType();
		networkConfigSectionType.setInfo(networkMsgType);
		List<VAppNetworkConfigurationType> networkConfig = networkConfigSectionType
				.getNetworkConfig();
		networkConfig.add(vAppNetworkConfigurationType);

		return networkConfigSectionType;
	}

	private void addNatRule(List<NatRuleType> natRules, String name,
			String protocol, int intPort, int extPort, String vmId) {
		NatVmRuleType natVmRuleType = new NatVmRuleType();
		natVmRuleType.setExternalPort(extPort);
		natVmRuleType.setInternalPort(intPort);
		natVmRuleType.setVAppScopedVmId(vmId);
		natVmRuleType.setVmNicId(0);
		natVmRuleType.setProtocol(protocol);
		NatRuleType natRuleType = new NatRuleType();
		natRuleType.setVmRule(natVmRuleType);
		natRuleType.setDescription(name);
		natRules.add(natRuleType);
	}

	private void addFirewallRule(List<FirewallRuleType> fwRules, String name,
			String protocol, String srcIp, String dstIp, String portRange) {
		FirewallRuleType firewallRuleType = new FirewallRuleType();
		firewallRuleType.setDescription(name);
		firewallRuleType.setSourceIp(srcIp);
		firewallRuleType.setSourcePort(-1);
		firewallRuleType.setDestinationIp(dstIp);
		FirewallRuleProtocols protocols = new FirewallRuleProtocols();
		if (protocol.equalsIgnoreCase("ICMP")) {
			protocols.setIcmp(true);
			firewallRuleType.setIcmpSubType("any");
		} else if (protocol.equalsIgnoreCase("TCP")) {
            protocols.setTcp(true);
            firewallRuleType.setDestinationPortRange(portRange);
        } else if (protocol.equalsIgnoreCase("ANY")) {
            protocols.setAny(true);
            firewallRuleType.setDestinationPortRange(portRange);
        }
        firewallRuleType.setProtocols(protocols);
        fwRules.add(firewallRuleType);
    }

    @Override
    public void disconnect() throws Exception {
    }

	private void initializeMonitoringService(String url, String username,
			String password) {
		try {
			vimServiceClient = new VimServiceClient();
			vimServiceClient.initialize(url, username, password);
		} catch (Exception e) {
			logger.error("Cannot initialize the VimSerivceClient instance: "
					+ e);
			throw new RuntimeException(e);
		}

    }

    private Map<String, Object> convert(Map<String, String> a) {
        Map<String, Object> r = new HashMap<String, Object>();
        r.putAll(a);
        return r;
    }

    @Override
    public String[] getHosts() throws Exception {
        return vimServiceClient.getHosts();
    }

    @Override
    public String[] getVMs() throws Exception {
        return vimServiceClient.getVMs();
    }

    @Override
    public String[] getVMs(String hostId) throws Exception {
        return vimServiceClient.getVMs(hostId);
    }

	@Override
	public Map<String, String> getHostProperties(String hostId)
			throws Exception {
		return vimServiceClient.getHostProperties(hostId);
    }

    @Override
    public Map<String, String> getVMProperties(String vmId) throws Exception {
        return vimServiceClient.getVMProperties(vmId);
    }

    @Override
    public Map<String, Object> getVendorDetails() throws Exception {
        return vimServiceClient.getVendorDetails();
    }

}
