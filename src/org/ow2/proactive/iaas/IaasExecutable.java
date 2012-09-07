package org.ow2.proactive.iaas;


import java.io.Serializable;
import java.util.List;

import org.objectweb.proactive.core.ProActiveException;
import org.ow2.proactive.scheduler.common.task.TaskResult;
import org.ow2.proactive.scheduler.common.task.executable.JavaExecutable;

import com.xerox.amazonws.ec2.ReservationDescription.Instance;

import org.objectweb.proactive.core.ProActiveException;


public class IaasExecutable extends JavaExecutable{

	@Override
	public Serializable execute(TaskResult... arg0) throws Throwable {
		
		String imageId = "emi-72B73BF7";
		 EucalyptusConnector eucaObject = new EucalyptusConnector("5XWO2SRUSDODZFD3U3DVM", "15Odess52JVhXFCMbLk8FMGE5tbgw11THeJFLNca", "lol", "eucalyptus.partner.eucalyptus.com", 8773);

		try{
		
		List<Instance> result = eucaObject.start(imageId, 1, 1);
		System.out.println(result);
		//assertTrue(result != null);
		
		}
		catch(ProActiveException e){
			//fail("Method trew ProactiveException");
			
		}
		//Thread.sleep(60000);
		
	
		
		

		eucaObject.stop(imageId);
		


		
		return "OK";
	}

	
	
	
}
