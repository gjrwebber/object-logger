package org.gw.objectlogger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;


/**
 * The file is rolled each day at 00:00:00.000 each day.
 * 
 * @author gman
 * @since 1.0
 * @version 1.0
 */
public class DailyRollingStrategy implements IRollingStrategy {

	private static Logger logger = LoggerFactory.getLogger(DailyRollingStrategy.class);

	/**
	 * The time at which the next rolling of files should occur. 0 means it will
	 * be rolled next log.
	 */
	private long nextRoll = 0;

	/**
	 */
	public DailyRollingStrategy() {
	}

	/**
	 * Updates the <code>nextRoll</code> parameter.
	 */
	@Override
	public void didRoll() {
		logger.debug("Rolled daily log file.");
		Calendar tomorrow = Calendar.getInstance();
		tomorrow.add(Calendar.DATE, 1);
		tomorrow.set(Calendar.HOUR_OF_DAY, 0);
		tomorrow.set(Calendar.MINUTE, 0);
		tomorrow.set(Calendar.SECOND, 0);
		tomorrow.set(Calendar.MILLISECOND, 0);
		setNextRoll(tomorrow.getTimeInMillis());
	}

	/**
	 * Returns true if we should roll the file now. Return false by default
	 * (nextRoll = Long.MAX_VALUE) as rolling is optional.
	 * 
	 * @return Returns false as we do not want to roll by default.
	 */
	@Override
	public boolean doRoll() {
		boolean result = System.currentTimeMillis() >= nextRoll;
		if (logger.isDebugEnabled() && result) {
			logger.debug("Rolling " + this.getClass().getSimpleName()
					+ " log file.");
		}
		return result;
	}

	/**
	 * @param nextRoll
	 *            the nextRoll to set
	 */
	public void setNextRoll(long nextRoll) {
		this.nextRoll = nextRoll;
	}

}
