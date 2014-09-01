/**
 * TestObject.java (c) Copyright 2013 Graham Webber
 */
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

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author gman
 * @since 1.0
 * @version 1.0
 * 
 */
public class TestObject implements ITimestampedObjectSerialiser {

	public String name;
	private String other;

	private DataOutputStream output;

	public TestObject() {
	}
	/**
     * 
     */
	public TestObject(String bob, String other) {
		this.name = bob;
		this.other = other;
	}

	public String getOther() {
		return other;
	}

	public void setOther(String other) {
		this.other = other;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((other == null) ? 0 : other.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		TestObject other = (TestObject) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (this.other == null) {
			if (other.other != null) {
				return false;
			}
		} else if (!this.other.equals(other.other)) {
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.gw.objectlogger.ITimestampedObjectSerialiser#serialise
	 * (java.lang.Object)
	 */
	public byte[] serialise(TimestampedObject<?> object)
			throws SerialisationException {
		// if (!(object.getObj() instanceof TestObject)) {
		// throw new SerialisationException("Not a TestObject!");
		// }
		TestObject testObj = (TestObject) object.getObj();
		byte[] nameBytes = testObj.name.getBytes();
		byte[] otherBytes = testObj.other.getBytes();
		ByteBuffer buffer = ByteBuffer.allocate(8 + 2 + nameBytes.length
				+ otherBytes.length);
		buffer.putLong(object.getLogTime().getTime());
		buffer.put((byte) nameBytes.length);
		buffer.put(nameBytes);
		buffer.put((byte) otherBytes.length);
		buffer.put(otherBytes);
		return buffer.array();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.gw.objectlogger.ITimestampedObjectSerialiser#
	 * deserialise(byte[])
	 */
	@SuppressWarnings("unchecked")
	public <T> TimestampedObject<T> deserialise(Class<T> type, byte[] bytes)
			throws DeserialisationException {
		// if (!SerialisableTestObject.class.equals(type)) {
		// throw new DeserialisationException("Not a TestObject!");
		// }
		TestObject obj = new TestObject();
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		Date time = new Date(buffer.getLong());
		byte len = buffer.get();
		byte[] nameBytes = new byte[len];
		for (int i = 0; i < nameBytes.length; i++) {
			nameBytes[i] = buffer.get();
		}
		obj.name = new String(nameBytes);
		len = buffer.get();
		nameBytes = new byte[len];
		for (int i = 0; i < nameBytes.length; i++) {
			nameBytes[i] = buffer.get();
		}
		obj.other = new String(nameBytes);
		return (TimestampedObject<T>) new SerialisableTestObject(time, obj);
	}

	@Override
	public String toString() {
		return "TestObject [name=" + name + "]";
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
	public void write(List<TimestampedObject<?>> batch) throws IOException,
			SerialisationException {
		for (TimestampedObject<?> obj : batch) {
			write(obj);
		}
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
				e.printStackTrace();
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
	@JsonIgnore
	public boolean isOpen() {
		return output != null;
	}
	@Override
	@JsonIgnore
	public String getExtension() {
		return "test";
	}
}
