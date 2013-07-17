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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import javax.security.sasl.AuthenticationException;
import javax.xml.bind.JAXBElement;

import org.apache.log4j.Logger;
import org.ow2.proactive.iaas.IaasApi;
import org.ow2.proactive.iaas.IaasInstance;
import org.ow2.proactive.iaas.IaasMonitoringApi;
import org.ow2.proactive.iaas.monitoring.IaasMonitoringException;
import org.ow2.proactive.iaas.vcloud.monitoring.ViServiceClientException;
import org.ow2.proactive.iaas.vcloud.monitoring.VimServiceClient;

import com.vmware.vcloud.api.rest.schema.CaptureVAppParamsType;
import com.vmware.vcloud.api.rest.schema.CloneVAppParamsType;
import com.vmware.vcloud.api.rest.schema.CustomizationSectionType;
import com.vmware.vcloud.api.rest.schema.FirewallRuleProtocols;
import com.vmware.vcloud.api.rest.schema.FirewallRuleType;
import com.vmware.vcloud.api.rest.schema.FirewallServiceType;
import com.vmware.vcloud.api.rest.schema.GuestCustomizationSectionType;
import com.vmware.vcloud.api.rest.schema.InstantiateVAppTemplateParamsType;
import com.vmware.vcloud.api.rest.schema.InstantiationParamsType;
import com.vmware.vcloud.api.rest.schema.NatRuleType;
import com.vmware.vcloud.api.rest.schema.NatServiceType;
import com.vmware.vcloud.api.rest.schema.NatVmRuleType;
import com.vmware.vcloud.api.rest.schema.NetworkConfigSectionType;
import com.vmware.vcloud.api.rest.schema.NetworkConfigurationType;
import com.vmware.vcloud.api.rest.schema.NetworkConnectionSectionType;
import com.vmware.vcloud.api.rest.schema.NetworkConnectionType;
import com.vmware.vcloud.api.rest.schema.NetworkFeaturesType;
import com.vmware.vcloud.api.rest.schema.NetworkServiceType;
import com.vmware.vcloud.api.rest.schema.ObjectFactory;
import com.vmware.vcloud.api.rest.schema.RecomposeVAppParamsType;
import com.vmware.vcloud.api.rest.schema.ReferenceType;
import com.vmware.vcloud.api.rest.schema.SourcedCompositionItemParamType;
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
import com.vmware.vcloud.sdk.constants.IpAddressAllocationModeType;
import com.vmware.vcloud.sdk.constants.NatPolicyType;
import com.vmware.vcloud.sdk.constants.NatTypeType;
import com.vmware.vcloud.sdk.constants.UndeployPowerActionType;
import com.vmware.vcloud.sdk.constants.VappStatus;
import com.vmware.vcloud.sdk.constants.Version;


public class VCloudAPI implements IaasApi, IaasMonitoringApi {

    private static final Logger logger = Logger.getLogger(VCloudAPI.class);

    private static Map<Integer, VCloudAPI> instances;

    private Map<String, IaasInstance> iaasInstances;
    private Map<IaasInstance, Vapp> vapps;
    private long created;
    private VcloudClient vCloudClient;
    private Organization org;
    private URI endpoint;

    private VimServiceClient vimServiceClient;

    private String credLogin;
    private String credPassword;

