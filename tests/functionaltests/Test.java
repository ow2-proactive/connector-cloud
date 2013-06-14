import java.util.Map;
import java.util.Map.Entry;

import org.junit.Ignore;
import org.ow2.proactive.iaas.VimServiceClientTest;
import org.ow2.proactive.iaas.vcloud.monitoring.VimServiceClient;


@Ignore
public class Test {

    /*** TO DELETE ***/
    public static void main(String[] args) throws Exception {

        VimServiceClient v;
        System.out.println("Avoid any cxf*.jar in the classpath: " + System.getProperty("java.class.path"));
        VimServiceClientTest test = new VimServiceClientTest();
        v = new VimServiceClient();
        v.initialize("https://10.1.244.13/sdk/", "administrator", "P@ssw0rd");

        Map<String, String> pr;

        String host = "host-123";
        pr = v.getHostProperties(host);
        System.out.println("HOST " + host);
        for (Entry<String, String> p : pr.entrySet()) {
            System.out.println("" + p.getKey() + ":" + p.getValue());
        }

        String vm = "vm-1747";
        pr = v.getVMProperties(vm);
        System.out.println("VM " + vm);
        for (Entry<String, String> p : pr.entrySet()) {
            System.out.println("" + p.getKey() + ":" + p.getValue());
            ;
        }
        /*
         * Map<String, Object> det = v.getVendorDetails(); Map<String, Object> hosts = (Map<String,
         * Object>) det.get("ClusterComputeResource"); for (Entry<String, Object> a:
         * hosts.entrySet()){ System.out.println("" + a.getKey() + ":" + a.getValue());; }
         */

        v.disconnect();
    }

}
