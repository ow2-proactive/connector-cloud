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

package org.ow2.proactive.iaas.nova;

import java.util.HashMap;
import java.util.Map;
import org.ow2.proactive.iaas.monitoring.IaaSNodesListener;
import org.ow2.proactive.iaas.monitoring.NodeType;

public class NovaMonService 
				implements NovaMonServiceMBean, IaaSNodesListener {

    public NovaMonService(
    		String url, String login, 
    		String password) {
    	// TODO to complete here.
    }
	
    @Override
    public String[] getVMs() {
    	// TODO to complete here.
        return new String[]{"vm1", "vm2"};
    }
    
    @Override
    public String[] getHosts() {
    	// TODO to complete here.
        return new String[]{"host1", "host2"};
    }

    @Override
    public Map<String, String> getHostProperties(String hostId) {
    	// TODO to complete here.
    	Map<String, String> ret = new HashMap<String,String>();
    	ret.put("cpu.usage", "0.25");
    	ret.put("mem.usage", "0.35");
    	return ret;
    }
    
    @Override
    public String[] getVMs(String hostId) {
    	// TODO to complete here.
        return new String[]{"vm1", "vm2"};
    }

    @Override
    public Map<String, String> getVMProperties(String vmId) {
    	// TODO to complete here.
    	Map<String, String> ret = new HashMap<String,String>();
    	ret.put("cpu.usage", "0.25");
    	ret.put("mem.usage", "0.35");
    	return ret;
    }

    public Map<String, Object> getSummary() {
    	Map<String, Object> summary = new HashMap<String, Object>();
    	
    	String[] hosts = this.getHosts();
    	for (String host: hosts) {
    		// put host properties
    		Map<String, Object> hostinfo = convert(getHostProperties(host));
    		
    		// put a list of vms with their properties
    		Map<String, Object> vmsinfo = new HashMap<String, Object>();
    		String[] vms = this.getVMs();
    		for (String vm: vms) {
    			vmsinfo.put(vm, this.getVMProperties(vm));
    		}
			hostinfo.put("vmsinfo", vmsinfo);	
			
			summary.put(host,  hostinfo);
    	}
    	
    	return summary;
    }
    
    private Map<String, Object> convert (Map<String, String> a){
    	Map<String, Object> r = new HashMap<String, Object>();
    	r.putAll(a);
    	return r;
    }
    
    public void registerNode(String nodeid, String jmxurl, NodeType type){
    	// TODO to complete here.
    }
    
    public void unregisterNode(String nodeid){
    	// TODO to complete here.
    }

}
