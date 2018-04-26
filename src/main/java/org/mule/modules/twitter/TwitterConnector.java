/**
 * (c) 2003-2015 MuleSoft, Inc. The software in this package is
 * published under the terms of the CPAL v1.0 license, a copy of which
 * has been included with this distribution in the LICENSE.md file.
 */

package org.mule.modules.twitter;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.UnhandledException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.annotations.ConnectionStrategy;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.display.Placement;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;
import org.mule.api.callback.SourceCallback;
import org.mule.modules.twitter.UserEvent.EventType;
import org.mule.modules.twitter.connection.strategy.TwitterConnectionManagement;

import twitter4j.*;
import twitter4j.Query.ResultType;
import twitter4j.Query.Unit;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Twitter is an online social networking service and microblogging service that enables its users to send and read
 * text-based posts of up to 140 characters, known as "tweets".
 *
 * @author MuleSoft, Inc.
 */
@Connector(name = "twitter", schemaVersion = "2.4", description = "Twitter Integration", friendlyName = "Twitter",
        minMuleVersion = "3.6")
public class TwitterConnector {

    private static final Log logger = LogFactory.getLog(TwitterConnector.class);

    @NotNull
    @ConnectionStrategy
    private TwitterConnectionManagement connectionManagement;

    private Twitter twitter;
    private TwitterStream twitterStream;

    /**
     * Returns tweets that match a specified query.
     * <p/>
     * This method calls http://search.twitter.com/search.json
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:search}
     *
     * @param query      The search query.
     * @param lang       Restricts tweets to the given language, given by an <a href="http://en.wikipedia.org/wiki/ISO_639-1">ISO 639-1 code</a>
     * @param locale     Specify the language of the query you are sending (only ja is currently effective). This is intended for language-specific clients and the default should work in the majority of cases.
     * @param maxId      If specified, returns tweets with status ids less than the given id
     * @param since      If specified, returns tweets since the given date. Date should be formatted as YYYY-MM-DD
     * @param sinceId    Returns tweets with status ids greater than the given id.
     * @param geocode    A {@link String} containing the latitude and longitude separated by ','. Used to get the tweets by users located within a given radius of the given latitude/longitude, where the user's location is taken from their Twitter profile
     * @param radius     The radius to be used in the geocode -ONLY VALID IF A GEOCODE IS GIVEN-
     * @param unit       The unit of measurement of the given radius. Can be 'mi' or 'km'. Miles by default.
     * @param until      If specified, returns tweets with generated before the given date. Date should be formatted as YYYY-MM-DD
     * @param resultType If specified, returns tweets included popular or real time or both in the responce. Both by default. Can be 'mixed', 'popular' or 'recent'.
     * @return the {@link QueryResult}
     * @throws TwitterException when Twitter service or network is unavailable
     */
    @Processor
    public QueryResult search(String query,
                              @Optional String lang,
                              @Optional String locale,
                              @Optional Long maxId,
                              @Optional String since,
                              @Optional Long sinceId,
                              @Optional String geocode,
                              @Optional String radius,
                              @Optional String unit,
                              @Optional String until,
                              @Optional String resultType) throws TwitterException {
        final Query q = new Query(query);

        if (lang != null) {
            q.setLang(lang);
        }
        if (locale != null) {
            q.setLocale(locale);
        }
        if (maxId != null && maxId.longValue() != 0) {
            q.setMaxId(maxId.longValue());
        }

        if (since != null) {
            q.setSince(since);
        }
        if (sinceId != null && sinceId.longValue() != 0) {
            q.setSinceId(sinceId.longValue());
        }
        if (geocode != null) {
            final String[] geocodeSplit = StringUtils.split(geocode, ',');
            final double latitude = Double.parseDouble(StringUtils.replace(geocodeSplit[0], " ", ""));
            final double longitude = Double.parseDouble(StringUtils.replace(geocodeSplit[1], " ", ""));
            q.setGeoCode(new GeoLocation(latitude, longitude), Double.parseDouble(radius), Unit.valueOf(unit));
        }
        if (until != null) {
            q.setUntil(until);
        }
        if (resultType != null) {
            q.setResultType(ResultType.valueOf(resultType));
        }
        return twitter.search(q);
    }

