package de.viktorreiser.androidtoolbox.showcase.swipablelistquickaction;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import de.viktorreiser.androidtoolbox.showcase.AndroidToolboxShowcaseActivity;
import de.viktorreiser.androidtoolbox.showcase.R;
import de.viktorreiser.androidtoolbox.showcase.swipablelistquickaction.SwipeableListQuickActionActivity.QuickAction;
import de.viktorreiser.toolbox.util.AndroidUtils;
import de.viktorreiser.toolbox.widget.HiddenQuickActionSetup;
import de.viktorreiser.toolbox.widget.SwipeableHiddenView;
import de.viktorreiser.toolbox.widget.SwipeableListItem.SwipeableSetup;
import de.viktorreiser.toolbox.widget.SwipeableListView;

public class SwipeableListQuickActionTypeActivity extends Activity {
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setTitle(AndroidToolboxShowcaseActivity.getActivityTitle(getClass()));
		
		SwipeableListView listView = new SwipeableListView(this);
		listView.setAdapter(new MyAdapter());
		setContentView(listView);
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
			mSetup1.setSwipeDirection(SwipeableSetup.DIRECTION_RIGHT);
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
			mSetup2.setSwipeDirection(SwipeableSetup.DIRECTION_LEFT);
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
				TextView t = new TextView(parent.getContext(), null, android.R.attr.textAppearanceLarge);
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
