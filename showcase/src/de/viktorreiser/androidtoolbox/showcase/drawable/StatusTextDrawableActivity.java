package de.viktorreiser.androidtoolbox.showcase.drawable;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import de.viktorreiser.androidtoolbox.showcase.DrawableShowcaseActivity;
import de.viktorreiser.toolbox.graphics.drawable.StatusTextDrawable;
import de.viktorreiser.toolbox.util.AndroidUtils;

/**
 * Demonstration of {@link StatusTextDrawable}.
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class StatusTextDrawableActivity extends Activity {
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setTitle(DrawableShowcaseActivity.getActivityTitle(this.getClass()));
		
		LinearLayout mainLayout = getLinearRowLayout();
		mainLayout.setOrientation(LinearLayout.VERTICAL);
		mainLayout.setBackgroundColor(0xff000088);
		
		LinearLayout row;
		
		row = getLinearRowLayout();
		row.addView(getStatusTextView("1", true, 0xffffffff, 0x88ff0000, 0x88000000, 0.1f, -1, 0.3f));
		row.addView(getStatusTextView("41", false, 0xffffffff, 0x88ff0000, 0x88000000, 0.1f, -1, 0.3f));
		mainLayout.addView(row);
		
		row = getLinearRowLayout();
		row.addView(getStatusTextView("OK", true, 0x88ff0000, 0x880ff000, 0x550ff000, 0.2f, 0.2f, 0.2f));
		row.addView(getStatusTextView("WRONG", false, 0x88ff0000, 0x880ff000, 0x550ff000, 0.2f, 0.2f, 0.2f));
		mainLayout.addView(row);
		
		row = getLinearRowLayout();
		row.addView(getStatusTextView("1A", true, 0xffffffff, 0xffff0000, 0xff330000, 0.3f, 0, 0.1f));
		row.addView(getStatusTextView("2*", false, 0xffffffff, 0xffff0000, 0xff330000, 0.3f, 0, 0.1f));
		mainLayout.addView(row);
		
		row = getLinearRowLayout();
		row.addView(getStatusTextView(null, true, 0xffffffff, 0xffff0000, 0x88000000, 0, 0.5f, 0));
		row.addView(getStatusTextView("?", false, 0xffffffff, 0xffff0000, 0x88000000, 0, 1, 0.1f));
		mainLayout.addView(row);
		
		setContentView(mainLayout);
	}
	
	private View getStatusTextView(String text, boolean square, int textColor, int strokeColor,
			int fillColor, float strokeWidth, float cornerRadius, float textPadding) {
		StatusTextDrawable drawable = new StatusTextDrawable();
		drawable.setText(text);
		drawable.setSquare(square);
		drawable.setTextColor(textColor);
		drawable.setStrokeColor(strokeColor);
		drawable.setFillColor(fillColor);
		drawable.setStrokeWidth(strokeWidth);
		drawable.setCornerRadius(cornerRadius);
		drawable.setTextPadding(textPadding);
		
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
		row.setLayoutParams(params);
		
		return row;
	}
}
