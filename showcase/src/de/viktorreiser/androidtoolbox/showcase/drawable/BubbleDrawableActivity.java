package de.viktorreiser.androidtoolbox.showcase.drawable;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import de.viktorreiser.androidtoolbox.showcase.AndroidToolboxShowcaseActivity;
import de.viktorreiser.toolbox.graphics.drawable.BubbleDrawable;
import de.viktorreiser.toolbox.graphics.drawable.BubbleDrawable.IndicatorDirection;
import de.viktorreiser.toolbox.util.AndroidUtils;

public class BubbleDrawableActivity extends Activity {
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setTitle(AndroidToolboxShowcaseActivity.getActivityTitle(this.getClass()));
		
		LinearLayout mainLayout = getLinearRowLayout();
		mainLayout.setOrientation(LinearLayout.VERTICAL);
		mainLayout.setBackgroundColor(0xff000088);
		
		LinearLayout row;
		
		row = getLinearRowLayout();
//		row.addView(getBubbleView());
//		row.addView(getBubbleView());
		mainLayout.addView(row);
		
		row = getLinearRowLayout();
//		row.addView(getBubbleView());
//		row.addView(getBubbleView());
		mainLayout.addView(row);
		
		setContentView(mainLayout);
	}
	
	private View getBubbleView(int strokeWidth, int corenerWidth, int width, int height, IndicatorDirection direction, float position) {
		BubbleDrawable drawable = new BubbleDrawable();
		
		
		View view = new View(this);
		view.setBackgroundDrawable(drawable);
		
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
		params.weight = 1f;
		params.bottomMargin = params.topMargin = params.leftMargin =
				params.rightMargin = AndroidUtils.dipToPixel(this, 10);
		view.setLayoutParams(params);
		
		return view;
	}
	
	private LinearLayout getLinearRowLayout() {
		LinearLayout row = new LinearLayout(this);
		row.setOrientation(LinearLayout.HORIZONTAL);
		
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
		params.weight = 1f;
		params.bottomMargin = params.topMargin = params.leftMargin =
				params.rightMargin = AndroidUtils.dipToPixel(this, 10);
		row.setLayoutParams(params);
		
		return row;
	}
}