    /**
     * Returns the 20 most recent statuses, including retweets, posted by the
     * authenticating user and that user's friends. This is the equivalent of
     * /timeline/home on the Web.<br>
     * Usage note: This home_timeline call is identical to statuses/friends_timeline,
     * except that home_timeline also contains retweets, while
     * statuses/friends_timeline does not for backwards compatibility reasons. In a
     * future version of the API, statuses/friends_timeline will be deprected and
     * replaced by home_timeline. <br>
     * This method calls http://api.twitter.com/1.1/statuses/home_timeline
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getHomeTimeline}
     *
     * @param page    Specifies the page of results to retrieve.
     * @param count   Specifies the number of records to retrieve. Must be less than or equal to 200.
     * @param sinceId Returns results with an ID greater than (that is, more recent than) the specified ID.
     *                There are limits to the number of Tweets which can be accessed through the API. If the
     *                limit of Tweets has occured since the since_id, the since_id will be forced to the
     *                oldest ID available.
     * @return list of {@link Status} of the home Timeline
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a href="http://dev.twitter.com/doc/get/statuses/home_timeline">GET
     * statuses/home_timeline | dev.twitter.com</a>
     */
    @Processor
    public ResponseList<Status> getHomeTimeline(@Placement(group = "Pagination") @Default(value = "1") int page,
                                                @Placement(group = "Pagination") @Default(value = "20") int count,
                                                @Placement(group = "Pagination") @Default(value = "-1") long sinceId)
            throws TwitterException {
        return twitter.getHomeTimeline(getPaging(page, count, sinceId));
    }

    /**
     * Returns the 20 most recent statuses posted from the authenticating user. It's
     * also possible to request another user's timeline via the id parameter.<br>
     * This is the equivalent of the Web / page for your own user, or the profile
     * page for a third party.<br>
     * For backwards compatibility reasons, retweets are stripped out of the
     * user_timeline when calling in XML or JSON (they appear with 'RT' in RSS and
     * Atom). If you'd like them included, you can merge them in from statuses
     * retweeted_by_me.<br>
     * <br>
     * This method calls http://api.twitter.com/1.1/statuses/user_timeline.json
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getUserTimelineByScreenName}
     *
     * @param screenName The screen name of the user for whom to return results for
     * @param page       Specifies the page of results to retrieve.
     * @param count      Specifies the number of tweets to try and retrieve, up to a maximum of 200. The value of count is
     *                   best thought of as a limit to the number of tweets to return because suspended or deleted content is removed
     *                   after the count has been applied.
     * @param sinceId    Returns results with an ID greater than (that is, more recent than) the specified ID. There are
     *                   limits to the number of Tweets which can be accessed through the API. If the limit of Tweets has occured since
     *                   the since_id, the since_id will be forced to the oldest ID available.
     * @return list of {@link Status} of the user Timeline
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a href="http://dev.twitter.com/doc/get/statuses/user_timeline">GET
     * statuses/user_timeline | dev.twitter.com</a>
     */
    @Processor
    public ResponseList<Status> getUserTimelineByScreenName(String screenName,
                                                            @Placement(group = "Pagination") @Default(value = "1") int page,
                                                            @Placement(group = "Pagination") @Default(value = "20") int count,
                                                            @Placement(group = "Pagination") @Default(value = "-1") long sinceId)
            throws TwitterException {
        return twitter.getUserTimeline(screenName, getPaging(page, count, sinceId));
    }

    /**
     * Returns the 20 most recent statuses posted from the authenticating user. It's
     * also possible to request another user's timeline via the id parameter.<br>
     * This is the equivalent of the Web / page for your own user, or the profile
     * page for a third party.<br>
     * For backwards compatibility reasons, retweets are stripped out of the
     * user_timeline when calling in XML or JSON (they appear with 'RT' in RSS and
     * Atom). If you'd like them included, you can merge them in from statuses
     * retweeted_by_me.<br>
     * <br>
     * This method calls http://api.twitter.com/1.1/statuses/user_timeline.json
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getUserTimelineByUserId}
     *
     * @param userId  specifies the ID of the user for whom to return the user_timeline
     * @param page    Specifies the page of results to retrieve.
     * @param count   Specifies the number of tweets to try and retrieve, up to a maximum of 200. The value of count is
     *                best thought of as a limit to the number of tweets to return because suspended or deleted content is removed
     *                after the count has been applied.
     * @param sinceId Returns results with an ID greater than (that is, more recent than) the specified ID. There are
     *                limits to the number of Tweets which can be accessed through the API. If the limit of Tweets has occured since
     *                the since_id, the since_id will be forced to the oldest ID available.
     * @return list of {@link Status} of the user Timeline
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a href="http://dev.twitter.com/doc/get/statuses/user_timeline">GET
     * statuses/user_timeline | dev.twitter.com</a>
     */
    @Processor
    public ResponseList<Status> getUserTimelineByUserId(long userId,
                                                        @Placement(group = "Pagination") @Default(value = "1") int page,
                                                        @Placement(group = "Pagination") @Default(value = "20") int count,
                                                        @Placement(group = "Pagination") @Default(value = "-1") long sinceId)
            throws TwitterException {
        return twitter.getUserTimeline(userId, getPaging(page, count, sinceId));
    }

