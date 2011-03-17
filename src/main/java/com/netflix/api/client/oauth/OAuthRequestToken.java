package com.netflix.api.client.oauth;

import java.net.URLDecoder;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates the token returned by an OAuth service provider after
 * a successful request token request.  
 * 
 * @author jharen
 */
public class OAuthRequestToken
{
	private static final Logger logger = LoggerFactory.getLogger(OAuthRequestToken.class);
	
	/**
	 * Value of the 'oauth_token' returned by a service provider as per
	 * section 5.3 of the OAuth spec.
	 */
	private String tokenText;
	
	/**
	 * Value of the 'oauth_token_secret' returned by a service provider as per
	 * section 5.3 of the OAuth spec.
	 */
	private String tokenSecret;
	
	/**
	 * Value of the 'application_name' returned by service provider.
	 */
	private String applicationName;
	
	/**
	 * Indicates what went wrong if token response was unintelligble.
	 */
	private String errorCause;
	
	
	/**
	 * Internal map used to store parsed response values.
	 */
	protected HashMap<String, String> values = new HashMap<String, String>();

	/**
	 * Constructs an empty RequestToken.
	 */
	public OAuthRequestToken()
	{
		// default do-nothing constructor
	}
	
	/**
	 * Constructs a valid request token from a single &-delimited string.
	 * @param delimitedResponse
	 */
	public OAuthRequestToken(String delimitedResponse)
	{
		if (delimitedResponse == null || delimitedResponse.equals(""))
			return;
		String[] components = delimitedResponse.split("&");
		
		try
		{
			for (int i = 0; i < components.length; i++)
			{
				String[] parts = components[i].split("=");
				String key = parts[0];
				String value = parts[1];
				this.values.put(key, value);
			}
		
			this.applicationName = URLDecoder.decode(this.values.get("application_name"), "UTF-8");
			this.tokenSecret = this.values.get("oauth_token_secret");
			this.tokenText = this.values.get("oauth_token");
			if (this.tokenText == null || this.tokenSecret == null)
			{
				logger.error("MALFORMED REQUEST TOKEN from response [" + delimitedResponse + "]");
			}
		}
		catch(Exception e)
		{
			this.tokenText = null;
			this.tokenSecret = null;
			this.applicationName = null;
			this.errorCause = delimitedResponse;
		}
	}
	
	/**
	 * Constructs a valid request token from separate strings.
	 * @param tokenText
	 * @param tokenSecret
	 */
	public OAuthRequestToken(String tokenText, String tokenSecret)
	{
		this.tokenText = tokenText;
		this.tokenSecret = tokenSecret;
	}
	
	/**
	 * Returns this object's value formatted as per RFC3986 section 2.1.
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder("oauth_token=");
		sb.append(this.tokenText);
		sb.append("&");
		sb.append("oauth_token_secret=");
		sb.append(this.tokenSecret);
		sb.append("&");
		sb.append("application_name=");
		sb.append(this.applicationName);
		return sb.toString();
	}
	
	public String getTokenText()
	{
		return tokenText;
	}

	public void setTokenText(String tokenText)
	{
		this.tokenText = tokenText;
	}

	public String getTokenSecret()
	{
		return tokenSecret;
	}

	public void setTokenSecret(String tokenSecret)
	{
		this.tokenSecret = tokenSecret;
	}

	public String getApplicationName()
	{
		return applicationName;
	}

	public void setApplicationName(String applicationName)
	{
		this.applicationName = applicationName;
	}

	public String getErrorCause()
	{
		return this.errorCause;
	}

	public void setErrorCause(String errorCause)
	{
		this.errorCause = errorCause;
	}
	
}
