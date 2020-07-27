package crypto;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.lambdaworks.crypto.SCrypt;

/**
 * Class used for encrypting and decrypting data using AES
 * Also contains methods for generating randomized passwords
 */
public class OldCrypto {
    private static long SALT_SEED = 766368308992441594L;
    private static byte[] SALT = null;

    public static final int ENCRYPT = Cipher.ENCRYPT_MODE;
    public static final int DECRYPT = Cipher.DECRYPT_MODE;

    public static String NUMERICAL = "0123456789",
            LOWERCASE = "abcdefghijklmnopqrstuvwxyz",
            UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
            SYMBOLICAL = " !\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";

    // Flags for the charsets. Bits are set based on the charset
    public static final int FLAG_NUMERICAL = 1,
            FLAG_LOWERCASE = 2,
            FLAG_UPPERCASE = 4,
            FLAG_SYMBOLICAL = 8;

    private static final int[] CHAR_FLAGS = new int[65536];

    // Sets up the CHAR_FLAGS array, setting the flags of each character in each charset for faster lookups
    static {
        for(char c : NUMERICAL.toCharArray())
            CHAR_FLAGS[c] = FLAG_NUMERICAL;
        for(char c : LOWERCASE.toCharArray())
            CHAR_FLAGS[c] = FLAG_LOWERCASE;
        for(char c : UPPERCASE.toCharArray())
            CHAR_FLAGS[c] = FLAG_UPPERCASE;
        for(char c : SYMBOLICAL.toCharArray())
            CHAR_FLAGS[c] = FLAG_SYMBOLICAL;
    }

