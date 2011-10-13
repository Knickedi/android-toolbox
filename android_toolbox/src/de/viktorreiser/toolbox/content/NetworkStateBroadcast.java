package de.viktorreiser.toolbox.content;

import de.viktorreiser.toolbox.util.L;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiManager;

/**
 * Network state information.<br>
 * <br>
 * This class simplifies handling with connect information of WiFi and mobile network and broadcasts
 * a {@link #NETWORK_STATE_ACTION} on change of state. Use static methods to get information about
 * change.<br>
 * <br>
 * <b>Setup in manifest file</b>:
 * 
 * <pre>
 * {@code <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
 * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
 * 
 * <receiver android:name="de.viktorreiser.toolbox.content.WifiStateBroadcast">
 * 	<intent-filter>
 * 		<action android:name="android.net.conn.CONNECTIVITY_CHANGE" />
 * 		<action android:name="android.net.wifi.supplicant.CONNECTION_CHANGE" />
 * 	</intent-filter>
 * </receiver>
 * }
 * </pre>
 * 
 * <i>Depends on</i>: {@link L}
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class NetworkStateBroadcast extends BroadcastReceiver {
	
	// PUBLIC CONSTANT ============================================================================
	
	/** Action broadcasted if network state changes. */
	public static final String NETWORK_STATE_ACTION = NetworkStateBroadcast.class.getName()
			+ ".ACTION_NETWORK_STATE";
	
	// PRIVATE ====================================================================================
	
	/** {@code true} if WiFi is connected. */
	private static boolean mWifiConnected = false;
	
	/** {@code true} if mobile network is connected. */
	private static boolean mMobileConnected = false;
	
	// PUBLIC =====================================================================================
	
	/**
	 * Is WiFi connected?<br>
	 * <br>
	 * {@code false} until first change noticed or {@link #initialCheck(Context)} is called.<br>
	 * <br>
	 * <b>Note</b>: Remember that a connection with WiFi does not guarantee Internet access.
	 * 
	 * @return {@code true} if WiFi is connected
	 */
	public static boolean isWifiConnected() {
		return mWifiConnected;
	}
	
	/**
	 * Is mobile network connected?<br>
	 * <br>
	 * {@code false} until first change noticed or {@link #initialCheck(Context)} is called.
	 * 
	 * @return {@code true} if mobile network is connected
	 */
	public static boolean isMobileConnected() {
		return mMobileConnected;
	}
	
	/**
	 * Is mobile network or WiFi connected?<br>
	 * <br>
	 * {@code false} until first change noticed or {@link #initialCheck(Context)} is called.<br>
	 * <br>
	 * <b>Note</b>: Remember that a connection to WiFi does not guarantee Internet access.
	 * 
	 * @return {@code true} if mobile network or WiFi is connected
	 */
	public static boolean isConnected() {
		return mMobileConnected || mWifiConnected;
	}
	
	/**
	 * Perform initial connection check on application startup.<br>
	 * <br>
	 * This check <b>won't</b> send a broadcast message.<br>
	 * <br>
	 * A good place to call this method is in a custom {@link Application#onCreate()}.
	 * 
	 * @param context
	 */
	public static void initialCheck(Context context) {
		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		mWifiConnected = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
				.getState() == State.CONNECTED;
		
		mMobileConnected = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
				.getState() == State.CONNECTED;
	}
	
	// OVERRIDDEN =================================================================================
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		boolean changed = false;
		
		if (intent.getAction().equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
			if (!intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, true)) {
				if (mWifiConnected) {
					mWifiConnected = false;
					changed = true;
				}
			}
		} else if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
			ConnectivityManager cm = (ConnectivityManager) context.getSystemService(
					Context.CONNECTIVITY_SERVICE);
			
			NetworkInfo netInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
			
			if (netInfo.getState() == NetworkInfo.State.CONNECTED) {
				if (!mMobileConnected) {
					mMobileConnected = true;
					changed = true;
				}
			} else if (netInfo.getState() == NetworkInfo.State.DISCONNECTING
					|| netInfo.getState() == NetworkInfo.State.DISCONNECTED) {
				if (mMobileConnected) {
					mMobileConnected = false;
					changed = true;
				}
			}
			
			netInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			
			if (netInfo.getState() == NetworkInfo.State.CONNECTED) {
				if (!mWifiConnected) {
					mWifiConnected = true;
					changed = true;
				}
			} else if (netInfo.getState() == NetworkInfo.State.DISCONNECTING
					|| netInfo.getState() == NetworkInfo.State.DISCONNECTED) {
				if (mWifiConnected) {
					mWifiConnected = false;
					changed = true;
				}
			}
		}
		
		if (changed) {
			context.sendBroadcast(new Intent(NETWORK_STATE_ACTION));
			
			if (L.isI()) {
				L.i("Network state: mobile=" + String.valueOf(mMobileConnected)
						+ " wifi=" + String.valueOf(mWifiConnected));
			}
		}
	}
}
