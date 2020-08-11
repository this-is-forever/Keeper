package com.github.thisisforever.crypto;

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

public class Utility {

    private static final byte BYTE_FILL_VALUE = (byte) 0;
    private static final char CHAR_FILL_VALUE = (char) 0;
    private static final SecureRandom PRNG = new SecureRandom();
    private static Charset ENCODING_CHARSET = StandardCharsets.UTF_8;

    public static void setEncodingCharset(Charset cs) {
        ENCODING_CHARSET = cs;
    }

    public static byte[] generateRandomBytes(int size) {
        byte[] bytes = new byte[size];
        fillRandomly(bytes);
        return bytes;
    }

    public static void fillRandomly(byte[] array) {
        PRNG.nextBytes(array);
    }

    public static void erase(byte[] array) {
        if(array != null) {
            Arrays.fill(array, BYTE_FILL_VALUE);
        }
    }

    public static void erase(char[] array) {
        if(array != null) {
            Arrays.fill(array, CHAR_FILL_VALUE);
        }
    }

    public static byte[] encode(String s) {
        return s.getBytes(ENCODING_CHARSET);
    }

    public static String decode(byte[] encodedString) {
        return new String(encodedString, ENCODING_CHARSET);
    }

    public static byte[] encode(char[] chars) {
        return ENCODING_CHARSET.encode(CharBuffer.wrap(chars)).array();
    }

}
