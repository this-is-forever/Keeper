package crypto;

import com.lambdaworks.crypto.SCrypt;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;

/**
 * Allows for the encryption and decryption of data. Destroys data used in the process of encryption/decyption for
 * security purposes
 */
public class PasswordBasedCryptographer implements Cryptographer {

    private byte[] password;

    private DestroyableKey key, authKey;
    private byte[] iv, salt, authSalt;
    private byte[] ciphertext, plaintext, authHash;
    private byte[] packedData;

    /**
     * Instantiates a new object with a given password
     * @param password The password to use during encryption and decryption
     */
    public PasswordBasedCryptographer(char[] password) {
        changePassword(password);
    }

    /**
     * Changes the password for this object. Overwrites the old password in the process
     * @param password The new password to encrypt and decrypt with
     */
    public void changePassword(char[] password) {
        destroy();
        this.password = Encoding.encode(password);
    }

    /**
     * Encrypts the given plaintext data using the object's password
     * @param plaintext Data to encrypt
     * @return The resulting packed data, including salts, authorization data and ciphertext, or null if
     * encryption failed
     */
    public byte[] encrypt(byte[] plaintext) throws UnsupportedSystemException {
        try {
            this.plaintext = Arrays.copyOf(plaintext, plaintext.length);
            initEncryption();
            generateKeys();
            encrypt();
            generateAuthenticationHash();
            packData();
            return Arrays.copyOf(packedData, packedData.length);
        } catch (GeneralSecurityException e) {
            throw new UnsupportedSystemException("Unable to initiate crypto");
        } finally {
            cleanup();
        }
    }

    /**
     * Decrypts packed encryption data, returning the resulting plaintext data
     * @param encryptedData Data to decrypt
     * @return The resulting plaintext data
     * @throws InvalidKeyException If the object's password failed to decrypt the data
     * @throws AuthenticationException If data authentication failed (possible tampering or data corruption)
     * @throws GeneralSecurityException If initiating decryption failed
     * @throws DataFormatException If the data is misaligned (possible tampering or data corruption)
     */
    public byte[] decrypt(byte[] encryptedData)
            throws UnsupportedSystemException, InvalidKeyException, DataFormatException,
            AuthenticationException {
        try {
            packedData = Arrays.copyOf(encryptedData, encryptedData.length);
            unpackData();
            generateKeys();
            if (!authenticate()) {
                throw new AuthenticationException("Data authentication failed!");
            }
            decrypt();
            return Arrays.copyOf(plaintext, plaintext.length);
        } catch(GeneralSecurityException e) {
            throw new UnsupportedSystemException("Unable to initiate crypto");
        } finally {
            cleanup();
        }
    }

    /**
     * Erases the object's password data and sets the reference to null, allowing garbage collection
     */
    public void destroy() {
        CryptoUtil.erase(password);
        password = null;
    }

    /**
     * Generates random IV and salt data to be used for encryption
     */
    private void initEncryption() {
        iv = CryptoUtil.generateIV();
        salt = CryptoUtil.generateSalt();
        authSalt = CryptoUtil.generateSalt();
    }

    /**
     * Generates keys to be used during encryption/decryption using the object's IV and salt data
     * @throws GeneralSecurityException If scrypt failed to generate keys
     */
    private void generateKeys() throws GeneralSecurityException {
        byte[] keyData = SCrypt.scrypt(password, salt, CryptoUtil.SCRYPT_ITERATIONS_COUNT,
                CryptoUtil.SCRYPT_BLOCK_SIZE, CryptoUtil.SCRYPT_PARALLELISM_FACTOR, CryptoUtil.SCRYPT_KEY_LENGTH);
        key = new DestroyableKey(keyData);
        keyData = SCrypt.scrypt(password, authSalt, CryptoUtil.SCRYPT_ITERATIONS_COUNT_AUTHORIZATION,
                CryptoUtil.SCRYPT_BLOCK_SIZE, CryptoUtil.SCRYPT_PARALLELISM_FACTOR,CryptoUtil.SCRYPT_KEY_LENGTH);
        authKey = new DestroyableKey(keyData);
    }

    /**
     * Packs the object's encryption data into a single byte array, storing it in {@link PasswordBasedCryptographer#packedData}
     */
    private void packData() {
        ByteBuffer buffer = ByteBuffer.allocate(iv.length + salt.length + authSalt.length
                + authHash.length + ciphertext.length + Byte.BYTES + Integer.BYTES);
        buffer.put(salt);
        buffer.put(authSalt);
        buffer.put(iv);
        buffer.put((byte) authHash.length);
        buffer.put(authHash);
        buffer.putInt(ciphertext.length);
        buffer.put(ciphertext);
        packedData = buffer.array();
    }

