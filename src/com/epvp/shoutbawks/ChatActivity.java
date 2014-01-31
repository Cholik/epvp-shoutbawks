package com.epvp.shoutbawks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.epvp.shoutbawks.Epvp.NotLoggedInException;

// ============================================================================

/**
 * Secondary activity displaying the elitepvpers's premium shoutbox.
 * @author Ende!
 */

public class ChatActivity extends Activity 
{

	// ------------------------------------------------------------------------
	// Private constants
	
	private static final Map<String, String> SMILEY_REPLACEMENTS
			= initSmileyReplacementMap();
	private static final int MIN_UPDATE_WAIT_TIME = 5000; // milliseconds
	
	// ------------------------------------------------------------------------
	// Private subclasses
	
	/**
	 * Container representing a chat-channel page.
	 */
	
	private class ChatPage
	{
		
		// ------------------------------------------------
		// Private properties
		
		private String 						title;
		private int							channelId;
		private ChatAdapter					adapter;
		private ArrayList<ChatAdapter.Row>	chatContent;
		private Drawable					flag;
		private long						lastUpdateTime;
		private View						associatedView;
		private boolean						firstDataUpdate;
		
		// ------------------------------------------------
		// Constructor
		
		public ChatPage(String title, int channelId, Drawable flag)
		{
			this.title = title;
			this.channelId = channelId;
			this.flag = flag;
			
			chatContent = new ArrayList<ChatAdapter.Row>();
			adapter = new ChatAdapter(ChatActivity.this, chatContent);
			lastUpdateTime = 0;
			associatedView = null;
			firstDataUpdate = true;
		} // ==> ctor
		
		// ------------------------------------------------
		// Getters
		
		public String 					getTitle() 		{ return title; 	  }
		public int 						getChannelId() 	{ return channelId;   }
		public ChatAdapter 				getAdapter() 	{ return adapter;	  }
		public List<ChatAdapter.Row>	getContent()	{ return chatContent; }
		public Drawable					getFlag()		{ return flag;		  }
		
		// ------------------------------------------------
		
		public View getAssociatedView() 
		{
			return associatedView;
		} // ==> getAssociatedView
		
		// ------------------------------------------------
		
		public long getLastUpdateTime() 
		{
			return lastUpdateTime; 
		} // ==> getLastUpdateTime
		
		// ------------------------------------------------
		// Setters
		
		public void setAssociatedView(View view)
		{
			associatedView = view;
		} // ==> setAssociatedView
		
		// ------------------------------------------------
		// Public methods
		
		/**
		 * Retrieves the highest message ID in the list.
		 * @return
		 */
		
		public int getLastMsgId()
		{
			int highestId = 0;
			for (ChatAdapter.Row row : chatContent)
				if (row.getId() > highestId)
					highestId = row.getId();
			return highestId;
		} // ==> getLastMsgId
		
		// ------------------------------------------------
		
		/**
		 * Call this after page's data was updated.
		 */
		
		public void notifyDataUpdate()
		{
			adapter.notifyDataSetChanged();
			lastUpdateTime = System.currentTimeMillis();
			
			// If we have an associated view and this is the first data
			// feeding, scroll to bottom
			if (firstDataUpdate && associatedView != null)
			{
				ListView lv = (ListView)associatedView
						.findViewById(R.id.lstSbContent);
				lv.setSelection(lv.getAdapter().getCount() - 1);
				firstDataUpdate = false;
			}
		} // ==> notifyDataUpdate
		
		// ------------------------------------------------
		
	} // ==> ChatPage
	
	// ------------------------------------------------------------------------
	
	/**
	 * Adapter for the slide-view used to switch between chat channels.
	 */
	
	private class ChatPagerAdapter extends PagerAdapter
	{

		// ------------------------------------------------
		// Private properties
		
		private List<ChatPage> pages;
		
		// ------------------------------------------------
		// Constructor
		
		public ChatPagerAdapter(List<ChatPage> pages)
		{
			this.pages = pages;
		} // ==> ctor
		
