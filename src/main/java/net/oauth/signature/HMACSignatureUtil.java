package net.oauth.signature;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Thin wrapper class for HMAC-SHA1 signatures, necessary because the HMAC_SHA1 class
 * is private for some damn reason.
 * 
 * @author jharen
 */
public class HMACSignatureUtil
{
	/**
	 * Runs the required params through the HMAC-SHA1 signature encryption algorithm.  
	 * 
	 * @param signatureBaseString
	 * @param consumerSecret
	 * @param tokenSecret
	 * @return String set to the calculated digest octet string, first base64-encoded 
	 * per [RFC2045] section 6.8, then URL-encoded per section 5.1 of the OAuth spec.
	 * @throws Exception - If the Signature Base String is improperly encoded.
	 */
	public static String getHMACSHASignature(String signatureBaseString, String consumerSecret, String tokenSecret) throws Exception
	{
		HMAC_SHA1 hmac = new HMAC_SHA1();
		hmac.setConsumerSecret(consumerSecret);
		if (tokenSecret != null)
			hmac.setTokenSecret(tokenSecret);
		return hmac.getSignature(signatureBaseString);
	}
	
	/**
	 * Normalizes all request parameters into a single string that meets the requirements
	 * for building an OAuth Signature Base String (SBS) as per section 9.1.1 of the OAuth
	 * spec.
	 * @param parameters
	 * @return
	 */
	public static String getNormalizedParameterString(Map<String, String> parameters)
	{
		Collection<? extends Map.Entry<String, String>> entries = parameters.entrySet();
		try
		{
			return OAuthSignatureMethod.normalizeParameters(entries);
		} 
		catch (IOException e)
		{
			return null;
		}
	}
	
}
