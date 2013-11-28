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

import static org.ow2.proactive.iaas.vcloud.monitoring.VimServiceConstants.DS_STATIC_PROPERTIES;
import static org.ow2.proactive.iaas.vcloud.monitoring.VimServiceConstants.PROP_DS_CAPACITY;
import static org.ow2.proactive.iaas.vcloud.monitoring.VimServiceConstants.PROP_DS_FREE_SPACE;
import static org.ow2.proactive.iaas.vcloud.monitoring.VimServiceConstants.PROP_DS_TYPE;

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

import org.apache.log4j.Logger;

import com.vmware.vim25.ArrayOfGuestDiskInfo;
import com.vmware.vim25.ArrayOfGuestNicInfo;
import com.vmware.vim25.ArrayOfHostSystemIdentificationInfo;
import com.vmware.vim25.ArrayOfHostVirtualNic;
import com.vmware.vim25.ArrayOfManagedObjectReference;
import com.vmware.vim25.ArrayOfPerfCounterInfo;
import com.vmware.vim25.ArrayOfPhysicalNic;
import com.vmware.vim25.DynamicProperty;
import com.vmware.vim25.GuestDiskInfo;
import com.vmware.vim25.GuestNicInfo;
import com.vmware.vim25.HostSystemIdentificationInfo;
import com.vmware.vim25.HostVirtualNic;
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
import com.vmware.vim25.PhysicalNic;
import com.vmware.vim25.PropertyFilterSpec;
import com.vmware.vim25.PropertySpec;
import com.vmware.vim25.RetrieveOptions;
import com.vmware.vim25.RetrieveResult;
import com.vmware.vim25.RuntimeFaultFaultMsg;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.TraversalSpec;
import com.vmware.vim25.VimPortType;


public class VimServiceUtil {

    public static final String NO_HUMAN_READABLE_NAME_FROM_MOR_NAME = "<no-human-readable-name>";
    public static final String NO_ID_FROM_MOR_NAME = "<no-id>";
    public static final String VIM25_VM_TYPE = "VirtualMachine";
    public static final String VIM25_HOST_TYPE = "HostSystem";

    private static final Logger logger = Logger.getLogger(VimServiceUtil.class);

    public static void disableHttpsCertificateVerification() throws NoSuchAlgorithmException,
            KeyManagementException {
        TrustManager[] tms = new TrustManager[] { new RelaxedTrustManager() };
        SSLContext sslContext = SSLContext.getInstance("SSL");
        SSLSessionContext sslServerSessionContext = sslContext.getServerSessionContext();
        sslServerSessionContext.setSessionTimeout(0);
        sslContext.init(null, tms, null);
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
    }

    public static void disableHostNameVarifier() {
        HttpsURLConnection.setDefaultHostnameVerifier(new RelaxedHostNameVerifier());
    }

    public static List<ManagedObjectReference> getmObjRefsInContainerByType(String mObjRefType,
            ManagedObjectReference container, ServiceContent serviceContent, VimPortType vimPortType)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {

        ManagedObjectReference viewController = serviceContent.getViewManager();
        ManagedObjectReference containerView = vimPortType.createContainerView(viewController, container,
                Arrays.asList(mObjRefType), true);

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

        List<ObjectContent> objCtnt = vimPortType.retrieveProperties(serviceContent.getPropertyCollector(),
                Arrays.asList(propertyFilterSpec));

        List<ManagedObjectReference> result = new ArrayList<ManagedObjectReference>();
        for (ObjectContent oc : objCtnt) {
            result.add(oc.getObj());
        }
        return result;
    }

    public static Map<ManagedObjectReference, Map<String, String>> getmObjRefStaticProperties(
            String mObjRefType, String[] properties, ManagedObjectReference container,
            ServiceContent serviceContent, VimPortType vimPortType) throws InvalidPropertyFaultMsg,
            RuntimeFaultFaultMsg {

        ManagedObjectReference viewController = serviceContent.getViewManager();
        ManagedObjectReference containerView = vimPortType.createContainerView(viewController, container,
                Arrays.asList(mObjRefType), true);

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
                serviceContent.getPropertyCollector(), Arrays.asList(propertyFilterSpec));

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

