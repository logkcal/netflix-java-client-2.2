package com.netflix.api.client;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.api.NetflixAPIException;
import com.netflix.api.NetflixAPIResponse;
import com.netflix.api.client.dal.HttpMethodBuilder;
import com.netflix.api.client.oauth.OAuthAccessToken;
import com.netflix.api.client.oauth.OAuthRequestToken;

/**
 * Main point of interaction with Netflix API. <br />
 * This class encapsulates several steps of communicating 
 * with the Netflix API, including creating an HTTP connection,
 * making the request, obtaining a response, and closing the 
 * connection.  As such, it only returns String representations
 * of API response bodies.
 * <br />
 * Clients needing more flexiblity -- access to response streams,
 * for example -- should not use this class.  Instead, consider
 * using the <code>HttpMethodBuilder</code> backing class directly.
 * 
 * @author jharen
 */
public class NetflixAPIClient
{
	private static final Logger logger = LoggerFactory.getLogger(NetflixAPIClient.class);
	
	/**
	 * Indicates the method builder should make an HTTP GET Method.
	 */
	public static final String GET_METHOD_TYPE = "GET";
	
	/**
	 * Indicates the method builder should make an HTTP POST Method.
	 */
	public static final String POST_METHOD_TYPE = "POST";
	
	/**
	 * Indicates the method builder should make an HTTP DELETE Method.
	 */
	public static final String DELETE_METHOD_TYPE = "DELETE";
	
    /**
     * Specifies the signature method used to sign a request.
     * Should be HMAC-SHA1, a signature algorithm defined in RFC2104. 
     */
    private String SIGNATURE_METHOD = "HMAC-SHA1";
    
    /**
     * OAuth protocol version.
     */
    private String OAUTH_VERSION = "1.0";
	
	/**
	 * Makes http calls and returns results.
	 */
	private HttpClient httpClient;
	
	/**
	 * Back-end utility class for forming HTTP methods.
	 */
	private HttpMethodBuilder methodBuilder;
	
	/**
	 * Application developer's consumer key.
	 */
	private String consumerKey;
	
	/**
	 * Application developer's consumer secret.
	 */
	private String consumerSecret;
	
	/**
	 * Default no-arg constructor.
	 */
	public NetflixAPIClient()
	{
		// no-arg constructor
		this.httpClient = new HttpClient();
		this.methodBuilder = new HttpMethodBuilder(this);
		APIEndpoints.initToDefaults();
	}
	
	/**
	 * Standard constructor, initializes environment
	 * to default configuration.
	 * @param consumerKey
	 * @param consumerSecret
	 */
	public NetflixAPIClient(String consumerKey, String consumerSecret)
	{
		this.consumerKey = consumerKey;
		this.consumerSecret = consumerSecret;
		this.httpClient = new HttpClient();
		this.methodBuilder = new HttpMethodBuilder(this);
		APIEndpoints.initToDefaults();
	}
	
	/**
	 * Initializes environment to configuration described by the provided
	 * properties object.
	 * @param consumerKey
	 * @param consumerSecret
	 * @param props
	 */
	@SuppressWarnings("deprecation")
	public NetflixAPIClient(String consumerKey, String consumerSecret, Properties props)
	{
		this.consumerKey = consumerKey;
		this.consumerSecret = consumerSecret;
		try
		{
			int threads = Integer.decode(props.getProperty("THREADS"));
			if (threads > 0)
			{
				MultiThreadedHttpConnectionManager cm = new MultiThreadedHttpConnectionManager();
				cm.setMaxTotalConnections(threads);
				cm.setMaxConnectionsPerHost(threads);
				this.httpClient = new HttpClient(cm);
			}
			else this.httpClient = new HttpClient();
		}
		catch (Exception e) 
		{
			this.httpClient = new HttpClient();
		}
		this.methodBuilder = new HttpMethodBuilder(this, props);
		APIEndpoints.init(props);
	}
	
	/**
	 * Initializes environment to configuration described by the provided
	 * properties object and the given connection manager.
	 * @param consumerKey
	 * @param consumerSecret
	 * @param props
	 * @param cm
	 */
	public NetflixAPIClient(String consumerKey, String consumerSecret, 
			Properties props, MultiThreadedHttpConnectionManager cm)
	{
		this.consumerKey = consumerKey;
		this.consumerSecret = consumerSecret;
		this.httpClient = new HttpClient(cm);
		this.methodBuilder = new HttpMethodBuilder(this, props);
		APIEndpoints.init(props);
	}
	
