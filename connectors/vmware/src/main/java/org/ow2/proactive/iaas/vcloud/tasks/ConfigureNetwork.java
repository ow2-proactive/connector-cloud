package org.ow2.proactive.iaas.vcloud.tasks;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.ow2.proactive.iaas.IaasExecutable;
import org.ow2.proactive.iaas.vcloud.FirewallRule;
import org.ow2.proactive.iaas.vcloud.NatRule;
import org.ow2.proactive.iaas.vcloud.VCloudAPI;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scripting.PropertyUtils;


public class ConfigureNetwork extends IaasExecutable {

    @Override
    public Serializable execute(TaskResult... results) throws Throwable {
        VCloudAPI api = null;
        String vappId = null;
        try {
            api = (VCloudAPI) createApi(args);
            if (args.get("vappid") != null) {
                vappId = args.get("vappid").split("/")[2];
            } else if( System.getProperty("occi.compute.vendor.vmpath") != null ) {
                vappId = System.getProperty("occi.compute.vendor.vmpath").split("/")[2];
                PropertyUtils.propagateProperty("occi.compute.vendor.vmpath");
            } else {
                vappId = System.getProperty("vcloud.vapp.id");
                PropertyUtils.propagateProperty("vcloud.vapp.id");
            } 
            String vdcName = args.get(VCloudAPI.VCloudAPIConstants.InstanceParameters.VDC_NAME);
            System.out.println("[ConfigureNetwork task] Configuring vApp [" + vappId + "] with network [" +
                vdcName + "]...");

            List<NatRule> natRules = new ArrayList<NatRule>();
            natRules.add(new NatRule( "SSH", "TCP", 22, 22));
            natRules.add(new NatRule("RDP", "TCP", 3389, 3389));
            
            List<FirewallRule> firewallRules = new ArrayList<FirewallRule>();
            firewallRules.add(new FirewallRule("PING", "ICMP", "Any", "Any", "Any"));
            firewallRules.add(new FirewallRule("SSH", "TCP", "Any", "Any", "22"));
            firewallRules.add(new FirewallRule("RDP", "TCP", "Any", "Any", "3389"));
            firewallRules.add(new FirewallRule("In-Out", "ANY", "internal", "external", "Any"));;
            
            api.configureNetwork(vappId, vdcName, natRules, firewallRules);
            PropertyUtils.propagateProperty("vcloud.vapp.id");
            return "done";

        } catch (Throwable e) {
            e.printStackTrace();
            System.setProperty("error.description", e.getMessage());
            return e.getMessage();
        } finally {
            if(api !=null) {
                api.disconnect();
            }
        }
    }

}
