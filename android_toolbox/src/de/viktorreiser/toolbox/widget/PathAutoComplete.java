package de.viktorreiser.toolbox.widget;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Comparator;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Environment;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filterable;
import android.widget.ListAdapter;
import de.viktorreiser.toolbox.R;

/**
 * Auto complete text view for folder and file path.<br>
 * <br>
 * Following custom XML attributes are available:<br>
 * <ul>
 * <li>{@code showFiles="boolean"} - {@link #setShowFiles(boolean)}</li>
 * <li>{@code rootDir="String"} (use <code>{SDCARD}/path</code> as wildcard) -
 * {@link #setRootDir(String)}</li>
 * </ul>
 * 
 * <i>Depends on</i>: {@code res/layout/slim_dropdown_list_item.xml}, {@code res/values/attrs.xml}
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class PathAutoComplete extends AutoCompleteTextView {
	
	// PRIVATE ====================================================================================
	
	/** Resource for drop down item layout. */
	private static final int DROPDOWN_LAYOUT = R.layout.slim_dropdown_list_item;
	
	
	/** {@code true} if auto complete should list files too. */
	private boolean mShowFiles = true;
	
	/** Root directory from which to auto complete. */
	private String mRootDir = null;
	
	
	/** Empty auto complete adapter which will be set if no suggestions available. */
	private ArrayAdapter<String> mEmptyAdapter;
	
	/** Compare and sort files (directories first and by filename). */
	private Comparator<File> mFileComparator;
	
	/** Samba file filter (directory only or all files). */
	private FileFilter mFileFilter;
	
	/** No new line characters and make sure first character is a slash. */
	private InputFilter mInputFilter;
	
	/** Perform auto complete when getting focus. */
	private OnFocusChangeListener mFocusListener;
	
	/** Extern focus listener set with {@link #setOnFocusChangeListener(OnFocusChangeListener)}. */
	private OnFocusChangeListener mCustomFocusListener;
	
	/** Perform auto complete when view gets clicked. */
	private OnClickListener mClickListener;
	
	/** Used to set an string ID, compare it next time and know that adapter hasn't to be changed. */
	private String mLastAdapterId = "";
	
	// PUBLIC =====================================================================================
	
	/**
	 * Set root directory from which to auto complete.<br>
	 * <br>
	 * Default is set to SD card directory.<br>
	 * If given path is invalid then root directory will be set to SD card directory.
	 * 
	 * @param rootDir
	 *            path to root directory or {@code null} to set to SD card directory
	 */
	public void setRootDir(String rootDir) {
		mRootDir = Environment.getExternalStorageDirectory().getAbsolutePath();
		
		if (rootDir != null && !rootDir.trim().equals("")) {
			File file = new File(rootDir);
			
			if (file.exists() && file.isDirectory()) {
				mRootDir = file.getAbsolutePath();
			}
		}
	}
	
	/**
	 * Get root directory.
	 * 
	 * @return root directory
	 */
	public String getRootDir() {
		return mRootDir;
	}
	
	/**
	 * Should auto complete include files?<br>
	 * <br>
	 * Default is {@code true}.
	 * 
	 * @param showFiles
	 *            {@code true} if auto complete should include files
	 */
	public void setShowFiles(boolean showFiles) {
		mShowFiles = showFiles;
		mLastAdapterId = "";
	}
	
	/**
	 * Does auto complete include files?
	 * 
	 * @return {@code true} if auto complete includes files
	 */
	public boolean getShowFiles() {
		return mShowFiles;
	}
	
	/**
	 * Get full entered path (with root path).
	 * 
	 * @return absolute file path
	 */
	public String getPathText() {
		String path = getText().toString();
		
		if (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		
		return mRootDir + path;
	}
	
	// OVERRIDDEN =================================================================================
	
	public PathAutoComplete(Context context) {
		super(context);
		initialize(null);
	}
	
	public PathAutoComplete(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize(attrs);
	}
	
	public PathAutoComplete(Context context, AttributeSet attrs, int defStyle) {
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
	 * Ordinary set on focus change listener.
	 */
	@Override
	public void setOnFocusChangeListener(OnFocusChangeListener listener) {
		mCustomFocusListener = listener;
		
		if (mFocusListener == null) {
			super.setOnFocusChangeListener(listener);
		} else if (mCustomFocusListener == null) {
			super.setOnFocusChangeListener(mFocusListener);
		} else {
			super.setOnFocusChangeListener(new OnFocusChangeListener() {
				@Override
				public void onFocusChange(View v, boolean hasFocus) {
					mFocusListener.onFocusChange(v, hasFocus);
					mCustomFocusListener.onFocusChange(v, hasFocus);
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
		String currentString = text.toString();
		
		if (text.length() == 0) {
			mLastAdapterId = "";
			super.setAdapter(mEmptyAdapter);
			super.performFiltering(text, keyCode);
			return;
		}
		
		if (!mRootDir.equals("/")) {
			currentString = mRootDir + currentString;
		}
		
		int lastSlash = currentString.lastIndexOf("/");
		
		// get folder path before last slash
		String folderPath = currentString.substring(0, lastSlash + 1);
		
		// get last bit of path in lower case letters
		final String startsWith = currentString.substring(lastSlash + 1, currentString.length())
				.toLowerCase();
		
		// load adapter if not already did
		if (!mLastAdapterId.equals(folderPath)) {
			createAutoCompleteForFolder(folderPath);
		}
		
		mLastAdapterId = folderPath;
		
		// filter files for current set adapter
		super.performFiltering(startsWith, keyCode);
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
	
	// PRIVATE ====================================================================================
	
	private void initialize(AttributeSet attrs) {
		mEmptyAdapter = new ArrayAdapter<String>(getContext(), DROPDOWN_LAYOUT, new String [0]);
		
		mFileFilter = new FileFilter() {
			@Override
			public boolean accept(File file) {
				return mShowFiles || file.isDirectory();
			}
		};
		
		mClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				performFiltering(getText().toString(), 0);
			}
		};
		
		setRootDir(null);
		
		super.setOnClickListener(mClickListener);
		
		setupFileComparator();
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
	 * Setup text edit input filter.
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
	 * Setup file comparator for sorting files and folders.
	 */
	private void setupFileComparator() {
		mFileComparator = new Comparator<File>() {
			@Override
			public int compare(File f1, File f2) {
				if (mShowFiles) {
					if (f1.isFile() && f2.isDirectory()) {
						return 1;
					}
					
					if (f1.isDirectory() && f2.isFile()) {
						return -1;
					}
				}
				
				return f1.getName().compareTo(f2.getName());
			}
		};
	}
	
	/**
	 * Setup given XML attributes.
	 */
	private void setupXmlAttributes(AttributeSet attrs) {
		TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.PathAutoComplete);
		
		String showFiles = a.getString(R.styleable.PathAutoComplete_showFiles);
		if (showFiles != null) {
			setShowFiles(Boolean.parseBoolean(showFiles));
		}
		
		setRootDir(a.getString(R.styleable.PathAutoComplete_rootDir));
		
		a.recycle();
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
	 * Set auto complete adapter for given folder.
	 * 
	 * @param folder
	 *            path to folder
	 */
	private void createAutoCompleteForFolder(String folder) {
		File [] files = new File(folder).listFiles(mFileFilter);
		String [] complete = null;
		
		if (files != null && files.length != 0) {
			Arrays.sort(files, mFileComparator);
			
			complete = new String [files.length];
			
			for (int i = 0; i < files.length; i++) {
				complete[i] = files[i].getName();
				
				if (files[i].isDirectory()) {
					complete[i] += "/";
				}
			}
		}
		
		if (complete == null) {
			super.setAdapter(mEmptyAdapter);
		} else {
			super.setAdapter(new ArrayAdapter<String>(getContext(), DROPDOWN_LAYOUT, complete));
		}
	}
}