    protected Paging getPaging(int page, int count, long sinceId) {
        Paging paging = new Paging(page, count);
        if (sinceId > 0) {
            paging.setSinceId(sinceId);
        }
        return paging;
    }


    /**
     * Returns the 20 most recent mentions (status containing @username) for the
     * authenticating user. <br>
     * This method calls http://api.twitter.com/1.1/statuses/mentions_timeline
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getMentions}
     *
     * @param page    Specifies the page of results to retrieve.
     * @param count   Specifies the number of tweets to try and retrieve, up to a maximum of 200. The value of count is
     *                best thought of as a limit to the number of tweets to return because suspended or deleted content is removed
     *                after the count has been applied.
     * @param sinceId Returns results with an ID greater than (that is, more recent than) the specified ID. There are
     *                limits to the number of Tweets which can be accessed through the API. If the limit of Tweets has occured since
     *                the since_id, the since_id will be forced to the oldest ID available.
     * @return the 20 most recent mentions ({@link Status} containing @username) for the authenticating user.
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a href="http://dev.twitter.com/doc/get/statuses/mentions">GET
     * statuses/mentions | dev.twitter.com</a>
     */
    @Processor
    public ResponseList<Status> getMentionsTimeline(@Placement(group = "Pagination") @Default(value = "1") int page,
                                                    @Placement(group = "Pagination") @Default(value = "20") int count,
                                                    @Placement(group = "Pagination") @Default(value = "-1") long sinceId)
            throws TwitterException {
        return twitter.getMentionsTimeline(getPaging(page, count, sinceId));
    }

    /**
     * Returns the 20 most recent tweets of the authenticated user that have been
     * retweeted by others. <br>
     * This method calls http://api.twitter.com/1.1/statuses/retweets_of_me
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getRetweetsOfMe}
     *
     * @param page    Specifies the page of results to retrieve.
     * @param count   Specifies the number of tweets to try and retrieve, up to a maximum of 200. The value of count is
     *                best thought of as a limit to the number of tweets to return because suspended or deleted content is removed
     *                after the count has been applied.
     * @param sinceId Returns results with an ID greater than (that is, more recent than) the specified ID. There are
     *                limits to the number of Tweets which can be accessed through the API. If the limit of Tweets has occured since
     *                the since_id, the since_id will be forced to the oldest ID available.
     * @return the 20 most recent tweets ({@link Status})of the authenticated user that have been retweeted by others.
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a href="http://dev.twitter.com/doc/get/statuses/retweets_of_me">GET
     * statuses/retweets_of_me | dev.twitter.com</a>
     */
    @Processor
    public ResponseList<Status> getRetweetsOfMe(@Placement(group = "Pagination") @Default(value = "1") int page,
                                                @Placement(group = "Pagination") @Default(value = "20") int count,
                                                @Placement(group = "Pagination") @Default(value = "-1") long sinceId)
            throws TwitterException {
        return twitter.getRetweetsOfMe(getPaging(page, count, sinceId));
    }

    /**
     * Returns a single status, specified by the id parameter below. The status's
     * author will be returned inline. <br>
     * This method calls http://api.twitter.com/1.1/statuses/show
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:showStatus}
     *
     * @param id the numerical ID of the status you're trying to retrieve
     * @return a single {@link Status}
     * @throws twitter4j.TwitterException when Twitter service or network is unavailable
     * @see <a href="http://dev.twitter.com/doc/get/statuses/show/:id">GET
     * statuses/show/:id | dev.twitter.com</a>
     */
    @Processor
    public Status showStatus(long id) throws TwitterException {
        return twitter.showStatus(id);
    }

    /**
     * Answers user information for the authenticated user
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:showUser}
     *
     * @return a {@link User} object
     * @throws TwitterException when Twitter service or network is unavailable
     */
    @Processor
    public User showUser() throws TwitterException {
        return twitter.showUser(twitter.getId());
    }

    /**
     * Returns a cursored collection of user objects for users following the specified user.<br>
     * At this time, results are ordered with the most recent following first, however, this ordering is subject to unannounced
     * change and eventual consistency issues. Results are given in groups of 20 users and multiple "pages" of results can be
     * navigated through using the next_cursor value in subsequent requests.
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getFollowers}
     *
     * @param cursor Causes the results to be broken into pages of no more than 20 records at a time.
     * @return Paginated list of followers
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a href="https://dev.twitter.com/docs/misc/cursoring">Using cursors to navigate collections</a>
     */
    @Processor
    public PagableResponseList<User> getFollowers(@Default(value = "-1") long cursor)
            throws TwitterException {
        return twitter.getFollowersList(twitter.getId(), cursor);
    }

