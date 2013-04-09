package org.ow2.proactive.iaas.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {


	public static String argsToString(List<String> args){
		return argsToString(args, " ");
	}
	
	public static String argsToString(List<String> args, String sep){
		StringBuffer sb = new StringBuffer();
		boolean first = true;
		for (String s: args){
			if (first){
				sb.append(s);
				first = false;
			}else{
				sb.append(sep + s);
			}
		}
		return sb.toString();
	}
	
	public static String argsToString(String[] args){
		return argsToString(Arrays.asList(args));
	}
	
	public static Map<String, String> convert (Map<String, Object> src) {
	    HashMap<String, String> outp = new HashMap<String, String> ();
	    for (String key: src.keySet()) {
	        Object value = src.get(key);
	        if (value != null)
    	        outp.put(key, value.toString());
	    }
	    return outp;
	}
}
