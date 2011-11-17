package de.viktorreiser.androidtoolbox.showcase;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.ScrollView;
import android.widget.TextView;
import de.viktorreiser.toolbox.util.AndroidUtils;

/**
 * Display HTML formated information.<br>
 * <br>
 * Just use {@link #showInfo(Context, String)}.
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class InformationActivity extends Activity {
	
	// PUBLIC =====================================================================================
	
	/**
	 * Display information in a new activity.
	 * 
	 * @param context
	 * @param textAsHtml
	 *            string which can contain HTML - {@link Html#fromHtml(String)} will be used to
	 *            decode it
	 */
	public static final void showInfo(Context context, String textAsHtml) {
		Intent intent = new Intent(context, InformationActivity.class);
		intent.putExtra("info", textAsHtml);
		context.startActivity(intent);
	}
	
	// OVERRIDDEN =================================================================================
	
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		
		setTitle("Information");
		
		TextView tv = new TextView(this);
		tv.setTextColor(0xffffffff);
		tv.setLinkTextColor(0xffa3b8fe);
		int p = AndroidUtils.dipToPixel(this, 10);
		tv.setPadding(p, p, p, p);
		tv.setText(Html.fromHtml(getIntent().getStringExtra("info")));
		tv.setMovementMethod(LinkMovementMethod.getInstance());
		
		ScrollView sv = new ScrollView(this);
		sv.addView(tv);
		
		setContentView(sv);
	}
}
