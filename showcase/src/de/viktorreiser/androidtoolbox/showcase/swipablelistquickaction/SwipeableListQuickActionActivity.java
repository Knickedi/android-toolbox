package de.viktorreiser.androidtoolbox.showcase.swipablelistquickaction;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;
import de.viktorreiser.androidtoolbox.showcase.AndroidToolboxShowcaseActivity;
import de.viktorreiser.androidtoolbox.showcase.R;
import de.viktorreiser.toolbox.util.AndroidUtils;
import de.viktorreiser.toolbox.widget.HiddenQuickActionSetup;
import de.viktorreiser.toolbox.widget.HiddenQuickActionSetup.OnQuickActionListener;
import de.viktorreiser.toolbox.widget.SwipeableHiddenView;
import de.viktorreiser.toolbox.widget.SwipeableListView;

public class SwipeableListQuickActionActivity extends Activity implements OnQuickActionListener {
	
	public static final String [][] ITEMS = new String [] [] {
			new String [] {"Google", "http://www.google.com/"},
			new String [] {"Ebay", "http://www.ebay.com/"},
			new String [] {"StackOverflow", "http://stackoverflow.com/"},
	};
	
	public static final class QuickAction {
		public static final int OPEN = 1;
		public static final int COPY = 2;
	}
	
	private HiddenQuickActionSetup mQuickActionSetup;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(AndroidToolboxShowcaseActivity.getActivityTitle(this.getClass()));
		
		setupQuickAction();
		
		SwipeableListView listView = new SwipeableListView(this);
		listView.setAdapter(new MyAdapter());
		setContentView(listView);
	}
	
	@Override
	public void onQuickAction(AdapterView<?> parent, View view, int position, int quickActionId) {
		switch (quickActionId) {
		case QuickAction.OPEN:
			Intent i = new Intent(Intent.ACTION_VIEW);
			i.setData(Uri.parse(ITEMS[position][1]));
			startActivity(i);
			break;
		
		case QuickAction.COPY:
			ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			cm.setText(ITEMS[position][1]);
			Toast.makeText(this, "URL copied to clipboard", Toast.LENGTH_SHORT).show();
			break;
		}
	}
	
	private void setupQuickAction() {
		mQuickActionSetup = new HiddenQuickActionSetup(this);
		mQuickActionSetup.setOnQuickActionListener(this);
		
		// a nice cubic ease animation
		mQuickActionSetup.setOpenAnimation(new Interpolator() {
			@Override
			public float getInterpolation(float v) {
				v -= 1;
				return v * v * v + 1;
			}
		});
		mQuickActionSetup.setCloseAnimation(new Interpolator() {
			@Override
			public float getInterpolation(float v) {
				return v * v * v;
			}
		});
		
		int imageSize = AndroidUtils.dipToPixel(this, 40);
		
		mQuickActionSetup.setBackgroundResource(R.drawable.quickaction_background);
		mQuickActionSetup.setImageSize(imageSize, imageSize);
		mQuickActionSetup.setAnimationSpeed(700);
		mQuickActionSetup.setStartOffset(AndroidUtils.dipToPixel(this, 30));
		mQuickActionSetup.setStopOffset(AndroidUtils.dipToPixel(this, 50));
		mQuickActionSetup.setSwipeOnLongClick(true);
		
		mQuickActionSetup.addAction(QuickAction.OPEN,
				"Open URL", R.drawable.quickaction_urlopen);
		mQuickActionSetup.addAction(QuickAction.COPY,
				"Copy URL to clipboard", R.drawable.quickaction_url);
	}
	
	
	private class MyAdapter extends BaseAdapter {
		
		@Override
		public int getCount() {
			return ITEMS.length;
		}
		
		@Override
		public Object getItem(int position) {
			return position;
		}
		
		@Override
		public long getItemId(int position) {
			return position;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			
			if (convertView == null) {
				convertView = (SwipeableHiddenView) getLayoutInflater().inflate(
						R.layout.swipeable_quick_action_simple_two_lines, null);
				((SwipeableHiddenView) convertView).setHiddenViewSetup(mQuickActionSetup);
				
				holder = new ViewHolder();
				holder.title = (TextView) convertView.findViewById(R.id.title);
				holder.link = (TextView) convertView.findViewById(R.id.link);
				
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			holder.title.setText(ITEMS[position][0]);
			holder.link.setText(ITEMS[position][1]);
			
			return convertView;
		}
		
		private class ViewHolder {
			public TextView title;
			public TextView link;
		}
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
				.setMessage("You will see that the indicator (if enabled) will cover the the "
						+ "list item if it is on top. If you have a better idea please check the "
						+ " project page and suggest your solution for this issue.")
				.setPositiveButton("OK", null)
				.create().show();
		
		return true;
	}
}
