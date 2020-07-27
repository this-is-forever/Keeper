package crypto;

import application.ui.UIEntry;

import javax.crypto.SecretKey;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class PasswordArchiveManager {

    private static final int PASSWORD_KEY_LENGTH = 32;

    private SecretKey key, authKey, entryKey, entryAuthKey;
    private char[] databasePassword;
    private byte[] databasePasswordEncrypted;
    private final File entryKeyFile;

    public PasswordArchiveManager(char[] password, File keyFile) throws InvalidPasswordException {
        databasePassword = password;
        entryKeyFile = keyFile;
        byte[] keyBytes;
        if(!entryKeyFile.exists())
            keyBytes = Crypto.randomBytes(PASSWORD_KEY_LENGTH * 2);
        else
            keyBytes = Crypto.decryptFile(entryKeyFile, databasePassword);
        if(keyBytes == null)
            throw new InvalidPasswordException("Password was incorrect");
        entryKey = new DestroyableKey(keyBytes, 0, PASSWORD_KEY_LENGTH);
        entryAuthKey = new DestroyableKey(keyBytes, PASSWORD_KEY_LENGTH, PASSWORD_KEY_LENGTH);
        Crypto.erase(keyBytes);
    }

    public void changePassword(char[] password) {
        databasePassword = password;
    }

    public byte[] encryptPassword(char[] password) {
        byte[] passwordBytes = Encoding.encode(password);
        return Crypto.encrypt(passwordBytes, entryKey, entryAuthKey);
    }

    public String decryptPassword(byte[] encrypted) {
        ByteBuffer data = ByteBuffer.wrap(encrypted);
        byte[] iv = new byte[Crypto.IV_LENGTH];
        data.get(iv);
        short authDataLength = data.getShort();
        assert authDataLength == Crypto.AUTHENTICATION_DATA_LENGTH : "Fatal Error: entry password authentication " +
                "data is of invalid size! Tampering?";
        byte[] authData = new byte[Crypto.AUTHENTICATION_DATA_LENGTH];
        data.get(authData);
        int dataLength = data.getInt();
        assert dataLength == data.remaining() : "Fatal Error: entry ciphertext is of invalid size! Tampering?";
        byte[] ciphertext = new byte[dataLength];
        data.get(ciphertext);
        byte[] plaintext;
        try {
            plaintext = Crypto.decrypt(ciphertext, authData, iv, entryKey, entryAuthKey);
        } catch (InvalidPasswordException | AuthenticationException e) {
            e.printStackTrace();
            return null;
        }
        String result = Encoding.decode(plaintext);
        Crypto.erase(plaintext);
        Crypto.erase(iv);
        Crypto.erase(ciphertext);
        Crypto.erase(authData);
        return result;
    }

    public ArrayList<Entry> openDatabase(File f) {
        byte[] plaintext = decryptFile(f);
        if(plaintext == null)
            return null;
        ByteBuffer dataReader = ByteBuffer.wrap(plaintext);
        ArrayList<Entry> entries = new ArrayList<>();

        byte[] data;
        while(dataReader.hasRemaining()) {
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
        Crypto.erase(plaintext);
        return entries;
    }

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
        Crypto.erase(key);
        Crypto.erase(authKey);
        Crypto.erase(ciphertext);
        Crypto.erase(iv);
        Crypto.erase(authData);
        return plaintext;
    }

    public boolean closeDatabase(File f, ArrayList<UIEntry> database) {
        ByteBuffer buffer = ByteBuffer.allocate(PASSWORD_KEY_LENGTH * 2);
        buffer.put(entryKey.getEncoded());
        buffer.put(entryAuthKey.getEncoded());
        byte[] array = buffer.array();
        Crypto.encryptFile(entryKeyFile, array, databasePassword);
        Crypto.erase(array);
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
        byte[] keySalt = Crypto.generateSalt();
        key = Crypto.deriveKey(databasePassword, keySalt);
        byte[] authSalt = Crypto.generateSalt();
        authKey = Crypto.deriveKey(databasePassword, authSalt);

        byte[] ciphertext = Crypto.encrypt(plaintextData, key, keySalt, authKey, authSalt);
        Crypto.erase(key);
        Crypto.erase(authKey);
        Crypto.erase(entryKey);
        Crypto.erase(entryAuthKey);
        Crypto.erase(databasePassword);
        Crypto.erase(plaintextData);
        Crypto.erase(sizeBytes);
        if(ciphertext == null)
            return false;
        try(FileOutputStream out = new FileOutputStream(f)) {
            out.write(ciphertext);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        Crypto.erase(ciphertext);
        return true;
    }

}
