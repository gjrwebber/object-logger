package org.gw.objectlogger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The file is rolled in accordance with the <code>rollingPeriodInMins</code>
 * parameter. The rolling occurs on the minute, so if an object is first logged
 * at 14:19:58 and then at 14:20:01, and the <code>rollingPeriodInMins</code>
 * parameter is set to 1, there will be 2 files at this point in time, one for
 * 14:59 and for 14:20.
 * 
 * @author gman
 * @since 1.0
 * @version 1.0
 * 
 */
public class MinuteRollingStrategy extends DailyRollingStrategy {

	private static Logger logger = LoggerFactory
			.getLogger(MinuteRollingStrategy.class);

	/**
	 * The default period in minutes for rolling the log file.
	 */
	private static final int defaultRollingPeriodInMins = 30;

	/**
	 * The period specifying how many minutes worth of data one file should
	 * contain.
	 */
	private int rollingPeriodInMins;

	/**
	 * max rolling limit
	 */
	private final int maxRolling = 60 * 24;

	/**
	 * min rolling limit
	 */
	private final int minRolling = 1;

	/**
	 * milliseconds in a second
	 */
	private final int millisPerMin = 60000;

	/**
     * 
     */
	public MinuteRollingStrategy() {
		this(defaultRollingPeriodInMins);

		// Call didRoll() so that the nextRoll will be set
		didRoll();
	}

	public MinuteRollingStrategy(int rollingPeriodInMins) {
		this.rollingPeriodInMins = rollingPeriodInMins;
	}

	/**
	 * @return the rollingPeriodInMins
	 */
	public int getRollingPeriodInMins() {
		return rollingPeriodInMins;
	}

	/**
	 * @param rollingPeriodInMins
	 *            the rollingPeriodInMins to set
	 */
	public void setRollingPeriodInMins(int rollingPeriodInMins) {
		if (rollingPeriodInMins > maxRolling) {
			rollingPeriodInMins = maxRolling;
		} else if (rollingPeriodInMins < minRolling) {
			rollingPeriodInMins = minRolling;
		}
		this.rollingPeriodInMins = rollingPeriodInMins;
	}

	/**
	 * Calculates the next roll time by calculating the time between now and the
	 * next minute using rollingPeriodInMins. If the time falls on the next day,
	 * the nextRoll will be 00:00.
	 */
	@Override
	public void didRoll() {
		logger.debug("Rolled minute log file.");
		long nextMin = System.currentTimeMillis() + getRollingPeriodInMins()
				* millisPerMin;
		long remainder = nextMin % millisPerMin;
		long nextRoll = nextMin - remainder;
		setNextRoll(nextRoll);
	}

}
