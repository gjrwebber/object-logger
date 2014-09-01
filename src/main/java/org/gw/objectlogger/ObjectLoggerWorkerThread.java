package org.gw.objectlogger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class ObjectLoggerWorkerThread implements Runnable {

	private static Logger logger = LoggerFactory
			.getLogger(ObjectLoggerWorkerThread.class);

	/**
	 * Flag which the Thread uses to determine whether it should continue
	 * running.
	 */
	private final AtomicBoolean running = new AtomicBoolean(true);

	/**
	 * The {@link Thread} which runs this {@link Runnable} for asynchronous
	 * logging. Will only be started when the first logAsync method is called.
	 */
	private Thread thread;

	private Set<ObjectLogger<?>> allObjectLoggers;

	private ReentrantLock lock = new ReentrantLock();

	/**
	 * Starts the Thread
	 */
	public ObjectLoggerWorkerThread() {

		thread = new Thread(this, "ObjectLoggers Worker Thread");
		thread.start();
	}

	/**
	 * Loops through the <code>allObjectLoggers</code> {@link java.util.Set} and grabs the
	 * next
	 */
	@Override
	public void run() {
		if (getAllObjectLoggers() == null) {
			logger.info("allObjectLoggers is null. Shutting down ObjectLoggerWorkerThread.");
			return;
		}
		running.set(true);

		logger.info("ObjectLoggerWorkerThread running...");

		while (running.get()) {
			lock.lock();
			try {
				for (ObjectLogger<?> objLogger : getAllObjectLoggers()) {
					try {
						objLogger.logAllInQueue();
					} catch (Exception e) {
						logger.error(
								"An exception was caught in the ObjectLoggers Logger Thread: "
										+ e.getMessage(), e);
					}
				}
			} finally {
				lock.unlock();
			}
			try {
				if (getAllObjectLoggers().size() > 0) {
					Thread.sleep(100);
				} else {
					Thread.sleep(1000);
				}
			} catch (InterruptedException e) {
				// Don't care
			}
		}

		logger.info("ObjectLoggerWorkerThread stopped.");
	}

	/**
	 * To shutdown the asynchronous logger.
	 */
	public void shutdown() {
		running.set(false);
		if (thread != null) {
			thread.interrupt();
		}
		thread = null;
	}

	/**
	 * @return the allObjectLoggers
	 */
	public Set<ObjectLogger<?>> getAllObjectLoggers() {
		if (allObjectLoggers == null) {
			allObjectLoggers = new HashSet<ObjectLogger<?>>();
		}
		return allObjectLoggers;
	}

	/**
	 * @param allObjectLoggers
	 *            the allObjectLoggers to set
	 */
	public void setAllObjectLoggers(Set<ObjectLogger<?>> allObjectLoggers) {
		this.allObjectLoggers = allObjectLoggers;
	}

	/**
	 * Adds and {@link ObjectLogger} to the set of all {@link ObjectLogger}s
	 * 
	 * @param objLogger
	 */
	public void addLogger(ObjectLogger<?> objLogger) {
		lock.lock();
		try {
			getAllObjectLoggers().add(objLogger);
		} finally {
			lock.unlock();
		}
	}

}