    /**
     * Updates the authenticating user's status also know as tweeting. For each update attempt, the update text is compared
     * with the authenticating user’s recent tweets. Any attempt that would result in duplication will be blocked, resulting
     * in a 403 error. Therefore, a user cannot submit the same status twice in a row.
     * This method calls http://api.twitter.com/1.1/statuses/update
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:updateStatus}
     *
     * @param status    the text of your status update
     * @param inReplyTo The ID of an existing status that the update is in reply to.
     * @param latitude  The latitude of the location this tweet refers to. This parameter will be ignored unless it is
     *                  inside the range -90.0 to +90.0 (North is positive) inclusive.
     * @param longitude he longitude of the location this tweet refers to. The valid ranges for longitude is -180.0 to
     *                  +180.0 (East is positive) inclusive. This parameter will be ignored if outside that range or if there not a
     *                  corresponding lat parameter.
     * @return the latest {@link Status}
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a href="http://dev.twitter.com/doc/post/statuses/update">POST
     * statuses/update | dev.twitter.com</a>
     */
    @Processor
    public Status updateStatus(String status,
                               @Default(value = "-1") long inReplyTo,
                               @Placement(group = "Coordinates") @Optional Double latitude,
                               @Placement(group = "Coordinates") @Optional Double longitude) throws TwitterException {
        StatusUpdate update = new StatusUpdate(status);
        if (inReplyTo > 0) {
            update.setInReplyToStatusId(inReplyTo);
        }
        if (latitude != null && longitude != null) {
            update.setLocation(new GeoLocation(latitude, longitude));
        }
        Status response = twitter.updateStatus(update);

        //Twitter4j doesn't throw exception when json reponse has 'error: Could not authenticate with OAuth'
        if (response.getId() == -1) {
            throw new TwitterException("Could not authenticate with OAuth\n");
        }

        return response;
    }

    /**
     * Destroys the status specified by the required ID parameter.<br>
     * Usage note: The authenticating user must be the author of the specified
     * status. <br>
     * This method calls http://api.twitter.com/1.1/statuses/destroy
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:destroyStatus}
     *
     * @param statusId The ID of the status to destroy.
     * @return the deleted {@link Status}
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a href="http://dev.twitter.com/doc/post/statuses/destroy/:id">POST
     * statuses/destroy/:id | dev.twitter.com</a>
     */
    @Processor
    public Status destroyStatus(long statusId) throws TwitterException {
        return twitter.destroyStatus(statusId);
    }

    /**
     * Retweets a tweet. Returns the original tweet with retweet details embedded. <br>
     * This method calls http://api.twitter.com/1.1/statuses/retweet
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:retweetStatus}
     *
     * @param statusId The ID of the status to retweet.
     * @return the retweeted {@link Status}
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a href="http://dev.twitter.com/doc/post/statuses/retweet/:id">POST
     * statuses/retweet/:id | dev.twitter.com</a>
     */
    @Processor
    public Status retweetStatus(long statusId) throws TwitterException {
        return twitter.retweetStatus(statusId);
    }

    /**
     * Returns up to 100 of the first retweets of a given tweet. <br>
     * This method calls http://api.twitter.com/1.1/statuses/retweets
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getRetweets}
     *
     * @param statusId The numerical ID of the tweet you want the retweets of.
     * @return the retweets ({@link Status}) of a given tweet
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a href="http://dev.twitter.com/doc/get/statuses/retweets/:id">Tweets
     * Resources > statuses/retweets/:id</a>
     * @since Twitter4J 2.0.10
     */
    @Processor
    public ResponseList<Status> getRetweets(long statusId) throws TwitterException {
        return twitter.getRetweets(statusId);
    }

    /**
     * Set the OAuth verifier after it has been retrieved via requestAuthorization.
     * The resulting access tokens will be logged to the INFO level so the user can
     * reuse them as part of the configuration in the future if desired.
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:setOauthVerifier}
     *
     * @param requestToken  request token from Twitter
     * @param oauthVerifier The OAuth verifier code from Twitter.
     * @return Twitter AccessToken info.
     * @throws TwitterException when Twitter service or network is unavailable
     */
    @Processor
    public AccessToken setOauthVerifier(@Optional RequestToken requestToken, String oauthVerifier) throws TwitterException {
        AccessToken accessToken;
        if (requestToken != null) {
            accessToken = twitter.getOAuthAccessToken(requestToken, oauthVerifier);
        } else {
            accessToken = twitter.getOAuthAccessToken(oauthVerifier);
        }

        logger.info("Got OAuth access tokens. Access token:" + accessToken.getToken()
                + " Access token secret:" + accessToken.getTokenSecret());

        return accessToken;
    }

