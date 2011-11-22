package de.viktorreiser.androidtoolbox.showcase;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.bugsense.trace.BugSenseHandler;

/**
 * Showcase for android toolbox features.<br>
 * <br>
 * This activity will list the main categories of the demonstrations. So it will start other
 * activities in this package which will list demonstrations for a certain category.
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class AndroidToolboxShowcaseActivity extends Activity implements OnItemClickListener {
	
	// PRIVATE ====================================================================================
	
	private static final Class<?> [] mShowcaseActivities = new Class [] {
			null,
			SwipeableShowcaseActivity.class,
			DrawableShowcaseActivity.class
	};
	
	private static final String [] mShowcaseNames = new String [] {
			"Information",
			"Swipeable (list)",
			"Custom drawables"
	};
	
	private static final String [] mShowcaseDescriptions = new String [] {
			"Some general information about this app",
			"Demonstrates how to use a swipeable (list) item and it's implementations",
			"Demonstrates the features of custom drawable implementations"
	};
	
	// PUBLIC =====================================================================================
	
	/**
	 * Get activity title for a certain activity.
	 * 
	 * @param activityClass
	 *            class of activity to get the title for
	 * 
	 * @return {@code title + " showcase"} or {@code null} if given class is not a category activity
	 */
	public static final String getActivityTitle(Class<?> activityClass) {
		for (int i = 0; i < mShowcaseActivities.length; i++) {
			if (activityClass.equals(mShowcaseActivities[i])) {
				return mShowcaseNames[i] + " showcase";
			}
		}
		
		return null;
	}
	
	
	public static class ShowcaseAdapter extends BaseAdapter {
		
		private Context mmContext;
		private String [] mmNames;
		private String [] mmDescriptions;
		
		public ShowcaseAdapter(Context context, String [] names, String [] descriptions) {
			mmContext = context;
			mmNames = names;
			mmDescriptions = descriptions;
		}
		
		@Override
		public int getCount() {
			return mmNames.length;
		}
		
		@Override
		public Object getItem(int arg0) {
			return null;
		}
		
		@Override
		public long getItemId(int position) {
			return 0;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = ((LayoutInflater) mmContext
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
						.inflate(android.R.layout.simple_list_item_2, null);
			}
			
			((TextView) convertView.findViewById(android.R.id.text1)).setText(
					mmNames[position]);
			((TextView) convertView.findViewById(android.R.id.text2)).setText(
					mmDescriptions[position]);
			
			return convertView;
		}
	}
	
	// OVERRIDDEN =================================================================================
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		BugSenseHandler.setup(this, "00ef9ee6");
		
		ListView list = new ListView(this);
		list.setOnItemClickListener(this);
		list.setAdapter(new ShowcaseAdapter(this, mShowcaseNames, mShowcaseDescriptions));
		
		setContentView(list);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		showInfo();
		
		return false;
	}
	
	@Override
	public void onItemClick(AdapterView<?> a, View v, int p, long id) {
		if (p == 0) {
			showInfo();
		} else {
			startActivity(new Intent(this, mShowcaseActivities[p]));
		}
	}
	
	// PRIVATE ====================================================================================
	
	private void showInfo() {
		String info = "<h1>Android Toolbox</h1>"
				+ "First of all: This application is made for developers. You won't find "
				+ "something useful here as a ordinary user.<br>"
				+ "<br>"
				+ "It's a <a href='https://github.com/Knickedi/android-toolbox'>project hosted "
				+ "on GitHub</a> which provides some useful tools for android developers.<br>"
				+ "<br>"
				+ "<a href='https://github.com/Knickedi/android-toolbox/tree/master/showcase'>"
				+ "This showcase application</a> is demonstrating how to use those tools by "
				+ "providing a working source and it's visual results. Please report bugs if the "
				+ "provided examples don't work right way on your device.<br>"
				+ "<br>"
				+ "The showcase app will be extended when there's something new to present or on "
				+ "request when there's somthing unclear about a certain tool or feature.<br>"
				+ "<br>"
				+ "<b>Press the menu button in the showcase activities for some additional information.</b>";
		InformationActivity.showInfo(this, info);
	}
}