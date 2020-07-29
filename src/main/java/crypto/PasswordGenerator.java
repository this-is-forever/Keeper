package crypto;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Generates passwords with lower- and uppercase letters, numbers and symbols. Ensures passwords generated have at
 * least one of each type of desired character, if possible
 */
public class PasswordGenerator {

    public static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    public static final String NUMBERS = "0123456789";
    public static final String SYMBOLS = "`~!@#$%^&*()-=_+,./<>?;':\"[]{}\\|";
    // Contains the sets themselves
    private static String[] CHARACTER_SETS = {
            UPPERCASE, LOWERCASE, NUMBERS, SYMBOLS
    };

    // Indexes which define the indexes for the character sets and flags that determine whether they're enabled
    private static int UPPERCASE_INDEX = 0,
            LOWERCASE_INDEX = 1,
            NUMBERS_INDEX = 2,
            SYMBOLS_INDEX = 3;

    // Contains flags that determine whether each of the above character sets should be enabled during generation
    private final boolean[] characterSetFlags;

    // Psuedo random number generator used for generating password characters
    private SecureRandom prng;

    public PasswordGenerator() {
        characterSetFlags = new boolean[CHARACTER_SETS.length];
    }

    /**
     * Sets flags for each generator option
     * @param uppercase true if the generator should produce uppercase letters, otherwise false
     * @param lowercase true if the generator should produce lowercase letters, otherwise false
     * @param numbers true if the generator should produce numbers, otherwise false
     * @param symbols true if the generator should produce symbols, otherwise false
     */
    public void setFlags(boolean uppercase, boolean lowercase, boolean numbers, boolean symbols) {
        characterSetFlags[UPPERCASE_INDEX] = uppercase;
        characterSetFlags[LOWERCASE_INDEX] = lowercase;
        characterSetFlags[NUMBERS_INDEX] = numbers;
        characterSetFlags[SYMBOLS_INDEX] = symbols;
    }

    /**
     * Determines whether at least one password character set has been enabled, otherwise false
     * @return true if at least one flag within {@link PasswordGenerator#characterSetFlags} is set to true,
     * otherwise false
     */
    public boolean anySelected() {
        boolean selected = false;
        for(boolean flag : characterSetFlags)
            selected |= flag;
        return selected;
    }

    /**
     * Generates a password with the given length. Ensures at least one of each type of character is produced
     * if possible (if the length is smaller than the number of enabled sets, it won't be able to produce a password
     * with at least one of each type)
     * @param length the length of the password to be generated
     * @return a char[] containing the generated password
     * @throws RuntimeException in the event no character sets were enabled prior to generating the password
     */
    public char[] generatePassword(int length) {
        initRandom();
        String charset = generateCharacterSetString();

        if(charset.isEmpty()) {
            throw new RuntimeException("Error: invalid choice of password character sets");
        }

        char[] password = generateUnvalidatedPassword(charset, length);
        validatePassword(password);
        return password;
    }

    /**
     * Initializes this object's pseudo-random number generator
     */
    private void initRandom() {
        prng = new SecureRandom();
    }

    /**
     * Concatenates all currently selected character sets into one large {@link String}
     * @return the resulting {@link String} after concatenation
     */
    private String generateCharacterSetString() {
        String charset = "";
        final int sets = CHARACTER_SETS.length;
        for(int i = 0; i < sets; i++) {
            if(characterSetFlags[i]) {
                charset += CHARACTER_SETS[i];
            }
        }
        return charset;
    }

    /**
     * Produces a random series of characters with a given length using a given charset
     * @param charset The set from which to choose characters. See
     * {@link PasswordGenerator#generateCharacterSetString()}.
     * @param length The length of the password to generate
     * @return the resulting char[] with randomized characters
     */
    private char[] generateUnvalidatedPassword(String charset, int length) {
        char[] password = new char[length];
        for(int i = 0; i < length; i++) {
            password[i] = charset.charAt(prng.nextInt(charset.length()));
        }
        return password;
    }

    /**
     * Ensures a password contains at least one character from each of the enabled character sets for this
     * {@link PasswordGenerator} object. If a charset is missing, a random character is changed to a random character
     * from that charset. If the password's length is shorter than the number of chosen character sets, validation
     * will ensure that each character is of a different charset instead of ensuring one of each is present.
     * @param password The password to validate
     */
    private void validatePassword(char[] password) {
        ArrayList<Integer> unchangedIndexes = range(password.length);
        List passwordList = Arrays.asList(password);
        // Verify that the password contains at least one of each character set
        for(int i = 0; i < CHARACTER_SETS.length && unchangedIndexes.size() > 0; i++) {
            // Skip this character set if it isn't enabled
            if(!characterSetFlags[i])
                continue;
            char[] chars = CHARACTER_SETS[i].toCharArray();
            List charsetList = Arrays.asList(chars);
            // Does the password contain at least one of the character set?
            if(Collections.disjoint(passwordList, charsetList)) {
                // No; pop a random index from the list of indexes and swap it out with a random character from this set
                int index = unchangedIndexes.remove(prng.nextInt(unchangedIndexes.size()));
                password[index] = chars[prng.nextInt(chars.length)];
            }
        }
    }

    /**
     * Generates a list of int values ranging from 0 to a value, exclusive
     * @param to The value to generate values up to
     * @return An {@link ArrayList} containing the generated values
     */
    private static ArrayList<Integer> range(int to) {
        ArrayList<Integer> numbers = new ArrayList<>();
        for(int i = 0; i < to; i++) {
            numbers.add(i);
        }
        return numbers;
    }

}
