package org.ow2.proactive.iaas.monitoring.vmprocesses;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;


public class VMPPattern {
	
    private static final Logger logger = Logger.getLogger(VMPPattern.class);
    
	private static List<VMPPattern> patterns;
	
	static{
		String resourceprop = "/os-vmprocesses.properties";
		logger.debug("Parsing patterns in '"+resourceprop+"'...");
		Properties props = new Properties();
		try {
			props.load(VMProcess.class.getClass().getResourceAsStream(resourceprop));
		} catch (IOException e) {
			logger.error("Error while getting properties.", e);
		}
		patterns = new ArrayList<VMPPattern>();
		for(int i=0; i<ConfKeys.MAXPATTERNS; i++){
			VMPPattern patt = VMPPattern.extractVMPPatern(props, i);
			if (patt != null){
				logger.debug("### Found pattern: " + patt);
				patterns.add(patt);
			}else{
				break;
			}
		}
	}
	
	private String name;
	private String executablePattern;
	private Map<String, String> regexs;
	private Map<String, String> convers;
	
	public VMPPattern(String name, String argsprefix, 
			Map<String, String> regexs, Map<String, String> convers) {
		this.name = name;
		this.executablePattern = argsprefix;
		this.regexs = regexs;
		this.convers = convers;
	}
	
	public String getExecutablePattern(){
		return executablePattern;
	}

	public String getName(){
		return name;
	}

	public Map<String, String> getRegexs(){
		return regexs;
	}
	
	public static VMPPattern extractVMPPatern(Properties p, int index){
		Map<String, String> regexes = new HashMap<String, String>();
		Map<String, String> convers = new HashMap<String, String>();
		
		String namep = p.getProperty(ConfKeys.VMPATTERNX + index + ".name");
		String argsp = p.getProperty(ConfKeys.VMPATTERNX + index + ".expattern");
		
		for (int i=0 ; i< ConfKeys.MAXREGEXS; i++){
			String regexi = p.getProperty(ConfKeys.VMPATTERNX + index + ".regex." + i);
			if (regexi == null){
				break;
			}
			String[] split = regexi.split(ConfKeys.NAME_REGEX_SEP);
			
			if (split.length == 3){
				regexes.put(split[0], split[1]);
				convers.put(split[0], split[2]);
			}
		}
		
		if (argsp != null){
			return new VMPPattern(namep, argsp, regexes, convers);
		}else{
			return null;
		}
	}
	
	
	public boolean isVMP(String processargs) {
		// process arguments prefix checking
		if (getExecutablePattern() != null){ // If attribute present check it.
			if (processargs.matches(getExecutablePattern())){
				return true;
			}
		}
			
		// other criterias
		// ...
		
		return false;
	}
	
	
	public VMProcess getVMP(long pid, String args) {
		VMProcess process = null;
		
		if (isVMP(args) == false){
			return null;
		}
		
		if (args.matches(getExecutablePattern())){
			process = getVMProcessFromArgs(args);
		}
		
		if (process != null){
			process.setProperty("pid", new Long(pid));
			
		}
		return process;
	}
	
	
	private VMProcess getVMProcessFromArgs(String args){
		// apply my vmppattern rules to find out info about the process
		VMProcess vmp = new VMProcess();
		for (String propname: regexs.keySet()){
			String value = getValue(args, regexs.get(propname));
			String factorStr = convers.get(propname);
			try{
			    
			    if (factorStr.equals("=")){
			        // Leave value as it is.
			    } else if (factorStr.startsWith("i")) {
			        Float v = Float.parseFloat(value);
			        Float f = Float.parseFloat(factorStr.substring(1));
			        value = String.format("%d", (int)(f*v));
			    } else if (factorStr.startsWith("f")){
			        Float f = Float.parseFloat(factorStr.substring(1));
			        Float v = Float.parseFloat(value);
			        value = String.format("%.1f", f*v);
			    }
			}catch(Exception e){
			    logger.debug(String.format("Error parsing: factor='%s' value='%s'. Leaving original value.", factorStr, value), e);
			}
			if (value != null) { 
    			vmp.setProperty(propname, value);
			} else {
        		logger.warn("Problem parsing " + propname + " from '" + args + "'");
			}
			
		}
		return vmp;
	}
	
	private String getValue(String txt, String regex){
        Pattern strMatch = Pattern.compile(regex);
        Matcher m = strMatch.matcher(txt);
        if(m.find()){
            return m.group(1);
        }else{
        	logger.warn("Could not find match between args '"+txt+"' and regex '"+regex+"'.");
        	return null;
        }	
	}
			
	public static VMPPattern whatVMPPatternMatches(
			String args) {
		for (VMPPattern p: patterns){
			boolean proce = p.isVMP(args);
			if (proce == true){
				return p;
			}
		}
		return null;
	}

	public String toString(){
		return "name='" + name + "' executablePattern='" + getExecutablePattern() + "' regexs='" + this.regexs.size() + "'";
	}
}
