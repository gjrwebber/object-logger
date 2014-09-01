/**
 * TimestampedObjectSetTest.java (c) Copyright 2013 Graham Webber
 */
package org.gw.objectlogger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.gw.objectlogger.TimestampedObject;
import org.gw.objectlogger.TimestampedObjectSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author gman
 * @since 1.0
 * @version 1.0
 * 
 */
public class TimestampedObjectSetTest {

    private Comparator<TimestampedObject<SerialisableTestObject>> oneParamComparator = new Comparator<TimestampedObject<SerialisableTestObject>>() {

        @Override
        public int compare(TimestampedObject<SerialisableTestObject> obj1, TimestampedObject<SerialisableTestObject> obj2) {
            return obj1.getObj().one - obj2.getObj().one;
        }
    };
    private TimestampedObjectSet<SerialisableTestObject> set = new TimestampedObjectSet<SerialisableTestObject>();
    private List<SerialisableTestObject> list = new ArrayList<SerialisableTestObject>();
    private Set<SerialisableTestObject> unique = new HashSet<SerialisableTestObject>();
    private SerialisableTestObject obj;
    private SerialisableTestObject obj2;
    private SerialisableTestObject obj3;
    private SerialisableTestObject obj4;
    private SerialisableTestObject obj5;
    private Date separatorDate;
    private Date afterSeparatorDate;

    @Before
    public void init() {
        obj = null;
        obj2 = null;
        obj3 = null;
        obj4 = null;
        obj5 = null;
        separatorDate = null;
        afterSeparatorDate = null;
        set.clear();
        list.clear();
    }

    /**
     * Test method for
     * {@link org.gw.objectlogger.TimestampedObjectSet#add(org.gw.objectlogger.TimestampedObject)}.
     */
    @Test
    public void testAdd() {
        obj = new SerialisableTestObject();
        obj.one = 11;
        obj.two = 21f;
        obj.three = "31";
        list.add(obj);
        Assert.assertTrue("SerialisableSet not empty!", set.isEmpty());
        set.add(new SerialisableTest(obj));
        Assert.assertEquals("SerialisableSet should have size 1", 1, set.size());
        obj2 = new SerialisableTestObject();
        obj2.one = 12;
        obj2.two = 22f;
        obj2.three = "32";
        list.add(obj2);
        
        // Add to unique set for uniqueness test later
        unique.add(obj2);
        // Add to set
        set.add(new SerialisableTest(obj2));
        
        Assert.assertEquals("SerialisableSet should have size 2", 2, set.size());
        
        obj3 = new SerialisableTestObject();
        obj3.one = 13;
        obj3.two = 23f;
        obj3.three = "33";
        list.add(obj3);

        // Add to unique set for uniqueness test later
        unique.add(obj3);
        set.add(new SerialisableTest(obj3));
        Assert.assertEquals("SerialisableSet should have size 3", 3, set.size());

        separatorDate = new Date();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        obj4 = new SerialisableTestObject();
        obj4.one = 11;
        obj4.two = 24f;
        obj4.three = "34";
        list.add(obj4);

        // Add to unique set for uniqueness test later
        unique.add(obj4);
        set.add(new SerialisableTest(obj4));
        Assert.assertEquals("SerialisableSet should have size 4", 4, set.size());

        obj5 = new SerialisableTestObject();
        obj5.one = 15;
        obj5.two = 25f;
        obj5.three = "35";
        list.add(obj5);

        // Add to unique set for uniqueness test later
        unique.add(obj5);
        set.add(new SerialisableTest(obj5));
        Assert.assertEquals("SerialisableSet should have size 5", 5, set.size());
    }

    /**
     * Test method for {@link org.gw.objectlogger.TimestampedObjectSet#asList()}.
     */
    @Test
    public void testAsList() {
        // Depends on testAdd();
        testAdd();

        List<SerialisableTestObject> result = set.asList();
        Assert.assertEquals(list, result);
    }

    @Test
    public void testGetForDate() {

        // Depends on testAdd();
        testAdd();

        SerialisableTestObject result = set.getForDate(new Date(0));

        Assert.assertNull("Expected null.", result);

        result = set.getForDate(new Date());

        Assert.assertEquals(obj5, result);

    }

    @Test
    public void testGetForDateComparator() {

        // Depends on testAdd();
        testAdd();

        Comparable<SerialisableTestObject> comparable = new Comparable<SerialisableTestObject>() {

            @Override
            public int compareTo(SerialisableTestObject o) {
                if ("32".equals(o.three)) {
                    return 0;
                } else {
                    return 1;
                }
            }
        };
        SerialisableTestObject result = set.getForDate(new Date(), comparable);

        Assert.assertEquals(obj2, result);

        result = set.getForDate(new Date(0), comparable);

        Assert.assertNull("Expected null.", result);

        result = set.getForDate(new Date(), new Comparable<SerialisableTestObject>() {

            @Override
            public int compareTo(SerialisableTestObject o) {
                if ("34".equals(o.three)) {
                    return 0;
                } else {
                    return 1;
                }
            }
        });

        Assert.assertEquals(obj4, result);

    }

    /**
     * Test method for {@link org.gw.objectlogger.TimestampedObjectSet#getUniqueForDate(java.util.Date)}.
     */
    @Test
    public void testGetUniqueForDate() {
        // Depends on testAdd();
        testAdd();
        
        //Add 100 more
        for (int i = 0; i < 100; i++) {

            obj = new SerialisableTestObject();
            obj.one = 100+i;
            obj.two = 200+i;
            obj.three = String.valueOf(300+i);
            set.add(new SerialisableTest(obj));
			
		}

        // Add another 100 with the same one parameter which will become the unique ones.
        for (int i = 0; i < 100; i++) {

            obj = new SerialisableTestObject();
            obj.one = 100+i;
            obj.two = 300+i;
            obj.three = String.valueOf(400+i);
            // Add to unique set
            unique.add(obj);
            set.add(new SerialisableTest(obj));
			
		}

        Set<TimestampedObject<SerialisableTestObject>> result = set.getUniqueForDate(new Date(), oneParamComparator);
        Assert.assertEquals(unique.size(), result.size());
        
        Iterator<TimestampedObject<SerialisableTestObject>> it = result.iterator();
        while (it.hasNext()) {
        	TimestampedObject<SerialisableTestObject> r = it.next();
            for (SerialisableTestObject u : unique) {
                if (r.getObj().one == u.one && r.getObj().two == u.two && r.getObj().three.equals(u.three)) {
                    it.remove();
                    break;
                }
            }
        }
        Assert.assertFalse("Iterator should be empty: " + it, it.hasNext());
    }

    private class SerialisableTestObject {
        private int one;
        private float two;
        private String three;
        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("SerialisableTestObject [one=");
            builder.append(one);
            builder.append(", two=");
            builder.append(two);
            builder.append(", three=");
            builder.append(three);
            builder.append("]");
            return builder.toString();
        }

    }

    private class SerialisableTest extends TimestampedObject<SerialisableTestObject> {

        public SerialisableTest(SerialisableTestObject obj) {
            super(obj);
        }

    }
}
