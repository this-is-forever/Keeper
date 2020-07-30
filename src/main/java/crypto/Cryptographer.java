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
public class Cryptographer {
    // The length of the initialization vector
    private static final int IV_LENGTH = 16;
    // Used for creating byte arrays from Strings/char[] and vice versa
    private static final Charset ENCODER = StandardCharsets.UTF_8;
    // The algorithm used during encryption
    //private static final String ALGORITHM_MODE_PADDING = "AES/GCM/NoPadding";
    private static final String ALGORITHM_MODE_PADDING = "AES/CBC/PKCS5PADDING";
    private static final String ALGORITHM = "AES";

    // The length of the salt, in bytes, generated for SCrypt during key generation. Ensures encryption output
    // differs each time encryption occurs
    public static final int SALT_LENGTH = 32;
    // The number of iterations for SCrypt to make when creating a key; a power of 2; bigger numbers = more time
    private static final int SCRYPT_ITERATIONS_COUNT = 262144;
    private static final int SCRYPT_ITERATIONS_COUNT_AUTHORIZATION = 16384;
    // The block size used by SCrypt during key generation. Bigger = more memory required
    private static final int SCRYPT_BLOCK_SIZE = 8;
    // The number of threads used by SCrypt; bigger = more threads, = more memory
    private static final int SCRYPT_PARALLELISM_FACTOR = 1;
    // The length of the key generated by SCrypt and used by AES; must be 128, 192 or 256 bits
    private static final int SCRYPT_KEY_LENGTH_BITS = 256;
    private static final int SCRYPT_KEY_LENGTH = SCRYPT_KEY_LENGTH_BITS / 8;

    private static String AUTHENTICATION_ALGORITHM = "HmacSHA256";
    // The length of the output of the authentication algorithm
    private static final int AUTHENTICATION_DATA_LENGTH_BITS = 256;
    private static final int AUTHENTICATION_DATA_LENGTH = AUTHENTICATION_DATA_LENGTH_BITS / 8;

    private byte[] password;

    private DestroyableKey key, authKey;
    private byte[] iv, salt, authSalt;
    private byte[] ciphertext, plaintext, authHash;
    private byte[] packedData;

    /**
     * Writes over a byte array with zeroes. If data references null, the method does nothing
     * @param data An array of bytes to overwrite
     */
    public static void erase(byte[] data) {
        if(data == null)
            return;
        Arrays.fill(data, (byte)0);
    }

    /**
     * Writes over a char array with zeroes. If data references null, the method does nothing
     * @param data An array of chars to overwrite
     */
    public static void erase(char[] data) {
        if(data == null)
            return;
        Arrays.fill(data, (char)0);
    }

