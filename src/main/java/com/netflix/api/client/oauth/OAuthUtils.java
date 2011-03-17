package com.netflix.api.client.oauth;

import java.util.Map;
import java.util.UUID;

import net.oauth.OAuth;
import net.oauth.signature.HMACSignatureUtil;

/**
 * Suite of utility methods for creating and processing OAuth requests.
 * 
 * Supports the creation of OAuth authorization headers preloaded with
 * sensible defaults, generation of Signature Base Strings, and HMAC-SHA1
 * signature generation.
 * 
 * @author jharen
 */
public class OAuthUtils
{
    /**
     * Runs the required params through the HMAC-SHA1 signature encryption algorithm.
     * @param signatureBaseString
     * @param consumerSecret
     * @param tokenSecret
     * @return
     * @throws Exception
     */
    public static String getHMACSHASignature(String signatureBaseString, String consumerSecret, String tokenSecret) throws Exception
    {
    	return HMACSignatureUtil.getHMACSHASignature(signatureBaseString, consumerSecret, tokenSecret);
    }
    
    /**
     * Builds a Signature Base String for the HMAC-SHA1 signature method as per
     * specification at http://oauth.net/core/1.0/#anchor14 .
     * @param httpMethod - String of "GET", "POST", "PUT", etc.
     * @param uri String of any valid URI
     * @param params - map of request parameters
     * @return - String for generating HMAC-SHA1 signatures.
     */
    public static String getSignatureBaseString(String httpMethod, String uri, Map<String, String> params)
    {
    	StringBuffer baseSignature = new StringBuffer();
    	baseSignature.append(OAuth.percentEncode(httpMethod.toUpperCase()));
    	baseSignature.append("&");
    	baseSignature.append(OAuth.percentEncode(uri));
    	if (params != null && !(params.isEmpty()))
    	{
    		baseSignature.append("&");
    		String paramString = HMACSignatureUtil.getNormalizedParameterString(params);
    		baseSignature.append(OAuth.percentEncode(paramString));
    	}
    	return baseSignature.toString();
    }
    
    /**
     * Provides a request string-ready representation of the number of
     * seconds since epoch. 
     * @return number of seconds since January 1, 1970 00:00:00 GMT.
     */
    public static String getNewOAuthTimeStamp()
    {
    	return String.valueOf(System.currentTimeMillis()/1000);
    }
    
    /**
     * Provides a request string-ready nonce value, a randomly generated 
     * integer for a individual request.  These are intended to be combined 
     * with a timestamp to produce a unique value to prevent replay attacks.
     * @return a random String value, built from a 128-bit UUID.
     */
    public static String getNewNonceValue()
    {
    	return UUID.randomUUID().toString().replace("-", "");
    }
    
}
