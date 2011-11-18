package de.viktorreiser.androidtoolbox.showcase;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import de.viktorreiser.androidtoolbox.showcase.AndroidToolboxShowcaseActivity.ShowcaseAdapter;
import de.viktorreiser.androidtoolbox.showcase.swipeable.SwipeableListDetachedActivity;
import de.viktorreiser.androidtoolbox.showcase.swipeable.SwipeableListQuickActionActivity;
import de.viktorreiser.androidtoolbox.showcase.swipeable.SwipeableListQuickActionTypeActivity;

/**
 * List of swipeable (list) demonstrations.
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class SwipeableShowcaseActivity extends Activity implements OnItemClickListener {
	
	// PRIVATE ====================================================================================
	
	private static final Class<?> [] mShowcaseActivities = new Class [] {
			SwipeableListQuickActionActivity.class,
			SwipeableListQuickActionTypeActivity.class,
			SwipeableListDetachedActivity.class
	};
	
	private static final String [] mShowcaseNames = new String [] {
			"Quick action",
			"Quick action (view type)",
			"Swipeable detached"
	};
	
	private static final String [] mShowcaseDescriptions = new String [] {
			"Ordinary quick action implementation for list items of the same kind",
			"Quick action implementation which uses list view type to use different setups for list items",
			"This shows how you can use a swipeable view without a swipeable list"
	};
	
	// PUBLIC =====================================================================================
	
	/**
	 * Get activity title for a certain activity.
	 * 
	 * @param activityClass
	 *            class of activity to get the title for
	 * 
	 * @return {@code "title"} or {@code null} if given class is not a swipeable activity
	 */
	public static final String getActivityTitle(Class<?> activityClass) {
		for (int i = 0; i < mShowcaseActivities.length; i++) {
			if (activityClass.equals(mShowcaseActivities[i])) {
				return mShowcaseNames[i];
			}
		}
		
		return null;
	}
	
	// OVERRIDDEN =================================================================================
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		
		setTitle(AndroidToolboxShowcaseActivity.getActivityTitle(getClass()));
		
		ListView list = new ListView(this);
		list.setOnItemClickListener(this);
		list.setAdapter(new ShowcaseAdapter(this, mShowcaseNames, mShowcaseDescriptions));
		
		setContentView(list);
	}
	
	@Override
	public void onItemClick(AdapterView<?> a, View v, int p, long id) {
		startActivity(new Intent(this, mShowcaseActivities[p]));
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		String info = "<h1>Swipeable (list)</h1>"
				+ "The swipeable item is not an implementation itself, it's rather an API which "
				+ "enables you (the developer) to create a view which reacts on a left or right "
				+ "swipe (within a list).<br>"
				+ "<br>"
				+ "You will find demonstrations of full working view implementations here. E.g. "
				+ "there's a powerful implementation which enables you to hide any kind of view "
				+ "behind another one. This one is used to implement the quick action view.<br>"
				+ "<br>"
				+ "For now those are the only implementations. Maybe there will be more soon. "
				+ "You can achieve any kind of view. E.g. you could create a view which "
				+ "represents a contact and a swipe to the left will call him and right will send "
				+ "a SMS...";
		InformationActivity.showInfo(this, info);
		
		return false;
	}
}
