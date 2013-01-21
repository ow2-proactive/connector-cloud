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
import org.ow2.proactive.iaas.IaasInstance;

import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
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
import org.apache.commons.httpclient.HttpStatus;
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
import static org.ow2.proactive.iaas.cloudstack.CloudStackAPI.CloudStackAPIConstants.ApiParameters.API_KEY;
import static org.ow2.proactive.iaas.cloudstack.CloudStackAPI.CloudStackAPIConstants.ApiParameters.API_URL;
import static org.ow2.proactive.iaas.cloudstack.CloudStackAPI.CloudStackAPIConstants.ApiParameters.SECRET_KEY;
import static org.ow2.proactive.iaas.cloudstack.CloudStackAPI.CloudStackAPIConstants.InstanceParameters.NAME;
import static org.ow2.proactive.iaas.cloudstack.CloudStackAPI.CloudStackAPIConstants.InstanceParameters.SERVICE_OFFERING;
import static org.ow2.proactive.iaas.cloudstack.CloudStackAPI.CloudStackAPIConstants.InstanceParameters.TEMPLATE;
import static org.ow2.proactive.iaas.cloudstack.CloudStackAPI.CloudStackAPIConstants.InstanceParameters.USER_DATA;
import static org.ow2.proactive.iaas.cloudstack.CloudStackAPI.CloudStackAPIConstants.InstanceParameters.ZONE;
import static org.ow2.proactive.iaas.cloudstack.CloudStackAPI.CloudStackAPIConstants.JOB_STATE_PENDING;
import static org.ow2.proactive.iaas.cloudstack.CloudStackAPI.CloudStackAPIConstants.Response.ID;
import static org.ow2.proactive.iaas.cloudstack.CloudStackAPI.CloudStackAPIConstants.Response.JOB_ID;
import static org.ow2.proactive.iaas.cloudstack.CloudStackAPI.CloudStackAPIConstants.Response.JOB_STATUS;
import static org.ow2.proactive.iaas.cloudstack.CloudStackAPI.CloudStackAPIConstants.Response.STATE;
import static org.ow2.proactive.iaas.cloudstack.CloudStackAPI.CloudStackAPIConstants.VM_STATE_RUNNING;

/**
 * A Cloudstack Rest API client capable of starting and stopping instances.
 *
 * Most of the accepted parameters are related to the Cloudstack API, refer to it's documentation for details.
 * All the Cloudstack API dependant code and constants should be in this class.
 */
public class CloudStackAPI implements IaasApi {

    private static final String ENCODING = "UTF-8";

    private static final String SECURITY_ALGORITHM = "HmacSHA1";
    private static final int PERIOD_FOR_ASYNC_JOB_QUERIES = 5000;

    private final String apiURL;
    private final String apiKey;
    private final String secretKey;

    public CloudStackAPI(String apiUrl, String apiKey, String secretKey) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.apiURL = apiUrl;
    }

    public CloudStackAPI(Map<String, String> args) {
        this(args.get(API_URL), args.get(API_KEY), args.get(SECRET_KEY));
    }

    @Override
    public IaasInstance startInstance(Map<String, String> arguments) throws Exception {

        List<NameValuePair> params = asList(
                findArgument(SERVICE_OFFERING, arguments),
                findArgument(TEMPLATE, arguments),
                findArgument(NAME, arguments),
                new BasicNameValuePair(USER_DATA, new String(Base64.encodeBase64(findArgument(USER_DATA, arguments).getValue().getBytes(ENCODING)), ENCODING)),
                findArgument(ZONE, arguments));

        String responseString = callApi("deployVirtualMachine", params);

        String instanceId = xpath(responseString, ID);

        String jobId = xpath(responseString, JOB_ID);
        waitUntilAsynchronousOperationEnds(jobId);

        return new IaasInstance(instanceId);
    }

    @Override
    public void stopInstance(IaasInstance instance) throws Exception {

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("id", instance.getInstanceId()));

        String responseString = callApi("destroyVirtualMachine", params);

        String jobId = xpath(responseString, JOB_ID);
        waitUntilAsynchronousOperationEnds(jobId);
    }

    @Override
    public boolean isInstanceStarted(IaasInstance instance) throws Exception {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("id", instance.getInstanceId()));

        String responseString = callApi("listVirtualMachines", params);

        String instanceState = xpath(responseString, STATE);
        return VM_STATE_RUNNING.equals(instanceState);
    }

    public void attachVolume(IaasInstance instance, String diskId) throws Exception {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("id", diskId));
        params.add(new BasicNameValuePair("virtualmachineid", instance.getInstanceId()));
        String responseString = callApi("attachVolume", params);
        String jobId = xpath(responseString, JOB_ID);
        waitUntilAsynchronousOperationEnds(jobId);
    }

    public void reboot(String instanceId) throws Exception {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("id", instanceId));
        String responseString = callApi("rebootVirtualMachine", params);

        String jobId = xpath(responseString, JOB_ID);
        waitUntilAsynchronousOperationEnds(jobId);
    }

    private void waitUntilAsynchronousOperationEnds(String jobId) throws Exception {
        List<NameValuePair> params = Collections.<NameValuePair>singletonList(new BasicNameValuePair("jobid", jobId));

        while (true) {
            String responseString = callApi("queryAsyncJobResult", params);
            String jobStatus = xpath(responseString, JOB_STATUS);
            if (!JOB_STATE_PENDING.equals(jobStatus)) {
                break;
            }
            Thread.sleep(PERIOD_FOR_ASYNC_JOB_QUERIES);
        }
    }

    private String callApi(String command, List<NameValuePair> params) throws Exception {
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
        String responseBody = IOUtils.toString(content);
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new Exception("Failed to perform operation on Cloudstack API: " + responseBody);
        }
        return responseBody;
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

    private static String xpath(String xmlAsString, String xpathExpression) throws XPathExpressionException {
        InputSource source = new InputSource(new StringReader(xmlAsString));
        XPath xpath = XPathFactory.newInstance().newXPath();
        return (String) xpath.compile(xpathExpression).evaluate(source, XPathConstants.STRING);
    }

    private static NameValuePair findArgument(String key, Map<String, String> arguments) {
        return new BasicNameValuePair(key, arguments.get(key));
    }

    /**
     * Cloudstack API parameters exposed to jobs.
     */
    public class CloudStackAPIConstants {
        static final String JOB_STATE_PENDING = "0";
        static final String VM_STATE_RUNNING = "Running";

        public class ApiParameters {
            static final String API_URL= "apiurl";
            static final String API_KEY = "apikey";
            static final String SECRET_KEY= "secretkey";
        }

        public class InstanceParameters {
            static final String NAME = "name";
            static final String ZONE = "zoneid";
            static final String SERVICE_OFFERING = "serviceofferingid";
            static final String TEMPLATE = "templateid";
            static final String USER_DATA = "userdata";
        }

        class Response {
            static final String ID = "//id";
            static final String JOB_ID = "//jobid";
            static final String JOB_STATUS = "//jobstatus";
            static final String STATE = "//state";
        }
    }

    public static final Comparator<NameValuePair> NAME_VALUE_PAIR_COMPARATOR = new Comparator<NameValuePair>() {
        @Override
        public int compare(NameValuePair o1, NameValuePair o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };
}
