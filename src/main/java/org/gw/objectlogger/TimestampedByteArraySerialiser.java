package org.gw.objectlogger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link ITimestampedObjectSerialiser} that de/serialises a byte array.
 * 
 * @author Gman
 * 
 */
public class TimestampedByteArraySerialiser
		implements
			ITimestampedObjectSerialiser {

	private static Logger logger = LoggerFactory
			.getLogger(TimestampedByteArraySerialiser.class);

	private DataOutputStream output;

	private static final String extension = "data";

	public TimestampedByteArraySerialiser() {
	}

	/**
	 * 
	 * @param serialisable
	 * @return
	 * @throws SerialisationException
	 */
	private byte[] serialise(TimestampedObject<?> serialisable)
			throws SerialisationException {
		try {
			byte[] data = (byte[]) serialisable.getObj();
			ByteBuffer buffer = ByteBuffer.allocate(data.length + 8);
			buffer.putLong(serialisable.getLogTime().getTime());
			buffer.put(data, 0, data.length);
			return buffer.array();
		} catch (ClassCastException e) {
			throw new SerialisationException("Expected byte array, but got "
					+ serialisable.getObj().getClass().getSimpleName());
		}
	}

	/**
	 * 
	 * @param type
	 * @param bytes
	 * @return
	 * @throws DeserialisationException
	 */
	@SuppressWarnings("unchecked")
	private <T> TimestampedObject<T> deserialise(Class<T> type, byte[] bytes)
			throws DeserialisationException {
		if (!type.equals(byte[].class)) {
			throw new IllegalStateException("Expected byte array, but got "
					+ type.getSimpleName());
		}
		TimestampedObject<byte[]> serialisable = new TimestampedObject<byte[]>();
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		serialisable.setLogTime(new Date(buffer.getLong()));
		byte[] data = new byte[bytes.length - 8];
		buffer.get(data, 0, data.length);
		serialisable.setObj(data);
		return (TimestampedObject<T>) serialisable;
	}

	@Override
	public void write(TimestampedObject<?> object) throws IOException,
			SerialisationException {
		byte[] data = serialise(object);
		output.writeInt(data.length);
		output.write(data);
		output.flush();
	}

	@Override
	public <T> TimestampedObjectSet<T> readAll(File file, Class<T> type)
			throws IOException, DeserialisationException {

		TimestampedObjectSet<T> set = new TimestampedObjectSet<T>();
		DataInputStream input = new DataInputStream(
				FileUtils.openInputStream(file));
		int length;
		try {
			while ((length = input.readInt()) > 0) {
				byte[] buffer = new byte[length];
				int read = IOUtils.read(input, buffer);
				byte[] actualBytes = ByteBuffer.wrap(buffer, 0, read).array();

				TimestampedObject<T> deserialised = deserialise(type,
						actualBytes);
				set.add(deserialised);
			}
		} catch (EOFException e) {
			// Reached end of file
		} finally {
			try {
				input.close();
			} catch (Exception e) {
				logger.warn(e.getMessage());
			}
		}
		return set;
	}

	@Override
	public void open(File file) throws IOException {
		output = new DataOutputStream(FileUtils.openOutputStream(file));
	}

	@Override
	public void close() throws IOException {
		if (output != null) {
			try {
				output.close();
			} finally {
				output = null;
			}
		}
	}

	@Override
	public String getExtension() {
		return extension;
	}

	@Override
	public boolean isOpen() {
		return output != null;
	}

	/**
	 * Writes the batch synchronously by looping through each
	 * {@link TimestampedObject} and calling write(TimestampedObject)
	 */
	@Override
	public void write(List<TimestampedObject<?>> batch) throws IOException,
			SerialisationException {
		for (TimestampedObject<?> obj : batch) {
			write(obj);
		}
	}

}
