package de.viktorreiser.toolbox.widget;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileFilter;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filterable;
import android.widget.ListAdapter;
import android.widget.Toast;
import de.viktorreiser.toolbox.R;
import de.viktorreiser.toolbox.os.WeakPool;
import de.viktorreiser.toolbox.util.AndroidUtils;
import de.viktorreiser.toolbox.util.SmbUtils;

/**
 * Intelligent samba share auto complete text view which queries for information asynchronously.<br>
 * <br>
 * This class needs <a href="http://jcifs.samba.org/">JCIFS samba</a> library.<br>
 * <br>
 * While WiFi is disabled auto completion is disabled too.<br>
 * <b>Note</b>: Don't forget to request for Internet permission in manifest! <br>
 * <br>
 * Following custom XML attributes are available:<br>
 * <ul>
 * <li>{@code showFiles="boolean"} - {@link #setShowFiles(boolean)}</li>
 * <li>{@code sambaUser="String"} - {@link #setLoginData(String, String)}</li>
 * <li>{@code sambaPassword="String"} - {@link #setLoginData(String, String)}</li>
 * <li>{@code deniedAccessText="String or ID"} - {@link #setDeniedAccessMessage(int)}</li>
 * <li>{@code deniedAccessImage="ID"} (valid with given text) -
 * {@link #setDeniedAccessMessage(int, int)}
 * </ul>
 * 
 * <i>Depends on</i>: {@link WeakPool}, {@link AndroidUtils}, {@link SmbUtils},
 * {@code res/layout/slim_dropdown_list_item.xml}, {@code res/values/attrs.xml}
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class SambaAutoComplete extends AutoCompleteTextView {
	
	// PRIVATE ====================================================================================
	
	/** Resource for drop down item as layout. */
	private static final int DROPDOWN_LAYOUT = R.layout.slim_dropdown_list_item;
	
	/**
	 * Period in millisecond which has to be waited before searching for available hosts will be
	 * triggered again.
	 */
	private static final int HOST_TASK_PERIOD = 2500;
	
	/** Pool for retaining asynchronous task after configuration changes. */
	private static final WeakPool<Object> mDataPool = new WeakPool<Object>();
	
	/** Incrementing ID for safe pooling. */
	private static int mCurrentPoolId = 1;
	
	
	/** Samba user name. */
	private String mUser = "";
	
	/** Samba user password. */
	private String mPassword = "";
	
	/** Listener which will be informed when samba data is loaded in background. */
	private OnSambaLoadListener mSambaLoadListener;
	
	/** Should be files listed in suggestion list too? */
	private boolean mShowFiles = true;
	
	
	/** Empty auto complete adapter. */
	private ArrayAdapter<String> mEmptyAdapter;
	
	/** Comparator for samba files. */
	private Comparator<SmbFile> mSmbFileComparator;
	
	/** No new line characters and make sure first character is a slash. */
	private InputFilter mInputFilter;
	
	/** Perform auto complete when getting focus. */
	private OnFocusChangeListener mFocusListener;
	
	/** Extern focus listener set with {@link #setOnFocusChangeListener(OnFocusChangeListener)}. */
	private OnFocusChangeListener mCustomFocusListener;
	
	/** Perform auto complete when view gets clicked. */
	private OnClickListener mClickListener;
	
	/** Toast message for denied access. */
	private Toast mDeniedAccessToast;
	
	/** Available hosts found. */
	private String [] mAvailableHosts;
	
	/** If background task is busy this will be the last input to complete when task returns. */
	private String mLastTextInput;
	
	/** Used to set an string ID, compare it next time and know that adapter hasn't to be changed. */
	private String mLastAdapterId = "";
	
	/** Is view loading samba information in background? */
	private boolean mIsLoading = false;
	
	
	/** Get available hosts task pool ID. */
	private int mAvailableHostsTaskId;
	
	/** Timestamp of last complete of available host task. */
	private long mLastAvailableHostsCheck = 0;
	
	/** Get auto complete suggestion for given folder pool ID. */
	private int mPathAutoCompleteTaskId;
	
	/** Current folder path (which is completed). */
	private String mFolderPath = "";
	
	/** Current complete criteria. */
	private String mStartsWith = "";
	
	// PUBLIC =====================================================================================
	
	/**
	 * Set samba login data.
	 * 
	 * @param user
	 *            samba user name
	 * @param password
	 *            samba user password
	 */
	public void setLoginData(String user, String password) {
		if (user != null && !user.trim().equals("")) {
			mUser = user;
			mPassword = password != null && password.trim().equals("") ? "" : password;
		} else {
			mPassword = "";
			mUser = "";
		}
		
		if (!mLastAdapterId.equals("hosts")) {
			mLastAdapterId = "";
		}
	}
	
	/**
	 * Set listener which will be informed when samba is loading data in background.
	 * 
	 * @param listener
	 *            samba load listener
	 */
	public void setOnSambaLoadListener(OnSambaLoadListener listener) {
		mSambaLoadListener = listener;
	}
	
	/**
	 * Get samba load listener.
	 * 
	 * @return samba load listener or {@code null} if no listener is set
	 */
	public OnSambaLoadListener getOnSambaLoadListener() {
		return mSambaLoadListener;
	}
	
	/**
	 * Is view loading data in background?
	 * 
	 * @return {@code true} if view is loading data in background
	 */
	public boolean isSambaLoading() {
		return mIsLoading;
	}
	
	/**
	 * Should be files listed in suggestion list too?<br>
	 * <br>
	 * Default is {@code true}.
	 * 
	 * @param showFiles
	 *            {@code true} if files should be listed
	 */
	public void setShowFiles(boolean showFiles) {
		if (mShowFiles != showFiles) {
			mShowFiles = showFiles;
			
			if (!mLastAdapterId.equals("hosts")) {
				mLastAdapterId = "";
				
				PathAutoCompleteTask pathTask = getPathAutoCompleteTask();
				
				// conditions changed - requery path completion
				if (pathTask != null && pathTask.getStatus() == Status.RUNNING) {
					pathTask.cancel(false);
					pathTask = new PathAutoCompleteTask(pathTask.mmFile, mShowFiles);
					pathTask.view = new WeakReference<SambaAutoComplete>(this);
					pathTask.execute();
					
					mPathAutoCompleteTaskId = mCurrentPoolId++;
					mDataPool.put(mPathAutoCompleteTaskId, pathTask);
				}
			}
		}
	}
	
	/**
	 * Should be files listed in suggestion list too?
	 * 
	 * @return {@code true} if files should be listed
	 */
	public boolean getShowFiles() {
		return mShowFiles;
	}
	
	/**
	 * Set toast message to display when samba access was denied to entered path.<br>
	 * <br>
	 * Probably the user has to check samba and user password (see
	 * {@link #setLoginData(String, String)}).
	 * 
	 * @param text
	 *            text as string resource
	 */
	public void setDeniedAccessMessage(int text) {
		setDeniedAccessMessage(getContext().getString(text));
	}
	
	/**
	 * Set toast message to display when samba access was denied to entered path.<br>
	 * <br>
	 * Probably the user has to check samba and user password (see
	 * {@link #setLoginData(String, String)}).
	 * 
	 * @param text
	 *            text as string
	 */
	public void setDeniedAccessMessage(CharSequence text) {
		if (mDeniedAccessToast != null) {
			mDeniedAccessToast.cancel();
		}
		
		mDeniedAccessToast = Toast.makeText(getContext(), text, Toast.LENGTH_LONG);
	}
	
	/**
	 * Set toast message to display when samba access was denied to entered path.<br>
	 * <br>
	 * Probably the user has to check samba and user password (see
	 * {@link #setLoginData(String, String)}).
	 * 
	 * @param text
	 *            text as string
	 * @param imgId
	 *            image resource for {@link AndroidUtils#makeImageToast(Context, int, int, int)}
	 * 
	 * @see AndroidUtils#makeImageToast(Context, int, CharSequence, int)
	 */
	public void setDeniedAccessMessage(CharSequence text, int imgId) {
		if (mDeniedAccessToast != null) {
			mDeniedAccessToast.cancel();
		}
		
		mDeniedAccessToast = AndroidUtils.makeImageToast(
				getContext(), imgId, text, Toast.LENGTH_LONG);
	}
	
	/**
	 * Set toast message to display when samba access was denied to entered path.<br>
	 * <br>
	 * Probably the user has to check samba and user password (see
	 * {@link #setLoginData(String, String)}).
	 * 
	 * @param text
	 *            text as string resource
	 * @param imgId
	 *            image resource for {@link AndroidUtils#makeImageToast(Context, int, int, int)}
	 * 
	 * @see AndroidUtils#makeImageToast(Context, int, CharSequence, int)
	 */
	public void setDeniedAccessMessage(int text, int imgId) {
		setDeniedAccessMessage(getContext().getString(text), imgId);
	}
	
	
	/**
	 * Listener which will be informed when samba is loading data in background.
	 */
	public static interface OnSambaLoadListener {
		
		/**
		 * Samba is loading data in background.
		 * 
		 * @param loading
		 *            {@code true} if started to load or {@code false} if stopped to load
		 */
		public void onSambaLoad(boolean loading);
	}
	
	// OVERRIDDEN =================================================================================
	
	public SambaAutoComplete(Context context) {
		super(context);
		initialize(null);
	}
	
	public SambaAutoComplete(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize(attrs);
	}
	
	public SambaAutoComplete(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize(attrs);
	}
	
	
	/**
	 * No input filter for this view.
	 */
	@Override
	public void setFilters(InputFilter [] inputfilters) {
		
	}
	
	/**
	 * No adapter for this view.
	 */
	@Override
	public <T extends ListAdapter & Filterable> void setAdapter(T adapter) {
		
	}
	
	/**
	 * <i>Overridden for internal use!</i><br>
	 * <br>
	 * Ordinary set on focus change listener.
	 */
	@Override
	public void setOnFocusChangeListener(final OnFocusChangeListener l) {
		if (mFocusListener == null) {
			super.setOnFocusChangeListener(l);
		} else if (l == null) {
			super.setOnFocusChangeListener(mFocusListener);
		} else {
			super.setOnFocusChangeListener(new OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					mFocusListener.onFocusChange(v, hasFocus);
					l.onFocusChange(v, hasFocus);
				}
			});
		}
	}
	
	/**
	 * <i>Overridden for internal use!</i><br>
	 * <br>
	 * Ordinary set on click listener.
	 */
	@Override
	public void setOnClickListener(final OnClickListener listener) {
		if (mClickListener == null) {
			super.setOnClickListener(listener);
		} else if (listener == null) {
			super.setOnClickListener(mClickListener);
		} else {
			super.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mClickListener.onClick(v);
					listener.onClick(v);
				}
			});
		}
	}
	
	/**
	 * <i>Overridden for internal use!</i><br>
	 * <br>
	 * Get focus listener set with {@link #setOnFocusChangeListener(OnFocusChangeListener)}.
	 */
	@Override
	public OnFocusChangeListener getOnFocusChangeListener() {
		return mCustomFocusListener;
	}
	
	/**
	 * <i>Overridden for internal use!</i><br>
	 * <br>
	 * Perform auto complete suggestion search and update.
	 * 
	 * @see AutoCompleteTextView
	 */
	@Override
	protected void performFiltering(CharSequence text, int keyCode) {
		mLastTextInput = text.toString();
		
		// nothing to check if WiFi is disabled
		if (!AndroidUtils.isWiFiConnected(getContext()) || mLastTextInput.length() == 0) {
			mLastAdapterId = "";
			super.setAdapter(mEmptyAdapter);
			super.performFiltering(text, keyCode);
			triggerLoading(false);
			return;
		}
		
		// query for auto completion
		if (!performCompletion(mLastTextInput)) {
			mLastAdapterId = "";
			super.setAdapter(mEmptyAdapter);
		}
		
		// hide busy indicator if no task is running
		PathAutoCompleteTask pathTask = getPathAutoCompleteTask();
		AvailableHostsTask hostTask = getAvailableHostsTask();
		
		if ((pathTask == null || pathTask.getStatus() != Status.RUNNING)
				&& (hostTask == null || hostTask.getStatus() != Status.RUNNING)) {
			triggerLoading(false);
		}
	}
	
	/**
	 * <i>Overridden for internal use!</i><br>
	 * <br>
	 * Add clicked suggestion to text.
	 * 
	 * @see AutoCompleteTextView
	 */
	@Override
	protected void replaceText(CharSequence text) {
		// entered path to last slash + selected suggestion
		String path = getText().toString().trim();
		path = path.substring(0, path.lastIndexOf("/") + 1) + text;
		
		super.replaceText(path);
		
		// perform filtering again so a new adapter will be loaded (if necessary)
		performFiltering(path, 0);
	}
	
	/**
	 * <i>Overridden for internal use!</i><br>
	 * <br>
	 * Save state.
	 */
	@Override
	public Parcelable onSaveInstanceState() {
		final SavedState myState = new SavedState(super.onSaveInstanceState());
		myState.availableHostsTaskId = mAvailableHostsTaskId;
		myState.pathAutoComleteTaskId = mPathAutoCompleteTaskId;
		return myState;
	}
	
	/**
	 * <i>Overridden for internal use!</i><br>
	 * <br>
	 * Restore state.
	 */
	@Override
	public void onRestoreInstanceState(Parcelable state) {
		SavedState myState = (SavedState) state;
		super.onRestoreInstanceState(myState.getSuperState());
		mAvailableHostsTaskId = myState.availableHostsTaskId;
		mPathAutoCompleteTaskId = myState.pathAutoComleteTaskId;
		mLastAvailableHostsCheck = myState.lastAvailableHostsCheck;
		
		AvailableHostsTask hostTask = getAvailableHostsTask();
		PathAutoCompleteTask pathTask = getPathAutoCompleteTask();
		
		if (hostTask != null) {
			hostTask.view = new WeakReference<SambaAutoComplete>(this);
			
			if (hostTask.getStatus() == Status.RUNNING) {
				mIsLoading = true;
			}
		}
		
		if (pathTask != null) {
			pathTask.view = new WeakReference<SambaAutoComplete>(this);
			
			if (pathTask.getStatus() == Status.RUNNING) {
				mIsLoading = true;
			}
		}
	}
	
	// PRIVATE ====================================================================================
	
	private void initialize(AttributeSet attrs) {
		mEmptyAdapter = new ArrayAdapter<String>(getContext(), DROPDOWN_LAYOUT, new String [0]);
		
		mClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				performFiltering(getText().toString(), 0);
			}
		};
		
		super.setOnClickListener(mClickListener);
		
		setupSmbFileComparator();
		setupInputFilter();
		setupFocusListener();
		
		if (attrs != null) {
			setupXmlAttributes(attrs);
		}
		
		super.setThreshold(0);
		super.setAdapter(mEmptyAdapter);
		super.setFilters(new InputFilter [] {mInputFilter});
	}
	
	/**
	 * Setup given XML attributes.
	 */
	private void setupXmlAttributes(AttributeSet attrs) {
		TypedArray a = getContext().obtainStyledAttributes(
				attrs, R.styleable.SambaAutoComplete);
		
		String showFiles = a.getString(R.styleable.SambaAutoComplete_showFiles);
		if (showFiles != null) {
			setShowFiles(Boolean.parseBoolean(showFiles));
		}
		
		setLoginData(
				a.getString(R.styleable.SambaAutoComplete_sambaUser),
				a.getString(R.styleable.SambaAutoComplete_sambaPassword));
		
		int image = a.getResourceId(R.styleable.SambaAutoComplete_deniedAccessImage, 0);
		String text = a.getString(R.styleable.SambaAutoComplete_deniedAccessText);
		
		if (text != null) {
			if (image == 0) {
				setDeniedAccessMessage(text);
			} else {
				setDeniedAccessMessage(text, image);
			}
		}
		
		a.recycle();
	}
	
	/**
	 * Setup input filter for view.
	 */
	private void setupInputFilter() {
		mInputFilter = new InputFilter() {
			@Override
			public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
					int dstart, int dend) {
				// only delete leading slash if it's the last character
				if (start == end) {
					return dstart == 0 && dend != dest.length() ? "/" : null;
				}
				
				String r = source.toString();
				
				// do not replace leading slash
				if (dstart == 0 && !r.startsWith("/")) {
					r = r.length() == 1 ? "/" : "/" + r;
				}
				
				// remove all new line characters
				return r.replace("\n", "");
			}
		};
	}
	
	/**
	 * Setup samba file comparator for sorting files and directories.
	 */
	private void setupSmbFileComparator() {
		mSmbFileComparator = new Comparator<SmbFile>() {
			@Override
			public int compare(SmbFile f1, SmbFile f2) {
				if (mShowFiles) {
					try {
						if (f1.isFile() && f2.isDirectory()) {
							return 1;
						}
						
						if (f1.isDirectory() && f2.isFile()) {
							return -1;
						}
					} catch (SmbException e) {
					}
				}
				
				return f1.getName().compareTo(f2.getName());
			}
		};
	}
	
	/**
	 * Setup focus listener which perform auto completion if text field getting focus.
	 */
	private void setupFocusListener() {
		mFocusListener = new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus && !getText().toString().trim().equals("")) {
					performFiltering(getText(), 0);
				}
			}
		};
		
		super.setOnFocusChangeListener(mFocusListener);
	}
	
	
	/**
	 * Set adapter by super class.
	 */
	private <T extends ListAdapter & Filterable> void setSuperAdapter(T adapter) {
		super.setAdapter(adapter);
	}
	
	/**
	 * Perform filtering by super class.
	 */
	private void performSuperFiltering(CharSequence text, int keyCode) {
		super.performFiltering(text, keyCode);
	}
	
	
	/**
	 * Get task from weak object pool.
	 * 
	 * @return task or {@code null} if task is garbage collected or not available
	 */
	private PathAutoCompleteTask getPathAutoCompleteTask() {
		Object task = mDataPool.get(mPathAutoCompleteTaskId);
		
		if (task != null && task instanceof PathAutoCompleteTask) {
			return (PathAutoCompleteTask) task;
		} else {
			return null;
		}
	}
	
	/**
	 * Get task from weak object pool.
	 * 
	 * @return task or {@code null} if task is garbage collected or not available
	 */
	private AvailableHostsTask getAvailableHostsTask() {
		Object task = mDataPool.get(mAvailableHostsTaskId);
		
		if (task != null && task instanceof AvailableHostsTask) {
			return (AvailableHostsTask) task;
		} else {
			return null;
		}
	}
	
	/**
	 * Perform search for auto completion.
	 * 
	 * @param text
	 *            current entered text
	 * 
	 * @return {@code true} if auto completion adapter was set otherwise no suggestion were found
	 */
	private boolean performCompletion(String text) {
		String p = text.substring(1);
		int firstSlash = p.indexOf("/");
		
		String host = firstSlash == -1 ? p : p.substring(0, firstSlash);
		String path = firstSlash == -1 ? "" : p.substring(firstSlash + 1, p.length());
		
		if (firstSlash == -1) {
			AvailableHostsTask hostTask = getAvailableHostsTask();
			PathAutoCompleteTask pathTask = getPathAutoCompleteTask();
			
			if (pathTask != null) {
				pathTask.cancel(false);
				mPathAutoCompleteTaskId = 0;
			}
			
			if ((hostTask == null || hostTask.getStatus() != Status.RUNNING)
					&& new Date().getTime() - mLastAvailableHostsCheck > HOST_TASK_PERIOD) {
				triggerLoading(true);
				hostTask = new AvailableHostsTask();
				hostTask.view = new WeakReference<SambaAutoComplete>(this);
				hostTask.execute();
				
				mAvailableHostsTaskId = mCurrentPoolId++;
				mDataPool.put(mAvailableHostsTaskId, hostTask);
			}
			
			// auto complete host if no path given
			if (mAvailableHosts != null) {
				if (!mLastAdapterId.equals("hosts")) {
					mLastAdapterId = "hosts";
					super.setAdapter(new ArrayAdapter<String>(getContext(), DROPDOWN_LAYOUT,
							mAvailableHosts));
				}
				
				super.performFiltering(host, 0);
				
				return true;
			}
		} else {
			return autoCompletePath(host, path);
		}
		
		return false;
	}
	
	/**
	 * Get auto complete for path.
	 * 
	 * @param host
	 *            samba host
	 * @param path
	 *            path on host
	 */
	private boolean autoCompletePath(String host, final String path) {
		int lastSlash = path.lastIndexOf("/");
		
		// get folder path before last slash
		String folderPath = lastSlash == -1 ? "" : path.substring(0, lastSlash + 1);
		
		// get last bit of path in lower case letters
		mStartsWith = lastSlash == -1 ? path.toLowerCase() : path.substring(
				lastSlash + 1, path.length()).toLowerCase();
		
		if (mLastAdapterId.equals("/" + folderPath)) {
			super.performFiltering(mStartsWith, 0);
			return true;
		}
		
		// update or cancel running task
		PathAutoCompleteTask pathTask = getPathAutoCompleteTask();
		AvailableHostsTask hostTaks = getAvailableHostsTask();
		
		if (hostTaks != null) {
			hostTaks.cancel(false);
			mAvailableHostsTaskId = 0;
		}
		
		if (pathTask != null) {
			if (pathTask.getStatus() == Status.RUNNING) {
				if (!mFolderPath.equals(folderPath)) {
					pathTask.cancel(false);
					pathTask = null;
				}
			} else {
				pathTask = null;
			}
		}
		
		mFolderPath = folderPath;
		
		// if no task is running we create a new one
		if (pathTask == null) {
			triggerLoading(true);
			
			pathTask = new PathAutoCompleteTask(
					SmbUtils.getFile(mUser, mPassword, host, folderPath), mShowFiles);
			pathTask.view = new WeakReference<SambaAutoComplete>(this);
			pathTask.execute();
			
			mPathAutoCompleteTaskId = mCurrentPoolId++;
			mDataPool.put(mPathAutoCompleteTaskId, pathTask);
		}
		
		return false;
	}
	
	/**
	 * Set samba data loading state and call listener.
	 * 
	 * @param isLoading
	 *            {@code true} when started to load data or {@code false} when stopped
	 */
	private void triggerLoading(boolean isLoading) {
		if (mIsLoading != isLoading) {
			mIsLoading = isLoading;
			
			if (mSambaLoadListener != null) {
				mSambaLoadListener.onSambaLoad(isLoading);
			}
		}
	}
	
	
	/**
	 * Used to re/store state of view.
	 * 
	 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
	 */
	private static class SavedState extends BaseSavedState {
		
		// PUBLIC ---------------------------------------------------------------------------------
		
		int availableHostsTaskId;
		int pathAutoComleteTaskId;
		int lastAvailableHostsCheck;
		
		@SuppressWarnings("unused")
		public static final Parcelable.Creator<SavedState> CREATOR =
				new Parcelable.Creator<SavedState>() {
					@Override
					public SavedState createFromParcel(Parcel in) {
						return new SavedState(in);
					}
					
					@Override
					public SavedState [] newArray(int size) {
						return new SavedState [size];
					}
				};
		
		// OVERRIDDEN -----------------------------------------------------------------------------
		
		public SavedState(Parcel source) {
			super(source);
			availableHostsTaskId = source.readInt();
			pathAutoComleteTaskId = source.readInt();
			lastAvailableHostsCheck = source.readInt();
		}
		
		public SavedState(Parcelable superState) {
			super(superState);
		}
		
		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeInt(availableHostsTaskId);
			dest.writeInt(pathAutoComleteTaskId);
			dest.writeInt(lastAvailableHostsCheck);
		}
	}
	
	/**
	 * Asynchronous task which retrieves a list of available hosts from samba.
	 * 
	 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
	 */
	private static class AvailableHostsTask extends AsyncTask<Void, Void, String []> {
		
		// PUBLIC ---------------------------------------------------------------------------------
		
		WeakReference<SambaAutoComplete> view = null;
		
		// OVERRIDDEN -----------------------------------------------------------------------------
		
		@Override
		protected String [] doInBackground(Void... params) {
			return SmbUtils.listAvailableHosts(true);
		}
		
		@Override
		protected void onPostExecute(String [] hosts) {
			SambaAutoComplete v = view.get();
			
			if (v == null) {
				return;
			}
			
			v.triggerLoading(false);
			v.mLastAvailableHostsCheck = new Date().getTime();
			
			if (v.mAvailableHosts != null && v.mAvailableHosts.length != 0) {
				// new hosts received - add to already found hosts
				HashSet<String> currentHosts = new HashSet<String>(
						Arrays.asList(v.mAvailableHosts));
				List<String> newHostList = new ArrayList<String>();
				newHostList.addAll(currentHosts);
				
				for (int i = 0; i < hosts.length; i++) {
					if (!currentHosts.contains(hosts[i] + "/")) {
						newHostList.add(hosts[i] + "/");
					}
				}
				
				v.mAvailableHosts = newHostList.toArray(new String [0]);
			} else {
				// no hosts available - take what you get
				String [] hostsWithSlash = new String [hosts.length];
				
				for (int i = 0; i < hostsWithSlash.length; i++) {
					hostsWithSlash[i] = hosts[i] + "/";
				}
				
				v.mAvailableHosts = hostsWithSlash;
			}
			
			// reset adapter when host adapter set
			if (v.mLastAdapterId.equals("hosts")) {
				v.mLastAdapterId = "";
			}
			
			if (v.isFocused()) {
				v.performFiltering(v.mLastTextInput, 0);
			}
		}
	}
	
	/**
	 * Asynchronous task which loads auto completion for given folder from samba.
	 * 
	 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
	 */
	private static class PathAutoCompleteTask extends AsyncTask<Void, Void, Void> {
		
		// PRIVATE --------------------------------------------------------------------------------
		
		private SmbFile mmFile;
		private SmbFile [] mmFiles;
		private boolean mmShowFiles;
		private SmbException mmException;
		
		// PUBLIC ---------------------------------------------------------------------------------
		
		WeakReference<SambaAutoComplete> view = null;
		
		/**
		 * Create path completion task.
		 * 
		 * @param file
		 *            path to folder for which to create an auto completion adapter
		 * @param showFiles
		 *            should files be listed in auto complete suggestions
		 */
		public PathAutoCompleteTask(SmbFile file, boolean showFiles) {
			mmFile = file;
			mmShowFiles = showFiles;
		}
		
		// OVERRIDDEN -----------------------------------------------------------------------------
		
		@Override
		protected Void doInBackground(Void... params) {
			try {
				mmFiles = mmFile.listFiles(new SmbFileFilter() {
					@Override
					public boolean accept(SmbFile file) throws SmbException {
						return mmShowFiles || file.isDirectory();
					}
				});
			} catch (SmbException e) {
				mmException = e;
			}
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			SambaAutoComplete v = view.get();
			
			if (v == null) {
				return;
			}
			
			v.triggerLoading(false);
			
			if (mmException != null) {
				if (SmbUtils.isAccessDenied(mmException)) {
					if (v.mDeniedAccessToast != null) {
						v.mDeniedAccessToast.cancel();
						v.mDeniedAccessToast.show();
					}
				}
				
				v.mLastAdapterId = "/" + v.mFolderPath;
				
				return;
			}
			
			if (mmFiles != null && mmFiles.length != 0) {
				// sort files and trigger auto complete after that
				Arrays.sort(mmFiles, v.mSmbFileComparator);
				
				String [] complete = new String [mmFiles.length];
				
				for (int i = 0; i < mmFiles.length; i++) {
					complete[i] = mmFiles[i].getName();
				}
				
				v.setSuperAdapter(new ArrayAdapter<String>(v.getContext(), DROPDOWN_LAYOUT,
						complete));
				v.performSuperFiltering(v.mStartsWith, 0);
			} else {
				v.setSuperAdapter(v.mEmptyAdapter);
			}
			
			v.mLastAdapterId = "/" + v.mFolderPath;
		}
	}
}
