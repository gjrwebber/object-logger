package org.gw.objectlogger;

import org.apache.commons.io.FileUtils;
import org.gw.commons.utils.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link IFileSystemDataSource} which uses the file system to persist the log
 * data to a folder defined by:
 * <p>
 * 
 * <pre>
 * <code>fileSystemLoggerPath</code>/<code>yyyy-MM-dd</code>/<code>filename</code>
 * <code>fileSystemLoggerPath</code> is by default defined by: FileUtils.getUserDirectoryPath() + File.separator + "ObjectLogger"
 * </pre>
 * 
 * The file will be rolled daily by default. Change to a
 * {@link MinuteRollingStrategy} to roll on the minute instead if required.
 * 
 * @author gman
 * @since 1.0
 * @version 1.0
 * 
 */
public class FileSystemDataSource implements IFileSystemDataSource {

	private static Logger logger = LoggerFactory.getLogger(FileSystemDataSource.class);

	/**
	 * The default log path is the {user.directory}/ObjectLogger
	 */
	private final String defaultObjectLogPath = FileUtils
			.getUserDirectoryPath() + File.separator + "ObjectLogger";

	/**
	 * The default number of days lookup is 28
	 */
	private final String defaultNumberOfDaysLookupStr = "28";

	/**
	 * The absolute path to the log files.
	 */
	private String fileSystemLoggerPath;

	/**
	 * The number of days to go back in time to look for the previous file from
	 * a given date.
	 */
	private int numberOfPastDaysLookup = 0;

	/**
	 * The {@link IRollingStrategy} for this {@link FileSystemDataSource}
	 */
	private IRollingStrategy rollingStrategy;

	/**
	 * The {@link ITimestampedObjectSerialiser} for serialising the logged
	 * {@link Object}. {@link TimestampedObjectJsonSerialiser} is used by
	 * default.
	 */
	private ITimestampedObjectSerialiser serialiser;

	/**
	 * The filename for writing the logged {@link Object}. This file will be
	 * found in <code>fileSystemLoggerPath</code>/ <code>yyyy-MM-dd</code>/
	 * <code>filename</code>
	 */
	private String filename;

	/**
	 * {@link java.util.concurrent.locks.ReentrantLock} to lock writing to the log file.
	 */
	private ReentrantLock writeLock = new ReentrantLock();

	/**
	 * The format of the date for the log folder name
	 */
	private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

	/**
	 * Creates a {@link FileSystemDataSource} with the generic type name as the
	 * filename using a {@link DailyRollingStrategy} and
	 * {@link TimestampedObjectJsonSerialiser}.
	 */
	public FileSystemDataSource(Class<?> type) {
		this(type.getSimpleName(), new DailyRollingStrategy(),
				new TimestampedObjectJsonSerialiser());
	}

	/**
	 * Creates a {@link FileSystemDataSource} with the given filename using the
	 * default {@link DailyRollingStrategy} and
	 * {@link TimestampedObjectJsonSerialiser}.
	 */
	public FileSystemDataSource(String filename) {
		this(filename, new DailyRollingStrategy(),
				new TimestampedObjectJsonSerialiser());
	}

	/**
	 * Creates a {@link FileSystemDataSource} with the given filename and
	 * {@link IRollingStrategy} and using the default
	 * {@link TimestampedObjectJsonSerialiser}.
	 */
	public FileSystemDataSource(String filename,
			IRollingStrategy rollingStrategy) {
		this(filename, rollingStrategy, new TimestampedObjectJsonSerialiser());
	}

	/**
	 * Creates a {@link FileSystemDataSource} with the given filename and
	 * {@link ITimestampedObjectSerialiser} and using the default
	 * {@link DailyRollingStrategy}.
	 */
	public FileSystemDataSource(String filename,
			ITimestampedObjectSerialiser serialiser) {
		this(filename, new DailyRollingStrategy(), serialiser);
	}

