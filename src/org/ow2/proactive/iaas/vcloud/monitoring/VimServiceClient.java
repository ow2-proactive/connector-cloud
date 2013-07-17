/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2012 INRIA/University of
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
 * ################################################################
 * %$ACTIVEEON_INITIAL_DEV$
 */

package org.ow2.proactive.iaas.vcloud.monitoring;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.BindingProvider;

import com.vmware.vim25.ArrayOfManagedObjectReference;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.Event;
import com.vmware.vim25.EventFilterSpec;
import com.vmware.vim25.EventFilterSpecByTime;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VimService;
import org.apache.log4j.Logger;

import static org.ow2.proactive.iaas.vcloud.monitoring.VimServiceConstants.DYNAMIC_PROPERTIES;
import static org.ow2.proactive.iaas.vcloud.monitoring.VimServiceConstants.HOST_STATIC_PROPERTIES;
import static org.ow2.proactive.iaas.vcloud.monitoring.VimServiceConstants.VM_STATIC_PROPERTIES;

public class VimServiceClient {

    private static final Logger logger = Logger
            .getLogger(VimServiceClient.class);

    /** 20 minutes session renewal interval. */
    private static final long LOGIN_INTERVAL = TimeUnit.MINUTES.toMillis(20);

    private static final String MOBJ_NAME = "ServiceInstance";
    private final ManagedObjectReference mobjRef = new ManagedObjectReference();
    private VimPortType vimPort;
    private ServiceContent serviceContent;

    private String vimServiceUrl;
    private String username;
    private String password;

    private volatile long nextLoginTime = -1;

    private boolean isConnected = false;

    public void initialize(String url, String username, String password)
            throws ViServiceClientException {
        this.vimServiceUrl = url;
        this.username = username;
        this.password = password;
        try {

            VimServiceUtil.disableHttpsCertificateVerification();
            VimServiceUtil.disableHostNameVarifier();

            mobjRef.setType(MOBJ_NAME);
            mobjRef.setValue(MOBJ_NAME);

            ensureConnected();
        } catch (Exception error) {
            logger.error("Cannot initialize VCenterServiceClient instance:",
                    error);
            throw new ViServiceClientException(error);
        }
    }

