package crypto;

import application.ui.UIEntry;

import javax.crypto.SecretKey;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Implements methods that enable the encryption and decryption of passwords, as well as the encryption and
 * decryption of a database of {@link UIEntry} objects
 */
public class PasswordArchiveManager {

    // Defines the length of the key, in bytes, used for password encryption/decryption and for authentication
    private static final int PASSWORD_KEY_LENGTH = 32;

    // References to keys used to encrypt/decrypt and authenticate
    private SecretKey key, authKey, entryKey, entryAuthKey;
    // References the database's password
    private char[] databasePassword;
    // References the file to which the key file will be saved
    private final File entryKeyFile;

    /**
     * Instantiates the archive manager with a given password and key file location
     * @param password A char[] containing the password to the database, which will be erased when the database is
     *                 saved
     * @param keyFile A reference to a {@link File} object, from which the key file will be read and decrypted. If the
     *                file does not exist, a new key and auth key will be created and saved to the file
     * @throws InvalidPasswordException if the password given fails to decrypt the given key file
     */
    public PasswordArchiveManager(char[] password, File keyFile) throws InvalidPasswordException {
        databasePassword = password;
        entryKeyFile = keyFile;
        // Decrypt the key file and obtain the keys used for password encrypt/decrypt
        // If the key file doesn't exist, create new keys which will be saved later
        byte[] keyBytes;
        if(!entryKeyFile.exists())
            keyBytes = Crypto.randomBytes(PASSWORD_KEY_LENGTH * 2);
        else
            keyBytes = Crypto.decryptFile(entryKeyFile, databasePassword);
        // Decryption failed; throw exception
        if(keyBytes == null)
            throw new InvalidPasswordException("Password was incorrect");
        // Create handles to the keys which can be used by AES
        entryKey = new DestroyableKey(keyBytes, 0, PASSWORD_KEY_LENGTH);
        entryAuthKey = new DestroyableKey(keyBytes, PASSWORD_KEY_LENGTH, PASSWORD_KEY_LENGTH);
        // Erase the array for security
        Crypto.erase(keyBytes);
    }

    /**
     * Changes the database password NOTE: untested as of 7/28/2020
     * @param password The new password for the database
     */
    public void changePassword(char[] password) {
        databasePassword = password;
    }

    /**
     * Encrypts a password with the password encryption key and generates authentication data
     * @param password The password to encrypt
     * @return A byte[] containing data required during authentication and decryption, or null if encryption somehow
     * failed
     */
    public byte[] encryptPassword(char[] password) {
        // Encode the password using UTF-8, generating a byte[]
        byte[] passwordBytes = Encoding.encode(password);
        // Encrypt the password bytes
        byte[] data = Crypto.encrypt(passwordBytes, entryKey, entryAuthKey);
        // Erase the encoded password
        Crypto.erase(passwordBytes);
        return data;
    }

    /**
     * Decrypts the given password data, authenticating in the process
     * @param encrypted A byte[] containing the output of {@link PasswordArchiveManager#encryptPassword}
     * @return a {@link String} containing the decrypted password if decryption and authentication were successful,
     * otherwise null
     */
    public String decryptPassword(byte[] encrypted) {
        // Wrap the data in a buffer so we may dismantle it into smaller arrays
        ByteBuffer data = ByteBuffer.wrap(encrypted);
        // Pull out the IV used to encrypt
        byte[] iv = new byte[Crypto.IV_LENGTH];
        data.get(iv);
        // Pull out authentication data
        short authDataLength = data.getShort();
        assert authDataLength == Crypto.AUTHENTICATION_DATA_LENGTH : "Fatal Error: entry password authentication " +
                "data is of invalid size! Tampering?";
        byte[] authData = new byte[Crypto.AUTHENTICATION_DATA_LENGTH];
        data.get(authData);
        // Pull out the ciphertext
        int ciphertextLength = data.getInt();
        assert ciphertextLength == data.remaining() : "Fatal Error: entry ciphertext is of invalid size! Tampering?";
        byte[] ciphertext = new byte[ciphertextLength];
        data.get(ciphertext);

        // Attempt to decrypt the password using the entry key and entry auth key
        byte[] plaintext;
        try {
            plaintext = Crypto.decrypt(ciphertext, authData, iv, entryKey, entryAuthKey);
        } catch (InvalidPasswordException | AuthenticationException e) {
            // Return null if authentication or decryption fail
            e.printStackTrace();
            return null;
        }
        // Decode the resulting bytes
        String result = Encoding.decode(plaintext);
        // Erase sensitive data
        Crypto.erase(plaintext);
        Crypto.erase(iv);
        Crypto.erase(ciphertext);
        Crypto.erase(authData);
        // return the result
        return result;
    }

