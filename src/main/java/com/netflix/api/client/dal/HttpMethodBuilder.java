package com.netflix.api.client.dal;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import net.oauth.OAuth;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.api.NetflixAPIException;
import com.netflix.api.NetflixAPIResponse;
import com.netflix.api.client.APIEndpoints;
import com.netflix.api.client.NetflixAPIClient;
import com.netflix.api.client.NetflixAPICustomer;
import com.netflix.api.client.oauth.OAuthAccessToken;
import com.netflix.api.client.oauth.OAuthRequestToken;
import com.netflix.api.client.oauth.OAuthUtils;

/**
 * Builds methods for making HTTP calls to the Netflix API.
 * 
 * @author jharen
 */
public class HttpMethodBuilder
{
	private static final Logger logger = LoggerFactory.getLogger(HttpMethodBuilder.class);
	
	/**
	 * Backreference to parent APIClient.
	 */
	private NetflixAPIClient netflixAPIClient;
	
	/**
	 * Makes http calls and returns results.
	 */
	private HttpClient httpClient = null;
	
	public HttpMethodBuilder(NetflixAPIClient netflixAPIClient)
	{
		this.netflixAPIClient = netflixAPIClient;
		this.httpClient = this.netflixAPIClient.getHttpClient();
	}
	
	/**
	 * @param props  
	 */
	public HttpMethodBuilder(NetflixAPIClient netflixAPIClient, Properties props)
	{
		this.netflixAPIClient = netflixAPIClient;
		this.httpClient = this.netflixAPIClient.getHttpClient();
	}
	
    /**
     * Creates a Map of OAuth key/value pairs preset
     * with default values.
     * @return
     */
    public Map<String, String> getDefaultOAuthParameters()
    {
    	HashMap<String, String> parameters = new HashMap<String, String>();
    	parameters.put("oauth_consumer_key", this.netflixAPIClient.getConsumerKey());
    	parameters.put("oauth_timestamp", OAuthUtils.getNewOAuthTimeStamp());
    	parameters.put("oauth_nonce", OAuthUtils.getNewNonceValue());
    	parameters.put("oauth_signature_method", this.netflixAPIClient.getSignatureMethod());
    	parameters.put("oauth_version", this.netflixAPIClient.getOauthVersion());
    	return parameters;
    }
	
	/**
     * Builds a Http GET Method ready for execution.  <br />
     * The GET method returned has been preset with the 
     * "consumer key only" level authorization (see 
     * http://developer.netflix.com/docs/Security#0_18325 for more info).
     * The GETs hereby returned are applicable for the the autocomplete 
     * resource of the REST API and the Javascript API.
     * 
     * @param uri - the resource URI to call.
     * @param parameters - map of request parameters to send in request.
     * @return - GET method with valid auth header set. 
     */
	public GetMethod buildConsumerKeyedGetMethod(String uri, Map<String, String> parameters) throws Exception
	{
    	GetMethod method = this.newGetMethod(uri);
    	method.setDoAuthentication(false);
    	String queryString = this.createNormalizedQueryString((HashMap<String, String>) parameters);
    	
    	method.setQueryString(queryString);
    	
    	if (logger.isDebugEnabled())
    	{
    		String message = "Created method [ GET " + uri 
    		+ "\n" + queryString + " ]";
    		logger.debug(message);
    	}
    	return method;
    }

	/**
     * Builds a Http GET Method ready for execution.  <br />
     * The GET method returned has been preset with the 
     * "consumer key + signature" level authorization (see 
     * http://developer.netflix.com/docs/Security#0_18325 for more info).
     * The GETs hereby returned are applicable for calls NOT requiring
     * user auth, and all the back-channel talk between the service provider
     * and consumer applications.
     * 
     * @param uri - the resource URI to call.
     * @param parameters - map of request parameters to send in request.
     * @return - GET method with valid auth header set. 
     * @throws Exception - if signature generation fails
     */
    public GetMethod buildConsumerSignedGetMethod(String uri, Map<String, String> parameters) throws Exception
    {
    	GetMethod method = this.newGetMethod(uri);
    	method.setDoAuthentication(true);
    	
    	String signatureBaseString = OAuthUtils.getSignatureBaseString("GET", uri, parameters);
    	String signatureParameter = OAuthUtils.getHMACSHASignature(signatureBaseString, this.netflixAPIClient.getConsumerSecret(), null);
    	
    	parameters.put("oauth_signature",signatureParameter);
    	String authHeader = this.createAuthorizationHeader((HashMap<String, String>) parameters);
    	method.setQueryString(this.createNonOAuthQueryString(parameters));
    	method.setRequestHeader("Authorization", authHeader);
    	
    	if (logger.isDebugEnabled())
    	{
    		String message = "Created method [ GET " + uri 
    		+ "\n Authorization: " +
    				authHeader + " ]";
    		logger.debug(message);
    	}
    	
    	return method;
    }
    
