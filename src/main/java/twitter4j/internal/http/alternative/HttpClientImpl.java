/**
 * (c) 2003-2015 MuleSoft, Inc. The software in this package is
 * published under the terms of the CPAL v1.0 license, a copy of which
 * has been included with this distribution in the LICENSE.md file.
 */

package twitter4j.internal.http.alternative; // NOSONAR

import java.util.Map;

import twitter4j.HttpClient;
import twitter4j.HttpClientConfiguration;
import twitter4j.HttpClientFactory;
import twitter4j.HttpParameter;
import twitter4j.HttpRequest;
import twitter4j.HttpResponse;
import twitter4j.HttpResponseListener;
import twitter4j.TwitterException;
import twitter4j.auth.Authorization;


public class HttpClientImpl implements HttpClient {
    private final HttpClient client;

    public HttpClientImpl(HttpClientConfiguration conf) {
        if (HttpClientHiddenConstructionArgument.useMule()) {
            client = new MuleHttpClient(conf);
        } else {
            client = HttpClientFactory.getInstance(conf);
        }
    }

    public HttpResponse request(HttpRequest req) throws TwitterException {
        return client.request(req);
    }

    public void shutdown() {
        ((HttpClientImpl) client).shutdown();
    }

	@Override
	public void addDefaultRequestHeader(String arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public HttpResponse delete(String arg0) throws TwitterException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpResponse delete(String arg0, HttpParameter[] arg1, Authorization arg2, HttpResponseListener arg3)
			throws TwitterException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpResponse get(String arg0) throws TwitterException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpResponse get(String arg0, HttpParameter[] arg1, Authorization arg2, HttpResponseListener arg3)
			throws TwitterException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, String> getRequestHeaders() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpResponse head(String arg0) throws TwitterException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpResponse post(String arg0) throws TwitterException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpResponse post(String arg0, HttpParameter[] arg1, Authorization arg2, HttpResponseListener arg3)
			throws TwitterException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpResponse put(String arg0) throws TwitterException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpResponse put(String arg0, HttpParameter[] arg1, Authorization arg2, HttpResponseListener arg3)
			throws TwitterException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HttpResponse request(HttpRequest arg0, HttpResponseListener arg1) throws TwitterException {
		// TODO Auto-generated method stub
		return null;
	}

}
