package com.epvp.shoutbawks;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.*;

import android.os.Parcel;
import android.os.Parcelable;

// ============================================================================

/**
 * Class capable of performing simple interaction with the elitepvpers forum.
 * @author Ende!
 */

class Epvp implements Parcelable
{
	
	// ------------------------------------------------------------------------
	// Public subclasses
	
	/**
	 * Exception class thrown in case a called method requires a successful
	 * login to work properly.
	 */
	
	public class NotLoggedInException extends Exception
	{

		// ------------------------------------------------
		// Public constructor
		
		public NotLoggedInException() 
		{
			super("There was no successful login, yet.");
		} // ==> ctor
		
		// ------------------------------------------------
		// Private constants
		
		private static final long serialVersionUID = 4571974144252479329L;
		
		// ------------------------------------------------
		
	} // ==> NotLoggedInException
	
	// ------------------------------------------------------------------------
	// Private constants
	
	private static final String BASE_URL = "http://www.elitepvpers.com/forum/";
	private static final String LOGIN_URL = BASE_URL + "login.php?do=login";
	private static final String SESS_RESTORE_URL = BASE_URL;
	private static final String SB_URL = BASE_URL + "mgc_cb_evo_ajax.php";
	private static final String BOARD_OVERVIEW_URL = BASE_URL + "/";
	private static final String GRAB_SB_SECURITY_TOK_URL 
			= BASE_URL + "mgc_cb_evo.php?do=view_chatbox";

	// ------------------------------------------------------------------------
	// Private attributes
	
	HashMap<String, String> cookies;
	String					cachedSbSecurityToken;
	String					cachedUsername;
	HTTPClient				http;

	// ------------------------------------------------------------------------
	// Public constructor
	
	/**
	 * Default constructor.
	 */
	
	public Epvp()
	{
		cookies 				= null;
		cachedSbSecurityToken 	= null;
		cachedUsername			= null;
		http					= new HTTPClient();
	} // ==> ctor
	
	// ------------------------------------------------------------------------
	// Public methods
	
	/**
	 * Performs a login using a username and the corresponding password
	 * @param username 	The username
	 * @param password 	The password
	 * @return true if the login succeeded, else false
	 */
	
