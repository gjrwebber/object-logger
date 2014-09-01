package org.gw.objectlogger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

/**
 * An {@link ITimestampedObjectSerialiser} which uses
 * <code>com.fasterxml.jackson.databind.ObjectMapper</code> to serialis the
 * {@link TimestampedObject} to a json string.
 *
 * @param <T> The underlying {@link TimestampedObject} type.
 * @author Gman
 */
public class TimestampedObjectJsonSerialiser
        implements
        ITimestampedObjectSerialiser {

    private static Logger logger = LoggerFactory
            .getLogger(TimestampedObjectJsonSerialiser.class);

    /**
     * The {@link TypeFactory} to use when reading the Json objects.
     */
    private TypeFactory typeFactory = TypeFactory.defaultInstance();

    /**
     * The {@link ObjectMapper} to use when reading/writing Json objects.
     */
    private ObjectMapper mapper = new ObjectMapper();

    private RandomAccessFile raf;

    private static final byte openBracket = '[';
    private static final byte closeBracket = ']';
    private static final byte separator = ',';
    private static final byte newLine = '\n';
    private static final byte[] separatorData = new byte[]{separator, newLine};
    private static final byte[] openData = new byte[]{openBracket, newLine};
    private static final byte[] closeData = new byte[]{newLine, closeBracket, newLine};

    private static final String extension = "json";

    /**
     * The offsetAtStart is where we write new Objects. 2 = closing bracket + newLine
     * offset = raf.length() - offsetAtStart
     */
//    private static final int offsetAtStart = 2;
//    private static final int lengthAtStart = 4;

    /**
     * Serialises the given Object to an array of bytes.
     */
    private byte[] serialise(Object object) throws SerialisationException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            mapper.writeValue(out, object);
        } catch (Exception e) {
            throw new SerialisationException("Could not serialise to json.", e);
        }
        byte[] bytes = out.toByteArray();
        try {
            out.close();
        } catch (IOException e) {
            // Dont care.
        }
        return bytes;
    }

    /**
     * Deserialises the contents of the file to an array of
     * {@link TimestampedObject}s
     */
    @SuppressWarnings("unchecked")
    private <T> List<TimestampedObject<T>> deserialise(Class<T> type, File file)
            throws DeserialisationException {

        try {

            List<TimestampedObject<T>> result = (List<TimestampedObject<T>>) mapper
                    .readValue(file, typeFactory.constructCollectionType(
                            List.class, typeFactory.constructParametricType(
                                    TimestampedObject.class, type)));
            return result;
        } catch (Exception e) {
            throw new DeserialisationException(
                    "Could not deserialise to json.", e);
        }

    }

    /**
     * Writes the given {@link TimestampedObject} to the
     * {@link java.io.RandomAccessFile} at 2 bytes from the end since the end of the file
     * is ']\n'
     */
    @Override
    public void write(TimestampedObject<?> object)
            throws SerialisationException, IOException {

        // Set the offset at which to write the data
        setOffset();

        // Write the object
        doWrite(object);

        // Write the closing bracket
        writeClosing();
    }

    private void doWrite(TimestampedObject<?> object)
            throws SerialisationException, IOException {
        if (raf == null) {
            throw new IllegalStateException("Please call open(File) first");
        }
        if (object == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Object is null. Nothing log.");
            }
            return;
        }

        byte[] data = serialise(object);
        if (data != null && data.length > 0) {
            // If the size is greater than the initial file size
            if (raf.length() > openData.length + closeData.length) {
                // Next write the separator data (",\n")
                raf.write(separatorData);
            }
            // First write the json object
            raf.write(data);
        }
    }

    private void writeClosing() throws IOException {
//        raf.seek(raf.length() - separatorData.length);
        raf.write(closeData);
    }

    /**
     * @return the offset at which to write the next object.
     * @throws java.io.IOException
     */
    private void setOffset() throws IOException {
        long offset = raf.length() - closeData.length;
        raf.seek(offset);
    }

    /**
     * Writes the given {@link java.util.List} of {@link TimestampedObject}s to the
     * {@link java.io.RandomAccessFile}.
     */
    @Override
    public void write(List<TimestampedObject<?>> batch)
            throws SerialisationException, IOException {
        if (raf == null) {
            throw new IllegalStateException("Please call open(File) first");
        }
        if (batch == null || batch.size() == 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("Nothing in batch to log.");
            }
            return;
        }

        setOffset();

        for (TimestampedObject<?> object : batch) {
            doWrite(object);
        }

        writeClosing();
    }

    /**
     * Reads the contents of the given {@link java.io.File} as a
     * {@link TimestampedObjectSet}.
     */
    @Override
    public <T> TimestampedObjectSet<T> readAll(File file, Class<T> type)
            throws DeserialisationException {
        List<TimestampedObject<T>> result = deserialise(type, file);
        TimestampedObjectSet<T> set = new TimestampedObjectSet<T>();
        if (result != null) {
            for (TimestampedObject<T> obj : result) {
                set.add(obj);
            }
        }
        return set;
    }

    /**
     * Assumes the file is empty, and writes '[\n\n]' char to the file. This is the
     * opening bracket of a JSON array.
     */
    @Override
    public void open(File file) throws IOException {
        FileUtils.touch(file);
        raf = new RandomAccessFile(file, "rwd");
        raf.write(openData);
        raf.write(closeData);
    }

    /**
     * Closes the file by replacing the last ',' with a ']', therefore closing
     * the JSON array of objects.
     */
    @Override
    public void close() throws IOException {
        raf.close();
    }

    /**
     * Returns the "json" extension.
     */
    @Override
    public String getExtension() {
        return extension;
    }

    @Override
    public boolean isOpen() {
        return raf != null && raf.getChannel().isOpen();
    }

}
