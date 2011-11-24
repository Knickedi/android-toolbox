package de.viktorreiser.androidtoolbox.showcase.widget;

import de.viktorreiser.androidtoolbox.showcase.R;
import de.viktorreiser.androidtoolbox.showcase.WidgetShowcaseActivity;
import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Demonstration of all kind of custom preference implementations.
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class CustomPreferenceActivity extends PreferenceActivity {
	
	// OVERRIDDEN =================================================================================
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		
		setTitle(WidgetShowcaseActivity.getShowcaseTitle(getClass()));
		
		addPreferencesFromResource(R.xml.custom_preference_settings);
	}
}
