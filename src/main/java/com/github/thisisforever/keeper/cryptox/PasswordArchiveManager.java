package com.github.thisisforever.keeper.cryptox;

import com.github.thisisforever.keeper.application.ui.UIEntry;
import com.github.thisisforever.crypto.*;

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
    // References the file to which the key file will be saved
    private final File entryKeyFile;

    private Cryptographer entryCryptographer;
    private SensitiveFileScribe scribe;
    private DestroyableKey entryKey;

    /**
     * Instantiates the archive manager with a given password and key file location
     * @param password A char[] containing the password to the database, which will be erased when the database is
     *                 saved
     * @param keyFile A reference to a {@link File} object, from which the key file will be read and decrypted. If the
     *                file does not exist, a new key and auth key will be created and saved to the file
     */
    public PasswordArchiveManager(char[] password, File keyFile) {
        scribe = new SensitiveFileScribe(new AESGCMCryptographerWithPasswordBasedKeyDerivation(password));
        entryKeyFile = keyFile;
    }

    public void populateEntryKeys() throws CryptographicFailureException, IOException {
        // Decrypt the key file and obtain the keys used for password encrypt/decrypt
        // If the key file doesn't exist, create new keys which will be saved later
        byte[] keyBytes;
        if(!entryKeyFile.exists())
            keyBytes = Utility.generateRandomBytes(PASSWORD_KEY_LENGTH);
        else {
            keyBytes = scribe.readAndDecrypt(entryKeyFile);
        }
        // Create handles to the keys which can be used by AES
        entryKey = new DestroyableKey(keyBytes);
        entryCryptographer = new AESGCMCryptographerWithKey(entryKey);
        Utility.erase(keyBytes);
    }

    /**
     * Changes the database password NOTE: untested as of 7/28/2020
     * @param password The new password for the database
     */
    public void changePassword(char[] password) {
        //NYI archiveCryptographer.changePassword(password);
    }

    /**
     * Encrypts a password with the password encryption key and generates authentication data
     * @param password The password to encrypt
     * @return A byte[] containing data required during authentication and decryption, or null if encryption somehow
     * failed
     */
    public byte[] encryptPassword(char[] password) {
        // Encode the password using UTF-8, generating a byte[]
        byte[] passwordBytes = Utility.encode(password);
        // Encrypt the password bytes
        byte[] data;
        try {
            data = entryCryptographer.encrypt(passwordBytes);
            return data;
        } finally {
            Utility.erase(passwordBytes);
        }
    }

    /**
     * Decrypts the given password data, authenticating in the process
     * @param encryptedData A byte[] containing the output of {@link PasswordArchiveManager#encryptPassword}
     * @return a {@link String} containing the decrypted password if decryption and authentication were successful,
     * otherwise null
     */
    public String decryptPassword(byte[] encryptedData) throws CryptographicFailureException {
        byte[] plaintext = entryCryptographer.decrypt(encryptedData);
        String result = Utility.decode(plaintext);
        Utility.erase(plaintext);
        return result;
    }

    /**
     * Loads all password entries from a given file. If authentication or decryption fail, the method returns null
     * @param f The file to load entries from
     * @return an {@link ArrayList} of all {@link Entry} objects that were decrypted, or null if authentication or
     * decryption were unsuccessful.
     */
    public ArrayList<Entry> openDatabase(File f) throws CryptographicFailureException, IOException {
        byte[] plaintext = scribe.readAndDecrypt(f);

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
                website = Utility.decode(data);
                Utility.erase(data);
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
                username = Utility.decode(data);
                Utility.erase(data);
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
        Utility.erase(plaintext);
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
    public boolean closeDatabase(File f, ArrayList<UIEntry> database)  {
        byte[] keyBytes = entryKey.getEncoded();
        try {
            scribe.encryptAndWrite(entryKeyFile, keyBytes);
        } catch(IOException e) {
            e.printStackTrace();
            System.err.println("Unable to write to key file!");
            return false;
        }
        Utility.erase(keyBytes);
        // Convert all of the entry data to bytes and write them to a byte stream
        ErasableByteStream byteStream = new ErasableByteStream(16384);
        byte[] sizeBytes = new byte[2];
        byte[] stringData;
        ByteBuffer sizeBuffer = ByteBuffer.wrap(sizeBytes);
        for (UIEntry uie : database) {
            Entry e = uie.getEntry();
            String website = e.getWebsite();
            stringData = Utility.encode(website);
            short size = (short) stringData.length;
            sizeBuffer.putShort(size);
            sizeBuffer.position(0);
            byteStream.writeBytes(sizeBytes);
            if(size > 0) {
                byteStream.writeBytes(stringData);
                Utility.erase(stringData);
            }
            String username = e.getUsername();
            stringData = Utility.encode(username);
            size = (short) stringData.length;
            sizeBuffer.putShort(size);
            sizeBuffer.position(0);
            byteStream.writeBytes(sizeBytes);
            if(size > 0) {
                byteStream.writeBytes(stringData);
                Utility.erase(stringData);
            }
            byte[] passwordData = e.getPasswordData();
            if(passwordData == null || passwordData.length == 0) {
                sizeBuffer.putShort((short) 0);
                byteStream.writeBytes(sizeBytes);
            } else {
                sizeBuffer.putShort((short) passwordData.length);
                byteStream.writeBytes(sizeBytes);
                byteStream.writeBytes(passwordData);
                Utility.erase(passwordData);
            }
            sizeBuffer.position(0);
        }
        byte[] plaintextData = byteStream.toByteArray();
        byteStream.erase();

        try {
            // Encrypt the data using the given keys and salts
            scribe.encryptAndWrite(f, plaintextData);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            cleanup();
            Utility.erase(plaintextData);
            Utility.erase(sizeBytes);
        }
    }

    private void cleanup() {
        entryCryptographer.destroy();
        scribe.destroy();
    }

}
