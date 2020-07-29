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
public class PasswordGen {

    public static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    public static final String NUMBERS = "0123456789";
    public static final String SYMBOLS = "`~!@#$%^&*()-=_+,./<>?;':\"[]{}\\|";
    //public static String[] CUSTOM_CHARSET; NYI

    // Contains flags that determine whether each of the above character sets should be enabled during generation
    public static final boolean[] PASSWORD_CHARACTER_FLAGS = new boolean[4];
    // Contains the sets themselves
    private static String[] CHARACTER_SETS = {
            UPPERCASE, LOWERCASE, NUMBERS, SYMBOLS
    };

    // Indexes which define the indexes for the character sets and flags that determine whether they're enabled
    private static int UPPERCASE_INDEX = 0,
            LOWERCASE_INDEX = 1,
            NUMBERS_INDEX = 2,
            SYMBOLS_INDEX = 3;

    /**
     * Sets flags for each generator option
     * @param uppercase true if the generator should produce uppercase letters, otherwise false
     * @param lowercase true if the generator should produce lowercase letters, otherwise false
     * @param numbers true if the generator should produce numbers, otherwise false
     * @param symbols true if the generator should produce symbols, otherwise false
     */
    public static void setFlags(boolean uppercase, boolean lowercase, boolean numbers, boolean symbols) {
        PASSWORD_CHARACTER_FLAGS[UPPERCASE_INDEX] = uppercase;
        PASSWORD_CHARACTER_FLAGS[LOWERCASE_INDEX] = lowercase;
        PASSWORD_CHARACTER_FLAGS[NUMBERS_INDEX] = numbers;
        PASSWORD_CHARACTER_FLAGS[SYMBOLS_INDEX] = symbols;
    }

    /**
     * Determines whether at least one password character set has been enabled, otherwise false
     * @return true if at least one flag within {@link PasswordGen#PASSWORD_CHARACTER_FLAGS} is set to true,
     * otherwise false
     */
    public static boolean anySelected() {
        boolean selected = false;
        for(boolean flag : PASSWORD_CHARACTER_FLAGS)
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
    public static char[] generatePassword(int length) {
        // Add each of the character sets to a String
        String charset = "";
        for(int i = 0; i < PASSWORD_CHARACTER_FLAGS.length; i++) {
            if(PASSWORD_CHARACTER_FLAGS[i])
                charset += CHARACTER_SETS[i];
        }
        // If the set is empty, none are selected; throw an exception
        if(charset.isEmpty())
            throw new RuntimeException("Error: invalid choice of password character sets");
        // Generate a randomized password with the given length
        char[] password = new char[length];
        SecureRandom prng = new SecureRandom();
        // Add each index we iterate over to an array list which will be used during verification
        ArrayList<Integer> indexes = new ArrayList<>(length);
        for(int i = 0; i < length; i++) {
            password[i] = charset.charAt(prng.nextInt(charset.length()));
            indexes.add(i);
        }
        List passwordList = Arrays.asList(password);
        // Verify that the password contains at least one of each character set
        for(int i = 0; i < CHARACTER_SETS.length && indexes.size() > 0; i++) {
            // Skip this character set if it isn't enabled
            if(!PASSWORD_CHARACTER_FLAGS[i])
                continue;
            char[] chars = CHARACTER_SETS[i].toCharArray();
            List charsetList = Arrays.asList(chars);
            // Does the password contain at least one of the character set?
            if(Collections.disjoint(passwordList, charsetList)) {
                // No; pop a random index from the list of indexes and swap it out with a random character from this set
                int index = indexes.remove(prng.nextInt(indexes.size()));
                password[index] = chars[prng.nextInt(chars.length)];
            }
        }
        // Return the generated password
        return password;
    }

}
