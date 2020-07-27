package crypto;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Encoding {

    public static final Charset ENCODING = StandardCharsets.UTF_8;

    /**
     * Securely(?) converts an array of char to an array of byte
     * @param c The array to convert
     * @return The resulting byte array
     */
    public static byte[] encode(char [] c) {
        ByteBuffer bb = ENCODING.encode(CharBuffer.wrap(c));
        byte[] b = new byte[bb.limit()];
        bb.get(b);
        Crypto.erase(bb.array());
        return b;
    }

    /**
     * Encodes a String using the encoding specification specified by ENCODING
     * @param s The String to encode
     * @return A byte array containing the encoded String
     */
    public static byte[] encode(String s) {
        return s.getBytes(ENCODING);
    }

    public static String decode(byte[] bytes) {
        try {
            return new String(bytes, ENCODING.name());
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }
}
