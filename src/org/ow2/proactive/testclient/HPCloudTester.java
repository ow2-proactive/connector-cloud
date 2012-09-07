package org.ow2.proactive.testclient;

import java.net.URI;
import java.net.URISyntaxException;

import javax.security.sasl.AuthenticationException;

import org.ow2.proactive.iaas.NovaAPI;

public class HPCloudTester{

	public static void main(String[] args) {
		
		try{
		
		NovaAPI hpAPI = NovaAPI.getNovaAPI("henri.piriou@activeeon.com", "webmail@2020", "henri.piriou@activeeon.com-tenant1", 
									 URI.create("https://region-a.geo-1.identity.hpcloudsvc.com:35357/v2.0/tokens"));
		
//		NovaAPI hpAPI = NovaAPI.getNovaAPI("henri.piriou@activeeon.com", "webmail@2020", "henri.piriou@activeeon.com-tenant1", 
//				new URI("https://az-1.region-a.geo-1.compute.hpcloudsvc.com/v1.1/82756809334402"));
		
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		
		
		
	}
}
