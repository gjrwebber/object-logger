package org.gw.objectlogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.List;

/**
 * An interface specifying the data source contract for the {@link ObjectLogger}
 * .
 * 
 * @author Gman
 * 
 * @param <T>
 *            The parametized type to be logged.
 */
public interface IDataSource {

	/**
	 * Persists the {@link TimestampedObject} in the data source
	 * 
	 * @param object
	 */
	void persist(TimestampedObject<?> object) throws DataSourceException;

	/**
	 * Persists the {@link java.util.List} of {@link TimestampedObject}s in the data
	 * source
	 * 
	 * @param object
	 */
	void persist(List<TimestampedObject<?>> batch) throws DataSourceException;

	/**
	 * Returns all {@link TimestampedObject}s in the given {@link java.io.File}.
	 * 
	 * @param type
	 *            The type of Object expected to be return. Should be the same
	 *            type as persisted by this {@link IDataSource}.
	 * @param file
	 *            The File to get all Objects
	 * @return Returns all {@link TimestampedObject}s in the given {@link java.io.File}.
	 * @throws java.io.FileNotFoundException
	 *             If the file could not be found.
	 */
	<T> TimestampedObjectSet<T> getAll(Class<T> type, File file)
			throws FileNotFoundException;

	/**
	 * Returns all {@link TimestampedObject}s from all available {@link java.io.File}s
	 * from the given {@link java.util.Date}.
	 * 
	 * @param type
	 *            The type of Object expected to be return. Should be the same
	 *            type as persisted by this {@link IDataSource}.
	 * @param from
	 *            The {@link java.util.Date} for the earliest File inclusive.
	 * @return Returns all {@link TimestampedObject}s in the given {@link java.io.File}.
	 * @throws java.io.FileNotFoundException
	 *             If the file could not be found.
	 */
	<T> TimestampedObjectSet<T> getAll(Class<T> type, Date from)
			throws FileNotFoundException;

	/**
	 * Returns all {@link TimestampedObject}s from all available {@link java.io.File}s.
	 * 
	 * @param type
	 *            The type of Object expected to be return. Should be the same
	 *            type as persisted by this {@link IDataSource}.
	 * @return Returns all {@link TimestampedObject}s from all available files.
	 * @throws java.io.FileNotFoundException
	 *             If the file could not be found.
	 */
	<T> TimestampedObjectSet<T> getAll(Class<T> type)
			throws FileNotFoundException;

	/**
	 * Returns all {@link TimestampedObject}s from all available {@link java.io.File}s
	 * between the given {@link java.util.Date}s.
	 * 
	 * @param type
	 *            The type of Object expected to be return. Should be the same
	 *            type as persisted by this {@link IDataSource}.
	 * @param from
	 *            The {@link java.util.Date} for the earliest File.
	 * @param to
	 *            The {@link java.util.Date} for the latest File exclusive.
	 * @return Returns all {@link TimestampedObject}s in the given {@link java.io.File}.
	 * @throws java.io.FileNotFoundException
	 *             If the file could not be found.
	 */
	<T> TimestampedObjectSet<T> getAll(Class<T> type, Date from, Date to)
			throws FileNotFoundException;

	/**
	 * Cleans the {@link IDataSource}. See concrete classes for more info.
	 */
	void clean();
}