		// ------------------------------------------------
		// Overridden methods of base class
		
		@Override
		public int getCount()
		{
			return pages.size();
		} // ==> getCount
		
		// ------------------------------------------------

		@Override
		public boolean isViewFromObject(View view, Object obj)
		{
			return view.equals(obj);
		} // ==> isViewFromObject
		
		// ------------------------------------------------
		
		@Override
		public Object instantiateItem(ViewGroup container, int position)
		{
			ChatPage pageData = pages.get(position);
			
			// Inflate fresh page from XML
			View page = ((LayoutInflater)ChatActivity.this.getSystemService(
					Context.LAYOUT_INFLATER_SERVICE
					)).inflate(R.layout.chat_page, null);
			
			// Create and set adapter for LineEdit
	        ((ListView)page.findViewById(R.id.lstSbContent)).setAdapter(
	        		pageData.getAdapter()
	        		);
	        
	        // Set channel flag
	        ((ImageView)page.findViewById(R.id.channelFlag))
	        		.setImageDrawable(pageData.getFlag());
			
	        pageData.setAssociatedView(page);
			container.addView(page);
			return page;
		} // ==> instantiateItem
		
		// ------------------------------------------------
		
		@Override
		public void destroyItem(ViewGroup container, 
								int position, 
								Object object)
		{
			container.removeView((View)object);
		} // ==> destroyItem

		// ------------------------------------------------
		
		@Override
		public CharSequence getPageTitle(int position)
		{
			return pages.get(position).getTitle();
		} // ==> getPageTitle

		// ------------------------------------------------
				
	} // ==> ChatPagerAdapter
	
	// ------------------------------------------------------------------------
	// Private attributes
	
	private Thread 				updateThread;
	private Epvp				epvp;
	private Animation			fadeIn;
	private Animation			fadeOut;
	private Drawable			statusTextBgError;
	private Drawable			statusTextBgInfo;
	private List<ChatPage>		channelPages;
	private ChatPagerAdapter	pagerAdapter;
	private ChatPage			activePage;
	
	private final Pattern		messageSlicer;
	private final Pattern		messageParser;
	private final Pattern		smileyFinder;
	private final Pattern		nameColorExtractor;	

    // ------------------------------------------------------------------------
	// Constructor
	
	/**
	 * Default constructor.
	 */
	
	public ChatActivity() 
	{
		updateThread	= null;
		epvp			= null;
		
		// Precompile frequently used regexp patterns
		messageSlicer = Pattern.compile(
				"<td.*?id=\"chat_(\\d*)\".*?>(.*?)<!-- Don't align chats -->", 
				Pattern.DOTALL
				);
		messageParser = Pattern.compile(
				"<span class=\"mgc_cb_evo_date\">\\s*(.*?)&nbsp;.*?" // time
				+ "html\">&lt;((?:<.*?>)?.*(?:</span>)?)&gt;.*?" // username
				+ "<span class=\"normalfont\">\\s*(.*?)\\s*</span>", // message
				Pattern.DOTALL
				);
		smileyFinder = Pattern.compile(
				"<img .*? src=\".*?smilies/(.*?)\".*?class=\"inlineimg\" />" 
				);
		nameColorExtractor = Pattern.compile(
				"<span style=\"color:(.*?)\">(.*?)</span>"
				);
	} // ==> ctor
	
    // ------------------------------------------------------------------------
	// Overridden methods of base class
		
   	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        
        // Retrieve data from intent
        epvp = getIntent()
        		.getParcelableExtra(LoginActivity.EPVP_INSTANCE_EXTRA);
        
        // Load animations
 		fadeIn	= AnimationUtils.loadAnimation(this, R.anim.status_fade_in);
 		fadeOut	= AnimationUtils.loadAnimation(this, R.anim.status_fade_out);
 		
 		// Load drawables
 		statusTextBgInfo = getResources()
 				.getDrawable(R.drawable.status_text_bg_info);
 		statusTextBgError = getResources()
 				.getDrawable(R.drawable.status_text_bg_error);
        