    /**
     * Creates a new array with a given size and fills it with random values
     * @param size The size of the new array
     * @return a byte array of randomly generated values with the desired size
     */
    public static byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        SecureRandom prng = new SecureRandom();
        prng.nextBytes(bytes);
        return bytes;
    }

    /**
     * Generates an array of random bytes to be used for password salting, with length set by
     * {@link Cryptographer@SALT_LENGTH}
     * @return an array of random bytes with the predefined size
     */
    private static byte[] generateSalt() {
        return randomBytes(SALT_LENGTH);
    }

    /**
     * Generates an array of random bytes to be used for initialization vectors, with length set by
     * {@link Cryptographer#IV_LENGTH}
     * @return an array of random bytes with the predefined size
     */
    private static byte[] generateIV() {
        return randomBytes(IV_LENGTH);
    }

    /**
     * Instantiates a new object with a given password
     * @param password The password to use during encryption and decryption
     */
    public Cryptographer(char[] password) {
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
    public byte[] encrypt(byte[] plaintext) {
        try {
            this.plaintext = Arrays.copyOf(plaintext, plaintext.length);
            initEncryption();
            generateKeys();
            encrypt();
            generateAuthenticationHash();
            packData();
            return Arrays.copyOf(packedData, packedData.length);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            return null;
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
            throws InvalidKeyException, AuthenticationException, GeneralSecurityException, DataFormatException {
        try {
            packedData = Arrays.copyOf(encryptedData, encryptedData.length);
            unpackData();
            generateKeys();
            if (!authenticate()) {
                throw new AuthenticationException("Data authentication failed!");
            }
            decrypt();
            return Arrays.copyOf(plaintext, plaintext.length);
        } finally {
            cleanup();
        }
    }

    /**
     * Encrypts data with the given keys
     * @param data The plaintext data to encryption
     * @param key The key to use to decrypt the data
     * @param authKey The key to use to generate authentication data
     * @return the resulting packed encryption data, including IV, authentication data and ciphertext
     * @throws GeneralSecurityException If initializing encryption failed
     */
    public byte[] encryptWithKeys(byte[] data, DestroyableKey key, DestroyableKey authKey)
            throws GeneralSecurityException {
        try {
            plaintext = Arrays.copyOf(data, data.length);
            this.key = key;
            this.authKey = authKey;
            initEncryption();
            encrypt();
            generateAuthenticationHash();
            packDataWithoutSalts();
            return Arrays.copyOf(packedData, packedData.length);
        } finally {
            eraseArrays();
            this.key = null;
            this.authKey = null;
        }
    }

    /**
     * Decrypts the given packed data, returning the resulting plaintext data
     * @param data Packed data, created by {@link Cryptographer#encryptWithKeys}
     * @param key The key to decrypt with
     * @param authKey The key to authenticate the data with
     * @return the resulting plaintext data
     * @throws DataFormatException If the data is misaligned (possible tampering or data corruption)
     * @throws AuthenticationException If the data couldn't be authenticated (possible tampering or data corruption)
     * @throws GeneralSecurityException If initiating decryption failed (system does not support the algorithm)
     * @throws InvalidKeyException If the key given failed to decrypt the data
     */
    public byte[] decryptWithKeys(byte[] data, DestroyableKey key, DestroyableKey authKey)
            throws DataFormatException, AuthenticationException, GeneralSecurityException, InvalidKeyException {
        try {
            this.key = key;
            this.authKey = authKey;
            packedData = Arrays.copyOf(data, data.length);
            unpackDataWithoutKeys();
            if (!authenticate()) {
                throw new AuthenticationException("Data authentication failed!");
            }
            decrypt();
            return Arrays.copyOf(plaintext, plaintext.length);
        } finally {
            eraseArrays();
            this.key = null;
            this.authKey = null;
        }
    }

    /**
     * Erases the object's password data and sets the reference to null, allowing garbage collection
     */
    public void destroy() {
        erase(password);
        password = null;
    }

    /**
     * Generates random IV and salt data to be used for encryption
     */
    private void initEncryption() {
        iv = generateIV();
        salt = generateSalt();
        authSalt = generateSalt();
    }

    /**
     * Generates keys to be used during encryption/decryption using the object's IV and salt data
     * @throws GeneralSecurityException If scrypt failed to generate keys
     */
    private void generateKeys() throws GeneralSecurityException {
        byte[] keyData = SCrypt.scrypt(password, salt, SCRYPT_ITERATIONS_COUNT,
                SCRYPT_BLOCK_SIZE, SCRYPT_PARALLELISM_FACTOR, SCRYPT_KEY_LENGTH);
        key = new DestroyableKey(keyData);
        keyData = SCrypt.scrypt(password, authSalt, SCRYPT_ITERATIONS_COUNT_AUTHORIZATION,
                SCRYPT_BLOCK_SIZE, SCRYPT_PARALLELISM_FACTOR, SCRYPT_KEY_LENGTH);
        authKey = new DestroyableKey(keyData);
    }

    /**
     * Packs the object's encryption data into a single byte array, storing it in {@link Cryptographer#packedData}
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
     * Packs the object's encryption data into a single byte array, storing it in {@link Cryptographer#packedData}.
     * This does not include salt data.
     */
    private void packDataWithoutSalts() {
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
     * Unpacks the data stored in {@link Cryptographer#packedData}, storing them in the object's various data
     * arrays so that it may be used during decryption.
     * @throws DataFormatException If there is a data misalignment (possible data corruption or tampering)
     */
    private void unpackData() throws DataFormatException {
        iv = new byte[IV_LENGTH];
        salt = new byte[SALT_LENGTH];
        authSalt = new byte[SALT_LENGTH];
        ByteBuffer buffer = ByteBuffer.wrap(packedData);
        buffer.get(salt);
        buffer.get(authSalt);
        buffer.get(iv);

        byte authLength = buffer.get();
        if(authLength != AUTHENTICATION_DATA_LENGTH) {
            throw new DataFormatException("Authentication hash was of incorrect size");
        }
        if(authLength > buffer.remaining()) {
            throw new DataFormatException("Reached end of data while parsing");
        }
        authHash = new byte[AUTHENTICATION_DATA_LENGTH];
        buffer.get(authHash);

        int ciphertextLength = buffer.getInt();
        if(ciphertextLength != buffer.remaining()) {
            throw new DataFormatException("Ciphertext length did not match remaining data");
        }
        ciphertext = new byte[ciphertextLength];
        buffer.get(ciphertext);
    }

    /**
     * Unpacks the data packed in {@link Cryptographer#packedData}, storing IV data, authentication data and
     * ciphertext data in the object's respective arrays.
     * @throws DataFormatException If there was a data misalignment (possible corruption or data tampering)
     */
    private void unpackDataWithoutKeys() throws DataFormatException {
        iv = new byte[IV_LENGTH];
        ByteBuffer buffer = ByteBuffer.wrap(packedData);
        buffer.get(iv);

        short authLength = buffer.getShort();
        if(authLength != AUTHENTICATION_DATA_LENGTH) {
            System.out.println(authLength);
            throw new DataFormatException("Authentication hash was of incorrect size");
        }
        if(authLength > buffer.remaining()) {
            throw new DataFormatException("Reached end of data while parsing");
        }
        authHash = new byte[AUTHENTICATION_DATA_LENGTH];
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
        Mac mac = Mac.getInstance(AUTHENTICATION_ALGORITHM);
        mac.init(authKey);
        mac.update(iv);
        mac.update(ciphertext);
        authHash = mac.doFinal();
    }

    /**
     * Authenticates ciphertext data by hashing it and comparing it to the hash stored in
     * {@link Cryptographer#authHash}
     * @return true if authentication was successful, otherwise false
     * @throws java.security.InvalidKeyException If an invalid key was given
     * @throws NoSuchAlgorithmException If the hashing algorithm is unsupported by the user's JVM
     */
    private boolean authenticate() throws java.security.InvalidKeyException, NoSuchAlgorithmException {
        Mac mac = Mac.getInstance(AUTHENTICATION_ALGORITHM);
        mac.init(authKey);
        mac.update(iv);
        mac.update(ciphertext);
        byte[] generatedAuthHash = mac.doFinal();
        boolean match = MessageDigest.isEqual(authHash, generatedAuthHash);
        erase(generatedAuthHash);
        return match;
    }

    /**
     * Encrypts the data in {@link Cryptographer#plaintext}, storing it in {@link Cryptographer#ciphertext}
     * @throws GeneralSecurityException If encryption failed
     */
    private void encrypt() throws GeneralSecurityException {
        try {
            Cipher c = Cipher.getInstance(ALGORITHM_MODE_PADDING);
            c.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
            ciphertext = c.doFinal(plaintext);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | java.security.InvalidKeyException
                | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
            throw new GeneralSecurityException("Encryption failed.");
        }
    }

    /**
     * Attempts to decrypt the data in {@link Cryptographer#ciphertext}, storing the result in
     * {@link Cryptographer#plaintext}
     * @throws InvalidKeyException If the object's key failed to decrypt the data
     */
    private void decrypt() throws InvalidKeyException {
        try {
            Cipher c = Cipher.getInstance(ALGORITHM_MODE_PADDING);
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
        erase(iv);
        erase(salt);
        erase(authSalt);
        erase(authHash);
        erase(ciphertext);
        erase(plaintext);
        erase(packedData);
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

    /**
     * Generates a 32 byte AES key from a password to be used for encryption and decryption purposes.
     * Uses SCrypt and a given salt
     * @param saltData The salt to use during key derivation
     * @return the SecretKey created.
     */
    private DestroyableKey generateKey(byte[] saltData) {
        byte[] bytes;
        try {
            // Use SCrypt to create a 16 byte hash of the password with a randomized salt
            bytes = SCrypt.scrypt(password, saltData, SCRYPT_ITERATIONS_COUNT,
                    SCRYPT_BLOCK_SIZE, SCRYPT_PARALLELISM_FACTOR, SCRYPT_KEY_LENGTH);
        } catch (GeneralSecurityException e) {
            return null;
        }
        // Wipe the array
        // Create a new SecretKeySpec with the bytes given
        return new DestroyableKey(bytes);
    }

}
