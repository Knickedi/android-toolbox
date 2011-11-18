package de.viktorreiser.toolbox.widget;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import de.viktorreiser.toolbox.util.AndroidUtils;
import de.viktorreiser.toolbox.widget.SwipeableHiddenView.HiddenViewSetup;

/**
 * Quick action setup for {@link SwipeableHiddenView}.<br>
 * <br>
 * Documentation of the {@link SwipeableHiddenView} shows how to use this setup. The setup itself
 * creates an quick action panel as hidden view. The user can uncover it and perform added quick
 * actions on the according list item. the quick action are symbolized by clickable images.
 * Everytime when you touch an action an popup indicator will be shown. This will contain the
 * clicked action and a description which will explain the action which will be performed when the
 * action is clicked. The default layout of this indicator is similar to system toast but this can
 * be modified any time.<br>
 * <br>
 * <b>Note</b>: If you add too much or too big quick actions they will center horizontally and
 * overflow by left and right edge of the hidden view.<br>
 * <br>
 * Some of the configurations are locked when the setup is attached to a swipeable hidden view. This
 * is always documented on the respective configuration method. Example:
 * 
 * <pre>
 * {@link HiddenQuickActionSetup} setup = new {@link HiddenQuickActionSetup}(context);
 * setup.{@code setOnQuickActionListener}(myListener);
 * setup.{@code addAction}(ACTION_1, R.drawable.action_description_1, R.drawable.image_action_1);
 * setup.{@code addAction}(ACTION_2, R.drawable.action_description_1, R.drawable.image_action_2);
 * 
 * // ... add this setup to {@link SwipeableHiddenView} ...
 * </pre>
 * 
 * <i>Depends on</i>: {@link AndroidUtils}
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class HiddenQuickActionSetup extends HiddenViewSetup {
	
	// PRIVATE ====================================================================================
	
	/** Layout which contains the image action views. */
	private UnpressableLinearLayout mLinearLayout;
	
	/** Width which is used for the image views. */
	private int mImageWidth = LayoutParams.WRAP_CONTENT;
	
	/** Height which is used for the image views. */
	private int mImageHeight = LayoutParams.WRAP_CONTENT;
	
	/** Action click listener. */
	private OnQuickActionListener mQuickActionListener;
	
	/** Touch listener for quick action views which triggers the click. */
	private OnTouchListener mTouchListener;
	
	/** Popup which will contain and display the indicator. */
	private PopupWindow mIndicatorPopup;
	
	/** Delay in milliseconds before the indicator will be shown. */
	private int mIndicatorDelay = 300;
	
	/** Reference to view of indicator which contains the image view. */
	private ImageView mIndicatorImage;
	
	/** Reference to view of indicator which contains the title. */
	private TextView mIndicatorTitle;
	
	/** Handler which fires delayed "show popup". */
	private Handler mPopupDelayHandler = new Handler();
	
	/** Routine which shows the popup. */
	private Runnable mIndicatorStart;
	
	/** Reference to current clicked quick action (image view). */
	private View mClickedActionView;
	
	/** Spacing between display edge / list item and indicator popup. */
	private Rect mIndicatorSpacing;
	
	/** {@code true} if swipeable view should be closed on quick action click. */
	private boolean mCloseSwipeableOnQuickAction = true;
	
	// PUBLIC =====================================================================================
	
	/**
	 * Interface for receiving a quick action click event.
	 * 
	 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
	 */
	public static interface OnQuickActionListener {
		
		/**
		 * Quick action clicked.
		 * 
		 * @param parent
		 *            adapter (list) to which the item on which the action is performed relates to
		 * @param view
		 *            the item (swipeable hidden) view in the list on which the action is performed
		 * @param position
		 *            position of the item view in the list
		 * @param quickActionId
		 *            action ID which was assigned to this action
		 */
		public void onQuickAction(AdapterView<?> parent, View view, int position, int quickActionId);
		
	}
	
	
	/**
	 * Create a quick action setup.
	 * 
	 * @param context
	 */
	public HiddenQuickActionSetup(Context context) {
		mLinearLayout = new UnpressableLinearLayout(context);
		mLinearLayout.setLayoutParams(new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		mLinearLayout.setOrientation(LinearLayout.HORIZONTAL);
		mLinearLayout.setGravity(Gravity.CENTER);
		mLinearLayout.setPadding(0, 0, 0, 0);
		
		mIndicatorSpacing = new Rect(0, 0, 0, AndroidUtils.dipToPixel(context, 10));
		
		setupQuickActionTouchListener();
		setupShowPopupStart();
		
		mIndicatorPopup = new PopupWindow(context);
		mIndicatorPopup.setAnimationStyle(android.R.style.Animation_Toast);
		mIndicatorPopup.setBackgroundDrawable(null);
		setupDefaultIndicator();
	}
	
	
	/**
	 * Set background for quick action panel.
	 * 
	 * @param d
	 *            drawable
	 */
	public void setBackgroundDrawable(Drawable d) {
		mLinearLayout.setBackgroundDrawable(d);
		mLinearLayout.invalidate();
	}
	
	/**
	 * Set background for quick action panel.
	 * 
	 * @param resId
	 *            resource ID for drawable
	 */
	public void setBackgroundResource(int resId) {
		mLinearLayout.setBackgroundResource(resId);
		mLinearLayout.invalidate();
	}
	
	/**
	 * Set background for quick action panel.
	 * 
	 * @param color
	 *            color value for {@link View#setBackgroundColor(int)}
	 */
	public void setBackgroundColor(int color) {
		mLinearLayout.setBackgroundColor(color);
		mLinearLayout.invalidate();
	}
	
	/**
	 * Set padding for quick action panel in pixel.
	 * 
	 * @param p
	 *            padding in pixel
	 */
	public void setViewPadding(int left, int top, int right, int bottom) {
		mLinearLayout.setPadding(left, top, right, bottom);
		mLinearLayout.invalidate();
	}
	
	/**
	 * Set quick action image size in pixel.
	 * 
	 * @param width
	 *            width in pixel or {@link LayoutParams#WRAP_CONTENT}
	 * @param height
	 *            height in pixel or {@link LayoutParams#WRAP_CONTENT}
	 */
	public void setImageSize(int width, int height) {
		if (width == LayoutParams.FILL_PARENT || height == LayoutParams.FILL_PARENT) {
			throw new IllegalArgumentException("width or height can't be FILL_PARENT!");
		}
		
		mImageWidth = width;
		mImageHeight = height;
		int count = mLinearLayout.getChildCount() - 1;
		
		for (int i = 0; i < count; i++) {
			LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)
					((ViewGroup) mLinearLayout.getChildAt(i)).getChildAt(0).getLayoutParams();
			lp.width = width;
			lp.height = height;
		}
		
		mLinearLayout.requestLayout();
	}
	
	/**
	 * Should swipeable view be close if a quick action is clicked (default is {@code true})?
	 * 
	 * @param close
	 *            {@code true} will close the swipeable view
	 */
	public void setCloseSwipableOnQuickActionClick(boolean close) {
		mCloseSwipeableOnQuickAction = close;
	}
	
	/**
	 * Set delay in milliseconds until indicator pops up.
	 * 
	 * @param delay
	 *            delay in milliseconds ({@code < 0 ==} disabled and {@code 0 ==} no delay)
	 */
	public void setIndicatorDelay(int delay) {
		mIndicatorDelay = delay;
	}
	
	/**
	 * Set layout of popup indicator.<br>
	 * <br>
	 * Layout will be stretched in width and wrapped in height.<br>
	 * It should contain an text view with ID {@code android.R.id.title} and an image view with ID
	 * {@code android.R.id.icon}.
	 * 
	 * @param resId
	 *            layout resource
	 */
	public void setIndicatorLayout(int resId) {
		if (resId == 0) {
			setIndicatorLayout(null);
		} else {
			LayoutInflater li = (LayoutInflater) mLinearLayout.getContext().getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);
			setIndicatorLayout(li.inflate(resId, null));
		}
	}
	
	/**
	 * Set layout of popup indicator.<br>
	 * <br>
	 * Layout will be stretched in width and wrapped in height.<br>
	 * It should contain an text view with ID {@code android.R.id.title} and an image view with ID
	 * {@code android.R.id.icon}.
	 * 
	 * @param content
	 *            view which contains and represents the indicator layout
	 */
	public void setIndicatorLayout(View content) {
		if (content == null) {
			setupDefaultIndicator();
		} else {
			View image = content.findViewById(android.R.id.icon);
			View title = content.findViewById(android.R.id.title);
			
			if (title != null && title instanceof TextView) {
				mIndicatorImage = (ImageView) image;
			} else {
				throw new IllegalStateException(
						"Indicator layout doesn't define image view with android ID icon!");
			}
			
			if (title != null && title instanceof TextView) {
				mIndicatorTitle = (TextView) title;
			} else {
				throw new IllegalStateException(
						"Indicator layout doesn't define text view with android ID title!");
			}
			
			mIndicatorPopup.setContentView(content);
		}
	}
	
	/**
	 * Set quick action image size in pixel for indicator.
	 * 
	 * @param width
	 *            width in pixel or {@link LayoutParams#WRAP_CONTENT}
	 * @param height
	 *            height in pixel or {@link LayoutParams#WRAP_CONTENT}
	 */
	public void setIndicatorImageSize(int width, int height) {
		if (width == LayoutParams.FILL_PARENT || height == LayoutParams.FILL_PARENT) {
			throw new IllegalArgumentException("width or height can't be FILL_PARENT!");
		}
		
		mIndicatorImage.getLayoutParams().width = width;
		mIndicatorImage.getLayoutParams().height = height;
	}
	
	/**
	 * Set spacings of indicator popup in pixel.
	 * 
	 * @param left
	 *            spacing to left display edge
	 * @param top
	 *            spacing to top display edge
	 * @param right
	 *            spacing to right display edge
	 * @param bottom
	 *            to swipeable item
	 */
	public void setIndicatorSpacing(int left, int top, int right, int bottom) {
		mIndicatorSpacing = new Rect(left, top, right, bottom);
	}
	
	/**
	 * Add quick action to setup.
	 * 
	 * @param actionId
	 *            ID which will be reported to {@link OnQuickActionListener} when it is performed
	 * @param actionDescriptionResId
	 *            action description shown when hovered (can be {@code 0} for no indicator)
	 * @param drawableResId
	 *            drawable for quick action
	 * 
	 * @return {@code false} if action ID already set
	 */
	public boolean addAction(int actionId, int actionDescriptionResId, int drawableResId) {
		return addAction(
				actionId,
				actionDescriptionResId == 0 ? null : mLinearLayout.getContext().getResources()
						.getString(actionDescriptionResId),
				mLinearLayout.getContext().getResources().getDrawable(drawableResId));
	}
	
	/**
	 * Add quick action to setup.
	 * 
	 * @param actionId
	 *            ID which will be reported to {@link OnQuickActionListener} when it is performed
	 * @param actionDescriptionResId
	 *            action description shown when hovered (can be {@code 0} for no indicator)
	 * @param drawable
	 *            drawable for quick action
	 * 
	 * @return {@code false} if action ID already set
	 */
	public boolean addAction(int actionId, int actionDescriptionResId, Drawable drawable) {
		return addAction(
				actionId,
				actionDescriptionResId == 0 ? null : mLinearLayout.getContext().getResources()
						.getString(actionDescriptionResId),
				drawable);
	}
	
	/**
	 * Add quick action to setup.
	 * 
	 * @param actionId
	 *            ID which will be reported to {@link OnQuickActionListener} when it is performed
	 * @param actionDescription
	 *            action description shown when hovered (can be {@code null} for no indicator)
	 * @param drawableResId
	 *            drawable for quick action
	 * 
	 * @return {@code false} if action ID already set
	 */
	public boolean addAction(int actionId, String actionDescription, int drawableResId) {
		return addAction(actionId, actionDescription,
				mLinearLayout.getContext().getResources().getDrawable(drawableResId));
	}
	
	/**
	 * Add quick action to setup.
	 * 
	 * @param actionId
	 *            ID which will be reported to {@link OnQuickActionListener} when it is performed
	 * @param actionDescription
	 *            action description shown when hovered (can be {@code null} for no indicator)
	 * @param drawable
	 *            drawable for quick action
	 * 
	 * @return {@code false} if action ID already set
	 */
	public boolean addAction(int actionId, String actionDescription, Drawable drawable) {
		int count = mLinearLayout.getChildCount();
		
		for (int i = 0; i < count; i++) {
			if (actionId == ((ActionInfo) mLinearLayout.getChildAt(i).getTag()).id) {
				return false;
			}
		}
		
		ImageView iv = new ImageView(mLinearLayout.getContext());
		
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				mImageWidth, mImageHeight);
		params.addRule(RelativeLayout.CENTER_IN_PARENT);
		
		iv.setLayoutParams(params);
		iv.setImageDrawable(drawable);
		
		ActionInfo info = new ActionInfo();
		info.id = actionId;
		info.description = actionDescription;
		
		RelativeLayout rl = new RelativeLayout(mLinearLayout.getContext());
		LinearLayout.LayoutParams params2 = new LinearLayout.LayoutParams(
				0, LayoutParams.FILL_PARENT);
		params2.weight = 1;
		rl.setLayoutParams(params2);
		rl.addView(iv);
		rl.setTag(info);
		rl.setOnTouchListener(mTouchListener);
		
		mLinearLayout.addView(rl);
		
		return true;
	}
	
	/**
	 * Remove added action.
	 * 
	 * @param actionId
	 *            ID which will be reported to {@link OnQuickActionListener} when it is performed
	 *            and was used in {@link #addAction} to set the action
	 * 
	 * @return {@code true} if action successfully removed from setup
	 */
	public boolean removeAction(int actionId) {
		int count = mLinearLayout.getChildCount();
		
		for (int i = 0; i < count; i++) {
			if (actionId == (Integer) ((ActionInfo) mLinearLayout.getChildAt(i).getTag()).id) {
				mLinearLayout.removeViewAt(i);
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Set listener for quick action which will be triggered if it is clicked.
	 * 
	 * @param l
	 *            listener for quick action
	 */
	public void setOnQuickActionListener(OnQuickActionListener l) {
		mQuickActionListener = l;
	}
	
	// OVERRIDDEN =================================================================================
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public View getHiddenView() {
		return mLinearLayout;
	}
	
	// PRIVATE ====================================================================================
	
	private void setupDefaultIndicator() {
		LinearLayout popupContent = new LinearLayout(mLinearLayout.getContext());
		popupContent.setBackgroundDrawable(
				Toast.makeText(mLinearLayout.getContext(), "", 0).getView().getBackground());
		
		final Context c = mLinearLayout.getContext();
		LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
				AndroidUtils.dipToPixel(c, 25), AndroidUtils.dipToPixel(c, 25));
		imageParams.rightMargin = AndroidUtils.dipToPixel(c, 15);
		imageParams.gravity = Gravity.CENTER_VERTICAL;
		
		LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		titleParams.gravity = Gravity.CENTER_VERTICAL;
		
		mIndicatorImage = new ImageView(mLinearLayout.getContext());
		mIndicatorTitle = new TextView(mLinearLayout.getContext());
		mIndicatorTitle.setTextColor(0xffffffff);
		
		popupContent.addView(mIndicatorImage, imageParams);
		popupContent.addView(mIndicatorTitle, titleParams);
		
		mIndicatorPopup.setContentView(popupContent);
	}
	
	private void setupQuickActionTouchListener() {
		mTouchListener = new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				int a = event.getAction();
				
				if (a == MotionEvent.ACTION_DOWN) {
					if (isHiddenViewCovered()) {
						return false;
					}
					
					Drawable drawable = ((ImageView) ((ViewGroup) v).getChildAt(0)).getDrawable();
					
					if (drawable instanceof StateListDrawable) {
						drawable = ((StateListDrawable) drawable).getCurrent();
					}
					
					mIndicatorImage.setImageDrawable(drawable);
					mClickedActionView = v;
					
					if (mIndicatorDelay == 0) {
						mIndicatorStart.run();
					} else if (mIndicatorDelay > 0 && ((ActionInfo) v.getTag()).description != null) {
						mPopupDelayHandler.postDelayed(mIndicatorStart, mIndicatorDelay);
					}
					
					v.setPressed(true);
					v.invalidate();
				} else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
					if (a == MotionEvent.ACTION_UP) {
						if (mCloseSwipeableOnQuickAction) {
							closeHiddenView();
						}
						
						if (mQuickActionListener != null) {
							mQuickActionListener.onQuickAction(
									getCurrentListView(),
									getCurrentSwipeableHiddenView(),
									getCurrentPosition(),
									((ActionInfo) v.getTag()).id);
						}
					}
					
					mPopupDelayHandler.removeCallbacks(mIndicatorStart);
					mIndicatorPopup.dismiss();
					v.setPressed(false);
					v.invalidate();
				}
				
				return true;
			}
		};
	}
	
	private void setupShowPopupStart() {
		mIndicatorStart = new Runnable() {
			@Override
			public void run() {
				int width = mLinearLayout.getRootView().getWidth() - mIndicatorSpacing.left
						- mIndicatorSpacing.right;
				mIndicatorPopup.getContentView().measure(width | MeasureSpec.AT_MOST, 0);
				int height = mIndicatorPopup.getContentView().getMeasuredHeight();
				
				mIndicatorPopup.setWidth(width);
				mIndicatorPopup.setHeight(height);
				mIndicatorTitle.setText(((ActionInfo) mClickedActionView.getTag()).description);
				
				int hiddenViewY = AndroidUtils.getContentLocation(mLinearLayout).y;
				
				if (height + mIndicatorSpacing.top + mIndicatorSpacing.bottom <= hiddenViewY) {
					mIndicatorPopup.showAtLocation(mLinearLayout,
							Gravity.TOP | Gravity.CENTER_HORIZONTAL,
							mIndicatorSpacing.left, hiddenViewY - height - mIndicatorSpacing.top
									+ AndroidUtils.getContentOffsetFromTop(mLinearLayout));
				} else {
					mIndicatorPopup.showAtLocation(mLinearLayout,
							Gravity.TOP | Gravity.CENTER_HORIZONTAL,
							mIndicatorSpacing.left, mIndicatorSpacing.top
									+ AndroidUtils.getContentOffsetFromTop(mLinearLayout));
				}
			}
		};
	}
	
	/**
	 * Layout which doesn't set pressed state on children.
	 * 
	 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
	 */
	private class UnpressableLinearLayout extends LinearLayout {
		
		public UnpressableLinearLayout(Context context) {
			super(context);
		}
		
		@Override
		public void dispatchSetPressed(boolean pressed) {
			
		}
	}
	
	private class ActionInfo {
		int id;
		String description;
	}
}
