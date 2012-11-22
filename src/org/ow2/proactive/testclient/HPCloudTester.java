package org.ow2.proactive.testclient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import javax.security.sasl.AuthenticationException;

import org.ow2.proactive.iaas.CloudProvider;
import org.ow2.proactive.iaas.NovaAPI;
import org.ow2.proactive.iaas.OpenStackAPI;

public class HPCloudTester{

	public static void main(String[] args) {
		
		try{
		
		OpenStackAPI hpAPI = OpenStackAPI.getOpenStackAPI("henri.piriou@activeeon.com", "webmail@2020", "henri.piriou@activeeon.com-tenant1", 
									 URI.create("https://region-a.geo-1.identity.hpcloudsvc.com:35357/v2.0/tokens"), CloudProvider.HPCLOUD);
		
	
		
        String step = null;

            //////////////////
            // Start the VM
            step = "Start";
            String name = "Test-Iaas-" + Math.round(Math.random() * 1000);
            String flavor = "100";
            String image = "1234";
            HashMap<String, String> metadata = new HashMap<String, String>();
            
            hpAPI.listAvailableFlavors();
            
            String request = hpAPI.createServer(name, image, flavor, metadata);   //starting the server
            System.out.println(request);
            //vmid = request.getResult().get(Compute.ResultValue.VMID);
		
		
		
		
		
		
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		
		
		
	}
}
