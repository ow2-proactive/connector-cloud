package org.ow2.proactive.iaas.vcloud;

public class FirewallRule {
    private String name;
    private String protocol;
    private String srcIp;
    private String dstIp;
    private String portRange;

    public FirewallRule(String name, String protocol, String srcIp, String dstIp, String portRange) {
        super();
        this.name = name;
        this.protocol = protocol;
        this.srcIp = srcIp;
        this.dstIp = dstIp;
        this.portRange = portRange;
    }

    public String getName() {
        return name;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getSrcIp() {
        return srcIp;
    }

    public String getDstIp() {
        return dstIp;
    }

    public String getPortRange() {
        return portRange;
    }

}