    /**
     * Start the OAuth request authorization process.
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:requestAuthorization}
     *
     * @param callbackUrl the url to be requested when the user authorizes this app
     * @return The user request token. For retrieving the authorization URL please call requestToken.getAuthorizationURL()
     * @throws TwitterException when Twitter service or network is unavailable
     */
    @Processor
    public RequestToken requestAuthorization(@Optional String callbackUrl) throws TwitterException {
        RequestToken token = twitter.getOAuthRequestToken(callbackUrl);
        return token;
    }

    /**
     * Search for places (cities and neighborhoods) that can be attached to a
     * statuses/update. Given a latitude and a longitude, return a list of all the
     * valid places that can be used as a place_id when updating a status.
     * Conceptually, a query can be made from the user's location, retrieve a list of
     * places, have the user validate the location he or she is at, and then send the
     * ID of this location up with a call to statuses/update.<br>
     * There are multiple granularities of places that can be returned --
     * "neighborhoods", "cities", etc. At this time, only United States data is
     * available through this method.
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:reverseGeoCode}
     *
     * @param latitude  latitude coordinate. Mandatory if ip is not specified
     * @param longitude longitude coordinate.
     * @return a {@link ResponseList} of {@link Place}
     * @throws TwitterException when Twitter service or network is unavailable
     */
    @Processor
    public ResponseList<Place> reverseGeoCode(@Placement(group = "Coordinates") @Optional Double latitude,
                                              @Placement(group = "Coordinates") @Optional Double longitude)
            throws TwitterException {
        return twitter.reverseGeoCode(createQuery(latitude, longitude, null));
    }

    /**
     * Search for places that can be attached to a statuses/update. Given a latitude
     * and a longitude pair, or and IP address, this request will return a list of
     * all the valid places that can be used as the place_id when updating a status.
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:searchPlaces}
     *
     * @param latitude  latitude coordinate. Mandatory if ip is not specified
     * @param longitude longitude coordinate.
     * @param ip        the ip. Mandatory if coordinates are not specified
     * @return a {@link ResponseList} of {@link Place}
     * @throws TwitterException when Twitter service or network is unavailable
     */
    @Processor
    public ResponseList<Place> searchPlaces(@Placement(group = "Coordinates") @Optional Double latitude,
                                            @Placement(group = "Coordinates") @Optional Double longitude,
                                            @Optional String ip) throws TwitterException {
        return twitter.searchPlaces(createQuery(latitude, longitude, ip));
    }

    private GeoQuery createQuery(Double latitude, Double longitude, String ip) {
        if (ip == null) {
            return new GeoQuery(new GeoLocation(latitude, longitude));
        }
        return new GeoQuery(ip);
    }

    /**
     * Find out more details of a place that was returned from the reverseGeoCode
     * operation.
     * <p/>
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getGeoDetails}
     *
     * @param id The ID of the location to query about.
     * @return a {@link Place}
     * @throws TwitterException when Twitter service or network is unavailable
     */
    @Processor
    public Place getGeoDetails(String id) throws TwitterException {
        return twitter.getGeoDetails(id);
    }

    /**
     * Locates places near the given coordinates which are similar in name.
     * Conceptually you would use this method to get a list of known places to choose from first.
     * Then, if the desired place doesn't exist, make a request to POST geo/place to create a new one.
     * The token contained in the response is the token needed to be able to create a new place.
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getSimilarPlaces}
     *
     * @param latitude        The latitude to search around. This parameter will be ignored unless it is inside the range
     *                        -90.0 to +90.0 (North is positive) inclusive. It will also be ignored if there
     *                        isn't a corresponding long parameter.
     * @param longitude       The longitude to search around. The valid ranges for longitude is -180.0 to +180.0
     *                        (East is positive) inclusive. This parameter will be ignored if outside that range,
     *                        if it is not a number, if geo_enabled is disabled, or if there not
     *                        a corresponding lat parameter.
     * @param placeName       The name a place is known as.
     * @param containedWithin This is the place_id which you would like to restrict the search results to.
     *                        Setting this value means only places within the given place_id will be found.
     * @param streetAddress   This parameter searches for places which have this given street address.
     *                        There are other well-known, and application specific attributes available.
     *                        Custom attributes are also permitted.
     * @return places
     * @throws TwitterException when Twitter service or network is unavailable
     */
    @Processor
    public List<Place> getSimilarPlaces(@Placement(group = "Coordinates") Double latitude,
                                          @Placement(group = "Coordinates") Double longitude,
                                          String placeName, @Optional String containedWithin,
                                          @Optional String streetAddress) throws TwitterException {
        return twitter.getSimilarPlaces(new GeoLocation(latitude, longitude),
                placeName, containedWithin, streetAddress);
    }

