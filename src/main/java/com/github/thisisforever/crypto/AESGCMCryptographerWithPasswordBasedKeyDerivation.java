package com.github.thisisforever.crypto;

import java.nio.ByteBuffer;

public class AESGCMCryptographerWithPasswordBasedKeyDerivation extends AESGCMCryptographerWithKey {

    private byte[] password;

    public AESGCMCryptographerWithPasswordBasedKeyDerivation(char[] password) {
        this.password = Utility.encode(password);
    }

    @Override
    public byte[] encrypt(byte[] data) {
        SCryptKeyFactory.SCryptKey key = SCryptKeyFactory.deriveKey(password);
        byte[] ciphertextAndIV = null;
        try {
            byte[] salt = key.getSalt();
            this.key = key;
            ciphertextAndIV = super.encrypt(data);
            ByteBuffer buffer = ByteBuffer.allocate(Byte.BYTES + salt.length
                    + Integer.BYTES + ciphertextAndIV.length);
            buffer.put((byte) salt.length);
            buffer.put(salt);
            buffer.putInt(ciphertextAndIV.length);
            buffer.put(ciphertextAndIV);
            return buffer.array();
        } finally {
            key.destroy();
            this.key = null;
            Utility.erase(ciphertextAndIV);
        }
    }

    @Override
    public byte[] decrypt(byte[] data) throws CryptographicFailureException {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        final int saltLength = buffer.get();
        if(saltLength <= 0) {
            throw new RuntimeException("Fatal error: salt length in encrypted data is of invalid length");
        }
        byte[] salt = new byte[saltLength];
        buffer.get(salt);

        final int ciphertextAndIVLength = buffer.getInt();
        if(buffer.remaining() != ciphertextAndIVLength) {
            Utility.erase(salt);
            throw new RuntimeException("Fatal error: ciphertext length in encrypted data is of wrong length");
        }
        byte[] ciphertextAndIV = new byte[ciphertextAndIVLength];
        buffer.get(ciphertextAndIV);

        SCryptKeyFactory.SCryptKey key = SCryptKeyFactory.deriveKey(password, salt);
        this.key = key;
        try {
            return super.decrypt(ciphertextAndIV);
        } finally {
            key.destroy();
            Utility.erase(ciphertextAndIV);
            this.key = null;
        }
    }

    @Override
    public void destroy() {
        Utility.erase(password);
    }

}
