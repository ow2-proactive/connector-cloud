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
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

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
import org.xml.sax.InputSource;

import static java.util.Arrays.asList;

public class CloudStackAPI implements IaasApi {

    public static final String ENCODING = "UTF-8";
    public static final String SECURITY_ALGORITHM = "HmacSHA1";

    private static final String PENDING = "0";

    private final String apiURL;
    private final String apiKey;
    private final String secretKey;

    public CloudStackAPI(String apiUrl, String apiKey, String secretKey) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.apiURL = apiUrl;
    }

    public CloudStackAPI(Map<String, String> args) {
        this(args.get("apikey"), args.get("secretkey"), args.get("apiurl"));
    }

    @Override
    public IaasVM startVm(Map<String, String> arguments) throws Exception {

        List<NameValuePair> params = asList(
                findArgument("serviceofferingid", arguments),
                findArgument("templateid", arguments),
                findArgument("name", arguments),
                new BasicNameValuePair("userdata", new String(Base64.encodeBase64(findArgument("userdata", arguments).getValue().getBytes(ENCODING)), ENCODING)),
                findArgument("zoneid", arguments));

        String responseString = callApi("deployVirtualMachine", params);

        System.out.println(responseString);

        String vmId = xpath(responseString, "//id");

        String jobId = xpath(responseString, "//jobid");
        waitUntilAsynchronousOperationEnds(jobId);

        return new IaasVM(vmId);

    }

    private String xpath(String xmlAsString, String xpathExpression) throws XPathExpressionException {
        InputSource source = new InputSource(new StringReader(xmlAsString));
        XPath xpath = XPathFactory.newInstance().newXPath();
        return (String) xpath.compile(xpathExpression).evaluate(source, XPathConstants.STRING);
    }

    private void waitUntilAsynchronousOperationEnds(String jobId) throws IOException, NoSuchAlgorithmException, InvalidKeyException, URISyntaxException, XPathExpressionException, InterruptedException {
        List<NameValuePair> params = Collections.<NameValuePair>singletonList(new BasicNameValuePair("jobid", jobId));

        while (true) {
            String responseString = callApi("queryAsyncJobResult", params);
            String jobStatus = xpath(responseString, "//jobstatus");
            if (!PENDING.equals(jobStatus)) {
                break;
            }
            Thread.sleep(5000);
        }
    }

    private NameValuePair findArgument(String key, Map<String, String> arguments) {
        return new BasicNameValuePair(key, arguments.get(key));
    }

    @Override
    public void stopVm(IaasVM vm) throws Exception {

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("id", vm.getVmId()));

        String responseString = callApi("destroyVirtualMachine", params);

        System.out.println(responseString);

        String jobId = xpath(responseString, "//jobid");
        waitUntilAsynchronousOperationEnds(jobId);

    }

    @Override
    public boolean isVmStarted(IaasVM vm) throws Exception {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("id", vm.getVmId()));

        String responseString = callApi("listVirtualMachines", params);

        System.out.println(responseString);

        String vmState = xpath(responseString, "//state");
        return "Running".equals(vmState);
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

    public void attachVolume(IaasVM vm, Map<String, String> args) throws Exception {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("id", findArgument("diskid", args).getValue()));
        params.add(new BasicNameValuePair("virtualmachineid", vm.getVmId()));
        String responseString = callApi("attachVolume", params);
        System.out.println(responseString);
        String jobId = xpath(responseString, "//jobid");
        waitUntilAsynchronousOperationEnds(jobId);
    }

    public void reboot(String vmId) throws Exception {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("id", vmId));
        String responseString = callApi("rebootVirtualMachine", params);

        System.out.println(responseString);

        String jobId = xpath(responseString, "//jobid");
        waitUntilAsynchronousOperationEnds(jobId);
    }
}