    public static ObjectContent[] getObjectProperties(ManagedObjectReference mobj, String[] properties,
            ServiceContent serviceContent, VimPortType vimPort) throws InvalidPropertyFaultMsg,
            RuntimeFaultFaultMsg {
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
        List<ObjectContent> listobjcont = vimPort.retrieveProperties(serviceContent.getPropertyCollector(),
                listpfs);
        // List<ObjectContent> listobjcont =
        // retrievePropertiesAllObjects(listpfs,
        // serviceContent, vimPort);
        return listobjcont.toArray(new ObjectContent[listobjcont.size()]);
    }

    public static List<ObjectContent> retrievePropertiesAllObjects(List<PropertyFilterSpec> listpfs,
            ServiceContent serviceContent, VimPortType vimPort) throws InvalidPropertyFaultMsg,
            RuntimeFaultFaultMsg {

        RetrieveOptions propObjectRetrieveOpts = new RetrieveOptions();
        List<ObjectContent> listobjcontent = new ArrayList<ObjectContent>();

        RetrieveResult rslts = vimPort.retrievePropertiesEx(serviceContent.getPropertyCollector(), listpfs,
                propObjectRetrieveOpts);
        if (rslts != null && rslts.getObjects() != null && !rslts.getObjects().isEmpty()) {
            listobjcontent.addAll(rslts.getObjects());
        }
        String token = null;
        if (rslts != null && rslts.getToken() != null) {
            token = rslts.getToken();
        }
        while (token != null && !token.isEmpty()) {
            rslts = vimPort.continueRetrievePropertiesEx(serviceContent.getPropertyCollector(), token);
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
        return getEntityByTypeAndId(VIM25_HOST_TYPE, hostId);
    }

    public static ManagedObjectReference getVMById(String vmId) {
        return getEntityByTypeAndId(VIM25_VM_TYPE, vmId);
    }

    public static ManagedObjectReference getEntityByTypeAndId(String type, String id) {
        ManagedObjectReference ref = new ManagedObjectReference();
        ref.setType(type);
        ref.setValue(id);
        return ref;
    }

    public static Map<String, String> getHostStaticProperties(String hostId, String[] properties,
            ServiceContent serviceContent, VimPortType vimPort) throws InvalidPropertyFaultMsg,
            RuntimeFaultFaultMsg {

        return getStaticProperties(getmObjRefByHostId(hostId), properties, serviceContent, vimPort);
    }

    public static Map<String, String> getVMStaticProperties(String vmId, String[] properties,
            ServiceContent serviceContent, VimPortType vimPort) throws InvalidPropertyFaultMsg,
            RuntimeFaultFaultMsg {
        return getStaticProperties(getVMById(vmId), properties, serviceContent, vimPort);
    }

    public static Map<String, Object> getRawStaticProperties(ManagedObjectReference mObjRef,
            String[] properties, ServiceContent serviceContent, VimPortType vimPort)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        ObjectContent[] oContents = VimServiceUtil.getObjectProperties(mObjRef, properties, serviceContent,
                vimPort);
        Map<String, Object> propMap = new HashMap<String, Object>();
        if (oContents != null) {
            for (ObjectContent oc : oContents) {
                List<DynamicProperty> dynamicPropertyList = oc.getPropSet();
                if (dynamicPropertyList != null) {
                    for (DynamicProperty dp : dynamicPropertyList) {
                        propMap.put(dp.getName(), dp);
                    }
                }
            }
        }
        return propMap;
    }

