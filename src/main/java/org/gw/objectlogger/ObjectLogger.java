package org.gw.objectlogger;

import org.gw.commons.utils.GenericsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Abstract Object logger class that logs an {@link Object} to an
 * {@link IDataSource} asynchronously by default. To log synchronously, set
 * <code>synchronous</code to true.
 * <p>
 * The default {@link IDataSource} used is a {@link FileSystemDataSource} with
 * the &lt;T&gt; type simple name as the log filename. See
 * {@link FileSystemDataSource} for details on it's defaults.
 * 
 * 
 * @author gman
 * @since 1.0
 * @version 1.0
 * 
 * @param <T>
 *            THe {@link Object} to log
 */
public abstract class ObjectLogger<T> implements IObjectLogger<T> {

	private static Logger logger = LoggerFactory.getLogger(ObjectLogger.class);

	@Value("${object.logger.enable:true}")
	private boolean enabled = true;

	@Value("${object.logger.synchronous:false}")
	private boolean synchronous = false;

	@Value("${object.logger.clean:false}")
	private boolean clean = false;

	/**
	 * The {@link IDataSource} for this {@link ObjectLogger}. Defaults to the
	 * {@link FileSystemDataSource}.
	 */
	private IDataSource dataSource;

	/**
	 * The {@link java.util.concurrent.BlockingQueue} to store Objects that have yet to be logged.
	 */
	private final BlockingQueue<TimestampedObject<T>> queue;

	/**
	 * The default queue capacity
	 */
	private static int defaultQueueCapacity = 100000;

	/**
	 * The static worker thread that makes the {@link ObjectLogger} logs the
	 * objects in its queue. For it to do that it must add itself to the
	 * {@link ObjectLoggerWorkerThread}'s set of {@link ObjectLogger}s.
	 */
	private final static ObjectLoggerWorkerThread worker = new ObjectLoggerWorkerThread();

	/**
	 * Creates a {@link ObjectLogger} using a {@link java.util.concurrent.LinkedBlockingQueue} with
	 * the default capacity and a default {@link FileSystemDataSource}.
	 */
	public ObjectLogger() {
		this(defaultQueueCapacity);
	}

	/**
	 * Creates a {@link ObjectLogger} using a {@link java.util.concurrent.LinkedBlockingQueue} with
	 * the default capacity and the given {@link IDataSource}.
	 */
	public ObjectLogger(IDataSource dataSource) {
		this(defaultQueueCapacity, dataSource);
	}

	/**
	 * Creates a {@link ObjectLogger} using a {@link java.util.concurrent.LinkedBlockingQueue} with
	 * the given capacity and a default {@link FileSystemDataSource}.
	 */
	@SuppressWarnings("unchecked")
	public ObjectLogger(int capacity) {
		queue = new LinkedBlockingQueue<TimestampedObject<T>>(capacity);

		String filename = ((Class<T>) GenericsUtil.getGenericType(this
                .getClass())).getSimpleName();
		this.dataSource = new FileSystemDataSource(filename);

		worker.addLogger(this);
	}

	/**
	 * Creates a {@link ObjectLogger} using a {@link java.util.concurrent.LinkedBlockingQueue} with
	 * the given capacity and {@link IDataSource}.
	 */
	public ObjectLogger(int capacity, IDataSource dataSource) {
		queue = new LinkedBlockingQueue<TimestampedObject<T>>(capacity);
		this.dataSource = dataSource;

		worker.addLogger(this);

	}

	/**
	 * Cleans the datasource if the clean flag is set.
	 */
	@PostConstruct
	public void init() {
		if (clean) {
			this.dataSource.clean();
		}
	}

	/**
	 * Logs the &lt;T&gt; asynchronously unless <code>synchronous</code> is set
	 * to true. If not, the method returns immediately and the &lt;T&gt; is
	 * added to a {@link java.util.concurrent.BlockingQueue} which is read and logged in sequential
	 * order.
	 * 
	 * @param object
	 *            The &lt;T&gt; to be logged.
	 */
	@Override
	public void log(T object) {

		// Return if disabled
		if (!isEnabled()) {
			return;
		}

		// Nothing to log.
		if (object == null) {
			logger.warn("Logger was passed a null object.");
			return;
		}

		log(new TimestampedObject<T>(object));
	}

	/**
	 * Logs the {@link TimestampedObject} asynchronously unless
	 * <code>synchronous</code> is set to true. If not, the method returns
	 * immediately and the {@link TimestampedObject} is added to a
	 * {@link java.util.concurrent.BlockingQueue} which is read and logged in sequential order.
	 * 
	 * @param object
	 *            The {@link TimestampedObject} to be logged.
	 */
	@Override
	public void log(TimestampedObject<T> object) {

		// Return if disabled
		if (!isEnabled()) {
			return;
		}

		// Nothing to log.
		if (object == null) {
			logger.warn("Logger was passed a null object.");
			return;
		}

		// If synchronous call asyncLog
		if (isSynchronous()) {
			doLog(object);
		} else {
			// if asynchronous add it to the queue.
			try {
				queue.add(object);
			} catch (IllegalStateException e) {
				// If capacity is too great.
				logger.error("Could not log object as the Async Logger queue has reached capacity. Discarding.");
			}
		}
	}

	/**
	 * Calls persist on the {@link IDataSource}. Default access applied as the
	 * ObjectLoggerWorkerThread needs to use this method.
	 */
	private void doLog(TimestampedObject<?> object) {
		// Nothing to log.
		if (object == null) {
			logger.warn("Logger was passed a null object.");
			return;
		}

		try {

			if (logger.isDebugEnabled()) {
				logger.debug("Logging object of type: "
						+ object.getClass().getSimpleName());
			}

			dataSource.persist(object);

			if (logger.isDebugEnabled()) {
				logger.debug("Finished logging object of type: "
						+ object.getClass().getSimpleName());
			}
		} catch (DataSourceException e) {
			logger.error(
					"Could not log data as an DataSourceException occured.", e);
		}
	}

	/**
	 * Logs all {@link TimestampedObject}s in the queue.
	 */
	void logAllInQueue() {
		if (queue.isEmpty()) {
			return;
		}
		List<TimestampedObject<?>> readyToLog = new ArrayList<TimestampedObject<?>>();
		queue.drainTo(readyToLog);

		try {

			if (logger.isDebugEnabled()) {
				logger.debug("Logging batch of type: "
						+ readyToLog.get(0).getClass().getSimpleName());
			}

			dataSource.persist(readyToLog);

			if (logger.isDebugEnabled()) {
				logger.debug("Finished logging batch of type: "
						+ readyToLog.get(0).getClass().getSimpleName());
			}
		} catch (DataSourceException e) {
			logger.error(
					"Could not log batch as an DataSourceException occured.", e);
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * @return the synchronous
	 */
	public boolean isSynchronous() {
		return synchronous;
	}

	/**
	 * @param synchronous
	 *            the synchronous to set
	 */
	public synchronized void setSynchronous(boolean synchronous) {
		this.synchronous = synchronous;
	}

	/**
	 * @return the dataSource
	 */
	public IDataSource getDataSource() {
		return dataSource;
	}

	/**
	 * @param dataSource
	 *            the dataSource to set
	 */
	public void setDataSource(IDataSource dataSource) {
		this.dataSource = dataSource;
	}

	public int getQueueSize() {
		return queue.size();
	}

	/**
	 * @return the clean
	 */
	public boolean isClean() {
		return clean;
	}

	/**
	 * @param clean
	 *            the clean to set
	 */
	public void setClean(boolean clean) {
		this.clean = clean;
	}
}
