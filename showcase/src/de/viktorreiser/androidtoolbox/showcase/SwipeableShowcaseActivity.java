package de.viktorreiser.androidtoolbox.showcase;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.widget.ListView;
import de.viktorreiser.androidtoolbox.showcase.swipeable.SwipeableListDetachedActivity;
import de.viktorreiser.androidtoolbox.showcase.swipeable.SwipeableListQuickActionActivity;
import de.viktorreiser.androidtoolbox.showcase.swipeable.SwipeableListQuickActionTypeActivity;

/**
 * List of swipeable (list) demonstrations.
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class SwipeableShowcaseActivity extends Activity {
	
	// PRIVATE ====================================================================================
	
	private static final Object [][] mShowcases = new Object [] [] {
			new Object [] {
					SwipeableListQuickActionActivity.class,
					"Quick action",
					"Ordinary quick action implementation for list items of the same kind"
			
			},
			new Object [] {
					SwipeableListQuickActionTypeActivity.class,
					"Quick action (view type)",
					"Quick action implementation which uses list view type to use different setups for list items"
			},
			new Object [] {
					SwipeableListDetachedActivity.class,
					"Swipeable detached",
					"This shows how you can use a swipeable view without a swipeable list"
			}
	};
	
	// PUBLIC =====================================================================================
	
	public static final String getShowcaseTitle(Class<?> activityClass) {
		return Showcase.getTitle(activityClass, mShowcases);
	}
	
	// OVERRIDDEN =================================================================================
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		
		setTitle(AndroidToolboxShowcaseActivity.getShowcaseTitle(getClass()));
		
		Showcase.Adapter adapter = new Showcase.Adapter(this, mShowcases);

		
		ListView list = new ListView(this);
		list.setOnItemClickListener(adapter);
		list.setAdapter(adapter);
		
		setContentView(list);
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