	/**
     * Builds a Http GET Method ready for execution.  <br />
     * The GET method returned has been preset with the 
     * "consumer key + signature" level authorization (see 
     * http://developer.netflix.com/docs/Security#0_18325 for more info).
     * The GETs hereby returned are applicable for calls NOT requiring
     * user auth, and all the back-channel talk between the service provider
     * and consumer applications.
     * 
     * @param uri - the resource URI to call.
     * @param parameters - map of request parameters to send in request.
     * @return - GET method with aut params encoding as url parameters. 
     * @throws Exception - if signature generation fails
     */
    public GetMethod buildConsumerSignedGetMethodWithQueryString(String uri, Map<String, String> parameters) throws Exception
    {
    	GetMethod method = this.newGetMethod(uri);
    	method.setDoAuthentication(true);
    	
    	String signatureBaseString = OAuthUtils.getSignatureBaseString("GET", uri, parameters);
    	String signatureParameter = OAuthUtils.getHMACSHASignature(signatureBaseString, this.netflixAPIClient.getConsumerSecret(), null);
    	
    	parameters.put("oauth_signature", signatureParameter);
    	String queryString = this.createNormalizedQueryString((HashMap<String, String>) parameters);
    	
    	method.setQueryString(queryString);
    	
    	if (logger.isDebugEnabled())
    	{
    		String message = "Created method [ GET " + uri 
    		+ "?" + queryString + " ]";
    		logger.debug(message);
    	}
    	
    	return method;
    }
    
	/**
     * Builds a Http POST Method ready for execution.  <br />
     * The POST method returned has been preset with the 
     * "consumer key + signature" level authorization (see 
     * http://developer.netflix.com/docs/Security#0_18325 for more info).
     * The POSTS hereby returned are applicable for calls NOT requiring
     * user auth, and all the back-channel talk between the service provider
     * and consumer applications.
     * 
     * @param uri - the resource URI to call.
     * @param parameters - map of request parameters to send in request.
     * @return - POST method with valid auth header set. 
     * @throws Exception - if signature generation fails
     */
    public PostMethod buildConsumerSignedPostMethod(String uri, Map<String, String> parameters) throws Exception
    {
    	PostMethod method = this.newPostMethod(uri);
    	method.setDoAuthentication(true);
    	
    	String signatureBaseString = OAuthUtils.getSignatureBaseString("POST", uri, parameters);
    	String signatureParameter = OAuthUtils.getHMACSHASignature(signatureBaseString, this.netflixAPIClient.getConsumerSecret(), null);
    	
    	parameters.put("oauth_signature",signatureParameter);
    	String authHeader = this.createAuthorizationHeader((HashMap<String, String>) parameters);
    	this.setNonOauthParams(method, parameters);
    	method.setRequestHeader("Authorization", authHeader);
    	
    	if (logger.isDebugEnabled())
    	{
    		String message = "Created method [ POST " + uri 
    		+ "\n Authorization: " +
    				authHeader + " ]";
    		logger.debug(message);
    	}
    	
    	return method;
    }
    
    /**
     * Builds a Http GET Method ready for execution.  <br />
     * The GET returned has a valid, resource-specific access token
     * pre-loaded and ready to go.
     * 
     * @param uri
     * @param parameters
     * @param customer
     * @return
     * @throws Exception
     */
    public GetMethod buildCustomerAuthorizedGetMethod(String uri, Map<String, String> parameters, NetflixAPICustomer customer) throws Exception
    {
    	OAuthAccessToken accessToken = customer.getAccessToken();
    	if (accessToken == null)
    		throw new NetflixAPIException("Customer has no access token.");
    	return this.buildConsumerSignedGetMethodWithAccessSecret(uri, parameters, accessToken);
    }

