package de.viktorreiser.androidtoolbox.showcase.swipeable;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
import de.viktorreiser.androidtoolbox.showcase.R;
import de.viktorreiser.androidtoolbox.showcase.SwipeableShowcaseActivity;
import de.viktorreiser.toolbox.widget.HiddenQuickActionSetup;
import de.viktorreiser.toolbox.widget.SwipeableHiddenView;

public class SwipeableListDetachedActivity extends Activity {
	
	// OVERRIDDEN =================================================================================
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		
		setTitle(SwipeableShowcaseActivity.getActivityTitle(getClass()));
		setContentView(R.layout.swipeable_detached);
		
		HiddenQuickActionSetup s1 = new HiddenQuickActionSetup(this);
		s1.setDetachFromList(true);
		s1.setSwipeOnLongClick(true);
		s1.setBackgroundResource(R.drawable.quickaction_background);
		s1.addAction(1, 0, R.drawable.quickaction_url);
		s1.addAction(2, 0, R.drawable.quickaction_urlopen);
		((SwipeableHiddenView) findViewById(R.id.quick_action_view)).setHiddenViewSetup(s1);
		findViewById(R.id.quick_action_view).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Toast.makeText(SwipeableListDetachedActivity.this,
						"Clicked!", Toast.LENGTH_SHORT).show();
			}
		});
	}
}
