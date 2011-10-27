package de.viktorreiser.androidtoolbox.showcase;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import de.viktorreiser.androidtoolbox.showcase.statustextdrawable.StatusTextDrawableActivity;
import de.viktorreiser.androidtoolbox.showcase.swipablelistquickaction.SwipeableListQuickActionTypeActivity;
import de.viktorreiser.androidtoolbox.showcase.swipablelistquickaction.SwipeableListQuickActionActivity;

/**
 * Showcase for android toolbox features.
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class AndroidToolboxShowcaseActivity extends Activity implements OnItemClickListener {
	
	private static final String [] mShowcaseNames = new String [] {
			"Swipeable List Quick Action",
			"Swipeable List Quick Action (different types)",
			"Status Text Drawable",
//			"Bubble Drawable"
	};
	
	private static final Class<?> [] mShowcaseActivities = new Class [] {
			SwipeableListQuickActionActivity.class,
			SwipeableListQuickActionTypeActivity.class,
			StatusTextDrawableActivity.class,
//			BubbleDrawableActivity.class
	};
	
	public static final String getActivityTitle(Class<?> activityClass) {
		for (int i = 0; i < mShowcaseActivities.length; i++) {
			if (mShowcaseActivities[i].equals(activityClass)) {
				return mShowcaseNames[i];
			}
		}
		
		return null;
	}
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		ListView list = new ListView(this);
		list.setOnItemClickListener(this);
		list.setAdapter(new ArrayAdapter<String>(
				this, android.R.layout.simple_list_item_1, mShowcaseNames));
		
		setContentView(list);
		
		SharedPreferences pref = getSharedPreferences("global", 0);
		
		if (pref.getBoolean("first_run", true)) {
			new AlertDialog.Builder(this)
					.setTitle("Info")
					.setMessage("Please check the activity menus!"
							+ " They could provide further information!")
					.setPositiveButton("OK", null)
					.create().show();
			
			Editor editor = pref.edit();
			editor.putBoolean("first_run", false);
			editor.commit();
		}
	}
	
	@Override
	public void onItemClick(AdapterView<?> a, View v, int p, long id) {
		startActivity(new Intent(this, mShowcaseActivities[p]));
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add("Info");
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		new AlertDialog.Builder(this)
				.setTitle("Info")
				.setMessage("If you find a bug please visit the project"
						+ " page and commit an issue on that!")
				.setPositiveButton("OK", null)
				.create().show();
		
		return true;
	}
}