    /**
     * Loads all password entries from a given file. If authentication or decryption fail, the method returns null
     * @param f The file to load entries from
     * @return an {@link ArrayList} of all {@link Entry} objects that were decrypted, or null if authentication or
     * decryption were unsuccessful.
     */
    public ArrayList<Entry> openDatabase(File f) {
        // Begin by opening and decrypting the file
        byte[] plaintext = decryptFile(f);
        // If decrypt returned null, decryption failed; return
        if(plaintext == null)
            return null;
        // Decryption was successful; continue
        // Wrap the plaintext in a byte buffer so we can grab chunks of data
        ByteBuffer dataReader = ByteBuffer.wrap(plaintext);
        // Begin a list of entry objects, which we will add to as we read each entry
        ArrayList<Entry> entries = new ArrayList<>();

        // Continue reading in data so long as there is more data to read
        byte[] data;
        while(dataReader.hasRemaining()) {
            // Read a website, username and encrypted password data for this entry, and add it to the list
            short size = dataReader.getShort();
            String website;
            if(size > 0) {
                assert dataReader.remaining() >= size : "Error reading archive; possible corruption or tampering";
                assert dataReader.hasRemaining() : "Error: unexpectedly reached end of archive while parsing website";
                data = new byte[size];
                dataReader.get(data);
                website = Encoding.decode(data);
                Crypto.erase(data);
            } else
                website = "";

            assert dataReader.hasRemaining() : "Error: unexpectedly reached end of archive while parsing " +
                    "username length";

            size = dataReader.getShort();
            String username;
            if(size > 0) {
                assert dataReader.remaining() >= size : "Error reading archive; possible corruption or tampering";
                assert dataReader.hasRemaining() : "Error: unexpectedly reached end of archive while parsing username";
                data = new byte[size];
                dataReader.get(data);
                username = Encoding.decode(data);
                Crypto.erase(data);
            } else
                username = "";

            assert dataReader.hasRemaining() : "Error: unexpectedly reached end of archive while parsing " +
                    "password length";

            size = dataReader.getShort();
            if(size > 0) {
                assert dataReader.remaining() >= size : "Error reading archive; possible corruption or tampering";
                assert dataReader.hasRemaining() : "Error: unexpectedly reached end of archive while parsing password";
                data = new byte[size];
                dataReader.get(data);
            } else
                data = null;
            entries.add(new Entry(website, username, data));
        }
        // Erase the plaintext data
        Crypto.erase(plaintext);
        // Return the list of entries
        return entries;
    }

