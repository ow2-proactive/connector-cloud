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

import static org.ow2.proactive.iaas.vcloud.monitoring.VimServiceConstants.DYNAMIC_PROPERTIES;
import static org.ow2.proactive.iaas.vcloud.monitoring.VimServiceConstants.HOST_STATIC_PROPERTIES;
import static org.ow2.proactive.iaas.vcloud.monitoring.VimServiceConstants.VM_STATIC_PROPERTIES;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.xml.ws.BindingProvider;

import org.apache.log4j.Logger;

import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;
import com.vmware.vim25.VimService;

public class VimServiceClient {

	private static final Logger logger = Logger
			.getLogger(VimServiceClient.class);

	/** 20 minutes session renewal interval. */
	private static final long LOGIN_INTERVAL = TimeUnit.MINUTES.toMillis(20);

	private final ManagedObjectReference mobjRef = new ManagedObjectReference();
	private final String mobjName = "ServiceInstance";
	private VimService vimService;
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

			mobjRef.setType(mobjName);
			mobjRef.setValue(mobjName);

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
				vimService = new VimService();
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

	private void disconnet() throws ViServiceClientException {
		if (isConnected) {
			try {
				vimPort.logout(serviceContent.getSessionManager());
			} catch (RuntimeFaultFaultMsg e) {
				logger.error("Cannot logout:", e);
				throw new ViServiceClientException(e);
			}
		}
		isConnected = false;
	}

	/*
	 * At the first attempt after 'LOGIN_INTERVAL' period, we explicitly
	 * disconnect and the reconnect to ensure that we have a valid VimService
	 * session.
	 */
	private void ensureConnected() throws ViServiceClientException {
		if (System.currentTimeMillis() > nextLoginTime) {
			synchronized (this) {
				if (System.currentTimeMillis() > nextLoginTime) {
					disconnet();
					connect();
				}
			}
		}
	}

	public String[] getHosts() throws ViServiceClientException {
		return getmObjIds("HostSystem", getRootFolder());
	}

	public String[] getVMs() throws ViServiceClientException {
		return getmObjIds("VirtualMachine", getRootFolder());
	}

	public String[] getVMs(String hostId) throws ViServiceClientException {
		return getmObjIds("VirtualMachine",
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
			VimServiceUtil.updateKeys(propMap);
			return propMap;
		} catch (Exception error) {
			logger.error("Cannot retrieve host properties: " + hostId, error);
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
			VimServiceUtil.updateKeys(propMap);
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

	public void close() throws ViServiceClientException {
		if (isConnected) {
			try {
				vimPort.logout(serviceContent.getSessionManager());
			} catch (RuntimeFaultFaultMsg e) {
				logger.error("Cannot logout:", e);
				throw new ViServiceClientException(e);
			}
			isConnected = false;
		}
	}

	private void setNextLoginTime() {
		nextLoginTime = System.currentTimeMillis() + LOGIN_INTERVAL;
	}
}
