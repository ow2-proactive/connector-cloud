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

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.TrustManager;

import com.vmware.vim25.ArrayOfPerfCounterInfo;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.InvalidPropertyFaultMsg;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ObjectContent;
import com.vmware.vim25.ObjectSpec;
import com.vmware.vim25.PerfCounterInfo;
import com.vmware.vim25.PerfEntityMetric;
import com.vmware.vim25.PerfEntityMetricBase;
import com.vmware.vim25.PerfMetricId;
import com.vmware.vim25.PerfMetricIntSeries;
import com.vmware.vim25.PerfMetricSeries;
import com.vmware.vim25.PerfQuerySpec;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.VimPortType;

public class VimServiceUtil {

	public static void disableHttpsCertificateVerification()
			throws NoSuchAlgorithmException, KeyManagementException {
		TrustManager[] tms = new TrustManager[] { new RelaxedTrustManager() };
		SSLContext sslContext = SSLContext.getInstance("SSL");
		SSLSessionContext sslServerSessionContext = sslContext
				.getServerSessionContext();
		sslServerSessionContext.setSessionTimeout(0);
		sslContext.init(null, tms, null);
		HttpsURLConnection.setDefaultSSLSocketFactory(sslContext
				.getSocketFactory());
	}

	public static void disableHostNameVarifier() {
		HttpsURLConnection
				.setDefaultHostnameVerifier(new RelaxedHostNameVerifier());
	}

	public static List<ManagedObjectReference> getmObjRefsInContainerByType(
			String mObjRefType, ManagedObjectReference container,
			ServiceContent serviceContent, VimPortType vimPortType)
			throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {

		ManagedObjectReference viewController = serviceContent.getViewManager();
		ManagedObjectReference containerView = vimPortType.createContainerView(
				viewController, container, Arrays.asList(mObjRefType), true);

		PropertySpec propertySpec = new PropertySpec();
		propertySpec.setAll(Boolean.FALSE);
		propertySpec.setType(mObjRefType);

		TraversalSpec traversalSpec = new TraversalSpec();
		traversalSpec.setName("view");
		traversalSpec.setPath("view");
		traversalSpec.setSkip(false);
		traversalSpec.setType("ContainerView");

		ObjectSpec objectSpec = new ObjectSpec();
		objectSpec.setObj(containerView);
		objectSpec.setSkip(Boolean.TRUE);
		objectSpec.getSelectSet().add(traversalSpec);

		PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
		propertyFilterSpec.getPropSet().add(propertySpec);
		propertyFilterSpec.getObjectSet().add(objectSpec);

		List<ObjectContent> objCtnt = vimPortType.retrieveProperties(
				serviceContent.getPropertyCollector(),
				Arrays.asList(propertyFilterSpec));

		List<ManagedObjectReference> result = new ArrayList<ManagedObjectReference>();
		for (ObjectContent oc : objCtnt) {
			result.add(oc.getObj());
		}
		return result;
	}

	public static Map<ManagedObjectReference, Map<String, String>> getmObjRefStaticProperties(
			String mObjRefType, String[] properties,
			ManagedObjectReference container, ServiceContent serviceContent,
			VimPortType vimPortType) throws InvalidPropertyFaultMsg,
			RuntimeFaultFaultMsg {

		ManagedObjectReference viewController = serviceContent.getViewManager();
		ManagedObjectReference containerView = vimPortType.createContainerView(
				viewController, container, Arrays.asList(mObjRefType), true);

		PropertySpec propertySpec = new PropertySpec();
		propertySpec.setAll(Boolean.FALSE);
		propertySpec.setType(mObjRefType);
		propertySpec.getPathSet().addAll(Arrays.asList(properties));

		TraversalSpec traversalSpec = new TraversalSpec();
		traversalSpec.setName("view");
		traversalSpec.setPath("view");
		traversalSpec.setSkip(false);
		traversalSpec.setType("ContainerView");

		ObjectSpec objectSpec = new ObjectSpec();
		objectSpec.setObj(containerView);
		objectSpec.setSkip(Boolean.TRUE);
		objectSpec.getSelectSet().add(traversalSpec);

		PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
		propertyFilterSpec.getPropSet().add(propertySpec);
		propertyFilterSpec.getObjectSet().add(objectSpec);

		List<ObjectContent> objContent = vimPortType.retrieveProperties(
				serviceContent.getPropertyCollector(),
				Arrays.asList(propertyFilterSpec));

		Map<ManagedObjectReference, Map<String, String>> retPropMap = new HashMap<ManagedObjectReference, Map<String, String>>();
		for (ObjectContent oc : objContent) {
			Map<String, String> propMap = new HashMap<String, String>();
			List<DynamicProperty> propSet = oc.getPropSet();
			if (propSet != null) {
				for (DynamicProperty dp : propSet) {
					propMap.put(dp.getName(), dp.getVal().toString());
				}
			}
			retPropMap.put(oc.getObj(), propMap);
		}
		return retPropMap;
	}

