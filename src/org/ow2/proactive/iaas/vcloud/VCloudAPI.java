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
import java.math.BigInteger;
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
import org.ow2.proactive.iaas.IaasApi;
import org.ow2.proactive.iaas.IaasInstance;

import com.vmware.vcloud.api.rest.schema.CaptureVAppParamsType;
import com.vmware.vcloud.api.rest.schema.CustomizationSectionType;
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
import com.vmware.vcloud.sdk.VirtualCpu;
import com.vmware.vcloud.sdk.VirtualDisk;
import com.vmware.vcloud.sdk.VirtualMemory;
import com.vmware.vcloud.sdk.constants.BusSubType;
import com.vmware.vcloud.sdk.constants.BusType;
import com.vmware.vcloud.sdk.constants.FenceModeValuesType;
import com.vmware.vcloud.sdk.constants.FirewallPolicyType;
import com.vmware.vcloud.sdk.constants.NatPolicyType;
import com.vmware.vcloud.sdk.constants.NatTypeType;
import com.vmware.vcloud.sdk.constants.UndeployPowerActionType;
import com.vmware.vcloud.sdk.constants.VappStatus;
import com.vmware.vcloud.sdk.constants.Version;


public class VCloudAPI implements IaasApi {

    private static final Logger logger = Logger.getLogger(VCloudAPI.class);

    private static Map<Integer, VCloudAPI> instances;

    private Map<String, IaasInstance> iaasInstances;
    private long created;
    private VcloudClient vcd;
    private Organization org;
    private URI endpoint;

    /////
    // VCLOUD FACTORY
    /////
    public static IaasApi getVCloudAPI(Map<String, String> args) throws URISyntaxException,
            AuthenticationException {
        return getVCloudAPI(args.get(VCloudAPIConstants.ApiParameters.USER_NAME),
                args.get(VCloudAPIConstants.ApiParameters.PASSWORD),
                new URI(args.get(VCloudAPIConstants.ApiParameters.API_URL)),
                args.get(VCloudAPIConstants.ApiParameters.ORGANIZATION_NAME));
    }

