package de.viktorreiser.toolbox.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import de.viktorreiser.toolbox.R;

/**
 * View flipper which doesn't flip between views but between a state change of a single view.<br>
 * <br>
 * First: This view container can only manage a single view!<br>
 * Add a child, {@link #setAnimation(AnimType, Animation)}, {@link #saveState()} before making
 * changes to the child and {@link #changeState(boolean)} when changes are set to the child.<br>
 * <br>
 * Following custom XML attributes are available:<br>
 * <br>
 * {@code animation(In|Out|ReverseIn|RevereseOut)="animationResId"} -
 * {@link #setAnimation(AnimType, Animation)}<br>
 * <br>
 * <i>Depends on</i>: {@code res/values/attrs.xml}
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class ViewStateFlipper extends FrameLayout {
	
	// PRIVATE ====================================================================================
	
	private ImageView mCachedState;
	private View mMainView;
	private AnimationListener mAnimationListener;
	private Animation mInAnimation;
	private Animation mOutAnimation;
	private Animation mInReverseAnimation;
	private Animation mOutReverseAnimation;
	
	// PUBLIC =====================================================================================
	
	public static enum AnimType {
		IN, OUT, IN_REVERSE, OUT_REVERSE
	};
	
	
	/**
	 * Set animation which will be used for state change by resource ID.
	 * 
	 * @param type
	 *            type of animation to set
	 * @param resId
	 *            animation resource ID
	 * 
	 * @see #setAnimation(AnimType, Animation)
	 */
	public void setAnimation(AnimType type, int resId) {
		setAnimation(type, AnimationUtils.loadAnimation(getContext(), resId));
	}
	
	/**
	 * Set animation which will be used for state change.<br>
	 * <br>
	 * {@link AnimType#IN} - how the new state comes in<br>
	 * {@link AnimType#OUT} - how the old state gets out<br>
	 * <br>
	 * The idea of reverse animation is to get the feeling that you restore an previous state, so
	 * reverse a state change (e.g. a forward/backward navigation)<br>
	 * {@link AnimType#IN_REVERSE} - typically the animation comes from where {@link AnimType#OUT}
	 * went to<br>
	 * {@link AnimType#OUT_REVERSE} - typically the animation goes to where {@link AnimType#IN} came
	 * from
	 * 
	 * @param type
	 *            type of animation to set
	 * @param animation
	 *            animation which will be set
	 * 
	 * @see #changeState(boolean)
	 */
	public void setAnimation(AnimType type, Animation animation) {
		switch (type) {
		case IN:
			mInAnimation = animation;
			break;
		case OUT:
			mOutAnimation = animation;
			break;
		case IN_REVERSE:
			mInReverseAnimation = animation;
			break;
		case OUT_REVERSE:
			mOutReverseAnimation = animation;
			break;
		}
		
		if (animation != null) {
			switch (type) {
			case OUT:
			case OUT_REVERSE:
				animation.setAnimationListener(mAnimationListener);
				break;
			}
		}
	}
	
	/**
	 * Save state of child before you change it and call {@link #changeState(boolean)} after changes
	 * are made.
	 */
	public void saveState() {
		if (mMainView != null) {
			mMainView.setDrawingCacheEnabled(true);
			Bitmap b = mMainView.getDrawingCache();
			
			if (b != null) {
				mCachedState.setImageBitmap(Bitmap.createBitmap(b));
			}
			
			mMainView.setDrawingCacheEnabled(false);
		}
	}
	
	/**
	 * Animate child state change (saved before with {@link #saveState()}).
	 * 
	 * @param forward
	 *            {@code true} will use {@link AnimType#IN} and {@link AnimType#OUT}, {@code false}
	 *            will use {@link AnimType#IN_REVERSE} and {@link AnimType#OUT_REVERSE}
	 * 
	 * @see #setAnimation(AnimType, Animation)
	 */
	public void changeState(boolean forward) {
		
		Animation out = forward ? mOutAnimation : mOutReverseAnimation;
		
		if (out != null) {
			mCachedState.setVisibility(View.VISIBLE);
			mCachedState.clearAnimation();
			mCachedState.startAnimation(out);
		} else {
			mCachedState.setVisibility(View.GONE);
		}
		
		if (mMainView != null) {
			Animation in = forward ? mInAnimation : mInReverseAnimation;
			
			if (in != null) {
				mMainView.clearAnimation();
				mMainView.startAnimation(in);
			}
		}
	}
	
	// OVERRIDDEN =================================================================================
	
	public ViewStateFlipper(Context context) {
		super(context);
		initialize(null);
	}
	
	public ViewStateFlipper(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize(attrs);
	}
	
	public ViewStateFlipper(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize(attrs);
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
	
	/**
	 * You can only add a single child!
	 */
	@Override
	public void addView(View child) {
		addView(child, 1);
	}
	
	/**
	 * You can only add a single child!
	 */
	@Override
	public void addView(View child, int index) {
		checkAddView();
		mMainView = child;
		super.addView(child, 1);
	}
	
	/**
	 * You can only add a single child!
	 */
	@Override
	public void addView(View child, ViewGroup.LayoutParams params) {
		addView(child, 1, params);
	}
	
	/**
	 * You can only add a single child!
	 */
	@Override
	public void addView(View child, int index, ViewGroup.LayoutParams params) {
		checkAddView();
		mMainView = child;
		super.addView(child, 1, params);
	}
	
	/**
	 * You can only remove the child you set!
	 */
	@Override
	public void removeView(View view) {
		if (mCachedState != view) {
			super.removeView(view);
		}
	}
	
	/**
	 * You can only remove the child you set!
	 */
	@Override
	public void removeViewAt(int index) {
		if (mCachedState != getChildAt(index)) {
			removeViewAt(index);
		}
	}
	
	/**
	 * You can only remove the child you set!
	 */
	@Override
	public void removeAllViews() {
		mMainView = null;
		int count = getChildCount();
		
		for (int i = 0; i < count; i++) {
			if (mCachedState != getChildAt(i)) {
				removeViewAt(i);
			}
		}
	}
	
	/**
	 * You can only remove the child you set!
	 */
	@Override
	public void removeViews(int start, int count) {
		for (int i = start; i < start + count; i++) {
			if (mCachedState != getChildAt(i)) {
				removeViewAt(i);
			}
		}
	}
	
	// PRIVATE ====================================================================================
	
	private void initialize(AttributeSet attrs) {
		mCachedState = new ImageView(getContext());
		mCachedState.setVisibility(View.GONE);
		super.addView(mCachedState, -1, new FrameLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		
		mAnimationListener = new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
				
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
				
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				mCachedState.setVisibility(View.GONE);
			}
		};
		
		if (attrs == null) {
			return;
		}
		
		TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ViewStateFlipper);
		int res;
		
		if ((res = a.getResourceId(R.styleable.ViewStateFlipper_animationIn, 0)) != 0) {
			setAnimation(AnimType.IN, AnimationUtils.loadAnimation(getContext(), res));
		}
		if ((res = a.getResourceId(R.styleable.ViewStateFlipper_animationOut, 0)) != 0) {
			setAnimation(AnimType.OUT, AnimationUtils.loadAnimation(getContext(), res));
		}
		if ((res = a.getResourceId(R.styleable.ViewStateFlipper_animationReverseIn, 0)) != 0) {
			setAnimation(AnimType.IN_REVERSE, AnimationUtils.loadAnimation(getContext(), res));
		}
		if ((res = a.getResourceId(R.styleable.ViewStateFlipper_animationReverseOut, 0)) != 0) {
			setAnimation(AnimType.OUT_REVERSE, AnimationUtils.loadAnimation(getContext(), res));
		}
		
		a.recycle();
	}
	
	
	private void checkAddView() {
		if (getChildCount() == 2) {
			throw new IllegalStateException(getClass().getSimpleName()
					+ " allows only a single child view!");
		}
	}
}
