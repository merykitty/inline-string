package io.github.merykitty.inlinestring.internal;

import java.io.UnsupportedEncodingException;
import java.lang.foreign.MemorySegment.Scope;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Objects;
import jdk.incubator.vector.VectorShape;

public record StringData(long firstHalf, long secondHalf, Scope scope) {
    public static StringData defaultInstance() {
        return DEFAULT_INSTANCE;
    }

    public static boolean compressible(long length) {
        return COMPRESSED_STRINGS && length <= COMPRESS_THRESHOLD;
    }

    public static final boolean COMPRESSED_STRINGS =
            ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN &&
            VectorShape.preferredShape().vectorBitSize() >= 256;
    /**
     * An ASCII string with less than this character is compressed.
     */
    public static final int COMPRESS_THRESHOLD = Long.BYTES * 2 - 1;

    private static final StringData DEFAULT_INSTANCE = new StringData(0, 0, null);

    /*
     * StringIndexOutOfBoundsException  if {@code index} is
     * negative or greater than or equal to {@code length}.
     *
     * Caller needs to ensure that the {@code length} parameter
     * is non-negative.
     */
    public static void checkIndex(int index, int length) {
        if (!testIndex(index, length)) {
            throw new StringIndexOutOfBoundsException("Index " + index + " out of bounds for length " + length);
        }
    }

    public static void checkOffset(int offset, int length) {
        if (!testOffset(offset, length)) {
            throw new StringIndexOutOfBoundsException("Offset " + offset + " out of bounds for length " + length);
        }
    }

    public static void checkBoundsOffCount(int offset, int count, int length) {
        if (!testBoundsOffCount(offset, count, length)) {
            throw new StringIndexOutOfBoundsException("Range [" + offset + ", " + offset + " + " + count + ") out of bounds for length " + length);
        }
    }

    public static void checkBoundsBeginEnd(int begin, int end, int length) {
        if (!testBoundsBeginEnd(begin, end, length)) {
            throw new StringIndexOutOfBoundsException("Range [" + begin + ", " + end + ") out of bounds for length " + length);
        }
    }

    public static boolean testIndex(int index, int length) {
        return Integer.compareUnsigned(index, length) < 0;
    }

    public static boolean testOffset(int offset, int length) {
        return Integer.compareUnsigned(offset, length) <= 0;
    }

    public static boolean testBoundsOffCount(int offset, int count, int length) {
        int end = offset + count;
        return testBoundsBeginEnd(offset, end, length);
    }

    public static boolean testBoundsBeginEnd(int begin, int end, int length) {
        return Integer.compareUnsigned(end, length) <= 0 &&
                Integer.compareUnsigned(begin, end) <= 0;
    }

    public static Charset lookupCharset(String csn) throws UnsupportedEncodingException {
        Objects.requireNonNull(csn);
        try {
            return Charset.forName(csn);
        } catch (UnsupportedCharsetException | IllegalCharsetNameException x) {
            throw new UnsupportedEncodingException(csn);
        }
    }
}