	/**
	 * Calls netflix API using no signing of any kind (appropriate for the
	 * 'no auth' level of security).  Primarily of interest only to clients
	 * of the javascript API.
	 * 
	 * @param uri - the uri to call
	 * @param callParameters - a map of key-value pairs to set as standard HTTP GET
	 * method parameters (e.g., ?foo=bar&biz=baz).
	 * @return - a string of the server's response
	 * @throws Exception - if a server communication error occurs.
	 */
	public NetflixAPIResponse makeUnsignedApiCall(String uri, Map<String, String> callParameters) throws Exception
	{
		NetflixAPIResponse response = new NetflixAPIResponse();
		GetMethod method = null;
		if (callParameters == null)
			callParameters = new HashMap<String, String>();
		callParameters.putAll(methodBuilder.getDefaultOAuthParameters());
		
		method = methodBuilder.buildConsumerKeyedGetMethod(uri, callParameters);
		httpClient.executeMethod(method);
		response.setResponseBody(method.getResponseBodyAsString());
		response.setStatusCode(method.getStatusCode());
		response.setStatusLine(method.getStatusLine().toString());
		response.setResponseHeaders(this.resolveResponseHeaders(method));
		method.releaseConnection();
		
		if (logger.isDebugEnabled())
		{
			response.setExecutionSummary("Calling [" + uri + "] resulted in status code [" + response.getStatusLine() + "] and response\n" + response.getResponseBody());
			logger.debug(response.getExecutionSummary());
		}
		return response;
	}
	
	/**
	 * Calls Netflix API using the "consumer key and secret" level of security.
	 * Use this for restricted resources that don't require a customer's authorization.
	 *  
	 * @param uri - the uri to call
	 * @param callParameters - a map of key-value pairs to be placed in the query string
	 * (for GET methods) or in the POST body (for POSTs).
	 * @param methodType - either "GET" or "POST".
	 * @return - a string of the server's response
	 * @throws Exception - if a server communication error occurs.
	 */
	public NetflixAPIResponse makeConsumerSignedApiCall(String uri, Map<String, String> callParameters, String methodType) throws Exception
	{
		NetflixAPIResponse response = new NetflixAPIResponse();
		HttpMethod method = null;
		if (callParameters == null)
			callParameters = new HashMap<String, String>();
		callParameters.putAll(methodBuilder.getDefaultOAuthParameters());
		
		if (methodType.equalsIgnoreCase(GET_METHOD_TYPE))
			method = methodBuilder.buildConsumerSignedGetMethod(uri, callParameters);
		else if (methodType.equalsIgnoreCase(POST_METHOD_TYPE))
			method = methodBuilder.buildConsumerSignedPostMethod(uri, callParameters);
		else throw new NetflixAPIException("No valid HTTP method specified: must be GET or POST for consumer-signed calls.");
		
		httpClient.executeMethod(method);
		response.setResponseBody(method.getResponseBodyAsString());
		response.setStatusCode(method.getStatusCode());
		response.setStatusLine(method.getStatusLine().toString());
		response.setResponseHeaders(this.resolveResponseHeaders(method));
		method.releaseConnection();
		
		if (logger.isDebugEnabled())
		{
			response.setExecutionSummary("Calling [" + uri + "] resulted in status code [" + response.getStatusLine() + "] and response\n" + response.getResponseBody());
			logger.debug(response.getExecutionSummary());
		}
		
		return response;
	}
	
