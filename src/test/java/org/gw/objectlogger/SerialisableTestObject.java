/**
 * SerialisableTestObject.java (c) Copyright 2013 Graham Webber
 */
package org.gw.objectlogger;

import java.util.Date;

/**
 * @author gman
 * @since 1.0
 * @version 1.0
 *
 */
public class SerialisableTestObject extends TimestampedObject<TestObject> {

    public SerialisableTestObject() {
    }

    /**
     * 
     * @param obj
     */
    public SerialisableTestObject(TestObject obj) {
        super(obj);
    }
    
    public SerialisableTestObject(Date logTime, TestObject obj) {
        super(logTime, obj);
    }

}
