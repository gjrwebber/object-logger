/**
 * ByteLoggerTest.java (c) Copyright 2013 Graham Webber
 */
package org.gw.objectlogger;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author gman
 * @since 1.0
 * @version 1.0
 * 
 */
public class ByteLoggerTest {
	private ObjectLogger<byte[]> logger;
	private FileSystemDataSource source;
	private File file;
	private File objectLoggerPath = new File(FileUtils.getTempDirectoryPath()
			+ File.separatorChar + getClass().getSimpleName());

	private byte[] bytes1 = new byte[]{1, 2, 3};
	private byte[] bytes2 = new byte[]{4, 5, 6};
	private byte[] bytes3 = new byte[]{7, 8, 9};
	private byte[] bytes4 = new byte[]{10, 11, 12};

	@Before
	public void init() throws IOException {
		MinuteRollingStrategy strategy = new MinuteRollingStrategy(10);
		source = new FileSystemDataSource("ByteLogger", strategy,
				new TimestampedByteArraySerialiser()) {
		};
		source.setFileSystemLoggerPath(objectLoggerPath.getAbsolutePath());
		logger = new ByteArrayLogger(source);
		logger.setSynchronous(true);
		if (objectLoggerPath.exists()) {
			FileUtils.forceDelete(objectLoggerPath);
		}

		file = source.getFile();
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
	public void testLogSerialisable() throws IOException, InterruptedException {

		Assert.assertTrue(
				"logged file is already there: " + file.getAbsolutePath(),
				!file.exists());
		logger.log(new TimestampedObject<byte[]>(bytes1));
		logger.log(new TimestampedObject<byte[]>(bytes2));
		Thread.sleep(10);
		logger.log(new TimestampedObject<byte[]>(bytes3));
		logger.log(new TimestampedObject<byte[]>(bytes4));
		Assert.assertTrue(
				"logged file was not created: " + file.getAbsolutePath(),
				file.exists());

		System.out.println("Reading from " + source.getFile());
		Assert.assertTrue(
				"Logged file is not available: " + file.getAbsolutePath(),
				file.exists());
		List<byte[]> objs = source.getAll(byte[].class).asList();
		Assert.assertNotNull("Json file was not deserialised. Null.", objs);
		Assert.assertFalse("Logged file was empty. Null.", objs.isEmpty());
		Assert.assertEquals(
				"Logged file has wrong number of entries. Expected 4, but was "
						+ objs.size(), 4, objs.size());

		byte[] b1 = (byte[]) objs.get(0);
		byte[] b2 = (byte[]) objs.get(1);
		byte[] b3 = (byte[]) objs.get(2);
		byte[] b4 = (byte[]) objs.get(3);
		Assert.assertTrue(Arrays.equals(bytes1, b1));
		Assert.assertTrue(Arrays.equals(bytes2, b2));
		Assert.assertTrue(Arrays.equals(bytes3, b3));
		Assert.assertTrue(Arrays.equals(bytes4, b4));
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
	public void testLog() throws IOException, InterruptedException {

		Assert.assertTrue(
				"logged file is already there: " + file.getAbsolutePath(),
				!file.exists());
		logger.log(bytes1);
		logger.log(bytes2);
		Thread.sleep(10);
		logger.log(bytes3);
		logger.log(bytes4);
		Assert.assertTrue(
				"logged file was not created: " + file.getAbsolutePath(),
				file.exists());

		System.out.println("Reading from " + source.getFile());
		Assert.assertTrue(
				"Logged file is not available: " + file.getAbsolutePath(),
				file.exists());
		List<byte[]> objs = source.getAll(byte[].class).asList();
		Assert.assertNotNull("Json file was not deserialised. Null.", objs);
		Assert.assertFalse("Logged file was empty. Null.", objs.isEmpty());
		Assert.assertEquals(
				"Logged file has wrong number of entries. Expected 4, but was "
						+ objs.size(), 4, objs.size());

		byte[] b1 = (byte[]) objs.get(0);
		byte[] b2 = (byte[]) objs.get(1);
		byte[] b3 = (byte[]) objs.get(2);
		byte[] b4 = (byte[]) objs.get(3);
		Assert.assertTrue(Arrays.equals(bytes1, b1));
		Assert.assertTrue(Arrays.equals(bytes2, b2));
		Assert.assertTrue(Arrays.equals(bytes3, b3));
		Assert.assertTrue(Arrays.equals(bytes4, b4));
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
	public void testLogAsync() throws IOException, InterruptedException {

		int numByteArrays = 100;
		int logArrayLength = 200;
		int numThreads = 100;
		
		// Set to asynchronous
		logger.setSynchronous(false);
		final byte[][] bytes = new byte[numByteArrays][logArrayLength];
		for (int i = 0; i < bytes.length; i++) {
			byte[] bs = bytes[i];
			Arrays.fill(bs, (byte) i);
		}
		
		final Set<byte[]> expected = new HashSet<byte[]>();

		Assert.assertTrue(
				"logged file is already there: " + file.getAbsolutePath(),
				!file.exists());
		final CountDownLatch latch = new CountDownLatch(numThreads);
		final Thread[] threads = new Thread[numThreads];
		for (int i = 0; i < numThreads; i++) {
			Thread t = new Thread(new Runnable() {

				@Override
				public void run() {
					for (int i = 0; i < bytes.length; i++) {
						byte[] bs = bytes[i];
						logger.log(bs);
						expected.add(bs);
					}
					latch.countDown();
				}
			});
			threads[i] = t;

		}

		for (int i = 0; i < threads.length; i++) {
			Thread thread = threads[i];
			thread.start();
		}

		if (!latch.await(10, TimeUnit.SECONDS)) {
			Assert.fail("Threads took too long to log.");
		}

		while (logger.getQueueSize() > 0) {
			Thread.sleep(500);
		}

        Thread.sleep(500);

        Assert.assertEquals("Queue is not empty", 0, logger.getQueueSize());
		
		Assert.assertTrue(
				"logged file was not created: " + file.getAbsolutePath(),
				file.exists());
		Assert.assertEquals("Wrong file size: ", numByteArrays *numThreads * (logArrayLength + 8 + 4), file.length());
		System.out.println("Reading from " + source.getFile());
		Assert.assertTrue(
				"Logged file is not available: " + file.getAbsolutePath(),
				file.exists());
		List<byte[]> objs = source.getAll(byte[].class).asList();
		Assert.assertNotNull("Json file was not deserialised. Null.", objs);
		Assert.assertFalse("Logged file was empty.", objs.isEmpty());

		Assert.assertEquals(
				"Logged file has wrong number of entries. Expected "
						+ bytes.length + ", but was " + objs.size(),
				bytes.length * numThreads, objs.size());

		for (byte[] b : expected) {
			boolean ok = false;
			for (byte[] logged : objs) {
				Assert.assertEquals("Wrong length of logged array ",
						logArrayLength, logged.length);
				if (Arrays.equals(b, logged)) {
					ok = true;
				}
			}
			if (!ok) {
				Assert.fail("Could not find byte array: " + Arrays.toString(b));
			}
		}

	}

}
