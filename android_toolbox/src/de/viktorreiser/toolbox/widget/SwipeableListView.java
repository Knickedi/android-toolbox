package de.viktorreiser.toolbox.widget;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

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
import de.viktorreiser.toolbox.widget.SwipeableListItem.SwipeEvent;

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
	
	// reflection hack to force list selector clearance
	private static Field mSelectorRect = null;
	
	static {
		try {
			mSelectorRect = AbsListView.class.getDeclaredField("mSelectorRect");
			mSelectorRect.setAccessible(true);
		} catch (Exception e) {
		}
	}
	
	
	private int mRestorePosition = INVALID_POSITION;
	
	private int mStartX;
	private int mStartY;
	private int mStartOffset;
	
	private SwipeableListItem mSwipeableView;
	
	private boolean mSwipeStarted = false;
	private int mSwipeablePosition = INVALID_POSITION;
	
	private boolean mConsumeClick = false;
	boolean mConsumeSelection = false;
	boolean mCancelSwipeOnFocusLoss = false;
	
	private Map<SwipeableListItem, Integer> mCachedPositions =
			new HashMap<SwipeableListItem, Integer>();
	
	private OnCreateContextMenuListener mCreateContextMenuListener = null;
	private DataSetObserver mChangeObserver = new DataSetObserver() {
		@Override
		public void onChanged() {
			if (mSwipeableView != null) {
				if (mSwipeStarted) {
					// onScroll will try to recover current swipe
					mRestorePosition = mSwipeablePosition;
				} else {
					mSwipeableView.swipeStateReset();
					mSwipeableView = null;
				}
			}
		}
	};
	
	// PUBLIC =====================================================================================
	
	/**
	 * Should any swipe be canceled if list loses focus (default {@code false}).<br>
	 * <br>
	 * Focus loss happens e.g. when a dialog pops up. Maybe you wan't to perform something like
	 * that when the user performs a (long) click.
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
			if (mSwipeStarted) {
				// onScroll will try to recover current swipe
				mRestorePosition = mSwipeablePosition;
			} else {
				mSwipeableView.swipeStateReset();
				mSwipeableView = null;
			}
		}
		
		if (getAdapter() != null) {
			// detach data observer from old adapter
			getAdapter().unregisterDataSetObserver(mChangeObserver);
		}
		
		if (adapter != null) {
			// attach to new adapter
			adapter.registerDataSetObserver(mChangeObserver);
		}
		
		super.setAdapter(adapter);
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public void setOnScrollListener(final OnScrollListener l) {
		if (l == null || l == this) {
			super.setOnScrollListener(this);
		} else {
			// merge given scroll listener with our own
			super.setOnScrollListener(new OnScrollListener() {
				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) {
					SwipeableListView.this.onScrollStateChanged(view, scrollState);
					l.onScrollStateChanged(view, scrollState);
				}
				
				@Override
				public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
						int totalItemCount) {
					SwipeableListView.this.onScroll(
							view, firstVisibleItem, visibleItemCount, totalItemCount);
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
		
		// track all swipeable views and reset their swipe states if they don't point to
		// the same position anymore -> this resets recycled swipeable views (for reuse)
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
			// scroll state cancels swipeable view (list scroll is performed)
			cancelSwipe();
		}
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public void setOnCreateContextMenuListener(OnCreateContextMenuListener l) {
		// merge create context listener with our own
		mCreateContextMenuListener = (l == this ? null : l);
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (mSwipeableView != null) {
			if (mSwipeStarted) {
				// swipeable started, consume all click events
				mConsumeClick = true;
				return;
			} else if (mSwipeableView.swipeOnLongClick()) {
				// swipeable requests context click event and consumes it
				sendSwipe(SwipeEvent.LONG_CLICK);
				mSwipeStarted = mConsumeClick = true;
				mConsumeSelection = !mSwipeableView.swipeDoesntHideListSelector();
				invalidate();
				return;
			}
		}
		
		// inform external listener if it's set
		if (mCreateContextMenuListener != null) {
			mCreateContextMenuListener.onCreateContextMenu(menu, v, menuInfo);
		}
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public boolean performItemClick(View view, int position, long id) {
		// swipeable consumes item click
		if (mConsumeClick) {
			return false;
		}
		
		// swipeable requests click event and consumes it
		if (mSwipeableView != null && mSwipeableView.swipeOnClick()) {
			sendSwipe(SwipeEvent.CLICK);
			mSwipeStarted = false;
			return false;
		}
		
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
		return handleTouchEvent(ev, false);
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		return handleTouchEvent(ev, true);
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public void dispatchDraw(Canvas canvas) {
		if (mConsumeSelection) {
			// list insists on drawing the selector
			// cancel this in last possible moment before actual draw
			
			setPressed(false);
			
			if (mSwipeableView != null) {
				((View) mSwipeableView).setPressed(false);
			}
			
			try {
				((Rect) mSelectorRect.get(this)).setEmpty();
			} catch (Exception e) {
			}
		}
		
		super.dispatchDraw(canvas);
	}
	
	// PRIVATE ====================================================================================
	
	private void initialize() {
		super.setOnScrollListener(this);
		super.setOnCreateContextMenuListener(this);
	}
	
	private boolean handleTouchEvent(MotionEvent ev, boolean intercept) {
		mStartOffset = (int) ev.getX() - mStartX;
		int action = ev.getAction();
		
		
		if (action == MotionEvent.ACTION_DOWN) {
			if (intercept) {
				setupSwipeableClick(ev);
				super.onInterceptTouchEvent(ev);
			} else {
				// let the non intercept call always wont to take control
				super.onTouchEvent(ev);
			}
			
		} else if (action == MotionEvent.ACTION_MOVE) {
			// this calculated slop will always trigger the list to scroll
			// so a started swipe should be canceled anyway
			if (mSwipeableView != null
					&& Math.abs((int) ev.getY() - mStartY) > ViewConfiguration.getTouchSlop()
							* getContext().getResources().getDisplayMetrics().density + 0.5f * 2) {
				cancelSwipe();
				return super.onTouchEvent(ev);
			}
			
			if (intercept) {
				if (mSwipeableView != null) {
					if (sendSwipe(SwipeEvent.MOVE)) {
						// we have a swipeable view ready and it reports a start
						// remember that, clear selection if needed and intercept
						mSwipeStarted = mConsumeClick = true;
						mConsumeSelection = !mSwipeableView.swipeDoesntHideListSelector();
						
						if (!mConsumeSelection) {
							ev.setAction(MotionEvent.ACTION_DOWN);
							super.onTouchEvent(ev);
							ev.setAction(action);
						}
					}
				} else {
					// let the list take control of everything else
					if (super.onInterceptTouchEvent(ev)) {
						cancelSwipe();
						return true;
					} else {
						return false;
					}
				}
			} else {
				if (mSwipeableView == null) {
					// no swipeable view (or already cancel) - just call the list touch routine
					return super.onTouchEvent(ev);
				} else if (mSwipeStarted) {
					// swipe started, report move
					sendSwipe(SwipeEvent.MOVE);
				} else if (sendSwipe(SwipeEvent.MOVE)) {
					// swipe started, remember that and clear selection if needed
					mSwipeStarted = mConsumeClick = true;
					mConsumeSelection = !mSwipeableView.swipeDoesntHideListSelector();
					return true;
				} else {
					// let the list take control of everything else
					return super.onTouchEvent(ev);
				}
			}
		} else if (action == MotionEvent.ACTION_UP) {
			if (mSwipeStarted) {
				sendSwipe(SwipeEvent.STOP);
			} else {
				mConsumeClick = false;
			}
			
			if (intercept) {
				super.onInterceptTouchEvent(ev);
			} else {
				super.onTouchEvent(ev);
			}
		} else if (action == MotionEvent.ACTION_CANCEL) {
			cancelSwipe();
			mConsumeClick = false;
			return super.onTouchEvent(ev);
		}

		return intercept ? mSwipeStarted : true;
	}
	
	/**
	 * Send {@link SwipeEvent#CANCEL} if {@link #mSwipeStarted}.
	 */
	private void cancelSwipe() {
		if (mSwipeStarted) {
			sendSwipe(SwipeEvent.CANCEL);
			mSwipeStarted = false;
			mSwipeableView = null;
		}
	}
	
	/**
	 * Try to restore swipeable by given {@link #mRestorePosition}.
	 */
	private void restoreSwipe() {
		if (mRestorePosition == INVALID_POSITION || mSwipeableView == null) {
			return;
		}
		
		int wantedChild = mSwipeablePosition - getFirstVisiblePosition();
		View v = getChildAt(wantedChild);
		
		if (wantedChild < 0 || wantedChild >= getChildCount()
				|| !(v instanceof SwipeableListItem)) {
			// nothing to restore
			mSwipeStarted = false;
			mSwipeableView = null;
		} else {
			SwipeableListItem previous = mSwipeableView;
			mSwipeableView = (SwipeableListItem) v;
			mSwipeableView.swipeStateReset();
			mSwipeablePosition = mRestorePosition;
			
			if (mSwipeableView.onViewSwipe(this, SwipeEvent.RESTORE, mStartOffset,
					mSwipeablePosition, previous)) {
				mCachedPositions.put(mSwipeableView, mRestorePosition);
			} else  {
				mSwipeableView = null;
			}
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
		return mSwipeableView.onViewSwipe(this, type, mStartOffset, mSwipeablePosition, null);
	}
	
	/**
	 * Prepare touch down event for a swipeable view action.
	 * 
	 * @param ev
	 *            current touch down event
	 */
	private void setupSwipeableClick(MotionEvent ev) {
		int oldPosition = mSwipeablePosition;
		
		mStartX = (int) ev.getX();
		mStartY = (int) ev.getY();
		mSwipeStarted = false;
		mConsumeSelection = false;
		mStartOffset = 0;
		
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
			
			if (sendSwipe(SwipeEvent.START)) {
				mConsumeClick = mSwipeStarted = true;
				mConsumeSelection = !mSwipeableView.swipeDoesntHideListSelector();
			}
		} else {
			// it's not a swipeable view which was clicked
			mSwipeableView = null;
			mSwipeablePosition = INVALID_POSITION;
		}
	}
}
