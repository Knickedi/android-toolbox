package de.viktorreiser.androidtoolbox.showcase;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;
import de.viktorreiser.androidtoolbox.showcase.widget.CustomPreferenceActivity;
import de.viktorreiser.androidtoolbox.showcase.widget.NumberPickerActivity;

/**
 * List of custom widget demonstrations.
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class WidgetShowcaseActivity extends Activity {
	
	// PRIVATE ====================================================================================
	
	private static final Object [][] mShowcases = new Object [] [] {
			new Object [] {
					NumberPickerActivity.class,
					"Number picker",
					"This demonstrates how to use and the features of the number picker port from android"
			
			},
			new Object [] {
					CustomPreferenceActivity.class,
					"Custom preferences",
					"Demonstration of custom preference implementations"
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
}
