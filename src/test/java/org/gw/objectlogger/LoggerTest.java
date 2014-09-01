/**
 * LoggerTest.java (c) Copyright 2013 Graham Webber
 */
package org.gw.objectlogger;

import org.apache.commons.io.FileUtils;
import org.aspectj.lang.Aspects;
import org.gw.commons.aspects.TimeShiftAspect;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author gman
 * @since 1.0
 * @version 1.0
 * 
 */
public class LoggerTest {
	private ObjectLogger<TestObject> logger;
	private FileSystemDataSource source;

	private TimeShiftAspect timeAspect = Aspects
			.aspectOf(TimeShiftAspect.class);

	private File objectLoggerPath = new File(FileUtils.getUserDirectory()
			, getClass().getSimpleName());

	@Before
	public void init() throws IOException {
		MinuteRollingStrategy strategy = new MinuteRollingStrategy(1);
		source = new FileSystemDataSource("test", strategy, new TestObject());
		logger = new ObjectLogger<TestObject>(source) {
		};

		source.setFileSystemLoggerPath(objectLoggerPath.getAbsolutePath());
		logger.setSynchronous(true);
		timeAspect.resetTime();

		if (objectLoggerPath.exists()) {
			FileUtils.forceDelete(objectLoggerPath);
		}

	}

	/**
	 * Test method for
	 * {@link org.gw.objectlogger.ObjectLogger#log(Object)}
	 * .
	 * 
	 * @throws java.io.IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testAppend() throws IOException, InterruptedException {
		TestObject obj1 = new TestObject("bob", "jill");
		TestObject obj2 = new TestObject("jane", "lara");

		File file = source.getFile();

		Assert.assertTrue(
				"Json file is already there: " + file.getAbsolutePath(),
				!file.exists());
		logger.log(obj1);
		logger.log(obj2);
		// Thread.sleep(500);
		Assert.assertTrue(
				"Json file was not created: " + file.getAbsolutePath(),
				file.exists());
	}

	/**
	 * Test method for
	 * {@link org.gw.objectlogger.IDataSource#getAll(Class)}
	 * .
	 * 
	 * @throws java.io.IOException
	 * @throws InterruptedException
	 */
	@Test
	public void testAsList() throws IOException, InterruptedException {

		File file = source.getFile();

		if (!file.exists()) {
			testAppend();
		}

		System.out.println("Reading from " + source.getFile());
		Assert.assertTrue(
				"Json file is not available: " + file.getAbsolutePath(),
				file.exists());
		List<TestObject> objs = source.getAll(TestObject.class).asList();
		Assert.assertNotNull("Json file was not deserialised. Null.", objs);
		Assert.assertFalse("Json file was empty. Null.", objs.isEmpty());
		Assert.assertEquals("bob", objs.get(0).name);
		Assert.assertEquals("jill", objs.get(0).getOther());
		Assert.assertEquals("jane", objs.get(1).name);
		Assert.assertEquals("lara", objs.get(1).getOther());
	}

