package org.ow2.proactive.iaas.vcloud;

import java.io.IOException;

import org.apache.commons.io.IOUtils;

public class CustomizeScriptBuilder {

	private static final String scriptTemplate;
	private static final String RM_NODE_NAME_TOKEN = "@rm.node.name@";
	private static final String RM_URL_TOKEN = "@rm.url@";
	private static final String RM_CRED_VAL_TOKEN = "@rm.cred.val@";

	private String rmNodeName;
	private String rmUrl;
	private String rmCredentialValue;

	static {
		try {
			scriptTemplate = IOUtils.toString(CustomizeScriptBuilder.class
					.getResourceAsStream("customize-script-template"));
		} catch (IOException e) {
			throw new RuntimeException("Cannot load script template:", e);
		}
	}

	public void setRmNodeName(String rmNodeName) {
		this.rmNodeName = rmNodeName;
	}

	public void setRmUrl(String rmUrl) {
		this.rmUrl = rmUrl;
	}

	public void setRmCredentialValue(String rmCredentialValue) {
		this.rmCredentialValue = rmCredentialValue;
	}

	public String buildScript() {
		return scriptTemplate.replace(RM_NODE_NAME_TOKEN, rmNodeName)
				.replace(RM_URL_TOKEN, rmUrl)
				.replace(RM_CRED_VAL_TOKEN, rmCredentialValue);

	}
}