    /**
     * Closes the database, encrypting it and saving it to the given file. Additionally, encrypts and writes the
     * keys to {@link PasswordArchiveManager#entryKeyFile} in the process.
     * @param f The file to save the archive to
     * @param database An {@link ArrayList} of {@link UIEntry} objects containing entries to encrypt and save
     * @return true on success, otherwise false
     */
    public boolean closeDatabase(File f, ArrayList<UIEntry> database) {
        // Encrypt the password encryption/decryption key and auth key and save them to the entryKeyFile
        ByteBuffer buffer = ByteBuffer.allocate(PASSWORD_KEY_LENGTH * 2);
        buffer.put(entryKey.getEncoded());
        buffer.put(entryAuthKey.getEncoded());
        byte[] array = buffer.array();
        Crypto.encryptFile(entryKeyFile, array, databasePassword);
        Crypto.erase(array);
        // Convert all of the entry data to bytes and write them to a byte stream
        EraseableByteStream byteStream = new EraseableByteStream(16384);
        byte[] sizeBytes = new byte[2];
        byte[] stringData;
        ByteBuffer sizeBuffer = ByteBuffer.wrap(sizeBytes);
        for (UIEntry uie : database) {
            Entry e = uie.getEntry();
            String website = e.getWebsite();
            stringData = Encoding.encode(website);
            short size = (short) stringData.length;
            sizeBuffer.putShort(size);
            sizeBuffer.position(0);
            byteStream.writeBytes(sizeBytes);
            if(size > 0) {
                byteStream.writeBytes(stringData);
                Crypto.erase(stringData);
            }
            String username = e.getUsername();
            stringData = Encoding.encode(username);
            size = (short) stringData.length;
            sizeBuffer.putShort(size);
            sizeBuffer.position(0);
            byteStream.writeBytes(sizeBytes);
            if(size > 0) {
                byteStream.writeBytes(stringData);
                Crypto.erase(stringData);
            }
            byte[] passwordData = e.getPasswordData();
            if(passwordData == null || passwordData.length == 0) {
                sizeBuffer.putShort((short) 0);
                byteStream.writeBytes(sizeBytes);
            } else {
                sizeBuffer.putShort((short) passwordData.length);
                byteStream.writeBytes(sizeBytes);
                byteStream.writeBytes(passwordData);
                Crypto.erase(passwordData);
            }
            sizeBuffer.position(0);
        }
        byte[] plaintextData = byteStream.toByteArray();
        byteStream.erase();
        // Generate salts which will be used to encrypt and authenticate the data, and generate keys
        byte[] keySalt = Crypto.generateSalt();
        key = Crypto.deriveKey(databasePassword, keySalt);
        byte[] authSalt = Crypto.generateSalt();
        authKey = Crypto.deriveKey(databasePassword, authSalt);
        // Encrypt the data using the given keys and salts
        byte[] ciphertext = Crypto.encrypt(plaintextData, key, keySalt, authKey, authSalt);
        // Erase sensitive info
        Crypto.erase(key);
        Crypto.erase(authKey);
        Crypto.erase(entryKey);
        Crypto.erase(entryAuthKey);
        Crypto.erase(databasePassword);
        Crypto.erase(plaintextData);
        Crypto.erase(sizeBytes);
        // Were we successful? If not, return false
        if(ciphertext == null)
            return false;
        try(FileOutputStream out = new FileOutputStream(f)) {
            out.write(ciphertext);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        // We were, return true
        Crypto.erase(ciphertext);
        return true;
    }

    /**
     * Decrypts a given file and returns the result. Authenticates data to ensure it wasn't tampered with
     * @param f The file to decrypt
     * @return a byte[] containing decrypted information, or null if authentication or decryption failed
     */
    private byte[] decryptFile(File f) {
        byte[] iv = new byte[Crypto.IV_LENGTH];
        byte[] salt = new byte[Crypto.SALT_LENGTH];
        byte[] authSalt = new byte[Crypto.SALT_LENGTH];
        byte[] authData;
        byte[] ciphertext;
        // Read the salt, iv and cipher text from the file
        try(DataInputStream in = new DataInputStream(new FileInputStream(f))) {
            in.read(salt);
            in.read(authSalt);
            in.read(iv);
            short authLength = in.readShort();
            assert authLength == Crypto.AUTHENTICATION_DATA_LENGTH : "Major error: opened file has invalid authentication " +
                    "data length; Was your file tampered with?";
            authData = new byte[authLength];
            in.read(authData);
            int ciphertextLength = in.readInt();
            assert ciphertextLength == in.available() : "Major error: opened file has invalid cipher length;" +
                    " Was your file tampered with?";
            ciphertext = new byte[ciphertextLength];
            in.read(ciphertext);
        } catch (IOException e) {
            // File doesn't exist; exit
            e.printStackTrace();
            return null;
        }// An error occurred while reading the file; exit

        // Generate a key from the given password and salt
        key = Crypto.deriveKey(databasePassword, salt);
        // Generate the auth key from the given password and salt
        authKey = Crypto.deriveKey(databasePassword, authSalt);
        // Destroy the salt
        Crypto.erase(salt);
        Crypto.erase(authSalt);
        // Attempt to decrypt the file; if an error occurs, exit by returning null
        byte[] plaintext;
        try {
            plaintext = Crypto.decrypt(ciphertext, authData, iv, key, authKey);
        } catch (InvalidPasswordException | AuthenticationException e) {
            plaintext = null;
        }
        // Write over the sensitive data for security purposes
        Crypto.erase(key);
        Crypto.erase(authKey);
        Crypto.erase(ciphertext);
        Crypto.erase(iv);
        Crypto.erase(authData);
        return plaintext;
    }

}