 		// Create chat page for each channel
 		channelPages = new ArrayList<ChatPage>();
 		channelPages.add(new ChatPage(
 				"German", 
 				0, 
 				getResources().getDrawable(R.drawable.de_flag)
 				));
 		channelPages.add(new ChatPage(
 				"English", 
 				1, 
 				getResources().getDrawable(R.drawable.us_flag)
 				));
 		
 		pagerAdapter = new ChatPagerAdapter(channelPages);
 		ViewPager pager = ((ViewPager)findViewById(R.id.chatPager));
 		pager.setAdapter(pagerAdapter);
 		pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() 
 		{
			@Override
			public void onPageSelected(int pos) 
			{
				activePage = channelPages.get(pos);
				
				// Update chat
				new Thread(new Runnable()
				{
					@Override
					public void run()
					{
						updateChat(activePage, false);
					}
				}).start();
			} // ==> onPageSelected
			
			// We don't need those
			@Override
			public void onPageScrolled(int pos, float posOff, int posOffPx) {}
			@Override 
			public void onPageScrollStateChanged(int state) {}
		});
 		
 		activePage = channelPages.get(0);
 		
        // Register handler for "Send" button on keyboard
        EditText tfMsg = (EditText)findViewById(R.id.tfMessage);
        tfMsg.setOnEditorActionListener(new OnEditorActionListener() 
        {
			@Override
			public boolean onEditorAction(TextView view, 
										  int actionId, 
										  KeyEvent event)
			{
				if (actionId == EditorInfo.IME_ACTION_SEND)
				{
					onSendTapped(view);
					return true;
				}
				
				return false;
			} // ==> onEditorAction
		});
    } // ==> onCreate

    // ------------------------------------------------------------------------

   	@Override
   	protected void onResume() 
   	{
   		super.onResume();
   		
   		// Fire update thread (if not already done)
   		if (updateThread == null)
   		{
	   		updateThread = new Thread(new Runnable()
	   		{
				@Override
				public void run() 
				{
					while (true)
					{
						updateChat(activePage, false);
						try 
						{
							synchronized (this) 
							{
								this.wait((int)(SettingsActivity.getSettings(
										ChatActivity.this
										).getChatUpdateRate() * 1000.f));
							}
						} 
						catch (InterruptedException e) 
						{
							return;
						}
					}
				} // ==> run
	   		});
	   		updateThread.start();
   		}
   	} // ==> onResume

    // ------------------------------------------------------------------------
   	
   	@Override
   	protected void onPause() 
    {
   		super.onPause();
   		
   		// Stop update thread
   		if (updateThread != null)
   		{
   			updateThread.interrupt();
   			updateThread = null;
   		}   		
   	} // ==> onPause

    // ------------------------------------------------------------------------
   	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        getMenuInflater().inflate(R.menu.activity_chat, menu);
        return true;
    } // ==> onCreateOptionsMenu
    
    // ------------------------------------------------------------------------    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	switch (item.getItemId())
    	{
            case R.id.menu_logout:
            	logout();           	
                return true;
            case R.id.menu_settings:
            	LoginActivity.displaySettings(this);
            	return true;
            case R.id.menu_about:
            	showAbout();
            	return true;
            case R.id.menu_refresh:
            	// Do chat update off the UI thread
            	new Thread(new Runnable() 
            	{
					@Override
					public void run() 
					{
						updateChat(activePage, true);
					} // ==> run
				}).start();
            	return true;
        }

        return super.onOptionsItemSelected(item);
    } // ==> onOptionsItemSelected
    
    // ------------------------------------------------------------------------
    // Callbacks for UI widgets
    
    /**
     * Callback for "Send" button.
     * @param view	The affected view.
     */
    
    public void onSendTapped(View view)
    {
    	final EditText 	edt = (EditText)findViewById(R.id.tfMessage);
    	final Button 	btn = (Button)findViewById(R.id.btnSendMessage);
    	
    	edt.setEnabled(false);
    	btn.setEnabled(false);
    	
		new AsyncTask<Void, Void, Boolean>()
		{
			@Override
			protected Boolean doInBackground(Void... params) 
			{
				try 
		    	{
					// invisible prefix to detect messages sent by the app
					return epvp.sendSbMessage(
							edt.getText().toString(),
							activePage.getChannelId()
							);
		    	}
				catch (NotLoggedInException e)
		    	{
					handleNotLoggedInException();
					return false;
				}
			} // ==> doInBackground

			@Override
			protected void onPostExecute(Boolean result) 
			{
				if (!result)
				{
					AlertDialog alert = new AlertDialog
								.Builder(ChatActivity.this)
								.create();
					alert.setTitle(getString(R.string.error));
					alert.setMessage(getString(R.string.could_not_send_msg));
					alert.setButton(AlertDialog.BUTTON_NEUTRAL, 
								    getString(R.string.ok), 
								    new AlertDialog.OnClickListener() 
					{						
						@Override
						public void onClick(DialogInterface arg0, int arg1) 
						{
							return;
						} // ==> onClick
					});
				}
				
				edt.setText("");
				edt.setEnabled(true);
				btn.setEnabled(true);
			} // ==> onPostExecute
		}
		.execute();    	
    } // ==> onSendTapped
    
    // ------------------------------------------------------------------------
    
    @Override
    public void onBackPressed()
    {
    	// ignore.
    } // ==> onBackPressed
    
    // ------------------------------------------------------------------------
    // Private methods
    
    /**
     * Updates the chat view of currently active channel.
     * @param updatePage The channel page to update.
     * @param manual True if the update was requested manually, else false.
     */
    
    private void updateChat(final ChatPage updatePage, boolean manual)
    {
    	// If no manual update, check if last update was at least
    	// MIN_UPDATE_WAIT_TIME milliseconds before and if not, just return.
    	if (!manual && updatePage.getLastUpdateTime() + MIN_UPDATE_WAIT_TIME
    			> System.currentTimeMillis())
    	{
    		return;
    	}
    	
    	showStatus(getString(R.string.updating), false);
    	
    	// Retrieve data
    	try 
    	{
			final String sbContent = epvp.getShoutboxContent(
					activePage.getChannelId()
					);
			
			if (sbContent == null)
			{
				showStatus(getString(R.string.updating_failed), true, 2000);
				return;
			}
			
			// Extract messages and create map
			Matcher msgMatcher = messageSlicer.matcher(sbContent);
			final TreeMap<Integer, String> messages 
					= new TreeMap<Integer, String>();
			while (msgMatcher.find())
			{
				try
				{
					messages.put(
							Integer.parseInt(msgMatcher.group(1)), 
							msgMatcher.group(2)
							);
				}
				catch (NumberFormatException e)
				{
					Log.w("shoutbawks", "Unable to parse message");
					continue;
				}
			}
			
			// Update view
			runOnUiThread(new Runnable()
			{	
				@Override
				public void run() 
				{
					for (Entry<Integer, String> curMsg : messages.entrySet()) 
					{
						// Is old message? If so, continue looping
						if (curMsg.getKey() <= updatePage.getLastMsgId())
							continue;
						
						// Parse message and push it into the chat view
						Matcher parser 
								= messageParser.matcher(curMsg.getValue());
						
						if (!parser.find())
						{
							Log.w("shoutbawks", "Unable to parse message");
							continue;
						}
						
						updatePage.getContent().add(new ChatAdapter.Row(
								formatUsername(parser.group(2)), 
								replaceSmilies(parser.group(3)),
								parser.group(1),
								curMsg.getKey()
								));
					}
					
					// Flush data into widget
					updatePage.notifyDataUpdate();
					hideStatus();
				} // ==> run
			});			
		}
    	catch (NotLoggedInException e)
    	{
			handleNotLoggedInException();
			return;
		}
    } // ==> updateChat
    
    // ------------------------------------------------------------------------
    
    /**
     * Handles a NotLoggedInException exception.
     */
    
    private void handleNotLoggedInException()
    {
    	// Fire alert and throw user back to login screen
    	AlertDialog alert = new AlertDialog.Builder(this).create();
    	
    	alert.setMessage(getText(R.string.not_logged_in));
    	alert.setTitle(getText(R.string.error));
    	alert.setCancelable(false);
    	alert.setCanceledOnTouchOutside(false);
    	alert.setButton(AlertDialog.BUTTON_NEUTRAL,
    					getText(R.string.ok),
    					new AlertDialog.OnClickListener()
    	{			
			@Override
			public void onClick(DialogInterface arg0, int arg1) 
			{
				logout();
			} // ==> onClick
		});
    } // ==> handleNotLoggedInException
    
    // ------------------------------------------------------------------------
    
    /**
     * Logs the user out and switches back to the login activity.
     */
    
    private void logout()
    {
    	// Clear stored session
    	SharedPreferences.Editor editor 
    			= getPreferences(MODE_PRIVATE).edit();
    			
    	editor.remove("login_session");
    	editor.commit();
    	
    	// Return to login activity
    	Intent intent = new Intent(this, LoginActivity.class);
    	intent.putExtra(LoginActivity.LOGGED_OUT_EXTRA, true);
    	startActivity(intent);
    } // ==> logout
    
    // ------------------------------------------------------------------------
    
    /**
     * Initialized the smiley replacement map.
     * @return The smiley replacement map.
     */
    
    private static Map<String, String> initSmileyReplacementMap()
    {
    	HashMap<String, String> map = new HashMap<String, String>();
    	
    	map.put("smile.gif", 		":)");
    	map.put("frown.gif", 		":(");
    	map.put("confused.gif", 	"oô");
    	map.put("wink.gif", 		";)");
    	map.put("mad.gif", 			";O");
    	map.put("tongue.gif", 		":p");
    	map.put("redface.gif", 		":o");
    	map.put("biggrin.gif", 		":D");
    	map.put("eek.gif", 			":O");
    	
    	map.put("rtfm.gif", 		"/rtfm/");
    	map.put("pimp.gif", 		"/pimp/");
    	map.put("mofo.gif", 		"/mofo/");
    	map.put("bandit.gif", 		"/bandit/");
    	map.put("handsdown.gif",	"/handsdown/");
    	map.put("rolleyes.gif", 	"/rolleyes/");
    	map.put("cool.gif", 		"/cool/");
    	map.put("facepalm.gif", 	"/facepalm/");
    	map.put("awesome.gif", 		"/awesome/");
    	
    	return Collections.unmodifiableMap(map);
    } // ==> initReplacementMap
       
    // ------------------------------------------------------------------------
    
    /**
     * Replaces all smiley image occurrences in a string with a text version.
     * @param stringToParse	The string to parse.
     * @return The string with all smiley images replaced.
     */
    
    private String replaceSmilies(String stringToParse)
    {
    	String	stripped	= new String(stringToParse);
    	Matcher matcher 	= smileyFinder.matcher(stringToParse);
    	
    	if (matcher == null)
    		return stringToParse;
    	
    	while (matcher.find())
    	{
    		String replacement = SMILEY_REPLACEMENTS.get(matcher.group(1));
    		
    		if (replacement == null)
    		{
    			Log.w("shoutbawks", "unknown smiley");
    			continue;
    		}
    		
    		stripped = stripped.replace(matcher.group(), replacement);
    	}
    	
    	return stripped;
    } // ==> replaceSmilies
    
    // ------------------------------------------------------------------------
    
    /**
     * Translates a received username block to a Html.fromHtml parsable string. 
     * @param unformattedUsername The received username HTML block.
     * @return The translated username.
     */
    
    private String formatUsername(String unformattedUsername)
    {
    	if (SettingsActivity.getSettings(this).colorizeNames())
    	{
    		// Reformat color notation
	    	Matcher m = nameColorExtractor.matcher(unformattedUsername);
	    	if (m.find())
	    	{
	    		String color = m.group(1);
	    		
	    		// Special favor for GMods (TagSoup doesn't know "orange")
	    		if (color.equals("orange"))
	    			color = "#FFA500";
	    		
	    		return String.format(
	    				"<font color='%s'>%s</font>",
	    				color,
	    				m.group(2)
	    				);
	    	}
    	}
    	
    	return unformattedUsername;
    } // ==> formatUsername

    // ------------------------------------------------------------------------
    
    /**
     * Shows the status notification in the top area of the screen and updates
     * the shown text.
     * @param status 	The status text.
     * @param error		Decides if status is an error or not.
     * @see hideStatus
     */
    
	private void showStatus(final String status, final boolean isError)
    {
		runOnUiThread(new Runnable() 
		{
			@Override
			public void run() 
			{
				final TextView lblStatus 
						= (TextView)findViewById(R.id.lblStatus);
		    	lblStatus.setText(status);
		    	
		    	// Red background in case of error, else green
		    	if (isError)
		    		lblStatus.setBackgroundDrawable(statusTextBgError);
		    	else
		    		lblStatus.setBackgroundDrawable(statusTextBgInfo);
		    	
		    	lblStatus.startAnimation(fadeIn);
		    	lblStatus.setVisibility(TextView.VISIBLE);
			} // ==> run
		});
    } // ==> tellStatus
    
	// ------------------------------------------------------------------------
    
	/**
	 * Updates status text, sets it visible and hides it after X seconds.
	 * @param status	The status text.
	 * @param error		Decides if status is an error or not.
	 * @param hideDelay	The time to wait until the status is hidden again (ms).
	 */
	
    private void showStatus(final String status, 
    						final boolean isError, 
    						final int hideDelay)
    {
    	runOnUiThread(new Runnable() 
    	{
			@Override
			public void run() 
			{
				showStatus(status, isError);
				
				Handler handler = new Handler();
		    	handler.postDelayed(new Runnable() 
		    	{
					@Override
					public void run() 
					{
						hideStatus();
					} // ==> run
				}, hideDelay);
			} // ==> run
		});
    } // ==> showStatus
	
    // ------------------------------------------------------------------------
    
    /**
     * Hides the status again after it was set using tellStatus
     * @see tellStatus
     */
    
    private void hideStatus()
    {
    	runOnUiThread(new Runnable() 
    	{
			@Override
			public void run() 
			{
				final TextView lblStatus 
						= (TextView)findViewById(R.id.lblStatus);
				
				// Hide view when animation is finished
		    	fadeOut.setAnimationListener(new AnimationListener() 
		    	{
					@Override
					public void onAnimationEnd(Animation anim) 
					{
						lblStatus.setVisibility(TextView.INVISIBLE);
					} // ==> onAnimationEnd

					@Override
					public void onAnimationRepeat(Animation anim) {}
					@Override
					public void onAnimationStart(Animation anim) {}
				});
		    	
		    	lblStatus.startAnimation(fadeOut);
			} // ==> run
		});
    } // ==> hideStatus
    
    // ------------------------------------------------------------------------
    
    /**
     * Displays a tiny about dialog.
     */
    
    private void showAbout()
    {
    	// Create text view with HTML support and clickable links
    	TextView view = new TextView(this);
    	view.setText(Html.fromHtml(getString(R.string.about_text)));
    	view.setMovementMethod(LinkMovementMethod.getInstance());
    	
    	// Display in alert
    	AlertDialog dialog = new AlertDialog.Builder(this).create();
    	dialog.setTitle(R.string.about);
    	dialog.setView(view);
    	dialog.setButton(AlertDialog.BUTTON_NEUTRAL, 
    					 getString(R.string.ok), 
    					 new OnClickListener() 
    	{
			@Override
			public void onClick(DialogInterface arg0, int arg1) {}
		});
    	dialog.show();
    } // ==> showAbout
    
    // ------------------------------------------------------------------------
    
} // ==> ChatActivity

// ============================================================================
