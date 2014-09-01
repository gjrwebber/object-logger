package org.gw.objectlogger;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * An interface to specify the serialisation of a {@link TimestampedObject}.
 * 
 * @author Gman
 * 
 * @param <T>
 *            The underlying type of {@link TimestampedObject}
 */
public interface ITimestampedObjectSerialiser {
	void write(TimestampedObject<?> object) throws IOException,
			SerialisationException;
	void write(List<TimestampedObject<?>> batch) throws IOException,
			SerialisationException;
	<T> TimestampedObjectSet<T> readAll(File file, Class<T> type)
			throws IOException, DeserialisationException;
	void open(File file) throws IOException;
	void close() throws IOException;
	String getExtension();
	boolean isOpen();
}
