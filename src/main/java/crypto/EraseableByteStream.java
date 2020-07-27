package crypto;

import java.io.ByteArrayOutputStream;

public class EraseableByteStream extends ByteArrayOutputStream {

    public EraseableByteStream() {
        super();
    }

    public EraseableByteStream(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Overwrites all data in the buffer with zeroes.
     */
    public void erase() {
        Crypto.erase(buf);
    }

}
