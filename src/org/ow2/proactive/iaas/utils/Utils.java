package org.ow2.proactive.iaas.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Utils {

    public static final String OPTIONS_SEP = ",";
    public static final String KEY_VALUE_SEP = "=";
    
    public static String argsToString(List<String> args) {
        return argsToString(args, " ");
    }

    public static String argsToString(List<String> args, String sep) {
        StringBuffer sb = new StringBuffer();
        boolean first = true;
        for (String s : args) {
            if (first) {
                sb.append(s);
                first = false;
            } else {
                sb.append(sep + s);
            }
        }
        return sb.toString();
    }

    public static String argsToString(String[] args) {
        return argsToString(Arrays.asList(args));
    }

    public static Map<String, String> convertToStringMap(Map<String, Object> src) {
        HashMap<String, String> outp = new HashMap<String, String>();
        for (String key : src.keySet()) {
            Object value = src.get(key);
            if (value != null)
                outp.put(key, value.toString());
        }
        return outp;
    }

    public static Map<String, Object> convertToObjectMap(Map<String, String> a) {
        Map<String, Object> r = new HashMap<String, Object>();
        r.putAll(a);
        return r;
    }

    public static String getValueFromParameters(String flag, String options) {
        String[] allpairs = options.split(OPTIONS_SEP);
        for (String pair : allpairs) {
            if (pair.startsWith(flag + KEY_VALUE_SEP)) {
                String[] keyvalue = pair.split(KEY_VALUE_SEP);
                if (keyvalue.length != 2) {
                    throw new RuntimeException("Could not retrieve value for parameter '" + flag + "'.");
                }
                return keyvalue[1];
            }
        }
        return null;
    }

    public static boolean isPresentInParameters(String flag, String options) {
        String[] allprops = options.split(OPTIONS_SEP);
        for (String prop: allprops){
            if (prop.equals(flag))
                return true;
        }
        return false;
    }
}
