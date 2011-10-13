package de.viktorreiser.toolbox.preference;

import java.util.LinkedList;
import java.util.List;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.ListPreference;
import android.util.AttributeSet;
import de.viktorreiser.toolbox.R;
import de.viktorreiser.toolbox.util.StringUtils;

/**
 * A {@link ListPreference} that allows multiple selection.<br>
 * <br>
 * Following custom XML attributes are available:<br>
 * <ul>
 * <li>{@code valueSeparator="string"} - {@link #setSeparator(String)}</li>
 * </ul>
 * <br>
 * <i>Depends on</i>: {@link StringUtils}, {@code res/values/attrs.xml}
 * 
 * @author <a href="http://blog.350nice.com/wp/archives/240">Original source</a> | Viktor Reiser
 *         &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class MultiSelectListPreference extends ListPreference {
	
	// PUBLIC =====================================================================================
	
	/**
	 * Default separator between stored values.<br>
	 * <br>
	 * Unique enough to never match any existing value.
	 */
	public static final String SEPARATOR = "←ŧø→";
	
	// PRIVATE ====================================================================================
	
	/** Selected list items. */
	private boolean [] mSelected;
	
	/** Separator string. */
	private String mSeparator;
	
	// PUBLIC =====================================================================================
	
	/**
	 * Get (currently) selected entries.<br>
	 * <br>
	 * Modifications on this array are made on internal selection too (so you can change stored
	 * value in an {@link android.preference.OnPreferenceChangeListener}).
	 * 
	 * @return (currently) selected entries
	 */
	public boolean [] getSelected() {
		return mSelected;
	}
	
	/**
	 * Set separator between selected values which will be joined and saved to preferences.
	 * 
	 * @param separator
	 *            if {@code null} or empty {@link #SEPARATOR} will be used
	 */
	public void setSeparator(String separator) {
		mSeparator = SEPARATOR;
		
		if (separator != null && !separator.trim().equals("")) {
			mSeparator = separator;
		}
	}
	
	// OVERRIDDEN =================================================================================
	
	public MultiSelectListPreference(Context context) {
		this(context, null);
	}
	
	public MultiSelectListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mSelected = new boolean [getEntries().length];
		mSeparator = SEPARATOR;
		
		if (attrs == null) {
			return;
		}
		
		TypedArray a = getContext().obtainStyledAttributes(
				attrs, R.styleable.MultiSelectListPreference);
		setSeparator(a.getString(R.styleable.MultiSelectListPreference_valueSeparator));
		a.recycle();
	}
	
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public void setEntries(CharSequence [] entries) {
		super.setEntries(entries);
		mSelected = new boolean [entries.length];
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	protected void onPrepareDialogBuilder(Builder builder) {
		CharSequence [] entries = getEntries();
		CharSequence [] entryValues = getEntryValues();
		
		if (entries == null || entryValues == null || entries.length != entryValues.length) {
			throw new IllegalStateException(
					"ListPreference requires an entries array and an " +
							"entryValues array which are both the same length");
		}
		
		restoreCheckedEntries();
		
		builder.setMultiChoiceItems(entries, mSelected,
				new DialogInterface.OnMultiChoiceClickListener() {
					public void onClick(DialogInterface dialog, int which, boolean val) {
						mSelected[which] = val;
					}
				});
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		CharSequence [] entryValues = getEntryValues();
		
		if (positiveResult && entryValues != null) {
			List<CharSequence> values = new LinkedList<CharSequence>();
			
			for (int i = 0; i < entryValues.length; i++) {
				if (mSelected[i]) {
					values.add(entryValues[i]);
				}
			}
			
			if (callChangeListener(StringUtils.join(mSeparator, values.toArray()))) {
				values.clear();
				
				for (int i = 0; i < entryValues.length; i++) {
					if (mSelected[i]) {
						values.add(entryValues[i]);
					}
				}
				
				setValue(StringUtils.join(SEPARATOR, values.toArray()));
			}
		}
	}
	
	// PRIVATE ====================================================================================
	
	/**
	 * Read and parse current values from persisted value and set list selection.
	 */
	private void restoreCheckedEntries() {
		CharSequence [] entryValues = getEntryValues();
		
		String value = getValue();
		String [] values = new String [0];
		
		if (value != null) {
			values = StringUtils.split(mSeparator, value);
		}
		
		for (int j = 0; j < values.length; j++) {
			String v = values[j].trim();
			
			for (int i = 0; i < entryValues.length; i++) {
				CharSequence entry = entryValues[i];
				
				if (entry.equals(v)) {
					mSelected[i] = true;
					break;
				}
			}
		}
	}
}
