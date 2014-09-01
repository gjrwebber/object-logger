/**
 * AsyncLoggerTest.java (c) Copyright 2013 Graham Webber
 */
package org.gw.objectlogger;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.StopWatch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author gman
 * @since 1.0
 * @version 1.0
 * 
 */
public class AsyncLoggerTest {
	private File objectLoggerPath = new File(FileUtils.getTempDirectoryPath()
			+ File.separatorChar + getClass().getSimpleName());

	@Test
	public void performanceTest() throws IOException, InterruptedException {

		MinuteRollingStrategy strategy = new MinuteRollingStrategy(1);
		FileSystemDataSource source = new FileSystemDataSource(
				"testAsyncPerformance", strategy,
				new TimestampedObjectJsonSerialiser());
		FileSystemDataSource source2 = new FileSystemDataSource(
				"testAsyncPerformance2", strategy,
				new TimestampedObjectJsonSerialiser());
		source.setFileSystemLoggerPath(objectLoggerPath.getAbsolutePath());
		source2.setFileSystemLoggerPath(objectLoggerPath.getAbsolutePath());
		ObjectLogger<TestObject> logger = new ObjectLogger<TestObject>(source) {
		};
		ObjectLogger<TestObject> logger2 = new ObjectLogger<TestObject>(source2) {
		};

		logger.setClean(true);
		logger.init();

		logger2.setClean(true);
		logger2.init();

		File perfFile = source.getFile();
		File perfFile2 = source2.getFile();

		List<TestObject> objs = new ArrayList<TestObject>();
		for (int i = 0; i < 1000; i++) {
			objs.add(new TestObject("" + i, "" + i));
		}

		StopWatch watch = new StopWatch();
		watch.start();
		for (TestObject obj : objs) {
			logger.log(obj);
			logger2.log(obj);
		}
		watch.stop();

		Thread.sleep(2000);

		Assert.assertTrue(
				"Performance log file does not exist: "
						+ perfFile.getAbsolutePath(), perfFile.exists());
		Assert.assertTrue(
				"Performance log file 2 does not exist: "
						+ perfFile2.getAbsolutePath(), perfFile2.exists());

		List<TestObject> readObjs = new ArrayList<TestObject>(source.getAll(
				TestObject.class).asList());
		List<TestObject> readObjs2 = new ArrayList<TestObject>(source2.getAll(
				TestObject.class).asList());
		Assert.assertEquals(objs, readObjs);
		Assert.assertEquals(objs, readObjs2);

		Assert.assertTrue(
				"Too slow. Expected max 20ms, but took "
						+ watch.getTotalTimeMillis() + "ms",
				watch.getTotalTimeMillis() <= 20);

	}

}
