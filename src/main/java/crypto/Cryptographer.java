package crypto;

public interface Cryptographer {

    byte[] encrypt(byte[] data) throws UnsupportedSystemException, InvalidKeyException;

    byte[] decrypt(byte[] data) throws UnsupportedSystemException, InvalidKeyException, DataFormatException,
            AuthenticationException;

    void destroy();

}
