/**
 * (c) 2003-2015 MuleSoft, Inc. The software in this package is
 * published under the terms of the CPAL v1.0 license, a copy of which
 * has been included with this distribution in the LICENSE.md file.
 */

package org.mule.modules.twitter.connection.strategy;


import org.mule.api.ConnectionException;
import org.mule.api.ConnectionExceptionCode;
import org.mule.api.MuleContext;
import org.mule.api.annotations.*;
import org.mule.api.annotations.components.ConnectionManagement;
import org.mule.api.annotations.display.FriendlyName;
import org.mule.api.annotations.display.Password;
import org.mule.api.annotations.display.Placement;
import org.mule.api.annotations.param.ConnectionKey;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;
import org.mule.api.context.MuleContextAware;
import twitter4j.*;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.internal.http.alternative.HttpClientHiddenConstructionArgument;
import twitter4j.internal.http.alternative.MuleHttpClient;

@ConnectionManagement(friendlyName = "Configuration")
public class TwitterConnectionManagement implements MuleContextAware {

    private Twitter twitter;

    /**
     * Whether to use SSL in API calls to Twitter
     */
    @Configurable
    @Default("true")
    @FriendlyName("Use SSL")
    private boolean useSSL;

    /**
     * Proxy host
     */
    @Configurable
    @Optional
    @Placement(group = "Proxy settings", tab = "Proxy")
    private String proxyHost;

    /**
     * Proxy port
     */
    @Configurable
    @Default("-1")
    @Placement(group = "Proxy settings", tab = "Proxy")
    private int proxyPort;

    /**
     * Proxy username
     */
    @Configurable
    @Optional
    @Placement(group = "Proxy settings", tab = "Proxy")
    private String proxyUsername;

    /**
     * Proxy password
     */
    @Configurable
    @Optional
    @Placement(group = "Proxy settings", tab = "Proxy")
    @Password
    private String proxyPassword;

    /**
     * Twitter Stream Base Url
     */
    @Configurable
    @Default("https://stream.twitter.com/1.1/")
    @Placement(group = "Streaming settings", tab = "Streaming")
    private String streamBaseUrl;

    /**
     * Twitter Site Stream Base Url
     */
    @Configurable

    @Default("https://sitestream.twitter.com/1.1/")
    @Placement(group = "Streaming settings", tab = "Streaming")
    private String siteStreamBaseUrl;

    private String connectionId;

    private Configuration config;

    /**
     * Connects to Twitter
     *
     * @param consumerKey       The consumer key used by this application
     * @param consumerSecret    The consumer secret by this application
     * @param accessToken       The access token provided by Twitter OAuth tool.
     * @param accessTokenSecret The access token secret provided by Twitter OAuth tool.
     * @throws org.mule.api.ConnectionException when the connection fails
     */
    @Connect
    @TestConnectivity
    public void connect(@ConnectionKey String consumerKey, String consumerSecret, @ConnectionKey String accessToken, String accessTokenSecret) throws ConnectionException {
        this.config = new ConfigurationBuilder()
        		.setTweetModeExtended(true)
                .setOAuthConsumerKey(consumerKey)
                .setOAuthConsumerSecret(consumerSecret)
                .setOAuthAccessToken(accessToken)
                .setOAuthAccessTokenSecret(accessTokenSecret)
                .setStreamBaseURL(getStreamBaseUrl())
                .setSiteStreamBaseURL(getSiteStreamBaseUrl())
                .setHttpProxyHost(getProxyHost())
                .setHttpProxyPort(getProxyPort())
                .setHttpProxyUser(getProxyUsername())
                .setHttpProxyPassword(getProxyPassword()).build();

        HttpClientHiddenConstructionArgument.setUseMule(true);
        twitter = new TwitterFactory(config).getInstance();

        //for connectivity testing
        try {
            twitter.getRateLimitStatus();

        } catch (TwitterException te) {
            throw new ConnectionException(ConnectionExceptionCode.UNKNOWN, null, "Bad credentials", te);
        }

        this.connectionId = consumerKey + "-" + accessToken;
    }

    @Disconnect
    public void disconnect() {
        twitter = null;
    }

    @ValidateConnection
    public boolean validateConnection() {
        return twitter != null;
    }

    @ConnectionIdentifier
    public String getConnectionIdentifier() {
        return this.connectionId;
    }

    public TwitterStream newStream() {
        HttpClientHiddenConstructionArgument.setUseMule(false);
        return new TwitterStreamFactory(config).getInstance();
    }

    public String getStreamBaseUrl() {
        return streamBaseUrl;
    }

    public void setStreamBaseUrl(String streamBaseUrl) {
        this.streamBaseUrl = streamBaseUrl;
    }

    public String getSiteStreamBaseUrl() {
        return siteStreamBaseUrl;
    }

    public void setSiteStreamBaseUrl(String siteStreamBaseUrl) {
        this.siteStreamBaseUrl = siteStreamBaseUrl;
    }

    public boolean isUseSSL() {
        return useSSL;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getProxyUsername() {
        return proxyUsername;
    }

    public void setProxyUsername(String proxyUsername) {
        this.proxyUsername = proxyUsername;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    public boolean getUseSSL() {
        return useSSL;
    }

    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

    public void setMuleContext(MuleContext context) {
        MuleHttpClient.setMuleContext(context);
    }

    public Twitter getTwitter() {
        return twitter;
    }
}
