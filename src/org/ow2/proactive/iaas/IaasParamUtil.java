package org.ow2.proactive.iaas;

public class IaasParamUtil {

	private IaasParamUtil() {
	}

	public static String getParameterValue(String paramName, Object... params) {
		for (Object param : params) {
			String sparam = param.toString();
			if (sparam.indexOf('=') > 0) {
				String[] kv = sparam.split("=");
				if (kv.length == 2 && kv[0].equals(paramName)) {
					return kv[1];
				}
			}
		}
		return null;
	}

}
