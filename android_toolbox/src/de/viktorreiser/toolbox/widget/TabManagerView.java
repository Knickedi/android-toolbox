package de.viktorreiser.toolbox.widget;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.graphics.drawable.StateListDrawable;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.viktorreiser.toolbox.R;

/**
 * Full customizable tab manager.<br>
 * <br>
 * This might the tab manager a developer always wished for because the default layout manager lacks
 * the features which were implemented here.
 * <ul>
 * <li>First thing to say is that this tab manager doesn't support activities as views. But most of
 * the time this seams not be a feature but an overhead which causes trouble. This tab manager
 * supports views only but you can still keep it dynamic since you can remove and add tabs at
 * runtime and so change the content of a tab.</li>
 * <li>You have the ability to indicate a loading status of a tab (indicator at to top left corner
 * of tab image - this might look awkward if no tab image is defined but it will work).</li>
 * <li>You can indicate a tab status (e.g. a message count or something like new update etc.) by
 * adding an status icon to it (indicator at top right corner of tab image - this might look awkward
 * if no tab image is defined but it will work).</li>
 * <li>Tab manager can define a minimum width for a tab and so you get a scrollable tab bar.</li>
 * <li>Finally, if you don't like the default theme you can <i>easily</i> define your own or use
 * {@link DefaultLayoutSetup} to modify the default layout.</li>
 * </ul>
 * <b>How to define a custom tab layout</b> (see further below for default tab layout definition):<br>
 * <br>
 * The obvious things to do is to call following methods (custom XML attribute in brackets)<br>
 * <ul>
 * <li>{@link #setTabContentSeparator(int)} ({@code tabContentseparator}) - define a separator view
 * between the tabs and the content</li>
 * <li>{@link #setTabSeparator(int)} ({@code tabSeparator}) - define a separator view between tabs</li>
 * <li>{@link #setTabMinimumWidth(int)} ({@code tabScrollEdgeWidth}) - set minimal width of a single
 * tab</li>
 * <li>{@link #setTabScrollEdgeColor(int)} ({@code tabScrollEdgeColor}) - set color of scroll edge
 * visualization when (only has effect if minimal width is defined)</li>
 * <li>{@link #setTabScrollEdgeWidth(int)} ({@code tabMinimumWidth}) - set width of scroll edge
 * visualization</li>
 * <li>Last thing to to is to call {@link #setTabLayout(int)} ({@code tabLayout}) but there are some
 * rules and behaviors you have to know about</li>
 * </ul>
 * <ul>
 * <li>The view which contains the title has to be a {@code TextView} with the ID
 * {@code android.R.id.title}</li>
 * <li>The view which contains the image has to be a {@code ImageView} with the ID
 * {@code android.R.id.icon}</li>
 * <li>The view which contains the loading indicator can be any {@code View} with the ID
 * {@code android.R.id.icon1}</li>
 * <li>The view which contains the status image has to be a {@code ImageView} with the ID
 * {@code android.R.id.icon2}</li>
 * </ul>
 * Every view is optional and will be set if it is available. But you will get an exception if you
 * define one of the IDs on a wrong view. Everytime when an information is set (or removed) the view
 * will become visible (or gone again). This could be used to define a very flexible layout.<br>
 * <br>
 * Tab layout will be stretched in height so every tab will take the same amount of height as the
 * biggest one. It will also be stretched in width so e.g. one single tab can fit the whole tab
 * manager width. It will also be shrunk to fit the tab manager width (but remember minimum tab
 * width, this will make the tab bar scrollable if the tabs don't fit and show the scroll edges).
 * The tab supports the states normal, pressed and selected.<br>
 * <br>
 * <b>Default layout definition</b> (it's generated programmatically to avoid overhead but uses the
 * same techniques in the end and doesn't produce something you couldn't).<br>
 * <br>
 * {@code color_selector} is a {@code selector} drawable resource which defines white color for
 * normal state and black color for pressed state.<br>
 * <br>
 * {@code background_selector} is a {@code selector} drawable resource which defines top bottom
 * gradients for the states. Normal state is {@code 0xff444444} - {@code 0xff000000} . Pressed state
 * is {@code 0xff888888} - {@code 0xff444444}. Selected state is {@code 0xffffffff} -
 * {@code 0xffbbbbbb}.<br>
 * <br>
 * Scroll edge and tab content separator color is {@code 0xffbbbbbb}. Tab content separator has a
 * height of 2dip.<br>
 * <br>
 * Minimum width of a tab is 40dip, so at least the tab image and the overlay icons are visible and
 * have a padding to the tab edge.<br>
 * <br>
 * The definition of the tab seams to be complicated (in fact it is). You never have to define such
 * a layout on your own, because you'll know what you will need and use. This layout expects the
 * worst case: Developer uses every possible constellation (tab image without title, title and
 * loading, only title, etc.). So this layout has the aim to work in any case. There are some
 * comments which try to explain the clue and thoughts behind it.
 * 
 * <pre>
 * {@code <!--
 * gravity - in case a tab image is missing tab should be aligned to bottom
 * padding (no bottom) - bottom padding will come from image, title or overlay images
 *                       this ensures a correct layout if one of the view is not used
 * -->
 * <LinearLayout
 * 	xmlns:android="http://schemas.android.com/apk/res/android"
 * 	android:layout_height="fill_parent"
 * 	android:layout_width="fill_parent"
 * 	android:orientation="vertical"
 * 	android:gravity="bottom"
 * 	android:paddingTop="3dip"
 * 	android:paddingLeft="3dip"
 * 	android:paddingRight="3dip">
 * 
 * 	<!-- width - image (20) + 2 * overlay offset (7)  -->
 * 	<FrameLayout
 * 		android:layout_width="34dip"
 * 		android:layout_height="wrap_content"
 * 		android:layout_gravity="center_horizontal">
 * 
 * 		<!--
 * 		height - image (20) + overlay offset (7) + bottom margin (3)
 * 		padding bottom - space to tab bottom (see root element) or to title
 * 		padding top - overlay offset
 * 		-->
 * 		<ImageView
 * 			android:id="@android:id/icon"
 * 			android:layout_gravity="bottom|center_horizontal"
 * 			android:layout_width="20dip"
 * 			android:layout_height="30dip"
 * 			android:paddingBottom="3dip"
 * 			android:paddingTop="7dip"
 * 			android:background="@drawable/background_selector"
 * 			android:visibility="gone" />
 * 
 * 		<!--
 * 		height - overlay size (14) + bottom margin (3)
 * 		padding bottom - space to tab bottom or to title (in case image is missing)
 * 		-->
 * 		<PogressBar
 * 			android:id="@android:id/icon1"
 * 			style="@android:style/Widget.ProgressBar.Small"
 * 			android:layout_width="14dip"
 * 			android:layout_height="17dip"
 * 			android:paddingBottom="3dip"
 * 			android:visibility="gone" />
 * 
 * 		<!-- same as progress bar -->
 * 		<ImageView
 * 			android:id="@android:id/icon2"
 * 			android:layout_gravity="top|right"
 * 			android:layout_width="14dip"
 * 			android:layout_height="17dip"
 * 			android:paddingBottom="3dip"
 * 			android:visibility="gone" />
 * 
 * 	</FrameLayout>
 * 	
 * 	<!-- margin bottom - needed because root expects the children to do that -->
 * 	<TextView
 * 		android:id="@android:id/title"
 * 		android:layout_width="wrap_content"
 * 		android:layout_height="wrap_content"
 * 		android:textColor="@drawable/color_selector"
 * 		android:marginBottom="3dip"
 * 		android:visibility="gone"
 * 		android:singleLine="true"
 * 		android:ellipsize="marquee" />
 * 
 * </LinearLayout>}
 * </pre>
 * 
 * <i>Depends on</i>: {@code res/values/attrs.xml}
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class TabManagerView extends LinearLayout {
	
	// PRIVATE ====================================================================================
	
	private boolean mChangesLock = false;
	private LayoutInflater mLayoutInflater = (LayoutInflater) getContext()
			.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	
	private DefaultLayoutSetup mDefaultLayoutSetup = new DefaultLayoutSetup();
	private int mTabLayout;
	private int mTabSeparator;
	private int mTabScrollEdgeColor = 0x13579bdf;
	private int mTabScrollEdgeWidth = (int) (15 * getContext().getResources()
			.getDisplayMetrics().density + 0.5);
	private OnTabChangeListener mTabChangeListener;
	private int mTabMinimumWidth = -1;
	
	private LinearLayout mTabContainer;
	private List<TabData> mTabData = new ArrayList<TabData>();
	private int mCurrentTabPosition = -1;
	
	// PUBLIC =====================================================================================
	
	/**
	 * Add tab.<br>
	 * <br>
	 * Any layout changes will be used for the tab definition. So you can define another layout for
	 * every tab definition. It's unusual but you could do that (e.g. a grean tab, a blue tab, etc).
	 * 
	 * @param tab
	 *            tab definition (expects content to be set)
	 */
	public void addTab(Tab tab) {
		// check for needed information
		if (tab.content == null) {
			throw new NullPointerException("Content not defined!");
		}
		
		if (tab.tag != null) {
			for (TabData data : mTabData) {
				if (tab.tag.equals(data.tag)) {
					throw new IllegalArgumentException("Tab tag " + tab.tag + " already defined!");
				}
			}
		}
		
		int tabCount = getTabCount();
		
		TabData tabData = new TabData();
		tabData.tag = tab.tag;
		tabData.content = tab.content;
		
		// add tab content and ID
		if (tab.position >= tabCount || tab.position <= -1) {
			tab.position = -1;
			mTabData.add(tabData);
		} else {
			mTabData.add(tab.position, tabData);
		}
		
		// run first setup when first tab is added
		if (!mChangesLock) {
			if (mTabLayout == 0) {
				DefaultLayoutSetup setup = mDefaultLayoutSetup;
				
				if (getChildCount() == 1) {
					View separator = new View(getContext());
					separator.setLayoutParams(new LinearLayout.LayoutParams(
							LayoutParams.FILL_PARENT, (int)
							setup.tabContentSeparatorHeight));
					separator.setBackgroundColor(setup.tabContentSeperatorColor);
					setTabContentSeparator(separator);
				}
				
				if (mTabMinimumWidth == -1) {
					mTabMinimumWidth = setup.image + 2 * setup.overlayOffset + 2 * setup.border;
				}
				
				if (mTabScrollEdgeColor == 0x13579bdf) {
					mTabScrollEdgeColor = 0xffbbbbbb;
				}
			} else if (mTabScrollEdgeColor == 0x13579bdf) {
				mTabScrollEdgeColor = 0xff000000;
			}
			
			int [] colors = new int [] {mTabScrollEdgeColor, 0};
			
			View leftGradient = new View(getContext());
			leftGradient.setBackgroundDrawable(
					new GradientDrawable(Orientation.LEFT_RIGHT, colors));
			
			View rightGradient = new View(getContext());
			rightGradient.setBackgroundDrawable(
					new GradientDrawable(Orientation.RIGHT_LEFT, colors));
			
			FrameLayout tabScrollView = (FrameLayout) getChildAt(0);
			tabScrollView.addView(mTabContainer, new FrameLayout.LayoutParams(
					LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
			tabScrollView.addView(leftGradient, new FrameLayout.LayoutParams(0, 0, Gravity.LEFT));
			tabScrollView.addView(rightGradient, new FrameLayout.LayoutParams(0, 0, Gravity.RIGHT));
		}
		
		mChangesLock = true;
		
		// create tab
		View child;
		
		if (mTabLayout == 0) {
			child = generateDefaultTabProgrammatically();
		} else {
			child = mLayoutInflater.inflate(mTabLayout, mTabContainer, false);
		}
		
		LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		cp.weight = 1f;
		child.setLayoutParams(cp);
		
		View image = child.findViewById(android.R.id.icon);
		View title = child.findViewById(android.R.id.title);
		
		// setup tab image
		if (tab.image != null && image != null) {
			if (!(image instanceof ImageView)) {
				throw new IllegalStateException(
						"View with ID icon is defined but is not an image view!");
			}
			
			image.setVisibility(View.VISIBLE);
			((ImageView) image).setImageDrawable(tab.image);
		}
		
		// setup tab title
		if (tab.title != null && title != null || title instanceof TextView) {
			if (!(title instanceof TextView)) {
				throw new IllegalStateException(
						"View with ID title is defined but is not an text view!");
			}
			
			title.setVisibility(View.VISIBLE);
			((TextView) title).setText(tab.title);
		}
		
		tabData.busy = child.findViewById(android.R.id.icon1);
		
		if (tabData.busy != null) {
			tabData.busy.setVisibility(View.GONE);
		}
		
		View statusView = child.findViewById(android.R.id.icon2);
		
		if (statusView != null) {
			if (!(statusView instanceof ImageView)) {
				throw new IllegalStateException(
						"Tab layout contains status icon but it's not an image view!");
			}
			
			tabData.status = (ImageView) statusView;
			tabData.status.setVisibility(View.GONE);
		}
		
		int count = mTabContainer.getChildCount();
		
		if (tab.position == -1 || count == 0) {
			// append tab
			if (mTabSeparator != 0 && count != 0) {
				View separator = mLayoutInflater.inflate(mTabSeparator, mTabContainer, false);
				LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
						separator.getLayoutParams());
				p.height = LayoutParams.FILL_PARENT;
				mTabContainer.addView(separator, p);
			}
			
			mTabContainer.addView(child);
		} else {
			// put tab to given position
			tab.position = mTabSeparator == 0 ? tab.position : tab.position * 2;
			
			mTabContainer.addView(child, tab.position);
			
			if (mTabSeparator != 0 && count != 0) {
				View separator = mLayoutInflater.inflate(mTabSeparator, mTabContainer, false);
				LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
						separator.getLayoutParams());
				p.height = LayoutParams.FILL_PARENT;
				mTabContainer.addView(separator, tab.position + 1, p);
			}
		}
		
		if (getTabCount() == 1) {
			setCurrentTab(0);
		}
	}
	
	/**
	 * Remove added tab.
	 * 
	 * @param position
	 *            tab position
	 * 
	 * @return {@code true} if tab removed, {@code false} if tab position is invalid
	 */
	public boolean removeTab(int position) {
		if (position < 0 || position >= getTabCount()) {
			return false;
		}
		
		if (mTabSeparator == 0) {
			mTabContainer.removeViewAt(position);
		} else {
			mTabContainer.removeViewAt(position * 2);
			
			if (getTabCount() != 1) {
				if (position == getTabCount()) {
					mTabContainer.removeViewAt(position * 2 - 1);
				} else {
					mTabContainer.removeViewAt(position * 2);
				}
			}
		}
		
		mTabData.remove(position);
		super.removeViewAt(getChildCount() - 1);
		
		if (getTabCount() == 0) {
			mCurrentTabPosition = -1;
		} else {
			setCurrentTab(0);
		}
		
		return true;
	}
	
	/**
	 * Remove added tab.
	 * 
	 * @param tag
	 *            tab tag
	 * 
	 * @return {@code true} if tab removed {@code false} if tab position is invalid
	 */
	public boolean removeTab(String tag) {
		return removeTab(getTabPosition(tag));
	}
	
	/**
	 * Get amount of available tabs.
	 * 
	 * @return amount of available tabs
	 */
	public int getTabCount() {
		return mTabData.size();
	}
	
	/**
	 * Get content view of tab.
	 * 
	 * @param position
	 *            tab position
	 * 
	 * @return content view or {@code null} if position is invalid
	 */
	public View getTabView(int position) {
		if (position < 0 || position >= getTabCount()) {
			return null;
		}
		
		return mTabData.get(position).content;
	}
	
	/**
	 * Get content view of tab.
	 * 
	 * @param tag
	 *            tab tag
	 * 
	 * @return content view or {@code null} if position is invalid
	 */
	public View getTabView(String tag) {
		return getTabView(getTabPosition(tag));
	}
	
	/**
	 * Set current shown tab.
	 * 
	 * @param position
	 *            tab position
	 * 
	 * @return {@code true} if current tab set, {@code false} if tab position is invalid
	 */
	public boolean setCurrentTab(int position) {
		if (position < 0 || position >= getTabCount()) {
			return false;
		}
		
		if (mCurrentTabPosition == position) {
			return true;
		}
		
		if (mCurrentTabPosition != -1) {
			super.removeViewAt(getChildCount() - 1);
		}
		
		int p = mTabSeparator == 0 ? position : position * 2;
		int count = mTabContainer.getChildCount();
		View child = mTabContainer.getChildAt(p);
		TabScrollView scroll = (TabScrollView) getChildAt(0);
		
		if (child.getLeft() < scroll.mmScrollOffset) {
			scroll.mmScrollOffset = child.getLeft();
		} else if (child.getRight() > scroll.mmScrollOffset + getWidth()) {
			scroll.mmScrollOffset = child.getRight() - getWidth();
		}
		
		for (int i = 0; i < count; i++) {
			mTabContainer.getChildAt(i).setSelected(i == p);
		}
		
		super.addView(mTabData.get(position).content, -1, new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		
		if (mTabChangeListener != null) {
			mTabChangeListener.onTabChange(this, position, getTabTag(position),
					mCurrentTabPosition, getTabTag(mCurrentTabPosition));
		}
		
		mCurrentTabPosition = position;
		
		return true;
	}
	
	/**
	 * Set current shown tab.
	 * 
	 * @param tag
	 *            tab tag
	 * 
	 * @return {@code true} if current tab set, {@code false} if tab position is invalid
	 */
	public boolean setCurrentTab(String tag) {
		return setCurrentTab(getTabPosition(tag));
	}
	
	/**
	 * Get current tab position.
	 * 
	 * @return current tab position or -1 if no tabs available
	 */
	public int getCurrentTab() {
		return mCurrentTabPosition;
	}
	
	/**
	 * Get current tab tag.
	 * 
	 * @return current tab tag or {@code null} if no tabs are available or the tab at this position
	 *         has no tag
	 */
	public String getCurrentTabTag() {
		return mTabData.get(mCurrentTabPosition).tag;
	}
	
	/**
	 * Set or remove the loading indicator for the given tab.
	 * 
	 * @param position
	 *            tab position
	 * @param loading
	 *            {@code true} to set the loading indicator, {@code false} to remove it
	 * 
	 * @return {@code true} if loading indicator set or removed, {@code false} if position is
	 *         invalid or tab layout doesn't support load indicator
	 */
	public boolean setTabLoading(int position, boolean loading) {
		if (position < 0 || position >= getTabCount()) {
			return false;
		}
		
		View busyView = mTabData.get(position).busy;
		
		if (busyView != null) {
			busyView.setVisibility(loading ? View.VISIBLE : View.GONE);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Set or remove the loading indicator for the given tab.
	 * 
	 * @param tag
	 *            tab tag
	 * @param loading
	 *            {@code true} to set the loading indicator, {@code false} to remove it
	 * 
	 * @return {@code true} if loading indicator set or removed, {@code false} if position is
	 *         invalid or tab layout doesn't support load indicator
	 */
	public boolean setTabLoading(String tag, boolean loading) {
		return setTabLoading(getTabPosition(tag), loading);
	}
	
	/**
	 * Is tab currently indicates loading ({@link #setTabLoading(int, boolean)})?
	 * 
	 * @param position
	 *            tab position
	 * 
	 * @return {@code true} if it is loading, {@code false} if not or position is invalid or tab
	 *         layout doesn't support load indicator
	 */
	public boolean isTabLoading(int position) {
		if (position < 0 || position >= getTabCount()) {
			return false;
		}
		
		View busyView = mTabData.get(position).busy;
		
		if (busyView != null) {
			return busyView.getVisibility() == View.VISIBLE;
		}
		
		return false;
	}
	
	/**
	 * Is tab currently indicates loading ({@link #setTabLoading(int, boolean)})?
	 * 
	 * @param tag
	 *            tab tag
	 * 
	 * @return {@code true} if it is loading, {@code false} if not or position is invalid or tab
	 *         layout doesn't support load indicator
	 */
	public boolean isTabLoading(String tag) {
		return isTabLoading(getTabPosition(tag));
	}
	
	/**
	 * Set or remove status icon for given tab.
	 * 
	 * @param position
	 *            tab position
	 * @param drawable
	 *            status drawable or {@code null} to remove status
	 * 
	 * @return {@code true} if status set or removed, {@code false} if position is invalid or tab
	 *         layout doesn't support status
	 */
	public boolean setTabStatus(int position, Drawable drawable) {
		if (position < 0 || position >= getTabCount()) {
			return false;
		}
		
		ImageView statusView = mTabData.get(position).status;
		
		if (statusView != null) {
			statusView.setVisibility(drawable != null ? View.VISIBLE : View.GONE);
			statusView.setImageDrawable(drawable);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Set or remove status icon for given tab.
	 * 
	 * @param tag
	 *            tab tag
	 * @param drawable
	 *            status drawable or {@code null} to remove status
	 * 
	 * @return {@code true} if status set or removed, {@code false} if position is invalid or tab
	 *         layout doesn't support status
	 */
	public boolean setTabStatus(String tag, Drawable drawable) {
		return setTabStatus(getTabPosition(tag), drawable);
	}
	
	/**
	 * Set or remove status icon for given tab.
	 * 
	 * @param position
	 *            tab position
	 * @param resId
	 *            status drawable resource or {@code 0} to remove status
	 * 
	 * @return {@code true} if status set or removed, {@code false} if position is invalid or tab
	 *         layout doesn't support status
	 */
	public boolean setTabStatus(int position, int resId) {
		return setTabStatus(position,
				resId == 0 ? null : getContext().getResources().getDrawable(resId));
	}
	
	/**
	 * Set or remove status icon for given tab.
	 * 
	 * @param tag
	 *            tab tag
	 * @param resId
	 *            status drawable resource or {@code 0} to remove status
	 * 
	 * @return {@code true} if status set or removed, {@code false} if position is invalid or tab
	 *         layout doesn't support status
	 */
	public boolean setTabStatus(String tag, int resId) {
		return setTabStatus(getTabPosition(tag),
				resId == 0 ? null : getContext().getResources().getDrawable(resId));
	}
	
	/**
	 * Get tab status drawable ({@link #setTabStatus(int, Drawable)}).
	 * 
	 * @param position
	 *            tab position
	 * 
	 * @return status drawable or {@code null} if no status is set (or is removed) or position is
	 *         invalid or tab layout doesn't support status indicator
	 */
	public Drawable getTabStatus(int position) {
		if (position < 0 || position >= getTabCount()) {
			return null;
		}
		
		ImageView iv = mTabData.get(position).status;
		
		if (iv != null) {
			return iv.getDrawable();
		}
		
		return null;
	}
	
	/**
	 * Get tab status drawable ({@link #setTabStatus(int, Drawable)}).
	 * 
	 * @param tag
	 *            tab tag
	 * 
	 * @return status drawable or {@code null} if no status is set (or is removed) or position is
	 *         invalid or tab layout doesn't support status indicator
	 */
	public Drawable getTabStatus(String tag) {
		return getTabStatus(getTabPosition(tag));
	}
	
	/**
	 * Get position for tab tag.
	 * 
	 * @param tag
	 *            tab tag
	 * 
	 * @return position of tab for tab tag or {@code -1} if no tab with given tab tag found
	 */
	public int getTabPosition(String tag) {
		for (int i = 0; i < mTabData.size(); i++) {
			if (tag.equals(mTabData.get(i).tag)) {
				return i;
			}
		}
		
		return -1;
	}
	
	/**
	 * Get tab tag for position.
	 * 
	 * @param position
	 *            tab position
	 * 
	 * @return tab tag or {@code null} if position is invalid or tab didn't define an ID
	 */
	public String getTabTag(int position) {
		return position < 0 || position >= getTabCount() ? null : mTabData.get(position).tag;
	}
	
	
	/**
	 * Set tab change listener which will be informed if a tab is selected or set to be current.
	 * 
	 * @param l
	 *            listener
	 */
	public void setOnTabChangeListener(OnTabChangeListener l) {
		mTabChangeListener = l;
	}
	
	/**
	 * Get tab change listener which will be informed if a tab is selected or set to be current.
	 * 
	 * @return set listener or {@code null} if no listener set
	 */
	public OnTabChangeListener getOnTabChageListener() {
		return mTabChangeListener;
	}
	
	
	/**
	 * Layout resource which will be used to create a tab.
	 * 
	 * @param resId
	 *            resource for layout ({@code 0} will reset used layout and default layout will be
	 *            used)
	 */
	public void setTabLayout(int resId) {
		mTabLayout = resId;
	}
	
	/**
	 * Layout resource which will be used as separator between tabs.<br>
	 * <br>
	 * <b>Hint:</b> Has to be called before first call of {@link #addTab(Tab)}.
	 * 
	 * @param resId
	 *            resource for layout ({@code 0} will reset used layout and default layout will be
	 *            used)
	 */
	public void setTabSeparator(int resId) {
		mTabSeparator = resId;
	}
	
	/**
	 * Layout resource which will be used as separator between the tabs and the content.
	 * 
	 * @param resId
	 *            resource for layout
	 */
	public void setTabContentSeparator(int resId) {
		if (getChildCount() == 3 || getChildCount() == 2 && mCurrentTabPosition == -1) {
			super.removeViewAt(1);
		}
		
		View child = mLayoutInflater.inflate(resId, this, false);
		LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(child.getLayoutParams());
		p.width = LayoutParams.FILL_PARENT;
		super.addView(child, 1, p);
	}
	
	/**
	 * Layout resource which will be used as separator between the tabs and the content.
	 * 
	 * @param separator
	 *            separator view
	 */
	public void setTabContentSeparator(View separator) {
		if (getChildCount() == 3 || getChildCount() == 2 && mCurrentTabPosition == -1) {
			super.removeViewAt(1);
		}
		
		LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		ViewGroup.LayoutParams ps = separator.getLayoutParams();
		
		if (ps != null) {
			if (ps instanceof MarginLayoutParams) {
				MarginLayoutParams mp = (MarginLayoutParams) ps;
				p.bottomMargin = mp.bottomMargin;
				p.topMargin = mp.topMargin;
				p.leftMargin = mp.leftMargin;
				p.rightMargin = mp.rightMargin;
			}
			
			p.height = ps.height;
		}
		
		super.addView(separator, 1, p);
	}
	
	/**
	 * (Outer) color of scroll edge showed when to much tabs were added.<br>
	 * <br>
	 * <b>Hint:</b> Has to be called before first call of {@link #addTab(Tab)}.
	 * 
	 * @param color
	 *            outer color of scroll edge as {@code 0xAARRGGBB}
	 */
	public void setTabScrollEdgeColor(int color) {
		if (mChangesLock) {
			throw new IllegalStateException("Tabs already added, call this before that!");
		}
		
		mTabScrollEdgeColor = color;
	}
	
	/**
	 * Width of scroll edge in pixel showed when to much tabs were added.
	 * 
	 * @param width
	 *            width of scroll edge in pixel
	 */
	public void setTabScrollEdgeWidth(int width) {
		mTabScrollEdgeWidth = width;
	}
	
	/**
	 * Set minimum width of a tab in pixel.
	 * 
	 * @param width
	 *            minimum width of a tab in pixel
	 */
	public void setTabMinimumWidth(int width) {
		mTabMinimumWidth = width;
		requestLayout();
	}
	
	/**
	 * Get default layout setup and modify it.
	 * 
	 * @return default layout setup
	 */
	public DefaultLayoutSetup getDefaultLayoutSetup() {
		return mDefaultLayoutSetup;
	}
	
	
	/**
	 * Tab change listener which will be informed if a tab is selected.
	 * 
	 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
	 */
	public static interface OnTabChangeListener {
		
		/**
		 * Tab changed.
		 * 
		 * @param position
		 *            current tab position
		 * @param tag
		 *            its tab ID
		 * @param positionBefore
		 *            last tab position, {@code -1} if no tab was loaded before
		 * @param tagBefore
		 *            last tab ID, {@code null} if no tab was laoded before
		 */
		public void onTabChange(TabManagerView tabManager, int position, String tag,
				int positionBefore, String tagBefore);
	}
	
	
	/**
	 * Tab definition for {@link TabManagerView#addTab(Tab)}.<br>
	 * <br>
	 * Besides everything else the content is <b>not</b> optional.
	 * 
	 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
	 */
	public static class Tab {
		
		protected Context context;
		protected String tag;
		protected String title;
		protected int position = -1;
		protected Drawable image;
		protected View content;
		
		
		public Tab(Context context) {
			this.context = context;
		}
		
		/**
		 * Set tag which is used for tab actions an lookup (besides its position).
		 * 
		 * @param tag
		 * 
		 * @return tab definition
		 */
		public Tab setTag(String tag) {
			this.tag = tag;
			return this;
		}
		
		/**
		 * Set title of tab.
		 * 
		 * @param resId
		 * 
		 * @return tab definition
		 */
		public Tab setTitle(int resId) {
			title = context.getResources().getString(resId);
			return this;
		}
		
		/**
		 * Set title of tab.
		 * 
		 * @param title
		 * 
		 * @return tab definition
		 */
		public Tab setTitle(String title) {
			this.title = title;
			return this;
		}
		
		/**
		 * Set image icon of tab.
		 * 
		 * @param resId
		 * 
		 * @return tab definition
		 */
		public Tab setImage(int resId) {
			image = context.getResources().getDrawable(resId);
			return this;
		}
		
		/**
		 * Set image icon of tab.
		 * 
		 * @param image
		 * 
		 * @return tab definition
		 */
		public Tab setImage(Drawable image) {
			this.image = image;
			return this;
		}
		
		/**
		 * Set position on which you like to place the tab (among the already added tabs).
		 * 
		 * @param position
		 * 
		 * @return tab definition
		 */
		public Tab setPosition(int position) {
			this.position = position;
			return this;
		}
		
		/**
		 * Content to display if tab is selected.
		 * 
		 * @param resId
		 * 
		 * @return tab definition
		 */
		public Tab setContent(int resId) {
			this.content = ((LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(resId, null);
			return this;
		}
		
		/**
		 * Content to display if tab is selected.
		 * 
		 * @param content
		 * 
		 * @return tab definition
		 */
		public Tab setContent(View content) {
			this.content = content;
			return this;
		}
	}
	
	
	/**
	 * Default layout manipulator.<br>
	 * <br>
	 * {@link TabManagerView} describes the values which are used for the default layout. You can
	 * modify them with this setup if you don't like the default theme. Every change will take
	 * effect on next {@link TabManagerView#addTab(Tab)} call.<br>
	 * <br>
	 * <b>Note</b>: Layout definitions set direct on {@link TabManagerView} have higher priority and
	 * will always be used instead the default layout definition.
	 * 
	 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
	 */
	public class DefaultLayoutSetup {
		
		/**
		 * Restore initial state of default setup.
		 */
		public void restoreDefaultSetup() {
			final float densitiy = getContext().getResources().getDisplayMetrics().density;
			
			border = (int) (3 * densitiy + 0.5f);
			image = (int) (20 * densitiy + 0.5f);
			overlay = (int) (14 * densitiy + 0.5f);
			overlayOffset = (int) (7 * densitiy + 0.5f);
			
			titleColors = new int [] {0xff000000, 0xffffffff, 0xffffffff};
			
			normalOrientation = Orientation.TOP_BOTTOM;
			noramlColors = new int [] {0xff444444, 0xff000000};
			
			pressedOrientation = Orientation.TOP_BOTTOM;
			pressedColors = new int [] {0xff888888, 0xff444444};
			
			selectedOrientation = Orientation.TOP_BOTTOM;
			selectedColors = new int [] {0xffffffff, 0xffbbbbbb};
			
			tabContentSeparatorHeight = (int) (2 * densitiy + 0.5f);
			tabContentSeperatorColor = 0xffbbbbbb;
		}
		
		/**
		 * Set state colors of tab title (format {@code 0xAARRGGBB});
		 * 
		 * @param normal
		 *            color in normal state
		 * @param pressed
		 *            color in pressed state
		 * @param selected
		 *            color in selected state
		 */
		public void setTitleColors(int normal, int pressed, int selected) {
			titleColors = new int [] {selected, pressed, normal};
		}
		
		/**
		 * Set gradient setup for normal state.
		 * 
		 * @param orientation
		 *            gradient orientation for
		 *            {@link GradientDrawable#GradientDrawable(Orientation, int[])}
		 * @param colors
		 *            gradient colors for
		 *            {@link GradientDrawable#GradientDrawable(Orientation, int[])}
		 */
		public void setNormalStateGradient(Orientation orientation, int [] colors) {
			if (orientation == null) {
				throw new NullPointerException();
			}
			
			if (colors.length == 0) {
				throw new IllegalArgumentException("colors have to define at least one color");
			}
			
			normalOrientation = orientation;
			noramlColors = colors;
		}
		
		/**
		 * Set gradient setup for pressed state.
		 * 
		 * @param orientation
		 *            gradient orientation for
		 *            {@link GradientDrawable#GradientDrawable(Orientation, int[])}
		 * @param colors
		 *            gradient colors for
		 *            {@link GradientDrawable#GradientDrawable(Orientation, int[])}
		 */
		public void setPressedStateGradient(Orientation orientation, int [] colors) {
			if (orientation == null) {
				throw new NullPointerException();
			}
			
			if (colors.length == 0) {
				throw new IllegalArgumentException("colors have to define at least one color");
			}
			
			pressedOrientation = orientation;
			pressedColors = colors;
		}
		
		/**
		 * Set gradient setup for selected state.
		 * 
		 * @param orientation
		 *            gradient orientation for
		 *            {@link GradientDrawable#GradientDrawable(Orientation, int[])}
		 * @param colors
		 *            gradient colors for
		 *            {@link GradientDrawable#GradientDrawable(Orientation, int[])}
		 */
		public void setSelectedStateGradient(Orientation orientation, int [] colors) {
			if (orientation == null) {
				throw new NullPointerException();
			}
			
			if (colors.length == 0) {
				throw new IllegalArgumentException("colors have to define at least one color");
			}
			
			selectedOrientation = orientation;
			selectedColors = colors;
		}
		
		/**
		 * Setup tab content separator.<br>
		 * <br>
		 * <b>Note</b>: This has to be done before first call to {@link TabManagerView#addTab(Tab)}.
		 * 
		 * @param height
		 *            height of tab content separator in pixel
		 * @param color
		 *            color of tab content as {@code 0xAARRGGBB}
		 */
		public void setTabContentSeparator(int height, int color) {
			if (mChangesLock) {
				throw new IllegalStateException(
						"You have to call this before first call of addTab!");
			}
			
			if (height < 0) {
				throw new IllegalArgumentException("height has to be greater or equal 0!");
			}
			
			tabContentSeparatorHeight = height;
			tabContentSeperatorColor = color;
		}
		
		/**
		 * Set used dimensions for default tab layout (in pixel).<br>
		 * <br>
		 * <b>Note</b>: {@code image + overlayOffset * 2 >= overlay * 2} must be {@code true}.<br>
		 * This ensures that the two overlay icons don't overlap each other.
		 * 
		 * @param border
		 *            width of padding to the content of the tab
		 * @param image
		 *            size of image
		 * @param overlay
		 *            size of image overlay (load indicator and status indicator)
		 * @param overlayOffset
		 *            offset which the overlay icons will be pushed outside the image so they don't
		 *            completely overlay the tab image itself
		 */
		public void setDimensions(int border, int image, int overlay, int overlayOffset) {
			if (border < 0 || image < 0 || overlay < 0) {
				throw new IllegalArgumentException("border, image or overly is negative!");
			}
			
			if (image + overlayOffset * 2 < overlay * 2) {
				throw new IllegalArgumentException(
						"image + overlayOffset * 2 must be >= overlay * 2");
			}
			
			this.border = border;
			this.image = image;
			this.overlay = overlay;
			this.overlayOffset = overlayOffset;
		}
		
		// PRIVATE --------------------------------------------------------------------------------
		
		private int [] titleColors;
		
		private Orientation normalOrientation;
		private int [] noramlColors;
		
		private Orientation pressedOrientation;
		private int [] pressedColors;
		
		private Orientation selectedOrientation;
		private int [] selectedColors;
		
		private int tabContentSeparatorHeight;
		private int tabContentSeperatorColor;
		
		private int border;
		private int image;
		private int overlay;
		private int overlayOffset;
		
		
		private DefaultLayoutSetup() {
			restoreDefaultSetup();
		}
	}
	
	// OVERRIDDEN =================================================================================
	
	public TabManagerView(Context context) {
		super(context);
		initialize(null);
	}
	
	public TabManagerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initialize(attrs);
	}
	
	/**
	 * <i>No children allowed, this will throw an exception.</i>
	 */
	@Override
	public void addView(View child) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * <i>No children allowed, this will throw an exception.</i>
	 */
	@Override
	public void addView(View child, int index) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * <i>No children allowed, this will throw an exception.</i>
	 */
	@Override
	public void addView(View child, ViewGroup.LayoutParams params) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * <i>No children allowed, this will throw an exception.</i>
	 */
	@Override
	public void addView(View child, int index, ViewGroup.LayoutParams params) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * <i>No children allowed, this will throw an exception.</i>
	 */
	@Override
	public void removeView(View view) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * <i>No children allowed, this will throw an exception.</i>
	 */
	@Override
	public void removeViewAt(int index) {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * <i>No children allowed, this will throw an exception.</i>
	 */
	@Override
	public void removeAllViews() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * <i>No children allowed, this will throw an exception.</i>
	 */
	@Override
	public void removeViews(int start, int count) {
		throw new UnsupportedOperationException();
	}
	
	// PRIVATE ====================================================================================
	
	private void initialize(AttributeSet attrs) {
		setOrientation(LinearLayout.VERTICAL);
		super.addView(new TabScrollView(getContext()), -1, new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		
		mTabContainer = new LinearLayout(getContext());
		mTabContainer.setOrientation(LinearLayout.HORIZONTAL);
		
		if (attrs == null) {
			return;
		}
		
		TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.TabManagerView);
		
		mTabLayout = a.getResourceId(R.styleable.TabManagerView_tabLayout, 0);
		mTabSeparator = a.getResourceId(R.styleable.TabManagerView_tabSeparator, 0);
		
		if (a.hasValue(R.styleable.TabManagerView_tabMinimumWidth)) {
			mTabMinimumWidth = a.getDimensionPixelOffset(
					R.styleable.TabManagerView_tabMinimumWidth, -1);
		}
		
		if (a.hasValue(R.styleable.TabManagerView_tabScrollEdgeColor)) {
			mTabScrollEdgeColor = a.getColor(
					R.styleable.TabManagerView_tabScrollEdgeColor, 0);
		}
		
		if (a.hasValue(R.styleable.TabManagerView_tabScrollEdgeWidth)) {
			mTabScrollEdgeWidth = a.getDimensionPixelSize(
					R.styleable.TabManagerView_tabScrollEdgeWidth, 0);
		}
		
		if (a.hasValue(R.styleable.TabManagerView_tabContentseparator)) {
			setTabContentSeparator(a.getResourceId(
					R.styleable.TabManagerView_tabContentseparator, 0));
		}
		
		a.recycle();
	}
	
	
	private View generateDefaultTabProgrammatically() {
		DefaultLayoutSetup setup = mDefaultLayoutSetup;
		
		FrameLayout imageContainer = new FrameLayout(getContext());
		LinearLayout.LayoutParams icp = new LinearLayout.LayoutParams(
				setup.image + setup.overlayOffset * 2, LayoutParams.WRAP_CONTENT);
		icp.gravity = Gravity.CENTER_HORIZONTAL;
		imageContainer.setLayoutParams(icp);
		
		ProgressBar busy = new ProgressBar(
				getContext(), null, android.R.attr.progressBarStyleSmall);
		FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(
				setup.overlay, setup.overlay + setup.border);
		busy.setLayoutParams(bp);
		busy.setId(android.R.id.icon1);
		busy.setVisibility(View.GONE);
		busy.setPadding(0, 0, 0, setup.border);
		
		ImageView status = new ImageView(getContext());
		FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(
				setup.overlay, setup.overlay + setup.border);
		sp.gravity = Gravity.RIGHT | Gravity.TOP;
		status.setLayoutParams(sp);
		status.setId(android.R.id.icon2);
		status.setVisibility(View.GONE);
		status.setPadding(0, 0, 0, setup.border);
		
		ImageView image = new ImageView(getContext());
		FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(
				setup.image, setup.image + setup.border + setup.overlayOffset);
		ip.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
		image.setLayoutParams(ip);
		image.setId(android.R.id.icon);
		image.setVisibility(View.GONE);
		image.setPadding(0, setup.overlayOffset, 0, setup.border);
		
		imageContainer.addView(image);
		imageContainer.addView(busy);
		imageContainer.addView(status);
		
		LinearLayout tab = new LinearLayout(getContext());
		tab.setOrientation(LinearLayout.VERTICAL);
		tab.setGravity(Gravity.BOTTOM);
		tab.setPadding(setup.border, setup.border, setup.border, 0);
		
		TextView title = new TextView(getContext());
		LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		tp.gravity = Gravity.CENTER_HORIZONTAL;
		tp.bottomMargin = setup.border;
		title.setLayoutParams(tp);
		title.setEllipsize(TruncateAt.MARQUEE);
		title.setSingleLine(true);
		title.setId(android.R.id.title);
		title.setVisibility(View.GONE);
		title.setTextColor(new ColorStateList(new int [] [] {
				{android.R.attr.state_selected}, {android.R.attr.state_pressed}, {}},
				setup.titleColors));
		
		StateListDrawable background = new StateListDrawable();
		background.addState(new int [] {android.R.attr.state_selected},
				new GradientDrawable(setup.selectedOrientation, setup.selectedColors));
		background.addState(new int [] {android.R.attr.state_pressed},
				new GradientDrawable(setup.pressedOrientation, setup.pressedColors));
		background.addState(new int [] {},
				new GradientDrawable(setup.normalOrientation, setup.noramlColors));
		
		tab.addView(imageContainer);
		tab.addView(title);
		tab.setBackgroundDrawable(background);
		
		return tab;
	}
	
	
	private static class TabData {
		public String tag;
		public View content;
		public View busy;
		public ImageView status;
	}
	
	/**
	 * Frame layout which displays the tabs and adds the function to scroll them when there are to
	 * much added tabs.
	 * 
	 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
	 */
	private class TabScrollView extends FrameLayout {
		
		private int mmScrollOffset;
		private int mmStartX;
		private boolean mmScrolling;
		private View mmClickedTab;
		private int mmClickedTabPosition;
		
		
		public TabScrollView(Context context) {
			super(context);
		}
		
		
		@Override
		public boolean onTouchEvent(MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				mmStartX = (int) event.getX();
				mmClickedTab = null;
				int count = mTabContainer.getChildCount();
				
				// search for clicked tab
				for (int i = 0; i < count; i += mTabSeparator == 0 ? 1 : 2) {
					View c = mTabContainer.getChildAt(i);
					
					if (c.getLeft() <= mmStartX + mmScrollOffset
							&& c.getRight() >= mmStartX + mmScrollOffset) {
						mmClickedTab = c;
						mmClickedTab.setPressed(true);
						mmClickedTabPosition = mTabSeparator == 0 ? i : i / 2;
						break;
					}
				}
				
				mmScrolling = mmClickedTab == null && mTabContainer.getWidth() > getWidth();
			} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
				if (!mmScrolling && mTabContainer.getWidth() > getWidth()
						&& Math.abs(mmStartX - (int) event.getX())
						> ViewConfiguration.getTouchSlop()) {
					// tab bar is scrollable and now the scroll offset kickes in
					mmStartX = (int) event.getX();
					mmScrolling = true;
					
					if (mmClickedTab != null) {
						mmClickedTab.setPressed(false);
						mmClickedTab = null;
					}
				} else if (mmScrolling) {
					// just scroll - draw will fix an incorrect offset
					mmScrollOffset += mmStartX - (int) event.getX();
					mmStartX = (int) event.getX();
					invalidate();
				} else if (mmClickedTab != null) {
					int x = (int) event.getX();
					int y = (int) event.getY();
					
					// we're not scrolling - disable a pressed state on clicked tab when touch moves
					// out of its bounds
					mmClickedTab.setPressed(
							mmClickedTab.getLeft() <= x + mmScrollOffset
									&& mmClickedTab.getRight() >= x + mmScrollOffset
									&& mmClickedTab.getTop() <= y && mmClickedTab.getBottom() >= y);
				}
			} else if (event.getAction() == MotionEvent.ACTION_UP) {
				if (!mmScrolling && mmClickedTab != null && mmClickedTab.isPressed()) {
					setCurrentTab(mmClickedTabPosition);
					mmClickedTab.setPressed(false);
					mmClickedTab = null;
				}
			} else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
				if (mmClickedTab != null) {
					mmClickedTab.setPressed(false);
				}
			}
			
			return true;
		}
		
		@Override
		public void onMeasure(int widthSpec, int heightSpec) {
			if (getTabCount() > 0) {
				mTabContainer.measure(widthSpec, 0);
				
				// tab container tries to scale tabs so they fit the width
				// set the minimum width of tab (when) given in layout width parameter
				if (mTabMinimumWidth > mTabContainer.getChildAt(0).getMeasuredWidth()) {
					int separatorWidth = 0;
					
					if (mTabSeparator != 0 && getTabCount() > 1) {
						separatorWidth = mTabContainer.getChildAt(1).getMeasuredWidth();
					}
					
					// sum of all tabs and separators
					mTabContainer.measure((getTabCount() * mTabMinimumWidth
							+ (getTabCount() - 1) * separatorWidth) | MeasureSpec.EXACTLY, 0);
				}
				
				setMeasuredDimension(MeasureSpec.getSize(widthSpec),
						mTabContainer.getMeasuredHeight());
			} else {
				super.onMeasure(widthSpec, heightSpec);
			}
			
			// adjust scroll edge height
			if (getChildCount() > 2) {
				getChildAt(1).measure(mTabScrollEdgeWidth | MeasureSpec.EXACTLY,
						mTabContainer.getMeasuredHeight() | MeasureSpec.EXACTLY);
				getChildAt(2).measure(mTabScrollEdgeWidth | MeasureSpec.EXACTLY,
						mTabContainer.getMeasuredHeight() | MeasureSpec.EXACTLY);
			}
		}
		
		@Override
		public void dispatchDraw(Canvas canvas) {
			if (mTabContainer.getWidth() > getWidth()) {
				if (mmScrollOffset < 0) {
					mmScrollOffset = 0;
				} else if (mmScrollOffset >= mTabContainer.getWidth() - getWidth()) {
					mmScrollOffset = mTabContainer.getWidth() - getWidth();
				}
				
				// show scroll edges when needed
				getChildAt(1).setVisibility(mmScrollOffset == 0 ? View.GONE : View.VISIBLE);
				getChildAt(2).setVisibility(
						mmScrollOffset >= mTabContainer.getWidth() - getWidth()
								? View.GONE : View.VISIBLE);
				
				// scroll the tab container
				mTabContainer.scrollTo(mmScrollOffset, 0);
			} else if (getChildCount() > 0) {
				// tabs fit the width so simply hide the scroll edges
				mmScrollOffset = 0;
				getChildAt(1).setVisibility(View.GONE);
				getChildAt(2).setVisibility(View.GONE);
			}
			
			super.dispatchDraw(canvas);
		}
	}
}
