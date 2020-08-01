package crypto;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class PremadeKeyCryptographer implements Cryptographer{

    private DestroyableKey key, authKey;

    private byte[] plaintext, ciphertext;
    private byte[] iv;
    private byte[] authHash;
    private byte[] packedData;

    public PremadeKeyCryptographer(DestroyableKey key, DestroyableKey authKey) {
        this.key = key;
        this.authKey = authKey;
    }

    /**
     * Encrypts data with the given keys
     * @param data The plaintext data to encryption
     * @return the resulting packed encryption data, including IV, authentication data and ciphertext
     * @throws GeneralSecurityException If initializing encryption failed
     */
    @Override
    public byte[] encrypt(byte[] data) throws UnsupportedSystemException, InvalidKeyException {
        try {
            plaintext = Arrays.copyOf(data, data.length);
            initEncryption();
            encrypt();
            generateAuthenticationHash();
            packData();
            return Arrays.copyOf(packedData, packedData.length);
        } finally {
            cleanup();
        }
    }

    /**
     * Decrypts the given packed data, returning the resulting plaintext data
     * @param data Packed data, created by {@link PremadeKeyCryptographer#encrypt()}
     * @return the resulting plaintext data
     * @throws DataFormatException If the data is misaligned (possible tampering or data corruption)
     * @throws AuthenticationException If the data couldn't be authenticated (possible tampering or data corruption)
     * @throws GeneralSecurityException If initiating decryption failed (system does not support the algorithm)
     * @throws InvalidKeyException If the key given failed to decrypt the data
     */
    public byte[] decrypt(byte[] data)
            throws UnsupportedSystemException, DataFormatException, AuthenticationException, InvalidKeyException {
        try {
            packedData = Arrays.copyOf(data, data.length);
            unpackData();
            if (!authenticate()) {
                throw new AuthenticationException("Data authentication failed!");
            }
            decrypt();
            return Arrays.copyOf(plaintext, plaintext.length);
        } finally {
            cleanup();
        }
    }

    @Override
    public void destroy() {
        key.destroy();
        authKey.destroy();
        key = authKey = null;
    }

    public DestroyableKey getKey() {
        return key;
    }

    public DestroyableKey getAuthKey() {
        return authKey;
    }

    private void initEncryption() {
        iv = CryptoUtil.generateIV();
    }

    private void encrypt() throws UnsupportedSystemException {
        try {
            Cipher c = Cipher.getInstance(CryptoUtil.ALGORITHM_MODE_PADDING);
            c.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
            ciphertext = c.doFinal(plaintext);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | java.security.InvalidKeyException
                | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            throw new UnsupportedSystemException("Unable to initiate encryption");
        }
    }

    private void decrypt() throws InvalidKeyException {
        try {
            Cipher c = Cipher.getInstance(CryptoUtil.ALGORITHM_MODE_PADDING);
            c.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            plaintext = c.doFinal(ciphertext);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | java.security.InvalidKeyException
                | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            throw new InvalidKeyException("Invalid key");
        }
    }

    private void generateAuthenticationHash() throws UnsupportedSystemException, InvalidKeyException {
        try {
            Mac mac = Mac.getInstance(CryptoUtil.AUTHENTICATION_ALGORITHM);
            mac.init(authKey);
            mac.update(iv);
            mac.update(ciphertext);
            authHash = mac.doFinal();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new UnsupportedSystemException("Unable to generate authentication data");
        } catch (java.security.InvalidKeyException e) {
            throw new InvalidKeyException("Unable to initiate authentication with the given key");
        }
    }

    public boolean authenticate() throws UnsupportedSystemException, InvalidKeyException {
        try {
            Mac mac = Mac.getInstance(CryptoUtil.AUTHENTICATION_ALGORITHM);
            mac.init(authKey);
            mac.update(iv);
            mac.update(ciphertext);
            byte[] generatedAuthHash = mac.doFinal();
            boolean match = MessageDigest.isEqual(authHash, generatedAuthHash);
            CryptoUtil.erase(generatedAuthHash);
            return match;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new UnsupportedSystemException("Unable to generate authentication data");
        } catch (java.security.InvalidKeyException e) {
            throw new InvalidKeyException("Unable to initiate authentication with the given key");
        }
    }

    /**
     * Packs the object's encryption data into a single byte array, storing it in
     * {@link PremadeKeyCryptographer#packedData}.
     */
    private void packData() {
        ByteBuffer buffer = ByteBuffer.allocate(iv.length + authHash.length + ciphertext.length +
                Short.BYTES + Integer.BYTES);
        buffer.put(iv);
        buffer.putShort((short) authHash.length);
        buffer.put(authHash);
        buffer.putInt(ciphertext.length);
        buffer.put(ciphertext);
        packedData = buffer.array();
    }

    /**
     * Unpacks the data packed in {@link PremadeKeyCryptographer#packedData}, storing IV data, authentication data and
     * ciphertext data in the object's respective arrays.
     * @throws DataFormatException If there was a data misalignment (possible corruption or data tampering)
     */
    private void unpackData() throws DataFormatException {
        iv = new byte[CryptoUtil.IV_LENGTH];
        ByteBuffer buffer = ByteBuffer.wrap(packedData);
        buffer.get(iv);

        short authLength = buffer.getShort();
        if(authLength != CryptoUtil.AUTHENTICATION_DATA_LENGTH) {
            System.out.println(authLength);
            throw new DataFormatException("Authentication hash was of incorrect size");
        }
        if(authLength > buffer.remaining()) {
            throw new DataFormatException("Reached end of data while parsing");
        }
        authHash = new byte[CryptoUtil.AUTHENTICATION_DATA_LENGTH];
        buffer.get(authHash);

        int ciphertextLength = buffer.getInt();
        if(ciphertextLength != buffer.remaining()) {
            throw new DataFormatException("Ciphertext length did not match remaining data");
        }
        ciphertext = new byte[ciphertextLength];
        buffer.get(ciphertext);
    }

    private void cleanup() {
        CryptoUtil.erase(plaintext);
        CryptoUtil.erase(ciphertext);
        CryptoUtil.erase(packedData);
        CryptoUtil.erase(iv);
        CryptoUtil.erase(authHash);
        plaintext = ciphertext = packedData = iv = authHash = null;
    }
}
