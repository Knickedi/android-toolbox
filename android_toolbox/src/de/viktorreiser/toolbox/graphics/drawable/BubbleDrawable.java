package de.viktorreiser.toolbox.graphics.drawable;

import java.util.Arrays;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Region.Op;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

public class BubbleDrawable extends Drawable {
	
	// PRIVATE ====================================================================================
	
	private Paint mStrokePaint = new Paint();
	private Paint mFillPaint = new Paint();
	
	private Path mStrokePath = new Path();
	private Path mFillPath = new Path();
	private Path mIndicatorStrokePath = new Path();
	private Path mIndicatorFillPath = new Path();
	
	private int mCornerRadius = 50;
	private int mStrokeWidth = 10;
	private int mIndicatorWidth = 50;
	private int mIndicatorHeight = 80;
	private IndicatorDirection mIndicatorDirection = IndicatorDirection.LEFT;
	private float mIndicatorPosition = 0.5f;
	private float mIndicatorVerticalStrokeWidth = 0f;
	private float mIndicatorHorizontalStrokeWidth = 0f;
	private float mIndicatorVerticalInnerStrokeOffset = 0f;
	
	// PUBLIC =====================================================================================
	
	public enum IndicatorDirection {
		LEFT, TOP, RIGHT, BOTTOM
	}
	
	
	public BubbleDrawable() {
		mStrokePaint.setColor(0xaaaaaaaa);
		mFillPaint.setColor(0xaa000000);
	}
	
	
	public void setCornerRadius(int radius) {
		radius = Math.max(radius, 0);
		boolean changed = radius != mCornerRadius;
		mCornerRadius = radius;
		
		if (changed) {
			updateGeometry();
			invalidateSelf();
		}
	}
	
	public void setStrokeWidth(int width) {
		width = Math.max(width, 0);
		boolean changed = width != mStrokeWidth;
		mStrokeWidth = width;
		
		if (changed) {
			updateGeometry();
			invalidateSelf();
		}
	}
	
	public void setIndicatorPosition(IndicatorDirection direction, float position) {
		if (direction == null) {
			throw new NullPointerException();
		}
		
		position = Math.min(Math.max(position, 0), 1f);
		boolean directionChanged = direction != mIndicatorDirection;
		boolean positionChanged = position != mIndicatorPosition;
		mIndicatorDirection = direction;
		mIndicatorPosition = position;
		
		if (directionChanged) {
			updateGeometry();
			invalidateSelf();
		} else if (positionChanged) {
			invalidateSelf();
		}
	}
	
	public void setIndicatorDimensions(int width, int height) {
		width = Math.max(width, 0);
		height = Math.max(height, 0);
		
		boolean changed = width != mIndicatorWidth || height != mIndicatorHeight;
		mIndicatorWidth = width;
		mIndicatorHeight = height;
		
		if (changed) {
			updateGeometry();
			invalidateSelf();
		}
	}
	
	public void setStrokeColor(int strokeColor) {
		boolean changed = strokeColor != mStrokePaint.getColor();
		mStrokePaint.setColor(strokeColor);
		
		if (changed) {
			invalidateSelf();
		}
	}
	
	public void setFillColor(int fillColor) {
		boolean changed = fillColor != mFillPaint.getColor();
		mFillPaint.setColor(fillColor);
		
		if (changed) {
			invalidateSelf();
		}
	}
	