    /**
     * Builds a Http POST Method ready for execution.  <br />
     * The POST returned has a valid, resource-specific access token
     * pre-loaded and ready to go.
     * 
     * @param uri
     * @param parameters
     * @param customer
     * @return
     * @throws Exception
     */
    public PostMethod buildCustomerAuthorizedPostMethod(String uri, Map<String, String> parameters, NetflixAPICustomer customer) throws Exception
    {
    	OAuthAccessToken accessToken = customer.getAccessToken();
    	if (accessToken == null)
    		throw new NetflixAPIException("Customer has no access token.");
    	return this.buildConsumerSignedPostMethodWithAccessSecret(uri, parameters, accessToken);
    }
    
    /**
     * Builds a Http DELETE Method ready for execution.  <br />
     * The DELETE returned has a valid, resource-specific access token
     * pre-loaded and ready to go.
     * 
     * @param uri
     * @param parameters
     * @param customer
     * @return
     * @throws Exception
     */
    public DeleteMethod buildCustomerAuthorizedDeleteMethod(String uri, Map<String, String> parameters, NetflixAPICustomer customer) throws Exception
    {
    	OAuthAccessToken accessToken = customer.getAccessToken();
    	if (accessToken == null)
    		throw new NetflixAPIException("Customer has no access token.");
    	return this.buildConsumerSignedDeleteMethodWithAccessSecret(uri, parameters, accessToken);
    }
    
    /**
     * Builds a Http DELETE Method ready for execution.  <br />
     * The DELETE returned has a valid, resource-specific access token
     * pre-loaded and ready to go.
     * 
     * @param uri
     * @param parameters
     * @param customer
     * @return
     * @throws Exception
     */
    public DeleteMethod buildCustomerAuthorizedDeleteMethod(String uri, Map<String, String> parameters, NetflixAPICustomer customer, 
    		Map<String, String> requestHeaders) throws Exception
    {
    	OAuthAccessToken accessToken = customer.getAccessToken();
    	if (accessToken == null)
    		throw new NetflixAPIException("Customer has no access token.");
    	return (DeleteMethod) this.applyRequestHeadersToMethod(this.buildConsumerSignedDeleteMethodWithAccessSecret(uri, parameters, accessToken), requestHeaders);
    }
    
    /**
     * Builds a Http GET Method ready for execution.  <br />
     * The GET returned has a valid, resource-specific access token
     * pre-loaded and ready to go.
     * 
     * @param uri
     * @param parameters
     * @param customer
     * @return
     * @throws Exception
     */
    public GetMethod buildCustomerAuthorizedGetMethod(String uri, Map<String, String> parameters, NetflixAPICustomer customer,
    		Map<String, String> requestHeaders) throws Exception
    {
    	OAuthAccessToken accessToken = customer.getAccessToken();
    	if (accessToken == null)
    		throw new NetflixAPIException("Customer has no access token.");
    	return (GetMethod) this.applyRequestHeadersToMethod(this.buildConsumerSignedGetMethodWithAccessSecret(uri, parameters, accessToken), requestHeaders);
    }

    /**
     * Builds a Http POST Method ready for execution.  <br />
     * The POST returned has a valid, resource-specific access token
     * pre-loaded and ready to go.
     * 
     * @param uri
     * @param parameters
     * @param customer
     * @return
     * @throws Exception
     */
    public PostMethod buildCustomerAuthorizedPostMethod(String uri, Map<String, String> parameters, NetflixAPICustomer customer,
    		Map<String, String> requestHeaders) throws Exception
    {
    	OAuthAccessToken accessToken = customer.getAccessToken();
    	if (accessToken == null)
    		throw new NetflixAPIException("Customer has no access token.");
    	return (PostMethod) this.applyRequestHeadersToMethod(this.buildConsumerSignedPostMethodWithAccessSecret(uri, parameters, accessToken), requestHeaders);
    }
    
	/**
	 * Retrieves an access token for the specified user, using their Netflix credentials.<br />
	 * Note: this method requires the user give you their username and password.  It is against
	 * the Netflix TOS and as such is deprecated.  In other words, using this can get you busted.
	 * DON'T USE THIS!
	 * @param customer
	 * @return
	 * @throws Exception
	 */
    @Deprecated
	public OAuthAccessToken getAccessTokenFromServiceWithUserCredentials(NetflixAPICustomer customer) throws Exception
	{
		OAuthAccessToken accessToken;
		OAuthRequestToken requestToken = this.getNewRequestToken();
		this.authorizeRequestToken(requestToken, customer);
		accessToken = this.exchangeRequestForAccessToken(requestToken);
		customer.setAccessToken(accessToken);
		return accessToken;
	}
    
