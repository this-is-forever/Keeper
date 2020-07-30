package crypto;

/**
 * An exception thrown by {@link Cryptographer} when an invalid password is given during decryption
 */
public class InvalidKeyException extends Exception {

    public InvalidKeyException(String desc) {
        super(desc);
    }

}
