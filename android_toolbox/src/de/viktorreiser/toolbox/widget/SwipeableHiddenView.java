package de.viktorreiser.toolbox.widget;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ListView;
import de.viktorreiser.toolbox.widget.SwipeableHiddenView.HiddenViewSetup.SwipeDirection;

/**
 * Swipeable view for {@link SwipeableListView} which hides a view behind an item.<br>
 * <br>
 * Basically this view implements a swipeable list view item and provides a API to hide another view
 * behind it. The user can access the hidden view by swiping the list item to the left or right
 * side. Actually thats all but nevertheless pretty powerful. All you have to do is to pass an
 * implementation of {@link HiddenViewSetup} to {@link #setHiddenViewSetup(HiddenViewSetup)}.<br>
 * <br>
 * This view itself is a view container which takes <b>exactly one</b> child view and also expect
 * this to be set on first use (or it will throw an exception).<br>
 * <br>
 * <b>Layout behavior</b>:<br>
 * The view itself and the child have to define width FILL_PARENT. Further the view does not support
 * padding and the child does not support margin or gravity. Use a view container as child to bypass
 * those restrictions.<br>
 * If the given hidden view is smaller than the child then it will be stretched in height. The same
 * applies for a smaller child. The first case won't affect layout of list item. The second case
 * will stretch the item vertically when a swipe begins, on end (when hidden view is not visible
 * anymore) it will adjust height again so item matches the actual height of the child again. This
 * might look strange but anything else would look even uglier (like strange clipping). This view is
 * also able display dynamic layout changes of the hidden view.<br>
 * <br>
 * <b>Example</b> (this is not a pattern but a simple example how the use might look like):
 * 
 * <pre>
 * public class MyActivity extends {@link Activity} {
 * 
 * 	&#64;Override
 * 	public void onCreate(Bundle bundle) {
 * 		super.onCreate(bundle)
 *  
 * 		final MySetupImplementation setup = new MySetupImplementation(this);
 *  
 * 		(({@link SwipeableListView}) findViewById(R.id.swipeable_list))
 * 			.setAdapter(new ArrayAdapter&lt;String&gt;(
 * 				this,
 * 				R.id.layout_swipeable_hidden_view_with_nested_text_view,
 * 				R.id.nested_text_view_id,
 * 				new String [] {"1", "2", "3", "4", "5", "6", "7"}) {
 * 
 * 			&#64;Override
 * 			public View getView(int position, View convertView, ViewGroup parent) {
 * 				convertView = super.getView(position, convertView, parent);
 * 				{@link SwipeableHiddenView} v = ({@link SwipeableHiddenView}) convertView;
 *  
 * 				if (!v.{@link #isHiddenViewSetupSet()}) {
 * 					v.setHiddenViewSetup(setup);
 * 				}
 * 			}
 * 		});
 * 	}
 * }
 * </pre>
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class SwipeableHiddenView extends FrameLayout implements SwipeableListItem {
	
	// PRIVATE ====================================================================================
	
	/** Setup for swipeable view. */
	private HiddenViewSetup mData;
	
	/** Current item position given by {@link #onViewSwipe}. */
	private int mCurrentPosition = HiddenViewSetup.INVALID_POSITION;
	
	/** Current list view given by {@link #onViewSwipe}. */
	private ListView mCurrentListView;
	
	/** Offset in pixel given by last {@link #onViewSwipe} event. */
	private int mLastOffset = 0;
	
	private float mStartOffset;
	private boolean mStartDirection;
	
	/**
	 * Overlay view offset relative to its parent swipeable view (-1 left outside, 0 completely
	 * visible, 1 right outside).
	 */
	private float mOffset = 0f;
	
	/** {@code true} if needed offset reached for {@link #onViewSwipe} to start. */
	private boolean mStarted = false;
	
	
	/**
	 * Cached bitmap of hidden view.<br>
	 * <br>
	 * Because hidden view given by setup can be bound to other swipeable views we need to cache its
	 * last visible state for performing a swipe close animation.
	 */
	private View mHiddenViewCache;
	
	/** Hidden view given by setup ({@code null} if currently bound to another swipeable view). */
	private View mHiddenView;
	
	/** Child view which displays the content of the list item. */
	private View mOverlayView;
	
	
	/** Handler which will be used to post animation events. */
	private Handler mAnimationHandler = new Handler();
	
	/** {@code true} as long the swipeable view is animating. */
	private boolean mAnimating = false;
	
	/**
	 * Flag for animation direction.<br>
	 * <br>
	 * {@code true} means that animation will uncover hidden view and {@code false} will cover it.
	 */
	private boolean mAnimateForward = true;
	
	/**
	 * Animation calculation which changes values, invalidates view and calls itself until it
	 * finishes.
	 */
	private Runnable mAnimationStep;
	
	/** Last time in nanoseconds of last animation step (for smooth calculations). */
	private long mAnimationStepTime;
	
	
	private int mStartX;
	private boolean mLongClicked = false;
	
	private View mMotionTarget = null;
	
	// PUBLIC =====================================================================================
	
	/**
	 * Abstract configuration for {@link SwipeableHiddenView#setHiddenViewSetup(HiddenViewSetup)}.<br>
	 * <br>
	 * This is the base setup for any use of a hidden view swipeable.<br>
	 * The main task is to provide a implementation for {@link #getHiddenView()}. You can use the
	 * same setup object for multiple {@link SwipeableHiddenView}'s and so a single hidden view for
	 * all instances. The view will be shared between them since hidden view won't ever be visible
	 * in more than one {@link SwipeableHiddenView}.<br>
	 * <br>
	 * When hidden view triggers an action you can use {@link #getCurrentSwipeableHiddenView()},
	 * {@link #getCurrentListView()} and {@link #getCurrentPosition()} to determinate on which
	 * {@link SwipeableHiddenView}, list view and list item the action is performed.<br>
	 * When action needs to hide the hidden view again you can call {@link #closeHiddenView()}.<br>
	 * <br>
	 * Some of the configurations are locked when the setup is attached to a swipeable hidden view.
	 * This is always documented on the respective configuration method.
	 * 
	 * @see SwipeableHiddenView
	 * 
	 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
	 */
	public static abstract class HiddenViewSetup extends SwipeableSetup {
		
		// PRIVATE --------------------------------------------------------------------------------
		
		/** Set by {@link SwipeableHiddenView} when it binds {@link #getHiddenView()}. */
		private SwipeableHiddenView currentSwipeableHiddenView;
		
		/** Used by {@link #getCurrentPosition()}. */
		protected static final int INVALID_POSITION = -1;
		
		/** Interpolator for swipe open animation see {@link #setAnimation(Interpolator)}). */
		protected Interpolator openAnimation = new LinearInterpolator();
		
		/** Interpolator for swipe open animation see {@link #setAnimation(Interpolator)}). */
		protected Interpolator closeAnimation = openAnimation;
		
		/** Interrupt offset see {@link #setAnimationInteruptionOffset(float)}. */
		protected float interruptOffset = 1f;
		
		/** {@link #setSwipeDirection(int)} (default {@code DIRECTION_BOTH}). */
		protected SwipeDirection swipeDirection = SwipeDirection.BOTH;
		
		// PUBLIC ---------------------------------------------------------------------------------
		
		/**
		 * See {@link #setSwipeDirection(SwipeDirection)}.
		 * 
		 * @author Viktor Reiser &lt;<a
		 *         href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
		 */
		public enum SwipeDirection {
			BOTH, LEFT, RIGHT
		}
		
		/**
		 * Get current {@link SwipeableHiddenView} which is operating on the hidden view.
		 * 
		 * @see HiddenViewSetup
		 * @see SwipeableHiddenView
		 * 
		 * @return current {@link SwipeableHiddenView} or {@code null} if no swipeable view is
		 *         operating on the hidden view right now
		 */
		public final SwipeableHiddenView getCurrentSwipeableHiddenView() {
			return currentSwipeableHiddenView;
		}
		
		/**
		 * Get current item position of {@link SwipeableHiddenView} which is operating on the hidden
		 * view.
		 * 
		 * @see HiddenViewSetup
		 * @see SwipeableHiddenView
		 * 
		 * @return current item position
		 */
		public final int getCurrentPosition() {
			if (currentSwipeableHiddenView != null) {
				return currentSwipeableHiddenView.mCurrentPosition;
			} else {
				return INVALID_POSITION;
			}
		}
		
		/**
		 * Get current list view of {@link SwipeableHiddenView} which is operating on the hidden
		 * view.
		 * 
		 * @see HiddenViewSetup
		 * @see SwipeableHiddenView
		 * 
		 * @return current item position
		 */
		public final ListView getCurrentListView() {
			if (currentSwipeableHiddenView != null) {
				return currentSwipeableHiddenView.mCurrentListView;
			}
			
			return null;
		}
		
		/**
		 * Is hidden view completely visible.
		 * 
		 * @see HiddenViewSetup
		 * @see SwipeableHiddenView
		 * 
		 * @return {@code true} if hidden view is completely visible
		 */
		public final boolean isHiddenViewVisible() {
			if (currentSwipeableHiddenView != null) {
				return currentSwipeableHiddenView.isHiddenViewVisible();
			}
			
			return false;
		}
		
		/**
		 * Is hidden view completely covered.
		 * 
		 * @see HiddenViewSetup
		 * @see SwipeableHiddenView
		 * 
		 * @return {@code true} if hidden view is completely covered or the hidden view is not
		 *         managed by a {@link SwipeableHiddenView} right now
		 */
		public final boolean isHiddenViewCovered() {
			if (currentSwipeableHiddenView != null) {
				return currentSwipeableHiddenView.isHiddenViewCovered();
			}
			
			return true;
		}
		
		/**
		 * Request close on {@link SwipeableHiddenView} which is operating on the hidden view.
		 * 
		 * @see HiddenViewSetup
		 * @see SwipeableHiddenView
		 */
		public final void closeHiddenView() {
			if (currentSwipeableHiddenView != null) {
				currentSwipeableHiddenView.onViewSwipe(null, SwipeEvent.CLOSE, 0, 0, null);
			}
		}
		
		/**
		 * Set the interpolation for open swipe animation.<br>
		 * <br>
		 * Default is {@link LinearInterpolator}.<br>
		 * <i>Is locked after the setup is attached to a swipeable view.</i>
		 * 
		 * @param animation
		 *            interpolation to use (should return 0 for 0 and 1 for 1)
		 * 
		 * @see #setAnimation(Interpolator)
		 * @see #setCloseAnimation(Interpolator)
		 */
		public final void setOpenAnimation(Interpolator animation) {
			checkAnimation(animation);
			openAnimation = animation;
		}
		
		/**
		 * Set the interpolation for close swipe animation.<br>
		 * <br>
		 * Default is {@link LinearInterpolator}.<br>
		 * <i>Is locked after the setup is attached to a swipeable view.</i>
		 * 
		 * @param animation
		 *            interpolation to use (should return 0 for 0 and 1 for 1)
		 * 
		 * @see #setAnimation(Interpolator)
		 * @see #setOpenAnimation(Interpolator)
		 */
		public final void setCloseAnimation(Interpolator animation) {
			checkAnimation(animation);
			this.closeAnimation = animation;
		}
		
		/**
		 * Set the interpolation for open and close swipe animation.<br>
		 * <br>
		 * Default is {@link LinearInterpolator}.<br>
		 * <i>Is locked after the setup is attached to a swipeable view.</i><br>
		 * <br>
		 * Using a single animation interpolator instead setting separate open and close
		 * interpolators has the benefit that change of direction has not to be calculated because
		 * interpolator has not to be changed (with separate interpolators a binary search for
		 * current of old interpolator value on the new interpolator when swipe direction changes).<br>
		 * <br>
		 * Example for an single interpolator which calculates quadratic in and out ease:
		 * 
		 * <pre>
		 * &sol;*
		 *  * Function sketch:
		 *  * x >= 0.5 | -0.5(2(x - 1))&sup2; + 1
		 *  *   else   | 0.5(2x)&sup2;
		 *  *
		 *  * 1 |     . '
		 *  *   |    .
		 *  *   |   .
		 *  *   |. '
		 *  *    ---------
		 *  * 0         1
		 *  *&sol;
		 * public float getInterpolation(float v) {
		 * 	if (v >= 0.5) {
		 * 		v -= 1;
		 * 		v *= 2;
		 * 		return -0.5f * v * v + 1f;
		 * 	} else {
		 * 		v *= 2;
		 * 		return 0.5f * v * v;
		 * 	}
		 * }
		 * </pre>
		 * 
		 * Example for separate open and close interpolators which calculates cubic ease:<br>
		 * (The benefit is that the animation doesn't ease at the beginning of a open swipe and
		 * behaves the same with close. This is not the case in the example above because it eases
		 * always at the beginning and ending.)
		 * 
		 * <pre>
		 * &sol;*                                    
		 *  * Function sketch (open):
		 *  * (x - 1)&sup3; + 1
		 *  *
		 *  * 1 |    .---
		 *  *   |  .'
		 *  *   | :
		 *  *   |:
		 *  *    ---------
		 *  * 0         1
		 *  *&sol;
		 * public float getInterpolation(float v) {
		 * 	v -= 1;
		 * 	return v * v * v + 1;
		 * }
		 * 
		 * &sol;*                                    
		 *  * Function sketch (close):
		 *  * x&sup3;
		 *  *
		 *  * 1 |       :
		 *  *   |      :
		 *  *   |    .'
		 *  *   |...-
		 *  *    ---------
		 *  * 0         1
		 *  *&sol;
		 * public float getInterpolation(float v) {
		 * 	return v * v * v;
		 * }
		 * </pre>
		 * 
		 * @param animation
		 *            interpolation to use (should return 0 for 0 and 1 for 1)
		 */
		public final void setAnimation(Interpolator animation) {
			checkAnimation(animation);
			closeAnimation = animation;
			openAnimation = animation;
		}
		
		/**
		 * Offset which is needed to interrupt a running animation (default is {@code 1}).<br>
		 * <br>
		 * Consider that that the given value doesn't relate to the visual effect but to the given
		 * ease function (e.g. if it's linear then the given value is proportional to the visual
		 * effect).
		 * 
		 * @param offset
		 *            {@code 1} means that you can't stop a swipe animation, {@code 0.5} means that
		 *            you can't stop it when it already passed half of the animation, {@code 1}
		 *            means that you can stop a swiped view animation at any time as long it has not
		 *            finished
		 */
		public final void setAnimationInteruptionOffset(float offset) {
			if (offset < 0f || offset > 1f) {
				throw new IllegalArgumentException("0 <= offset <= 1 not true");
			}
			
			interruptOffset = offset / 2;
		}
		
		/**
		 * Set the allowed directions for swiping the view on and off screen (default is
		 * {@link SwipeDirection#BOTH}.<br>
		 * <br>
		 * <i>Is locked after the setup is attached to a swipeable view.</i>
		 * 
		 * @param direction
		 *            left only, right only or both
		 */
		public void setSwipeDirection(SwipeDirection direction) {
			if (direction == null) {
				throw new NullPointerException();
			}
			
			checkChangesLock();
			this.swipeDirection = direction;
		}
		
		// PRIVATE --------------------------------------------------------------------------------
		
		/**
		 * Used to check whether animation return 0 for 0 and 1 for 1.
		 * 
		 * @param animation
		 *            animation to test
		 */
		private void checkAnimation(Interpolator animation) {
			if (Math.round(animation.getInterpolation(0) * 1000000) != 0
					|| Math.round(animation.getInterpolation(1) * 1000000) != 1000000) {
				throw new IllegalArgumentException(
						"Animation should return 0 for 0 and 1 for 1");
			}
		}
		
		// ABSTRACT -------------------------------------------------------------------------------
		
		/**
		 * Get hidden view for {@link SwipeableHiddenView}.
		 * 
		 * @see HiddenViewSetup
		 * @see SwipeableHiddenView
		 * @see #updateHiddenView()
		 * 
		 * @return hidden view
		 */
		public abstract View getHiddenView();
		
		// PRIVATE --------------------------------------------------------------------------------
		
		/**
		 * Update if a new hidden view is set by setup.<br>
		 * <br>
		 * "New hidden view" means a new reference to a view <b>not</b> that the view has been
		 * updated! Use this method in a extending class when this decides to return an absolutely
		 * new view (reference) so it has to be updated in the swipeable view.
		 */
		protected final void updateHiddenView() {
			if (currentSwipeableHiddenView != null) {
				currentSwipeableHiddenView.mHiddenView = null;
				currentSwipeableHiddenView.bindHiddenView();
			}
		}
	}
	
	
	/**
	 * Set hidden view setup for swipeable view.<br>
	 * <br>
	 * It's necessary to set a hidden view setup or an exception will be thrown on first use.<br>
	 * You can't change the setup after it was set.
	 * 
	 * @param setup
	 *            hidden view setup
	 * 
	 * @see SwipeableHiddenView
	 * @see #isHiddenViewVisible()
	 */
	public void setHiddenViewSetup(HiddenViewSetup setup) {
		if (mData != null) {
			throw new IllegalStateException("Setup already set, you can't set another one!");
		}
		
		if (setup == null) {
			throw new NullPointerException();
		}
		
		mData = setup;
	}
	
	/**
	 * Is hidden view setup already set with {@link #setHiddenViewSetup(HiddenViewSetup)}?
	 * 
	 * @return {@code true} if setup already set
	 */
	public boolean isHiddenViewSetupSet() {
		return mData != null;
	}
	
	/**
	 * Is hidden view completely visible?<br>
	 * <br>
	 * {@link #isHiddenViewCovered()} {@code == false} and {@link #isHiddenViewVisible()}
	 * {@code == false} means that the view is currently swiped or it's animating a close or open
	 * swipe.
	 * 
	 * @return {@code true} when hidden view is completely visible
	 * 
	 * @see #isHiddenViewCovered()
	 */
	public boolean isHiddenViewVisible() {
		return Math.round(Math.abs(mOffset * 1000)) == 1000;
	}
	
	/**
	 * Is hidden view completely covered?<br>
	 * <br>
	 * {@link #isHiddenViewCovered()} {@code == false} and {@link #isHiddenViewVisible()}
	 * {@code == false} means that the view is currently swiped or it's animating a close or open
	 * swipe.
	 * 
	 * @return {@code true} when hidden view is completely covered
	 * 
	 * @see #isHiddenViewVisible()
	 */
	public boolean isHiddenViewCovered() {
		return Math.round(mOffset * 1000) == 0;
	}
	
	/**
	 * Close swiped view.<br>
	 * <br>
	 * Use this when the view is detached from list by calling
	 * {@link HiddenViewSetup#setDetachFromList(boolean)}. Otherwise you should use
	 * {@link HiddenViewSetup#closeHiddenView()} and not this method.
	 */
	public void closeHiddenView() {
		if (!isHiddenViewCovered()) {
			onViewSwipe(null, SwipeEvent.CLOSE, 0, -1, null);
		}
	}
	
	// OVERRIDDEN =================================================================================
	
	public SwipeableHiddenView(Context context) {
		super(context);
		initialize(null);
	}
	
	public SwipeableHiddenView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize(attrs);
	}
	
	public SwipeableHiddenView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize(attrs);
	}
	
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public boolean onViewSwipe(ListView listView, SwipeEvent event, int offset, int position,
			SwipeableListItem restoreItem) {
		checkRequirements();
		
		boolean mayInterruptAnimation = mAnimating && Math.abs(mOffset) < mData.interruptOffset
				|| mAnimating && !mAnimateForward;
		
		if (event == SwipeEvent.START) {
			mStarted = mayInterruptAnimation;
		}
		
		if (!mStarted && (!mAnimating || mayInterruptAnimation)
				&& Math.abs(offset) >= mData.startOffset) {
			mStarted = true;
			mStartDirection = Math.abs(mOffset) < 0.5f;
			calculateAnimationDirectionChange(mStartDirection);
			mStartOffset = mOffset;
			mLastOffset = mData.stickyStart ? 0 : offset;
		}
		
		switch (event) {
		case RESTORE:
			if (restoreItem instanceof SwipeableHiddenView) {
				SwipeableHiddenView v = (SwipeableHiddenView) restoreItem;
				
				mAnimateForward = v.mAnimateForward;
				mAnimating = v.mAnimating;
				mAnimationStepTime = v.mAnimationStepTime;
				mStarted = v.mStarted;
				mCurrentListView = v.mCurrentListView;
				mCurrentPosition = v.mCurrentPosition;
				mLastOffset = v.mLastOffset;
				mOffset = v.mOffset;
				
				bindHiddenView();
				requestLayout();
				
				return true;
			} else {
				return false;
			}
			
		case START:
			bindHiddenView();
			
			if (!mAnimating || mayInterruptAnimation) {
				mCurrentPosition = position;
				mCurrentListView = listView;
				mAnimationHandler.removeCallbacks(mAnimationStep);
				mAnimating = false;
				mData.currentSwipeableHiddenView = this;
			}
			break;
		
		case MOVE:
			if (mStarted && offset != mLastOffset) {
				boolean wasCoveredBefore = isHiddenViewCovered();
				boolean wasVisibleBefore = isHiddenViewVisible();
				float lastOffset = mOffset;
				mOffset += 1f * (offset - mLastOffset) / getWidth();
				
				if (mData.swipeDirection == SwipeDirection.LEFT && mOffset > 0
						|| mData.swipeDirection == SwipeDirection.RIGHT && mOffset < 0) {
					mOffset = 0;
				}
				
				if (Math.abs(mOffset) > 1f) {
					mOffset = 1f;
				}
				
				if (wasCoveredBefore || wasVisibleBefore) {
					// manual direction change, calculation avoids sticky start
					mAnimateForward = wasCoveredBefore;
				} else {
					calculateAnimationDirectionChange(lastOffset >= 0 && mOffset < 0
							|| lastOffset < 0 && mOffset >= 0
							|| Math.abs(lastOffset) - Math.abs(mOffset) < 0);
				}
				
				if (wasCoveredBefore != isHiddenViewCovered()) {
					requestLayout();
				}
				
				invalidate();
			}
			break;
		
		case STOP:
			if (mStarted) {
				
				
				if (isHiddenViewCovered()) {
					animate(false);
				} else if (isHiddenViewVisible()) {
					animate(true);
				} else {
					calculateAnimationDirectionChange(mStartDirection);
					float moved = Math.abs(mOffset) - Math.abs(mStartOffset);
					float stop = 1f * mData.stopOffset / getWidth();
					
					animate(Math.abs(mStartOffset) < 0.5f ? moved > stop : moved > -stop);
				}
			}
			break;
		
		case CLOSE:
		case CANCEL:
			animate(false);
			break;
		
		case CLICK:
		case LONG_CLICK:
			if (mData.swipeDirection == SwipeDirection.LEFT) {
				mOffset = -0.05f;
			} else {
				mOffset = 0.05f;
			}
			requestLayout();
			animate(true);
			break;
		}
		
		mLastOffset = offset;
		
		return mStarted;
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public void swipeStateReset() {
		if (mData.currentSwipeableHiddenView == this) {
			mData.currentSwipeableHiddenView = null;
		}
		
		mCurrentPosition = HiddenViewSetup.INVALID_POSITION;
		mCurrentListView = null;
		mStarted = false;
		mOffset = 0f;
		mLastOffset = 0;
		mAnimationHandler.removeCallbacks(mAnimationStep);
		mAnimating = false;
		invalidate();
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public boolean swipeOnClick() {
		checkRequirements();
		return mData.consumeClick;
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public boolean swipeOnLongClick() {
		checkRequirements();
		return mData.consumeLongClick;
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public boolean swipeDoesntHideListSelector() {
		checkRequirements();
		return mData.dontHideSelector;
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		checkRequirements();
		
		LayoutParams p = (LayoutParams) mOverlayView.getLayoutParams();
		p.leftMargin = p.rightMargin = p.topMargin = p.bottomMargin = 0;
		p.gravity = -1;
		p.width = getLayoutParams().width = LayoutParams.FILL_PARENT;
		
		mOverlayView.measure(widthMeasureSpec, 0);
		int height = mOverlayView.getMeasuredHeight();
		
		if (mHiddenView != null && !isHiddenViewCovered()) {
			mHiddenView.measure(widthMeasureSpec, 0);
			
			if (mHiddenView.getMeasuredHeight() > mOverlayView.getMeasuredHeight()) {
				height = mHiddenView.getMeasuredHeight();
				mOverlayView.measure(widthMeasureSpec, height | MeasureSpec.EXACTLY);
			} else {
				mHiddenView.measure(widthMeasureSpec,
						mOverlayView.getMeasuredHeight() | MeasureSpec.EXACTLY);
			}
		} else if (!isHiddenViewCovered() && mHiddenViewCache.getLayoutParams().height != 0) {
			mHiddenViewCache.measure(
					widthMeasureSpec, mHiddenViewCache.getLayoutParams().height);
			
			if (mHiddenViewCache.getMeasuredHeight() > mOverlayView.getMeasuredHeight()) {
				height = mHiddenViewCache.getMeasuredHeight();
			}
		} else {
			mHiddenViewCache.measure(widthMeasureSpec, height | MeasureSpec.EXACTLY);
		}
		
		setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height);
	}
	
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public void onLayout(boolean changed, int left, int top, int right, int bottom) {
		// ignore that fancy stuff with gravity, padding and margins
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			getChildAt(i).layout(0, 0, getMeasuredWidth(), getMeasuredHeight());
		}
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public void dispatchDraw(Canvas canvas) {
		long drawingTime = getDrawingTime();
		
		// L.d("draw  " + mOffset);
		if (!isHiddenViewCovered()) {
			canvas.save();
			
			Interpolator interpolator = mAnimateForward
					? mData.openAnimation : mData.closeAnimation;
			float offset = getWidth() * interpolator.getInterpolation(Math.abs(mOffset));
			
			if (mOffset >= 0) {
				// clip from left edge towards right edge
				canvas.clipRect(0, 0, offset, getHeight());
			} else {
				// clip from right edge towards left edge
				canvas.clipRect(getWidth() - offset, 0, getWidth(), getHeight());
			}
			
			if (mHiddenView == null) {
				// if hidden view is controlled by another swipeable hidden view we draw its
				// cached bitmap (view witch contains it)
				drawChild(canvas, mHiddenViewCache, drawingTime);
			} else {
				drawChild(canvas, mHiddenView, drawingTime);
			}
			
			canvas.restore();
			
			int overlayOffset = Math.round(offset);
			overlayOffset = mOffset >= 0 ? overlayOffset : -overlayOffset;
			
			mOverlayView.offsetLeftAndRight(overlayOffset);
			drawChild(canvas, mOverlayView, drawingTime);
			mOverlayView.offsetLeftAndRight(-overlayOffset);
		} else {
			drawChild(canvas, mOverlayView, drawingTime);
		}
	}
	
	/**
	 * <i>Overridden for internal use!</i><br>
	 * <br>
	 * Copied from original android source and modified for our needs: Stripped disallow of
	 * interception and we don't iterate over all children to find touch event match but delegate to
	 * hidden or overlay view directly.
	 */
	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {
		final int action = ev.getAction();
		final float xf = ev.getX();
		final float yf = ev.getY();
		
		if (action == MotionEvent.ACTION_DOWN) {
			if (mMotionTarget != null) {
				mMotionTarget = null;
			}
			
			if (!onInterceptTouchEvent(ev)) {
				ev.setAction(MotionEvent.ACTION_DOWN);
				final View v = isHiddenViewCovered() ? mOverlayView : mHiddenView;
				
				if (v.dispatchTouchEvent(ev)) {
					mMotionTarget = v;
					return true;
				}
			}
		}
		
		boolean isUpOrCancel = (action == MotionEvent.ACTION_UP) ||
				(action == MotionEvent.ACTION_CANCEL);
		
		final View target = mMotionTarget;
		if (target == null) {
			ev.setLocation(xf, yf);
			return super.dispatchTouchEvent(ev);
		}
		
		if (onInterceptTouchEvent(ev)) {
			ev.setAction(MotionEvent.ACTION_CANCEL);
			ev.setLocation(xf, yf);
			target.dispatchTouchEvent(ev);
			mMotionTarget = null;
			return true;
		}
		
		if (isUpOrCancel) {
			mMotionTarget = null;
		}
		
		ev.setLocation(xf, yf);
		
		return target.dispatchTouchEvent(ev);
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public boolean onTouchEvent(MotionEvent e) {
		return mData.detachedFromList ? handleTouch(e, false) : super.onTouchEvent(e);
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public boolean onInterceptTouchEvent(MotionEvent e) {
		return mData.detachedFromList ? handleTouch(e, true) : super.onInterceptTouchEvent(e);
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public boolean performClick() {
		if (mData.consumeClick) {
			onViewSwipe(null, SwipeEvent.CLICK, 0, -1, null);
			return true;
		}
		
		return mStarted ? false : super.performClick();
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public boolean performLongClick() {
		if (mStarted) {
			return false;
		} else if (mData.consumeLongClick) {
			mLongClicked = true;
			onViewSwipe(null, SwipeEvent.LONG_CLICK, 0, -1, null);
			return true;
		} else if (super.performLongClick()) {
			mLongClicked = true;
			onViewSwipe(null, SwipeEvent.CANCEL, 0, -1, null);
			return true;
		} else {
			return false;
		}
	}
	
	/*
	 * Fix all predefined view group function so they only support a single view and do not break
	 * intern used layout (first is animation caching view, second is (temporary) hidden view, third
	 * (or second) is the added overlay view)
	 */
	
	/**
	 * You can only add a single child!
	 */
	@Override
	public void addView(View child) {
		addView(child, -1);
	}
	
	/**
	 * You can only add a single child!
	 */
	@Override
	public void addView(View child, int index) {
		checkAddView();
		mOverlayView = child;
		super.addView(child, -1);
	}
	
	/**
	 * You can only add a single child!
	 */
	@Override
	public void addView(View child, ViewGroup.LayoutParams params) {
		addView(child, -1, params);
	}
	
	/**
	 * You can only add a single child!
	 */
	@Override
	public void addView(View child, int index, ViewGroup.LayoutParams params) {
		checkAddView();
		mOverlayView = child;
		super.addView(child, -1, params);
	}
	
	/**
	 * You can only remove the child you set!
	 */
	@Override
	public void removeView(View view) {
		if (mHiddenViewCache != view) {
			if (mHiddenView != null && mHiddenView == view) {
				mHiddenView.setDrawingCacheEnabled(true);
				Bitmap cache = mHiddenView.getDrawingCache();
				
				mHiddenViewCache.setBackgroundDrawable(
						new BitmapDrawable(getContext().getResources(),
								cache == null ? null : Bitmap.createBitmap(cache)));
				mHiddenView.setDrawingCacheEnabled(false);
				mHiddenViewCache.getLayoutParams().height = mHiddenView.getHeight();
				mHiddenView = null;
			}
			
			if (mOverlayView == view) {
				mOverlayView = null;
			}
			
			super.removeView(view);
		}
	}
	
	/**
	 * You can only remove the child you set!
	 */
	@Override
	public void removeViewAt(int index) {
		if (index != 0 && (mHiddenView == null || index > 1)) {
			mOverlayView = null;
			super.removeViewAt(index);
		}
	}
	
	/**
	 * You can only remove the child you set!
	 */
	@Override
	public void removeAllViews() {
		int count = getChildCount();
		
		if (mHiddenView != null && count > 2 || mHiddenView == null && count > 1) {
			mOverlayView = null;
			super.removeViewAt(count - 1);
		}
	}
	
	/**
	 * You can only remove the child you set!
	 */
	@Override
	public void removeViews(int start, int count) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * No padding for this view group.
	 */
	@Override
	public void setPadding(int l, int t, int r, int b) {
		if (l != 0 || t != 0 || r != 0 || b != 0) {
			throw new IllegalArgumentException(getClass().getSimpleName()
					+ " does not allow padding parameters! Use inner view for that!");
		}
	}
	
	// PRIVATE ====================================================================================
	
	/**
	 * Initialize swipeable hidden view.
	 * 
	 * @param attrs
	 *            attributes from XML file
	 */
	private void initialize(AttributeSet attrs) {
		mHiddenViewCache = new View(getContext());
		super.addView(mHiddenViewCache, -1,
				new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		
		mAnimationStep = new Runnable() {
			@Override
			public void run() {
				if (!mAnimating) {
					return;
				}
				
				float step = (System.nanoTime() - mAnimationStepTime) / 1000000f
						/ mData.animationSpeed;
				float offsetBefore = mOffset;
				
				if (mOffset >= 0) {
					mOffset += mAnimateForward ? step : -step;
				} else {
					mOffset += mAnimateForward ? -step : step;
				}
				
				if (mAnimateForward && Math.abs(mOffset) >= 1.0f) {
					mOffset = mOffset >= 0 ? 1f : -1f;
					mAnimating = false;
				} else if (!mAnimateForward
						&& (offsetBefore >= 0 && mOffset <= 0 || offsetBefore < 0 && mOffset >= 0)) {
					mOffset = 0f;
					mAnimating = false;
					requestLayout();
				}
				
				mAnimationStepTime = System.nanoTime();
				invalidate();
				
				if (mAnimating) {
					mAnimationHandler.postDelayed(mAnimationStep, 30);
				}
			}
		};
		
		if (attrs == null) {
			return;
		}
	}
	
	private boolean handleTouch(MotionEvent e, boolean intercept) {
		if (mData.consumeClick) {
			setClickable(true);
		}
		
		if (mData.consumeLongClick) {
			setLongClickable(true);
		}
		
		switch (e.getAction()) {
		case MotionEvent.ACTION_DOWN:
			if (intercept) {
				mLongClicked = false;
				mStartX = (int) e.getX();
				mStarted = onViewSwipe(null, SwipeEvent.START, 0, -1, null);
			}
			break;
		
		case MotionEvent.ACTION_MOVE:
			if (!mLongClicked) {
				mStarted |= onViewSwipe(null, SwipeEvent.MOVE, (int) e.getX() - mStartX, -1, null);
			}
			break;
		
		case MotionEvent.ACTION_UP:
			if (!mLongClicked) {
				onViewSwipe(null, SwipeEvent.STOP, (int) e.getX() - mStartX, -1, null);
			}
			break;
		
		case MotionEvent.ACTION_CANCEL:
			if (!mLongClicked) {
				onViewSwipe(null, SwipeEvent.CANCEL, (int) e.getX() - mStartX, -1, null);
			}
			break;
		}
		
		if (intercept) {
			super.onInterceptTouchEvent(e);
			return mStarted;
		} else {
			super.onTouchEvent(e);
			return true;
		}
	}
	
	
	/**
	 * Check for illegal state when using {@code addView} methods (only one external child allowed).
	 */
	private void checkAddView() {
		int count = getChildCount();
		
		if (mHiddenView != null && count > 2 || mHiddenView == null && count > 1) {
			throw new IllegalStateException(
					"Swipeable action list item can host only one direct child!");
		}
	}
	
	/**
	 * Check perform requirements ({@link HiddenViewSetup} and a external child).
	 */
	private void checkRequirements() {
		if (isInEditMode()) {
			return;
		}
		
		if (mData == null) {
			throw new IllegalStateException(getClass().getSimpleName()
					+ " needs a a setup which is set by setHiddenViewSetup()!");
		}
		
		if (mOverlayView == null) {
			throw new IllegalStateException(getClass().getSimpleName()
					+ " needs a child to have function!");
		}
	}
	
	private void bindHiddenView() {
		if (mHiddenView == null) {
			mHiddenViewCache.getLayoutParams().height = 0;
			mHiddenView = mData.getHiddenView();
			
			if (mHiddenView.getParent() != null) {
				((ViewGroup) mHiddenView.getParent()).removeView(mHiddenView);
			}
			
			super.addView(mHiddenView, 1, new LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		}
	}
	
	
	/**
	 * Start animation.
	 * 
	 * @param forward
	 *            {@code true} means that animation will reveal and {@code false} that it will hide
	 *            the view in the background (the hidden view)
	 */
	private void animate(boolean forward) {
		calculateAnimationDirectionChange(forward);
		
		if (!mAnimating) {
			mAnimating = true;
			mAnimationStepTime = System.nanoTime();
			mAnimationHandler.post(mAnimationStep);
		}
	}
	
	private void calculateAnimationDirectionChange(boolean forward) {
		if (mAnimateForward == forward || mData.openAnimation == mData.closeAnimation
				|| isHiddenViewCovered() || isHiddenViewVisible()) {
			mAnimateForward = forward;
			return;
		}
		
		mAnimateForward = forward;
		
		int current = Math.round((forward ? mData.closeAnimation : mData.openAnimation)
				.getInterpolation(Math.abs(mOffset)) * 1000);
		Interpolator interpolator = forward ? mData.openAnimation : mData.closeAnimation;
		boolean wasNegativ = mOffset < 0;
		mOffset = Math.abs(mOffset);
		
		float left = 0;
		float right = 1;
		
		// never trust an infinite binary search loop
		for (int i = 0; i < 15; i++) {
			int v = Math.round(interpolator.getInterpolation(mOffset) * 1000);
			
			if (v > (int) (current + 0.5f)) {
				right = mOffset;
				mOffset = (left + mOffset) / 2;
			} else if (v < (int) (current + 0.5f)) {
				left = mOffset;
				mOffset = (right + mOffset) / 2;
			} else {
				break;
			}
		}
		
		if (wasNegativ) {
			mOffset = -mOffset;
		}
	}
}
