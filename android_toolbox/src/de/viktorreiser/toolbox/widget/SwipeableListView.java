package de.viktorreiser.toolbox.widget;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import de.viktorreiser.toolbox.widget.SwipeableListItem.SwipeEvent;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewConfiguration;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * List which supports swipeable item views.<br>
 * <br>
 * The list itself behaves exactly the same as it did before and therefore doesn't provide any own
 * methods. But it allows its item views to behave differently. Item views can implement a reaction
 * on a horizontal swipe motion. All they have to do is to implement {@link SwipeableListItem}, be
 * used as direct view item in the list (not nested in another view group) and react on the events
 * they receive from the list.<br>
 * <br>
 * The clue about this technique is that this allows the list to still perform item (long) clicks
 * and place the selection as long the swipeable view decides not to perform until a certain offset
 * is reached. It also enables the view to process a swipe cancel in an correct way because the list
 * will inform it about that.<br>
 * Beside that this technique enables you to develop any kind of swipeable list items which all can
 * be used in the same list at the same time.<br>
 * <br>
 * <i>Depends on</i>: {@link SwipeableListItem}
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class SwipeableListView extends ListView implements OnScrollListener,
		OnCreateContextMenuListener {
	
	// PRIVATE ====================================================================================
	
	/** Reflected helper for selection clearance. */
	private static Field mSelectorRect = null;
	
	/** Reflected helper for selection clearance. */
	private static Method mRectSetEmpty = null;
	
	static {
		try {
			mSelectorRect = AbsListView.class.getDeclaredField("mSelectorRect");
			mSelectorRect.setAccessible(true);
			mRectSetEmpty = Rect.class.getDeclaredMethod("setEmpty");
			mRectSetEmpty.setAccessible(true);
		} catch (Exception e) {
			mRectSetEmpty = null;
		}
	}
	
	
	/**
	 * Try to restore old swipeable view state on list data change.<br>
	 * <br>
	 * When {@link #setAdapter(ListAdapter)} is called or data change on the current adapter is
	 * performed and list is currently performing a swipe action this variable will be set to its
	 * position. When list is refreshed it will be checked whether the swiped position is still
	 * visible. When this is the case then the swipe action will be performed on the (new) view at
	 * this position.<br>
	 * <br>
	 * This might restore a swipe state on a completely different swipeable view or the swipe could
	 * have an awkward offset. But it's the way the list view handles a data change on (long) click
	 * too. It will also click or long click a different item. So we just follow this pattern!
	 */
	private int mRestorePosition = INVALID_POSITION;
	
	/**
	 * Motion event value of X coordinate when swipe was started.<br>
	 * <br>
	 * This is used to calculate the swipe offset.
	 */
	private int mSwipeX;
	
	/**
	 * Motion event value of X coordinate when swipe was started.<br>
	 * <br>
	 * Used to intercept view item click for swipe motion.
	 */
	private int mSwipeY;
	
	/**
	 * Does list currently handles a swipe action?<br>
	 * <br>
	 * Is {@code true} after {@link SwipeEvent#START}, while {@link SwipeEvent#MOVE}, until
	 * {@link SwipeEvent#CANCEL} or (long) click (whether {@link SwipeEvent#CLICK} or
	 * {@link SwipeEvent#LONG_CLICK} is sent or not).
	 */
	private boolean mHasSwipeable = false;
	
	/**
	 * Reference to swipeable view on which the list is currently operating.<br>
	 * <br>
	 * Is at least available as long {@link #mHasSwipeable} is {@code true}. But it's often
	 * available until next swipe and represents the last swiped view.
	 */
	private SwipeableListItem mSwipeableView;
	
	/**
	 * Position of swipeable view in list.<br>
	 * <br>
	 * It has the same lifecycle like {@link #mSwipeableView}.
	 */
	private int mSwipeablePosition = INVALID_POSITION;
	
	/**
	 * X motion offset of swipe (positive when right of and negative to left of started position).<br>
	 * <br>
	 * This is the last offset calculated for a swipe action. It has the same lifecycle like
	 * {@link #mSwipeableView}.
	 */
	private int mSwipeOffset;
	
	/**
	 * Swipe was stated by view.<br>
	 * <br>
	 * Swipe action performed and {@link SwipeableListItem#onViewSwipe} returned {@code true}.<br>
	 * It has the same lifecycle like {@link #mHasSwipeable} besides the fact that this variable
	 * waits for a swipe start of view.
	 */
	private boolean mSwipeableStarted;
	
	/**
	 * {@code true} if next incoming click event has to be consumed.<br>
	 * <br>
	 * This will suppress the default click. It will be set with {@link #mSwipeableStarted} or on
	 * long click when swipeable view consumes it. Item will remain pressed and trigger a click on
	 * touch up which we have to consume.
	 */
	private boolean mConsumeClick;
	
	/** Flag which avoids double initialization by touch and interception touch event. */
	private boolean mStartAlreadyRequested = false;
	
	/**
	 * Flag disables selection in draw method.
	 */
	boolean mConsumeSelection = false;
	
	/** {@code true} if swipe should be canceled on focus loss. */
	boolean mCancelSwipeOnFocusLoss = false;
	
	/**
	 * Reused swipeable views and their current associated item view positions.<br>
	 * <br>
	 * This is used in {@code onScroll}, The views will be added to this map if they are not already
	 * in it. If they are then we get their last saved position and if it is reused it will differ
	 * so {@link SwipeableListItem#swipeStateReset()} will be called.
	 */
	private Map<SwipeableListItem, Integer> mCachedPositions =
			new HashMap<SwipeableListItem, Integer>();
	
	
	/**
	 * Create context menu listener set from outside of class.<br>
	 * <br>
	 * Only call it if swipe view doesn't consume long click.
	 */
	private OnCreateContextMenuListener mCreateContextMenuListener = null;
	
	/**
	 * Listener for data changes of adapter.<br>
	 * <br>
	 * This will try to restore current swipe action.
	 */
	private DataSetObserver mChangeObserver = new DataSetObserver() {
		@Override
		public void onChanged() {
			if (mSwipeableView != null) {
				if (mHasSwipeable) {
					// onScroll will try to recover current swipe
					mRestorePosition = mSwipeablePosition;
				}
				
				mSwipeableView.swipeStateReset();
				mSwipeableView = null;
			}
		}
	};
	
	// PUBLIC =====================================================================================
	
	/**
	 * Should any swipe be canceled if list loses focus (default {@code false}).
	 * 
	 * @param cancel
	 *            {@code true} if swipe should be canceled on focus loss
	 */
	public void setCancelSwipeOnFocusLoss(boolean cancel) {
		mCancelSwipeOnFocusLoss = cancel;
	}
	
	// OVERRIDDEN =================================================================================
	
	public SwipeableListView(Context context) {
		super(context);
		initialize();
	}
	
	public SwipeableListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize();
	}
	
	public SwipeableListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initialize();
	}
	
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public void setAdapter(ListAdapter adapter) {
		if (mSwipeableView != null) {
			if (mHasSwipeable) {
				// onScroll will try to recover current swipe
				mRestorePosition = mSwipeablePosition;
			}
			
			mSwipeableView.swipeStateReset();
			mSwipeableView = null;
		}
		
		if (getAdapter() != null) {
			getAdapter().unregisterDataSetObserver(mChangeObserver);
		}
		
		if (adapter != null) {
			adapter.registerDataSetObserver(mChangeObserver);
		}
		
		super.setAdapter(adapter);
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public void setOnScrollListener(final OnScrollListener l) {
		// merge given scroll listener with or own
		if (l == null || l == this) {
			super.setOnScrollListener(this);
		} else {
			super.setOnScrollListener(new OnScrollListener() {
				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) {
					this.onScrollStateChanged(view, scrollState);
					l.onScrollStateChanged(view, scrollState);
				}
				
				@Override
				public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
						int totalItemCount) {
					this.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
					l.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
				}
			});
		}
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
			int totalItemCount) {
		restoreSwipe();
		
		// track all swipeable views and reset them if they don't point to the same position
		// anymore -> this resets recycled swipeable views
		for (int i = 0; i < visibleItemCount; i++) {
			View child = getChildAt(i);
			
			if (child instanceof SwipeableListItem) {
				SwipeableListItem swipeable = (SwipeableListItem) child;
				Integer previousPosition = mCachedPositions.get(swipeable);
				
				if (previousPosition == null) {
					mCachedPositions.put(swipeable, i + firstVisibleItem);
				} else if (previousPosition != i + firstVisibleItem) {
					swipeable.swipeStateReset();
					mCachedPositions.put(swipeable, i + firstVisibleItem);
				}
			}
		}
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (scrollState == SCROLL_STATE_FLING || scrollState == SCROLL_STATE_TOUCH_SCROLL) {
			// scroll state cancels swipeable view
			cancelSwipe();
		}
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public void setOnCreateContextMenuListener(OnCreateContextMenuListener l) {
		// merge create context listener with our own
		mCreateContextMenuListener = l == null || l == this ? null : l;
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (mHasSwipeable) {
			if (mSwipeableStarted) {
				// swipeable started, consume all click events
				mConsumeClick = true;
				return;
			} else if (mSwipeableView.swipeOnLongClick()) {
				// swipeable requests context click event and consumes it
				sendSwipe(SwipeEvent.LONG_CLICK);
				mConsumeClick = true;
				mHasSwipeable = false;
				mConsumeSelection = !mSwipeableView.swipeDoesntHideListSelector();
				invalidate();
				return;
			}
		}
		
		// inform external listener if it's set
		if (mCreateContextMenuListener != null) {
			mCreateContextMenuListener.onCreateContextMenu(menu, v, menuInfo);
		}
		
		mHasSwipeable = false;
		mSwipeableStarted = false;
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public boolean performItemClick(View view, int position, long id) {
		// swipeable consumes item click
		if (mConsumeClick || mHasSwipeable && mSwipeableStarted) {
			return false;
		}
		
		// swipeable requests click event and consumes it
		//
		// we check for view directly instead for swipeable flag for a good reason:
		// context click performed -> swipeable dind't want to consume it -> reset swipeable state
		// -> default context menu requested -> no context menu received -> list performs click!
		if (mSwipeableView != null && mSwipeableView.swipeOnClick()) {
			sendSwipe(SwipeEvent.CLICK);
			mHasSwipeable = false;
			return false;
		}
		
		// click requested so cancel swipeable
		// cancelSwipe();
		
		return super.performItemClick(view, position, id);
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		super.onWindowFocusChanged(hasWindowFocus);
		
		// a context menu which pops up will trigger this
		// if window loses focus we have to cancel swipe anyway
		if (!hasWindowFocus && mCancelSwipeOnFocusLoss) {
			cancelSwipe();
		}
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		mSwipeOffset = (int) ev.getX() - mSwipeX;
		
		if (ev.getAction() == MotionEvent.ACTION_DOWN) {
			setupSwipeableClick(ev);
		} else if (ev.getAction() == MotionEvent.ACTION_UP) {
			// swipeable is active and started, send stop
			if (mHasSwipeable && mSwipeableStarted) {
				sendSwipe(SwipeEvent.STOP);
			}
			
			mSwipeableStarted = false;
		} else if (ev.getAction() == MotionEvent.ACTION_MOVE) {
			// swipeable is active, move event reports start, not started yet, do it now
			if (mHasSwipeable && (sendSwipe(SwipeEvent.MOVE) || mSwipeableStarted)) {
				mSwipeableStarted = true;
				mConsumeClick = true;
				mConsumeSelection = !mSwipeableView.swipeDoesntHideListSelector();
			}
		} else if (ev.getAction() == MotionEvent.ACTION_CANCEL) {
			// swipeable is active, send cancel
			if (mHasSwipeable) {
				mSwipeOffset = 0;
				cancelSwipe();
			}
		}
		
		if (ev.getAction() != MotionEvent.ACTION_DOWN) {
			mStartAlreadyRequested = false;
		}
		
		int offset = (int) (ViewConfiguration.getTouchSlop()
				* getContext().getResources().getDisplayMetrics().density + 0.5f) * 2;
		
		// while interacting with a swipeable view we double the offset until list scroll kicks in
		return ev.getAction() != MotionEvent.ACTION_MOVE || !mHasSwipeable || !mSwipeableStarted
				|| Math.abs((int) ev.getY() - mSwipeY) > offset
				? super.onTouchEvent(ev) : true;
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (ev.getAction() == MotionEvent.ACTION_DOWN) {
			setupSwipeableClick(ev);
		} else {
			mStartAlreadyRequested = false;
		}
		
		if (super.onInterceptTouchEvent(ev)) {
			return true;
		} else if (mHasSwipeable
				&& Math.abs((int) ev.getX() - mSwipeX) > ViewConfiguration.getTouchSlop()) {
			// we intercept for horizontal scroll of a swipeable view
			// to set the selector list view needs a touch down event
			int oldAction = ev.getAction();
			ev.setAction(MotionEvent.ACTION_DOWN);
			super.onTouchEvent(ev);
			ev.setAction(oldAction);
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public void draw(Canvas canvas) {
		if (mConsumeSelection) {
			// list insists on drawing the selector
			// cancel this on last possible moment before actual draw
			
			setPressed(false);
			if (mSwipeableView != null) {
				((View) mSwipeableView).setPressed(false);
			}
			
			try {
				mRectSetEmpty.invoke(mSelectorRect.get(this));
			} catch (Exception e) {
			}
		}
		
		super.draw(canvas);
	}
	
	// PRIVATE ====================================================================================
	
	private void initialize() {
		super.setOnScrollListener(this);
		super.setOnCreateContextMenuListener(this);
	}
	
	
	/**
	 * Send {@link SwipeEvent#CANCEL} if {@link #mHasSwipeable}.
	 */
	private void cancelSwipe() {
		if (mHasSwipeable) {
			mSwipeableStarted = false;
			mHasSwipeable = false;
			sendSwipe(SwipeEvent.CANCEL);
		}
	}
	
	/**
	 * Try to restore swipeable by given {@link #mRestorePosition}.
	 */
	private void restoreSwipe() {
		if (mRestorePosition == INVALID_POSITION) {
			return;
		}
		
		int wantedChild = mSwipeablePosition - getFirstVisiblePosition();
		View v = getChildAt(wantedChild);
		
		if (wantedChild < 0 || wantedChild >= getChildCount()
				|| !(v instanceof SwipeableListItem)) {
			// nothing to restore
			mHasSwipeable = false;
			mConsumeClick = false;
		} else {
			mSwipeableView = (SwipeableListItem) v;
			mSwipeableView.swipeStateReset();
			mSwipeablePosition = mRestorePosition;
			int previousOffset = mSwipeOffset;
			
			// restart swipe action on view at previous position
			mSwipeOffset = 0;
			
			mSwipeableStarted = sendSwipe(SwipeEvent.START);
			
			mSwipeOffset = previousOffset;
			mSwipeableStarted |= sendSwipe(SwipeEvent.MOVE);
			
			mConsumeClick = mSwipeableStarted;
			
			// restore position of view so it won't be cleared in onScroll
			mCachedPositions.put(mSwipeableView, mRestorePosition);
		}
		
		mRestorePosition = INVALID_POSITION;
	}
	
	/**
	 * Send swipe event to current swipeable view.
	 * 
	 * @param type
	 *            swipe event
	 * 
	 * @return {@link SwipeableListItem#onViewSwipe(ListView, SwipeEvent, int, int)}
	 */
	private boolean sendSwipe(SwipeEvent type) {
		return mSwipeableView.onViewSwipe(this, type, mSwipeOffset, mSwipeablePosition);
	}
	
	/**
	 * Prepare touch down event for a swipeable view action.
	 * 
	 * @param ev
	 *            current touch down event
	 */
	private void setupSwipeableClick(MotionEvent ev) {
		if (mStartAlreadyRequested) {
			return;
		}
		
		mStartAlreadyRequested = true;
		int oldPosition = mSwipeablePosition;
		
		mConsumeClick = false;
		mSwipeX = (int) ev.getX();
		mSwipeY = (int) ev.getY();
		mHasSwipeable = false;
		mSwipeableStarted = false;
		mConsumeSelection = false;
		mSwipeOffset = 0;
		
		mSwipeablePosition = pointToPosition((int) ev.getX(), (int) ev.getY());
		int wantedChild = mSwipeablePosition - getFirstVisiblePosition();
		View v = getChildAt(wantedChild);
		
		SwipeableListItem view = v instanceof SwipeableListItem ? (SwipeableListItem) v : null;
		
		// send close event to old swipeable if it's not the same
		if (mSwipeableView != null && mSwipeablePosition != oldPosition) {
			int newPosition = mSwipeablePosition;
			mSwipeablePosition = oldPosition;
			sendSwipe(SwipeEvent.CLOSE);
			mSwipeablePosition = newPosition;
		}
		
		if (view != null) {
			// start swipe
			mSwipeableView = (SwipeableListItem) view;
			mHasSwipeable = true;
			
			if (sendSwipe(SwipeEvent.START)) {
				mSwipeableStarted = true;
				mConsumeClick = true;
				mConsumeSelection = !mSwipeableView.swipeDoesntHideListSelector();
			}
		} else {
			// it's not a swipeable view which was clicked
			mSwipeableView = null;
			mSwipeablePosition = INVALID_POSITION;
		}
	}
}
