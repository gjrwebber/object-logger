package org.gw.objectlogger;

/**
 * A concrete {@link ObjectLogger} for byte arrays
 * 
 * @author Gman
 * 
 */
public final class ByteArrayLogger extends ObjectLogger<byte[]> {

	/**
	 * Creates a {@link ByteArrayLogger} using a {@link FileSystemDataSource}
	 * with the given filename and a {@link MinuteRollingStrategy}.
	 */
	public ByteArrayLogger(String filename) {
		super(new FileSystemDataSource(filename, new MinuteRollingStrategy()));
	}

	/**
	 * @param dataSource
	 *            A byte array {@link IDataSource}
	 */
	public ByteArrayLogger(IDataSource dataSource) {
		super(dataSource);
	}

	/**
	 * @param capacity
	 *            The capacity of the asynchronous queue
	 * @param dataSource
	 *            A byte array {@link IDataSource}
	 */
	public ByteArrayLogger(int capacity, IDataSource dataSource) {
		super(capacity, dataSource);
	}

	/**
	 * Creates a {@link ByteArrayLogger} using a {@link FileSystemDataSource}
	 * with the given filename and a {@link MinuteRollingStrategy} with the
	 * given <code>rollingPeriodInMins</code>.
	 */
	public ByteArrayLogger(String filename, int capacity,
			int rollingPeriodInMins) {
		super(capacity, new FileSystemDataSource(filename,
				new MinuteRollingStrategy(rollingPeriodInMins)));
	}

}