	public boolean login(String username, String password)
	{
		try 
		{
			// Send login request via HTTP POST
			Map<String, String> params = new HashMap<String, String>();
			
			params.put("vb_login_username", username);
			params.put("vb_login_password", password);
			params.put("cookieuser", 		"1");
			params.put("securitytoken", 	"guest");
			params.put("do", 				"login");
			
			HTTPClient.Response reply = http.post(LOGIN_URL, params, null);
			
			// Check for success and process cookies
			if (reply == null || reply.getSetCookies().size() < 3)
				return false;
			
			HashMap<String, String> setCookies = reply.getSetCookies();
			
			if (!setCookies.containsKey("bbsessionhash")
					|| !setCookies.containsKey("bbuserid")
					|| !setCookies.containsKey("bbpassword"))
			{
				return false;
			}
									
			this.cookies = setCookies;
			return true;
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		return false;
	} // ==> login(String, String)

	// ------------------------------------------------------------------------
	
	/**
	 * Performs a login using a previously stored session.
	 * @param previousSession	The login session (retrieved via getSession)
	 * @return true in case of success, else false
	 * @see getSession
	 */
	
	public boolean login(EpvpLoginSession previousSession)
	{
		// Set persistent cookies and try to retrieve a session hash
		HashMap<String, String> cookies = new HashMap<String, String>();
		
		cookies.put("bbuserid", Integer.toString(previousSession.getUserId()));
		cookies.put("bbpassword", previousSession.getPasswordHash());
		
		HTTPClient.Response reply = http.get(SESS_RESTORE_URL, null, cookies);
		
		// Check if we were able to grab a session hash
		if (reply == null 
				|| !reply.getSetCookies().containsKey("bbsessionhash"))
		{
			return false;
		}
		
		cookies.putAll(reply.getSetCookies());
		this.cookies 		= cookies;
		this.cachedUsername = previousSession.getUsername();
		
		return true;
	} // ==> login(LoginSession)
	
	// ------------------------------------------------------------------------
	
	/**
	 * Retrieves the content of the premium shoutbox
	 * @param channelId	The channel ID (0 = general, 1 = English)
	 * @return The HTML/XML code of the current shoutbox content or null in
	 * 		   case the request failed
	 * @throws NotLoggedInException
	 */
	
	public String getShoutboxContent(int channelId) throws NotLoggedInException
	{
		if (cookies == null)
			throw new NotLoggedInException();
		
		if (cachedSbSecurityToken == null)
		{
			cachedSbSecurityToken = grabSbSecurityToken();
			if (cachedSbSecurityToken == null)
				return null;
		}
		
		HashMap<String, String> params = new HashMap<String, String>();
		
		params.put("channel_id",	Integer.toString(channelId));
		params.put("do", 			"ajax_refresh_chat");
		params.put("first_load", 	"0");
		params.put("location",		"full");
		params.put("s", 			"");
		params.put("securitytoken",	cachedSbSecurityToken);
		params.put("status", 		"open");
		
		HTTPClient.Response reply = http.post(SB_URL, params, cookies);
		
		// Request failed? Refresh token and try again
		if (reply == null)
		{
			cachedSbSecurityToken = grabSbSecurityToken();
			if (cachedSbSecurityToken == null)
				return null;
			http.post(SB_URL, params, cookies);
		}
		
		return (reply == null) ? null : reply.getBody();
	} // ==> getShoutboxContent
	
	// ------------------------------------------------------------------------
	
	/**
	 * Adds a message to the premium shoutbox.
	 * @param message The message to send.
	 * @param channelId The ID of the channel to send the message to.
	 * @return true in case of success, else false.
	 * @throws NotLoggedInException
	 */
	
	public boolean sendSbMessage(String message, 
								 int channelId) throws NotLoggedInException
	{
		if (cookies == null)
			throw new NotLoggedInException();
		
		if (cachedSbSecurityToken == null)
		{
			cachedSbSecurityToken = grabSbSecurityToken();
			if (cachedSbSecurityToken == null)
				return false;
		}
		
		// Send request to server
		HashMap<String, String> params = new HashMap<String, String>();
		
		params.put("do", 			"ajax_chat");
		params.put("channel_id", 	Integer.toString(channelId));
		params.put("chat", 			message);
		params.put("securitytoken", cachedSbSecurityToken);
		params.put("s", 			"");
		
		HTTPClient.Response reply = http.post(SB_URL, params, cookies);
		
		// Request failed? Grab new security token and try again
		if (reply == null)
		{
			cachedSbSecurityToken = grabSbSecurityToken();
			if (cachedSbSecurityToken == null)
				return false;
			reply = http.post(SB_URL, params, cookies);
		}
		
		// Send HTTP request posting the message
		return (reply != null);
	} // ==> sendSbMessage
	
	// ------------------------------------------------------------------------
	
	/**
	 * Retrieves a fresh security token for usage with the EVO shoutbox.
	 * @return In case of a successful request, the security token, else null.
	 * @throws NotLoggedInException
	 */
	
	public String grabSbSecurityToken() throws NotLoggedInException
	{
		if (cookies == null)
			throw new NotLoggedInException();
	
		// Request HTML code of SB page
		HTTPClient.Response reply = http.get(
				GRAB_SB_SECURITY_TOK_URL, 
				null, 
				cookies
				);
		
		if (reply == null)
			return null;
		
		// Find security token via regular expression
		Pattern p = Pattern.compile("var SECURITYTOKEN = \"(.*?)\";");
		Matcher m = p.matcher(reply.getBody());
		
		if (!m.find())
			return null;
		
		return m.group(1);
	} // ==> grabSbSecurityToken
	
	// ------------------------------------------------------------------------
	
	/**
	 * Retrieves the username of the currently logged in account.
	 * @return The username in case of success, or null if the request fails.
	 * @throws NotLoggedInException
	 */
	
	public String getUsername() throws NotLoggedInException
	{
		if (cookies == null)
			throw new NotLoggedInException();
		
		if (cachedUsername == null)
		{
			// Send request
			HTTPClient.Response reply = http.get(
					BOARD_OVERVIEW_URL, 
					null, 
					cookies
					);
			
			if (reply == null)
				return null;
			
			// Grab username
			Pattern p = Pattern.compile(
					"<ul id=\"userbaritems\">\\s*?"
					+ "<li>.*?<a rel=\"nofollow\" href.*?>(.*?)</a>"
					);
			Matcher m = p.matcher(reply.getBody());
			
			if (!m.find())
				return null;
			
			cachedUsername = m.group(1);
		}
		
		return cachedUsername;
	} // ==> getUsername

	// ------------------------------------------------------------------------
	
	/**
	 * Retrieves the user ID.
	 * @return The user ID in case of success, else -1.
	 * @throws NotLoggedInException
	 */
	
	public int getUserId() throws NotLoggedInException
	{
		if (cookies == null)
			throw new NotLoggedInException();
		
		int ret = -1;
		try
		{ 
			String temp = cookies.get("bbuserid");
			if (temp == null) 
				return -1;
			ret = Integer.parseInt(temp);
		}
		catch (NumberFormatException e) {}
		
		return ret;
	} // ==> getUserId

	// ------------------------------------------------------------------------
	
	/**
	 * Retrieves the password hash of the currently logged in account.
	 * @return The password hash in case of success, else null
	 * @throws NotLoggedInException
	 */
	
	public String getPasswordHash() throws NotLoggedInException
	{
		if (cookies == null) 
			throw new NotLoggedInException();
		
		return cookies.get("bbpassword");
	} // ==> getPasswordHash
	
	// ------------------------------------------------------------------------
	
	/**
	 * Retrieves the current login session for storage for later usage
	 * @return The current login session
	 * @throws NotLoggedInException
	 */
	
	public EpvpLoginSession getSession() throws NotLoggedInException
	{
		if (cookies == null)
			throw new NotLoggedInException();
		
		return new EpvpLoginSession(getUsername(), 
									getUserId(), 
									getPasswordHash());
	} // ==> getSession

	// ------------------------------------------------------------------------
	// Implementation of Parcelable interface
	
	/**
	 * Constructor used for creating an instance from a parcel.
	 * @param in The parcel to load data from.
	 */
	
	@SuppressWarnings("unchecked") // we're sure everything is OK
	private Epvp(Parcel in)
	{
		cookies = (HashMap<String, String>)in.readSerializable();
		cachedSbSecurityToken = in.readString();
		cachedUsername = in.readString();
		http = new HTTPClient();
	} // ==> ctor(Parcel)
	
	// ------------------------------------------------------------------------
	
	public static final Parcelable.Creator<Epvp> CREATOR
			= new Parcelable.Creator<Epvp>() 
			{
				@Override
				public Epvp createFromParcel(Parcel source) 
				{
					return new Epvp(source);
				} // ==> createFromParcel
		
				@Override
				public Epvp[] newArray(int size) 
				{
					return null; // TODO: check if returning null is OK here
				} // ==> newArray
			}; // ==> CREATOR
	
	// ------------------------------------------------------------------------
	
	@Override
	public int describeContents() 
	{
		return 0;
	} // ==> describeContents

	// ------------------------------------------------------------------------
	
	@Override
	public void writeToParcel(Parcel dest, int flags) 
	{
		dest.writeSerializable(cookies);
		dest.writeString(cachedSbSecurityToken);
		dest.writeString(cachedUsername);
	} // ==> writeToParcel
	
	// ------------------------------------------------------------------------
	
} // ==> Epvp

// ============================================================================