	@Test
	public void testCrossingMinute() throws IOException, InterruptedException {

		List<TestObject> objs = new ArrayList<TestObject>();

		List<TestObject> beforeSwitch = new ArrayList<TestObject>();
		List<TestObject> afterSwitch = new ArrayList<TestObject>();

		Calendar prev = Calendar.getInstance();
        prev.set(Calendar.HOUR_OF_DAY, 0);
        prev.add(Calendar.MINUTE, -5);
		prev.set(Calendar.SECOND, 55);
		prev.set(Calendar.MILLISECOND, 0);
        System.out.println("prev: " + prev.getTime());

		Calendar next = Calendar.getInstance();
		next.setTime(prev.getTime());
		next.add(Calendar.MINUTE, 3);
		next.set(Calendar.SECOND, 10);
		System.out.println("next: " + next.getTime());

		// Set the system time the current minute:59.500
		// This should produce 2 files - minute.59..json and (minute+1).00..json
		// because we sleep for 600ms in between loggin
		// objects.
		timeAspect.setSystemTime(prev);

		// Add some objects (should go to first file minute.59..json)
		for (int i = 0; i < 3; i++) {
			TestObject obj = new TestObject("" + i, "" + i);
			logger.log(new SerialisableTestObject(obj));
			beforeSwitch.add(obj);
			objs.add(obj);
		}

        Date firstLotFinished = new Date(System.currentTimeMillis()+1000);
        System.out.println("firstLotFinish: " + firstLotFinished);
		timeAspect.setSystemTime(next);

		// Log some more objects (should go to second file (minute+1).00..json)
		for (int i = 3; i < 6; i++) {
			TestObject obj = new TestObject("" + i, "" + i);
			logger.log(obj);
			afterSwitch.add(obj);
			objs.add(obj);
		}
		Date secondLotFinish = new Date();
        System.out.println("secondLotFinish: " + secondLotFinish);

		// Set the system time to 00:01:10
		timeAspect.setSystemTime(new Date(next.getTimeInMillis() + 60000));

		// Test second file
		List<TestObject> afterObjs = source.getAll(TestObject.class,
				next.getTime(), new Date()).asList();
		Assert.assertEquals("Data after time switch is incorrect.", afterSwitch, afterObjs);

		// test first file
		List<TestObject> beforeObjs = source.getAll(TestObject.class,
				prev.getTime(), firstLotFinished).asList();
		Assert.assertEquals("Data before time switch is incorrect.", beforeSwitch, beforeObjs);

		List<TestObject> allInRange = source.getAll(TestObject.class,
				prev.getTime(), new Date()).asList();
		Assert.assertEquals("All data is incorrect.", objs, allInRange);
	}

	@Test
	public void testCrossingDate() throws IOException, InterruptedException {

		List<TestObject> objs = new ArrayList<TestObject>();

		List<TestObject> beforeSwitch = new ArrayList<TestObject>();
		List<TestObject> afterSwitch = new ArrayList<TestObject>();

		Calendar prev = Calendar.getInstance();
		prev.add(Calendar.DATE, -2);
		prev.set(Calendar.HOUR_OF_DAY, 23);
		prev.set(Calendar.MINUTE, 59);
		prev.set(Calendar.SECOND, 50);
		prev.set(Calendar.MILLISECOND, 0);

		Calendar next = Calendar.getInstance();
		next.set(Calendar.HOUR_OF_DAY, 0);
		next.set(Calendar.MINUTE, 0);
		next.set(Calendar.SECOND, 10);
		next.set(Calendar.MILLISECOND, 0);

		// Set the system time to 1 day ago at 23:59:50
		timeAspect.setSystemTime(prev);

		// log some objects at that time
		for (int i = 0; i < 3; i++) {
			TestObject obj = new TestObject("" + i, "" + i);
			logger.log(obj);
			beforeSwitch.add(obj);
			objs.add(obj);
		}

		Date firstLotFinished = new Date(System.currentTimeMillis()+1000);

		// Set the system time to 00:00:10
		timeAspect.setSystemTime(next);

		// log some more objects on this new day
		for (int i = 3; i < 6; i++) {
			TestObject obj = new TestObject("" + i, "" + i);
			logger.log(obj);
			afterSwitch.add(obj);
			objs.add(obj);
		}

		Date secondLotFinish = new Date();

		// Set the system time to 00:01:10
		timeAspect.setSystemTime(new Date(next.getTimeInMillis() + 60000));

		// test the new objects
		List<TestObject> afterObjs = source.getAll(TestObject.class,
				next.getTime(), new Date()).asList();
		Assert.assertEquals("Data after time switch is incorrect.", afterSwitch, afterObjs);

		// test the previous days objects
		List<TestObject> beforeObjs = source.getAll(TestObject.class,
				prev.getTime(), firstLotFinished).asList();
		Assert.assertEquals("Data before time switch is incorrect.", beforeSwitch, beforeObjs);

		// Test get objects in range
		List<TestObject> allInRange = source.getAll(TestObject.class,
				new Date(System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000))
				.asList();
		Assert.assertEquals("All data is incorrect.", objs, allInRange);
	}
}
