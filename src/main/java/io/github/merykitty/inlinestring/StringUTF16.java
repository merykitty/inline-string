package io.github.merykitty.inlinestring;

import io.github.merykitty.inlinestring.internal.ArraysSupport;
import io.github.merykitty.inlinestring.internal.Utils;

import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.Locale;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.invoke.MethodType.methodType;

final class StringUTF16 {
    public static byte[] newBytesFor(int len) {
        try {
            return (byte[]) NEW_BYTES_FOR.invokeExact(len);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static void putChar(byte[] val, int index, int c) {
        try {
            PUT_CHAR.invokeExact(val, index, c);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static char getChar(byte[] val, int index) {
        try {
            return (char) GET_CHAR.invokeExact(val, index);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static int codePointAt(byte[] value, int index, int end) {
        try {
            return (int) CODE_POINT_AT.invokeExact(value, index, end);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static int codePointBefore(byte[] value, int index) {
        try {
            return (int) CODE_POINT_BEFORE.invokeExact(value, index);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static int codePointCount(byte[] value, int beginIndex, int endIndex) {
        try {
            return (int) CODE_POINT_COUNT.invokeExact(value, beginIndex, endIndex);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static char[] toChars(byte[] value) {
        try {
            return (char[]) TO_CHARS.invokeExact(value);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }


    public static byte[] toBytes(char[] val, int off, int len) {
        try {
            return (byte[]) TO_BYTES_0.invokeExact(val, off, len);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static byte[] compress(char[] val, int off, int len) {
        try {
            return (byte[]) COMPRESS_0.invokeExact(val, off, len);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static byte[] compress(byte[] val, int off, int len) {
        try {
            return (byte[]) COMPRESS_1.invokeExact(val, off, len);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static int compress(char[] src, int srcOff, byte[] dst, int dstOff, int len) {
        try {
            return (int) COMPRESS_2.invokeExact(src, srcOff, dst, dstOff, len);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static int compress(byte[] src, int srcOff, byte[] dst, int dstOff, int len) {
        try {
            return (int) COMPRESS_3.invokeExact(src, srcOff, dst, dstOff, len);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static byte[] toBytes(int[] val, int index, int len) {
        try {
            return (byte[]) TO_BYTES_1.invokeExact(val, index, len);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static byte[] toBytes(char c) {
        try {
            return (byte[]) TO_BYTES_2.invokeExact(c);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static byte[] toBytesSupplementary(int cp) {
        try {
            return (byte[]) TO_BYTES_SUPPLEMENTARY.invokeExact(cp);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static void getChars(byte[] value, int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        try {
            GET_CHARS.invokeExact(value, srcBegin, srcEnd, dst, dstBegin);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static int compareTo(byte[] value, byte[] other) {
        try {
            return (int) COMPARE_TO.invokeExact(value, other);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static int compareToLatin1(byte[] value, byte[] other) {
        try {
            return (int) COMPARE_TO_LATIN1.invokeExact(value, other);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static int compareToCI(byte[] value, byte[] other) {
        try {
            return (int) COMPARE_TO_CI.invokeExact(value, other);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static int compareToCI_Latin1(byte[] value, byte[] other) {
        try {
            return (int) COMPARE_TO_CI_LATIN1.invokeExact(value, other);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static int hashCode(byte[] value) {
        try {
            return (int) HASH_CODE.invokeExact(value);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static int indexOf(byte[] value, int ch, int fromIndex) {
        try {
            return (int) INDEX_OF_0.invokeExact(value, ch, fromIndex);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static int indexOf(byte[] value, byte[] str) {
        try {
            return (int) INDEX_OF_1.invokeExact(value, str);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static int indexOf(byte[] value, int valueCount, byte[] str, int strCount, int fromIndex) {
        try {
            return (int) INDEX_OF_2.invokeExact(value, valueCount, str, strCount, fromIndex);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static int indexOfLatin1(byte[] value, byte[] str) {
        try {
            return (int) INDEX_OF_LATIN1_0.invokeExact(value, str);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static int indexOfLatin1(byte[] src, int srcCount, byte[] tgt, int tgtCount, int fromIndex) {
        try {
            return (int) INDEX_OF_LATIN1_1.invokeExact(src, srcCount, tgt, tgtCount, fromIndex);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static int lastIndexOf(byte[] src, int srcCount,
                                  byte[] tgt, int tgtCount, int fromIndex) {
        try {
            return (int) LAST_INDEX_OF_0.invokeExact(src, srcCount, tgt, tgtCount, fromIndex);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static int lastIndexOf(byte[] value, int ch, int fromIndex) {
        try {
            return (int) LAST_INDEX_OF_1.invokeExact(value, ch, fromIndex);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static InlineString replace(byte[] value, char oldChar, char newChar) {
        int len = value.length >> 1;
        int i = -1;
        while (++i < len) {
            if (getChar(value, i) == oldChar) {
                break;
            }
        }
        if (i < len) {
            byte[] buf = new byte[value.length];
            for (int j = 0; j < i; j++) {
                putChar(buf, j, getChar(value, j)); // TBD:arraycopy?
            }
            while (i < len) {
                char c = getChar(value, i);
                putChar(buf, i, c == oldChar ? newChar : c);
                i++;
            }
            // Check if we should try to compress to latin1
            if (Utils.COMPACT_STRINGS &&
                    !StringLatin1.canEncode(oldChar) &&
                    StringLatin1.canEncode(newChar)) {
                byte[] val = compress(buf, 0, len);
                if (val != null) {
                    return new InlineString(val, Utils.LATIN1);
                }
            }
            return new InlineString(buf, Utils.UTF16);
        }
        return new InlineString(value, Utils.UTF16);
    }

    public static InlineString replace(byte[] value, int valLen, boolean valLat1,
                                       byte[] targ, int targLen, boolean targLat1,
                                       byte[] repl, int replLen, boolean replLat1)
    {
        assert targLen > 0;
        assert !valLat1 || !targLat1 || !replLat1;

        //  Possible combinations of the arguments/result encodings:
        //  +---+--------+--------+--------+-----------------------+
        //  | # | VALUE  | TARGET | REPL   | RESULT                |
        //  +===+========+========+========+=======================+
        //  | 1 | Latin1 | Latin1 |  UTF16 | null or UTF16         |
        //  +---+--------+--------+--------+-----------------------+
        //  | 2 | Latin1 |  UTF16 | Latin1 | null                  |
        //  +---+--------+--------+--------+-----------------------+
        //  | 3 | Latin1 |  UTF16 |  UTF16 | null                  |
        //  +---+--------+--------+--------+-----------------------+
        //  | 4 |  UTF16 | Latin1 | Latin1 | null or UTF16         |
        //  +---+--------+--------+--------+-----------------------+
        //  | 5 |  UTF16 | Latin1 |  UTF16 | null or UTF16         |
        //  +---+--------+--------+--------+-----------------------+
        //  | 6 |  UTF16 |  UTF16 | Latin1 | null, Latin1 or UTF16 |
        //  +---+--------+--------+--------+-----------------------+
        //  | 7 |  UTF16 |  UTF16 |  UTF16 | null or UTF16         |
        //  +---+--------+--------+--------+-----------------------+

        if (Utils.COMPACT_STRINGS && valLat1 && !targLat1) {
            // combinations 2 or 3
            return new InlineString(value, Utils.UTF16); // for string to return this;
        }

        int i = (Utils.COMPACT_STRINGS && valLat1)
                ? StringLatin1.indexOf(value, targ) :
                (Utils.COMPACT_STRINGS && targLat1)
                        ? indexOfLatin1(value, targ)
                        : indexOf(value, targ);
        if (i < 0) {
            return new InlineString(value, Utils.UTF16); // for string to return this;
        }

        // find and store indices of substrings to replace
        int j, p = 0;
        int[] pos = new int[16];
        pos[0] = i;
        i += targLen;
        while ((j = ((Utils.COMPACT_STRINGS && valLat1)
                ? StringLatin1.indexOf(value, valLen, targ, targLen, i) :
                (Utils.COMPACT_STRINGS && targLat1)
                        ? indexOfLatin1(value, valLen, targ, targLen, i)
                        : indexOf(value, valLen, targ, targLen, i))) > 0)
        {
            if (++p == pos.length) {
                pos = Arrays.copyOf(pos, ArraysSupport.newLength(p, 1, p >> 1));
            }
            pos[p] = j;
            i = j + targLen;
        }

        int resultLen;
        try {
            resultLen = Math.addExact(valLen,
                    Math.multiplyExact(++p, replLen - targLen));
        } catch (ArithmeticException ignored) {
            throw new OutOfMemoryError("Required length exceeds implementation limit");
        }
        if (resultLen == 0) {
            return InlineString.EMPTY_STRING;
        }

        byte[] result = newBytesFor(resultLen);
        int posFrom = 0, posTo = 0;
        for (int q = 0; q < p; ++q) {
            int nextPos = pos[q];
            if (Utils.COMPACT_STRINGS && valLat1) {
                while (posFrom < nextPos) {
                    char c = (char)(value[posFrom++] & 0xff);
                    putChar(result, posTo++, c);
                }
            } else {
                while (posFrom < nextPos) {
                    putChar(result, posTo++, getChar(value, posFrom++));
                }
            }
            posFrom += targLen;
            if (Utils.COMPACT_STRINGS && replLat1) {
                for (int k = 0; k < replLen; ++k) {
                    char c = (char)(repl[k] & 0xff);
                    putChar(result, posTo++, c);
                }
            } else {
                for (int k = 0; k < replLen; ++k) {
                    putChar(result, posTo++, getChar(repl, k));
                }
            }
        }
        if (Utils.COMPACT_STRINGS && valLat1) {
            while (posFrom < valLen) {
                char c = (char)(value[posFrom++] & 0xff);
                putChar(result, posTo++, c);
            }
        } else {
            while (posFrom < valLen) {
                putChar(result, posTo++, getChar(value, posFrom++));
            }
        }

        if (Utils.COMPACT_STRINGS && replLat1 && !targLat1) {
            // combination 6
            byte[] lat1Result = compress(result, 0, resultLen);
            if (lat1Result != null) {
                return new InlineString(lat1Result, Utils.LATIN1);
            }
        }
        return new InlineString(result, Utils.UTF16);
    }

    public static boolean regionMatchesCI(byte[] value, int toffset,
                                          byte[] other, int ooffset, int len) {
        try {
            return (boolean) REGION_MATCHES_CI.invokeExact(value, toffset, other, ooffset, len);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static boolean regionMatchesCI_Latin1(byte[] value, int toffset,
                                                 byte[] other, int ooffset,
                                                 int len) {
        try {
            return (boolean) REGION_MATCHES_CI_LATIN1.invokeExact(value, toffset, other, ooffset, len);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static InlineString toLowerCase(InlineString str, byte[] value, Locale locale) {
        try {
            return new InlineString((String)TO_LOWER_CASE.invokeExact(str.toString(), value, locale));
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static InlineString toUpperCase(InlineString str, byte[] value, Locale locale) {
        try {
            return new InlineString((String)TO_UPPER_CASE.invokeExact(str.toString(), value, locale));
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static InlineString trim(byte[] value) {
        int length = value.length >> 1;
        int len = length;
        int st = 0;
        while (st < len && getChar(value, st) <= ' ') {
            st++;
        }
        while (st < len && getChar(value, len - 1) <= ' ') {
            len--;
        }
        return ((st > 0) || (len < length )) ?
                new InlineString(Arrays.copyOfRange(value, st << 1, len << 1), Utils.UTF16) :
                new InlineString(value, Utils.UTF16);
    }

    public static int indexOfNonWhitespace(byte[] value) {
        try {
            return (int) INDEX_OF_NON_WHITESPACE.invokeExact(value);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static int lastIndexOfNonWhitespace(byte[] value) {
        try {
            return (int) LAST_INDEX_OF_NON_WHITESPACE.invokeExact(value);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static InlineString strip(byte[] value) {
        int length = value.length >>> 1;
        int left = indexOfNonWhitespace(value);
        if (left == length) {
            return InlineString.EMPTY_STRING;
        }
        int right = lastIndexOfNonWhitespace(value);
        boolean ifChanged = (left > 0) || (right < length);
        return ifChanged ? newString(value, left, right - left) : new InlineString(value, Utils.UTF16);
    }

    public static InlineString stripLeading(byte[] value) {
        int length = value.length >>> 1;
        int left = indexOfNonWhitespace(value);
        return (left != 0) ? newString(value, left, length - left) : new InlineString(value, Utils.UTF16);
    }

    public static InlineString stripTrailing(byte[] value) {
        int length = value.length >>> 1;
        int right = lastIndexOfNonWhitespace(value);
        return (right != length) ? newString(value, 0, right) : new InlineString(value, Utils.UTF16);
    }

    static Stream<InlineString.ref> lines(byte[] value) {
        return StreamSupport.stream(LinesSpliterator.spliterator(value), false);
    }

    public static InlineString newString(byte[] val, int index, int len) {
        if (len == 0) {
            return InlineString.EMPTY_STRING;
        }
        if (Utils.COMPACT_STRINGS) {
            byte[] buf = compress(val, index, len);
            if (buf != null) {
                return new InlineString(buf, Utils.LATIN1);
            }
        }
        int last = index + len;
        return new InlineString(Arrays.copyOfRange(val, index << 1, last << 1), Utils.UTF16);
    }

    public static boolean contentEquals(byte[] v1, byte[] v2, int len) {
        try {
            return (boolean) CONTENT_EQUALS_0.invokeExact(v1, v2, len);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static boolean contentEquals(byte[] value, CharSequence cs, int len) {
        try {
            return (boolean) CONTENT_EQUALS_1.invokeExact(value, cs, len);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static char charAt(byte[] value, int index) {
        try {
            return (char) CHAR_AT.invokeExact(value, index);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static int lastIndexOfLatin1(byte[] src, int srcCount,
                                        byte[] tgt, int tgtCount, int fromIndex) {
        try {
            return (int) LAST_INDEX_OF_LATIN1.invokeExact(src, srcCount, tgt, tgtCount, fromIndex);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static Spliterator.OfInt charsSpliterator(byte[] array, int acs) {
        try {
            return (Spliterator.OfInt) CHARS_SPLITERATOR.invokeExact(array, acs);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static Spliterator.OfInt codePointsSpliterator(byte[] array, int acs) {
        try {
            return (Spliterator.OfInt) CODE_POINTS_SPLITERATOR.invokeExact(array, acs);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    private static final MethodHandle NEW_BYTES_FOR;
    private static final MethodHandle PUT_CHAR;
    private static final MethodHandle GET_CHAR;
    private static final MethodHandle CODE_POINT_AT;
    private static final MethodHandle CODE_POINT_BEFORE;
    private static final MethodHandle CODE_POINT_COUNT;
    private static final MethodHandle TO_CHARS;
    private static final MethodHandle TO_BYTES_0;
    private static final MethodHandle COMPRESS_0;
    private static final MethodHandle COMPRESS_1;
    private static final MethodHandle COMPRESS_2;
    private static final MethodHandle COMPRESS_3;
    private static final MethodHandle TO_BYTES_1;
    private static final MethodHandle TO_BYTES_2;
    private static final MethodHandle TO_BYTES_SUPPLEMENTARY;
    private static final MethodHandle GET_CHARS;
    private static final MethodHandle COMPARE_TO;
    private static final MethodHandle COMPARE_TO_LATIN1;
    private static final MethodHandle COMPARE_TO_CI;
    private static final MethodHandle COMPARE_TO_CI_LATIN1;
    private static final MethodHandle HASH_CODE;
    private static final MethodHandle INDEX_OF_0;
    private static final MethodHandle INDEX_OF_1;
    private static final MethodHandle INDEX_OF_2;
    private static final MethodHandle INDEX_OF_LATIN1_0;
    private static final MethodHandle INDEX_OF_LATIN1_1;
    private static final MethodHandle LAST_INDEX_OF_0;
    private static final MethodHandle LAST_INDEX_OF_1;
    private static final MethodHandle REGION_MATCHES_CI;
    private static final MethodHandle REGION_MATCHES_CI_LATIN1;
    private static final MethodHandle TO_LOWER_CASE;
    private static final MethodHandle TO_UPPER_CASE;
    private static final MethodHandle INDEX_OF_NON_WHITESPACE;
    private static final MethodHandle LAST_INDEX_OF_NON_WHITESPACE;
    private static final MethodHandle CONTENT_EQUALS_0;
    private static final MethodHandle CONTENT_EQUALS_1;
    private static final MethodHandle CHAR_AT;
    private static final MethodHandle LAST_INDEX_OF_LATIN1;
    private static final MethodHandle CHARS_SPLITERATOR;
    private static final MethodHandle CODE_POINTS_SPLITERATOR;

    private static final class LinesSpliterator implements Spliterator<InlineString.ref> {
        private byte[] value;
        private int index;        // current index, modified on advance/split
        private final int fence;  // one past last index

        private LinesSpliterator(byte[] value, int start, int length) {
            this.value = value;
            this.index = start;
            this.fence = start + length;
        }

        private int indexOfLineSeparator(int start) {
            for (int current = start; current < fence; current++) {
                char ch = getChar(value, current);
                if (ch == '\n' || ch == '\r') {
                    return current;
                }
            }
            return fence;
        }

        private int skipLineSeparator(int start) {
            if (start < fence) {
                if (getChar(value, start) == '\r') {
                    int next = start + 1;
                    if (next < fence && getChar(value, next) == '\n') {
                        return next + 1;
                    }
                }
                return start + 1;
            }
            return fence;
        }

        private InlineString next() {
            int start = index;
            int end = indexOfLineSeparator(start);
            index = skipLineSeparator(end);
            return newString(value, start, end - start);
        }

        @Override
        public boolean tryAdvance(Consumer<? super InlineString.ref> action) {
            if (action == null) {
                throw new NullPointerException("tryAdvance action missing");
            }
            if (index != fence) {
                action.accept(next());
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(Consumer<? super InlineString.ref> action) {
            if (action == null) {
                throw new NullPointerException("forEachRemaining action missing");
            }
            while (index != fence) {
                action.accept(next());
            }
        }

        @Override
        public Spliterator<InlineString.ref> trySplit() {
            int half = (fence + index) >>> 1;
            int mid = skipLineSeparator(indexOfLineSeparator(half));
            if (mid < fence) {
                int start = index;
                index = mid;
                return new LinesSpliterator(value, start, mid - start);
            }
            return null;
        }

        @Override
        public long estimateSize() {
            return fence - index + 1;
        }

        @Override
        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL;
        }

        static LinesSpliterator spliterator(byte[] value) {
            return new LinesSpliterator(value, 0, value.length >>> 1);
        }
    }

    static {
        try {
            var lookup = Utils.STRING_LOOKUP;
            var klass = lookup.findClass("java.lang.StringUTF16");
            NEW_BYTES_FOR = lookup.findStatic(klass, "newBytesFor", methodType(Byte.TYPE.arrayType(),
                    Integer.TYPE));
            PUT_CHAR = lookup.findStatic(klass, "putChar", methodType(Void.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE));
            GET_CHAR = lookup.findStatic(klass, "getChar", methodType(Character.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE));
            CODE_POINT_AT = lookup.findStatic(klass, "codePointAt", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE));
            CODE_POINT_BEFORE = lookup.findStatic(klass, "codePointBefore", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE));
            CODE_POINT_COUNT = lookup.findStatic(klass, "codePointCount", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE));
            TO_CHARS = lookup.findStatic(klass, "toChars", methodType(Character.TYPE.arrayType(),
                    Byte.TYPE.arrayType()));
            TO_BYTES_0 = lookup.findStatic(klass, "toBytes", methodType(Byte.TYPE.arrayType(),
                    Character.TYPE.arrayType(), Integer.TYPE, Integer.TYPE));
            COMPRESS_0 = lookup.findStatic(klass, "compress", methodType(Byte.TYPE.arrayType(),
                    Character.TYPE.arrayType(), Integer.TYPE, Integer.TYPE));
            COMPRESS_1 = lookup.findStatic(klass, "compress", methodType(Byte.TYPE.arrayType(),
                    Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE));
            COMPRESS_2 = lookup.findStatic(klass, "compress", methodType(Integer.TYPE,
                    Character.TYPE.arrayType(), Integer.TYPE, Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE));
            COMPRESS_3 = lookup.findStatic(klass, "compress", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE, Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE));
            TO_BYTES_1 = lookup.findStatic(klass, "toBytes", methodType(Byte.TYPE.arrayType(),
                    Integer.TYPE.arrayType(), Integer.TYPE, Integer.TYPE));
            TO_BYTES_2 = lookup.findStatic(klass, "toBytes", methodType(Byte.TYPE.arrayType(),
                    Character.TYPE));
            TO_BYTES_SUPPLEMENTARY = lookup.findStatic(klass, "toBytesSupplementary", methodType(Byte.TYPE.arrayType(),
                    Integer.TYPE));
            GET_CHARS = lookup.findStatic(klass, "getChars", methodType(Void.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE, Character.TYPE.arrayType(), Integer.TYPE));
            COMPARE_TO = lookup.findStatic(klass, "compareTo", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType(), Byte.TYPE.arrayType()));
            COMPARE_TO_LATIN1 = lookup.findStatic(klass, "compareToLatin1", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType(), Byte.TYPE.arrayType()));
            COMPARE_TO_CI = lookup.findStatic(klass, "compareToCI", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType(), Byte.TYPE.arrayType()));
            COMPARE_TO_CI_LATIN1 = lookup.findStatic(klass, "compareToCI_Latin1", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType(), Byte.TYPE.arrayType()));
            HASH_CODE = lookup.findStatic(klass, "hashCode", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType()));
            INDEX_OF_0 = lookup.findStatic(klass, "indexOf", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE));
            INDEX_OF_1 = lookup.findStatic(klass, "indexOf", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType(), Byte.TYPE.arrayType()));
            INDEX_OF_2 = lookup.findStatic(klass, "indexOf", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE, Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE));
            INDEX_OF_LATIN1_0 = lookup.findStatic(klass, "indexOfLatin1", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType(), Byte.TYPE.arrayType()));
            INDEX_OF_LATIN1_1 = lookup.findStatic(klass, "indexOfLatin1", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE, Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE));
            LAST_INDEX_OF_0 = lookup.findStatic(klass, "lastIndexOf", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE, Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE));
            LAST_INDEX_OF_1 = lookup.findStatic(klass, "lastIndexOf", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE));
            REGION_MATCHES_CI = lookup.findStatic(klass, "regionMatchesCI", methodType(Boolean.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE, Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE));
            REGION_MATCHES_CI_LATIN1 = lookup.findStatic(klass, "regionMatchesCI_Latin1", methodType(Boolean.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE, Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE));
            TO_LOWER_CASE = lookup.findStatic(klass, "toLowerCase", methodType(String.class,
                    String.class, Byte.TYPE.arrayType(), Locale.class));
            TO_UPPER_CASE = lookup.findStatic(klass, "toUpperCase", methodType(String.class,
                    String.class, Byte.TYPE.arrayType(), Locale.class));
            INDEX_OF_NON_WHITESPACE = lookup.findStatic(klass, "indexOfNonWhitespace", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType()));
            LAST_INDEX_OF_NON_WHITESPACE = lookup.findStatic(klass, "lastIndexOfNonWhitespace", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType()));
            CONTENT_EQUALS_0 = lookup.findStatic(klass, "contentEquals", methodType(Boolean.TYPE,
                    Byte.TYPE.arrayType(), Byte.TYPE.arrayType(), Integer.TYPE));
            CONTENT_EQUALS_1 = lookup.findStatic(klass, "contentEquals", methodType(Boolean.TYPE,
                    Byte.TYPE.arrayType(), CharSequence.class, Integer.TYPE));
            CHAR_AT = lookup.findStatic(klass, "charAt", methodType(Character.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE));
            LAST_INDEX_OF_LATIN1 = lookup.findStatic(klass, "lastIndexOfLatin1", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE, Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE));
            var charsSpliteratorClass = lookup.findClass("java.lang.StringUTF16$CharsSpliterator");
            CHARS_SPLITERATOR = lookup.findConstructor(charsSpliteratorClass, methodType(Void.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE));
            var codePointsSpliteratorClass = lookup.findClass("java.lang.StringUTF16$CodePointsSpliterator");
            CODE_POINTS_SPLITERATOR = lookup.findConstructor(codePointsSpliteratorClass, methodType(Void.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