	// OVERRIDDEN =================================================================================
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public void draw(Canvas canvas) {
		if (mFillPath.isEmpty()) {
			return;
		}
		
		canvas.drawPath(mFillPath, mFillPaint);
		
		// no fill path == no indicator at all
		if (mIndicatorFillPath.isEmpty()) {
			if (mStrokeWidth > 0) {
				canvas.drawPath(mStrokePath, mStrokePaint);
			}
			
			return;
		}
		
		final float iP = mIndicatorPosition;
		final float iW = mIndicatorWidth;
		final float cR = mCornerRadius;
		final float sW = mStrokeWidth;
		final float iVSW = mIndicatorVerticalStrokeWidth;
		
		float offset;
		final boolean isHorizontal = mIndicatorDirection == IndicatorDirection.TOP
				|| mIndicatorDirection == IndicatorDirection.BOTTOM;
		
		// get the needed offset to move indicator by calculating it by given position
		if (isHorizontal) {
			final float w = getBounds().width();
			offset = Math.min(Math.max(
					w * iP - iVSW - iW / 2f, Math.max(cR, sW)), w - cR - iVSW * 2 - iW);
		} else {
			final float h = getBounds().height();
			offset = Math.min(Math.max(
					h * iP - iVSW - iW / 2f, Math.max(cR, sW)), h - cR - iVSW * 2 - iW);
		}
		
		// no need for complicated stroke clipping, because there's no stroke
		// just draw the indicator without stroke and that's it
		if (mStrokeWidth < 0) {
			canvas.save();
			canvas.translate(isHorizontal ? offset : 0, isHorizontal ? 0 : offset);
			canvas.drawPath(mIndicatorFillPath, mFillPaint);
			canvas.restore();
		}
		
		if (mStrokeWidth < 0) {
			
		} else {
			if (mIndicatorFillPath.isEmpty()) {
				canvas.drawPath(mStrokePath, mStrokePaint);
			} else {
				
				
				canvas.translate(isHorizontal ? offset : 0, isHorizontal ? 0 : offset);
				canvas.clipPath(mIndicatorFillPath, Op.DIFFERENCE);
				canvas.translate(isHorizontal ? -offset : 0, isHorizontal ? 0 : -offset);
				canvas.drawPath(mStrokePath, mStrokePaint);
				canvas.clipRect(getBounds(), Op.REPLACE);
				canvas.translate(isHorizontal ? offset : 0, isHorizontal ? 0 : offset);
				canvas.drawPath(mIndicatorFillPath, mFillPaint);
				canvas.drawPath(mIndicatorStrokePath, mStrokePaint);
				
				canvas.restore();
			}
		}
	}
	
