package crypto;

/**
 * An exception thrown when authentication fails during a decryption process
 */
public class AuthenticationException extends Exception {
    public AuthenticationException(String s) {
        super(s);
    }
}
