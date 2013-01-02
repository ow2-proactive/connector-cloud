package org.ow2.proactive.testclient;

import org.ow2.proactive.iaas.IaasVM;
import org.ow2.proactive.iaas.cloudstack.CloudStackAPI;

import java.util.HashMap;

public class Main {

    public static void main(String[] args) throws Exception {

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("apiurl", "http://localhost:8080/client/api");
        params.put("apikey", "dQEdbQVukQYkzGl9O_sG5qknip0mnXBtPfVBaJMiZd5LbwNuf3HTNi8hfxzLcXm32auykyoHuV_PIkak2kLeuA");
        params.put("secretkey", "VV_w_yDEqST8ovh0mkQpDh8nXEzyMBsW0wFyCEhjneZazHIX8IcNCAgsjGF3p2ZzeVqyxYT6vwWJm6TSv5tdoQ");
        CloudStackAPI cloudStackAPI = new CloudStackAPI(params);

        HashMap<String, String> paramsStart = new HashMap<String, String>();
        paramsStart.put("serviceofferingid","4fe8b730-f227-4693-8b5e-bf384c566853");
        paramsStart.put("templateid", "23c780e0-564d-4a02-b03c-3eb28847cfb1");
        paramsStart.put("zoneid", "ff2169df-f439-4694-817c-31babf50df9f");
        paramsStart.put("userdata", "192.168.56.1");

        IaasVM vm = cloudStackAPI.startVm(paramsStart);
        System.out.println(vm.getVmId());

        cloudStackAPI.isVmStarted(vm.getVmId());

        HashMap<String, String> paramsStarted = new HashMap<String, String>();
        paramsStarted.put("diskid", "cf48db13-a1d9-4c2a-9df7-22a88b7ab885");
        cloudStackAPI.attachVolume(vm, paramsStarted);

        cloudStackAPI.isVmStarted(vm.getVmId());

        cloudStackAPI.reboot(vm.getVmId());

        cloudStackAPI.isVmStarted(vm.getVmId());


//        cloudStackAPI.isVmStarted("5381718c-dc52-4338-a233-e02154cbd293");
//        cloudStackAPI.stopVm(paramsStarted);
    }    /*
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
