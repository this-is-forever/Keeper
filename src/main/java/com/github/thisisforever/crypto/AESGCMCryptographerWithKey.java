package com.github.thisisforever.crypto;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class AESGCMCryptographerWithKey implements Cryptographer {

    // The length of the initialization vector
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16; // bytes
    private static final int GCM_TAG_LENGTH_BITS = GCM_TAG_LENGTH * 8;
    // The algorithm used during encryption
    private static final String ALGORITHM_MODE_PADDING = "AES/GCM/NoPadding";

    protected DestroyableKey key;

    protected AESGCMCryptographerWithKey() {

    }

    public AESGCMCryptographerWithKey(DestroyableKey key) {
        this.key = key;
    }

    @Override
    public byte[] encrypt(byte[] plaintext) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM_MODE_PADDING);
            byte[] iv = Utility.generateRandomBytes(GCM_IV_LENGTH);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);
            byte[] ciphertext = cipher.doFinal(plaintext);
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            Utility.erase(iv);
            Utility.erase(ciphertext);
            return buffer.array();
        } catch (NoSuchPaddingException | NoSuchAlgorithmException
                | BadPaddingException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException("Fatal error: system does not support AES in GCM mode");
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Fatal error: derived key failed to decrypt data");
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException("Fatal error: illegal block size; was the data tampered with?");
        }
    }

    @Override
    public byte[] decrypt(byte[] data) throws CryptographicFailureException {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM_MODE_PADDING);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, data, 0, GCM_IV_LENGTH);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);
            byte[] plaintext = cipher.doFinal(data, GCM_IV_LENGTH, data.length - GCM_IV_LENGTH);
            return plaintext;
        } catch (AEADBadTagException e) {
            throw new CryptographicFailureException("Tag mismatch!");
        } catch (NoSuchPaddingException | NoSuchAlgorithmException
                | BadPaddingException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException("Fatal error: system does not support AES in GCM mode");
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Fatal error: derived key failed to decrypt data");
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException("Fatal error: illegal block size; was the data tampered with?");
        }
    }

    @Override
    public void destroy() {
        key.destroy();
    }
}
