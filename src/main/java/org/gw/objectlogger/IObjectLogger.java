/**
 * 
 */
package org.gw.objectlogger;

/**
 * A logging interface defining the ability to log {@link TimestampedObject}
 * objects.
 * 
 * @author gman
 * @since 1.0
 * @version 1.0
 * 
 */
public interface IObjectLogger<T> {
	/**
	 * Logs a {@link TimestampedObject} paramatized by &lt;T&gt;
	 * 
	 * @param serialisable
	 *            The {@link TimestampedObject} to log
	 */
	void log(TimestampedObject<T> serialisable);
	/**
	 * Logs an {@link Object} byt wrapping it in a {@link TimestampedObject}
	 * 
	 * @param object
	 *            The {@link Object} to log
	 */
	void log(T object);
	
	/**
	 * Returns the {@link IDataSource} used by this {@link IObjectLogger}
	 * 
	 * @return the {@link IDataSource} used by this {@link IObjectLogger}
	 */
	IDataSource getDataSource();

}