    public static synchronized VCloudAPI getVCloudAPI(String login, String password, URI endpoint,
            String orgName) throws AuthenticationException {
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
                throw new AuthenticationException("Authentication failed to " + endpoint, t);
            }
            instances.put(hash, instance);
        }
        return instance;
    }

    public VCloudAPI(String login, String password, URI endpoint, String orgName) throws IOException,
            KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException,
            VCloudException {
        this.iaasInstances = new HashMap<String, IaasInstance>();
        this.created = System.currentTimeMillis();
        this.endpoint = endpoint;
        authenticate(login, password, orgName);
    }

    public void authenticate(String login, String password, String orgName) throws VCloudException,
            KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
        VcloudClient.setLogLevel(Level.OFF);
        vcd = new VcloudClient(this.endpoint.toString(), Version.V5_1);
        vcd.registerScheme("https", 443, FakeSSLSocketFactory.getInstance());
        vcd.login(login, password);
        org = Organization.getOrganizationByReference(vcd, vcd.getOrgRefsByName().get(orgName));

        logger.info("Authentication success for " + login);
    }

    @Override
    public IaasInstance startInstance(Map<String, String> arguments) throws Exception {
        IaasInstance iaasInstance = createInstance(arguments);
        arguments.put(VCloudAPIConstants.InstanceParameters.INSTANCE_ID, iaasInstance.getInstanceId());
        deployInstance(arguments);
        return iaasInstance;
    }

    public IaasInstance createInstance(Map<String, String> arguments) throws Exception {
        String templateName = arguments.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.TEMPLATE_NAME);
        String instanceName = arguments.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.INSTANCE_NAME);
        String vdcName = arguments.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.VDC_NAME);

        Vdc vdc = Vdc.getVdcByReference(vcd, org.getVdcRefsByName().get(vdcName));

        InstantiateVAppTemplateParamsType instVappTemplParamsType = new InstantiateVAppTemplateParamsType();
        instVappTemplParamsType.setName(instanceName);
        instVappTemplParamsType.setSource(getVAppTemplate(templateName).getReference());
        Vapp vapp = vdc.instantiateVappTemplate(instVappTemplParamsType);

        vapp.getTasks().get(0).waitForTask(0);

        String instanceID = vapp.getReference().getId();

        IaasInstance iaasInstance = new IaasInstance(instanceID);
        iaasInstances.put(instanceID, iaasInstance);

        logger.info("[" + instanceID + "] Instanciated '" + templateName + "' as '" + instanceName + "' on " +
            vdcName + " ");

        return iaasInstance;
    }

    public void configureNetwork(Map<String, String> arguments) throws Exception {
        String instanceID = arguments.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.INSTANCE_ID);
        String vdcName = arguments.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.VDC_NAME);

        Vapp vapp = Vapp.getVappById(vcd, instanceID);
        Vdc vdc = Vdc.getVdcByReference(vcd, org.getVdcRefsByName().get(vdcName));

        String vappNet = vapp.getNetworkNames().iterator().next();
        VM vm = vapp.getChildrenVms().get(0);
        String id = vm.getResource().getVAppScopedLocalId();

        NetworkConfigSectionType networkConfigSectionType = buildNetworkConfigSection(vdc, id, vappNet);

        InstantiationParamsType instantiationParamsType = new InstantiationParamsType();
        List<JAXBElement<? extends SectionType>> section = instantiationParamsType.getSection();
        section.add(new ObjectFactory().createNetworkConfigSection(networkConfigSectionType));

        RecomposeVAppParamsType recomposeVappParamsType = new RecomposeVAppParamsType();
        recomposeVappParamsType.setName(vapp.getReference().getName());
        recomposeVappParamsType.setInstantiationParams(instantiationParamsType);

        Task recomposeVapp = vapp.recomposeVapp(recomposeVappParamsType);
        recomposeVapp.waitForTask(0);

        logger.info("[" + instanceID + "] vApp network reconfigured");
    }

    public void configureVM(String instanceID, int cpu, int memoryMB, int diskMB) throws Exception {
        logger.debug("[" + instanceID + "] Reconfigure VM");
        Vapp vapp = Vapp.getVappById(vcd, instanceID);
        VM vm = vapp.getChildrenVms().get(0);

        // CPU
        logger.debug("[" + instanceID + "] Modifying CPU count");
        VirtualCpu vCpu = vm.getCpu();
        vCpu.setNoOfCpus(cpu);
        vm.updateCpu(vCpu).waitForTask(0);

        // Memory
        logger.debug("[" + instanceID + "] Modifying memory amount");
        VirtualMemory vMemory = vm.getMemory();
        vMemory.setMemorySize(BigInteger.valueOf(memoryMB));
        vm.updateMemory(vMemory).waitForTask(0);

        // Disk
        logger.debug("[" + instanceID + "] Modifying disk size");
        List<VirtualDisk> vDisks = vm.getDisks();
        Iterator<VirtualDisk> it = vDisks.iterator();
        boolean updated = false;
        while (it.hasNext()) {
            VirtualDisk vDisk = it.next();
            if (vDisk.isHardDisk() && !updated) {
                updated = true;
                logger.debug("[" + instanceID + "] Disk " + vDisk.getHardDiskBusType() + " " +
                    vDisk.getHardDiskSize() + " MB -> " + diskMB + " MB");
                vDisk.updateHardDiskSize(BigInteger.valueOf(diskMB));
                vm.updateDisks(vDisks).waitForTask(0);
            }
        }

        vm.upgradeHardware().waitForTask(0);
        logger.info("[" + instanceID + "] VM reconfigured : " + cpu + " CPU / " + memoryMB + " MB RAM / " +
            diskMB + " MB Disk");
    }

    public String deployInstance(Map<String, String> arguments) throws Exception {
        String instanceID = arguments.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.INSTANCE_ID);
        logger.debug("[" + instanceID + "] vApp being deployed");

        Vapp vapp = Vapp.getVappById(vcd, instanceID);
        vapp.deploy(true, 0, false).waitForTask(0);

        vapp = Vapp.getVappByReference(vcd, vapp.getReference());
        String ip = vapp.getNetworkConfigSection().getNetworkConfig().iterator().next().getConfiguration()
                .getRouterInfo().getExternalIp();

        logger.info("[" + instanceID + "] vApp deployed on " + ip);

        return ip;
    }

    public void startInstance(IaasInstance instance) throws Exception {
        logger.debug("[" + instance.getInstanceId() + "] VM being started");
        Vapp vapp = Vapp.getVappById(vcd, instance.getInstanceId());
        if (vapp.isDeployed()) {
            VM vm = vapp.getChildrenVms().iterator().next();
            vm.powerOn().waitForTask(0);
        }
        logger.info("[" + instance.getInstanceId() + "] VM started");
    }

    @Override
    public void stopInstance(IaasInstance instance) throws Exception {
        logger.debug("[" + instance.getInstanceId() + "] VM being stopped");
        Vapp vapp = Vapp.getVappById(vcd, instance.getInstanceId());
        if (vapp.isDeployed()) {
            VM vm = vapp.getChildrenVms().iterator().next();
            vapp.powerOff().waitForTask(0);
        }
        logger.info("[" + instance.getInstanceId() + "] VM stopped");
    }

    public void undeployInstance(IaasInstance instance) throws Exception {
        logger.debug("[" + instance.getInstanceId() + "] vApp being undeployed");
        Vapp vapp = Vapp.getVappById(vcd, instance.getInstanceId());
        if (vapp.isDeployed()) {
            if (vapp.getVappStatus() == VappStatus.POWERED_ON) {
                vapp.undeploy(UndeployPowerActionType.POWEROFF).waitForTask(0);
            } else {
                vapp.undeploy(UndeployPowerActionType.DEFAULT).waitForTask(0);
            }
        }
        logger.info("[" + instance.getInstanceId() + "] vApp undeployed");
    }

    public void deleteInstance(IaasInstance instance) throws Exception {
        logger.debug("[" + instance.getInstanceId() + "] vApp being deleted");
        Vapp.getVappById(vcd, instance.getInstanceId()).delete().waitForTask(0);
        logger.info("[" + instance.getInstanceId() + "] vApp deleted");
    }

    public void rebootInstance(IaasInstance instance) throws Exception {
        logger.debug("[" + instance.getInstanceId() + "] vApp being rebooted");
        this.stopInstance(instance);
        this.startInstance(instance);
        logger.info("[" + instance.getInstanceId() + "] vApp rebooted");
    }

    public void templateFromInstance(Map<String, String> arguments, IaasInstance instance, String templateName)
            throws Exception {
        logger.debug("[" + instance.getInstanceId() + "] vApp templating");
        // get the vdc
        String vdcName = "COMMUN-P5"; //arguments.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.VDC_NAME);
        Vdc vdc = Vdc.getVdcByReference(vcd, org.getVdcRefsByName().get(vdcName));
        // capture vapp params
        Vapp vapp = Vapp.getVappById(vcd, instance.getInstanceId());
        CaptureVAppParamsType captureVappParams = new CaptureVAppParamsType();
        captureVappParams.setName(templateName);
        captureVappParams.setSource(vapp.getReference());
        // set the customization settings to true
        CustomizationSectionType customizationSectionType = new CustomizationSectionType();
        customizationSectionType.setCustomizeOnInstantiate(true);
        customizationSectionType.setInfo(new MsgType());
        // add the customization settings to the capture vapp params.
        captureVappParams.getSection().add(
                new com.vmware.vcloud.api.rest.schema.ObjectFactory()
                        .createCustomizationSection(customizationSectionType));

        // perform the capture vapp operation
        VappTemplate vappTemplate = vdc.captureVapp(captureVappParams);
        logger.debug("Sleeping 60sec.");
        Thread.sleep(60000);
        logger.info("[" + instance.getInstanceId() + "] vApp template done under '" + templateName + "'");
    }

    public void snapshotInstance(IaasInstance instance, String name, String description, boolean memory,
            boolean quiesce) throws Exception {
        logger.debug("[" + instance.getInstanceId() + "] vApp snapshoting");
        Vapp.getVappById(vcd, instance.getInstanceId()).createSnapshot(name, description, memory, quiesce)
                .waitForTask(0);
        logger.info("[" + instance.getInstanceId() + "] vApp snapshot done under '" + name + "'");
    }

    public void removeSnapshotInstance(IaasInstance instance) throws Exception {
        logger.debug("[" + instance.getInstanceId() + "] vApp removing all snapshots");
        Vapp.getVappById(vcd, instance.getInstanceId()).removeAllSnapshots().waitForTask(0);
        logger.info("[" + instance.getInstanceId() + "] vApp removed all snapshots");
    }

    public void revertSnapshotInstance(IaasInstance instance, String name, String description,
            boolean memory, boolean quiesce) throws Exception {
        logger.debug("[" + instance.getInstanceId() + "] vApp reverting to snapshot");
        Vapp.getVappById(vcd, instance.getInstanceId()).revertToCurrentSnapshot().waitForTask(0);
        logger.info("[" + instance.getInstanceId() + "] vApp revert to snapshot");
    }

    public void addDisk(IaasInstance instance, int sizeMB, String mountpoint) throws Exception {
        logger.debug("[" + instance.getInstanceId() + "] add a disk to the VM");
        Vapp vapp = Vapp.getVappById(vcd, instance.getInstanceId());
        VM vm = vapp.getChildrenVms().get(0);
        VirtualDisk vDisk = new VirtualDisk(BigInteger.valueOf(sizeMB), BusType.SCSI, BusSubType.LSI_LOGIC);
        List<VirtualDisk> vDisks = vm.getDisks();
        vDisks.add(vDisk);
        vm.updateDisks(vDisks).waitForTask(0);
        logger.info("[" + instance.getInstanceId() + "] Disk added to the vApp'");
    }

    @Override
    public boolean isInstanceStarted(IaasInstance instance) throws Exception {
        return Vapp.getVappById(vcd, instance.getInstanceId()).isDeployed();
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
            public static final String CORES = "cores";
            public static final String MEMORY = "memory";
            public static final String STORAGE = "storage";
        }
    }

    private VappTemplate getVAppTemplate(String vappTemplateName) throws VCloudException {
        logger.debug("Searching vApp Template named : " + vappTemplateName);
        Iterator<ReferenceType> itOrg = vcd.getOrgRefs().iterator();
        while (itOrg.hasNext()) {
            VappTemplate vappTemplate = getVAppTemplate(itOrg.next(), vappTemplateName);
            if (vappTemplate != null) {
                return vappTemplate;
            }
        }
        throw new VCloudException("vApp Template '" + vappTemplateName + "' not found.");
    }

    private VappTemplate getVAppTemplate(ReferenceType orgRef, String vappTemplateName)
            throws VCloudException {
        logger.debug("Looking for a vApp Template : " + vappTemplateName);
        Organization org = Organization.getOrganizationByReference(vcd, orgRef);
        Iterator<ReferenceType> itVdc = org.getVdcRefs().iterator();
        while (itVdc.hasNext()) {
            Vdc vdc = Vdc.getVdcByReference(vcd, itVdc.next());
            Iterator<ReferenceType> itTpl = vdc.getVappTemplateRefs().iterator();
            while (itTpl.hasNext()) {
                ReferenceType vappTemplateRef = itTpl.next();
                if (vappTemplateRef.getName().equals(vappTemplateName)) {
                    logger.debug("vApp Template Found : " + vappTemplateRef.getName() + " [" +
                        org.getReference().getName() + "/" + vdc.getReference().getName() + "]");
                    return VappTemplate.getVappTemplateByReference(vcd, vappTemplateRef);
                }
            }
        }
        return null;
    }

    private NetworkConfigSectionType buildNetworkConfigSection(Vdc vdc, String vmId, String networkName) {
        logger.debug("[" + vmId + "] Build network configuration");
        Collection<ReferenceType> availableNetworkRefs = vdc.getAvailableNetworkRefs();

        NetworkConfigurationType networkConfigurationType = new NetworkConfigurationType();
        networkConfigurationType.setParentNetwork(availableNetworkRefs.iterator().next());
        networkConfigurationType.setFenceMode(FenceModeValuesType.NATROUTED.value());

        NetworkFeaturesType features = new NetworkFeaturesType();
        List<JAXBElement<? extends NetworkServiceType>> networkService = features.getNetworkService();

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
        List<VAppNetworkConfigurationType> networkConfig = networkConfigSectionType.getNetworkConfig();
        networkConfig.add(vAppNetworkConfigurationType);

        return networkConfigSectionType;
    }

    private void addNatRule(List<NatRuleType> natRules, String name, String protocol, int intPort,
            int extPort, String vmId) {
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

    private void addFirewallRule(List<FirewallRuleType> fwRules, String name, String protocol, String srcIp,
            String dstIp, String portRange) {
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
}
