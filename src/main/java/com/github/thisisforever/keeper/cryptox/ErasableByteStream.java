package com.github.thisisforever.keeper.cryptox;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * An implementation of {@link ByteArrayOutputStream} whose underlying byte array can be overwritten for security
 * purposes.
 */
public class ErasableByteStream extends ByteArrayOutputStream {

    /**
     * Instantiates a new object with default initial capacity
     */
    public ErasableByteStream() {
        super();
    }

    /**
     * Instantiates a new object with a defined initial capacity
     * @param initialCapacity The number of bytes the stream should be initialized with
     */
    public ErasableByteStream(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Overwrites all data in the buffer with zeroes.
     */
    public void erase() {
        Arrays.fill(buf, (byte)0);
    }

}
