package de.viktorreiser.androidtoolbox.showcase;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import de.viktorreiser.androidtoolbox.showcase.AndroidToolboxShowcaseActivity.ShowcaseAdapter;
import de.viktorreiser.androidtoolbox.showcase.drawable.StatusTextDrawableActivity;

/**
 * List of custom drawable demonstrations.
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class DrawableShowcaseActivity extends Activity implements OnItemClickListener {
	
	// PRIVATE ====================================================================================
	
	private static final Class<?> [] mShowcaseActivities = new Class [] {
			StatusTextDrawableActivity.class
	};
	
	private static final String [] mShowcaseNames = new String [] {
			"Status text"
	};
	
	private static final String [] mShowcaseDescriptions = new String [] {
			"Scalable drawable which can be used for (e.g.) drawing and indicating a certain state"
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
		
		ListView lv = new ListView(this);
		lv.setOnItemClickListener(this);
		lv.setAdapter(new ShowcaseAdapter(this, mShowcaseNames, mShowcaseDescriptions));
		
		setContentView(lv);
	}
	
	@Override
	public void onItemClick(AdapterView<?> a, View v, int p, long id) {
		startActivity(new Intent(this, mShowcaseActivities[p]));
	}
}