	/**
	 * Calls the Netflix API using the "Access Token and Secret" level of security.
	 * Use this for accessing/managing a customer's information.
	 * 
	 * @param uri - the uri to call
	 * @param customer - the netflix customer on whose behalf the call is being made.
	 * @param callParameters - a map of key-value pairs to be placed in the query string
	 * (for GET methods) or in the POST body (for POSTs).
	 * @param methodType - either "GET", "DELETE" or "POST".
	 * @return - a string of the server's response
	 * @throws Exception - if a server communication error occurs.
	 */
	public NetflixAPIResponse makeCustomerAuthorizedApiCall(String uri, NetflixAPICustomer customer, Map<String, String> callParameters, String methodType) throws Exception
	{
		NetflixAPIResponse response = new NetflixAPIResponse();
		HttpMethod method = null;
		if (callParameters == null)
			callParameters = new HashMap<String, String>();
		callParameters.putAll(methodBuilder.getDefaultOAuthParameters());
		
		if (methodType.equalsIgnoreCase(GET_METHOD_TYPE))
			method = methodBuilder.buildCustomerAuthorizedGetMethod(uri, callParameters, customer);
		else if (methodType.equalsIgnoreCase(POST_METHOD_TYPE))
			method = methodBuilder.buildCustomerAuthorizedPostMethod(uri, callParameters, customer);
		else if (methodType.equalsIgnoreCase(DELETE_METHOD_TYPE))
			method = methodBuilder.buildCustomerAuthorizedDeleteMethod(uri, callParameters, customer);
		else throw new NetflixAPIException("No valid HTTP method specified: must be GET, POST or DELETE for customer authorized calls.");
		
		httpClient.executeMethod(method);
		response.setResponseBody(method.getResponseBodyAsString());
		response.setStatusCode(method.getStatusCode());
		response.setStatusLine(method.getStatusLine().toString());
		response.setResponseHeaders(this.resolveResponseHeaders(method));
		method.releaseConnection();
		
		if (logger.isDebugEnabled())
		{
			response.setExecutionSummary("Calling [" + uri + "] resulted in status code [" + response.getStatusLine() + "] and response\n" + response.getResponseBody());
			logger.debug(response.getExecutionSummary());
		}
		
		return response;
	}
	
	/**
	 * Calls the Netflix API using the "Access Token and Secret" level of security.
	 * Use this for accessing/managing a customer's information.
	 * 
	 * @param uri - the uri to call
	 * @param customer - the netflix customer on whose behalf the call is being made.
	 * @param callParameters - a map of key-value pairs to be placed in the query string
	 * (for GET methods) or in the POST body (for POSTs).
	 * @param methodType - either "GET", "DELETE" or "POST".
	 * @return - a string of the server's response
	 * @throws Exception - if a server communication error occurs.
	 */
	public NetflixAPIResponse makeCustomerAuthorizedApiCall(String uri, NetflixAPICustomer customer, Map<String, String> callParameters,
			Map<String, String> requestHeaders, String methodType) throws Exception
	{
		NetflixAPIResponse response = new NetflixAPIResponse();
		HttpMethod method = null;
		if (callParameters == null)
			callParameters = new HashMap<String, String>();
		callParameters.putAll(methodBuilder.getDefaultOAuthParameters());
		
		if (methodType.equalsIgnoreCase(GET_METHOD_TYPE))
			method = methodBuilder.buildCustomerAuthorizedGetMethod(uri, callParameters, customer, requestHeaders);
		else if (methodType.equalsIgnoreCase(POST_METHOD_TYPE))
			method = methodBuilder.buildCustomerAuthorizedPostMethod(uri, callParameters, customer, requestHeaders);
		else if (methodType.equalsIgnoreCase(DELETE_METHOD_TYPE))
			method = methodBuilder.buildCustomerAuthorizedDeleteMethod(uri, callParameters, customer, requestHeaders);
		else throw new NetflixAPIException("No valid HTTP method specified: must be GET, POST or DELETE for customer authorized calls.");
		
		httpClient.executeMethod(method);
		response.setResponseBody(method.getResponseBodyAsString());
		response.setStatusCode(method.getStatusCode());
		response.setStatusLine(method.getStatusLine().toString());
		response.setResponseHeaders(this.resolveResponseHeaders(method));
		method.releaseConnection();
		
		if (logger.isDebugEnabled())
		{
			response.setExecutionSummary("Calling [" + uri + "] resulted in status code [" + response.getStatusLine() + "] and response\n" + response.getResponseBody());
			logger.debug(response.getExecutionSummary());
		}
		
		return response;
	}
	
