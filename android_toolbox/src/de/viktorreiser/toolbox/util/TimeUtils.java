package de.viktorreiser.toolbox.util;

import java.math.BigInteger;
import java.util.Calendar;

/**
 * Static helper for time related tasks (<b>Beta</b>).
 * 
 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
 */
public class TimeUtils {
	
	/**
	 * Constants for time values (and conversion between them).
	 * 
	 * @author Viktor Reiser &lt;<a href="mailto:viktorreiser@gmx.de">viktorreiser@gmx.de</a>&gt;
	 */
	public static enum TimeUnit {
		NANOSECOND(1L),
		MICROSECOND(1000L),
		MILLISECOND(1000000L),
		SECOND(1000000000L),
		MINUTE(60L * 1000000000L),
		HOUR(360L * 1000000000L),
		DAY(8640L * 1000000000L),
		WEEK(60480 * 1000000000L),
		MONTH(259200L * 1000000000L),
		YEAR(3110400L * 1000000000L);
		
		private final long mValue;
		
		TimeUnit(long value) {
			mValue = value;
		}
		
		/**
		 * Convert from one time unit to another.<br>
		 * <br>
		 * {@code TimeUnit.SECOND.convert(3600, TimeUnit.MILLISECOND) == 3}<br>
		 * <br>
		 * If converted value is greater than {@link Long#MAX_VALUE} then {@link Long#MAX_VALUE}
		 * will be returned.<br>
		 * <br>
		 * <b>Note</b>: {@link #MONTH} {@code == 30 *} {@link #DAY} and {@link #YEAR}
		 * {@code == 360 *} {@link #DAY}
		 * 
		 * @param value
		 *            time value
		 * @param unit
		 *            time unit of {@code value}
		 * 
		 * @return converted value according to {@code enum} constant on which this method was
		 *         called
		 */
		public long convert(long value, TimeUnit unit) {
			BigInteger i = BigInteger.valueOf(unit.mValue);
			i = i.multiply(BigInteger.valueOf(value));
			i = i.divide(BigInteger.valueOf(mValue));
			
			return i.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) >= 0
					? Long.MAX_VALUE : i.longValue();
		}
	}
	
	/**
	 * Get (internationalized) full name of month.
	 * 
	 * @param month
	 *            1 to 12 (0 is 12, 13 is 1, and so on)
	 * 
	 * @return month name
	 */
	public static String getFullMonthName(int month) {
		Calendar c = Calendar.getInstance();
		c.set(Calendar.MONTH, 0);
		c.add(Calendar.MONTH, month - 1);
		return String.format("%tB", c);
	}
	
	/**
	 * Get (internationalized) short name of month.
	 * 
	 * @param month
	 *            1 to 12 (0 is 12, 13 is 1, and so on)
	 * 
	 * @return month name
	 */
	public static String getShortMonthName(int month) {
		Calendar c = Calendar.getInstance();
		c.set(Calendar.MONTH, 0);
		c.add(Calendar.MONTH, month - 1);
		return String.format("%tb", c);
	}
	
	/**
	 * Get (internationalized) full name of day.
	 * 
	 * @param day
	 *            1 to 7 (0 is 7, 8 is 1, and so on)
	 * 
	 * @return day name
	 */
	public static String getFullDayName(int day) {
		Calendar c = Calendar.getInstance();
		// date doesn't matter - it has to be a Monday
		// I new that first August 2011 is one ;-)
		c.set(2011, 7, 1, 0, 0, 0);
		c.add(Calendar.DAY_OF_MONTH, day - 1);
		return String.format("%tA", c);
	}
	
	/**
	 * Get (internationalized) short name of day.
	 * 
	 * @param day
	 *            1 to 7 (0 is 7, 8 is 1, and so on)
	 * 
	 * @return day name
	 */
	public static String getShortDayName(int day) {
		Calendar c = Calendar.getInstance();
		// date doesn't matter - it has to be a Monday
		// I knew that first August 2011 is one ;-)
		c.set(2011, 7, 1, 0, 0, 0);
		c.add(Calendar.DAY_OF_MONTH, day - 1);
		return String.format("%ta", c);
	}
	
	
	// PRIVATE ====================================================================================
	
	/**
	 * No constructor for static class.
	 */
	private TimeUtils() {
		
	}
}
