package com.netflix.api.client;

import java.util.Properties;

/**
 * Container class consisting of standard default values for 
 * a range of API endpoints.  Each exposed value can also be
 * manipulated at runtime.
 * 
 * @author jharen
 */
public class APIEndpoints
{
	public static String BASE_URI;
	public static String LOGIN_PATH;
	public static String REQUEST_TOKEN_PATH;
	public static String ACCESS_TOKEN_PATH;
	public static String CATALOG_URI;
	public static String INDEX_URI;
    public static String MOVIE_URI;
    public static String SERIES_URI;
    public static String PEOPLE_URI;
    public static String SERIES_SEASON_URI;
    public static String PROGRAMS_URI;
    public static String DISCS_URI;
    public static String USER_URI;
    
    public static void initToDefaults()
    {
    	BASE_URI = "http://api.netflix.com";
		LOGIN_PATH = "http://api-user.netflix.com/oauth/login";
		setDependentPaths();
    }
    
    public static void init(Properties props)
    {
    	try
    	{
    		BASE_URI = props.getProperty("BASE_URI");
    		LOGIN_PATH = props.getProperty("LOGIN_PATH");
    	}
    	catch (Exception e) 
    	{
			// can't read properties, use defaults
    		BASE_URI = "http://api.netflix.com";
    		LOGIN_PATH = "http://api-user.netflix.com/oauth/login";
		}
    	setDependentPaths();
    }
    
    private static void setDependentPaths()
    {
    	REQUEST_TOKEN_PATH = BASE_URI + "/oauth/request_token";
    	ACCESS_TOKEN_PATH = BASE_URI + "/oauth/access_token";
    	CATALOG_URI = BASE_URI + "/catalog/titles";
    	INDEX_URI = CATALOG_URI + "/index";
        MOVIE_URI = CATALOG_URI + "/movies";
        SERIES_URI = CATALOG_URI + "/series";
        PEOPLE_URI = BASE_URI + "/catalog/people";
        SERIES_SEASON_URI = SERIES_URI + "/seasons";
        PROGRAMS_URI = CATALOG_URI + "/programs";
        DISCS_URI = CATALOG_URI + "/discs";
        USER_URI = BASE_URI + "/users";
    }
    
    
}