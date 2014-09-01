/**
 * TimestampedObject.java (c) Copyright 2013 Graham Webber
 */
package org.gw.objectlogger;

import java.util.Date;

/**
 * An Object used by the {@link ObjectLogger} to log {@link Object}s with a
 * timestamp.
 * 
 * @author gman
 * @since 1.0
 * @version 1.0
 * 
 */
public class TimestampedObject<T> {

	private Date logTime;
	private T obj;

	public TimestampedObject() {
	}
	public TimestampedObject(T obj) {
		this.logTime = new Date();
		this.obj = obj;
	}
	public TimestampedObject(Date logTime, T obj) {
		this.logTime = logTime;
		this.obj = obj;
	}
	public Date getLogTime() {
		return logTime;
	}
	public void setLogTime(Date logTime) {
		this.logTime = logTime;
	}
	public T getObj() {
		return obj;
	}
	public void setObj(T obj) {
		this.obj = obj;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TimestampedObject [logTime=");
		builder.append(logTime);
		builder.append(", obj=");
		builder.append(obj);
		builder.append("]");
		return builder.toString();
	}

}
