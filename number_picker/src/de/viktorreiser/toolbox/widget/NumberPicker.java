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
 * This (useful) class is available on every android platform but not for public access.<br>
 * <br>
 * So this is the original class pulled from android source code and improved for public use.<br>
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
	
	protected static final int DEFAULT_START = 0;
	protected static final int DEFAULT_END = 200;
	
	
	private final Handler mHandler;
	private final Runnable mRunnable = new Runnable() {
		@Override
		public void run() {
			if (mIncrement) {
				changeCurrent(mCurrent + 1);
				mHandler.postDelayed(this, mSpeed);
			} else if (mDecrement) {
				changeCurrent(mCurrent - 1);
				mHandler.postDelayed(this, mSpeed);
			}
		}
	};
	
	private final EditText mText;
	private final InputFilter mNumberInputFilter;
	
	private String [] mDisplayedValues;
	protected int mStart;
	protected int mEnd;
	protected int mCurrent;
	protected int mPrevious;
	private OnChangedListener mListener;
	private Formatter mFormatter;
	private long mSpeed = 300;
	
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
		 * Number picker value change.
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
		 * Format number to display
		 * 
		 * @param value
		 *            value to display
		 * 
		 * @return string which represents the given number
		 */
		String toString(int value);
	}
	
	/**
	 * Use a custom NumberPicker formatting callback to use two-digit minutes strings like "01".<br>
	 * <br>
	 * Keeping a static formatter is the most efficient way to do this. It avoids creating temporary
	 * objects on every call to format().
	 */
	public static final Formatter TWO_DIGIT_FORMATTER = new NumberPicker.Formatter() {
		final StringBuilder mBuilder = new StringBuilder();
		final java.util.Formatter mFmt = new java.util.Formatter(mBuilder);
		final Object [] mArgs = new Object [1];
		
		@Override
		public String toString(int value) {
			mArgs[0] = value;
			mBuilder.delete(0, mBuilder.length());
			mFmt.format("%02d", mArgs);
			return mFmt.toString();
		}
	};
	
	
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
		
		if (attrs != null && !isInEditMode()) {
			// this crashes in edit mode (?!)
			TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.NumberPicker);
			
			vertical = !"0".equals(a.getString(R.styleable.NumberPicker_android_orientation));
			mStart = a.getInt(R.styleable.NumberPicker_rangeStart, DEFAULT_START);
			mEnd = a.getInt(R.styleable.NumberPicker_rangeEnd, DEFAULT_END);
			mSpeed = a.getInt(R.styleable.NumberPicker_speed, (int) mSpeed);
			mCurrent = a.getInt(R.styleable.NumberPicker_current, mCurrent);
			
			if (mStart > mEnd) {
				throw new IllegalArgumentException("rangetStart should be less equal rangeEnd");
			} else if (mSpeed < 1) {
				throw new IllegalArgumentException("speed should be greater zero");
			} else if (mCurrent < mStart) {
				mCurrent = mStart;
			} else if (mCurrent > mEnd) {
				mCurrent = mEnd;
			}
			
			a.recycle();
		}
		
		if (isInEditMode()) {
			// fix orientation attribute for editor
			vertical = "horizontal".equals(attrs.getAttributeValue(
					"http://schemas.android.com/apk/res/android", "orientation"));
		}
		
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		if (vertical) {
			setOrientation(VERTICAL);
			inflater.inflate(R.layout.number_picker_vertical, this, true);
		} else {
			setOrientation(HORIZONTAL);
			inflater.inflate(R.layout.number_picker_horizontal, this, true);
		}
		
		mHandler = new Handler();
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
		
		setCurrent(mCurrent);
	}
	
	/**
	 * Set value change listener.
	 * 
	 * @param listener
	 * 
	 * @see OnChangedListener
	 */
	public void setOnChangeListener(OnChangedListener listener) {
		mListener = listener;
	}
	
	/**
	 * Set value formatter.
	 * 
	 * @param formatter
	 * 
	 * @see Formatter
	 */
	public void setFormatter(Formatter formatter) {
		mFormatter = formatter;
	}
	
	/**
	 * Set the range of numbers allowed for the number picker.<br>
	 * <br>
	 * The current value will be automatically set to the start.<br>
	 * Default range is {@value #DEFAULT_START} to {@value #DEFAULT_END}.
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
		mCurrent = start;
		updateView();
	}
	
	/**
	 * Set the range of numbers allowed for the number picker.<br>
	 * <br>
	 * Default range is {@value #DEFAULT_START} to {@value #DEFAULT_END}.
	 * 
	 * @param start
	 *            the start of the range (inclusive)
	 * @param end
	 *            the end of the range (inclusive)
	 * @param current
	 *            current value to set
	 * 
	 * @throws IllegalArgumentException
	 *             {@code start > end}, {@code current < start} or {@code current > end}
	 */
	public void setRange(int start, int end, int current) {
		mStart = start;
		mEnd = end;
		mCurrent = current;
		updateView();
	}
	
	/**
	 * Set the range of numbers allowed for the number picker.<br>
	 * <br>
	 * The current value will be automatically set to the start.
	 * 
	 * @param start
	 *            the start of the range (inclusive)
	 * @param end
	 *            the end of the range (inclusive)
	 * @param displayedValues
	 *            the values displayed to the user ({@code null} will reset displayed values and
	 *            display numbers again)
	 * 
	 * @throws IllegalArgumentException
	 *             {@code start > end || start < -1 || end >= displayedValues.length}
	 */
	public void setRange(int start, int end, String [] displayedValues) {
		if (displayedValues == null) {
			mDisplayedValues = displayedValues;
			setRange(start, end);
		} else {
			if (start > end || start < -1 || end >= displayedValues.length) {
				throw new IllegalArgumentException();
			}
			
			mDisplayedValues = displayedValues;
			mStart = start;
			mEnd = end;
			mCurrent = start;
			updateView();
		}
	}
	
	/**
	 * Set the range of numbers allowed for the number picker.
	 * 
	 * @param start
	 *            the start of the range (inclusive)
	 * @param end
	 *            the end of the range (inclusive)
	 * @param current
	 *            current value to set
	 * @param displayedValues
	 *            the values displayed to the user ({@code null} will reset displayed values and
	 *            display numbers again)
	 * 
	 * @throws IllegalArgumentException
	 *             {@code start > end || current < start || current > end || start < -1 ||}
	 *             {@code end >= displayedValues.length}
	 */
	public void setRange(int start, int end, int current, String [] displayedValues) {
		if (displayedValues == null) {
			mDisplayedValues = displayedValues;
			setRange(start, end, current);
		} else {
			if (start > end || current < start || current > end || start < -1
					|| end >= displayedValues.length) {
				throw new IllegalArgumentException();
			}
			
			mDisplayedValues = displayedValues;
			mStart = start;
			mEnd = end;
			mCurrent = start;
			updateView();
		}
	}
	
	/**
	 * Set current value.
	 * 
	 * @param current
	 *            current value to set
	 * 
	 * @throws IllegalArgumentException
	 *             {@code current < start || current > end}
	 */
	public void setCurrent(int current) {
		if (current < mStart || current > mEnd) {
			throw new IllegalArgumentException();
		}
		
		mCurrent = current;
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
	 * Default is 300ms (per value).
	 * 
	 * @param speed
	 *            scroll speed in milliseconds
	 */
	public void setSpeed(long speed) {
		mSpeed = speed;
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
	 * <i>Overridden for internal use!</i>
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
	
	// PRIVATE ====================================================================================
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public void onClick(View v) {
		validateInput(mText);
		if (!mText.hasFocus())
			mText.requestFocus();
		
		// now perform the increment/decrement
		if (R.id.increment == v.getId()) {
			changeCurrent(mCurrent + 1);
		} else if (R.id.decrement == v.getId()) {
			changeCurrent(mCurrent - 1);
		}
	}
	
	private String formatNumber(int value) {
		return (mFormatter != null)
				? mFormatter.toString(value)
				: String.valueOf(value);
	}
	
	private void changeCurrent(int current) {
		
		// Wrap around the values if we go past the start or end
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
		/*
		 * If we don't have displayed values then use the current number else find the correct value
		 * in the displayed values for the current number.
		 */
		if (mDisplayedValues == null) {
			mText.setText(formatNumber(mCurrent));
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
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		/*
		 * When focus is lost check that the text field has valid values.
		 */
		if (!hasFocus) {
			validateInput(v);
		}
	}
	
	private void validateInput(View v) {
		String str = String.valueOf(((TextView) v).getText());
		if ("".equals(str)) {
			
			// Restore to the old value as we don't allow empty values
			updateView();
		} else {
			
			// Check the new value and ensure it's in range
			validateCurrentView(str);
		}
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public boolean onLongClick(View v) {
		
		/*
		 * The text view may still have focus so clear it's focus which will trigger the on focus
		 * changed and any typed values to be pulled.
		 */
		mText.clearFocus();
		
		if (R.id.increment == v.getId()) {
			mIncrement = true;
			mHandler.post(mRunnable);
		} else if (R.id.decrement == v.getId()) {
			mDecrement = true;
			mHandler.post(mRunnable);
		}
		return true;
	}
	
	/**
	 * Cancel increment handler (when increment button clicked).
	 */
	public void cancelIncrement() {
		mIncrement = false;
	}
	
	/**
	 * Cancel decrement handler (when decrement button clicked).
	 */
	public void cancelDecrement() {
		mDecrement = false;
	}
	
	private static final char [] DIGIT_CHARACTERS = new char [] {
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
	};
	
	private NumberPickerButton mIncrementButton;
	private NumberPickerButton mDecrementButton;
	
	private class NumberPickerInputFilter implements InputFilter {
		@Override
		public CharSequence filter(CharSequence source, int start, int end,
				Spanned dest, int dstart, int dend) {
			if (mDisplayedValues == null) {
				return mNumberInputFilter.filter(source, start, end, dest, dstart, dend);
			}
			CharSequence filtered = String.valueOf(source.subSequence(start, end));
			String result = String.valueOf(dest.subSequence(0, dstart))
					+ filtered
					+ dest.subSequence(dend, dest.length());
			String str = String.valueOf(result).toLowerCase();
			for (String val : mDisplayedValues) {
				val = val.toLowerCase();
				if (val.startsWith(str)) {
					return filtered;
				}
			}
			return "";
		}
	}
	
	private class NumberRangeKeyListener extends NumberKeyListener {
		
		// This doesn't allow for range limits when controlled by a
		// soft input method!
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
					+ filtered
					+ dest.subSequence(dend, dest.length());
			
			if ("".equals(result)) {
				return result;
			}
			int val = getSelectedPos(result);
			
			/*
			 * Ensure the user can't type in a value greater than the max allowed. We have to allow
			 * less than min as the user might want to delete some numbers and then type a new
			 * number.
			 */
			if (val > mEnd) {
				return "";
			} else {
				return filtered;
			}
		}
	}
	
	private int getSelectedPos(String str) {
		if (mDisplayedValues == null) {
			return Integer.parseInt(str);
		} else {
			for (int i = 0; i < mDisplayedValues.length; i++) {
				
				/* Don't force the user to type in jan when ja will do */
				str = str.toLowerCase();
				if (mDisplayedValues[i].toLowerCase().startsWith(str)) {
					return mStart + i;
				}
			}
			
			/*
			 * The user might have typed in a number into the month field i.e. 10 instead of OCT so
			 * support that too.
			 */
			try {
				return Integer.parseInt(str);
			} catch (NumberFormatException e) {
				
				/* Ignore as if it's not a number we don't care */
			}
		}
		return mStart;
	}
}
