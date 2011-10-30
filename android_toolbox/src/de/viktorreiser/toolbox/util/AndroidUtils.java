package de.viktorreiser.toolbox.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.os.Environment;
import android.os.StatFs;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Static helper utilities for android related tasks.
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class AndroidUtils {
	
	// PRIVATE ====================================================================================
	
	private static String mSQLiteVersion = null;
	private static Boolean mSQLiteSupportsFTS3 = null;
	private static Boolean mSQLiteSupportsFTS4 = null;
	private static Boolean mSQLiteSupportsForeignKeys = null;
	private static Rect mStatusBarRect = new Rect();
	private static int [] mLocation = new int [2];
	private static Field mFlingEndField = null;
	private static Method mFlingEndMethod = null;
	
	static {
		try {
			mFlingEndField = AbsListView.class.getDeclaredField("mFlingRunnable");
			mFlingEndField.setAccessible(true);
			mFlingEndMethod = mFlingEndField.getType().getDeclaredMethod("endFling");
			mFlingEndMethod.setAccessible(true);
		} catch (Exception e) {
			// implementation changed - can't do anything here
			mFlingEndMethod = null;
		}
	}
	
	// PUBLIC =====================================================================================
	
	/**
	 * Create toast notification with an image in front of text.
	 * 
	 * @param context
	 * @param imageResId
	 *            resource ID for image
	 * 
	 * @return toast with image, empty text and {@link Toast#LENGTH_SHORT}
	 * 
	 * @see #makeImageToast(Context, int, int, int)
	 */
	public static Toast makeImageToast(Context context, int imageResId) {
		return makeImageToast(context, imageResId, "", Toast.LENGTH_SHORT);
	}
	
	/**
	 * Create toast notification with an image in front of text.
	 * 
	 * @param context
	 * @param imageResId
	 *            resource ID for image
	 * @param textResId
	 *            resource ID for text
	 * @param length
	 *            {@link Toast#LENGTH_LONG} or {@link Toast#LENGTH_SHORT}
	 * 
	 * @return toast with image
	 * 
	 * @see #makeImageToast(Context, int, int, int)
	 */
	public static Toast makeImageToast(Context context, int imageResId, int textResId, int
			length) {
		return makeImageToast(context, imageResId, context.getString(textResId), length);
	}
	
	/**
	 * Create toast notification with an image in front of text.<br>
	 * <br>
	 * Since it's not that easy to recreate the original toast layout, we take the original and
	 * modify it. This is a really dirty hack which places an image view into an inflated toast
	 * layout.<br>
	 * However, it won't crash on implementation changes of android's toast layout (which will
	 * probably not happen). If so you will get an ordinary toast.
	 * 
	 * @param context
	 * @param imageResId
	 *            resource ID for image
	 * @param text
	 *            text to use
	 * @param length
	 *            {@link Toast#LENGTH_LONG} or {@link Toast#LENGTH_SHORT}
	 * 
	 * @return toast with image
	 */
	public static Toast makeImageToast(Context context, int imageResId, CharSequence text,
			int length) {
		Toast toast = Toast.makeText(context, text, length);
		
		View rootView = toast.getView();
		LinearLayout linearLayout = null;
		View messageTextView = null;
		
		// check (expected) toast layout
		if (rootView instanceof LinearLayout) {
			linearLayout = (LinearLayout) rootView;
			
			if (linearLayout.getChildCount() == 1) {
				View child = linearLayout.getChildAt(0);
				
				if (child instanceof TextView) {
					messageTextView = (TextView) child;
					
					if (!(messageTextView.getLayoutParams() instanceof LinearLayout.LayoutParams)) {
						messageTextView = null;
					}
				}
			}
		}
		
		// cancel modification because toast layout is not what we expected
		if (linearLayout == null || messageTextView == null) {
			L.w("failed to create image toast layout, using usual toast");
			return toast;
		}
		
		ViewGroup.LayoutParams textParams = messageTextView.getLayoutParams();
		((LinearLayout.LayoutParams) textParams).gravity = Gravity.CENTER_VERTICAL;
		
		// convert dip dimension
		int imageSize = dipToPixel(context, 25);
		int imageMargin = dipToPixel(context, 15);
		
		// setup image view layout parameters
		LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(imageSize, imageSize);
		imageParams.setMargins(0, 0, imageMargin, 0);
		imageParams.gravity = Gravity.CENTER_VERTICAL;
		
		// setup image view
		ImageView imageView = new ImageView(context);
		imageView.setImageResource(imageResId);
		imageView.setLayoutParams(imageParams);
		
		// modify root layout
		linearLayout.setOrientation(LinearLayout.HORIZONTAL);
		linearLayout.addView(imageView, 0);
		
		return toast;
	}
	
	
	/**
	 * Get free (unused) space on external storage.<br>
	 * <br>
	 * See {@link Environment#getExternalStorageDirectory()}.
	 * 
	 * @return free space in kilobytes
	 */
	public static int getFreeExteranlStorageSize() {
		StatFs stats = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
		int availableBlocks = stats.getAvailableBlocks();
		int blockSizeInBytes = stats.getBlockSize();
		
		return availableBlocks * (blockSizeInBytes / 1024);
	}
	
	/**
	 * Is device connected to network (WiFi or mobile).<br>
	 * <br>
	 * <b>Hint</b>: A connection to WiFi does not guarantee Internet access.
	 * 
	 * @param context
	 * 
	 * @return {@code true} if device is connected to mobile network or WiFi
	 */
	public static boolean isConnected(Context context) {
		return isWiFiConnected(context) || isMobileNetworkConnected(context);
	}
	
	/**
	 * Is device connected to WiFi?<br>
	 * <br>
	 * <b>Hint</b>: A connection to WiFi does not guarantee Internet access.
	 * 
	 * @param context
	 * 
	 * @return {@code true} if device is connected to an access point
	 */
	public static boolean isWiFiConnected(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		return cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
	}
	
	/**
	 * Is device connected to mobile network?
	 * 
	 * @param context
	 * 
	 * @return {@code true} if device is connected to mobile network
	 */
	public static boolean isMobileNetworkConnected(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		return cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnected();
	}
	
	
	/**
	 * Get installed SQLite version as string.
	 * 
	 * @return installed SQLite version as string (e.g. {@code "3.5.9"})
	 */
	public static String getSQLiteVersion() {
		if (mSQLiteVersion == null) {
			SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(":memory:", null);
			Cursor cursor = db.rawQuery("select sqlite_version() AS sqlite_version", null);
			
			mSQLiteVersion = "";
			
			while (cursor.moveToNext()) {
				mSQLiteVersion += "." + cursor.getString(0);
			}
			
			if (mSQLiteVersion.startsWith(".")) {
				mSQLiteVersion = mSQLiteVersion.substring(1);
			}
			
			cursor.close();
			db.close();
		}
		
		return mSQLiteVersion;
	}
	
	/**
	 * Does SQLite support FTS3 (full text search)?
	 * 
	 * @return {@code true} if SQLite supports FTS3
	 */
	public static boolean doesSQLiteSupportFTS3() {
		if (mSQLiteSupportsFTS3 == null) {
			mSQLiteSupportsFTS3 = StringUtils.compareVersion(
					".", "3.4.0", getSQLiteVersion()) <= 0;
		}
		
		return mSQLiteSupportsFTS3;
	}
	
	/**
	 * Does SQLite support FTS4 (full text search)?
	 * 
	 * @return {@code true} if SQLite supports FTS4
	 */
	public static boolean doesSQLiteSupportFTS4() {
		if (mSQLiteSupportsFTS4 == null) {
			mSQLiteSupportsFTS4 = StringUtils.compareVersion(
					".", "3.7.4", getSQLiteVersion()) <= 0;
		}
		
		return mSQLiteSupportsFTS4;
	}
	
	/**
	 * Does SQLite support foreign keys?
	 * 
	 * @return {@code true} if SQLite supports foreign keys
	 */
	public static boolean doesSQLiteSupportsForeignKeys() {
		if (mSQLiteSupportsForeignKeys == null) {
			mSQLiteSupportsForeignKeys = StringUtils.compareVersion(
					".", "3.6.19", getSQLiteVersion()) <= 0;
		}
		
		return mSQLiteSupportsForeignKeys;
	}
	
	
	/**
	 * Convert a DIP value to pixel.
	 * 
	 * @param context
	 * @param dip
	 *            dip value
	 * 
	 * @return pixel value which is calculated depending on your current device configuration
	 */
	public static int dipToPixel(Context context, float dip) {
		return (int) (dip * context.getResources().getDisplayMetrics().density + 0.5f);
	}
	
	/**
	 * Get status bar height.<br>
	 * <br>
	 * <b>Note</b>: Call this when content is completely inflated. So {@code onCreate} of an
	 * activity won't work.
	 * 
	 * @param window
	 *            window of application
	 * 
	 * @return height of status bar in pixel
	 */
	public static int getStatusBarHeight(Window window) {
		window.getDecorView().getWindowVisibleDisplayFrame(mStatusBarRect);
		return mStatusBarRect.top;
	}
	
	/**
	 * Get title bar height.<br>
	 * <br>
	 * <b>Note</b>: Call this when content is completely inflated. So {@code onCreate} of an
	 * activity won't work.
	 * 
	 * @param window
	 *            window of application
	 * 
	 * @return height of status bar in pixel
	 */
	public static int getTitleBarHeight(View view) {
		view.getWindowVisibleDisplayFrame(mStatusBarRect);
		return mStatusBarRect.top;
	}
	
	/**
	 * Get offset of application content from top (so status bar + title bar).<br>
	 * <br>
	 * <b>Note</b>: Call this when content is completely inflated. So {@code onCreate} of an
	 * activity won't work.
	 * 
	 * @param view
	 *            any view which is attached to the content
	 * 
	 * @return offset to top in pixel
	 */
	public static int getContentOffsetFromTop(View view) {
		int offset = view.getRootView().findViewById(Window.ID_ANDROID_CONTENT).getTop();
		
		if (offset == 0) {
			view.getWindowVisibleDisplayFrame(mStatusBarRect);
			offset = mStatusBarRect.top;
		}
		
		return offset;
	}
	
	/**
	 * Get location of view relative to screen.<br>
	 * <br>
	 * <b>Note</b>: Call this when content is completely inflated. So {@code onCreate} of an
	 * activity won't work.
	 * 
	 * @param view
	 *            any view which is attached to the content
	 * 
	 * @return location pixel
	 */
	public static Point getScreenLocation(View view) {
		view.getLocationOnScreen(mLocation);
		return new Point(mLocation[0], mLocation[1]);
	}
	
	/**
	 * Get location of view relative to application content (so no status bar and title bar).<br>
	 * <br>
	 * <b>Note</b>: Call this when content is completely inflated. So {@code onCreate} of an
	 * activity won't work.
	 * 
	 * @param view
	 *            any view which is attached to the content
	 * 
	 * @return location pixel
	 */
	public static Point getContentLocation(View view) {
		Point point = getScreenLocation(view);
		point.y -= getContentOffsetFromTop(view);
		return point;
	}
	
	/**
	 * Stop fling of list (using reflection).
	 * 
	 * @param list
	 *            list on which fling should be stopped
	 */
	public static void stopListFling(ListView list) {
		if (mFlingEndMethod != null) {
			try {
				mFlingEndMethod.invoke(mFlingEndField.get(list));
			} catch (Exception e) {
				// implementation changed - can't do anything here - we tried
			}
		}
	}
	
	// PRIVATE ====================================================================================
	
	/**
	 * No constructor for a static class.
	 */
	private AndroidUtils() {
		
	}
}
