package org.gw.objectlogger;

/**
 * An interface defining the strategy for rolling {@link FileSystemDataSource}
 * log files.
 * 
 * @author Gman
 * 
 */
public interface IRollingStrategy {

	/**
	 * Called when the log file has been rolled.
	 */
	void didRoll();

	/**
	 * Called to ascertain whether the log file should be rolled.
	 * 
	 * @return
	 */
	boolean doRoll();

}
