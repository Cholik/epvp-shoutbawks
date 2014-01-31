package com.epvp.shoutbawks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

// ============================================================================

/**
 * Preference activity.
 * @author Ende!
 */

public class SettingsActivity extends PreferenceActivity 
{

	// ------------------------------------------------------------------------
	// Public static subclasses
	
	/**
	 * Container for preference values.
	 */
	
	public static class Settings
	{
		
		// ------------------------------------------------
		// Private attributes
		
		private boolean colorizeNames;
		private float	chatUpdateRate;
		private boolean displayTimestamp;
		
		// ------------------------------------------------
		// Constructor
		
		public Settings(SharedPreferences prefs)
		{
			try
			{
				chatUpdateRate = Float.parseFloat(
						prefs.getString("update_rate", "10.0f")
						);
			}
			catch (NumberFormatException e)
			{
				chatUpdateRate = 10.f;
			}
			colorizeNames = prefs.getBoolean("colorized_names", true);
			displayTimestamp = prefs.getBoolean("display_timestamp", false);
		} // ==> ctor
		
		// ------------------------------------------------
		// Getters
		
		public boolean 	colorizeNames() 	{ return colorizeNames; 	}
		public float 	getChatUpdateRate() { return chatUpdateRate;	}
		public boolean	displayTimestamp()	{ return displayTimestamp;	}
		
		// ------------------------------------------------
		
	} // ==> Settings
	
	// ------------------------------------------------------------------------
	// Overridden methods of base class	
	
	@SuppressWarnings("deprecation") // use deprecated methods, as Google
									 // does not provide backports for new 
									 // preference method
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs);
		findPreference("update_rate").setOnPreferenceChangeListener(
				new OnPreferenceChangeListener() 
		{
			@Override
			public boolean onPreferenceChange(Preference pref, Object newVal) 
			{
				boolean parsingFailed 	= false;
				float	updateRate 		= 0.0f;
				try
				{
					updateRate = Float.parseFloat(newVal.toString());
				}
				catch (NumberFormatException e) 
				{
					parsingFailed = true;
				}
				
				if (parsingFailed)
				{
					Toast toast = Toast.makeText(
							SettingsActivity.this, 
							"Invalid value specified", 
							Toast.LENGTH_SHORT);
					toast.show();
					return false;
				}
				
				return true;
			} // ==> onPreferenceChange
		});
	} // ==> onCreate
	
	// ------------------------------------------------------------------------
	// Public static methods
	
	/**
	 * Retrieve current settings.
	 * @param ctx The context utilized to grab the SharedPreferences object.
	 * @return The current settings wrapped into a Settings object.
	 */
	
	public static Settings getSettings(Context ctx)
	{
		return new Settings(
				PreferenceManager.getDefaultSharedPreferences(ctx)
				);
	} // ==> getSettings
	
	// ------------------------------------------------------------------------
	
} // ==> SettingsActivity

// ============================================================================