    /**
     * Builds the method used to obtain access tokens.
     * @param uri
     * @param parameters
     * @return GetMethod - method that when called will return an access token.
     * @throws Exception
     */
    private GetMethod buildAccessTokenRequestMethod(String uri, Map<String, String> parameters, OAuthRequestToken authorizedRequestToken) throws Exception
    {
    	GetMethod method = (GetMethod) this.newGetMethod(uri);
    	method.setDoAuthentication(true);
    	
    	String signatureBaseString = OAuthUtils.getSignatureBaseString("GET", uri, parameters);
    	String signatureParameter = OAuthUtils.getHMACSHASignature(signatureBaseString, this.netflixAPIClient.getConsumerSecret(), authorizedRequestToken.getTokenSecret());
    	
    	parameters.put("oauth_signature", signatureParameter);
    	String queryString = this.createNormalizedQueryString((HashMap<String, String>) parameters);
    	
    	method.setQueryString(queryString);
    	
    	if (logger.isDebugEnabled())
    	{
    		String message = "Created method [ GET " + uri 
    		+ "?" + queryString + " ]";
    		logger.debug(message);
    	}
    	
    	return method;
    }
    
    /**
     * Builds a GetMethod suitable for full-security OAuth requests.
     * @param uri
     * @param parameters
     * @return GetMethod 
     * @throws Exception
     */
    protected GetMethod buildConsumerSignedGetMethodWithAccessSecret(String uri, Map<String, String> parameters, OAuthAccessToken accessToken) throws Exception
    {
    	GetMethod method = (GetMethod) this.newGetMethod(uri);
    	method.setDoAuthentication(true);
    	
    	parameters.put("oauth_timestamp", OAuthUtils.getNewOAuthTimeStamp());
    	parameters.put("oauth_nonce", OAuthUtils.getNewNonceValue());
    	parameters.put("oauth_token", accessToken.getTokenText());
    	
    	String signatureBaseString = OAuthUtils.getSignatureBaseString("GET", uri, parameters);
    	String signatureParameter = OAuthUtils.getHMACSHASignature(signatureBaseString, this.netflixAPIClient.getConsumerSecret(), accessToken.getTokenSecret());
    	
    	parameters.put("oauth_signature", signatureParameter);
    	String queryString = this.createNormalizedQueryString((HashMap<String, String>) parameters);
    	
    	method.setQueryString(queryString);
    	
    	if (logger.isDebugEnabled())
    	{
    		String message = "Created method [ GET " + uri 
    		+ "?" + queryString + " ]";
    		logger.debug(message);
    	}
    	
    	return method;
    }
    
    /**
     * Builds a PostMethod suitable for full-security OAuth requests.
     * @param uri
     * @param parameters
     * @return
     * @throws Exception
     */
    protected PostMethod buildConsumerSignedPostMethodWithAccessSecret(String uri, Map<String, String> parameters, OAuthAccessToken accessToken) throws Exception
    {
    	PostMethod method = new PostMethod(uri);
    	method.setDoAuthentication(true);
    	
    	parameters.put("oauth_timestamp", OAuthUtils.getNewOAuthTimeStamp());
    	parameters.put("oauth_nonce", OAuthUtils.getNewNonceValue());
    	parameters.put("oauth_token", accessToken.getTokenText());
    	
    	String signatureBaseString = OAuthUtils.getSignatureBaseString("POST", uri, parameters);
    	String signatureParameter = OAuthUtils.getHMACSHASignature(signatureBaseString, this.netflixAPIClient.getConsumerSecret(), accessToken.getTokenSecret());
    	
    	parameters.put("oauth_signature", signatureParameter);
    	String authHeader = this.createAuthorizationHeader((HashMap<String, String>) parameters);
    	this.setNonOauthParams(method, parameters);
    	
    	method.setRequestHeader("Authorization", authHeader);
    	
    	if (logger.isDebugEnabled())
    	{
    		String message = "Created method [ POST " + uri 
    		+ "\n Authorization: " +
    				authHeader + " ]";
    		logger.debug(message);
    	}
    	
    	return method;
    }
    
