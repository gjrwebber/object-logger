package org.gw.objectlogger;

import java.io.File;
import java.util.Date;

/**
 * Interface for a file system {@link IDataSource}.
 * 
 * @author Gman
 * 
 * @param <T>
 *            The parametized type to be logged.
 */
public interface IFileSystemDataSource extends IDataSource {

	/**
	 * Set the filename of the {@link IFileSystemDataSource}
	 * 
	 * @param filename
	 *            The filename
	 */
	void setFilename(String filename);

	/**
	 * Returns the {@link java.io.File} at the current time.
	 * 
	 * @return Returns the {@link java.io.File} at the current time.
	 */
	File getFile();

	/**
	 * Returns the {@link java.io.File} at the given time
	 * 
	 * @param date
	 *            The time predicate for returning the {@link java.io.File}
	 * @return Returns the {@link java.io.File} at the given time
	 */
	File getFile(Date date);
}
