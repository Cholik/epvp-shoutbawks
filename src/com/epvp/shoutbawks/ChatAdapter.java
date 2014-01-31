package com.epvp.shoutbawks;

import java.util.ArrayList;

import android.content.Context;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

// ============================================================================

/**
 * Adapter used for chat messages in the chat ListView.
 * @author Ende!
 */
public class ChatAdapter extends ArrayAdapter<ChatAdapter.Row> 
{

	// ------------------------------------------------------------------------
	// Subclasses
	
	public static class Row 
	{

		// ------------------------------------------------
		// Private attributes
		
		private String	message;
		private String 	author;
		private String	timestamp;
		private int		id;
		
		// ------------------------------------------------
		// Public constructor 
		
		/**
		 * Default constructor.
		 * @param author	The author of the message (HTML permitted).
		 * @param message	The message (HTML permitted).
		 * @param timestamp	The time stamp.
		 */
		
		public Row(String author, String message, String timestamp, int id)
		{
			this.author 	= author;
			this.message 	= message;
			this.timestamp 	= timestamp;
			this.id			= id;
		} // ==> ctor

		// ------------------------------------------------
		// Public getters
		
		public String 	getMessage() 	{ return message; 	}
		public String 	getAuthor() 	{ return author;	}
		public String 	getTimestamp()	{ return timestamp;	}
		public int		getId()			{ return id;		}
		
		// ------------------------------------------------
		
	} // ==> Row
	
	// ------------------------------------------------------------------------
	// Private attributes

	private ArrayList<Row>	data;
	private Context			ctx;
	
	// ------------------------------------------------------------------------
	// Public constructor and methods 
	
	/**
	 * Default constructor.
	 * @param context			The context.
	 * @param data				The data set the adapter is bound to.
	 */
	
	public ChatAdapter(Context context, ArrayList<Row> data)
	{
		super(context, R.layout.chat_row, data);
		this.data 	= data;
		this.ctx 	= context;
	} // ==> ctor
	
	// ------------------------------------------------------------------------
	// Overridden methods of base class
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) 
	{
		// Recycle view, if possible. If not, allocate new one.
		View row = convertView;
		if (row == null)
		{
			row = ((LayoutInflater)ctx.getSystemService(
					Context.LAYOUT_INFLATER_SERVICE)
					).inflate(R.layout.chat_row, null);
		}
		
		Row rowData = data.get(position);
		if (rowData == null)
			return row;
		
		// Fill view with data
		TextView authorCell = (TextView)row.findViewById(R.id.lblAuthorCell);
		TextView msgCell 	= (TextView)row.findViewById(R.id.lblMessageCell);
		TextView timeCell	= (TextView)row.findViewById(R.id.lblTimestamp);
		
		authorCell.setText(Html.fromHtml(rowData.getAuthor()));
		timeCell.setText(rowData.getTimestamp());
		msgCell.setText(Html.fromHtml(rowData.getMessage()));
		msgCell.setMovementMethod(LinkMovementMethod.getInstance());
		
		// Timestamp related stuff
		if (SettingsActivity.getSettings(ctx).displayTimestamp())
		{
			timeCell.setText(rowData.getTimestamp());
			timeCell.setVisibility(TextView.VISIBLE);
		}
		else
			timeCell.setVisibility(TextView.INVISIBLE);
		
		return row;
	} // ==> getView
	
	// ------------------------------------------------------------------------
	
} // ==> ChatAdapter

// ============================================================================
