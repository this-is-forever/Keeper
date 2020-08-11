package com.github.thisisforever.crypto;

/**
 * The {@link Cryptographer} interface defines an object that can encrypt and decrypt data, along with a method
 * to be called upon to delete sensitive data when the object is no longer needed.
 */
public interface Cryptographer {

    /**
     * Encrypts plaintext data, returning all information needed to decrypt the ciphertext data aside from they key
     * itself.
     * @param plaintext The data to encrypt
     * @return The resulting data as a byte array
     */
    byte[] encrypt(byte[] plaintext);

    /**
     * Decrypts ciphertext data and returns the result
     * @param ciphertext The data to decrypt; data should be encrypted using the encrypt method of a similar
     *                   {@link Cryptographer}.
     * @return The resulting plaintext data
     */
    byte[] decrypt(byte[] ciphertext) throws CryptographicFailureException;

    /**
     * Safely deletes all sensitive information associated with this {@link Cryptographer} object.
     */
    void destroy();

}
