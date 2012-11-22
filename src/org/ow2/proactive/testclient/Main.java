package org.ow2.proactive.testclient;

import java.security.PublicKey;
import org.ow2.proactive.scheduler.common.Scheduler;
import org.ow2.proactive.scheduler.common.SchedulerConnection;
import org.ow2.proactive.authentication.crypto.CredData;
import org.ow2.proactive.scheduler.common.SchedulerAuthenticationInterface;
import org.ow2.proactive.authentication.crypto.Credentials;
import java.io.File;

public class Main {
	/*
	public static void main(String[] args) throws Exception{
		String policyfile = "config/java.policy";

		System.out.println("This example will simply connect and disconnect from the local ProActive Scheduler...");
		
		if (args.length != 3){
			System.out.println("Syntax:\n   schedulerurl username password\nExample:\n   ./bin/run rmi://localhost:1099 demo demo");
			System.exit(0);
		}

		String url = args[0];
		String user = args[1];
		String pass = args[2];
		
		System.out.println("Setting Java Security Policy...");
		if (new File(policyfile).exists() == false){
			System.out.println("Couldn't find the Java Security Policy file '" + policyfile + "'.");
			System.out.println("Your current directory is '" + (new File(".")).getCanonicalPath() + "'.");
			System.exit(0);
		}
		System.setProperty("java.security.policy","config/java.policy");

		Scheduler scheduler;
		System.out.println("Connecting to Scheduler (user=" + user + " pass=" + pass + " url=" + url + ")...");
		
		try{
			CredData cred = new CredData(CredData.parseLogin(user), CredData.parseDomain(user), pass);
			SchedulerAuthenticationInterface auth = SchedulerConnection.join(url);
			PublicKey pubKey = auth.getPublicKey();
			Credentials crede = Credentials.createCredentials(cred, pubKey);
			scheduler = auth.login(crede);
	
			System.out.println("Disconnecting from Scheduler...");
			scheduler.disconnect();
			System.out.println("Example finished correctly!");
		}catch(Exception e){
			System.out.println("Execution problem: " + e.getMessage());
			e.printStackTrace();
		}
		
		System.exit(0);
	}*/
}
