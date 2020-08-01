package crypto;

/**
 * An exception thrown by {@link PasswordBasedCryptographer} when an invalid password is given during decryption
 */
public class InvalidKeyException extends Exception {

    public InvalidKeyException(String desc) {
        super(desc);
    }

}
