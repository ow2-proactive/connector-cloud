package org.ow2.proactive.iaas.defaultinfrastructure;

import org.ow2.proactive.iaas.IaasApi;
import org.ow2.proactive.iaas.IaasInstance;
import org.ow2.proactive.iaas.IaasMonitoringApi;
import org.ow2.proactive.iaas.monitoring.IaasMonitoringException;
import java.util.*;


public class DefaultAPI implements IaasApi, IaasMonitoringApi {

    @Override
    public IaasInstance startInstance(Map<String, String> arguments) throws Exception {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void stopInstance(IaasInstance instance) throws Exception {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean isInstanceStarted(IaasInstance instance) throws Exception {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public void disconnect() throws Exception {
    }

    /*
     * IaaSMonitoringApi implementation for Nova
     */

    @Override
    public String[] getHosts() throws IaasMonitoringException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public String[] getVMs() throws IaasMonitoringException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public String[] getVMs(String hostId) throws IaasMonitoringException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Map<String, String> getHostProperties(String hostId) throws IaasMonitoringException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Map<String, String> getVMProperties(String vmId) throws IaasMonitoringException {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Map<String, Object> getVendorDetails() throws IaasMonitoringException {
        throw new UnsupportedOperationException("not implemented");
    }

}
