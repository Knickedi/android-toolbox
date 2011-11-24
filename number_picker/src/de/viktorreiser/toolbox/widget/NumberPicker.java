/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Extended and improved by Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;.
 */

package de.viktorreiser.toolbox.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.method.NumberKeyListener;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.viktorreiser.toolbox.R;

/**
 * Number picker port from android source code (<b>Beta +</b>).<br>
 * <br>
 * This (useful) class is available on every android platform but not for public access.<br>
 * So this is the original class pulled from android source code and improved for public use.<br>
 * It's a view which contains a number field which can be increased and decreased by button press.<br>
 * <br>
 * Following custom XML attributes are available:<br>
 * <ul>
 * <li>{@code android:orientation} with {@code horizontal} can be used to use a alternative layout
 * (see {@link #setOrientation(int)}).</li>
 * <li>{@code rangeStart} and {@code rangeEnd} should be used at the same time (see
 * {@link #setRange(int, int)}). If {@code current} is not set {@code rangeStart} will be used.</li>
 * <li>{@code current} to set current value (see {@link #setCurrent(int)}).</li>
 * <li>{@code speed} to set scroll speed (see {@link #setSpeed(long)}).</li>
 * </ul>
 * 
 * @author Google | Viktor Reiser &lt;<a
 *         href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class NumberPicker extends LinearLayout implements OnClickListener,
		OnFocusChangeListener, OnLongClickListener {
	
	// PRIVATE ====================================================================================
	
	private static final int DEFAULT_START = 0;
	private static final int DEFAULT_END = 100;
	
	private static final char [] DIGIT_CHARACTERS = new char [] {
			'-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
	};
	
	private final Handler mChangeHandler;
	private final Runnable mChangeRunnable = new Runnable() {
		@Override
		public void run() {
			if (mIncrement) {
				changeCurrent(mCurrent + 1);
				mChangeHandler.postDelayed(this, mSpeed);
			} else if (mDecrement) {
				changeCurrent(mCurrent - 1);
				mChangeHandler.postDelayed(this, mSpeed);
			}
		}
	};
	
	private NumberPickerButton mIncrementButton;
	private NumberPickerButton mDecrementButton;
	
	private final EditText mText;
	private final InputFilter mNumberInputFilter;
	
	private String [] mDisplayedValues;
	private int mStart;
	private int mEnd;
	private int mCurrent;
	private int mPrevious;
	private OnChangedListener mListener;
	private Formatter mFormatter;
	private long mSpeed = 200;
	
	private boolean mIncrement;
	private boolean mDecrement;
	
	// PUBLIC =====================================================================================
	
	/**
	 * Value change listener.
	 * 
	 * @author Google | Viktor Reiser &lt;<a
	 *         href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
	 */
	public interface OnChangedListener {
		
		/**
		 * Number picker value changed.
		 * 
		 * @param picker
		 *            number picker
		 * @param oldVal
		 *            old value
		 * @param newVal
		 *            new value
		 */
		void onChanged(NumberPicker picker, int oldVal, int newVal);
	}
	
	/**
	 * Formatter for displayed values.
	 * 
	 * @author Google | Viktor Reiser &lt;<a
	 *         href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
	 */
	public interface Formatter {
		
		/**
		 * Format number for UI.<br>
		 * <br>
		 * You you write a formatter which is displaying the numbers from {@code 1-1000} as
		 * {@code 0001, 0324}. You could even define even use custom strings which represents the
		 * number.
		 * 
		 * @param value
		 *            value to display
		 * 
		 * @return string which represents the given number
		 */
		String toString(int value);
	}
	
	
	public NumberPicker(Context context) {
		this(context, null);
	}
	
	public NumberPicker(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	
	public NumberPicker(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs);
		
		mStart = DEFAULT_START;
		mEnd = DEFAULT_END;
		mCurrent = mStart;
		
		boolean vertical = true;
		int displayedValues = 0;
		
		if (attrs != null && !isInEditMode()) {
			// this crashes in edit mode (?!)
			TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.NumberPicker);
			
			mStart = a.getInt(R.styleable.NumberPicker_rangeStart, DEFAULT_START);
			mEnd = a.getInt(R.styleable.NumberPicker_rangeEnd, DEFAULT_END);
			mSpeed = a.getInt(R.styleable.NumberPicker_speed, (int) mSpeed);
			mCurrent = a.getInt(R.styleable.NumberPicker_current, mCurrent);
			
			String orientation = a.getString(R.styleable.NumberPicker_android_orientation);
			displayedValues = a.getResourceId(R.styleable.NumberPicker_displayedValues, 0);
			
			if (orientation != null) {
				vertical = !"0".equals(orientation);
			}
			
			a.recycle();
		} else if (attrs != null && isInEditMode()) {
			// fix orientation attribute for editor
			String orientation = attrs.getAttributeValue(
					"http://schemas.android.com/apk/res/android", "orientation");
			
			if (orientation != null) {
				vertical = !"horizontal".equals(orientation);
			}
		}
		
		// set a wrong orientation so our own orientation method will perform changes
		super.setOrientation(!vertical ? VERTICAL : HORIZONTAL);
		setOrientation(vertical ? VERTICAL : HORIZONTAL);
		
		mChangeHandler = new Handler();
		InputFilter inputFilter = new NumberPickerInputFilter();
		mNumberInputFilter = new NumberRangeKeyListener();
		mIncrementButton = (NumberPickerButton) findViewById(R.id.increment);
		mIncrementButton.setOnClickListener(this);
		mIncrementButton.setOnLongClickListener(this);
		mIncrementButton.setNumberPicker(this);
		mDecrementButton = (NumberPickerButton) findViewById(R.id.decrement);
		mDecrementButton.setOnClickListener(this);
		mDecrementButton.setOnLongClickListener(this);
		mDecrementButton.setNumberPicker(this);
		
		mText = (EditText) findViewById(R.id.timepicker_input);
		mText.setOnFocusChangeListener(this);
		mText.setFilters(new InputFilter [] {inputFilter});
		mText.setRawInputType(InputType.TYPE_CLASS_NUMBER);
		LayoutParams params = (LayoutParams) mText.getLayoutParams();
		params.weight = 1;
		mText.setLayoutParams(params);
		
		if (!isEnabled()) {
			setEnabled(false);
		}
		
		if (displayedValues != 0) {
			setDisplayedRange(mStart, displayedValues);
		} else {
			setRange(mStart, mEnd);
		}
	}
	
	/**
	 * Set value change listener.
	 * 
	 * @param listener
	 *            value change listener
	 */
	public void setOnChangeListener(OnChangedListener listener) {
		mListener = listener;
	}
	
	/**
	 * Set value formatter.<br>
	 * <br>
	 * The formatter has no effect if you use {@link #setRange(int, int, String[])} or
	 * {@link #setRange(int, int, int, String[])} as range definition. The default formatter will
	 * display the current value as it is without modifying it.
	 * 
	 * @param formatter
	 *            formatter which creates a representation for a certain value
	 */
	public void setFormatter(Formatter formatter) {
		mFormatter = formatter;
	}
	
	/**
	 * Set the range.<br>
	 * <br>
	 * The current value will be set to start or end when it's too small or too big.
	 * 
	 * @param start
	 *            the start of the range (inclusive)
	 * @param end
	 *            the end of the range (inclusive)
	 * 
	 * @throws IllegalArgumentException
	 *             {@code start > end}
	 */
	public void setRange(int start, int end) {
		mStart = start;
		mEnd = end;
		
		if (mStart > mEnd) {
			throw new IllegalArgumentException("start should be less equal end");
		}
		
		setCurrent(mCurrent);
	}
	
	/**
	 * Set the range with corresponding UI mapped values.
	 * 
	 * @param start
	 *            value of first entry
	 * @param stringArrayResId
	 *            resource ID for string array to display
	 * 
	 * @see #setDisplayedRange(int, String[])
	 */
	public void setDisplayedRange(int start, int stringArrayResId) {
		setDisplayedRange(start, getContext().getResources().getStringArray(stringArrayResId));
	}
	
	/**
	 * Set the range with corresponding UI mapped values.<br>
	 * <br>
	 * E.g. you have an array {@code ("a","b","c")} and a start value of 3 you'll get the values
	 * {@code (3, 4, 5)} internally but the given strings will be displayed to the user.
	 * {@link #setCurrent(int)} will accept also {@code (3, 4, 5)} and not {@code (0, 1, 2)}. You
	 * can iterate day names, month names or anything else like that.
	 * 
	 * @param start
	 *            value of first entry
	 * @param displayedValues
	 *            the values displayed to the user
	 * 
	 * @throws IllegalArgumentException
	 *             {@code displayedValues.length == 0}
	 */
	public void setDisplayedRange(int start, String [] displayedValues) {
		if (displayedValues.length == 0) {
			throw new IllegalArgumentException("displayedValues length is 0");
		} else {
			mDisplayedValues = displayedValues;
			mStart = start;
			mEnd = start + displayedValues.length - 1;
			setCurrent(mCurrent);
		}
	}
	
	/**
	 * Set current value.
	 * 
	 * @param current
	 *            current value to set - start or end will be used if given value is less the start
	 *            or greater then end
	 */
	public void setCurrent(int current) {
		mCurrent = current;
		
		if (mCurrent < mStart) {
			mCurrent = mStart;
		} else if (mCurrent > mEnd) {
			mCurrent = mEnd;
		}
		
		updateView();
	}
	
	/**
	 * Get current value.
	 * 
	 * @return current value
	 */
	public int getCurrent() {
		return mCurrent;
	}
	
	/**
	 * Set Scroll speed of numbers when +/- buttons are long pressed.<br>
	 * <br>
	 * Default is 200ms (per value).
	 * 
	 * @param speed
	 *            scroll speed in milliseconds
	 */
	public void setSpeed(long speed) {
		mSpeed = speed;
	}
	
	// PACKAGE ====================================================================================
	
	/**
	 * Cancel increment handler (when increment button clicked).
	 */
	void cancelIncrement() {
		mIncrement = false;
	}
	
	/**
	 * Cancel decrement handler (when decrement button clicked).
	 */
	void cancelDecrement() {
		mDecrement = false;
	}
	
	/**
	 * This class exists purely to cancel long click events.
	 * 
	 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
	 */
	static class NumberPickerButton extends ImageButton {
		
		// PRIVATE --------------------------------------------------------------------------------
		
		private NumberPicker mNumberPicker;
		
		// PUBLIC ---------------------------------------------------------------------------------
		
		public void setNumberPicker(NumberPicker picker) {
			mNumberPicker = picker;
		}
		
		// OVERRIDDEN -----------------------------------------------------------------------------
		
		public NumberPickerButton(Context context, AttributeSet attrs,
				int defStyle) {
			super(context, attrs, defStyle);
		}
		
		public NumberPickerButton(Context context, AttributeSet attrs) {
			super(context, attrs);
		}
		
		public NumberPickerButton(Context context) {
			super(context);
		}
		
		
		@Override
		public boolean onTouchEvent(MotionEvent event) {
			cancelLongpressIfRequired(event);
			return super.onTouchEvent(event);
		}
		
		@Override
		public boolean onTrackballEvent(MotionEvent event) {
			cancelLongpressIfRequired(event);
			return super.onTrackballEvent(event);
		}
		
		@Override
		public boolean onKeyUp(int keyCode, KeyEvent event) {
			if ((keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
					|| (keyCode == KeyEvent.KEYCODE_ENTER)) {
				cancelLongpress();
			}
			return super.onKeyUp(keyCode, event);
		}
		
		// PRIVATE --------------------------------------------------------------------------------
		
		private void cancelLongpressIfRequired(MotionEvent event) {
			if ((event.getAction() == MotionEvent.ACTION_CANCEL)
					|| (event.getAction() == MotionEvent.ACTION_UP)) {
				cancelLongpress();
			}
		}
		
		private void cancelLongpress() {
			if (R.id.increment == getId()) {
				mNumberPicker.cancelIncrement();
			} else if (R.id.decrement == getId()) {
				mNumberPicker.cancelDecrement();
			}
		}
	}
	
	// OVERRIDDEN =================================================================================
	
	/**
	 * Set the orientation of the number picker.
	 * 
	 * @param orientation
	 *            {@link LinearLayout#VERTICAL} or {@link LinearLayout#HORIZONTAL}
	 */
	@Override
	public void setOrientation(int orientation) {
		if (getOrientation() != orientation) {
			super.removeAllViews();
			super.setOrientation(orientation);
			
			LayoutInflater inflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			
			if (orientation == VERTICAL) {
				inflater.inflate(R.layout.number_picker_vertical, this, true);
			} else if (orientation == HORIZONTAL) {
				inflater.inflate(R.layout.number_picker_horizontal, this, true);
			} else {
				throw new IllegalArgumentException(
						"orientation has to be horizontal or vertical!");
			}
		}
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		mIncrementButton.setEnabled(enabled);
		mDecrementButton.setEnabled(enabled);
		mText.setEnabled(enabled);
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		if (!hasFocus) {
			validateInput(v);
		}
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public boolean onLongClick(View v) {
		if (R.id.increment == v.getId()) {
			mIncrement = true;
			mChangeHandler.post(mChangeRunnable);
		} else if (R.id.decrement == v.getId()) {
			mDecrement = true;
			mChangeHandler.post(mChangeRunnable);
		}
		return true;
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public void onClick(View v) {
		validateInput(mText);
		if (!mText.hasFocus())
			mText.requestFocus();
		
		if (R.id.increment == v.getId()) {
			changeCurrent(mCurrent + 1);
		} else if (R.id.decrement == v.getId()) {
			changeCurrent(mCurrent - 1);
		}
	}
	
	// PRIVATE ====================================================================================
	
	private void changeCurrent(int current) {
		if (current > mEnd) {
			current = mStart;
		} else if (current < mStart) {
			current = mEnd;
		}
		
		mPrevious = mCurrent;
		mCurrent = current;
		
		notifyChange();
		updateView();
	}
	
	private void notifyChange() {
		if (mListener != null) {
			mListener.onChanged(this, mPrevious, mCurrent);
		}
	}
	
	private void updateView() {
		if (mDisplayedValues == null) {
			mText.setText(mFormatter != null
					? mFormatter.toString(mCurrent) : String.valueOf(mCurrent));
		} else {
			mText.setText(mDisplayedValues[mCurrent - mStart]);
		}
		mText.setSelection(mText.getText().length());
	}
	
	private void validateCurrentView(CharSequence str) {
		int val = getSelectedPos(str.toString());
		
		if ((val >= mStart) && (val <= mEnd)) {
			if (mCurrent != val) {
				mPrevious = mCurrent;
				mCurrent = val;
				notifyChange();
			}
		}
		
		updateView();
	}
	
	private int getSelectedPos(String str) {
		if (mDisplayedValues == null) {
			if (mFormatter != null) {
				return mCurrent;
			} else {
				try {
					return Integer.parseInt(str);
				} catch (Exception e) {
				}
			}
		} else {
			for (int i = 0; i < mDisplayedValues.length; i++) {
				str = str.toLowerCase();
				if (mDisplayedValues[i].toLowerCase().startsWith(str)) {
					return mStart + i;
				}
			}
			
			try {
				return Integer.parseInt(str);
			} catch (NumberFormatException e) {
			}
		}
		
		return mCurrent;
	}
	
	private void validateInput(View v) {
		String str = String.valueOf(((TextView) v).getText());
		
		if ("".equals(str)) {
			updateView();
		} else {
			validateCurrentView(str);
		}
	}
	
	
	private class NumberPickerInputFilter implements InputFilter {
		@Override
		public CharSequence filter(CharSequence source, int start, int end,
				Spanned dest, int dstart, int dend) {
			if (mDisplayedValues == null) {
				if (mFormatter != null) {
					return null;
				} else {
					return mNumberInputFilter.filter(source, start, end, dest, dstart, dend);
				}
			}
			
			CharSequence filtered = String.valueOf(source.subSequence(start, end));
			String result = String.valueOf(dest.subSequence(0, dstart))
					+ filtered + dest.subSequence(dend, dest.length());
			String str = String.valueOf(result).toLowerCase();
			
			for (String val : mDisplayedValues) {
				if (val.toLowerCase().startsWith(str)) {
					return filtered;
				}
			}
			return "";
		}
	}
	
	private class NumberRangeKeyListener extends NumberKeyListener {
		
		@Override
		public int getInputType() {
			return InputType.TYPE_CLASS_NUMBER;
		}
		
		@Override
		protected char [] getAcceptedChars() {
			return DIGIT_CHARACTERS;
		}
		
		@Override
		public CharSequence filter(CharSequence source, int start, int end,
				Spanned dest, int dstart, int dend) {
			CharSequence filtered = super.filter(source, start, end, dest, dstart, dend);
			
			if (filtered == null) {
				filtered = source.subSequence(start, end);
			}
			
			String result = String.valueOf(dest.subSequence(0, dstart))
					+ filtered + dest.subSequence(dend, dest.length());
			
			if ("".equals(result)) {
				return result;
			} else if ("-".equals(result)) {
				return mStart < 0 ? null : "";
			}
			
			int val = getSelectedPos(result);
			
			return mEnd >= -1 && val > mEnd || mStart <= 1 && val < mStart ? "" : filtered;
		}
	}
}
