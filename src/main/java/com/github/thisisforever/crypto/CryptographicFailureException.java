package com.github.thisisforever.crypto;

/**
 * An exception thrown when a cryptographic process (encryption, decryption, authentication) fails.
 */
public class CryptographicFailureException extends Exception {
    public CryptographicFailureException(String details) {
        super(details);
    }
}
