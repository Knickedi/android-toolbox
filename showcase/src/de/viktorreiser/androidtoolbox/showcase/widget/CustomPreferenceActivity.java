package de.viktorreiser.androidtoolbox.showcase.widget;

import de.viktorreiser.androidtoolbox.showcase.WidgetShowcaseActivity;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class CustomPreferenceActivity extends PreferenceActivity {
	
	// OVERRIDDEN =================================================================================
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		
		setTitle(WidgetShowcaseActivity.getShowcaseTitle(getClass()));
	}
}