    public static Map<String, String> getStaticProperties(ManagedObjectReference mObjRef,
            String[] properties, ServiceContent serviceContent, VimPortType vimPort)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        ObjectContent[] oContents = VimServiceUtil.getObjectProperties(mObjRef, properties, serviceContent,
                vimPort);
        Map<String, String> propMap = new HashMap<String, String>();
        if (oContents != null) {
            for (ObjectContent oc : oContents) {
                List<DynamicProperty> dynamicPropertyList = oc.getPropSet();
                if (dynamicPropertyList != null) {
                    for (DynamicProperty dp : dynamicPropertyList) {
                        if (dp.getVal() instanceof ArrayOfManagedObjectReference) {
                            resolveArrayOfManagedObjectReference((ArrayOfManagedObjectReference) dp.getVal(),
                                    propMap, serviceContent, vimPort);
                        } else {
                            resloveAndAddDynamicPropertyToMap(dp, propMap, serviceContent, vimPort,
                                    mObjRef.getType());
                        }
                    }
                }
            }
        }
        return propMap;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void resloveAndAddDynamicPropertyToMap(DynamicProperty dp, Map propertyMap,
            ServiceContent serviceContent, VimPortType vimPort, String morType) {
        Object propertyValue = dp.getVal();
        if (propertyValue instanceof ArrayOfHostSystemIdentificationInfo) {
            List<HostSystemIdentificationInfo> hostSystemIdInfoList = ((ArrayOfHostSystemIdentificationInfo) propertyValue)
                    .getHostSystemIdentificationInfo();
            for (int index = 0; index < hostSystemIdInfoList.size(); index++) {
                HostSystemIdentificationInfo info = hostSystemIdInfoList.get(index);
                if (info.getIdentifierValue() != null) {
                    String key = String.format("host.identification.%s.%s", index, info.getIdentifierType()
                            .getKey());
                    propertyMap.put(key, info.getIdentifierValue());
                }
            }
        } else if (propertyValue instanceof ArrayOfGuestDiskInfo) {
            List<GuestDiskInfo> guestDiskInfoList = ((ArrayOfGuestDiskInfo) propertyValue).getGuestDiskInfo();
            if (guestDiskInfoList != null) {
                for (int index = 0; index < guestDiskInfoList.size(); index++) {
                    GuestDiskInfo guestDiskInfo = guestDiskInfoList.get(index);
                    Long capacity = guestDiskInfo.getCapacity();
                    if (capacity == null) {
                        continue;
                    }

                    propertyMap.put(String.format("disk.%s.path", index), guestDiskInfo.getDiskPath());
                    propertyMap.put(String.format("disk.%s.total", index), String.valueOf(capacity));
                    Long freeSpace = guestDiskInfo.getFreeSpace();
                    if (freeSpace == null) {
                        propertyMap.put(String.format("disk.%s.free", index), "0");
                        propertyMap.put(String.format("disk.%s.used", index), String.valueOf(capacity));
                    } else {
                        long used = capacity - freeSpace;
                        propertyMap.put(String.format("disk.%s.free", index), String.valueOf(freeSpace));
                        propertyMap.put(String.format("disk.%s.used", index), String.valueOf(used));
                    }
                }
            }
        } else if (propertyValue instanceof ArrayOfGuestNicInfo) {
            List<GuestNicInfo> guestNicInfoList = ((ArrayOfGuestNicInfo) propertyValue).getGuestNicInfo();
            if (guestNicInfoList != null) {
                for (int index = 0; index < guestNicInfoList.size(); index++) {
                    GuestNicInfo guestNicInfo = guestNicInfoList.get(index);
                    String ipAdd = String.valueOf(guestNicInfo.getIpAddress());
                    String mac = guestNicInfo.getMacAddress();

                    propertyMap.put(String.format("network.%s.ip", index), ipAdd);
                    propertyMap.put(String.format("network.%s.mac", index), mac);
                }
            }
        } else if (propertyValue instanceof ArrayOfPhysicalNic) {
            List<PhysicalNic> nicInfoList = ((ArrayOfPhysicalNic) propertyValue).getPhysicalNic();
            if (nicInfoList != null) {
                for (int index = 0; index < nicInfoList.size(); index++) {
                    PhysicalNic nicInfo = nicInfoList.get(index);
                    String mac = nicInfo.getMac();
                    String speed = String.valueOf(nicInfo.getLinkSpeed().getSpeedMb());
                    String ipAdd = nicInfo.getSpec().getIp().getIpAddress();

                    propertyMap.put(String.format("network.%s.ip", index), ipAdd);
                    propertyMap.put(String.format("network.%s.mac", index), mac);
                    propertyMap.put(String.format("network.%s.speed", index), speed);
                }
            }
        } else if (propertyValue instanceof ArrayOfHostVirtualNic) {
            List<HostVirtualNic> hostVirtualNicList = ((ArrayOfHostVirtualNic) propertyValue)
                    .getHostVirtualNic();
            if (hostVirtualNicList != null) {
                for (int index = 0; index < hostVirtualNicList.size(); index++) {
                    HostVirtualNic hostVirtualNic = hostVirtualNicList
                            .get(index);
                    String ip = hostVirtualNic.getSpec().getIp().getIpAddress();
                    String mac = hostVirtualNic.getSpec().getMac();
                    propertyMap.put(String.format("network.vnic.%s.ip", index),
                            ip);
                    propertyMap.put(
                            String.format("network.vnic.%s.mac", index), mac);
                }
            }
        }else if (propertyValue instanceof ManagedObjectReference) {
            if (dp.getName().equals(VimServiceConstants.PROP_VM_RESOURCE_POOL)) {
                ManagedObjectReference resp = getEntityByTypeAndId("ResourcePool",
                        ((ManagedObjectReference) propertyValue).getValue());
                String name = null;
                try {
                    name = getMORProperty(resp, "name", serviceContent, vimPort);
                    propertyMap.put(VimServiceConstants.PROP_VM_RESOURCE_POOL,
                            ((ManagedObjectReference) propertyValue).getValue());
                    propertyMap.put(VimServiceConstants.PROP_VM_RESOURCE_POOL_NAME, name);
                } catch (RuntimeFaultFaultMsg e) {
                    logger.error(e);
                }
            } else if (dp.getName().equals(VimServiceConstants.PROP_HOST_SITE)) {
                ManagedObjectReference resp = null;
                if (morType.equals(VIM25_VM_TYPE)) {
                    resp = getEntityByTypeAndId("Folder", ((ManagedObjectReference) propertyValue).getValue());
                } else {
                    resp = getEntityByTypeAndId("ClusterComputeResource",
                            ((ManagedObjectReference) propertyValue).getValue());
                }

                String name = null;
                try {
                    name = getMORProperty(resp, "name", serviceContent, vimPort);
                    propertyMap.put(VimServiceConstants.PROP_HOST_SITE,
                            ((ManagedObjectReference) propertyValue).getValue());
                    propertyMap.put(VimServiceConstants.PROP_HOST_SITE_NAME, name);
                } catch (RuntimeFaultFaultMsg e) {
                    logger.error(e);
                }
            } else {
                propertyMap.put(dp.getName(), ((ManagedObjectReference) propertyValue).getValue());
            }
        } else {
            propertyMap.put(dp.getName(), propertyValue.toString());
        }
    }

