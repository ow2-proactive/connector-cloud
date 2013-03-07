package org.ow2.proactive.iaas;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.ow2.proactive.iaas.nova.NovaAPI;
import org.ow2.proactive.iaas.nova.NovaAPI.NovaAPIConstants;

public class NovaTest {
    
    private static Map<String, String> getInstanceParams() {
        String nodeSourceName = "node-source-nova";
        String nodeName = "nodeva-node";
        
        Map<String, String> args = new HashMap<String, String>();
        args.put("name", nodeName);
        args.put("nodesource", nodeSourceName);
        //args.put(NovaAPIConstants.InstanceParameters.IMAGE_REF, "4113ecd8-0268-455d-b87d-202600ad0b9c");
        args.put(NovaAPIConstants.InstanceParameters.IMAGE_REF, "f56b6fe7-642b-4295-9176-502060eb9818");        
        args.put(NovaAPIConstants.InstanceParameters.FLAVOR_REF, "1");
        
        return args;
    }
    
    
    public static void main(String[] args) throws Exception {
        String apiUrl = "http://127.0.0.1:5000/v2.0";
        String userName = "admin";
        String password = "sa";
        String tenantName = "Admin";
        
        NovaAPI api = NovaAPI.getNovaAPI(userName, password, tenantName, new URI(apiUrl));
        api.startInstance(getInstanceParams());
    }
}
