/*
 *  *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2011 INRIA/University of
 *                 Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org or contact@activeeon.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; version 3 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 *  * $$PROACTIVE_INITIAL_DEV$$
 */
package org.ow2.proactive.iaas.cloudstack;

import org.ow2.proactive.iaas.IaasApi;
import org.ow2.proactive.iaas.IaasVM;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import static java.util.Arrays.asList;

public class CloudStackAPI implements IaasApi {

    public static final String ENCODING = "UTF-8";
    public static final String SECURITY_ALGORITHM = "HmacSHA1";

    private final String apiURL;
    private final String apiKey;
    private final String secretKey;

    public CloudStackAPI(Map<String, String> args) {
        this.apiKey = args.get("apikey");
        this.secretKey = args.get("secretkey");
        this.apiURL = args.get("apiurl");
    }

    @Override
    public IaasVM startVm(Map<String, String> arguments) throws Exception {

        List<NameValuePair> params = asList(
                findArgument("serviceofferingid", arguments),
                findArgument("templateid", arguments),
                new BasicNameValuePair("userdata", new String(Base64.encodeBase64(findArgument("userdata", arguments).getValue().getBytes(ENCODING)), ENCODING)),
                findArgument("zoneid", arguments));

        String responseString = callApi("deployVirtualMachine", params);

        System.out.println(responseString);

        DocumentBuilderFactory dbf =
                DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(responseString));
        Document doc = db.parse(is);
        NodeList nodes = doc.getElementsByTagName("id");

        // TODO query XML for vm id
        return new IaasVM(nodes.item(0).getTextContent());

    }

    private NameValuePair findArgument(String key, Map<String, String> arguments) {
        return new BasicNameValuePair(key, arguments.get(key));
    }

    @Override
    public void stopVm(Map<String, String> args) throws Exception {

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(findArgument("id", args));

        String responseString = callApi("destroyVirtualMachine", params);

        System.out.println(responseString);

    }

    @Override
    public boolean isVmStarted(String vmId) throws Exception {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("id", vmId));

        String responseString = callApi("listVirtualMachines", params);

        System.out.println(responseString);

        return responseString.contains("Running"); // TODO query xml for status field "State = Running"
    }

    private String callApi(String command, List<NameValuePair> params) throws NoSuchAlgorithmException, InvalidKeyException, URISyntaxException, IOException {
        List<NameValuePair> queryParams = new ArrayList<NameValuePair>();

        queryParams.add(new BasicNameValuePair("apikey", apiKey));
        queryParams.add(new BasicNameValuePair("command", command));

        queryParams.addAll(params);

        String signature = computeSignature(secretKey, queryParams);

        queryParams.add(new BasicNameValuePair("signature", signature));

        URI uri = new URIBuilder(apiURL).setQuery(URLEncodedUtils.format(queryParams, ENCODING)).build();
        HttpUriRequest get = new HttpGet(uri);
        HttpResponse response = new DefaultHttpClient().execute(get);
        InputStream content = response.getEntity().getContent();
        return IOUtils.toString(content);
    }

    private static String computeSignature(String secretKey, List<NameValuePair> params) throws NoSuchAlgorithmException, InvalidKeyException {

        Collections.sort(params, NAME_VALUE_PAIR_COMPARATOR);

        String paramAsString = URLEncodedUtils.format(params, ENCODING);
        paramAsString = paramAsString.toLowerCase();

        Mac mac = Mac.getInstance(SECURITY_ALGORITHM);
        SecretKeySpec secret_key = new SecretKeySpec(secretKey.getBytes(), SECURITY_ALGORITHM);
        mac.init(secret_key);
        byte[] digest = mac.doFinal(paramAsString.getBytes());
        return DatatypeConverter.printBase64Binary(digest);
    }

    public static final Comparator<NameValuePair> NAME_VALUE_PAIR_COMPARATOR = new Comparator<NameValuePair>() {
        @Override
        public int compare(NameValuePair o1, NameValuePair o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };

    public void attachVolume(String vmId, String diskId) throws IOException, NoSuchAlgorithmException, InvalidKeyException, URISyntaxException {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("id", diskId));
        params.add(new BasicNameValuePair("virtualmachineid", vmId));
        String responseString = callApi("attachVolume", params);
        System.out.println(responseString);
    }

    public void reboot(String vmId) throws IOException, NoSuchAlgorithmException, InvalidKeyException, URISyntaxException {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("id", vmId));
        String responseString = callApi("rebootVirtualMachine", params);
        System.out.println(responseString);
    }
}
