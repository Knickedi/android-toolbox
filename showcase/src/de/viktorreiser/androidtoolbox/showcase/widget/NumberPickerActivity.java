package de.viktorreiser.androidtoolbox.showcase.widget;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import de.viktorreiser.androidtoolbox.showcase.R;
import de.viktorreiser.androidtoolbox.showcase.WidgetShowcaseActivity;
import de.viktorreiser.toolbox.widget.NumberPicker;
import de.viktorreiser.toolbox.widget.NumberPicker.Formatter;
import de.viktorreiser.toolbox.widget.NumberPicker.OnChangedListener;

/**
 * Demonstration of {@link NumberPicker}.
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class NumberPickerActivity extends Activity {
	
	// OVERRIDDEN =================================================================================
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		
		setTitle(WidgetShowcaseActivity.getShowcaseTitle(getClass()));
		
		setContentView(R.layout.number_picker_showcase);
		
		((NumberPicker) findViewById(R.id.square_number_picker)).setFormatter(new Formatter() {
			@Override
			public String toString(int value) {
				int result = value * value;
				
				if (result < 10) {
					return "00" + result;
				} else if (result < 100) {
					return "0" + result;
				} else {
					return "" + result;
				}
			}
		});
		
		((NumberPicker) findViewById(R.id.day_number_picker))
				.setOnChangeListener(new OnChangedListener() {
					@Override
					public void onChanged(NumberPicker picker, int oldVal, int newVal) {
						((TextView) findViewById(R.id.day_number_picker_value)).setText(
								"Current day value: " + newVal);
					}
				});
		
		((TextView) findViewById(R.id.day_number_picker_value)).setText("Current day value: "
				+ ((NumberPicker) findViewById(R.id.day_number_picker)).getCurrent());
	}
}