	/**
	 * This will always return the needed padding so the content fits into the bubble.
	 */
	@Override
	public boolean getPadding(Rect padding) {
		if (getBounds().isEmpty()) {
			return false;
		}
		
		int offset = mStrokeWidth + 10 + (Math.max(mCornerRadius - mStrokeWidth - 10, 0) / 2);
		
		padding.left = offset;
		padding.right = offset;
		padding.top = offset;
		padding.bottom = offset;
		
		if (!mIndicatorFillPath.isEmpty()) {
			return true;
		}
		
		offset = (int) (mIndicatorHeight + mIndicatorHorizontalStrokeWidth + 0.5f);
		
		switch (mIndicatorDirection) {
		case LEFT:
			padding.left += offset;
			break;
		
		case RIGHT:
			padding.right += offset;
			break;
		
		case TOP:
			padding.top += offset;
			break;
		
		case BOTTOM:
			padding.bottom += offset;
			break;
		}
		
		return true;
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	protected void onBoundsChange(Rect bounds) {
		updateGeometry();
	}
	
	/**
	 * <i>Overridden for internal use!</i>
	 */
	@Override
	public int getOpacity() {
		int fill = mFillPaint.getAlpha();
		int stroke = mStrokePaint.getAlpha();
		
		if (fill == 0 && stroke == 0) {
			return PixelFormat.TRANSPARENT;
		} else if (fill == 255 && stroke == 255) {
			return PixelFormat.OPAQUE;
		} else {
			return PixelFormat.TRANSLUCENT;
		}
	}
	
	/**
	 * Set overall alpha (same alpha for stroke and fill color).
	 */
	@Override
	public void setAlpha(int alpha) {
		mFillPaint.setAlpha(alpha);
		mStrokePaint.setAlpha(alpha);
	}
	
	/**
	 * No color filter.
	 */
	@Override
	public void setColorFilter(ColorFilter cf) {
		
	}
	
	// PRIVATE ====================================================================================
	
	private void updateGeometry() {
		final Rect b = getBounds();
		
		mStrokePath.reset();
		mFillPath.reset();
		mIndicatorFillPath.reset();
		mIndicatorStrokePath.reset();
		
		if (b.isEmpty()) {
			return;
		}
		
		final boolean isHorizontal = mIndicatorDirection == IndicatorDirection.TOP
				|| mIndicatorDirection == IndicatorDirection.BOTTOM;
		final int innerCornerWidth = mCornerRadius - mStrokeWidth;
		final float sW = mStrokeWidth;
		final float iW = mIndicatorWidth;
		final float cR = mCornerRadius;
		final float iH = mIndicatorHeight;
		final float iVISO = iW * (iH + sW) / 2f / iH - iW / 2f;
		
		mIndicatorVerticalInnerStrokeOffset = iVISO;
		
		if (mIndicatorHeight > 0) {
			mIndicatorVerticalStrokeWidth = (float) (sW / Math.cos(Math.atan(iW / 2f / iH)));
		}
		
		final float iVSW = mIndicatorVerticalStrokeWidth;
		
		final RectF outerRect = new RectF(b.left, b.top, b.right, b.bottom);
		final RectF innerRect = new RectF(b.left + sW, b.top + sW, b.right - sW, b.bottom - sW);
		float neededSpace = iW + Math.max(iVISO + Math.max(sW, cR), iVSW + cR) * 2;
		
		// correct bubble stroke and fill rectangle so the indicator fits besides
		if (mIndicatorWidth > 0) {
			mIndicatorHorizontalStrokeWidth = 2f * iVSW * iH / iW;
			float addOffset = iH + mIndicatorHorizontalStrokeWidth;
			
			switch (mIndicatorDirection) {
			case LEFT:
				outerRect.left += addOffset;
				innerRect.left += addOffset;
				break;
			
			case RIGHT:
				outerRect.right -= addOffset;
				innerRect.right -= addOffset;
				break;
			
			case TOP:
				outerRect.top += addOffset;
				innerRect.top += addOffset;
				break;
			
			case BOTTOM:
				outerRect.bottom -= addOffset;
				innerRect.bottom -= addOffset;
				break;
			}
		}
		
		// only create indicator paths if there is room for it since the bounds might be to little
		// and/or the corners might need the room so there's not enough room for a indicator
		if (mIndicatorHeight > 0 && mIndicatorWidth > 0
				&& (!isHorizontal && b.height() >= neededSpace
				|| isHorizontal && b.width() >= neededSpace)) {
			// would throw an exception on indicator width == 0
			
			
			// create indicator stroke and fill path based on the given direction
			switch (mIndicatorDirection) {
			case LEFT:
				updateLeftIndicator(outerRect, innerRect);
				break;
			
			case RIGHT:
				// outerRect.right -= mIndicatorHeight;
				// innerRect.right -= mIndicatorHeight;
				//
				// mIndicatorFillPath.moveTo(outerRect.right, outerRect.top + mStrokeWidth);
				// mIndicatorFillPath.lineTo(outerRect.right + mIndicatorHeight,
				// outerRect.top + mStrokeWidth + mIndicatorWidth / 2f);
				// mIndicatorFillPath.lineTo(outerRect.right,
				// outerRect.top + mStrokeWidth + mIndicatorWidth);
				// mIndicatorFillPath.close();
				
				break;
			
			case TOP:
				// outerRect.top += mIndicatorHeight;
				// innerRect.top += mIndicatorHeight;
				//
				// mIndicatorFillPath.moveTo(outerRect.left + mStrokeWidth, outerRect.top);
				// mIndicatorFillPath.lineTo(outerRect.left + mStrokeWidth + mIndicatorWidth / 2f,
				// outerRect.top - mIndicatorHeight);
				// mIndicatorFillPath.lineTo(outerRect.left + mStrokeWidth + mIndicatorWidth,
				// outerRect.top);
				// mIndicatorFillPath.close();
				//
				// mIndicatorFillPath.setLastPoint(outerRect.left, outerRect.top);
				// mIndicatorFillPath.lineTo(outerRect.left + mIndicatorHeight,
				// outerRect.top + mStrokeWidth + mIndicatorWidth / 2f);
				// mIndicatorFillPath.close();
				
				break;
			
			case BOTTOM:
				// outerRect.bottom -= mIndicatorHeight;
				// innerRect.bottom -= mIndicatorHeight;
				//
				// mIndicatorFillPath.moveTo(outerRect.left + mStrokeWidth, outerRect.bottom);
				// mIndicatorFillPath.lineTo(outerRect.left + mStrokeWidth + mIndicatorWidth / 2f,
				// outerRect.bottom + mIndicatorHeight);
				// mIndicatorFillPath.lineTo(outerRect.left + mStrokeWidth + mIndicatorWidth,
				// outerRect.bottom);
				// mIndicatorFillPath.close();
				
				break;
			}
		}
		
		// that's the main fill and stroke path of the bubble
		if (mCornerRadius >= 1) {
			// create bubble rect with round corners
			float [] outerRadius = new float [8];
			Arrays.fill(outerRadius, mCornerRadius);
			
			mStrokePath.addRoundRect(outerRect, outerRadius, Direction.CW);
			
			if (innerCornerWidth >= 1) {
				float [] innerRadius = new float [8];
				Arrays.fill(innerRadius, innerCornerWidth);
				
				if (mStrokeWidth >= 1) {
					mStrokePath.addRoundRect(innerRect, innerRadius, Direction.CCW);
				}
				
				mFillPath.addRoundRect(innerRect, innerRadius, Direction.CW);
			} else {
				if (mStrokeWidth >= 1) {
					mStrokePath.addRect(innerRect, Direction.CCW);
				}
				
				mFillPath.addRect(innerRect, Direction.CW);
			}
		} else {
			// create bubble without round corners
			if (mStrokeWidth >= 1) {
				mStrokePath.addRect(outerRect, Direction.CW);
				mStrokePath.addRect(innerRect, Direction.CCW);
			}
			
			mFillPath.addRect(innerRect, Direction.CW);
		}
	}
	
	private void updateLeftIndicator(RectF outerRect, RectF innerRect) {
		final RectF oR = outerRect;
		final RectF iR = innerRect;
		final float iH = mIndicatorHeight;
		final float iW = mIndicatorWidth;
		final float iVSW = mIndicatorVerticalStrokeWidth;
		final float iHSW = mIndicatorHorizontalStrokeWidth;
		final float iVISO = mIndicatorVerticalInnerStrokeOffset;
		
		// fill indicator path which overlaps the stroke of the popup
		// 1. top right | 2. middle left | 3. bottom right
		mIndicatorFillPath.moveTo(iR.left, oR.top + iVSW - iVISO);
		mIndicatorFillPath.lineTo(oR.left - iH, oR.top + iVSW + iW / 2f);
		mIndicatorFillPath.lineTo(iR.left, oR.top + iVSW + iW + iVISO);
		mIndicatorFillPath.close();
		
		if (mStrokeWidth >= 1) {
			// fill path of indicator stroke which borders the popup outer stroke
			// 1. most top right | 2. outer middle left | 3. most bottom right
			// 4. bottom right | 5. inner middle left | 6. top right
			mIndicatorStrokePath.moveTo(oR.left, oR.top);
			mIndicatorStrokePath.lineTo(oR.left - iH - iHSW, oR.top + iVSW + iW / 2f);
			mIndicatorStrokePath.lineTo(oR.left, oR.top + iVSW * 2 + iW);
			mIndicatorStrokePath.lineTo(oR.left, oR.top + iVSW + iW);
			mIndicatorStrokePath.lineTo(oR.left - iH, oR.top + iVSW + iW / 2f);
			mIndicatorStrokePath.lineTo(oR.left, oR.top + iVSW);
			mIndicatorStrokePath.close();
		}
	}
}
