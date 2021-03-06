package com.github.thisisforever.crypto;

import com.lambdaworks.crypto.SCrypt;

import java.security.GeneralSecurityException;

public class SCryptKeyFactory {

    // The number of passes for SCrypt to make over the key; bigger = more time required
    private static final int SCRYPT_ITERATIONS_COUNT = 524288;
    // The block size used by SCrypt during key generation. Bigger = more memory required
    private static final int SCRYPT_BLOCK_SIZE = 8;
    // The number of threads used by SCrypt; bigger = more threads, = more memory
    private static final int SCRYPT_PARALLELISM_FACTOR = 1;
    // The length of the key generated by SCrypt and used by AES; must be 128, 192 or 256 bits
    private static final int SCRYPT_KEY_LENGTH_BITS = 256;
    private static final int SCRYPT_KEY_LENGTH = SCRYPT_KEY_LENGTH_BITS / 8;

    private static final int SCRYPT_SALT_LENGTH = 32;

    public static SCryptKey deriveKey(byte[] password) {
        byte[] salt = Utility.generateRandomBytes(SCRYPT_SALT_LENGTH);
        return deriveKey(password, salt);
    }

    public static SCryptKey deriveKey(byte[] password, byte[] salt) {
        try {
            byte[] keyData = SCrypt.scrypt(password, salt, SCRYPT_ITERATIONS_COUNT,
                    SCRYPT_BLOCK_SIZE, SCRYPT_PARALLELISM_FACTOR, SCRYPT_KEY_LENGTH);
            return new SCryptKey(keyData, salt);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to derive key");
        }
    }

    public static class SCryptKey extends DestroyableKey {
        private final byte[] salt;

        public SCryptKey(byte[] keyData, byte[] saltData) {
            super(keyData);
            salt = saltData;
        }

        public byte[] getSalt() {
            return salt;
        }

        @Override
        public void destroy() {
            super.destroy();
            Utility.erase(salt);
        }
    }

}