    /**
     * @param uri
     * @param parameters
     * @return
     * @throws Exception
     */
    public PostMethod buildConsumerSignedPostMethodWithAccessSecretAndQueryString(String uri, Map<String, String> parameters, OAuthAccessToken accessToken) throws Exception
    {
    	PostMethod method = new PostMethod(uri);
    	method.setDoAuthentication(true);
    	
    	parameters.put("oauth_timestamp", OAuthUtils.getNewOAuthTimeStamp());
    	parameters.put("oauth_nonce", OAuthUtils.getNewNonceValue());
    	parameters.put("oauth_token", accessToken.getTokenText());
    	
    	String signatureBaseString = OAuthUtils.getSignatureBaseString("POST", uri, parameters);
    	String signatureParameter = OAuthUtils.getHMACSHASignature(signatureBaseString, this.netflixAPIClient.getConsumerSecret(), accessToken.getTokenSecret());
    	
    	parameters.put("oauth_signature", signatureParameter);
    	String queryString = this.createNormalizedQueryString((HashMap<String, String>) parameters);
    	
    	method.setQueryString(queryString);
    	
    	if (logger.isDebugEnabled())
    	{
    		String message = "Created method [ POST " + uri + "]";
    		logger.debug(message);
    	}
    	
    	return method;
    }
    
    /**
     * Builds a DeleteMethod suitable for full-security OAuth requests.
     * @param uri
     * @param parameters
     * @return
     * @throws Exception
     */
    protected DeleteMethod buildConsumerSignedDeleteMethodWithAccessSecret(String uri, Map<String, String> parameters, OAuthAccessToken accessToken) throws Exception
    {
    	DeleteMethod method = this.newDeleteMethod(uri);
    	method.setDoAuthentication(true);
    	
    	parameters.put("oauth_timestamp", OAuthUtils.getNewOAuthTimeStamp());
    	parameters.put("oauth_nonce", OAuthUtils.getNewNonceValue());
    	parameters.put("oauth_token", accessToken.getTokenText());
    	
    	String signatureBaseString = OAuthUtils.getSignatureBaseString("DELETE", uri, parameters);
    	String signatureParameter = OAuthUtils.getHMACSHASignature(signatureBaseString, this.netflixAPIClient.getConsumerSecret(), accessToken.getTokenSecret());
    	
    	parameters.put("oauth_signature", signatureParameter);
    	String authHeader = this.createAuthorizationHeader((HashMap<String, String>) parameters);
    	
    	method.setRequestHeader("Authorization", authHeader);
    	
    	if (logger.isDebugEnabled())
    	{
    		String message = "Created method [ DELETE " + uri 
    		+ "\n Authorization: " +
    				authHeader + " ]";
    		logger.debug(message);
    	}
    	
    	return method;
    }
    
	/**
     * Performs the first major step in the sublime dance that is OAuth:
     * this retrieves a plain-vanilla, unauthorized request token from
     * the service provider.
     * 
     * @return
     * @throws Exception
     */
    public OAuthRequestToken getNewRequestToken() throws Exception
    {
    	return this.getNewRequestToken(null);
    }
    
    /**
     * Performs the first major step in the sublime dance that is OAuth:
     * this retrieves a plain-vanilla, unauthorized request token from
     * the service provider.
     * 
     * @return
     * @throws Exception
     */
    public OAuthRequestToken getNewRequestToken(Map<String, String> requestHeaders) throws Exception
    {
    	OAuthRequestToken token = null;
    	Map<String, String> parameters = this.getDefaultOAuthParameters();
    	String uri = APIEndpoints.REQUEST_TOKEN_PATH;
    	GetMethod getMethod = this.buildConsumerSignedGetMethodWithQueryString(uri, parameters);
    	if (requestHeaders != null)
    	{
    		for (String header : requestHeaders.keySet())
    		{
    			getMethod.addRequestHeader(header, requestHeaders.get(header));
    		}
    	}
    	NetflixAPIResponse response = this.netflixAPIClient.executeCustomMethod(getMethod);
    	if (logger.isDebugEnabled())
    	{
    		logger.debug(response.getResponseBody());
    	}
    	token = new OAuthRequestToken(response.getResponseBody());
    	getMethod.releaseConnection();
    	return token;
    }
    
