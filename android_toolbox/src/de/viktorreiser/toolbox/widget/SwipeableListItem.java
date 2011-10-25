package de.viktorreiser.toolbox.widget;

import android.widget.ListView;

/**
 * Swipeable list item view.<br>
 * <br>
 * A view which want to use the technique described by {@link SwipeableListView} should implement
 * this interface and follow the rules descried here.<br>
 * <br>
 * The whole process of swipe is controlled by {@link #onViewSwipe(ListView, SwipeEvent, int, int)}
 * and the given {@link SwipeEvent}. {@link #swipeOnClick()}, {@link #swipeOnLongClick()} and
 * {@link #swipeDoesntHideListSelector()} return some extra settings.<br>
 * {@link #swipeStateReset()} is there for a clean handling.<br>
 * <br>
 * See {@link SwipeEvent} for a description of events and how to handle them.<br>
 * <br>
 * Further there is {@link SwipeableSetup} as helper for developing swipeable views.<br>
 * <br>
 * <b>Note</b>: This view itself can only listen for touch events in range of
 * {@link android.view.ViewConfiguration#getTouchSlop()}. Vertical move will be intercepted for list
 * scroll and horizontal move will be intercepted for swipe. So in the end you can listen for clicks
 * only since move action will e canceled after a certain offset.
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public interface SwipeableListItem {
	
	/**
	 * Base class for swipeable view configuration (as helper for an own swipeable view
	 * implementation).<br>
	 * <br>
	 * It's your decision whether you use it for your own implementation of a swipeable or not. It
	 * just simplifies the configuration of the common values for a swipeable view.<br>
	 * <br>
	 * You could create an own swipeable view and pass an object of a class which extends this one
	 * to it. So you have full control over the settings in an central object and don't need to set
	 * it on each view. Besides that you can share central objects between different swipeable views
	 * (layouts, images, etc).<br>
	 * <br>
	 * <b>Example</b>: (You don't have to lock your setup or make the setup single, it's just an
	 * example. If your swipeable view gets along with changes its fine. You could also lock setup
	 * on a certain swipeable view state. <i>Note</i>: Only some configurations are locked in this
	 * base class, see it's documentation.)<br>
	 * 
	 * <pre>
	 * public void class MySwipable extends {@link SwipeableListItem} {
	 * 
	 * 	public static class</b> MySetup extends {@link SwipeableSetup} {
	 * 
	 * 		protected i;
	 * 
	 * 		public void setValue(int i) {
	 * 			{@link #checkChangesLock()};
	 * 			this.i = i;
	 * 		}
	 * 
	 * 		// We don't need that
	 * 		&#64;Override
	 * 		public void {@link #setStickyStart(boolean s)} {
	 * 			throw new {@link UnsupportedOperationException}("not supported");
	 * 		}
	 * 	}
	 * 
	 * 	private MySetup setup = null;
	 * 
	 * 	public void setSetup(MySetup setup) {
	 * 		if (this.setup != null) {
	 * 			throw new {@link IllegalStateException}("Setup already set");
	 * 		}
	 * 
	 * 		// lock setup object so important values can't be modified anymore
	 * 		setup.{@link #lockChanges} = true;
	 * 		this.setup = setup;
	 * 	}
	 * 
	 * 	// ...
	 * 	// your implementation which uses given setup (e.g. by setup.{@link #startOffset})
	 * 	// ...
	 * 
	 * }
	 * </pre>
	 * 
	 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
	 */
	public static abstract class SwipeableSetup {
		
		// PRIVATE ----------------------------------------------------------------------------
		
		/** Flag which locks setup for changes so {@link #checkChangesLock()} will throw. */
		protected boolean lockChanges = false;
		
		
		/** {@link #setStartOffset(int)} as absolute value (default {@code 50}). */
		protected int startOffset = 50;
		
		/** {@link #setStopOffset(int)} as absolute value (default {@code 100}). */
		protected int stopOffset = 100;
		
		/** {@link #setAnimationSpeed(int)} always {@code >= 0} (default {@code 500}). */
		protected int animationSpeed = 500;
		
		/** {@link #setStickyStart(boolean)} (default {@code true}). */
		protected boolean stickyStart = true;
		
		/** {@link #setSwipeOnClick(boolean)} (default {@code false}). */
		protected boolean consumeClick = false;
		
		/** {@link #setSwipeOnLongClick(boolean)} (default {@code false}) */
		protected boolean consumeLongClick = false;
		
		/** {@link #setHideSelectorOnStart(boolean)} but <b>inverted</b> (default {@code false}). */
		protected boolean dontHideSelector = false;
		
		// PUBLIC -----------------------------------------------------------------------------
		
		/**
		 * Set offset in pixel until the swipe starts.<br>
		 * <br>
		 * Offset until SwipeableListItem#onViewSwipe(ListView, SwipeEvent, int, int) returns
		 * {@code true}.<br>
		 * <br>
		 * <i>Is locked after the setup is attached to a swipeable view.</i>
		 * 
		 * @param startOffset
		 *            offset in pixel until the swipe starts
		 */
		public void setStartOffset(int startOffset) {
			checkChangesLock();
			this.startOffset = Math.abs(startOffset);
		}
		
		/**
		 * Set offset in pixel when an released (touch up) swipe should perform an action.<br>
		 * <br>
		 * This should be desirable greater than the given start offset but it doesn't have to. What
		 * the action should be is the decision of the implementation of the swipeable view (it
		 * could slide to the <i>end</i> when touch passed this stop offset).<br>
		 * <br>
		 * <i>Is locked after the setup is attached to a swipeable view.</i>
		 * 
		 * @param stopOffset
		 *            offset in pixel when an released (touch up) swipe should perform an action
		 */
		public void setStopOffset(int stopOffset) {
			checkChangesLock();
			this.stopOffset = Math.abs(stopOffset);
		}
		
		/**
		 * Should swipeable view jump the offset given with {@link #setStartOffset(int)} on start?
		 * 
		 * @param stickyStart
		 *            {@code true} when swipeable view should simulate a sticky start
		 */
		public void setStickyStart(boolean stickyStart) {
			this.stickyStart = stickyStart;
		}
		
		/**
		 * Should view perform swipe action on click?
		 * 
		 * @param swipeOnClick
		 *            {@code true} if view should perform swipe action on click
		 * 
		 * @see SwipeableListItem#swipeOnClick()
		 */
		public void setSwipeOnClick(boolean swipeOnClick) {
			consumeClick = swipeOnClick;
		}
		
		/**
		 * Should view perform swipe action on long click?
		 * 
		 * @param swipeOnLongClick
		 *            {@code true} if view should perform swipe action on long click
		 * 
		 * @see SwipeableListItem#swipeOnLongClick()
		 */
		public void setSwipeOnLongClick(boolean swipeOnLongClick) {
			consumeLongClick = swipeOnLongClick;
		}
		
		/**
		 * Should list hide selector when swipe of view starts?
		 * 
		 * @param hideSelector
		 *            {@code true} if selector should be hidden or {@code false} if it should remain
		 *            visible on swipe
		 * 
		 * @see SwipeableListItem#swipeDoesntHideListSelector()
		 */
		public void setHideSelectorOnStart(boolean hideSelector) {
			dontHideSelector = !hideSelector;
		}
		
		/**
		 * Set amount of millisecond which the swipeable needs to perform a complete animation.<br>
		 * <br>
		 * <i>Is locked after the setup is attached to a swipeable view.</i>
		 * 
		 * @param animationSpeed
		 *            amount of millisecond which the swipeable needs to perform a complete
		 *            animation
		 */
		public void setAnimationSpeed(int animationSpeed) {
			checkChangesLock();
			this.animationSpeed = animationSpeed < 0 ? 0 : animationSpeed;
		}
		
		// PRIVATE ----------------------------------------------------------------------------
		
		/**
		 * Check whether setup object is locked for changes.<br>
		 * <br>
		 * This should be used in extending class in own configuration setters.<br>
		 * {@link SwipeableSetup} describes provides a sample pattern.
		 */
		protected void checkChangesLock() {
			if (lockChanges) {
				throw new IllegalStateException(getClass().getSimpleName()
						+ " has been locked! Usually this happens if you already set the "
						+ " setup object and try to modify it after that.");
			}
		}
	}
	
	
	/* @formatter:off */
	/**
	 * State events for swipeable list item view.<br>
	 * <br>
	 * Take a look at {@link SwipeableListItem#swipeStateReset()} too.<br>
	 * <br>
	 * <table border="1" cellpadding="5">
	 * <tr>
	 * <td><pre>{@link #START}</pre></td>
	 * <td>This is always the first event when a swipe begins (touch down). View should begin
	 * swipe action when {@link SwipeableListItem#onViewSwipe} returns {@code true} the first
	 * time. As long it returns {@code false} the list handles click and long click events.</td>
	 * </tr>
	 * <tr>
	 * <td><pre>   {@link #MOVE}</pre></td>
	 * <td>Indicates a touch move and reports an offset which should be used to animate the
	 * swipe. Return {@code true} in {@link SwipeableListItem#onViewSwipe} here to start the swipe
	 * and so tell the list view that it should stop handling the click events.</td>
	 * </tr>
	 * <tr>
	 * <td><pre>      {@link #STOP}</pre></td>
	 * <td>{@link SwipeableListItem#onViewSwipe} did return {@code true} and indicated a swipe
	 * start. Now the user ended swipe (touch up) and the view should decide based on the given
	 * offset what the final result of swipe will be.</td>
	 * </tr>
	 * <tr>
	 * <td><pre>      {@link #CANCEL}</pre></td>
	 * <td>View should animate a swipe reset because list decided to cancel the swipe. (Of course
	 * there should be nothing to do as long {@link SwipeableListItem#onViewSwipe} dindn't
	 * return {@code true} yet.)</td>
	 * </tr>
	 * <tr>
	 * <td><pre>      {@link #CLICK}</pre>
	 * <pre>      {@link #LONG_CLICK}</pre></td>
	 * <td>Show swiped view because view requested to consume the given event. This event will
	 * only be reported as long {@link SwipeableListItem#onViewSwipe} didn't return {@code true}
	 * and {@link SwipeableListItem#swipeOnClick()} and/or
	 * {@link SwipeableListItem#swipeOnLongClick()} returns {@code true}. </td>
	 * </tr>
	 * <tr>
	 * <td><pre>         {@link #CLOSE}</pre></td>
	 * <td>Swipe events handled and another (!) view gets focus. This view should clean it's
	 * state in an animated way (for example if it is in a swiped state). The difference to
	 * {@link #CANCEL} is that a close is called just before interaction with another
	 * swipeable view begins.</td>
	 * </tr>
	 * </table>
	 * 
	 * @author Viktor Reiser &lt;<a
	 *         href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
	 */
	/* @formatter:on */
	public static enum SwipeEvent {
		/** Swipe event was started. */
		START,
		/** Swipe is still held and moved (offset changes). */
		MOVE,
		/** Swipe stopped because finger lifted (offset is final). */
		STOP,
		/**
		 * Swipe was canceled because list is performing another action now.<br>
		 * <br>
		 * <b>Note</b>: This is a soft cancel which tells the view to softly reverse swipe which was
		 * began to perform.
		 */
		CANCEL,
		/**
		 * View requested a click report and here it is.<br>
		 * <br>
		 * This blocked the default click operation.<br>
		 * {@link #CANCEL} is not called before this.
		 * 
		 * @see SwipeableListItem#swipeOnClick()
		 */
		CLICK,
		/**
		 * View requested a context click report and here it is.<br>
		 * <br>
		 * This blocked the default context click operation.<br>
		 * {@link #CANCEL} is not called before this.
		 * 
		 * @see SwipeableListItem#swipeOnLongClick()
		 */
		LONG_CLICK,
		/**
		 * List interacted with this view before but now focused on a different task.<br>
		 * <br>
		 * It's like view is loosing focus. It should clean its state which was caused by previous
		 * swipe interaction. Like {@link #CANCEL} this event is soft.
		 */
		CLOSE,
		RESTORE
	}
	
	
	/**
	 * List view callback with events which helps to perform swipe action on view item.
	 * 
	 * @param listView
	 *            list view which contains this view
	 * @param event
	 *            swipe event
	 * @param offset
	 *            offset in pixel of current swipe (so {@code 0} on {@link SwipeEvent#START})
	 * @param position
	 *            position of view in list
	 * 
	 * @return first return of {@code true} indicates that view started to perform swipe action -
	 *         usually swipe is started after a certain offset movement (e.g.
	 *         {@code Math.abs(offset) > 50}, this enables the list to perform click and long click
	 *         actions - return of {@code true} on {@link SwipeEvent#START} will begin the swipe
	 *         action immediately and consume any clicks and long clicks instantly
	 */
	public boolean onViewSwipe(ListView listView, SwipeEvent event, int offset, int position,
			SwipeableListItem restoreItem);
	
	/**
	 * Reset (immediately) any prior swipe state so it looks like this view hasn't ever been swiped.<br>
	 * <br>
	 * While {@link SwipeEvent#STOP}, {@link SwipeEvent#CANCEL} and {@link SwipeEvent#CLOSE} softly
	 * dismiss the swipe state this method restores the normal state by canceling running dismiss
	 * animations and do it the hard way (for example when view is recycled by list). It would be
	 * awkward if a <i>different</i> view has a swipeable state which was never performed on it.
	 */
	public void swipeStateReset();
	
	/**
	 * Should swipeable view consume click?<br>
	 * <br>
	 * Return {@code true}, receive {@link SwipeEvent#CLICK} and perform swipe action.<br>
	 * Default item click will be completely consumed!<br>
	 * You can do whatever you want on click, but it's obvious to perform a swipe action.<br>
	 * It's uncommon to return {@code true} here but {@code false} on {@link #swipeOnLongClick()}
	 * but you can do that anyway.
	 * 
	 * @return {@code true} if click should be consumed
	 */
	public boolean swipeOnClick();
	
	/**
	 * Should swipeable view consume long click?<br>
	 * <br>
	 * Return {@code true}, receive {@link SwipeEvent#LONG_CLICK} and perform swipe action.<br>
	 * Default item long click will be completely consumed!<br>
	 * You can do whatever you want on long click, but it's obvious to perform a swipe action.
	 * 
	 * @return {@code true} if long click should be consumed
	 */
	public boolean swipeOnLongClick();
	
	/**
	 * Should list not hide the selector when swipe action is started.<br>
	 * <br>
	 * {@code true} will show the list selector every time the finger touches the swipeable and not
	 * display it permanently.
	 * 
	 * (Sorry for the double negation, but this way the the default implementation hides the
	 * selector which is the more desirable behavior ;-)<br>
	 * <br>
	 * <b>Note</b>: Be aware of the fact that an not hidden selector does not move along with the
	 * swipe (but has the same size and position as the view which is currently touched).
	 * 
	 * @return {@code false} will hide the list selector on first return of {@code true} of
	 *         {@link #onViewSwipe} and {@code true} won't
	 */
	public boolean swipeDoesntHideListSelector();
}