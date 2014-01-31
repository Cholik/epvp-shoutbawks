package com.epvp.shoutbawks;

import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ============================================================================

/**
 * Simple HTTP client implementation capable of sending POST and GET requests.
 * @author Ende!
 */

public class HTTPClient 
{

	// ------------------------------------------------------------------------
	// Private constants
	
	private static final String CHARSET = "ISO-8859-1";
	private static final Pattern CONTENT_TYPE_PARSER 
			= Pattern.compile("charset=(.*?)(\\s|;|$)");
	
	// ------------------------------------------------------------------------
	// Public enumerators and subclasses
	
	/**
	 * Defines the request method
	 */
	
	public static enum RequestMethod
	{
		GET, POST;
	} // ==> RequestMethod
	
	// ------------------------------------------------------------------------
	
	/**
	 * Container for data replied by a HTTP server.
	 */
	
	public class Response
	{
		
		// ------------------------------------------------
		// Private variables
		
		private String 					responseBody;
		private HashMap<String, String>	setCookies;
		
		// ------------------------------------------------
		// Public stuff
		
		public Response(String body, HashMap<String, String> setCookies)
		{
			this.responseBody	= body;
			this.setCookies		= setCookies;
		} // ==> ctor

		// ------------------------------------------------
		
		public String 					getBody() 		{ return responseBody;}
		public HashMap<String, String>	getSetCookies() { return setCookies;  }
		
		// ------------------------------------------------
		
	} // ==> Response
	
	// ------------------------------------------------------------------------
	// Public methods
	
	/**
	 * Sends a HTTP request to a given URL and returns the server's response.
	 * @param requestUrl	The URL to request from
	 * @param parameters	The POST/GET parameters
	 * @param type			How to handle the parameters (GET/POST)
	 * @return The received response, or null if the request failed
	 */
	
	public Response performRequest(String requestUrl, 
								   Map<String, String> parameters, 
								   RequestMethod type,
								   Map<String, String> cookies)
	{
		try
		{
			String finalUrl = new String(requestUrl);
			String content 	= new String();
			
			// Encode and process parameters, if any
			if (parameters != null)
			{			
				for (Entry<String, String> cur : parameters.entrySet())
				{
					content += URLEncoder.encode(cur.getKey(), CHARSET) 
							+ new String("=")
							+ URLEncoder.encode(cur.getValue(), CHARSET)
							+ "&";
				}
				if (content.length() >= 1)
					content = content.substring(0, content.length() - 1);
				
				if (type == RequestMethod.GET)
				{
					finalUrl += "?";
					finalUrl += content;
				}
			}
			
			// Connect
			URL url = new URL(finalUrl);
			HttpURLConnection con = (HttpURLConnection)url.openConnection();
			con.setRequestMethod(type.name());
			//con.setReadTimeout(20 * 1000);
			
			// Encode and set cookies, if any
			String cookieField = new String();
			if (cookies != null)
			{
				for (Entry<String, String> cur : cookies.entrySet())
				{
					cookieField += URLEncoder.encode(cur.getKey(), CHARSET)
								+ new String("=")
								+ URLEncoder.encode(cur.getValue(), CHARSET)
								+ "; ";
				}
			}
			if (cookieField.length() >= 2)
			{
				cookieField = cookieField.substring(
						0, 
						cookieField.length() - 2
						);
				con.setRequestProperty("Cookie", cookieField);
			}
			
			// Do POST related stuff			
			if (type == RequestMethod.POST)
			{
				con.setRequestProperty("Content-Length", 
									   Integer.toString(content.length()));
				con.setRequestProperty("Content-Type", 
									   "application/x-www-form-urlencoded; "
									   + "charset=" + CHARSET);
				con.setDoOutput(true);
				OutputStreamWriter writer 
					= new OutputStreamWriter(con.getOutputStream(), CHARSET);
				writer.write(content);
				writer.flush();
				writer.close();
			}
			
			// Send request and receive reply
			String reply = null;
			String contentCharset = CHARSET;
			try
			{
				// Determine character set for string decoding.
				// If field is not set, default to CHARSET constant.
				String contentType = con.getHeaderField("Content-Type");
				if (contentType != null)
				{
					Matcher contentTypeMatcher = CONTENT_TYPE_PARSER.matcher(
							contentType
							);
					
					if (contentTypeMatcher.find())
						contentCharset = contentTypeMatcher.group(1);
				}	
				
				// Read data
				InputStream is = con.getInputStream();
				reply = new Scanner(is, contentCharset)
						.useDelimiter("\\A")
						.next();
				con.disconnect();
				is.close();
			}
			catch (SocketTimeoutException e)
			{
				return null;
			}
			
			// Parse response and forge return data
			HashMap<String, String> setCookies = new HashMap<String, String>();			
			for (Entry<String, List<String>> cur 
					: con.getHeaderFields().entrySet())
			{				
				if (cur.getKey() != null && cur.getKey().equals("Set-Cookie"))
				{
					for (String curCookie : cur.getValue())
					{
						if (curCookie == null)
							continue;
						
						String [] splitCookie = curCookie.split(";");
						if (splitCookie.length < 1)
							continue;
							
						splitCookie = splitCookie[0].split("=");
						
						if (splitCookie.length != 2)
							continue;
						
						setCookies.put(
								URLDecoder.decode(splitCookie[0], CHARSET),
								URLDecoder.decode(splitCookie[1], CHARSET)
								);
					}
					
					break;
				}
			}			
			
			return new Response(reply, setCookies);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	} // ==> performHttpRequest
	
	// ------------------------------------------------------------------------
	
	/**
	 * Sends a GET request to a given URL and returns the server's response.
	 * @param requestUrl	The URL to request from
	 * @param parameters	The GET parameters
	 * @return The received response, or null if the request failed
	 */
	
	public Response get(String requestUrl, 
			   			Map<String, String> parameters, 
			   			Map<String, String> cookies)
	{
		return performRequest(requestUrl,
							  parameters, 
							  RequestMethod.GET, 
							  cookies);
	} // ==> get
	
	// ------------------------------------------------------------------------
	
	/**
	 * Sends a POST request to a given URL and returns the server's response.
	 * @param requestUrl	The URL to request from
	 * @param parameters	The POST parameters
	 * @return The received response, or null if the request failed
	 */
	
	public Response post(String requestUrl, 
			   			 Map<String, String> parameters, 
			   			 Map<String, String> cookies)
	{
		return performRequest(requestUrl, 
							  parameters, 
							  RequestMethod.POST, 
							  cookies);
	} // ==> post
	
	// ------------------------------------------------------------------------
	
} // ==> HTTPClient

// ============================================================================
