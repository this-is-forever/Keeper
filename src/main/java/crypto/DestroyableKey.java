package crypto;

import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;
import java.util.Arrays;

/**
 *  A destroyable implementation of {@link SecretKey}, with keys based on byte array data. Underlying byte array data
 *  can be overwritten using the {@link DestroyableKey#destroy()} method, unlike the
 *  {@link javax.crypto.spec.SecretKeySpec} class.
 */
public class DestroyableKey implements SecretKey {

    // Constants for the algorithm and format of DestroyableKeys
    private static final String ALGORITHM = "AES";
    private static final String FORMAT = "RAW";

    // Flag set when destroy() is called
    private boolean destroyed;
    // The underlying key data. Call destroy() to overwrite the data
    private byte[] keyData;

    /**
     * Creates a new {@link DestroyableKey} with AES algorithm and RAW format.
     * @param keyData An array of bytes to derive the key from. A copy of the array is created for the object's
     *                underlying byte array. Use {@link DestroyableKey#destroy()} to erase the key from memory for
     *                security purposes.
     */
    public DestroyableKey(byte[] keyData) {
        this.keyData = Arrays.copyOf(keyData, keyData.length);
        destroyed = false;
    }

    /**
     * Creates a new {@link DestroyableKey} with AES algorithm and RAW format, based on data in a specific
     * spot in an array
     * @param keyData An array of bytes to derive the key from. A copy is created for the object's underlying byte
     *                array. Use {@link DestroyableKey#destroy()} to erase the key from memory for security purposes.
     * @param start The starting index to read key data from
     * @param length The number of bytes to read from the source array
     */
    public DestroyableKey(byte[] keyData, int start, int length) {
        this.keyData = Arrays.copyOfRange(keyData, start, start + length);
        destroyed = false;
    }

    /**
     * Gets the key's algorithm
     * @return a reference to {@link DestroyableKey#ALGORITHM}
     */
    @Override
    public String getAlgorithm() {
        return ALGORITHM;
    }

    /**
     * Gets the key's format
     * @return a reference to {@link DestroyableKey#FORMAT}
     */
    @Override
    public String getFormat() {
        return FORMAT;
    }

    /**
     * Creates a copy of this key's underlying array and returns a reference to the copy
     * @return a references to the key's underlying byte data
     * @throws IllegalStateException if the key has been destroyed
     */
    @Override
    public byte[] getEncoded() {
        if(destroyed)
            throw new IllegalStateException("Key was previously destroyed!");
        return Arrays.copyOf(keyData, keyData.length);
    }

    /**
     * Erases the key's underlying byte array
     * @throws IllegalStateException in the event {@link DestroyableKey#destroy} was already called upon this key
     */
    @Override
    public void destroy() {
        if(destroyed)
            throw new IllegalStateException("Key has already been destroyed");
        destroyed = true;
        Crypto.erase(keyData);
    }

    /**
     * Determines whether this key was previously destroyed
     * @return true if the key has been destroyed, otherwise false
     */
    @Override
    public boolean isDestroyed() {
        return destroyed;
    }
}