	/**
	 * Creates a {@link FileSystemDataSource} with the given filename,
	 * {@link ITimestampedObjectSerialiser} and {@link IRollingStrategy}.
	 */
	public FileSystemDataSource(String filename,
			IRollingStrategy rollingStrategy,
			ITimestampedObjectSerialiser serialiser) {

		setFilename(filename);
		setRollingStrategy(rollingStrategy);
		setSerialiser(serialiser);

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				if (getSerialiser() != null && getSerialiser().isOpen()) {
					try {
						getSerialiser().close();
					} catch (IOException e) {
						logger.warn(
								"Could not close serialiser: " + e.getMessage(),
								e);
					}
				}
			}
		}));
	}

	/**
	 * Appends the given {@link Object} to the file writing the length of the
	 * byte array first followed by the byte array itself
	 * 
	 * @throws DataSourceException
	 */
	@Override
	public void persist(TimestampedObject<?> object) throws DataSourceException {
		// Nothing to log.
		if (object == null) {
			logger.warn("FileSystemDataSource was passed a null object.");
			return;
		}

		writeLock.lock();

		try {
			/* Check if we roll the log */
			if (getSerialiser().isOpen() && rollingStrategy.doRoll()) {
				getSerialiser().close();
			}

			/*
			 * Open a new stream if output is null. This will happen after
			 * rolling
			 */
			if (!getSerialiser().isOpen()) {
				getSerialiser().open(getFile());
				rollingStrategy.didRoll();
				// openOutputStream();
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Persisting object of type: "
						+ object.getClass().getSimpleName());
			}

			// Call write() on the Serialiser
			getSerialiser().write(object);

			if (logger.isDebugEnabled()) {
				logger.debug("Finished persisting object of type: "
						+ object.getClass().getSimpleName());
			}
		} catch (SerialisationException e) {
			throw new DataSourceException(
					"Could not log data as the object could not be serialised.",
					e);
		} catch (IOException e) {
			throw new DataSourceException(
					"Could not log data as an IOException occured.", e);
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Appends the given {@link Object} to the file writing the length of the
	 * byte array first followed by the byte array itself
	 * 
	 * @throws DataSourceException
	 */
	@Override
	public void persist(List<TimestampedObject<?>> batch)
			throws DataSourceException {
		// Nothing to log.
		if (batch == null || batch.isEmpty()) {
			logger.warn("FileSystemDataSource was passed a null batch or batch was empty.");
			return;
		}

		writeLock.lock();

		try {
			/* Check if we roll the log */
			if (getSerialiser().isOpen() && rollingStrategy.doRoll()) {
				getSerialiser().close();
			}

			/*
			 * Open a new stream if output is null. This will happen after
			 * rolling
			 */
			if (!getSerialiser().isOpen()) {
				getSerialiser().open(getFile());
				rollingStrategy.didRoll();
				// openOutputStream();
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Persisting batch of type: "
						+ batch.get(0).getClass().getSimpleName());
			}

			// Call write() on the Serialiser
			getSerialiser().write(batch);

			if (logger.isDebugEnabled()) {
				logger.debug("Finished persisting batch of type: "
						+ batch.get(0).getClass().getSimpleName());
			}
		} catch (SerialisationException e) {
			throw new DataSourceException(
					"Could not log data as the batch could not be serialised.",
					e);
		} catch (IOException e) {
			throw new DataSourceException(
					"Could not log batch as an IOException occured.", e);
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Returns the existing {@link java.io.File}s between the given date range. If there
	 * are no {@link java.io.File}s in the given {@link java.util.Date}s a
	 * {@link java.io.FileNotFoundException} is thrown.
	 * 
	 * @param from
	 *            The from {@link java.util.Date} of the range (inclusive)
	 * @param to
	 *            The to {@link java.util.Date} of the range (exclusive)
	 * @return A {@link java.util.List} of {@link java.io.File}s in the given {@link java.util.Date} range
	 */
	protected List<File> getExistingFilesInRange(Date from, Date to)
			throws FileNotFoundException {
		List<File> filesInRange = new ArrayList<File>();

		/*
		 * Round the dates to the minute
		 */
		Date fromRounded = DateUtil.roundDownToMinute(from);
		Date toRounded = DateUtil.roundUpToMinute(to);

		/*
		 * Get a Calendar representation for easy manipulation
		 */
		Calendar cal = Calendar.getInstance();
		cal.setTime(fromRounded);

		File file;

		/*
		 * while "to" is not before the incrementing calendar
		 */
		while (cal.getTime().before(toRounded)) {
			file = getFile(cal.getTime());
			if (file.exists()) {
				filesInRange.add(file);
			}
			cal.add(Calendar.MINUTE, 1);
		}

		if (filesInRange.isEmpty()) {
			throw new FileNotFoundException("Could not find any "
					+ getFilename() + " at " + getFileSystemLoggerPath()
					+ " files between " + from + " (inclusive) and  " + to
					+ " (exclusive).");
		}
		return filesInRange;
	}

	/**
	 * Returns the {@link java.io.File} this logger will log to using the current date
	 * and time.
	 * 
	 * @return Returns the {@link java.io.File} this logger will log to using the given
	 *         date and time.
	 */
	@Override
	public File getFile() {
		return getFile(new Date());
	}

	/**
	 * Returns the {@link java.io.File} this logger will log to using the given date and
	 * time. The filename will have the hour and minute in the name.
	 * 
	 * @return Returns the {@link java.io.File} this logger will log to using the given
	 *         date and time.
	 */
	@Override
	public File getFile(Date date) {
		NumberFormat format = NumberFormat.getInstance();
		format.setParseIntegerOnly(true);
		format.setMinimumIntegerDigits(2);
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		String hour = format.format(cal.get(Calendar.HOUR_OF_DAY));
		String min = format.format(cal.get(Calendar.MINUTE));
		String theFilename = getFilename();
		theFilename = theFilename.replace(".", "-" + hour + "-" + min + ".");

		File parent = FileUtils.getFile(getFileSystemLoggerPath(),
				getRelativePath(date));
		return new File(parent, theFilename);
	}

	/**
	 * Returns all Serialised objects in the <code>fileSystemLoggerPath</code>
	 */
	@Override
	public <T> TimestampedObjectSet<T> getAll(Class<T> type)
			throws FileNotFoundException {
		assert type != null : "type cannot be null";

		Calendar from = Calendar.getInstance();
		from.add(Calendar.DATE, getNumberOfPastDaysLookup() * -1);
		return getAll(type, from.getTime());
	}

	/**
	 * Returns all Serialised objects in the <code>fileSystemLoggerPath</code>
	 * from the given {@link java.util.Date}.
	 */
	@Override
	public <T> TimestampedObjectSet<T> getAll(Class<T> type, Date from)
			throws FileNotFoundException {
		assert type != null : "type cannot be null";
		assert from != null : "from should not be null";

		TimestampedObjectSet<T> set = new TimestampedObjectSet<T>();
		for (File file : getExistingFilesInRange(from, new Date())) {
			TimestampedObjectSet<T> fileSet = getAll(type, file);
			set.addAll(fileSet);
		}
		return set;
	}

	/**
	 * Returns all Serialised objects in the <code>fileSystemLoggerPath</code>
	 * from the given {@link java.util.Date} (inclusive) to the given {@link java.util.Date}
	 * (exclusive).
	 * 
	 */
	@Override
	public <T> TimestampedObjectSet<T> getAll(Class<T> type, Date from, Date to)
			throws FileNotFoundException {
		assert type != null : "type cannot be null";
		assert from != null : "from should not be null";
		assert to != null : "to should not be null";
		assert from.before(to) : "from ("+from+") should be before to("+to+").";

		TimestampedObjectSet<T> set = new TimestampedObjectSet<T>();
		for (File file : getExistingFilesInRange(from, to)) {

			TimestampedObjectSet<T> fileSet = getAll(type, file);
			set.addAll(fileSet);
		}
		return set;
	}

	/**
	 * Return the contents of the given log file as a
	 * {@link TimestampedObjectSet} of {@link Object} of type T.
	 */
	@Override
	public <T> TimestampedObjectSet<T> getAll(Class<T> type, File file)
			throws FileNotFoundException {
		assert type != null : "type cannot be null";
		assert file != null : "file cannot be null";

		TimestampedObjectSet<T> set = null;
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Getting objects from: " + file.getAbsolutePath());
			}
			set = getSerialiser().readAll(file, type);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		} catch (DeserialisationException e) {
			// Should never happen
			logger.error("Could not deserialise appended object.", e);
		}
		return set;
	}

	/**
	 * @return the fileSystemLoggerPath
	 */
	protected String getFileSystemLoggerPath() {
		if (fileSystemLoggerPath == null || fileSystemLoggerPath.length() == 0) {
			fileSystemLoggerPath = System.getProperty("object.logger.path",
					defaultObjectLogPath);
		}
		return fileSystemLoggerPath;
	}

	/**
	 * @param objectLoggerPath
	 *            the objectLoggerPath to set
	 */
	public void setFileSystemLoggerPath(String objectLoggerPath) {
		this.fileSystemLoggerPath = objectLoggerPath;
	}

	/**
	 * Returns the relative path as the givens date to the log file for this
	 * generic object
	 * 
	 * @param date
	 *            The {@link java.util.Date} for the relative path
	 * @return Returns the relative path as the givens date to the log file for
	 *         this generic object
	 */

	protected String getRelativePath(Date date) {
		return formatter.format(date);
	}

	/**
	 * Returns the relative path as todays date to the log file for this generic
	 * object
	 * 
	 * @return Returns the relative path as todays date to the log file for this
	 *         generic object
	 */
	protected String getRelativePath() {
		return getRelativePath(new Date());
	}

	/**
	 * Returns the filename of the log file. It is calculated by:
	 * <p>
	 * <code>filename + "." + getSerialiser().getExtension()</code>
	 * <p>
	 * 
	 * 
	 * @return
	 */
	public String getFilename() {
		if (getSerialiser() != null) {
			return filename + "." + getSerialiser().getExtension();
		} else {
			return filename + ".log";
		}
	}

	/**
	 * 
	 * @param filename
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	public ITimestampedObjectSerialiser getSerialiser() {
		return serialiser;
	}

	public void setSerialiser(ITimestampedObjectSerialiser serialiser) {
		this.serialiser = serialiser;
	}

	/**
	 * @return the numberOfPastDaysLookup
	 */
	public int getNumberOfPastDaysLookup() {
		if (numberOfPastDaysLookup == 0) {

			String str = null;
			try {
				str = System.getProperty("object.logger.lookup.days",
						defaultNumberOfDaysLookupStr);
				int days = Integer.parseInt(str);
				if (days <= 0) {
					throw new NumberFormatException();
				}
				numberOfPastDaysLookup = days;
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException(
						"Could not parse -Dobject.logger.lookup.days. Expected number > 0, found "
								+ str);
			}
		}
		return numberOfPastDaysLookup;
	}

	/**
	 * Cleans the current log folder by deleting it.
	 */
	public void clean() {
		try {
			File folder = getFile().getParentFile();
			if (folder.exists()) {
				FileUtils.deleteDirectory(folder);
			}
		} catch (IOException e) {
			logger.warn(getRelativePath() + " could not be deleted. ERROR: "
					+ e.getMessage());
		}
	}

	/**
	 * @param numberOfPastDaysLookup
	 *            the numberOfPastDaysLookup to set
	 */
	public void setNumberOfPastDaysLookup(int numberOfPastDaysLookup) {
		this.numberOfPastDaysLookup = numberOfPastDaysLookup;
	}

	/**
	 * @return the rollingStrategy
	 */
	public IRollingStrategy getRollingStrategy() {
		return rollingStrategy;
	}

	/**
	 * @param rollingStrategy
	 *            the rollingStrategy to set
	 */
	public void setRollingStrategy(IRollingStrategy rollingStrategy) {
		this.rollingStrategy = rollingStrategy;
	}

}
