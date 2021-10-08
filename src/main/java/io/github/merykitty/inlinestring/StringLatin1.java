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

final class StringLatin1 {
    public static char charAt(byte[] value, int index) {
        InlineString.checkIndex(index, value.length);
        return (char)(value[index] & 0xff);
    }

    public static boolean canEncode(int cp) {
        return cp < 0x100;
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

    public static byte[] inflate(byte[] value, int off, int len) {
        try {
            return (byte[]) INFLATE_0.invokeExact(value, off, len);
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

    public static boolean equals(byte[] value, byte[] other) {
        try {
            return (boolean) EQUALS.invokeExact(value, other);
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

    public static int compareToUTF16(byte[] value, byte[] other) {
        try {
            return (int) COMPARE_TO_UTF16.invokeExact(value, other);
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

    public static int compareToCI_UTF16(byte[] value, byte[] other) {
        try {
            return (int) COMPARE_TO_CI_UTF16.invokeExact(value, other);
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

    public static int lastIndexOf(final byte[] value, int ch, int fromIndex) {
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
        if (canEncode(oldChar)) {
            int len = value.length;
            int i = -1;
            while (++i < len) {
                if (value[i] == (byte)oldChar) {
                    break;
                }
            }
            if (i < len) {
                if (canEncode(newChar)) {
                    byte[] buf = StringConcatHelper.newArray(len, Utils.LATIN1);
                    System.arraycopy(value, 0, buf, 0, i);
                    while (i < len) {
                        byte c = value[i];
                        buf[i] = (c == (byte)oldChar) ? (byte)newChar : c;
                        i++;
                    }
                    return new InlineString(buf, len, Utils.LATIN1, 0);
                } else {
                    byte[] buf = StringUTF16.newBytesFor(len);
                    // inflate from latin1 to UTF16
                    inflate(value, 0, buf, 0, i);
                    while (i < len) {
                        char c = (char)(value[i] & 0xff);
                        StringUTF16.putChar(buf, i, (c == oldChar) ? newChar : c);
                        i++;
                    }
                    return new InlineString(buf, len, Utils.UTF16, 0);
                }
            }
        }
        return new InlineString(value, value.length, Utils.LATIN1, 0);
    }

    public static InlineString replace(byte[] value, int valLen,
                                       byte[] targ, int targLen, byte[] repl, int replLen) {
        assert targLen > 0;
        int i, j, p = 0;
        if (valLen == 0 || (i = indexOf(value, valLen, targ, targLen, 0)) < 0) {
            return new InlineString(value, Utils.LATIN1);
        }

        // find and store indices of substrings to replace
        int[] pos = new int[16];
        pos[0] = i;
        i += targLen;
        while ((j = indexOf(value, valLen, targ, targLen, i)) > 0) {
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
            throw new OutOfMemoryError("Required index exceeds implementation limit");
        }
        if (resultLen == 0) {
            return InlineString.EMPTY_STRING;
        }

        byte[] result = StringConcatHelper.newArray(resultLen, Utils.LATIN1);
        int posFrom = 0, posTo = 0;
        for (int q = 0; q < p; ++q) {
            int nextPos = pos[q];
            while (posFrom < nextPos) {
                result[posTo++] = value[posFrom++];
            }
            posFrom += targLen;
            for (int k = 0; k < replLen; ++k) {
                result[posTo++] = repl[k];
            }
        }
        while (posFrom < valLen) {
            result[posTo++] = value[posFrom++];
        }
        return new InlineString(result, Utils.LATIN1);
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

    public static boolean regionMatchesCI_UTF16(byte[] value, int toffset,
                                                byte[] other, int ooffset, int len) {
        try {
            return (boolean) REGION_MATCHES_CI_UTF16.invokeExact(value, toffset, other, ooffset, len);
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
        int len = value.length;
        int st = 0;
        while ((st < len) && ((value[st] & 0xff) <= ' ')) {
            st++;
        }
        while ((st < len) && ((value[len - 1] & 0xff) <= ' ')) {
            len--;
        }
        return ((st > 0) || (len < value.length)) ?
                newString(value, st, len - st) : new InlineString(value, Utils.LATIN1);
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
        int left = indexOfNonWhitespace(value);
        if (left == value.length) {
            return InlineString.EMPTY_STRING;
        }
        int right = lastIndexOfNonWhitespace(value);
        boolean ifChanged = (left > 0) || (right < value.length);
        return ifChanged ? newString(value, left, right - left) : new InlineString(value, Utils.LATIN1);
    }

    public static InlineString stripLeading(byte[] value) {
        int left = indexOfNonWhitespace(value);
        return (left != 0) ? newString(value, left, value.length - left) : new InlineString(value, Utils.LATIN1);
    }

    public static InlineString stripTrailing(byte[] value) {
        int right = lastIndexOfNonWhitespace(value);
        return (right != value.length) ? newString(value, 0, right) : new InlineString(value, Utils.LATIN1);
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

    public static Stream<InlineString.ref> lines(byte[] value) {
        return StreamSupport.stream(LinesSpliterator.spliterator(value), false);
    }

    public static byte[] toBytes(int[] val, int off, int len) {
        try {
            return (byte[]) TO_BYTES_0.invokeExact(val, off, len);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static byte[] toBytes(char c) {
        try {
            return (byte[]) TO_BYTES_1.invokeExact(c);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static InlineString newString(byte[] val, int index, int len) {
        if (StringCompressed.compressible(len)) {
            var data = StringCompressed.compress(val, index, len);
            return new InlineString(InlineString.SMALL_STRING_VALUE, len, data.firstHalf(), data.secondHalf());
        } else {
            return new InlineString(Arrays.copyOfRange(val, index, index + len), len, Utils.LATIN1, 0);
        }
    }

    public static void inflate(byte[] src, int srcOff, char[] dst, int dstOff, int len) {
        try {
            INFLATE_2.invokeExact(src, srcOff, dst, dstOff, len);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }
    public static void inflate(byte[] src, int srcOff, byte[] dst, int dstOff, int len) {
        try {
            INFLATE_3.invokeExact(src, srcOff, dst, dstOff, len);
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

    private static final MethodHandle CHAR_AT;
    private static final MethodHandle CAN_ENCODE;
    private static final MethodHandle TO_CHARS;
    private static final MethodHandle INFLATE_0;
    private static final MethodHandle GET_CHARS;
    private static final MethodHandle EQUALS;
    private static final MethodHandle COMPARE_TO;
    private static final MethodHandle COMPARE_TO_UTF16;
    private static final MethodHandle COMPARE_TO_CI;
    private static final MethodHandle COMPARE_TO_CI_UTF16;
    private static final MethodHandle HASH_CODE;
    private static final MethodHandle INDEX_OF_0;
    private static final MethodHandle INDEX_OF_1;
    private static final MethodHandle INDEX_OF_2;
    private static final MethodHandle LAST_INDEX_OF_0;
    private static final MethodHandle LAST_INDEX_OF_1;
    private static final MethodHandle REGION_MATCHES_CI;
    private static final MethodHandle REGION_MATCHES_CI_UTF16;
    private static final MethodHandle TO_LOWER_CASE;
    private static final MethodHandle TO_UPPER_CASE;
    private static final MethodHandle INDEX_OF_NON_WHITESPACE;
    private static final MethodHandle LAST_INDEX_OF_NON_WHITESPACE;
    private static final MethodHandle GET_CHAR;
    private static final MethodHandle TO_BYTES_0;
    private static final MethodHandle TO_BYTES_1;
    private static final MethodHandle INFLATE_2;
    private static final MethodHandle INFLATE_3;
    private static final MethodHandle CHARS_SPLITERATOR;

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
                return new StringLatin1.LinesSpliterator(value, start, mid - start);
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
            return new LinesSpliterator(value, 0, value.length);
        }
    }

    static {
        try {
            var lookup = Utils.STRING_LOOKUP;
            var klass = lookup.findClass("java.lang.StringLatin1");
            CHAR_AT = lookup.findStatic(klass, "charAt", methodType(Character.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE));
            CAN_ENCODE = lookup.findStatic(klass, "canEncode", methodType(Boolean.TYPE,
                    Integer.TYPE));
            TO_CHARS = lookup.findStatic(klass, "toChars", methodType(Character.TYPE.arrayType(),
                    Byte.TYPE.arrayType()));
            INFLATE_0 = lookup.findStatic(klass, "inflate", methodType(Byte.TYPE.arrayType(),
                    Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE));
            GET_CHARS = lookup.findStatic(klass, "getChars", methodType(Void.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE, Character.TYPE.arrayType(), Integer.TYPE));
            EQUALS = lookup.findStatic(klass, "equals", methodType(Boolean.TYPE,
                    Byte.TYPE.arrayType(), Byte.TYPE.arrayType()));
            COMPARE_TO = lookup.findStatic(klass, "compareTo", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType(), Byte.TYPE.arrayType()));
            COMPARE_TO_UTF16 = lookup.findStatic(klass, "compareToUTF16", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType(), Byte.TYPE.arrayType()));
            COMPARE_TO_CI = lookup.findStatic(klass, "compareToCI", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType(), Byte.TYPE.arrayType()));
            COMPARE_TO_CI_UTF16 = lookup.findStatic(klass, "compareToCI_UTF16", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType(), Byte.TYPE.arrayType()));
            HASH_CODE = lookup.findStatic(klass, "hashCode", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType()));
            INDEX_OF_0 = lookup.findStatic(klass, "indexOf", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE));
            INDEX_OF_1 = lookup.findStatic(klass, "indexOf", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType(), Byte.TYPE.arrayType()));
            INDEX_OF_2 = lookup.findStatic(klass, "indexOf", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE, Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE));
            LAST_INDEX_OF_0 = lookup.findStatic(klass, "lastIndexOf", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE, Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE));
            LAST_INDEX_OF_1 = lookup.findStatic(klass, "lastIndexOf", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE));
            REGION_MATCHES_CI = lookup.findStatic(klass, "regionMatchesCI", methodType(Boolean.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE, Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE));
            REGION_MATCHES_CI_UTF16 = lookup.findStatic(klass, "regionMatchesCI_UTF16", methodType(Boolean.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE, Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE));
            TO_LOWER_CASE = lookup.findStatic(klass, "toLowerCase", methodType(String.class,
                    String.class, Byte.TYPE.arrayType(), Locale.class));
            TO_UPPER_CASE = lookup.findStatic(klass, "toUpperCase", methodType(String.class,
                    String.class, Byte.TYPE.arrayType(), Locale.class));
            INDEX_OF_NON_WHITESPACE = lookup.findStatic(klass, "indexOfNonWhitespace", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType()));
            LAST_INDEX_OF_NON_WHITESPACE = lookup.findStatic(klass, "lastIndexOfNonWhitespace", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType()));
            GET_CHAR = lookup.findStatic(klass, "getChar", methodType(Character.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE));
            TO_BYTES_0 = lookup.findStatic(klass, "toBytes", methodType(Byte.TYPE.arrayType(),
                    Integer.TYPE.arrayType(), Integer.TYPE, Integer.TYPE));
            TO_BYTES_1 = lookup.findStatic(klass, "toBytes", methodType(Byte.TYPE.arrayType(),
                    Character.TYPE));
            INFLATE_2 = lookup.findStatic(klass, "inflate", methodType(Void.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE, Character.TYPE.arrayType(), Integer.TYPE, Integer.TYPE));
            INFLATE_3 = lookup.findStatic(klass, "inflate", methodType(Void.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE, Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE));
            var charsSpliteratorClass = lookup.findClass("java.lang.StringLatin1$CharsSpliterator");
            CHARS_SPLITERATOR = lookup.findConstructor(charsSpliteratorClass, methodType(Void.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE)).asType(methodType(Spliterator.OfInt.class,
                    Byte.TYPE.arrayType(), Integer.TYPE));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
