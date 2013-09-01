package org.ow2.proactive.iaas.testutils;

import org.objectweb.proactive.core.util.ProActiveInet;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.registry.LocateRegistry;
import java.util.HashMap;

public class MBeanServerHelper {

    private JMXConnectorServer connectorServer;

    public void createJMXServer(MBeanServer mbs, String serverName) throws IOException {
        // Boot the JMX RMI connector server
        connectorServer = createJMXRMIConnectorServer(mbs, serverName);
        connectorServer.start();

    }

    public JMXServiceURL getAddress() {
        return connectorServer.getAddress();
    }

    private JMXConnectorServer createJMXRMIConnectorServer(MBeanServer mbs, String connectorServerName)
            throws IOException {

        int port = this.getJMXRMIConnectorServerPort();

        // Create or reuse an RMI registry needed for the connector server
        LocateRegistry.createRegistry(port);

        // Use the same hostname as ProActive (follows properties defined by ProActive configuration)
        final String hostname = ProActiveInet.getInstance().getHostname();

        // The asked address of the new connector server. The actual address can be different due to
        // JMX specification. See {@link JMXConnectorServerFactory} documentation.
        final String jmxConnectorServerURL = "service:jmx:rmi:///jndi/rmi://" + hostname + ":" + port + "/" + connectorServerName;

        JMXServiceURL jmxUrl = new JMXServiceURL(jmxConnectorServerURL);
        final HashMap<String, Object> env = new HashMap<String, Object>(1);

        // Create the connector server
        return JMXConnectorServerFactory.newJMXConnectorServer(jmxUrl, env, mbs);

    }

    private int getJMXRMIConnectorServerPort() {

        ServerSocket server;
        try {
            server = new ServerSocket(0);
            int port = server.getLocalPort();
            server.close();
            return port;
        } catch (Exception e) {
            // at worst try to return a random port
            return (int) (5000 + (Math.random() * 1000));
        }

    }

    public void stopJMXServer() throws IOException {
        if (connectorServer != null)
            connectorServer.stop();

    }

}