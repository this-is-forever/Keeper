package crypto;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * An implementation of {@link ByteArrayOutputStream} whose underlying byte array can be overwritten for security
 * purposes.
 */
public class EraseableByteStream extends ByteArrayOutputStream {

    /**
     * Instantiates a new object with default initial capacity
     */
    public EraseableByteStream() {
        super();
    }

    /**
     * Instantiates a new object with a defined initial capacity
     * @param initialCapacity The number of bytes the stream should be initialized with
     */
    public EraseableByteStream(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Overwrites all data in the buffer with zeroes.
     */
    public void erase() {
        Arrays.fill(buf, (byte)0);
    }

}