    private static String getMORProperty(ManagedObjectReference mor, String prop,
            ServiceContent serviceContent, VimPortType vimPort) throws RuntimeFaultFaultMsg {
        String ret = null;
        try {
            Map<String, Object> oc = VimServiceUtil.getRawStaticProperties(mor, new String[] { prop },
                    serviceContent, vimPort);
            ret = (String) ((DynamicProperty) oc.get("name")).getVal();
        } catch (InvalidPropertyFaultMsg e) {
            ret = "<no property '" + prop + "' found>";
            logger.error(ret, e);
        }
        return ret;

    }

    private static void resolveArrayOfManagedObjectReference(ArrayOfManagedObjectReference array,
            Map<String, String> propertyMap, ServiceContent serviceContent, VimPortType vimPort)
            throws InvalidPropertyFaultMsg, RuntimeFaultFaultMsg {
        List<ManagedObjectReference> mObjRefList = ((ArrayOfManagedObjectReference) array)
                .getManagedObjectReference();
        for (int index = 0; index < mObjRefList.size(); index++) {
            ManagedObjectReference mObjRef = mObjRefList.get(index);
            if ("Datastore".equals(mObjRef.getType())) {
                Map<String, String> props = getStaticProperties(mObjRef, DS_STATIC_PROPERTIES,
                        serviceContent, vimPort);
                String type = props.get(PROP_DS_TYPE);
                if ("VMFS".equals(type) || "NFS".equals(type)) {
                    String id = mObjRef.getValue();
                    String capacity = props.get(PROP_DS_CAPACITY);
                    if (capacity != null) {
                        propertyMap.put(String.format("host.datastore.%s.total", id), capacity);
                    }
                    String free = props.get(PROP_DS_FREE_SPACE);
                    propertyMap.put(String.format("host.datastore.%s.free", id), (free == null) ? "0" : free);
                }
                break;
            }
        }
    }