    /**
     * Unpacks the data stored in {@link PasswordBasedCryptographer#packedData}, storing them in the object's various data
     * arrays so that it may be used during decryption.
     * @throws DataFormatException If there is a data misalignment (possible data corruption or tampering)
     */
    private void unpackData() throws DataFormatException {
        iv = new byte[CryptoUtil.IV_LENGTH];
        salt = new byte[CryptoUtil.SALT_LENGTH];
        authSalt = new byte[CryptoUtil.SALT_LENGTH];
        ByteBuffer buffer = ByteBuffer.wrap(packedData);
        buffer.get(salt);
        buffer.get(authSalt);
        buffer.get(iv);

        byte authLength = buffer.get();
        if(authLength != CryptoUtil.AUTHENTICATION_DATA_LENGTH) {
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

    /**
     * Generates a hash to be used to authenticate data when decryption occurs
     * @throws NoSuchAlgorithmException If the hashing algorithm is unsupported by the user's JVM
     * @throws java.security.InvalidKeyException If hashing failed because the key was invalid
     */
    private void generateAuthenticationHash() throws NoSuchAlgorithmException, java.security.InvalidKeyException {
        Mac mac = Mac.getInstance(CryptoUtil.AUTHENTICATION_ALGORITHM);
        mac.init(authKey);
        mac.update(iv);
        mac.update(ciphertext);
        authHash = mac.doFinal();
    }

    /**
     * Authenticates ciphertext data by hashing it and comparing it to the hash stored in
     * {@link PasswordBasedCryptographer#authHash}
     * @return true if authentication was successful, otherwise false
     * @throws java.security.InvalidKeyException If an invalid key was given
     * @throws NoSuchAlgorithmException If the hashing algorithm is unsupported by the user's JVM
     */
    private boolean authenticate() throws java.security.InvalidKeyException, NoSuchAlgorithmException {
        Mac mac = Mac.getInstance(CryptoUtil.AUTHENTICATION_ALGORITHM);
        mac.init(authKey);
        mac.update(iv);
        mac.update(ciphertext);
        byte[] generatedAuthHash = mac.doFinal();
        boolean match = MessageDigest.isEqual(authHash, generatedAuthHash);
        CryptoUtil.erase(generatedAuthHash);
        return match;
    }

    /**
     * Encrypts the data in {@link PasswordBasedCryptographer#plaintext}, storing it in {@link PasswordBasedCryptographer#ciphertext}
     * @throws GeneralSecurityException If encryption failed
     */
    private void encrypt() throws GeneralSecurityException {
        try {
            Cipher c = Cipher.getInstance(CryptoUtil.ALGORITHM_MODE_PADDING);
            c.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
            ciphertext = c.doFinal(plaintext);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | java.security.InvalidKeyException
                | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            throw new GeneralSecurityException("Encryption failed.");
        }
    }

    /**
     * Attempts to decrypt the data in {@link PasswordBasedCryptographer#ciphertext}, storing the result in
     * {@link PasswordBasedCryptographer#plaintext}
     * @throws InvalidKeyException If the object's key failed to decrypt the data
     */
    private void decrypt() throws InvalidKeyException {
        try {
            Cipher c = Cipher.getInstance(CryptoUtil.ALGORITHM_MODE_PADDING);
            c.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            plaintext = c.doFinal(ciphertext);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | java.security.InvalidKeyException
                | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            throw new InvalidKeyException("Invalid password given.");
        }
    }

    /**
     * Overwrites the object's byte arrays and sets them to null, allowing them to be garbage collected
     */
    private void eraseArrays() {
        CryptoUtil.erase(iv);
        CryptoUtil.erase(salt);
        CryptoUtil.erase(authSalt);
        CryptoUtil.erase(authHash);
        CryptoUtil.erase(ciphertext);
        CryptoUtil.erase(plaintext);
        CryptoUtil.erase(packedData);
        iv = salt = authSalt = authHash = ciphertext = plaintext = packedData = null;
    }

    /**
     * Destroys this object's keys and wipes its arrays and allows space allocated for those items to be garbage
     * collected
     */
    private void cleanup() {
        key.destroy();
        authKey.destroy();
        key = authKey = null;
        eraseArrays();
    }

}
