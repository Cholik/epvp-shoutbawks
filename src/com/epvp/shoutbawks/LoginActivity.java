package com.epvp.shoutbawks;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.epvp.shoutbawks.Epvp.NotLoggedInException;

// ============================================================================

/** 
 * Main activity performing the initial login.
 * @author Ende!
 */

public class LoginActivity extends Activity 
{
	
	// ------------------------------------------------------------------------
	// Public constants
	
	public final static String LOGGED_OUT_EXTRA 	= "logged_out";
	public final static String EPVP_INSTANCE_EXTRA	= "epvp_instance";
	public final static String SESSION_PREF_KEY 	= "login_session";
	
	// ------------------------------------------------------------------------
	// Private attributes
	
	private	Epvp 			epvp;
	private ProgressDialog	loadingIndicator;

	// ------------------------------------------------------------------------
	// Constructor
	
	/**
	 * Default constructor.
	 */
	
	public LoginActivity()
	{
		epvp 				= new Epvp();
		loadingIndicator 	= null;
	} // ==> ctor
	
    // ------------------------------------------------------------------------
	// Overridden methods of base class
	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        // Did user just log out?
        if (getIntent().getBooleanExtra(LOGGED_OUT_EXTRA, false))
        	return;
        
        // Check for previously saved session
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        String prevSession 		= prefs.getString(SESSION_PREF_KEY, null);
        
