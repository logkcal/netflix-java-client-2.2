package com.netflix.api;

import java.io.IOException;
import java.util.Map;

/**
 * Encapsulation of a response from the Netflix API server.<br />
 * At a minimum, a <code>NetflixAPIResponse</code> obtained from the Netflix API client
 * will have valid values for the <code>statusLine</code> and <code>statusCode</code>
 * values.  The <code>responseBody</code> value will be set if the HTTP response
 * was not null.
 * 
 * @author jharen
 */
public class NetflixAPIResponse 
{
	/**
	 * The API server's response, as a stream.
	 */
	private String responseBody;
	
	/**
	 * HTTP status line resulting from the request.
	 */
	private String statusLine;
	
	/**
	 * HTTP status code resulting from the request.
	 */
	private int statusCode;
	
	/**
	 * HTTP Response headers resulting from the request.
	 */
	private Map<String, String> responseHeaders;
	
	/**
	 * A human-friendly record of the method's execution, fit for debugging.
	 */
	private String executionSummary;
	
	public String getResponseBody() throws IOException
	{
		return this.responseBody;
	}
	
	public void setResponseBody(String responseString)
	{
		this.responseBody = responseString;
	}

	public String getStatusLine()
	{
		return this.statusLine;
	}

	public void setStatusLine(String statusLine)
	{
		this.statusLine = statusLine;
	}

	public int getStatusCode()
	{
		return this.statusCode;
	}

	public void setStatusCode(int statusCode)
	{
		this.statusCode = statusCode;
	}
	
	public Map<String, String> getResponseHeaders()
	{
		return this.responseHeaders;
	}

	public void setResponseHeaders(Map<String, String> responseHeaders)
	{
		this.responseHeaders = responseHeaders;
	}
	
	public String getEtagHeaderValue()
	{
		if (this.responseHeaders == null)
			return null;
		
		// because some responders set it as "ETag", others as "Etag", grrr...
		String etag = this.responseHeaders.get("Etag");
		if (etag == null)
			return this.responseHeaders.get("ETag");
		else return etag;
	}
	
	public String getLocationHeaderValue()
	{
		if (this.responseHeaders == null)
			return null;
		return this.responseHeaders.get("Location");
	}

	public String getExecutionSummary()
	{
		return executionSummary;
	}

	public void setExecutionSummary(String executionSummary)
	{
		this.executionSummary = executionSummary;
	}

}