	public static ObjectContent[] getObjectProperties(
			ManagedObjectReference mobj, String[] properties,
			ServiceContent serviceContent, VimPortType vimPort)
			throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
		PropertyFilterSpec spec = new PropertyFilterSpec();
		// property spec
		PropertySpec propSpec = new PropertySpec();
		propSpec.setAll(properties == null || properties.length == 0);
		propSpec.setType(mobj.getType());
		// FIXME:
		propSpec.getPathSet().addAll(Arrays.asList(properties));
		spec.getPropSet().add(propSpec);
		// object spec
		ObjectSpec objSpec = new ObjectSpec();
		spec.getObjectSet().add(objSpec);
		spec.getObjectSet().get(0).setObj(mobj);
		spec.getObjectSet().get(0).setSkip(Boolean.FALSE);
		List<PropertyFilterSpec> listpfs = new ArrayList<PropertyFilterSpec>(1);
		listpfs.add(spec);
		List<ObjectContent> listobjcont = vimPort.retrieveProperties(
				serviceContent.getPropertyCollector(), listpfs);
		// List<ObjectContent> listobjcont =
		// retrievePropertiesAllObjects(listpfs,
		// serviceContent, vimPort);
		return listobjcont.toArray(new ObjectContent[listobjcont.size()]);
	}

	public static List<ObjectContent> retrievePropertiesAllObjects(
			List<PropertyFilterSpec> listpfs, ServiceContent serviceContent,
			VimPortType vimPort) throws InvalidPropertyFaultMsg,
			RuntimeFaultFaultMsg {

		RetrieveOptions propObjectRetrieveOpts = new RetrieveOptions();
		List<ObjectContent> listobjcontent = new ArrayList<ObjectContent>();

		RetrieveResult rslts = vimPort.retrievePropertiesEx(
				serviceContent.getPropertyCollector(), listpfs,
				propObjectRetrieveOpts);
		if (rslts != null && rslts.getObjects() != null
				&& !rslts.getObjects().isEmpty()) {
			listobjcontent.addAll(rslts.getObjects());
		}
		String token = null;
		if (rslts != null && rslts.getToken() != null) {
			token = rslts.getToken();
		}
		while (token != null && !token.isEmpty()) {
			rslts = vimPort.continueRetrievePropertiesEx(
					serviceContent.getPropertyCollector(), token);
			token = null;
			if (rslts != null) {
				token = rslts.getToken();
				if (rslts.getObjects() != null && !rslts.getObjects().isEmpty()) {
					listobjcontent.addAll(rslts.getObjects());
				}
			}
		}

		return listobjcontent;
	}

	public static ManagedObjectReference getmObjRefByHostId(String hostId) {
		return getEntityByTypeAndId("HostSystem", hostId);
	}

	public static ManagedObjectReference getVMById(String vmId) {
		return getEntityByTypeAndId("VirtualMachine", vmId);
	}

	private static ManagedObjectReference getEntityByTypeAndId(String type,
			String id) {
		ManagedObjectReference ref = new ManagedObjectReference();
		ref.setType(type);
		ref.setValue(id);
		return ref;
	}

	public static Map<String, String> getHostStaticProperties(String hostId,
			String[] properties, ServiceContent serviceContent,
			VimPortType vimPort) throws InvalidPropertyFaultMsg,
			RuntimeFaultFaultMsg {

		return getStaticProperties(getmObjRefByHostId(hostId), properties,
				serviceContent, vimPort);
	}

	public static Map<String, String> getVMStaticProperties(String vmId,
			String[] properties, ServiceContent serviceContent,
			VimPortType vimPort) throws InvalidPropertyFaultMsg,
			RuntimeFaultFaultMsg {
		return getStaticProperties(getVMById(vmId), properties, serviceContent,
				vimPort);
	}

	public static Map<String, String> getStaticProperties(
			ManagedObjectReference mObjRef, String[] properties,
			ServiceContent serviceContent, VimPortType vimPort)
			throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
		ObjectContent[] oContents = VimServiceUtil.getObjectProperties(mObjRef,
				properties, serviceContent, vimPort);
		Map<String, String> propMap = new HashMap<String, String>();
		if (oContents != null) {
			for (ObjectContent oc : oContents) {
				List<DynamicProperty> dynamicPropertyList = oc.getPropSet();
				if (dynamicPropertyList != null) {
					for (DynamicProperty dp : dynamicPropertyList) {
						propMap.put(dp.getName(), dp.getVal().toString());
					}
				}
			}
		}
		return propMap;
	}

	public static Map<String, String> getHostDynamicProperties(String hostId,
			String[] properties, ServiceContent serviceContent,
			VimPortType vimPort) throws InvalidPropertyFaultMsg,
			RuntimeFaultFaultMsg {
		return getDynamicProperties(getmObjRefByHostId(hostId), properties,
				serviceContent, vimPort);
	}

	public static Map<String, String> getVmDynamicProperties(String vmId,
			String[] properties, ServiceContent serviceContent,
			VimPortType vimPort) throws InvalidPropertyFaultMsg,
			RuntimeFaultFaultMsg {
		ManagedObjectReference vmById = getVMById(vmId);
		return getDynamicProperties(vmById, properties, serviceContent, vimPort);
	}

	public static Map<String, String> getDynamicProperties(
			ManagedObjectReference mObjRef, String[] properties,
			ServiceContent serviceContent, VimPortType vimPort)
			throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
		// Create Property Spec
		PropertySpec propertySpec = new PropertySpec();
		propertySpec.setAll(Boolean.FALSE);
		propertySpec.getPathSet().add("perfCounter");
		propertySpec.setType("PerformanceManager");
		List<PropertySpec> propertySpecs = new ArrayList<PropertySpec>();
		propertySpecs.add(propertySpec);

		// Now create Object Spec
		ObjectSpec objectSpec = new ObjectSpec();
		objectSpec.setObj(serviceContent.getPerfManager());
		List<ObjectSpec> objectSpecs = new ArrayList<ObjectSpec>();
		objectSpecs.add(objectSpec);

		// Create PropertyFilterSpec using the PropertySpec and ObjectPec
		// created above.
		PropertyFilterSpec propertyFilterSpec = new PropertyFilterSpec();
		propertyFilterSpec.getPropSet().add(propertySpec);
		propertyFilterSpec.getObjectSet().add(objectSpec);

		List<PropertyFilterSpec> propertyFilterSpecs = new ArrayList<PropertyFilterSpec>();
		propertyFilterSpecs.add(propertyFilterSpec);

		List<ObjectContent> objContentList = retrievePropertiesAllObjects(
				propertyFilterSpecs, serviceContent, vimPort);

		List<PerfCounterInfo> perfCounterList = null;

		if (objContentList != null && !objContentList.isEmpty()) {
			ObjectContent objContent = objContentList.get(0);
			List<DynamicProperty> propSet = objContent.getPropSet();
			if (propSet != null && !propSet.isEmpty()) {
				DynamicProperty dp = propSet.get(0);
				perfCounterList = ((ArrayOfPerfCounterInfo) dp.getVal())
						.getPerfCounterInfo();
			}
		}

		List<String> propList = Arrays.asList(properties);
		Map<Integer, PerfCounterInfo> perfCounterInfoMap = new HashMap<Integer, PerfCounterInfo>();
		for (PerfCounterInfo pci : perfCounterList) {
			if (propList.contains(getKey(pci))) {
				perfCounterInfoMap.put(pci.getKey(), pci);
			}
		}

		List<PerfMetricId> availablePerfMetrics = vimPort
				.queryAvailablePerfMetric(serviceContent.getPerfManager(),
						mObjRef, null, null, new Integer(20));

		List<PerfMetricId> listPerfMatricId = new ArrayList<PerfMetricId>();
		for (PerfMetricId perfMatricId : availablePerfMetrics) {
			if (perfCounterInfoMap.containsKey(perfMatricId.getCounterId())) {
				listPerfMatricId.add(perfMatricId);
			}
		}

		PerfQuerySpec qSpec = new PerfQuerySpec();
		qSpec.setEntity(mObjRef);
		// TODO: Consider optimal parameters
		qSpec.setMaxSample(1);
		qSpec.getMetricId().addAll(listPerfMatricId);
		qSpec.setIntervalId(new Integer(20));
		List<PerfQuerySpec> qSpecs = new ArrayList<PerfQuerySpec>();
		qSpecs.add(qSpec);

		List<PerfEntityMetricBase> queryPerf = vimPort.queryPerf(
				serviceContent.getPerfManager(), qSpecs);
		Map<String, String> nodeProperties = new HashMap<String, String>();
		for (PerfEntityMetricBase perfEntityMetricBase : queryPerf) {
			PerfEntityMetric perfEntityMetric = (PerfEntityMetric) perfEntityMetricBase;
			List<PerfMetricSeries> perfMetricsSeriesList = perfEntityMetric
					.getValue();
			for (PerfMetricSeries perfMetricsSeries : perfMetricsSeriesList) {
				PerfCounterInfo perfCounterInfo = perfCounterInfoMap
						.get(perfMetricsSeries.getId().getCounterId());

				if (perfCounterInfo != null
						&& perfMetricsSeries instanceof PerfMetricIntSeries) {
					List<Long> value = ((PerfMetricIntSeries) perfMetricsSeries)
							.getValue();
					if (!value.isEmpty()) {
						nodeProperties.put(getKey(perfCounterInfo), value
								.get(0).toString());
					}

				}
			}
		}
		return nodeProperties;
	}

	private static String getKey(PerfCounterInfo pci) {
		return (new StringBuilder()).append(pci.getGroupInfo().getKey())
				.append('.').append(pci.getNameInfo().getKey()).append('.')
				.append(pci.getRollupType().name()).toString();
	}

	public static void updateKeys(Map<String, String> propertyMap) {
		replaceKeyIfPresent(VimServiceConstants.PROP_CPU_CORES, "cpu.cores",
				propertyMap);
		replaceKeyIfPresent(VimServiceConstants.PROP_CPU_FREQUENCY,
				"cpu.frequency", propertyMap);
		replaceKeyIfPresent(VimServiceConstants.PROP_CPU_USAGE, "cpu.usage",
				propertyMap);

		replaceKeyIfPresent(VimServiceConstants.PROP_MEMORY_TOTAL,
				"memory.total", propertyMap);
		replaceKeyIfPresent(VimServiceConstants.PROP_MEM_USAGE, "memory.usage",
				propertyMap);

		replaceKeyIfPresent(VimServiceConstants.PROP_NET_RX_RATE,
				"network.0.rx", propertyMap);
		replaceKeyIfPresent(VimServiceConstants.PROP_NET_TX_RATE,
				"network.0.tx", propertyMap);
		replaceKeyIfPresent(VimServiceConstants.PROP_NETWORK_COUNT,
				"network.count", propertyMap);
		
		//VM
		replaceKeyIfPresent(VimServiceConstants.PROP_VM_CPU_CORES, "cpu.cores",
				propertyMap);
		replaceKeyIfPresent(VimServiceConstants.PROP_VM_MEMEORY_TOTAL,
				"memory.total", propertyMap);
	}

	private static void replaceKeyIfPresent(String oldKey, String newKey,
			Map<String, String> propertyMap) {
		if (propertyMap.containsKey(oldKey)) {
			propertyMap.put(newKey, propertyMap.remove(oldKey));
		}
	}

	// non-instantiable
	private VimServiceUtil() {
	}

}