    public static Map<String, String> getHostDynamicProperties(String hostId, String[] properties,
            ServiceContent serviceContent, VimPortType vimPort) throws InvalidPropertyFaultMsg,
            RuntimeFaultFaultMsg {
        return getDynamicProperties(getmObjRefByHostId(hostId), properties, serviceContent, vimPort);
    }

    public static Map<String, String> getVmDynamicProperties(String vmId, String[] properties,
            ServiceContent serviceContent, VimPortType vimPort) throws InvalidPropertyFaultMsg,
            RuntimeFaultFaultMsg {
        ManagedObjectReference vmById = getVMById(vmId);
        return getDynamicProperties(vmById, properties, serviceContent, vimPort);
    }

    public static Map<String, String> getDynamicProperties(ManagedObjectReference mObjRef,
            String[] properties, ServiceContent serviceContent, VimPortType vimPort)
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

        List<ObjectContent> objContentList = retrievePropertiesAllObjects(propertyFilterSpecs,
                serviceContent, vimPort);

        List<PerfCounterInfo> perfCounterList = null;

        if (objContentList != null && !objContentList.isEmpty()) {
            ObjectContent objContent = objContentList.get(0);
            List<DynamicProperty> propSet = objContent.getPropSet();
            if (propSet != null && !propSet.isEmpty()) {
                DynamicProperty dp = propSet.get(0);
                perfCounterList = ((ArrayOfPerfCounterInfo) dp.getVal()).getPerfCounterInfo();
            }
        }

        List<String> propList = Arrays.asList(properties);
        Map<Integer, PerfCounterInfo> perfCounterInfoMap = new HashMap<Integer, PerfCounterInfo>();
        for (PerfCounterInfo pci : perfCounterList) {
            if (propList.contains(getKey(pci))) {
                perfCounterInfoMap.put(pci.getKey(), pci);
            }
        }

        List<PerfMetricId> availablePerfMetrics = vimPort.queryAvailablePerfMetric(
                serviceContent.getPerfManager(), mObjRef, null, null, new Integer(20));

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

        List<PerfEntityMetricBase> queryPerf = vimPort.queryPerf(serviceContent.getPerfManager(), qSpecs);
        Map<String, String> nodeProperties = new HashMap<String, String>();
        for (PerfEntityMetricBase perfEntityMetricBase : queryPerf) {
            PerfEntityMetric perfEntityMetric = (PerfEntityMetric) perfEntityMetricBase;
            List<PerfMetricSeries> perfMetricsSeriesList = perfEntityMetric.getValue();
            for (PerfMetricSeries perfMetricsSeries : perfMetricsSeriesList) {
                PerfCounterInfo perfCounterInfo = perfCounterInfoMap.get(perfMetricsSeries.getId()
                        .getCounterId());

                if (perfCounterInfo != null && perfMetricsSeries instanceof PerfMetricIntSeries) {
                    List<Long> value = ((PerfMetricIntSeries) perfMetricsSeries).getValue();
                    if (!value.isEmpty()) {
                        nodeProperties.put(getKey(perfCounterInfo), value.get(0).toString());
                    }

                }
            }
        }
        return nodeProperties;
    }

    private static String getKey(PerfCounterInfo pci) {
        return (new StringBuilder()).append(pci.getGroupInfo().getKey()).append('.')
                .append(pci.getNameInfo().getKey()).append('.').append(pci.getRollupType().name()).toString();
    }

    public static String getHumanNameFromMorName(String name) {
        if (name == null) {
            return NO_HUMAN_READABLE_NAME_FROM_MOR_NAME;
        }
        if (name.indexOf("(") < 0) {
            return name.trim();
        } else {
            return name.substring(0, name.indexOf("(")).trim();
        }
    }

    public static String getIdFromMorName(String name) {
        if (name == null || name.isEmpty() || name.indexOf("(") < 0 || name.indexOf(")") < 0) {
            return NO_ID_FROM_MOR_NAME;
        }
        return name.substring(name.indexOf("(") + 1, name.indexOf(")")).replace("(", "").replace(")", "");
    }

    // non-instantiable
    private VimServiceUtil() {
    }

}
