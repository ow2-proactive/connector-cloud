package org.ow2.proactive.iaas.numergy;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class HttpUtility {

    private HttpUtility() {
    }

    public static HttpClient turnClientIntoInsecure(HttpClient client)
            throws SecurityException {

        try {
            SSLSocketFactory socketFactory = new SSLSocketFactory(
                    new RelaxedTrustStrategy(),
                    SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            Scheme https = new Scheme("https", 443, socketFactory);
            client.getConnectionManager().getSchemeRegistry().register(https);
            return client;
        } catch (KeyManagementException e) {
            throw new SecurityException(e);
        } catch (UnrecoverableKeyException e) {
            throw new SecurityException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new SecurityException(e);
        } catch (KeyStoreException e) {
            throw new SecurityException(e);
        }

    }

    private static class RelaxedTrustStrategy implements TrustStrategy {
        @Override
        public boolean isTrusted(X509Certificate[] arg0, String arg1)
                throws CertificateException {
            return true;
        }
    }

}