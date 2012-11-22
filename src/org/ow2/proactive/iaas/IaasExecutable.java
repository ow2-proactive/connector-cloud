package org.ow2.proactive.iaas;

import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;
import java.util.List;

import org.objectweb.proactive.core.ProActiveException;
import org.ow2.proactive.resourcemanager.resource.Result;
import org.ow2.proactive.resourcemanager.resource.iaas.compute.StartResult;
import org.ow2.proactive.resourcemanager.resource.iaas.compute.StopResult;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scheduler.common.task.executable.JavaExecutable;

import com.xerox.amazonws.ec2.ReservationDescription.Instance;

public class IaasExecutable extends JavaExecutable {

	public String cloudInfrastructure;
	public String operation;
	public String name; //will contain the uuid of the machine... relevant for stop-actions

	@Override
	public Serializable execute(TaskResult... arg0) throws Throwable {

		System.out
				.println("Cloud Infrastructure Value: " + cloudInfrastructure);
		String request = null;
		Result taskResult = null;
		if (cloudInfrastructure.equals("2")) { // FIXME: this is very dirty, has
												// been made just for testing
												// purposes.

			// EUCA

			String imageId = "emi-72B73BF7";
			EucalyptusConnector eucaObject = new EucalyptusConnector(
					"5XWO2SRUSDODZFD3U3DVM",
					"15Odess52JVhXFCMbLk8FMGE5tbgw11THeJFLNca", "lol",
					"eucalyptus.partner.eucalyptus.com", 8773);

			try {

				List<Instance> result = eucaObject.start(imageId, 1, 1);
				System.out.println(result);
				// assertTrue(result != null);

			} catch (ProActiveException e) {
				// fail("Method trew ProactiveException");

			}
			// Thread.sleep(60000);

			eucaObject.stop(imageId);

		}

		if (cloudInfrastructure.equals("1")) { // FIXME: this is very dirty, has
												// been made just for testing
												// purposes.
												// hpcloud

			if (operation.equals("deploy")){
			
			
				taskResult = new StartResult(true);
				
			try {

				OpenStackAPI hpAPI = OpenStackAPI
						.getOpenStackAPI(
								"henri.piriou@activeeon.com",
								"webmail@2020",
								"henri.piriou@activeeon.com-tenant1",
								URI.create("https://region-a.geo-1.identity.hpcloudsvc.com:35357/v2.0/tokens"),
								CloudProvider.HPCLOUD);

				// starting the vm
				String name = "Test-Iaas-" + Math.round(Math.random() * 1000);
				String flavor = "100";
				String image = "1234";
				HashMap<String, String> metadata = new HashMap<String, String>();

				hpAPI.listAvailableFlavors();

				request = hpAPI.createServer(name, image, flavor, metadata);
				System.out.println("RESOURCE ID: " + request);

			} catch (Exception e) {
				e.printStackTrace();
			}
			
			StartResult startResult = new StartResult(true);
			startResult.setVmid(request);
			taskResult = startResult;
		}
		}
		else if (operation.equals("undeploy")){
			
			OpenStackAPI hpAPI = OpenStackAPI
					.getOpenStackAPI(
							"henri.piriou@activeeon.com",
							"webmail@2020",
							"henri.piriou@activeeon.com-tenant1",
							URI.create("https://region-a.geo-1.identity.hpcloudsvc.com:35357/v2.0/tokens"),
							CloudProvider.HPCLOUD);
			
			boolean isSuccessful = hpAPI.deleteServer(name);
			
			taskResult = new StopResult(isSuccessful);
			
			
			
		}

		

		

		return taskResult;

	}

}