    public static IaasApi getVCloudAPI(Map<String, String> args) throws URISyntaxException,
            AuthenticationException {
        VCloudAPI vCloudAPI = getVCloudAPI(args.get(VCloudAPIConstants.ApiParameters.USER_NAME),
                args.get(VCloudAPIConstants.ApiParameters.PASSWORD),
                new URI(args.get(VCloudAPIConstants.ApiParameters.API_URL)),
                args.get(VCloudAPIConstants.ApiParameters.ORGANIZATION_NAME));
        if (args.get(VCloudAPIConstants.MonitoringParameters.URL) != null) {
            vCloudAPI.initializeMonitoringService(args.get(VCloudAPIConstants.MonitoringParameters.URL),
                    args.get(VCloudAPIConstants.MonitoringParameters.USERNAME),
                    args.get(VCloudAPIConstants.MonitoringParameters.PASSWORD));
        }
        return vCloudAPI;
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
                throw new AuthenticationException("Authentication failed to " + endpoint + " [login=" +
                    login + ", password=" + password + ", orgName=" + orgName + "]", t);
            }
            instances.put(hash, instance);
        }
        return instance;
    }

    public static synchronized VCloudAPI getVCloudAPI(String login, String password, URI endpoint,
            String orgName, String vimServiceUrl, String vimServiceUsername, String vimServicePassword)
            throws AuthenticationException {
        if (instances == null) {
            instances = new HashMap<Integer, VCloudAPI>();
        }
        int hash = (login + password).hashCode();
        VCloudAPI instance = instances.get(hash);
        if (instance == null) {
            try {
                instances.remove(hash);
                instance = new VCloudAPI(login, password, endpoint, orgName, vimServiceUrl,
                    vimServiceUsername, vimServicePassword);
            } catch (Throwable t) {
                throw new AuthenticationException("Authentication failed to " + endpoint + " [login=" +
                    login + ", password=" + password + ", orgName=" + orgName + "]", t);
            }
            instances.put(hash, instance);
        }
        return instance;
    }

    public VCloudAPI(String login, String password, URI endpoint, String orgName, String vimServiceUrl,
            String vimServiceUsername, String vimServicePassword) throws IOException, KeyManagementException,
            UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, VCloudException {
        this.iaasInstances = new HashMap<String, IaasInstance>();
        this.vapps = new HashMap<IaasInstance, Vapp>();
        this.created = System.currentTimeMillis();
        this.endpoint = endpoint;
        authenticate(login, password, orgName);
        initializeMonitoringService(vimServiceUrl, vimServiceUsername, vimServicePassword);
    }

    public VCloudAPI(String login, String password, URI endpoint, String orgName) throws IOException,
            KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException,
            VCloudException {
        this.iaasInstances = new HashMap<String, IaasInstance>();
        this.vapps = new HashMap<IaasInstance, Vapp>();
        this.created = System.currentTimeMillis();
        this.endpoint = endpoint;
        authenticate(login, password, orgName);
    }

    public void authenticate(String login, String password, String orgName) throws VCloudException,
            KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException {
        VcloudClient.setLogLevel(Level.OFF);
        credLogin = login;
        credPassword = password;
        logger.info("Authenticating to vCloud infrastructure...");
        vCloudClient = new VcloudClient(this.endpoint.toString(), Version.V5_1);
        vCloudClient.registerScheme("https", 443, FakeSSLSocketFactory.getInstance());
        vCloudClient.login(login, password);
        org = Organization.getOrganizationByReference(vCloudClient,
                vCloudClient.getOrgRefsByName().get(orgName));
        logger.debug("Authentication success for " + login);
    }

    private void checkConnection() throws VCloudException {
        try {
            vCloudClient.extendSession();
            vCloudClient.getUpdatedOrgList();
        } catch (VCloudException e) {
            logger.warn("Session seems to have expired, trying to reconnect..." + e.getMessage());
            logger.debug("Session seems to have expired, trying to reconnect...", e);
            vCloudClient.login(credLogin, credPassword);
        }
    }

    @Override
    public IaasInstance startInstance(Map<String, String> arguments) throws Exception {
        IaasInstance iaasInstance = createInstance(arguments);
        arguments.put(VCloudAPIConstants.InstanceParameters.INSTANCE_ID, iaasInstance.getInstanceId());
        configureNetwork(arguments);
        customizeGuestOs(arguments);
        deployInstance(arguments);
        return iaasInstance;
    }

    public IaasInstance createInstance(Map<String, String> arguments) throws Exception {
        String templateName = arguments.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.TEMPLATE_NAME);
        String instanceName = arguments.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.INSTANCE_NAME);
        String vdcName = arguments.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.VDC_NAME);

        checkConnection();
        Vdc vdc = Vdc.getVdcByReference(vCloudClient, org.getVdcRefsByName().get(vdcName));

        InstantiateVAppTemplateParamsType instVappTemplParamsType = new InstantiateVAppTemplateParamsType();
        instVappTemplParamsType.setName(instanceName);
        instVappTemplParamsType.setSource(getVAppTemplate(templateName).getReference());
        Vapp vapp = vdc.instantiateVappTemplate(instVappTemplParamsType);
        logger.debug("Instanciating '" + templateName + "' as '" + instanceName + "' on " + vdcName + " ");

        logger.debug("Number of tasks : " + vapp.getTasks().size());
        logger.debug("Task[0] : " + vapp.getTasks().get(0).getReference().getName());
        logger.debug("Task opname : " + vapp.getTasks().get(0).getResource().getOperation());
        Task task = vapp.getTasks().get(0);
        try {
            task.waitForTask(0);
        } catch (Throwable e) {
            logger.debug("ERR: Task error : " + task.getResource().getError());
        }

        vapp = Vapp.getVappByReference(vCloudClient, vapp.getReference());

        String instanceID = vapp.getReference().getId();

        IaasInstance iaasInstance = new IaasInstance(instanceID);
        iaasInstances.put(instanceID, iaasInstance);
        vapps.put(iaasInstance, vapp);

        System.out.println("[" + instanceID + "] Instanciated '" + templateName + "' as '" + instanceName +
            "' on " + vdcName + " ");

        return iaasInstance;
    }

    public void configureNetwork(Map<String, String> arguments) throws Exception {
        String instanceID = arguments.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.INSTANCE_ID);
        String vdcName = arguments.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.VDC_NAME);
        configureNetwork(instanceID, vdcName, new ArrayList(), new ArrayList());
    }

    public void configureNetwork(String instanceID, String vdcName, List<NatRule> natRules,
            List<FirewallRule> firewallRules) throws Exception {
        checkConnection();
        Vapp vapp = Vapp.getVappById(vCloudClient, instanceID);
        Vdc vdc = Vdc.getVdcByReference(vCloudClient, org.getVdcRefsByName().get(vdcName));

        String vappNet = vapp.getNetworkNames().iterator().next();
        VM vm = vapp.getChildrenVms().get(0);
        String id = vm.getResource().getVAppScopedLocalId();

        NetworkConfigSectionType networkConfigSectionType = buildNetworkConfigSection(vdc, id, vappNet,
                natRules, firewallRules);

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
        logger.info("[" + instanceID + "] Reconfiguring VM : " + cpu + " CPU / " + memoryMB + " MB RAM / " +
            diskMB + " MB Disk");
        checkConnection();
        Vapp vapp = Vapp.getVappById(vCloudClient, instanceID);
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
        logger.debug("[" + instanceID + "] VM reconfigured : " + cpu + " CPU / " + memoryMB + " MB RAM / " +
            diskMB + " MB Disk");
    }

    public void undeployInstance(String instanceID) throws Exception {
        logger.info("[" + instanceID + "] Undeploying vApp...");
        checkConnection();
        Vapp vapp = Vapp.getVappById(vCloudClient, instanceID);
        if (vapp.isDeployed()) {
            if (vapp.getVappStatus() == VappStatus.POWERED_ON) {
                vapp.undeploy(UndeployPowerActionType.POWEROFF).waitForTask(0);
            } else {
                vapp.undeploy(UndeployPowerActionType.DEFAULT).waitForTask(0);
            }
        }
        logger.debug("[" + instanceID + "] vApp undeployed");
    }

    private void customizeGuestOs(Map<String, String> arguments) throws Exception {
        String instanceID = arguments.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.INSTANCE_ID);
        Vapp vapp = Vapp.getVappById(vCloudClient, instanceID);
        VM vm = vapp.getChildrenVms().get(0);
        if (vm.isDeployed()) {
            if (logger.isDebugEnabled()) {
                logger.debug(String
                        .format("Virtual machine instance [%s] is already deployed. Undeploying it before customization.",
                                instanceID));
            }
            vm.undeploy(UndeployPowerActionType.FORCE).waitForTask(0);
        }
        GuestCustomizationSectionType guestCustomizationSection = vm.getGuestCustomizationSection();
        guestCustomizationSection.setEnabled(true);
        guestCustomizationSection.getDomainUserPassword();
        CustomizeScriptBuilder customizeScriptBuilder = new CustomizeScriptBuilder();
        customizeScriptBuilder.setRmNodeName(instanceID);
        customizeScriptBuilder
                .setRmUrl(arguments.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.RM_URL));
        customizeScriptBuilder.setRmCredentialValue(arguments
                .get(VCloudAPI.VCloudAPIConstants.InstanceParameters.RM_CRED_VAL));
        String script = customizeScriptBuilder.buildScript();
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Guest OS customization script: %n%s", script));
        }
        guestCustomizationSection.setCustomizationScript(script);
        vm.updateSection(guestCustomizationSection).waitForTask(0);
    }

    public void setPassword(String instanceID, String password) throws Exception {
        Vapp vapp = Vapp.getVappById(vCloudClient, instanceID);
        VM vm = vapp.getChildrenVms().get(0);
        GuestCustomizationSectionType guestCustomizationSection = vm.getGuestCustomizationSection();
        boolean isDeployed = vm.isDeployed();
        if (isDeployed) {
            logger.debug(String
                    .format("Virtual machine instance [%s] is already deployed. Undeploying it before customization.",
                            instanceID));
            vm.undeploy(UndeployPowerActionType.FORCE).waitForTask(0);
        }
        guestCustomizationSection.setAdminPassword(password);
        if (isDeployed) {
            vm.deploy(true, 0, true);
        }
    }

    public String getPassword(String instanceID) throws Exception {
        Vapp vapp = Vapp.getVappById(vCloudClient, instanceID);
        VM vm = vapp.getChildrenVms().get(0);
        GuestCustomizationSectionType guestCustomizationSection = vm.getGuestCustomizationSection();
        return guestCustomizationSection.getAdminPassword();
    }

    public List<String> getVmId(String instanceID) throws Exception {
        List<String> vmIDs = new ArrayList<String>();
        Vapp vapp = Vapp.getVappById(vCloudClient, instanceID);
        for (VM vm : vapp.getChildrenVms()) {
            String vmName = vm.getReference().getName();
            String vmId = vm.getReference().getId();
            String vmHref = vm.getReference().getHref();
            vmIDs.add(vmId);
        }
        return vmIDs;
    }

    public void listEverything() throws Exception {
        HashMap<String, ReferenceType> orgs = vCloudClient.getOrgRefsByName();
        for (Entry<String, ReferenceType> orgEntry : orgs.entrySet()) {
            System.out.println("ORG => " + orgEntry.getKey() + " --> " + orgEntry.getValue().getId());
            Organization org = Organization.getOrganizationByReference(vCloudClient, orgEntry.getValue());
            HashMap<String, ReferenceType> vdcEntries = org.getVdcRefsByName();
            for (Entry<String, ReferenceType> vdcEntry : vdcEntries.entrySet()) {
                System.out.println("  VDC => " + vdcEntry.getKey() + " --> " + vdcEntry.getValue().getId());

                Vdc vdc = Vdc.getVdcByReference(vCloudClient, org.getVdcRefsByName().get(vdcEntry.getKey()));
                HashMap<String, ReferenceType> vapps = vdc.getVappRefsByName();
                for (Entry<String, ReferenceType> vappEntry : vapps.entrySet()) {
                    String vappName = vappEntry.getKey();
                    String vappId = Vapp.getVappByReference(vCloudClient, vappEntry.getValue()).getResource()
                            .getId();
                    System.out.println("    VAPP => " + vappName + " --> " + vappId);

                    Vapp vapp = Vapp.getVappByReference(vCloudClient, vappEntry.getValue());
                    for (VM vm : vapp.getChildrenVms()) {
                        String vmName = vm.getReference().getName();
                        String vmId = vm.getReference().getId();
                        String vmHref = vm.getReference().getHref();
                        System.out.println("      VM => " + vmName + " --> " + vmId + " [" + vmHref + "]");
                    }
                }
            }
        }

    }

    public String deployInstance(Map<String, String> arguments) throws Exception {
        String instanceID = arguments.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.INSTANCE_ID);
        return deployInstance(instanceID);
    }

    public String deployInstance(String instanceID) throws Exception {
        checkConnection();
        Vapp vapp = Vapp.getVappById(vCloudClient, instanceID);
        vapp.deploy(true, 0, false).waitForTask(0);
        vapp = Vapp.getVappByReference(vCloudClient, vapp.getReference());
        VAppNetworkConfigurationType vAppNetworkConfigType = vapp.getNetworkConfigSection()
                .getNetworkConfig().iterator().next();
        NetworkConfigurationType networkConfig = vAppNetworkConfigType.getConfiguration();
        String ip = networkConfig.getRouterInfo().getExternalIp();

        logger.info("[" + instanceID + "] vApp deployed on " + ip);

        return ip;
    }

    public void startInstance(String instanceID) throws Exception {
        logger.info("[" + instanceID + "] Starting VM...");
        checkConnection();
        Vapp vapp = Vapp.getVappById(vCloudClient, instanceID);
        if (vapp.isDeployed()) {
            VM vm = vapp.getChildrenVms().iterator().next();
            vm.powerOn().waitForTask(0);
        }
        logger.debug("[" + instanceID + "] VM started");
    }

    public void stopInstance(String instanceID) throws Exception {
        logger.info("[" + instanceID + "] Stopping VM...");
        checkConnection();
        Vapp vapp = Vapp.getVappById(vCloudClient, instanceID);
        if (vapp.isDeployed()) {
            VM vm = vapp.getChildrenVms().iterator().next();
            vm.powerOff().waitForTask(0);
        }
        logger.debug("[" + instanceID + "] VM stopped");
    }

    public void deleteInstance(String instanceID) throws Exception {
        logger.info("[" + instanceID + "] Deleting vApp...");
        checkConnection();
        Vapp.getVappById(vCloudClient, instanceID).delete().waitForTask(0);
        logger.debug("[" + instanceID + "] vApp deleted");
    }

    public void rebootInstance(String instanceID) throws Exception {
        logger.info("[" + instanceID + "] Restarting vApp...");
        this.stopInstance(instanceID);
        this.startInstance(instanceID);
        logger.debug("[" + instanceID + "] vApp restarted");
    }

    public void templateFromInstance(Map<String, String> arguments, String instanceID, String templateName)
            throws Exception {
        logger.info("[" + instanceID + "] vApp templating...");
        checkConnection();
        // get the vdc
        String vdcName = "COMMUN-P5"; //arguments.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.VDC_NAME);
        Vdc vdc = Vdc.getVdcByReference(vCloudClient, org.getVdcRefsByName().get(vdcName));
        // capture vapp params
        Vapp vapp = Vapp.getVappById(vCloudClient, instanceID);
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
        logger.info("[" + instanceID + "] vApp template done under '" + templateName + "'");
    }

    public void snapshotInstance(String instanceID, String name, String description, boolean memory,
            boolean quiesce) throws Exception {
        logger.info("[" + instanceID + "] vApp snapshoting to '" + name + "'...");
        checkConnection();
        Vapp.getVappById(vCloudClient, instanceID).createSnapshot(name, description, memory, quiesce)
                .waitForTask(0);
        logger.debug("[" + instanceID + "] vApp snapshot done under '" + name + "'");
    }

    public void removeSnapshotInstance(String instanceID) throws Exception {
        logger.info("[" + instanceID + "] Removing all vApp snapshots");
        checkConnection();
        Vapp.getVappById(vCloudClient, instanceID).removeAllSnapshots().waitForTask(0);
        logger.debug("[" + instanceID + "] Removed all vApp snapshots");
    }

    public void revertSnapshotInstance(String instanceID) throws Exception {
        logger.info("[" + instanceID + "] Reverting vApp to snapshot");
        checkConnection();
        Vapp.getVappById(vCloudClient, instanceID).revertToCurrentSnapshot().waitForTask(0);
        logger.debug("[" + instanceID + "] vApp revert to snapshot");
    }

    public void addVMDisk(String instanceID, int sizeMB, String mountpoint) throws Exception {
        logger.info("[" + instanceID + "] add a disk to the VM...");
        checkConnection();
        Vapp vapp = Vapp.getVappById(vCloudClient, instanceID);
        VM vm = vapp.getChildrenVms().get(0);
        VirtualDisk vDisk = new VirtualDisk(BigInteger.valueOf(sizeMB), BusType.SCSI, BusSubType.LSI_LOGIC);
        List<VirtualDisk> vDisks = vm.getDisks();
        vDisks.add(vDisk);
        vm.updateDisks(vDisks).waitForTask(0);
        logger.debug("[" + instanceID + "] Disk added to the VM'");
    }

    public boolean isInstanceStarted(String instanceID) throws Exception {
        checkConnection();
        return Vapp.getVappById(vCloudClient, instanceID).isDeployed();
    }

    @Override
    public String getName() {
        return "VMWare vCloud";
    }

    public IaasInstance getIaasInstance(String instanceID) {
        return iaasInstances.get(instanceID);
    }

    public void attachAdditionalVirtualDisk(String instanceID, long diskSize, String busType,
            String busSubType) throws Exception {
        VirtualDisk additionalDisk = new VirtualDisk(BigInteger.valueOf(diskSize), BusType.valueOf(busType),
            BusSubType.valueOf(busSubType));
        attachAdditionalVirtualDisk(instanceID, additionalDisk);
    }

    public void attachAdditionalVirtualDisk(String instanceID, long diskSize, String busType,
            String busSubType, int busNumber, int unitNumber) throws Exception {
        VirtualDisk additionalDisk = new VirtualDisk(BigInteger.valueOf(diskSize), BusType.valueOf(busType),
            BusSubType.valueOf(busSubType), busNumber, unitNumber);
        attachAdditionalVirtualDisk(instanceID, additionalDisk);
    }

    private void attachAdditionalVirtualDisk(String instanceID, VirtualDisk additionalDisk) throws Exception {
        checkConnection();
        VM vm = Vapp.getVappById(vCloudClient, instanceID).getChildrenVms().get(0);
        List<VirtualDisk> disks = VM.getDisks(vCloudClient, vm.getReference());
        disks.add(additionalDisk);
        try {
            vm.updateDisks(disks).waitForTask(0);
        } catch (VCloudException e) {
            logger.error(
                    String.format(
                            "An error occurred while attaching an additional disk to the virtual machine (instance-id:%s) :%n",
                            instanceID), e);
            throw new Exception(e);
        }
    }

    public VcloudClient getVCloud() {
        return vCloudClient;
    }

    public String cloneVapp(String vdcName, String vappTemplateId, String vappName) throws Exception {
        Vdc vdc = Vdc.getVdcByReference(vCloudClient, org.getVdcRefByName(vdcName));
        Vapp originVapp = Vapp.getVappById(vCloudClient, vappTemplateId);

        CloneVAppParamsType cloneVappParamsType = new CloneVAppParamsType();
        cloneVappParamsType.setName(vappName);
        cloneVappParamsType.setSource(originVapp.getReference());
        Vapp clonedVapp = vdc.cloneVapp(cloneVappParamsType);
        List<Task> tasks = clonedVapp.getTasks();
        if (tasks.size() > 0) {
            tasks.get(0).waitForTask(0);
        }
        return clonedVapp.getResource().getId();
    }

    public String copy(String vdcName, String vappName, String vmTemplateName, String newVmName)
            throws Exception {
        Vapp vapp = findVappByName(vdcName, vappName);

        VM template = findVmByName(vapp, vmTemplateName);

        ReferenceType vappTemplateRef = new ReferenceType();
        vappTemplateRef.setName(newVmName);
        vappTemplateRef.setHref(template.getResource().getHref());

        SourcedCompositionItemParamType vmItem = new SourcedCompositionItemParamType();
        vmItem.setSource(vappTemplateRef);

        NetworkConnectionSectionType networkConnectionSectionType = configureStaticPoolNetwork(vapp);

        InstantiationParamsType vmInstantiationParamsType = new InstantiationParamsType();
        List<JAXBElement<? extends SectionType>> vmSections = vmInstantiationParamsType.getSection();
        vmSections.add(new ObjectFactory().createNetworkConnectionSection(networkConnectionSectionType));
        vmItem.setInstantiationParams(vmInstantiationParamsType);

        RecomposeVAppParamsType recomposeVAppParamsType = new RecomposeVAppParamsType();
        recomposeVAppParamsType.getSourcedItem().add(vmItem);
        vapp.recomposeVapp(recomposeVAppParamsType).waitForTask(0);

        List<Task> reTasks = vapp.getTasks();
        if (reTasks.size() > 0)
            reTasks.get(0).waitForTask(0);

        // reload
        vapp = findVappByName(vdcName, vappName);
        return findVmByName(vapp, newVmName).getResource().getId();
    }

    private Vapp findVappByName(String vdcName, String vappName) throws VCloudException {
        Vdc vdc = Vdc.getVdcByReference(vCloudClient, org.getVdcRefsByName().get(vdcName));
        ReferenceType vapp = vdc.getVappRefByName(vappName);
        return Vapp.getVappByReference(vCloudClient, vapp);
    }

    private NetworkConnectionSectionType configureStaticPoolNetwork(Vapp vapp) throws VCloudException {
        NetworkConnectionSectionType networkConnectionSectionType = new NetworkConnectionSectionType();
        MsgType networkInfo = new MsgType();
        networkConnectionSectionType.setInfo(networkInfo);

        NetworkConnectionType networkConnectionType = new NetworkConnectionType();
        networkConnectionType.setNetwork(vapp.getNetworkConfigSection().getNetworkConfig().get(0)
                .getNetworkName());
        networkConnectionType.setIpAddressAllocationMode(IpAddressAllocationModeType.POOL.value());
        networkConnectionType.setIsConnected(true);
        networkConnectionSectionType.getNetworkConnection().add(networkConnectionType);
        return networkConnectionSectionType;
    }

    private VM findVmByName(Vapp vapp, String vmTemplateName) throws VCloudException {
        for (VM vm : vapp.getChildrenVms()) {
            if (vm.getResource().getName().equals(vmTemplateName)) {
                return vm;
            }
        }
        return null;
    }

    public void customize(String vappId, String customizationScript) throws Exception {
        Vapp vapp = Vapp.getVappById(vCloudClient, vappId);
        for (VM vm : vapp.getChildrenVms()) {
            if (vm.isDeployed()) {
                vm.undeploy(UndeployPowerActionType.SHUTDOWN).waitForTask(0);
            }
            GuestCustomizationSectionType guestCustomizationSection = vm.getGuestCustomizationSection();
            guestCustomizationSection.setEnabled(true);
            guestCustomizationSection.setCustomizationScript(customizationScript);
            vm.updateSection(guestCustomizationSection).waitForTask(0);
            vm.deploy(true, 0, true).waitForTask(0);
        }
    }

    public class VCloudAPIConstants {
        public class ApiParameters {
            public static final String API_URL = "apiurl";
            public static final String USER_NAME = "username";
            public static final String PASSWORD = "password";
            public static final String ORGANIZATION_NAME = "organizationName";
        }

        public class InstanceParameters {
            public static final String INSTANCE_NAME = "instanceName";
            public static final String INSTANCE_ID = "instanceID";
            public static final String TEMPLATE_NAME = "templateName";
            public static final String VDC_NAME = "vdcName";
            public static final String RM_URL = "rm.url";
            public static final String RM_CRED_VAL = "rm.cred.val";
            public static final String CORES = "cores";
            public static final String MEMORY = "memory";
            public static final String STORAGE = "storage";
            public static final String PASSWORD = "vm.password";
        }

        public class MonitoringParameters {
            public static final String URL = "vim.service.url";
            public static final String USERNAME = "vim.service.username";
            public static final String PASSWORD = "vim.service.password";
        }

        public class VirtualDiskParameters {
            public static final String BUS_TYPE_SCSI = "SCSI";
            public static final String BUS_TYPE_IDE = "IDE";

            public static final String BUS_SUBTYPE_BUS_LOGIC = "BUS_LOGIC";
            public static final String BUS_SUBTYPE_LSI_LOGIC = "LSI_LOGIC";
            public static final String BUS_SUBTYPE_LSI_LOGIC_SAS = "LSI_LOGIC_SAS";
            public static final String BUS_SUBTYPE_PARA_VIRTUAL = "PARA_VIRTUAL";
        }
    }

    private VappTemplate getVAppTemplate(String vappTemplateName) throws VCloudException {
        logger.debug("Searching vApp Template named : " + vappTemplateName);
        Iterator<ReferenceType> itOrg = vCloudClient.getOrgRefs().iterator();
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
        Organization org = Organization.getOrganizationByReference(vCloudClient, orgRef);
        Iterator<ReferenceType> itVdc = org.getVdcRefs().iterator();
        while (itVdc.hasNext()) {
            Vdc vdc = Vdc.getVdcByReference(vCloudClient, itVdc.next());
            Iterator<ReferenceType> itTpl = vdc.getVappTemplateRefs().iterator();
            while (itTpl.hasNext()) {
                ReferenceType vappTemplateRef = itTpl.next();
                if (vappTemplateRef.getName().equals(vappTemplateName)) {
                    logger.debug("vApp Template Found : " + vappTemplateRef.getName() + " [" +
                        org.getReference().getName() + "/" + vdc.getReference().getName() + "]");
                    return VappTemplate.getVappTemplateByReference(vCloudClient, vappTemplateRef);
                }
            }
        }
        return null;
    }

    private NetworkConfigSectionType buildNetworkConfigSection(Vdc vdc, String vmId, String networkName,
            List<NatRule> natRules, List<FirewallRule> firewallRules) {
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

        for (NatRule rule : natRules) {
            addNatRule(natServiceType.getNatRule(), rule, vmId);
        }
        //        addNatRule(natServiceType.getNatRule(), "SSH", "TCP", 22, 22, vmId);
        //        addNatRule(natServiceType.getNatRule(), "RDP", "TCP", 3389, 3389, vmId);

        JAXBElement<NetworkServiceType> networkServiceType = new ObjectFactory()
                .createNetworkService(natServiceType);
        networkService.add(networkServiceType);

        // Setup Firewall
        logger.debug("[" + vmId + "] Setup Firewall");
        FirewallServiceType firewallServiceType = new FirewallServiceType();
        firewallServiceType.setIsEnabled(true);
        firewallServiceType.setDefaultAction(FirewallPolicyType.DROP.value());
        firewallServiceType.setLogDefaultAction(false);

        for (FirewallRule rule : firewallRules) {
            addFirewallRule(firewallServiceType.getFirewallRule(), rule);
        }
        //        List<FirewallRuleType> fwRules = firewallServiceType.getFirewallRule();
        //        addFirewallRule(fwRules, "PING", "ICMP", "Any", "Any", "Any");
        //        addFirewallRule(fwRules, "SSH", "TCP", "Any", "Any", "22");
        //        addFirewallRule(fwRules, "RDP", "TCP", "Any", "Any", "3389");
        //        addFirewallRule(fwRules, "In-Out", "ANY", "internal", "external", "Any");

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

    private void addNatRule(List<NatRuleType> natRules, NatRule rule, String vmId) {
        addNatRule(natRules, rule.getName(), rule.getProtocol(), rule.getIntPort(), rule.getExtPort(), vmId);
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

    private void addFirewallRule(List<FirewallRuleType> fwRules, FirewallRule rule) {
        addFirewallRule(fwRules, rule.getName(), rule.getProtocol(), rule.getSrcIp(), rule.getDstIp(),
                rule.getPortRange());
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

    @Override
    public void disconnect() throws Exception {
        //        vCloudClient.logout();
    }

    private void initializeMonitoringService(String url, String username, String password) {
        try {
            vimServiceClient = new VimServiceClient();
            vimServiceClient.initialize(url, username, password);
        } catch (Exception e) {
            logger.error("Cannot initialize the VimSerivceClient instance: " + e);
            throw new RuntimeException(e);
        }

    }

    private Map<String, Object> convert(Map<String, String> a) {
        Map<String, Object> r = new HashMap<String, Object>();
        r.putAll(a);
        return r;
    }

    @Override
    public String[] getHosts() throws IaasMonitoringException {
        try {
            return vimServiceClient.getHosts();
        } catch (ViServiceClientException e) {
            throw new IaasMonitoringException(e);
        }
    }

    @Override
    public String[] getVMs() throws IaasMonitoringException {
        try {
            return vimServiceClient.getVMs();
        } catch (ViServiceClientException e) {
            throw new IaasMonitoringException(e);
        }
    }

    @Override
    public String[] getVMs(String hostId) throws IaasMonitoringException {
        try {
            return vimServiceClient.getVMs(hostId);
        } catch (ViServiceClientException e) {
            throw new IaasMonitoringException(e);
        }
    }

    @Override
    public Map<String, String> getHostProperties(String hostId) throws IaasMonitoringException {
        try {
            return vimServiceClient.getHostProperties(hostId);
        } catch (ViServiceClientException e) {
            throw new IaasMonitoringException(e);
        }
    }

    @Override
    public Map<String, String> getVMProperties(String vmId) throws IaasMonitoringException {
        try {
            return vimServiceClient.getVMProperties(vmId);
        } catch (ViServiceClientException e) {
            throw new IaasMonitoringException(e);
        }
    }

    @Override
    public Map<String, Object> getVendorDetails() throws IaasMonitoringException {
        try {
            return vimServiceClient.getVendorDetails();
        } catch (ViServiceClientException e) {
            throw new IaasMonitoringException(e);
        }
    }

    @Override
    public void stopInstance(IaasInstance instance) throws Exception {
        stopInstance(instance.getInstanceId());
    }

    @Override
    public boolean isInstanceStarted(IaasInstance instance) throws Exception {
        return isInstanceStarted(instance.getInstanceId());
    }
}
