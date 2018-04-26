/**
 * (c) 2003-2015 MuleSoft, Inc. The software in this package is
 * published under the terms of the CPAL v1.0 license, a copy of which
 * has been included with this distribution in the LICENSE.md file.
 */

package org.mule.modules.twitter;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;

import twitter4j.HttpClientConfiguration;
import twitter4j.HttpResponse;
import twitter4j.TwitterException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class MuleHttpResponse extends HttpResponse {

    private final MuleMessage response;

    public MuleHttpResponse(HttpClientConfiguration conf, MuleMessage response) throws TwitterException {
        super(conf);
        this.response = response;
        try {
            this.is = response.getPayload(InputStream.class);
        } catch (TransformerException e) {
            throw new TwitterException(e);
        }
    }

    @Override
    public String getResponseHeader(String name) {
        return response.getInboundProperty(name);
    }

    @Override
    public Map<String, List<String>> getResponseHeaderFields() {
        throw new IllegalStateException();
    }

    @Override
    public void disconnect() throws IOException { // NOSONAR
    }

}