	/**
	 * The second major step in the OAuth flow. <br />
     * Sends the request token off to the service provider, on behalf of the
     * customer, so the customer can authenticate the request token.  This call
     * does not alter the request token itself; rather it affects the back-end server's
     * state.  The request token should then be exchangeable for an access token.
     * @param requestToken
     */
    private void authorizeRequestToken(OAuthRequestToken requestToken, NetflixAPICustomer customer) throws Exception
	{
    	PostMethod loginMethod = this.buildLoginMethod(APIEndpoints.LOGIN_PATH);
    	loginMethod.addParameter("oauth_token", requestToken.getTokenText());
    	loginMethod.addParameter("oauth_consumer_key", this.netflixAPIClient.getConsumerKey());
    	loginMethod.addParameter("application_name", requestToken.getApplicationName());
    	loginMethod.addParameter("name", customer.getUsername());
    	loginMethod.addParameter("password", customer.getPassword());
    	loginMethod.addParameter("accept_tos", "true");
    	loginMethod.addParameter("output", "pox");
    	loginMethod.addParameter("oauth_callback", "");
    	httpClient.executeMethod(loginMethod);
    	
    	if (logger.isDebugEnabled())
    	{
    		logger.debug("Response from authorize token: " + loginMethod.getResponseBodyAsString());
    	}
    	if (loginMethod.getStatusCode() != 200)
    		throw new NetflixAPIException(NetflixAPIException.LOGIN_FAILED);
    	loginMethod.releaseConnection();
	}
    
    /**
     * Builds a POST method specific to logging a user in.
     * @param uri
     * @param parameters
     * @return
     */
    private PostMethod buildLoginMethod(String uri)
	{
    	PostMethod method = (PostMethod) this.newPostMethod(uri);
    	
    	if (logger.isDebugEnabled())
    	{
    		String message = "Created method [ POST " + uri + "]";
    		logger.debug(message);
    	}
    	
    	return method;
    }
    
	/**
     * The third major step in the OAuth flow. <br />
     * Exchanges the now-authorized request token for an access token.
     * Aren't you happy now?
     * @param authorizedRequestToken
     * @return
     */
    public OAuthAccessToken exchangeRequestForAccessToken(OAuthRequestToken authorizedRequestToken) throws Exception
	{
    	return this.exchangeRequestForAccessToken(authorizedRequestToken, null);
	}
    
    /**
     * The third major step in the OAuth flow. <br />
     * Exchanges the now-authorized request token for an access token.
     * Aren't you happy now?
     * @param authorizedRequestToken
     * @return
     */
    public OAuthAccessToken exchangeRequestForAccessToken(OAuthRequestToken authorizedRequestToken, Map<String, String> requestHeaders) throws Exception
	{
    	OAuthAccessToken oat = null;
    	int statusCode = 0;
    	String uri = APIEndpoints.ACCESS_TOKEN_PATH;
    	Map<String, String> parameters = this.getDefaultOAuthParameters();
    	parameters.put("oauth_token", authorizedRequestToken.getTokenText());
    	
    	GetMethod exchangeMethod = this.buildAccessTokenRequestMethod(uri, parameters, authorizedRequestToken);
    	if (requestHeaders != null)
    	{
    		for (String header : requestHeaders.keySet())
    		{
    			exchangeMethod.setRequestHeader(header, requestHeaders.get(header));
    		}
    	}
    	
    	httpClient.executeMethod(exchangeMethod);
    	String response = exchangeMethod.getResponseBodyAsString();
    	statusCode = exchangeMethod.getStatusCode();
    	if (statusCode != 200)
    	{
    		String message = "Exchange of request token [ " + authorizedRequestToken.getTokenText() + " ] FAILED with " +
    				"response [ " + exchangeMethod.getResponseBodyAsString() + " ]";
    		logger.error(message);
    		oat = new OAuthAccessToken();
    		oat.setErrorCause(message);
    		exchangeMethod.releaseConnection();
    		return oat;
    	}
    	
    	if (logger.isDebugEnabled())
    	{
    		logger.debug("Response from exchange token: " + response);
    	}
    	oat = new OAuthAccessToken(response);
    	exchangeMethod.releaseConnection();
 		return oat; 
	}
    