        if (prevSession != null)
        {
        	// Found one, try to restore ...
        	try 
        	{
        		ObjectInputStream ois = new ObjectInputStream(
						new ByteArrayInputStream(
								Base64.decode(prevSession, Base64.DEFAULT)
								)
						);
				
				final EpvpLoginSession sess 
						= (EpvpLoginSession)ois.readObject();
				
				// Everything seems to be OK, restore data
				((EditText)findViewById(R.id.tfUsername))
						.setText(sess.getUsername());
				
				// Try to login with session data
				indicateLoading(true);
				new AsyncTask<Void, Void, Boolean>()
				{

					@Override
					protected Boolean doInBackground(Void... dontCare) 
					{
						return epvp.login(sess);
					} // ==> doInBackground
					
					@Override
					protected void onPostExecute(Boolean result)
					{
						if (result)
						{
							// Success! Switch to chat activity
		    				Intent intent = new Intent(
		    						LoginActivity.this, 
		    						ChatActivity.class
		    						);
		    				intent.putExtra(EPVP_INSTANCE_EXTRA, epvp);
		    				startActivity(intent);
						}
						else
						{
							Toast toast = Toast.makeText(
									LoginActivity.this, 
									getString(R.string.login_failed), 
									Toast.LENGTH_SHORT);
							toast.show();
						}
						indicateLoading(false);
					} // ==> onPostExecute
					
				}
				.execute();
				
			}
			// Something is wrong with the stored session, ignore it.
        	catch (NumberFormatException e) {}
        	catch (ClassNotFoundException e) {}
        	catch (IOException e) {}
        }
    } // ==> onCreate

    // ------------------------------------------------------------------------
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        getMenuInflater().inflate(R.menu.activity_login, menu);
        return true;
    } // ==> onCreateOptionsMenu
    
    // ------------------------------------------------------------------------
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	if (item.getItemId() == R.id.menu_settings)
    	{
        	displaySettings(this);
        	return true;
        }

        return super.onOptionsItemSelected(item);
    } // ==> onOptionsItemSelected

    // ------------------------------------------------------------------------
    // Callbacks for UI widgets
    
    /**
     * Handler for login button.
     * @param view	The tapped button.
     */
    
    public void onLoginTapped(View view)
    {
    	indicateLoading(true);
    	
    	// Perform login asynchronously
		new AsyncTask<Void, Void, Boolean>()
    	{
    		@Override
    		protected Boolean doInBackground(Void... dontCare) 
    		{
    			EditText username = (EditText)findViewById(R.id.tfUsername);
    			EditText password = (EditText)findViewById(R.id.tfPassword);
    			boolean ret = epvp.login(username.getText().toString(), 
    							  		 password.getText().toString());
    			
    			if (ret)
    			{
    				try 
    				{
						epvp.getUserId();
						epvp.getUsername();
						epvp.getPasswordHash();
					}
    				catch (NotLoggedInException e) 
					{
						e.printStackTrace();
					}
    			}

    			return ret;
    		} // ==> doInBackground
    		
    		@Override
    		protected void onPostExecute(Boolean result)
    		{
    			// Login successful?
    			if (result)
    			{
    				// Save login session?
    				if (((CheckBox)findViewById(R.id.cbxStayLoggedIn))
    						.isChecked())
    				{
    					// Serialize session to hex string
    					String serializedSession = null;
    					try 
						{
    						ByteArrayOutputStream bs 
    								= new ByteArrayOutputStream();
        					ObjectOutputStream oos 
        							= new ObjectOutputStream(bs);
							oos.writeObject(epvp.getSession());
	    					oos.close();
	    					
	    					serializedSession = Base64.encodeToString(
	    							bs.toByteArray(), 
	    							Base64.DEFAULT
	    							);
						}
						catch (IOException e) {} // TODO: handle this better 
    					catch (NotLoggedInException e) {} 
    					
    					if (serializedSession != null)
    					{
	    					// Save into app's shared preferences
	    					SharedPreferences.Editor editor 
	    							= getPreferences(MODE_PRIVATE).edit();
	    					editor.putString(SESSION_PREF_KEY, 
	    								     serializedSession);
	    					editor.commit();
    					}
    				}
    				
    				// Switch to chat activity
    				Intent intent = new Intent(
    						LoginActivity.this, 
    						ChatActivity.class
    						);
    				intent.putExtra(EPVP_INSTANCE_EXTRA, epvp);
    				startActivity(intent);
    			}
    			else // Nope, output alert and allow user to retry.
    			{
	    			AlertDialog alert = new AlertDialog
	    					.Builder(LoginActivity.this)
	    					.create();
	    			
	    			alert.setTitle(getString(R.string.login));
	    			alert.setMessage(getString(R.string.login_failed));
	    			alert.setButton(AlertDialog.BUTTON_NEUTRAL,
	    							getString(R.string.ok),
	    							new DialogInterface.OnClickListener()
	    			{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							return;
						}
					});
	    			
	    			alert.show();
    			}
    			
    			indicateLoading(false);
    		} // ==> onPostExecute
    	}
		.execute();
    } // ==> onLoginTapped
    
    // ------------------------------------------------------------------------
    // Private methods
    
    /**
     * Determines the current screen orientation
     * @return The current screen orientation.
     */
    
    @SuppressWarnings("deprecation") // no backports for new API available :(
	public int getScreenOrientation()
    {
        Display display = getWindowManager().getDefaultDisplay();
        int orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        if(display.getHeight() == display.getWidth()) // TODO fix square mode
            orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
        else
        {
        	if(display.getWidth() < display.getHeight())
            	orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            else 
            	orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        }
        return orientation;
    } // ==> getScreenOrientation
    
    // ------------------------------------------------------------------------
    
    /**
     * Whether spawns or hides the loading indicator (progress dialog) and
     * dis/enables the login button.
     * @param show true to show, false to hide the dialog
     */
    
    private synchronized void indicateLoading(boolean show)
    {
    	if (show)
    	{
    		if (loadingIndicator == null)
    		{
    			// Create and show loading indicator
    			loadingIndicator = new ProgressDialog(this);
    			loadingIndicator.setMessage(
    					getString(R.string.login_in_progress)
    					);
    			loadingIndicator.setCancelable(false);
    			loadingIndicator.setCanceledOnTouchOutside(false);
    			loadingIndicator.show();
    			
    			// Lock screen orientation
    			setRequestedOrientation(getScreenOrientation());
    		}
    	}
    	else
    	{
    		if (loadingIndicator != null)
    		{
    			// Free indicator
    			loadingIndicator.dismiss();
    			loadingIndicator = null;
    			
    			// Unlock orientation
    			setRequestedOrientation(
    					ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    					);
    		}
    	}
    } // ==> indicateLoading
    
    // ------------------------------------------------------------------------
    // Public static methods
    
    /**
     * Switches to the settings activity
     * @param context
     */
    
    public static void displaySettings(Context context)
    {
    	Intent intent = new Intent(context, SettingsActivity.class);
    	context.startActivity(intent);
    } // ==> displaySettings
    
    // ------------------------------------------------------------------------
    
} // ==> LoginActivity

// ============================================================================
