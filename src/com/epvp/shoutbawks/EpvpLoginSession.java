package com.epvp.shoutbawks;

import java.io.Serializable;

// ============================================================================

/**
 * Container class designed for (re)storing an e*pvp login session.
 * @author Ende!
 */

public class EpvpLoginSession implements Serializable
{
	
	// ------------------------------------------------------------------------
	// Public constructor
	
	/**
	 * Constructor.
	 * @param username	The logged in username
	 * @param userId	The logged in user's ID
	 * @param passHash	The corresponding password hash
	 * @throws IllegalArgumentException Thrown in case the session data is
	 * 									invalid
	 */
	
	public EpvpLoginSession(String username, int userId, String passHash)
	{
		if (username == null || userId == -1 || passHash == null)
			throw new IllegalArgumentException("session data is invalid");
		
		this.username 		= username;
		this.userId 		= userId;
		this.passwordHash 	= passHash;
	} // ==> ctor
	
	// ------------------------------------------------------------------------
	// Public getters
	
	public String 	getUsername() 		{ return username; 		}
	public String 	getPasswordHash() 	{ return passwordHash; 	}
	public int 		getUserId()			{ return userId;		}
	
	// ------------------------------------------------------------------------
	// Private attributes
	
	private String	username;
	private Integer userId;
	private String	passwordHash;

	// ------------------------------------------------------------------------
	// Private constants
	
	private static final long serialVersionUID = -2970344149643715546L;
	
	// ------------------------------------------------------------------------
	
} // ==> EpvpLoginSession

// ============================================================================
