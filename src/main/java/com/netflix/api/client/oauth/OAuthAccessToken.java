package com.netflix.api.client.oauth;

import java.util.HashMap;

public class OAuthAccessToken
{
	/**
	 * Value of the 'oauth_token' returned by a service provider as per
	 * section 6.3.2 of the OAuth spec.
	 */
	private String tokenText;
	
	/**
	 * Value of the 'oauth_token_secret' returned by a service provider as per
	 * section 6.3.2 of the OAuth spec.
	 */
	private String tokenSecret;
	
	/**
	 * The user ID of the customer on whose behalf the token was granted.
	 */
	private String tokenUserID;
	
	/**
	 * An indication from the originating server of any errors preventing the
	 * normal construction of this access token.  Clients should check this value
	 * before attempting to use this token; if this value is NULL, then the token
	 * is safe for use.  If not, the token is invalid and this value will
	 * indicate why.
	 */
	private String errorCause;
	
	/**
	 * Internal map used to store parsed response values.
	 */
	private HashMap<String, String> values = new HashMap<String, String>();
	
	/**
	 * Default no-arg constructor.
	 */
	public OAuthAccessToken()
	{
		// defualt no-arg constructor.
	}
	
	public OAuthAccessToken(String delimitedResponse)
	{
		if (delimitedResponse == null)
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
		
			this.tokenText = this.values.get("oauth_token");
			this.tokenUserID = this.values.get("user_id");
			this.tokenSecret = this.values.get("oauth_token_secret");
		}
		catch(Exception e)
		{
			this.tokenText = null;
			this.tokenSecret = null;
			this.tokenUserID = null;
		}
	}


	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder("oauth_token=");
		sb.append(this.tokenText);
		sb.append("&");
		sb.append("user_id=");
		sb.append(this.tokenUserID);
		sb.append("&");
		sb.append("oauth_token_secret=");
		sb.append(this.tokenSecret);
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

	public String getTokenUserID()
	{
		return tokenUserID;
	}

	public void setTokenUserID(String tokenUserID)
	{
		this.tokenUserID = tokenUserID;
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
