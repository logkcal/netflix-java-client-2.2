package com.netflix.api;

/**
 * Encapsulates errors and exceptions arising from using the 
 * Netflix API.
 * @author jharen
 */
public class NetflixAPIException extends Exception
{
	private static final long serialVersionUID = 4621907285151913250L;
	
	/**
	 * Boilerplate message to inform client of http 403 (Not Authorized) errors.
	 */
	public static final String APPLICATION_NOT_AUTHORIZED = "Application not authorized to perform request.";
	
	/**
	 * Message indicating an incorrect or lapsed access token.
	 */
	public static final String CUSTOMER_NOT_AUTHORIZED = "Customer not authorized to perform request.";
	
	/**
	 * Message indicating that the client was unable to log in to Netflix on
	 * behalf of a customer.
	 */
	public static final String LOGIN_FAILED = "Login failed.";

	/**
	 * 
	 */
	public NetflixAPIException()
	{
		// default no-arg
	}

	/**
	 * @param message
	 */
	public NetflixAPIException(String message)
	{
		super(message);
	}

	/**
	 * @param cause
	 */
	public NetflixAPIException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public NetflixAPIException(String message, Throwable cause)
	{
		super(message, cause);
	}

}
