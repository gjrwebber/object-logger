/**
 * JsonSerialiserTest.java (c) Copyright 2013 Graham Webber
 */
package org.gw.objectlogger;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author gman
 * @since 1.0
 * @version 1.0
 * 
 */
public class JsonSerialiserTest {
    private ObjectLogger<TestObject> logger;
    private FileSystemDataSource source;

    private File objectLoggerPath = new File(FileUtils.getTempDirectoryPath() + File.separatorChar + getClass().getSimpleName());
    private File file;

    @Before
    public void init() throws IOException {

        MinuteRollingStrategy strategy = new MinuteRollingStrategy(1);
        source = new FileSystemDataSource("test-json", strategy, new TimestampedObjectJsonSerialiser());
        logger = new ObjectLogger<TestObject>(source) {};
        logger.setSynchronous(true);
        source.setFileSystemLoggerPath(objectLoggerPath.getAbsolutePath());

        if (objectLoggerPath.exists()) {
            FileUtils.forceDelete(objectLoggerPath);
        }
        file = source.getFile();
    }

    /**
     * Test method for {@link org.gw.objectlogger.TimestampedObjectJsonSerialiser#serialise(Object)}.
     * 
     * @throws java.io.IOException
     * @throws InterruptedException
     */
    @Test
    public void testSerialise() throws IOException, InterruptedException {
        TestObject obj = new TestObject("bob", "bill");
        TestObject obj2 = new TestObject("lara", "jake");

        Assert.assertFalse("Json file is already there: " + file.getAbsolutePath(), file.exists());
        logger.log(obj);
        logger.log(obj2);
        Assert.assertTrue("Json file was not created: " + file.getAbsolutePath(), file.exists());

        System.out.println("Reading from " + source.getFile());
        Assert.assertTrue("Json file is not available: " + file.getAbsolutePath(), file.exists());

        List<TestObject> objs = source.getAll(TestObject.class).asList();
        Assert.assertEquals(2, objs.size());
        TestObject readObj = objs.get(0);
        Assert.assertNotNull("Json file was not deserialised. Null.", readObj);
        Assert.assertEquals("bob", readObj.name);
        Assert.assertEquals("bill", readObj.getOther());
        TestObject readObj2 = objs.get(1);
        Assert.assertNotNull("Json file was not deserialised. Null.", readObj2);
        Assert.assertEquals("lara", readObj2.name);
        Assert.assertEquals("jake", readObj2.getOther());
    }

}