    /** Encrypts or decrypts bytes stored in data using a given key and iv
     * cryptMode specifies whether the algorithm is encrypting or decrypting (Crypto.ENCRYPT or Crypto.DECRYPT)
     */
    public static byte[] crypt(int cryptMode, byte[] data, byte[] key, byte[] iv) {
        try {
            // Create a cipher instead of AES in CBC mode with PKCS 5 Padding
            Cipher c = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            // Create an AES secrey key spec based on the key given
            SecretKeySpec sks = new SecretKeySpec(key, "AES");
            // Encrypt or Decrypt data based on the mode given and return the resulting bytes if successful, null otherwise
            c.init(cryptMode, sks, new IvParameterSpec(iv));
            return c.doFinal(data);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Generates a 16 byte AES key from a password to be used for encryption and decryption purposes
     */
    public static SecretKey generateKey(char[] password) {
        initSalt(16);
        // Wrap the password in a char buffer
        CharBuffer charBuffer = CharBuffer.wrap(password);
        // Wrap the char buffer in a byte buffer based on UTF-8
        ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
        System.out.println(byteBuffer.array() == null);
        // Wipe the char buffer
        //wipe(charBuffer.array()); // clear sensitive data;
        byte[] bytes;
        try {
            // Use SCrypt to create a 16 byte hash of the password with the salt set by the program
            bytes = SCrypt.scrypt(byteBuffer.array(), SALT, 65536, 8, 1, 16);
        } catch (GeneralSecurityException e) {
            return null;
        }
        // Wipe the array
        wipe(byteBuffer.array());
        // Create a new SecretKeySpec with the bytes given
        return new SecretKeySpec(bytes, "AES");
    }

    @Deprecated
    public static byte[] generateKeyB(char[] password) {
        CharBuffer charBuffer = CharBuffer.wrap(password);
        ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
        wipe(charBuffer.array()); // clear sensitive data;
        byte[] bytes = null;
        try {
            bytes = SCrypt.scrypt(byteBuffer.array(), SALT, 65536, 8, 1, 16);
        } catch (GeneralSecurityException e) {
            return null;
        }
        wipe(byteBuffer.array());
        return bytes;
    }

    /**
     * Generates a key length bytes in length from a password to be used for encryption and decryption purposes
     */
    @Deprecated
    public static byte[] generateKeyB(char[] password, int length) {
        long time = System.currentTimeMillis();
        CharBuffer charBuffer = CharBuffer.wrap(password);
        ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
        wipe(charBuffer.array()); // clear sensitive data;
        byte[] bytes = null;
        try {
            bytes = SCrypt.scrypt(byteBuffer.array(), SALT, 16384, 8, 1, length);
        } catch (GeneralSecurityException e) {
            return null;
        }
        wipe(byteBuffer.array());
        System.out.println("Created key in " + (System.currentTimeMillis() - time) + "ms");
        return bytes;
    }

    /**
     * Fills a char array with null terminator characters to prevent important data from leaking
     */
    public static void wipe(char[] bytes) {
        Arrays.fill(bytes, '\u0000');
    }

    /**
     *  Fills a byte array with 0's to prevent important data from leaking
     */
    public static void wipe(byte[] bytes) {
        Arrays.fill(bytes, (byte) 0);
    }

    /**
     *  Generates an IV to be used during encryption and decryption. The IV is prefixed to the ciphertext when
     * encrypting
     * */
    public static byte[] generateIV(int len) {
        // Create a new SecureRandom and use it to generate random bytes to fill the IV with
        Random r = new SecureRandom();
        byte[] iv = new byte[len];
        r.nextBytes(iv);
        return iv;
    }

    /**
     *  Intializes the Crypt class with a salt to be used by the crypt function during encryption and decryption
     */
    public static void initSalt(int len) {
        Random r = null;
        try {
            // Create a new instance of SHA1PRNG for psuedo random number generation
            r = SecureRandom.getInstance("SHA1PRNG", "SUN");
            // Set the seed to the salt given by the class
            r.setSeed(SALT_SEED);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (r == null) {
            System.err.println("MAJOR ERROR: Unable to generate salt. Exiting!");
            System.exit(0);
        }
        // Create a new byte array with length of len
        SALT = new byte[len];
        // Initialize it with random bytes
        r.nextBytes(SALT);
    }

    /**
     * Generates a char array with all possible characters from the charsets described by flags
     * @param flags An int with bits set based on FLAG_NUMERICAL, FLAG_LOWERCASE, etc.
     * @return A charset with all of the possible characters
     */
    public static char[] getCharset(int flags) {
        String set = "";
        if ((flags & FLAG_NUMERICAL) != 0)
            set += OldCrypto.NUMERICAL;
        if ((flags & FLAG_LOWERCASE) != 0)
            set += OldCrypto.LOWERCASE;
        if ((flags & FLAG_UPPERCASE) != 0)
            set += OldCrypto.UPPERCASE;
        if ((flags & FLAG_SYMBOLICAL) != 0)
            set += OldCrypto.SYMBOLICAL;
        return set.toCharArray();
    }

    /**
     * Creates a randomized password with a given length based on the charset given. It will contain at least one
     * character from each charset
     * @param flags Flags representing which charsets should be used
     * @param length The desired length of the generated password
     * @return a char array with the randomized password
     */
    public static char[] generateRandomPassword(int flags, int length) {
        // Create a new char[] with the given length
        char[] gen = new char[length];
        // Generate the user's preset charset based on the flags given
        char[] charset = getCharset(flags);
        try {
            // Create a SecureRandom for pseudo-random number generation
            SecureRandom r = SecureRandom.getInstance("SHA1PRNG", "SUN");
            do {
                // Set each char in gen to a random character in the charset
                for (int i = 0; i < length; i++)
                    gen[i] = charset[r.nextInt(charset.length)];
                // Loop until the generated charset includes one character from each charset
            } while(!followsRules(gen, flags));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
        // Return the generated password
        return gen;
    }

    /**
     * Generates a new password from the given charset with a given length
     * @param charset The charset to choose from while generating the password
     * @param length The length of the new password
     * @return A randomly generated password that contains at least one character from each charset
     */
    public static char[] generateRandomPassword(char[] charset, int length) {
        // Create a new char[] based on the length given
        char[] gen = new char[length];
        try {
            // Create an instance of SHA1PRNG for pseudo random number generation
            SecureRandom r = SecureRandom.getInstance("SHA1PRNG", "SUN");
            // Set each char in gen to a random character in the charset
            for (int i = 0; i < length; i++)
                gen[i] = charset[r.nextInt(charset.length)];
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
        // Return the generated password
        return gen;
    }

    /**
     * Returns true if the password given contains at least one character from each charset described by flags
     * @param password The password to check
     * @param flags An int with bits set based on FLAG_NUMERICAL, FLAG_LOWERCASE, etc.
     * @return True if the password has at least one character from each charset, false otherwise
     */
    public static boolean followsRules(char[] password, int flags) {
        int foundFlags = 0;
        // Iterate over each character in the password
        for(char c : password) {
            // Set the bits in foundFlags for the charset that contains c
            foundFlags |= CHAR_FLAGS[c];
        }
        // Return true if the password contains at least one character from each charset
        return (flags & foundFlags) == flags;
    }

}
