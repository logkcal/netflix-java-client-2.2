package com.netflix.api.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import com.netflix.api.NetflixAPIResponse;
import com.netflix.api.client.oauth.OAuthAccessToken;
import com.netflix.api.utils.XMLUtils;

/**
 * @author John Haren
 *
 */
public class NetflixAPIClientIntegrationTest
{
	private static final Logger logger = LoggerFactory.getLogger(NetflixAPIClientIntegrationTest.class);
	
	private static NetflixAPIClient apiClient;
	
	private static NetflixAPICustomer customer;
	
	@Test
	public void checkTitleDetail()
	{
		String uri = APIEndpoints.MOVIE_URI + "/70075473";
		String details = null;
		try
		{
			details = apiClient.makeConsumerSignedApiCall(uri, null, NetflixAPIClient.GET_METHOD_TYPE).getResponseBody();
		}
		catch (Exception e)
		{
			fail(e.toString());
		}
		assertNotNull("Details call failed", details);
		
		if (logger.isInfoEnabled())
			logger.info("Calling [" + uri + "] resulted in:\n" + details);
	}
	
	@Test
	public void checkTitleDetailJSON()
	{
		String uri = APIEndpoints.MOVIE_URI + "/70075473";
		HashMap<String, String> parameters = new HashMap<String, String>();
		parameters.put("output", "json");
		String details = null;
		try
		{
			details = apiClient.makeConsumerSignedApiCall(uri, parameters, NetflixAPIClient.GET_METHOD_TYPE).getResponseBody();
		}
		catch (Exception e)
		{
			fail(e.toString());
		}
		assertNotNull("Details call failed", details);
		
		if (logger.isInfoEnabled())
			logger.info("Calling [" + uri + "] resulted in:\n" + details);
	}
	
	@Test
	public void checkPeopleSearch()
	{
		String uri = "http://netflixstaging.api.mashery.com/catalog/people";
		HashMap<String, String> callParameters = new HashMap<String, String>();
		callParameters.put("term", "Nathan Fillion");
		String results = null;
		try
		{
			NetflixAPIResponse response = apiClient.makeConsumerSignedApiCall(uri, callParameters, NetflixAPIClient.GET_METHOD_TYPE);
			results = response.getResponseBody();
		} catch (Exception e)
		{
			fail(e.toString());
		}
		
		if (logger.isInfoEnabled())
			logger.info("Calling [" + uri + "] resulted in:\n" + results);
	}
	
	@Test
	public void addToInstantQueue()
	{
		String uri = APIEndpoints.USER_URI + "/" + customer.getCustomerID() + "/queues/instant";
		HashMap<String, String> callParameters = new HashMap<String, String>();
		callParameters.put("title_ref", APIEndpoints.BASE_URI + "/catalog/titles/movies/848396");
		String lastCreatedQueueItem = null;
		String results = null;
		
		try
		{
			NetflixAPIResponse response = apiClient.makeCustomerAuthorizedApiCall(uri, customer, callParameters, NetflixAPIClient.POST_METHOD_TYPE);
			if (logger.isDebugEnabled())
				logger.debug(response.getExecutionSummary());
			results = response.getResponseBody();
			lastCreatedQueueItem = XMLUtils.getXPathElementTextFromString(results, "/status/resources_created/queue_item/id");
			this.deleteFromQueue(lastCreatedQueueItem);
		}
		catch (Exception e)
		{
			fail(e.toString());
		}
	}
	
	private void deleteFromQueue(String resourceID) throws Exception
	{
		String response = null;
		response = apiClient.makeCustomerAuthorizedApiCall(resourceID, customer, null, NetflixAPIClient.DELETE_METHOD_TYPE).getResponseBody();
		
		if (logger.isInfoEnabled())
			logger.info("Calling DELETE on [" + resourceID + "] resulted in:\n" + response);
	}
	
	@BeforeClass
	public static void beforeClass() throws Exception
	{
		Properties testProps = new Properties();
		testProps.load(NetflixAPIClientIntegrationTest.class.getResourceAsStream("/integration-test.properties"));
		String customerID = testProps.getProperty("customerID");
		String consumerKey = testProps.getProperty("consumerKey");
		String consumerSecret = testProps.getProperty("consumerSecret");
		
		Properties apiProps = new Properties();
		apiProps.load(NetflixAPIClientIntegrationTest.class.getResourceAsStream("/netflixAPIClient.properties"));
		apiClient = new NetflixAPIClient(consumerKey, consumerSecret, apiProps);
		
		String atString = IOUtils.toString(NetflixAPIClientIntegrationTest.class.getResourceAsStream("/token-" + customerID + ".txt"), "UTF-8");
		OAuthAccessToken accessToken = new OAuthAccessToken(atString);
		customer = new NetflixAPICustomer(accessToken);
	}
	
}
