package de.viktorreiser.toolbox.app;

import de.viktorreiser.toolbox.util.AndroidUtils;
import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.text.ClipboardManager;
import android.widget.Toast;

/**
 * Save text on clipboard and let it clear itself after a while (<b>Beta</b>).<br>
 * <br>
 * <i>Made for Android API &lt; 11.</i><br>
 * <br>
 * <i>Note</i>: You have to add this class as service in you Manifest file.
 * 
 * <pre>
 * {@code <service 
 * 	android:label="Clipboard clear timeout"
 * 	android:name="de.viktorreiser.toolbox.app.SafeClipboard" />
 * }
 * </pre>
 * 
 * Never create a instance of this class, it's not meant to be used that way. Use
 * {@link #setText(Context, String, long)} only.<br>
 * Service will be created by this class and it will exist only as long as it is waiting to clear
 * the clipboard. After that it will kill itself.<br>
 * <br>
 * {@code setClearToast} methods can be used to set information for a toast which will be displayed
 * when clipboard is cleared. A good place to call this method is in a custom
 * {@link Application#onCreate()}.<br>
 * 
 * <br>
 * <i>Depends on</i>: {@link AndroidUtils}
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class SafeClipboard extends Service {
	
	// PRIVATE ====================================================================================
	
	/** Extra for service which will contain text (as string) of clipboard. */
	private static final String EXTRA_TEXT = "text";
	
	/** Extra for service which will contain timeout (as integer) for text timeout. */
	private static final String EXTRA_TIMEOUT = "timeout";
	
	
	/** Image resource ID for toast image ({@link AndroidUtils#makeImageToast(Context, int)}), */
	private static int mToastImage = 0;
	
	/** Text for toast message. */
	private static String mToastMessage = "";
	
	/** Text resource ID for toast message. */
	private static int mToastMessageRes = 0;
	
	
	/** Text which was set to clipboard. */
	private String mCurrentText = null;
	
	/** Handler which will cause delayed clipboard clear. */
	private Handler mHandler = new Handler();
	
	/** Runnable which perform clipboard clear action. */
	private Runnable mRunable = new Runnable() {
		@Override
		public void run() {
			ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			
			// clear clipboard if current text equals text which was set
			if (cm.getText() != null && cm.getText().equals(mCurrentText)) {
				cm.setText(null);
				
				String text = null;
				
				// show a toast when message is available
				if (mToastMessageRes != 0) {
					text = getString(mToastMessageRes);
				} else if (mToastMessage != null) {
					text = mToastMessage;
				} else {
					return;
				}
				
				// show toast with image if drawable id is set
				// otherwise show an ordinary toast
				if (mToastImage == 0) {
					Toast.makeText(SafeClipboard.this, text, Toast.LENGTH_SHORT).show();
				} else {
					AndroidUtils.makeImageToast(
							SafeClipboard.this, mToastImage, text, Toast.LENGTH_SHORT).show();
				}
			}
			
			// kill service - job done
			stopSelf();
		}
	};
	
	// PUBLIC =====================================================================================
	
	/**
	 * Save text on clipboard and let it clear itself after given amount of milliseconds.<br>
	 * <br>
	 * Clipboard will only be cleared if it contains the text which was set with this method before,
	 * so when another text is clipped (e.g. by another application) it won't be cleared. A new call
	 * will stop old clearance timeout and start a new one.
	 * 
	 * @param context
	 * @param text
	 *            text to save on clipboard ({@code null} will clear clipboard)
	 * @param timeout
	 *            amount of milliseconds text will be available on clipboard ({@code < 1000} equals
	 *            forever)
	 */
	public static void setText(Context context, String text, long timeout) {
		ClipboardManager cm = (ClipboardManager) context.getSystemService(
				Context.CLIPBOARD_SERVICE);
		
		cm.setText(text);
		
		Intent service = new Intent(context, SafeClipboard.class);
		
		if (text == null || timeout < 1000) {
			// kill running service (if its running) because it's not needed anymore
			context.stopService(service);
		} else {
			// start (or update) service which will clear
			service.putExtra(EXTRA_TEXT, text);
			service.putExtra(EXTRA_TIMEOUT, timeout);
			context.startService(service);
		}
	}
	
	/**
	 * Create a toast when clipboard is cleared.
	 * 
	 * @param textRes
	 *            string resource for toast message ({@code 0} will disable toast)
	 */
	public static void setClearToast(int textRes) {
		mToastMessageRes = textRes;
	}
	
	/**
	 * Create a toast when clipboard is cleared.
	 * 
	 * @param text
	 *            string for toast message ({@code null} will disable toast)
	 */
	public static void setClearToast(String text) {
		mToastMessage = text;
	}
	
	/**
	 * Create a image toast when clipboard is cleared.
	 * 
	 * @param textRes
	 *            string resource for toast message ({@code 0} will disable toast)
	 * @param imageRes
	 *            drawable resource for toast message
	 * 
	 * @see AndroidUtils#makeImageToast(Context, int, int, int)
	 */
	public static void setClearToast(int textRes, int imageRes) {
		mToastMessageRes = textRes;
		mToastImage = imageRes;
	}
	
	/**
	 * Create a image toast when clipboard is cleared.
	 * 
	 * @param text
	 *            string for toast message ({@code null} will disable toast)
	 * @param imageRes
	 *            drawable resource for toast message
	 * 
	 * @see AndroidUtils#makeImageToast(Context, int, CharSequence, int)
	 */
	public static void setClearToast(String text, int imageRes) {
		mToastMessage = text;
		mToastImage = imageRes;
	}
	
	// OVERRIDDEN =================================================================================
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		
		if (intent == null) {
			return;
		}
		
		mCurrentText = intent.getStringExtra(EXTRA_TEXT);
		
		mHandler.removeCallbacks(mRunable);
		mHandler.postDelayed(mRunable, intent.getLongExtra(EXTRA_TIMEOUT, 1000));
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		mHandler.removeCallbacks(mRunable);
	}
}
