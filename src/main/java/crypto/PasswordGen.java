package crypto;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PasswordGen {

    public static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    public static final String NUMBERS = "0123456789";
    public static final String SYMBOLS = "`~!@#$%^&*()-=_+,./<>?;':\"[]{}\\|";
    public static String[] CUSTOM_CHARSET;

    public static final boolean[] PASSWORD_CHARACTER_FLAGS = new boolean[4];
    private static String[] CHARACTER_SETS = {
            UPPERCASE, LOWERCASE, NUMBERS, SYMBOLS
    };

    private static int UPPERCASE_INDEX = 0,
            LOWERCASE_INDEX = 1,
            NUMBERS_INDEX = 2,
            SYMBOLS_INDEX = 3;

    public static void setFlags(boolean uppercase, boolean lowercase, boolean numbers, boolean symbols) {
        PASSWORD_CHARACTER_FLAGS[UPPERCASE_INDEX] = uppercase;
        PASSWORD_CHARACTER_FLAGS[LOWERCASE_INDEX] = lowercase;
        PASSWORD_CHARACTER_FLAGS[NUMBERS_INDEX] = numbers;
        PASSWORD_CHARACTER_FLAGS[SYMBOLS_INDEX] = symbols;
    }

    /**
     * https://stackoverflow.com/a/51187090
     * @param charset
     */
    public static void setCustomCharset(String charset) {
        List<String> list = charset.codePoints()
                    .mapToObj(Character::toChars)
                    .map(String::valueOf)
                    .collect(Collectors.toList());
        CUSTOM_CHARSET = (String[])list.toArray();
    }

    public static boolean anySelected() {
        boolean selected = false;
        for(boolean flag : PASSWORD_CHARACTER_FLAGS)
            selected |= flag;
        return selected;
    }

    public static char[] generatePassword(int length) {
        String charset = "";
        for(int i = 0; i < PASSWORD_CHARACTER_FLAGS.length; i++) {
            if(PASSWORD_CHARACTER_FLAGS[i])
                charset += CHARACTER_SETS[i];
        }
        assert !charset.isEmpty() : "Error: invalid choice of password character sets";
        char[] password = new char[length];
        SecureRandom prng = new SecureRandom();
        ArrayList<Integer> indexes = new ArrayList<>(length);
        for(int i = 0; i < length; i++) {
            password[i] = charset.charAt(prng.nextInt(charset.length()));
            indexes.add(i);
        }
        List passwordList = Arrays.asList(password);
        for(int i = 0; i < CHARACTER_SETS.length && indexes.size() > 0; i++) {
            if(!PASSWORD_CHARACTER_FLAGS[i])
                continue;
            char[] chars = CHARACTER_SETS[i].toCharArray();
            List charsetList = Arrays.asList(chars);
            if(Collections.disjoint(passwordList, charsetList)) {
                int index = indexes.remove(prng.nextInt(indexes.size()));
                password[index] = chars[prng.nextInt(chars.length)];
            }
        }
        return password;
    }

}
