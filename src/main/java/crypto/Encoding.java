package crypto;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Class used to encode and decode {@link String} to and from byte[] using UTF-8
 */
public class Encoding {

    public static final Charset ENCODER = StandardCharsets.UTF_8;

    /**
     * Securely(?) converts an array of char to an array of byte
     * @param c The array to convert
     * @return The resulting byte array
     */
    public static byte[] encode(char [] c) {
        ByteBuffer bb = ENCODER.encode(CharBuffer.wrap(c));
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
        return s.getBytes(ENCODER);
    }

    /**
     * Decodes a byte[] into a {@link String} object
     * @param bytes the array to decode
     * @return a {@link String} object in the event decoding was successful, otherwise null
     */
    public static String decode(byte[] bytes) {
        try {
            return new String(bytes, ENCODER.name());
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }
}
