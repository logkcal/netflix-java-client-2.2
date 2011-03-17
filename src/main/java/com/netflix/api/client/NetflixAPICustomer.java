package com.netflix.api.client;


import com.netflix.api.client.oauth.OAuthAccessToken;

public class NetflixAPICustomer
{
	private String customerID;
	
	private String username;
	
	private String password;
	
	private OAuthAccessToken accessToken;
	
	public NetflixAPICustomer()
	{
		// default no - arg
	}
	
	public NetflixAPICustomer(OAuthAccessToken accessToken)
	{
		this.accessToken = accessToken;
		this.customerID = this.accessToken.getTokenUserID();
	}

	public NetflixAPICustomer(String username, String cleartextPassword)
	{
		this.username = username;
		this.password = cleartextPassword;
	}
	
	public String getUsername()
	{
		return username;
	}

	public void setUsername(String username)
	{
		this.username = username;
	}

	public String getPassword()
	{
		return password;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}

	public String getCustomerID()
	{
		return customerID;
	}

	public void setCustomerID(String customerID)
	{
		this.customerID = customerID;
	}

	public OAuthAccessToken getAccessToken()
	{
		return accessToken;
	}

	public void setAccessToken(OAuthAccessToken accessToken)
	{
		this.accessToken = accessToken;
	}
	
}
