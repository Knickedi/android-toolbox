package de.viktorreiser.androidtoolbox.showcase.widget;

import android.app.Activity;
import android.os.Bundle;
import de.viktorreiser.androidtoolbox.showcase.WidgetShowcaseActivity;

public class NumberPickerActivity extends Activity {
	
	// OVERRIDDEN =================================================================================
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		
		setTitle(WidgetShowcaseActivity.getShowcaseTitle(getClass()));
	}
}