	/**
	 * @param method
	 * @return
	 */
	public NetflixAPIResponse executeCustomMethod(HttpMethod method) throws Exception
	{
		NetflixAPIResponse response = new NetflixAPIResponse();
		httpClient.executeMethod(method);
		response.setResponseBody(method.getResponseBodyAsString());
		response.setStatusCode(method.getStatusCode());
		response.setStatusLine(method.getStatusLine().toString());
		response.setResponseHeaders(this.resolveResponseHeaders(method));
		String executionSummary = "Execution summary:\n" + response.getStatusLine() + "\n" +
			response.getResponseBody();
		response.setExecutionSummary(executionSummary);
		method.releaseConnection();
		
		if (logger.isDebugEnabled())
		{
			logger.debug(response.getExecutionSummary());
		}
		return response;
	}
	

	/**
	 * Returns the URL to redirect the user to so they can perform the
	 * appropriate out-of-band authorization.
	 * @param requestToken
	 * @param callbackURL
	 * @return String representing the appropriate URL for user auth.
	 */
	public String getNetflixAuthorizationURL(OAuthRequestToken requestToken, String callbackURL)
	{
		StringBuffer forwardURL = new StringBuffer(APIEndpoints.LOGIN_PATH);
		forwardURL.append("?oauth_token=" + requestToken.getTokenText());
		forwardURL.append("&oauth_consumer_key=" + this.getConsumerKey());
		forwardURL.append("&application_name=" + requestToken.getApplicationName());
		forwardURL.append("&accept_tos=true");
		forwardURL.append("&output=pox");
		forwardURL.append("&oauth_callback=" + callbackURL);
		try
		{
			return URLEncoder.encode(forwardURL.toString(), "UTF-8");
		}
		catch (UnsupportedEncodingException e)
		{
			return null;
		}
	}
	
	/**
	 * Fetches a new request token from the API front-ends.
	 * @return OAuthRequestToken - newly-obtained request token.
	 * @throws Exception
	 */
	public OAuthRequestToken getNewRequestToken() throws Exception
	{
		return this.getNewRequestToken(null);
	}
	
	/**
	 * Fetches a new request token from the API front-ends.
	 * @return OAuthRequestToken - newly-obtained request token.
	 * @throws Exception
	 */
	public OAuthRequestToken getNewRequestToken(Map<String, String> requestHeaders) throws Exception
	{
		return this.methodBuilder.getNewRequestToken(requestHeaders);
	}
	
	/**
     * The last major step in the OAuth flow. <br />
     * Exchanges <em>authorized</em> request token for an access token.
     * 
     * @param authorizedRequestToken
     * @return
     */
	public OAuthAccessToken exchangeRequestTokenForAccessToken(OAuthRequestToken authorizedRequestToken) throws Exception
	{
		return this.exchangeRequestTokenForAccessToken(authorizedRequestToken, null);
	}
	
	/**
     * The last major step in the OAuth flow. <br />
     * Exchanges <em>authorized</em> request token for an access token.
     * 
     * @param authorizedRequestToken
     * @return
     */
	public OAuthAccessToken exchangeRequestTokenForAccessToken(OAuthRequestToken authorizedRequestToken, Map<String, String> requestHeaders) throws Exception
	{
		return this.methodBuilder.exchangeRequestForAccessToken(authorizedRequestToken, requestHeaders);
	}
	
	/**
	 * Places all response headers returned from the executed method into an
	 * map of key-value pairs.
	 * @param method
	 * @return
	 */
	protected Map<String, String> resolveResponseHeaders(HttpMethod method)
	{
		Map<String, String> rh = new HashMap<String, String>();
		Header[] headers = method.getResponseHeaders();
		for (int i = 0; i < headers.length; i++)
		{
			rh.put(headers[i].getName(), headers[i].getValue());
		}
		return rh;
	}
	
	public String getConsumerKey()
	{
		return consumerKey;
	}

	public void setConsumerKey(String consumerKey)
	{
		this.consumerKey = consumerKey;
	}

	public String getConsumerSecret()
	{
		return consumerSecret;
	}

	public void setConsumerSecret(String consumerSecret)
	{
		this.consumerSecret = consumerSecret;
	}

	public String getSignatureMethod()
	{
		return SIGNATURE_METHOD;
	}

	public String getOauthVersion()
	{
		return OAUTH_VERSION;
	}

	/**
	 * @return the methodBuilder
	 */
	public HttpMethodBuilder getMethodBuilder()
	{
		return this.methodBuilder;
	}

	/**
	 * @return the httpClient
	 */
	public HttpClient getHttpClient()
	{
		return this.httpClient;
	}
	
}