	/**
	 * Creates the Authorization: header in the OAuth realm for 
	 * API requests.
	 * @param params
	 * @return - string containing oauth realm info.
	 */
	public String createAuthorizationHeader(HashMap<String, String> params)
	{
		boolean isFirstParam = true;
		StringBuilder sb = new StringBuilder();
		sb.append("OAuth ");
		for (String param : params.keySet())
		{
			if (isFirstParam == true)
				isFirstParam = false;
				
			String paramVal = params.get(param);
			if (paramVal != null && param.startsWith("oauth"))
			{
				if (!isFirstParam) sb.append(",");
				sb.append(param).append("=\"").append(OAuth.percentEncode(paramVal)).append("\"");
			}
		}
		return sb.toString();
	}
	
	/**
	 * Filters the OAuth-specific from the arbitrary parameters.
	 * @param method
	 * @param parameters
	 */
	public void setNonOauthParams(PostMethod method, Map<String, String> parameters)
	{
		for (String param : parameters.keySet())
		{
			if (!param.startsWith("oauth"))
			{
				method.setParameter(param, parameters.get(param));
			}
		}
	}
	
	/**
	 * Creates query string for GET requests.
	 * @param params
	 * @return - string containing oauth realm info.
	 */
	protected String createNormalizedQueryString(HashMap<String, String> params) throws Exception
	{
		Set<String> keySet = params.keySet();
		String[] keys = new String[keySet.size()];
		keySet.toArray(keys);
		Arrays.sort(keys);
		
		boolean isFirstParam = true;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < keys.length; i++)
		{
			if (isFirstParam == true)
				isFirstParam = false;
			else
				sb.append("&");
			String paramVal = params.get(keys[i]);
			if (paramVal != null)
				sb.append(keys[i]).append("=").append(URLEncoder.encode(paramVal, "UTF-8"));
		}
		return sb.toString();
	}
	
	/**
	 * Creates query string for GET requests.
	 * @param params
	 * @return - string containing oauth realm info.
	 */
	protected String createNonOAuthQueryString(Map<String, String> params) throws Exception
	{
		Set<String> keySet = params.keySet();
		String[] keys = new String[keySet.size()];
		keySet.toArray(keys);
		Arrays.sort(keys);
		
		boolean isFirstParam = true;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < keys.length; i++)
		{
			if (isFirstParam == true)
				isFirstParam = false;
				
			String paramVal = params.get(keys[i]);
			if (paramVal != null && !(keys[i].startsWith("oauth")))
			{
				if (!isFirstParam) sb.append("&");
				sb.append(keys[i]).append("=").append(URLEncoder.encode(paramVal, "UTF-8"));
			}
		}
		return sb.toString();
	}
	
	/**
	 * Creates an HttpMethod object with default settings
	 * (Ignores cookies and doesn't follow redirects).
	 * @param uri
	 * @return - defualt HttpMethod
	 */
	private GetMethod newGetMethod(String uri)
    {
        GetMethod method = new GetMethod(uri);
        HttpMethodParams params = method.getParams();
        params.setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
        method.setFollowRedirects(false);
        return method;
    }
	
	/**
	 * Creates an PostMethod object with default settings
	 * (Ignores cookies and doesn't follow redirects).
	 * @param uri
	 * @return - defualt HttpMethod
	 */
	private PostMethod newPostMethod(String uri)
    {
        PostMethod method = new PostMethod(uri);
        HttpMethodParams params = method.getParams();
        params.setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
        method.setFollowRedirects(false);
        return method;
    }
	
	/**
	 * Creates an PostMethod object with default settings
	 * (Ignores cookies and doesn't follow redirects).
	 * @param uri
	 * @return - defualt HttpMethod
	 */
	private DeleteMethod newDeleteMethod(String uri)
    {
        DeleteMethod method = new DeleteMethod(uri);
        HttpMethodParams params = method.getParams();
        params.setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
        method.setFollowRedirects(false);
        return method;
    }
	
	/**
	 * @param method
	 * @param requestHeaders
	 */
	private HttpMethod applyRequestHeadersToMethod(HttpMethod method, Map<String, String> requestHeaders)
	{
		for (String header : requestHeaders.keySet())
		{
			method.setRequestHeader(header, requestHeaders.get(header));
		}
		return method;
	}
	
}
