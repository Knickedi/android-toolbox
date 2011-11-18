package de.viktorreiser.androidtoolbox.showcase.swipeable;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import de.viktorreiser.androidtoolbox.showcase.InformationActivity;
import de.viktorreiser.androidtoolbox.showcase.R;
import de.viktorreiser.androidtoolbox.showcase.SwipeableShowcaseActivity;
import de.viktorreiser.toolbox.util.AndroidUtils;
import de.viktorreiser.toolbox.widget.HiddenQuickActionSetup;
import de.viktorreiser.toolbox.widget.SwipeableHiddenView;
import de.viktorreiser.toolbox.widget.SwipeableHiddenView.HiddenViewSetup.SwipeDirection;
import de.viktorreiser.toolbox.widget.SwipeableListView;

/**
 * Demonstration of different quick actions for list items by using list view types.
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class SwipeableListQuickActionTypeActivity extends Activity {
	
	// OVERRIDDEN =================================================================================
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setTitle(SwipeableShowcaseActivity.getActivityTitle(getClass()));
		
		SwipeableListView listView = new SwipeableListView(this);
		listView.setAdapter(new MyAdapter());
		setContentView(listView);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		String info = "<h1>Different quick actions for list view types</h1>"
				+ "This activity provides a (stupid) example which shows how to use different "
				+ "quick action setups for different list view types. It alternates between two "
				+ "different setups.<br>"
				+ "<br>"
				+ "The first one can be swipe to the right side and the second to the left side "
				+ "only. The quick action have no function, just dummies.<br>"
				+ "<br>"
				+ "This demonstration shows that you don't have to use a single setup for all "
				+ "and can implement all kind of fancy behaviors.";
		
		InformationActivity.showInfo(this, info);
		
		return false;
	}
	
	// PRIVATE ====================================================================================
	
	private static final class QuickAction {
		public static final int OPEN = 1;
		public static final int COPY = 2;
	}
	
	private class MyAdapter extends BaseAdapter {
		
		private HiddenQuickActionSetup mSetup1;
		private HiddenQuickActionSetup mSetup2;
		
		public MyAdapter() {
			Context ctx = SwipeableListQuickActionTypeActivity.this;
			int imageSize = AndroidUtils.dipToPixel(ctx, 40);
			
			mSetup1 = new HiddenQuickActionSetup(ctx);
			mSetup1.setBackgroundResource(R.drawable.quickaction_background);
			mSetup1.setImageSize(imageSize, imageSize);
			mSetup1.setAnimationSpeed(700);
			mSetup1.setStartOffset(AndroidUtils.dipToPixel(ctx, 30));
			mSetup1.setStopOffset(AndroidUtils.dipToPixel(ctx, 50));
			mSetup1.setSwipeOnLongClick(true);
			mSetup1.setSwipeDirection(SwipeDirection.RIGHT);
			mSetup1.addAction(QuickAction.OPEN,
					"Open URL", R.drawable.quickaction_urlopen);
			mSetup1.addAction(QuickAction.COPY,
					"Copy URL to clipboard", R.drawable.quickaction_url);
			
			mSetup2 = new HiddenQuickActionSetup(ctx);
			mSetup2.setBackgroundColor(0xff5555aa);
			mSetup2.setImageSize(imageSize, imageSize);
			mSetup2.setAnimationSpeed(700);
			mSetup2.setStartOffset(AndroidUtils.dipToPixel(ctx, 30));
			mSetup2.setStopOffset(AndroidUtils.dipToPixel(ctx, 50));
			mSetup2.setSwipeOnLongClick(true);
			mSetup2.setSwipeDirection(SwipeDirection.LEFT);
			mSetup2.addAction(QuickAction.OPEN,
					"Open URL", R.drawable.quickaction_urlopen);
		}
		
		
		@Override
		public int getCount() {
			return 20;
		}
		
		@Override
		public Object getItem(int position) {
			return null;
		}
		
		@Override
		public int getItemViewType(int position) {
			return position % 2;
		}
		
		@Override
		public int getViewTypeCount() {
			return 2;
		}
		
		@Override
		public long getItemId(int position) {
			return 0;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			int type = getItemViewType(position);
			
			if (convertView == null) {
				TextView t = new TextView(parent.getContext(), null,
						android.R.attr.textAppearanceLarge);
				int p = AndroidUtils.dipToPixel(parent.getContext(), 15);
				t.setPadding(p, p, p, p);
				
				SwipeableHiddenView h = new SwipeableHiddenView(parent.getContext());
				h.setHiddenViewSetup(type == 0 ? mSetup1 : mSetup2);
				h.addView(t);
				
				convertView = h;
				convertView.setTag(t);
			}
			
			((TextView) convertView.getTag()).setText("Item " + (position + 1));
			
			return convertView;
		}
	}
}
