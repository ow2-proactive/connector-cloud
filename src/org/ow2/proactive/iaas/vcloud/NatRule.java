package org.ow2.proactive.iaas.vcloud;

public class NatRule {
    private String name;
    private String protocol;
    private int intPort;
    private int extPort;

    public NatRule(String name, String protocol, int intPort, int extPort) {
        super();
        this.name = name;
        this.protocol = protocol;
        this.intPort = intPort;
        this.extPort = extPort;
    }

    public String getName() {
        return name;
    }

    public String getProtocol() {
        return protocol;
    }

    public int getIntPort() {
        return intPort;
    }

    public int getExtPort() {
        return extPort;
    }

}