    /**
     * Returns the sorted locations that Twitter has trending topic information for.
     * The response is an array of &quot;locations&quot; that encode the location's WOEID
     * (a <a href="http://developer.yahoo.com/geo/geoplanet/">Yahoo! Where On Earth ID</a>)
     * and some other human-readable information such as a canonical name and country the
     * location belongs in.
     * <br>The available trend locations will be sorted by distance to the lat
     * and long passed in. The sort is nearest to furthest.
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getAvailableTrends}
     *
     * @return the {@link Location}s
     * @throws TwitterException when Twitter service or network is unavailable
     */
    @Processor
    public ResponseList<Location> getAvailableTrends()
            throws TwitterException {

        return twitter.getAvailableTrends();
    }

    /**
     * Asynchronously retrieves public statuses that match one or more filter predicates.
     * <p/>
     * At least a keyword or userId must be specified. Multiple parameters may be
     * specified.
     * <p/>
     * Placing long parameters in the URL may cause the request to be rejected for excessive URL length.
     * <p/>
     * The default access level allows up to 200 track keywords and 400 follow userids.
     * <p/>
     * Only one Twitter stream can be consumed using the same credentials. As a consequence,
     * only one twitter stream can be consumed per connector instance.
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:filteredStream}
     *
     * @param count    the number of previous statuses to stream before transitioning to the live stream.
     * @param userIds  the user ids to follow
     * @param keywords the keywords to track
     * @param callback the {@link SourceCallback} used to dispatch messages when a response is received
     */
    @Source
    public void filteredStream(@Default("0") int count,
                               @Placement(group = "User Ids to Follow") @Optional List<String> userIds,
                               @Placement(group = "Keywords to Track") @Optional List<String> keywords,
                               final SourceCallback callback) {
        listenToStatues(callback).filter(new FilterQuery(count, toLongArray(userIds), toStringArray(keywords)));
    }

    /**
     * Asynchronously retrieves a random sample of all public statuses. The sample
     * size and quality varies depending on the account permissions
     * <p/>
     * The default access level provides a small proportion of the Firehose. The "Gardenhose"
     * access level provides a proportion more suitable for data mining
     * and research applications that desire a larger proportion to be
     * statistically significant sample.
     * <p/>
     * Only one Twitter stream can be consumed using the same credentials. As a consequence,
     * only one twitter stream can be consumed per connector instance.
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:sampleStream}
     *
     * @param callback the {@link SourceCallback} used to dispatch messages when a response is received
     */
    @Source
    public void sampleStream(final SourceCallback callback) {
        listenToStatues(callback).sample();
    }

    /**
     * Asynchronously retrieves all public statuses. This stream is not generally
     * available - it requires special permissions and its usage is discouraged by
     * Twitter
     * <p/>
     * Only one Twitter stream can be consumed using the same credentials. As a consequence,
     * only one twitter stream can be consumed per connector instance.
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:firehoseStream}
     *
     * @param count    Indicates the number of previous statuses to consider for delivery before transitioning to live
     *                 stream delivery.
     * @param callback the {@link SourceCallback} used to dispatch messageswhen a response is received
     */
    @Source
    public void firehoseStream(int count, final SourceCallback callback) {
        listenToStatues(callback).firehose(count);
    }

    /**
     * Asynchronously retrieves all statuses containing 'http:' and 'https:'. Like
     * Firehorse, its is not a generally available stream
     * <p/>
     * Only one Twitter stream can be consumed using the same credentials. As a consequence,
     * only one twitter stream can be consumed per connector instance.
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:linkStream}
     *
     * @param count    Indicates the number of previous statuses to consider for delivery before transitioning to live
     *                 stream delivery.
     * @param callback the {@link SourceCallback} used to dispatch messages when a response is received
     */
    @Source
    public void linkStream(int count, final SourceCallback callback) {
        listenToStatues(callback).links(count);
    }