    private void connect() throws ViServiceClientException {
        if (!isConnected) {
            try {
                VimService vimService = new VimService();
                vimPort = vimService.getVimPort();

                Map<String, Object> reqCtx = ((BindingProvider) vimPort)
                        .getRequestContext();
                reqCtx.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
                        vimServiceUrl);
                reqCtx.put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);

                serviceContent = vimPort.retrieveServiceContent(mobjRef);
                vimPort.login(serviceContent.getSessionManager(), username,
                        password, null);
                setNextLoginTime();
            } catch (Exception error) {
                logger.error(
                        "Cannot initialize VCenterServiceClient instance:",
                        error);
                throw new ViServiceClientException(error);
            }
        }
        isConnected = true;
    }

    /*
     * At the first attempt after 'LOGIN_INTERVAL' period, we explicitly
     * disconnect and the reconnect to ensure that we have a valid VimService
     * session.
     */
    private void ensureConnected() throws ViServiceClientException {
        if (System.currentTimeMillis() > nextLoginTime) {
            logger.debug("Ensuring connection with server.");
            synchronized (this) {
                try {
                    disconnect(); // disconnect
                } catch (ViServiceClientException e) {
                    logger.error("Ignoring exception while disconnecting: ", e);
                }
                connect();
            }
        }
    }

    public Map<String, Object> getVendorDetails()
            throws ViServiceClientException {
        ensureConnected();

        Map<String, Object> output = new HashMap<String, Object>();

        Map<String, Object> hierarchy = getClusterComputeResources();
        output.put("ClusterComputeResource", hierarchy);

        return output;
    }

    private Map<String, Object> getClusterComputeResources()
            throws ViServiceClientException {
        Map<String, Object> output = new HashMap<String, Object>();

        String[] domains = getmObjIds("ClusterComputeResource", getRootFolder());

        for (String domain : domains) {
            ManagedObjectReference domainMor = VimServiceUtil
                    .getEntityByTypeAndId("ClusterComputeResource", domain);

            try {
                Map<String, Object> propsOfDomain = VimServiceUtil
                        .getRawStaticProperties(domainMor, new String[] {
                                VimServiceConstants.PROP_NAME,
                                VimServiceConstants.PROP_CCR_HOST },
                                serviceContent, vimPort);

                Map<String, Object> domainMap = new HashMap<String, Object>();

                DynamicProperty hostProps = (DynamicProperty) propsOfDomain
                        .get(VimServiceConstants.PROP_CCR_HOST);
                DynamicProperty domainName = (DynamicProperty) propsOfDomain
                        .get(VimServiceConstants.PROP_NAME);

                ArrayOfManagedObjectReference hosts = (ArrayOfManagedObjectReference) hostProps
                        .getVal();
                List<ManagedObjectReference> hostsListMor = hosts
                        .getManagedObjectReference();

                domainMap.put("name", domainName.getVal());

                for (ManagedObjectReference hostMor : hostsListMor) {
                    Map<String, Object> hostMap = new HashMap<String, Object>();

                    Map<String, Object> propsOfHost = VimServiceUtil
                            .getRawStaticProperties(
                                    hostMor,
                                    new String[] {
                                            VimServiceConstants.PROP_NAME,
                                            VimServiceConstants.PROP_HOST_SYSTEM_IDENTIFICATION },
                                    serviceContent, vimPort);
                    DynamicProperty hostName = (DynamicProperty) propsOfHost
                            .get(VimServiceConstants.PROP_NAME);
                    DynamicProperty hostIds = (DynamicProperty) propsOfHost
                            .get(VimServiceConstants.PROP_HOST_SYSTEM_IDENTIFICATION);

                    VimServiceUtil.resloveAndAddDynamicPropertyToMap(hostName,
                            hostMap, serviceContent, vimPort,
                            VimServiceUtil.VIM25_HOST_TYPE);
                    VimServiceUtil.resloveAndAddDynamicPropertyToMap(hostIds,
                            hostMap, serviceContent, vimPort,
                            VimServiceUtil.VIM25_HOST_TYPE);

                    hostMap.put("id", hostMor.getValue());

                    domainMap.put(hostMor.getValue(), hostMap);
                }
                output.put(domain, domainMap);
            } catch (Exception e) {
                logger.warn("Could not get list of hosts for domain: " + domain);
            }
        }
        return output;
    }

    public String[] getHosts() throws ViServiceClientException {
        ensureConnected();
        return getmObjIds(VimServiceUtil.VIM25_HOST_TYPE, getRootFolder());
    }

    public String[] getVMs() throws ViServiceClientException {
        ensureConnected();
        return getmObjIds(VimServiceUtil.VIM25_VM_TYPE, getRootFolder());
    }

    public String[] getVMs(String hostId) throws ViServiceClientException {
        ensureConnected();
        return getmObjIds(VimServiceUtil.VIM25_VM_TYPE,
                VimServiceUtil.getmObjRefByHostId(hostId));
    }

    public Map<String, String> getHostProperties(String hostId)
            throws ViServiceClientException {
        ensureConnected();
        try {
            Map<String, String> propMap = new HashMap<String, String>();
            propMap.putAll(VimServiceUtil.getHostStaticProperties(hostId,
                    HOST_STATIC_PROPERTIES, serviceContent, vimPort));
            propMap.putAll(VimServiceUtil.getHostDynamicProperties(hostId,
                    DYNAMIC_PROPERTIES, serviceContent, vimPort));
            VimServicePropertyUtil.HOST.standardize(propMap);
            return propMap;
        } catch (Exception error) {
            logger.error("Cannot retrieve host properties: " + hostId, error);
            throw new ViServiceClientException(error);
        }
    }

    public Map<String, String> getHostStaticProperties(String hostId,
            String[] properties) throws ViServiceClientException {
        ensureConnected();
        try {
            return VimServiceUtil.getHostStaticProperties(hostId, properties,
                    serviceContent, vimPort);
        } catch (Exception error) {
            logger.error(
                    String.format(
                            "Cannot retrieve static host properties. %nhost-id: %s %nproperties: %s",
                            hostId, Arrays.asList(properties).toString()),
                    error);
            throw new ViServiceClientException(error);
        }

    }

    public Map<String, String> getVMProperties(String vmId)
            throws ViServiceClientException {
        ensureConnected();
        try {
            Map<String, String> propMap = new HashMap<String, String>();
            propMap.putAll(VimServiceUtil.getVMStaticProperties(vmId,
                    VM_STATIC_PROPERTIES, serviceContent, vimPort));
            propMap.putAll(VimServiceUtil.getVmDynamicProperties(vmId,
                    DYNAMIC_PROPERTIES, serviceContent, vimPort));
            VimServicePropertyUtil.VM.standardize(propMap);
            return propMap;
        } catch (Exception error) {
            logger.error("Cannot retrieve vm properties: " + vmId, error);
            throw new ViServiceClientException(error);
        }
    }

    private ManagedObjectReference getRootFolder() {
        return serviceContent.getRootFolder();
    }

    private String[] getmObjIds(String nodeType,
            ManagedObjectReference container) throws ViServiceClientException {
        ensureConnected();
        try {
            List<ManagedObjectReference> nodes = VimServiceUtil
                    .getmObjRefsInContainerByType(nodeType, container,
                            serviceContent, vimPort);
            String[] nodeIds = new String[nodes.size()];
            for (int i = 0; i < nodeIds.length; i++) {
                nodeIds[i] = nodes.get(i).getValue();
            }
            return nodeIds;
        } catch (Exception error) {
            logger.error("Cannot get the list of " + nodeType, error);
            throw new ViServiceClientException(error);
        }
    }

    public void disconnect() throws ViServiceClientException {
        if (isConnected) {
            try {
                vimPort.logout(serviceContent.getSessionManager());
            } catch (RuntimeFaultFaultMsg e) {
                logger.error("Cannot logout:", e);
                throw new ViServiceClientException(e);
            } finally {
                isConnected = false;
            }
        }
        isConnected = false;
    }

    public ManagedObjectReference createEventCollectorFromNow(
            EventFilterSpec eventFilter) throws ViServiceClientException {
        try {
            filterEventsStartingFromNow(eventFilter);
            return vimPort.createCollectorForEvents(serviceContent.getEventManager(), eventFilter);
        } catch (Exception e) {
            logger.error("Cannot create collector:", e);
            throw new ViServiceClientException(e);
        }
    }

    private void filterEventsStartingFromNow(EventFilterSpec eventFilter) throws RuntimeFaultFaultMsg {
        EventFilterSpecByTime eventFilterSpecByTime = new EventFilterSpecByTime();
        // retrieve current server time (might be different from the client time)
        XMLGregorianCalendar now = vimPort.currentTime(mobjRef);
        eventFilterSpecByTime.setBeginTime(now);
        eventFilter.setTime(eventFilterSpecByTime);
    }

    public List<Event> readNextEvents(ManagedObjectReference eventCollector,
            int pagingSize) throws ViServiceClientException {
        try {
            return vimPort.readNextEvents(eventCollector, pagingSize);
        } catch (RuntimeFaultFaultMsg e) {
            logger.error("Cannot read next events:", e);
            throw new ViServiceClientException(e);
        }
    }

    private void setNextLoginTime() {
        nextLoginTime = System.currentTimeMillis() + LOGIN_INTERVAL;
    }
}
