/**
 * TimestampedByteArray.java (c) Copyright 2013 Graham Webber
 */
package org.gw.objectlogger;

import java.util.Date;

/**
 * Concrete {@link TimestampedObject} with it's parametized type as a byte
 * array.
 * 
 * @author gman
 * @since 1.0
 * @version 1.0
 * 
 */
public final class TimestampedByteArray extends TimestampedObject<byte[]> {

	/**
     * 
     */
	public TimestampedByteArray() {
	}

	/**
	 * 
	 * @param obj
	 */
	public TimestampedByteArray(byte[] obj) {
		super(obj);
	}

	/**
	 * @param logTime
	 * @param obj
	 */
	public TimestampedByteArray(Date logTime, byte[] obj) {
		super(logTime, obj);
	}

}