    /**
     * Retrieves the following user updates notifications:<br/>
     * - New Statuses <br/>
     * - Block/Unblock events <br/>
     * - Follow events <br/>
     * - User profile updates <br/>
     * - Retweets <br/>
     * - List creation/deletion <br/>
     * - List member addition/remotion <br/>
     * - List subscription/unsubscription <br/>
     * - List updates <br/>
     * - Profile updates <br/>
     * <p/>
     * Such notifications are represented as UserEvent objects
     * <p/>
     * Only one Twitter stream can be consumed using the same credentials. As a consequence,
     * only one twitter stream can be consumed per connector instance.
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:userStream}
     *
     * @param keywords  the keywords to track for new statuses
     * @param callback_ the {@link SourceCallback} used to dispatch messages when a response is received
     */
    @Source
    public void userStream(@Placement(group = "Keywords to Track") List<String> keywords, final SourceCallback callback_) {
        initStream();
        final SoftCallback callback = new SoftCallback(callback_);
        twitterStream.addListener(new UserStreamAdapter() {
            @Override
            public void onException(Exception ex) {
                logger.warn("An exception occured while processing user stream", ex);
            }

            @Override
            public void onStatus(Status status) {
                try {
                    callback.process(UserEvent.fromPayload(EventType.NEW_STATUS, status.getUser(), status));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

            @Override
            public void onBlock(User source, User blockedUser) {
                try {
                    callback.process(UserEvent.fromTarget(EventType.BLOCK, source, blockedUser));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

            @Override
            public void onFollow(User source, User followedUser) {
                try {
                    callback.process(UserEvent.fromTarget(EventType.FOLLOW, source, followedUser));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

            @Override
            public void onUnblock(User source, User unblockedUser) {
                try {
                    callback.process(UserEvent.fromTarget(EventType.UNBLOCK, source, unblockedUser));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

            @Override
            public void onUserListCreation(User listOwner, UserList list) {
                try {
                    callback.process(UserEvent.fromPayload(EventType.LIST_CREATION, listOwner, list));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

            @Override
            public void onUserListDeletion(User listOwner, UserList list) {
                try {
                    callback.process(UserEvent.fromPayload(EventType.LIST_DELETION, listOwner, list));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

            @Override
            public void onUserListMemberAddition(User addedMember, User listOwner, UserList list) {
                try {
                    callback.process(UserEvent.from(EventType.LIST_MEMBER_ADDITION, addedMember, listOwner, list));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

            @Override
            public void onUserListMemberDeletion(User deletedMember, User listOwner, UserList list) {
                try {
                    callback.process(UserEvent.from(EventType.LIST_MEMBER_DELETION, deletedMember, listOwner,
                            list));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

            @Override
            public void onUserListSubscription(User subscriber, User listOwner, UserList list) {
                try {
                    callback.process(UserEvent.from(EventType.LIST_SUBSCRIPTION, subscriber, listOwner, list));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

            @Override
            public void onUserListUnsubscription(User subscriber, User listOwner, UserList list) {
                try {
                    callback.process(UserEvent.from(EventType.LIST_UNSUBSCRIPTION, subscriber, listOwner, list));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

            @Override
            public void onUserListUpdate(User listOwner, UserList list) {
                try {
                    callback.process(UserEvent.fromPayload(EventType.LIST_UPDATE, listOwner, list));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

            @Override
            public void onUserProfileUpdate(User updatedUser) {
                try {
                    callback.process(UserEvent.fromPayload(EventType.PROFILE_UPDATE, updatedUser, null));
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        });
        twitterStream.user(toStringArray(keywords));
    }


    /**
     * Asynchronously retrieves statutes for a set of supplied user's ids.
     * Site Streams are a beta service, so refer always to latest twitter documentation about them.
     * <p/>
     * Only one Twitter stream can be consumed using the same credentials. As a consequence,
     * only one twitter stream can be consumed per connector instance.
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:siteStream}
     *
     * @param userIds        ids of users to include in the stream
     * @param withFollowings withFollowings whether to receive status updates from people following
     * @param callback_      the {@link SourceCallback} used to dispatch messages when a response is received
     */
    @Source
    public void siteStream(@Placement(group = "User Ids to Follow") List<String> userIds,
                           @Default("false") boolean withFollowings,
                           final SourceCallback callback_) {
        initStream();
        final SoftCallback callback = new SoftCallback(callback_);
        twitterStream.addListener(new SiteStreamsAdapter() {

            @Override
            public void onException(Exception ex) {
                logger.warn("An exception occured while processing site stream", ex);
            }

            @Override
            public void onStatus(long forUser, Status status) {
                try {
                    callback.process(status);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }

        });
        twitterStream.site(withFollowings, toLongArray(userIds));
    }

    /**
     * Sends a new direct message to the specified user from the authenticating user.
     * Requires both the user and text parameters below. The text will be trimmed if
     * the length of the text is exceeding 140 characters.
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:sendDirectMessageByScreenName}
     *
     * @param screenName The screen name of the user to whom send the direct message
     * @param message    The text of your direct message
     * @return the {@link DirectMessage}
     * @throws TwitterException when Twitter service or network is unavailable
     */
    @Processor
    public DirectMessage sendDirectMessageByScreenName(String screenName, String message) throws TwitterException {
        return twitter.sendDirectMessage(screenName, message);
    }

    /**
     * Sends a new direct message to the specified user from the authenticating user.
     * Requires both the user and text parameters below. The text will be trimmed if
     * the length of the text is exceeding 140 characters.
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:sendDirectMessageByUserId}
     *
     * @param userId  The user ID of the user to whom send the direct message
     * @param message The text of your direct message
     * @return the {@link DirectMessage}
     * @throws TwitterException when Twitter service or network is unavailable
     */
    @Processor
    public DirectMessage sendDirectMessageByUserId(long userId, String message) throws TwitterException {
        return twitter.sendDirectMessage(userId, message);
    }

    /**
     * Returns the top 10 trending topics for a specific WOEID, if trending information is available for it.<br>
     * The response is an array of "trend" objects that encode the name of the trending topic, the query parameter that can be used to search for the topic on <a href="http://search.twitter.com/">Twitter Search</a>, and the Twitter Search URL.<br>
     * This information is cached for 5 minutes. Requesting more frequently than that will not return any more data, and will count against your rate limit usage.<br>
     * <br>This method calls http://api.twitter.com/1.1/trends/place.json
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getPlaceTrends}
     *
     * @param woeid <a href="http://developer.yahoo.com/geo/geoplanet/">The Yahoo! Where On Earth ID</a> of the location to return trending information for. Global information is available by using 1 as the WOEID.
     * @return trends
     * @throws twitter4j.TwitterException when Twitter service or network is unavailable
     */
    @Processor
    public Trends getPlaceTrends(@Default("1") int woeid) throws TwitterException {
        return twitter.getPlaceTrends(woeid);
    }

    /**
     * Returns the locations that Twitter has trending topic information for, closest to a specified location.<br>
     * The response is an array of "locations" that encode the location's WOEID and some other human-readable information such as a canonical name and country the location belongs in.<br>
     * A WOEID is a <a href="http://developer.yahoo.com/geo/geoplanet/">Yahoo! Where On Earth ID</a>.
     * <br>This method calls http://api.twitter.com/1.1/trends/closest.json
     * <p/>
     * {@sample.xml ../../../doc/twitter-connector.xml.sample twitter:getClosestTrends}
     *
     * @param latitude  The latitude of the location this tweet refers to. This parameter will be ignored unless it is
     *                  inside the range -90.0 to +90.0 (North is positive) inclusive.
     * @param longitude he longitude of the location this tweet refers to. The valid ranges for longitude is -180.0 to
     *                  +180.0 (East is positive) inclusive. This parameter will be ignored if outside that range or if there not a
     *                  corresponding lat parameter.
     * @return the locations
     * @throws TwitterException twitter4j.TwitterException when Twitter service or network is unavailable
     */
    @Processor
    public ResponseList<Location> getClosestTrends(double latitude, double longitude) throws TwitterException {
        return twitter.getClosestTrends(new GeoLocation(latitude, longitude));
    }

    private void initStream() {
        if (twitterStream != null) {
            throw new IllegalStateException("Only one stream can be consumed per twitter account");
        }
        this.twitterStream = getConnectionManagement().newStream();
    }

    private String[] toStringArray(List<String> list) {
        if (list == null) {
            return new String[0];
        }
        return list.toArray(new String[list.size()]);
    }

    private TwitterStream listenToStatues(final SourceCallback callback_) {
        initStream();
        final SoftCallback callback = new SoftCallback(callback_);
        twitterStream.addListener(new StatusAdapter() {
            @Override
            public void onException(Exception ex) {
                logger.warn("An exception occured while processing status stream", ex);
            }

            @Override
            public void onStatus(Status status) {
                try {
                    callback.process(status);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        });
        return twitterStream;
    }

    private long[] toLongArray(List<String> longList) {
        if (longList == null) {
            return new long[0];
        }
        long[] ls = new long[longList.size()];
        for (int i = 0; i < longList.size(); i++) {
            ls[i] = Long.parseLong(longList.get(i));
        }
        return ls;
    }

    public TwitterConnectionManagement getConnectionManagement() {
        return connectionManagement;
    }

    public void setConnectionManagement(TwitterConnectionManagement connectionManagement) {
        this.connectionManagement = connectionManagement;
        this.twitter = connectionManagement.getTwitter();
    }

    static final class SoftCallback implements SourceCallback {
        private final SourceCallback callback;

        public SoftCallback(SourceCallback callback) {
            this.callback = callback;
        }

        public Object process() throws Exception {
            try {
                return callback.process();
            } catch (Exception e) {
                throw new UnhandledException(e);
            }
        }

        public Object process(Object payload) {
            try {
                return callback.process(payload);
            } catch (Exception e) {
                throw new UnhandledException(e);
            }
        }

        public Object process(Object payload, Map<String, Object> properties) throws Exception {
            try {
                return callback.process(payload);
            } catch (Exception e) {
                throw new UnhandledException(e);
            }
        }

        public MuleEvent processEvent(MuleEvent event) throws MuleException {
            try {
                return callback.processEvent(event);
            } catch (Exception e) {
                throw new UnhandledException(e);
            }
        }
    }
}
