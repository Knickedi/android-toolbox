package de.viktorreiser.toolbox.graphics.drawable;

import java.util.Arrays;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * Drawable which will draw a status text on top of a colored shape.<br>
 * <br>
 * Primarily this drawable is made for something like a message count. You could use it for really
 * short text. But in the end it fits given text into a colored shape.<br>
 * <br>
 * Use {@link #setText(String)} to define the text itself.<br>
 * Use {@link #setTextColor(int)}, {@link #setStrokeColor(int)} and {@link #setFillColor(int)} to
 * setup the color behavior of this drawable.<br>
 * Use {@link #setStrokeWidth(float)}, {@link #setCornerRadius(float)} and
 * {@link #setSquare(boolean)} to influence the shape itself.<br>
 * <br>
 * Default settings are square shaped, {@code 0xccffffff} text color, {@code 0xaa000000} fill color,
 * {@code 0xaaff0000} stroke color, {@code 0.3} corner radius, {@code 0.055} stroke width and
 * {@code 0.075} text padding. A translucent square with round corners, red border and white text.
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class StatusTextDrawable extends Drawable {
	
	// PRIVATE ====================================================================================
	
	private String mText;
	private boolean mSquare = true;
	private float mStrokeWidth = 0.055f;
	private float mCornerRadius = 0.3f;
	private float mTextPadding = 0.1f;
	
	private Paint mTextPaint = new Paint();
	private Paint mStrokePaint = new Paint();
	private Paint mFillPaint = new Paint();
	
	private Path mStrokePath = new Path();
	private Path mFillPath = new Path();
	private float mTextHalfHeight;
	
	// PUBLIC =====================================================================================
	
	public StatusTextDrawable() {
		mTextPaint.setTextAlign(Align.CENTER);
		mTextPaint.setColor(0xccffffff);
		mFillPaint.setColor(0xaa000000);
		mStrokePaint.setColor(0xaaff0000);
	}
	
	/**
	 * Set text which should be placed in drawable.
	 * 
	 * @param text
	 *            text to draw or {@code null} for no text
	 */
	public void setText(String text) {
		boolean changed = text == mText || (text != null && text.equals(mText));
		mText = text;
		
		if (text != null) {
			mText = mText.trim();
		}
		
		if (changed) {
			updateGeometry(false, true);
			invalidateSelf();
		}
	}
	
	/**
	 * Set color of text.
	 * 
	 * @param color
	 *            color of text
	 */
	public void setTextColor(int color) {
		mTextPaint.setColor(color);
	}
	
	/**
	 * Set color of stroke (border).
	 * 
	 * @param color
	 *            color of stroke
	 */
	public void setStrokeColor(int color) {
		mStrokePaint.setColor(color);
	}
	
	/**
	 * Set color of background.
	 * 
	 * @param color
	 *            color of background
	 */
	public void setFillColor(int color) {
		mFillPaint.setColor(color);
	}
	
	/**
	 * Set padding of text to shape border or stroke edge.
	 * 
	 * @param padding
	 *            padding is relative to remaining width or height (depends on the smaller value) -
	 *            {@code 0} means no padding and {@code 1} <i>would</i> mean full padding but since
	 *            a full padding makes no sense the value is clamped to {@code [0,0.8]}
	 */
	public void setTextPadding(float padding) {
		padding = Math.max(padding, 0f);
		padding = Math.min(padding, 0.8f);
		padding /= 2;
		
		boolean changed = mTextPadding != padding;
		mTextPadding = padding;
		
		if (changed) {
			updateGeometry(true, true);
			invalidateSelf();
		}
	}
	
	/**
	 * Set stroke (border) width.
	 * 
	 * @param width
	 *            stroke width is relative to the overall width or height (depends on the smaller
	 *            value) - {@code 0} means no stroke and {@code 1} <i>would</i> mean full stroke but
	 *            since a full stroke makes no sense the value is clamped to {@code [0,0.8]}
	 */
	public void setStrokeWidth(float width) {
		width = Math.max(width, 0f);
		width = Math.min(width, 0.8f);
		width /= 2f;
		
		boolean changed = mStrokeWidth != width;
		mStrokeWidth = width;
		
		if (changed) {
			updateGeometry(true, true);
			invalidateSelf();
		}
	}
	
	/**
	 * Set radius of corner.
	 * 
	 * @param radius
	 *            negative value will make the shape to oval - {@code 0} will make the shape a
	 *            rectangle - otherwise the value is clamped to {@code [0,1]} where 1 means full
	 *            corner so a rectangle would have to half circles and a square qould become a
	 *            circle
	 */
	public void setCornerRadius(float radius) {
		radius = Math.max(radius, -1f);
		radius = Math.min(radius, 1f);
		radius /= 2f;
		
		boolean changed = mCornerRadius != radius;
		mCornerRadius = radius;
		
		if (changed) {
			updateGeometry(true, true);
			invalidateSelf();
		}
	}
	
	/**
	 * Force shape to be drawn in square dimensions.
	 * 
	 * @param square
	 *            {@code true} will force shape to be a square (with round corners) or a circle
	 *            depending on the given value of {@link #setCornerRadius(float)}
	 */
	public void setSquare(boolean square) {
		boolean changed = mSquare != square;
		mSquare = square;
		
		if (changed) {
			updateGeometry(true, true);
			invalidateSelf();
		}
	}
	
	// OVERRIDDEN =================================================================================
	
	/**
	 * Set overall alpha.<br>
	 * <br>
	 * This will override every alpha value which was given before by {@link #setStrokeColor(int)},
	 * {@link #setTextColor(int)} or {@link #setFillColor(int)}.
	 */
	@Override
	public void setAlpha(int alpha) {
		mTextPaint.setAlpha(alpha);
		mStrokePaint.setAlpha(alpha);
		mFillPaint.setAlpha(alpha);
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public int getOpacity() {
		if (mFillPath.isEmpty()) {
			return PixelFormat.OPAQUE;
		}
		
		final int stroke = mStrokePaint.getAlpha();
		final int fill = mFillPaint.getAlpha();
		final int text = mTextPaint.getAlpha();
		
		if ((mStrokePath.isEmpty() || stroke == 0) && fill == 0 && (mText == null || text == 0)) {
			return PixelFormat.TRANSPARENT;
		} else if ((mStrokePath.isEmpty() || stroke == 255) && fill == 255
				&& (mText == null || text == 255)) {
			return PixelFormat.OPAQUE;
		} else {
			return PixelFormat.TRANSLUCENT;
		}
	}
	
	/**
	 * No color filter.
	 */
	@Override
	public void setColorFilter(ColorFilter cf) {
		
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	protected void onBoundsChange(Rect bounds) {
		updateGeometry(true, true);
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public void draw(Canvas canvas) {
		if (mFillPath.isEmpty()) {
			return;
		}
		
		if (!mStrokePath.isEmpty()) {
			canvas.drawPath(mStrokePath, mStrokePaint);
		}
		
		canvas.drawPath(mFillPath, mFillPaint);
		
		if (mText != null) {
			Rect b = getBounds();
			mTextPaint.setLinearText(true);
			canvas.drawText(mText, b.width() / 2f, b.height() / 2f + mTextHalfHeight, mTextPaint);
		}
	}
	
	// PRIVATE ====================================================================================
	
	/**
	 * Update drawable geometry since parameters changed.
	 * 
	 * @param pathGeometry
	 *            {@code true} when drawable stroke and fill should be updated
	 * @param textGeometry
	 *            {@code true} when text positioning should be updated ({@code true} of
	 *            {@code pathGeometry} will do that anyways)
	 */
	private void updateGeometry(boolean pathGeometry, boolean textGeometry) {
		Rect bounds = getBounds();
		
		if (pathGeometry) {
			mStrokePath.reset();
			mFillPath.reset();
		}
		
		if (bounds.isEmpty()) {
			return;
		}
		
		float r = bounds.right;
		float l = bounds.left;
		float t = bounds.top;
		float b = bounds.bottom;
		
		if (mSquare) {
			if (bounds.width() > bounds.height()) {
				float diff = bounds.width() / 2f - bounds.height() / 2f;
				l += diff;
				r -= diff;
			} else if (bounds.height() > bounds.width()) {
				float diff = bounds.height() / 2f - bounds.width() / 2f;
				t += diff;
				b -= diff;
			}
		}
		
		float strokeWidth = Math.min(bounds.width(), bounds.height()) * mStrokeWidth;
		float outerCornerWidth = Math.min(bounds.width(), bounds.height()) * mCornerRadius;
		float innerCornerWidth = outerCornerWidth - strokeWidth;
		RectF outerRect = new RectF(l, t, r, b);
		RectF innerRect = new RectF(
				l + strokeWidth, t + strokeWidth, r - strokeWidth, b - strokeWidth);
		
		if (pathGeometry) {
			if (mCornerRadius < 0 || (mSquare && mCornerRadius >= 0.49)) {
				// create oval / circle
				if (strokeWidth >= 1) {
					mStrokePath.addOval(outerRect, Direction.CW);
					mStrokePath.addOval(innerRect, Direction.CCW);
				}
				
				mFillPath.addOval(innerRect, Direction.CW);
			} else if (outerCornerWidth < 1) {
				// create rectangle / square
				if (strokeWidth >= 1) {
					mStrokePath.addRect(outerRect, Direction.CW);
					mStrokePath.addRect(innerRect, Direction.CCW);
				}
				
				mFillPath.addRect(innerRect, Direction.CW);
			} else {
				// create rectangle / square with round corners
				float [] outerRadius = new float [8];
				Arrays.fill(outerRadius, outerCornerWidth);
				
				if (strokeWidth >= 1) {
					mStrokePath.addRoundRect(outerRect, outerRadius, Direction.CW);
				}
				
				if (innerCornerWidth > 1) {
					// it has round inner corners
					float [] innerRadius = new float [8];
					Arrays.fill(innerRadius, innerCornerWidth);
					
					if (strokeWidth >= 1) {
						mStrokePath.addRoundRect(innerRect, innerRadius, Direction.CCW);
					}
					
					mFillPath.addRoundRect(innerRect, innerRadius, Direction.CW);
				} else {
					// no round inner corner because stroke width is greater than outer corner width
					if (strokeWidth >= 1) {
						mStrokePath.addRect(innerRect, Direction.CCW);
					}
					
					mFillPath.addRect(innerRect, Direction.CW);
				}
			}
		}
		
		if ((pathGeometry || textGeometry) && mText != null) {
			// for getting the text bounds
			Rect tb = new Rect();
			int textLength = mText.length();
			
			// overall maximum text width and height
			// this is already enough for a simple rectangle bound testing
			float maxWidth = innerRect.width();
			float maxHeight = innerRect.height();
			float minForPadding = Math.min(maxWidth, maxHeight);
			maxWidth -= minForPadding * mTextPadding * 2;
			maxHeight -= minForPadding * mTextPadding * 2;
			
			// if shape is ellipse we need to test x^2/maxEllipseX + y^2/maxEllipseY <= 1
			float maxEllipseX = maxWidth * maxWidth / 4f;
			float maxEllipseY = maxHeight * maxHeight / 4f;
			innerCornerWidth -= minForPadding * mTextPadding;
			
			// if shape is a rectangle and has inner corners we need to test
			// ((x - maxWidth / 2)^2 + (y - maxHeight / 2)^2)/roundRectRadius <= 1
			// since the shape and the text bounds are symmetric this is enough as condition
			float roundRectRadius = innerCornerWidth * innerCornerWidth;
			
			// we will search binary for the needed text size
			float left = 5000;
			float middle = 2500;
			float right = 0;
			float runs = 0;
			boolean doesntFit = false;
			
			while (runs < 15) {
				mTextPaint.setTextSize(middle);
				mTextPaint.getTextBounds(mText, 0, textLength, tb);
				float oldMiddle = middle;
				
				if (Math.abs(tb.top) > maxHeight || Math.abs(tb.right) > maxWidth) {
					doesntFit = true;
				} else if (mCornerRadius < 0 || (mSquare && mCornerRadius >= 0.49)) {
					doesntFit = tb.right * tb.right / 4f / maxEllipseX
							+ tb.top * tb.top / 4f / maxEllipseY > 1;
				} else if (innerCornerWidth >= 1
						&& Math.abs(tb.top) + innerCornerWidth * 2 > maxHeight
						&& Math.abs(tb.right) + innerCornerWidth * 2 > maxWidth) {
					// we had to make sure that the given text bounds
					// are located within the outer corner radius
					float x = (Math.abs(tb.right) + innerCornerWidth * 2 - maxWidth) / 2f;
					float y = (Math.abs(tb.top) + innerCornerWidth * 2 - maxHeight) / 2f;
					
					doesntFit = (x * x + y * y) / roundRectRadius >= 1;
				} else {
					doesntFit = false;
				}
				
				if (doesntFit) {
					middle = (middle + right) / 2f;
					left = oldMiddle;
				} else {
					middle = (middle + left) / 2f;
					right = oldMiddle;
				}
				
				runs++;
			}
			
			mTextHalfHeight = Math.abs(tb.top) / 2;
		}
	}
}
