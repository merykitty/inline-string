package io.github.merykitty.inlinestring;

import io.github.merykitty.inlinestring.internal.SmallStringByteData;
import io.github.merykitty.inlinestring.internal.SmallStringCharData;
import io.github.merykitty.inlinestring.internal.Utils;
import static io.github.merykitty.inlinestring.internal.Helper.*;
import static io.github.merykitty.inlinestring.internal.Utils.COMPRESSED_STRINGS;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jdk.incubator.vector.*;
import jdk.internal.misc.Unsafe;

class StringCompressed {
    /**
     * A Latin1 string with less than this character is compressed.
     */
    static final int COMPRESS_THRESHOLD = Long.BYTES * 2;

    private static final VarHandle BYTE_ARRAY_AS_LONGS = MethodHandles.byteArrayViewVarHandle(Long.TYPE.arrayType(), ByteOrder.nativeOrder()).withInvokeExactBehavior();
    private static final VarHandle BYTE_ARRAY_AS_INTS = MethodHandles.byteArrayViewVarHandle(Integer.TYPE.arrayType(), ByteOrder.nativeOrder()).withInvokeExactBehavior();
    private static final VarHandle BYTE_ARRAY_AS_SHORTS = MethodHandles.byteArrayViewVarHandle(Short.TYPE.arrayType(), ByteOrder.nativeOrder()).withInvokeExactBehavior();
    private static final VarHandle BYTE_ARRAY_AS_BYTES = MethodHandles.arrayElementVarHandle(Byte.TYPE.arrayType()).withInvokeExactBehavior();
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();

    public static char charAt(long firstHalf, long secondHalf, int length, int index) {
        InlineString.checkIndex(index, length);
        return (char)codePointAt(firstHalf, secondHalf, index);
    }

    public static int codePointAt(long firstHalf, long secondHalf, int index) {
        return Byte.toUnsignedInt((byte)getCharHelper(firstHalf, secondHalf, index));
    }

    /**
     * Caller needs to perform range checks beforehand
     *
     * 0 <= srcBegin <= srcEnd <= length <= 16
     * 0 <= dstBegin <= dstBegin + (srcEnd - srcBegin) <= dst.length
     */
    public static void getChars(long firstHalf, long secondHalf, int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        var data = LongVector.zero(LongVector.SPECIES_128)
                .withLane(0, firstHalf)
                .withLane(1, secondHalf)
                .reinterpretAsBytes()
                .rearrange(BYTE_SLICE[srcBegin & (BYTE_SLICE.length - 1)]);
        int len = srcEnd - srcBegin;
        var inflatedAsLongs = data.convertShape(VectorOperators.ZERO_EXTEND_B2S, ShortVector.SPECIES_256, 0)
                .reinterpretAsLongs();
        decompress(new SmallStringCharData(inflatedAsLongs.lane(0),
                inflatedAsLongs.lane(1), inflatedAsLongs.lane(2), inflatedAsLongs.lane(3)),
                dst, dstBegin, len);
    }

    /**
     * s1 is compressed while s2 is a Latin1 string (maybe compressed)
     */
    public static int compareToLatin1(InlineString s1, InlineString s2) {
        var data1 = LongVector.zero(LongVector.SPECIES_128)
                .withLane(0, s1.firstHalf())
                .withLane(1, s1.secondHalf())
                .reinterpretAsBytes();
        ByteVector data2;
        if (s2.isCompressed()) {
            data2 = LongVector.zero(LongVector.SPECIES_128)
                    .withLane(0, s2.firstHalf())
                    .withLane(1, s2.secondHalf())
                    .reinterpretAsBytes();
        } else {
            // s2.value().index > 16
            data2 = ByteVector.fromArray(ByteVector.SPECIES_128, s2.value(), 0);
        }
        int i = data1.compare(VectorOperators.NE, data2)
                .firstTrue();
        if (i < Math.min(s1.length(), s2.length())) {
            int c1 = StringCompressed.codePointAt(s1.firstHalf(), s1.secondHalf(), i);
            int c2;
            if (s2.isCompressed()) {
                c2 = StringCompressed.codePointAt(s2.firstHalf(), s2.secondHalf(), i);
            } else {
                c2 = StringLatin1.charAt(s2.value(), i);
            }
            return c1 - c2;
        } else {
            return s1.length() - s2.length();
        }
    }

    /**
     * s1 is compressed while s2 is a UTF16 string
     */
    public static int compareToUTF16(InlineString s1, InlineString s2) {
        // fast path
        if (s1.isEmpty()) {
            return -s2.length();
        } else {
            int c1 = StringCompressed.codePointAt(s1.firstHalf(), s1.secondHalf(), 0);
            int c2 = StringUTF16.charAt(s2.value(), 0);
            if (c1 != c2) {
                return c1 - c2;
            }
        }
        int limit = Math.min(s1.length(), s2.length());
        var data1First = LongVector.zero(LongVector.SPECIES_128)
                .withLane(0, s1.firstHalf())
                .reinterpretAsBytes()
                .convertShape(VectorOperators.ZERO_EXTEND_B2S, ShortVector.SPECIES_128, 0)
                .reinterpretAsShorts();
        ShortVector data2First;
        if (s2.length() < ShortVector.SPECIES_128.length()) {
            var data2Compressed = compress(s2.value());
            data2First = LongVector.zero(LongVector.SPECIES_128)
                    .withLane(0, data2Compressed.firstHalf())
                    .withLane(1, data2Compressed.secondHalf())
                    .reinterpretAsShorts();
        } else {
            data2First = ByteVector.fromArray(ByteVector.SPECIES_128, s2.value(), 0)
                    .reinterpretAsShorts();
        }
        int i = data1First.compare(VectorOperators.NE, data2First)
                .firstTrue();
        if (i == ShortVector.SPECIES_128.length() && limit > Long.BYTES) {
            var data1Second = LongVector.zero(LongVector.SPECIES_128)
                    .withLane(0, s1.secondHalf())
                    .reinterpretAsBytes()
                    .convertShape(VectorOperators.ZERO_EXTEND_B2S, ShortVector.SPECIES_128, 0)
                    .reinterpretAsShorts();
            int loadOffset = Math.min(Long.BYTES, s2.length() - ShortVector.SPECIES_128.length());
            int regionOffset = Long.BYTES - loadOffset;
            var data2Second = ByteVector.fromArray(ByteVector.SPECIES_128, s2.value(), loadOffset)
                    .reinterpretAsShorts();
            if (regionOffset != 0) {
                data2Second = data2Second.rearrange(SHORT_SLICE_128[regionOffset & (SHORT_SLICE_128.length - 1)]);
            }
            i = data1Second.compare(VectorOperators.NE, data2Second)
                    .firstTrue() + Long.BYTES;
        }
        if (i < limit) {
            int c1 = StringCompressed.codePointAt(s1.firstHalf(), s1.secondHalf(), i);
            int c2 = StringUTF16.charAt(s2.value(), i);
            return c1 - c2;
        } else {
            return s1.length() - s2.length();
        }
    }

    /**
     * s1 is compressed while s2 is a Latin1 string (maybe compressed)
     * <p>
     * 0 <= offset1 < offset1 + len <= s1.length() <= 16
     * 0 <= offset2 < offset2 + len <= s2.length()
     */
    public static boolean regionMatchesLatin1(InlineString s1, int offset1, InlineString s2, int offset2, int len) {
        // Align 2 vectors to have corresponding regions at [offset1, offset1 + len[
        var data1 = LongVector.zero(LongVector.SPECIES_128)
                .withLane(0, s1.firstHalf())
                .withLane(1, s1.secondHalf())
                .reinterpretAsBytes();
        ByteVector data2; int regionOffset2;
        if (s2.isCompressed()) {
            // 0 <= offset2 <= offset2 + len <= length2 <= 16
            data2 = LongVector.zero(LongVector.SPECIES_128)
                    .withLane(0, s2.firstHalf())
                    .withLane(1, s2.secondHalf())
                    .reinterpretAsBytes();
            regionOffset2 = offset2;
        } else {
            // if (offset1 >= offset2) loadOffset = 0;
            // else if (offset2 - offset1 + vectorLength <= length2) loadOffset = offset2 - offset1;
            // else loadOffset = s2.index() - vectorLength
            int loadOffset = Math.max(0, Math.min(offset2 - offset1, s2.length() - ByteVector.SPECIES_128.length()));
            regionOffset2 = Math.min(offset2, Math.max(offset1, offset2 + ByteVector.SPECIES_128.length() - s2.length()));
            data2 = ByteVector.fromArray(ByteVector.SPECIES_128, s2.value(), loadOffset);
        }
        if (regionOffset2 != offset1) {
            data2 = data2.rearrange(BYTE_SLICE[(regionOffset2 - offset1) & (BYTE_SLICE.length - 1)]);
        }
        return data1.compare(VectorOperators.EQ, data2)
                .or(BYTE_INDEX_VECTOR.compare(VectorOperators.LT, (byte)offset1))
                .or(BYTE_INDEX_VECTOR.compare(VectorOperators.GE, (byte)(offset1 + len)))
                .allTrue();
    }

    /**
     * s1 is compressed while s2 is a UTF16 string
     *
     * 0 <= offset1 < offset1 + len <= s1.length()
     * 0 <= offset2 < offset2 + len <= s2.length()
     */
    public static boolean regionMatchesUTF16(InlineString s1, int offset1, InlineString s2, int offset2, int len) {
        // fast path
        int c1 = StringCompressed.codePointAt(s1.firstHalf(), s1.secondHalf(), offset1);
        int c2 = StringUTF16.charAt(s2.value(), offset2);
        if (c1 != c2) {
            return false;
        }
        var data2Compressed = SmallStringByteData.default;
        if (s2.length() < Long.BYTES) {
            data2Compressed = compress(s2.value());
        }
        if (offset1 < Long.BYTES) {
            var temp = regionMatchesUTF16Half(s1.firstHalf(), offset1, s2, offset2, Math.min(len, Long.BYTES - offset1), data2Compressed);
            if (!temp) {
                return false;
            }
        }
        if (offset1 + len > Long.BYTES) {
            int offset1Second = Math.max(offset1, Long.BYTES);
            var temp = regionMatchesUTF16Half(s1.secondHalf(), offset1Second - Long.BYTES,
                    s2, offset2 + offset1Second - offset1, offset1 + len - offset1Second, data2Compressed);
            return temp;
        }
        return true;
    }

    /**
     * Compare each half of the string
     *
     * 0 <= offset1 <= offset1 + len <= 8
     * 0 <= offset2 <= offset2 + len <= s2.index()
     *
     * @param half1 The half being compared
     * @param offset1 The offset from the beginning of the half
     * @param s2 The second UTF16 string
     * @param offset2 The offset of the second string correspond to offset1
     * @param len The len of the region being compared in the half
     * @param data2Compressed If length2 < 8, the caller need to provide the data of
     *                        it in the form of 2 longs for vector creation
     * @return The matchness of 2 regions in 2 strings
     */
    private static boolean regionMatchesUTF16Half(long half1, int offset1, InlineString s2, int offset2, int len, SmallStringByteData data2Compressed) {
        // Align 2 vectors to have corresponding regions at [offset1, offset1 + len[
        var data1 = LongVector.zero(LongVector.SPECIES_128)
                .withLane(0, half1)
                .reinterpretAsBytes()
                .convertShape(VectorOperators.ZERO_EXTEND_B2S, ShortVector.SPECIES_128, 0)
                .reinterpretAsShorts();
        ShortVector data2; int regionOffset2;
        if (s2.length() < Long.BYTES) {
            data2 = LongVector.zero(LongVector.SPECIES_128)
                    .withLane(0, data2Compressed.firstHalf())
                    .withLane(1, data2Compressed.secondHalf())
                    .reinterpretAsShorts();
            regionOffset2 = offset2;
        } else {
            int loadOffset = Math.max(0, Math.min(offset2 - offset1, s2.length() - ShortVector.SPECIES_128.length()));
            regionOffset2 = Math.min(offset2, Math.max(offset1, offset2 + ShortVector.SPECIES_128.length() - s2.length()));
            data2 = ByteVector.fromArray(ByteVector.SPECIES_128, s2.value(), loadOffset * Short.BYTES)
                    .reinterpretAsShorts();
        }
        if (regionOffset2 != offset1) {
            data2 = data2.rearrange(SHORT_SLICE_128[(regionOffset2 - offset1) & (SHORT_SLICE_128.length - 1)]);
        }
        return data1.compare(VectorOperators.EQ, data2)
                .or(SHORT_INDEX_VECTOR_128.compare(VectorOperators.LT, (byte)offset1))
                .or(SHORT_INDEX_VECTOR_128.compare(VectorOperators.GE, (byte)(offset1 + len)))
                .allTrue();
    }

    /**
     * 0 <= length <= 16
     */
    public static int hashCode(long firstHalf, long secondHalf, int length) {
        final int tempResult;
        if (IntVector.SPECIES_PREFERRED.length() >= Long.BYTES * 2) {
            // bitSize == 512
            var intSpecies = IntVector.SPECIES_512;
            var dataAsInts = LongVector.zero(LongVector.SPECIES_128)
                    .withLane(0, firstHalf)
                    .withLane(1, secondHalf)
                    .reinterpretAsBytes()
                    .convertShape(VectorOperators.ZERO_EXTEND_B2I, intSpecies, 0)
                    .reinterpretAsInts();
            tempResult = HASH_COEF.mul(dataAsInts).reduceLanes(VectorOperators.ADD);
        } else {
            // bitSize == 256
            var intSpecies = IntVector.SPECIES_256;
            var dataAsIntsFirst = LongVector.zero(LongVector.SPECIES_128)
                    .withLane(0, firstHalf)
                    .reinterpretAsBytes()
                    .convertShape(VectorOperators.ZERO_EXTEND_B2I, intSpecies, 0)
                    .reinterpretAsInts();
            var dataAsIntsSecond = LongVector.zero(LongVector.SPECIES_128)
                    .withLane(0, secondHalf)
                    .reinterpretAsBytes()
                    .convertShape(VectorOperators.ZERO_EXTEND_B2I, intSpecies, 0)
                    .reinterpretAsInts();
            var computedFirst = HASH_COEF.mul(dataAsIntsFirst);
            var computedSecond = HASH_COEF.mul(dataAsIntsSecond);
            int partCoef = 31 * 31 * 31 * 31 * 31 * 31 * 31 * 31;
            tempResult = computedFirst.mul(partCoef).add(computedSecond).reduceLanes(VectorOperators.ADD);
        }
        // Need to divide result by 31**(16 - index) in integer arithmetic
        // tempResult = result * 31**(16 - index) (mod 2**32)
        // result = tempResult * 31**(2**31 - 16 + index) (mod 2**32)
        return tempResult * HASH_INVERSION_COEF[length & (HASH_INVERSION_COEF.length - 1)];
    }

    /**
     * 0 <= length <= 16
     */
    public static int indexOf(long firstHalf, long secondHalf, int length, int ch, int fromIndex) {
        if (!StringLatin1.canEncode(ch)) {
            return -1;
        }
        fromIndex = Math.min(Math.max(0, fromIndex), length);
        var data = LongVector.zero(LongVector.SPECIES_128)
                .withLane(0, firstHalf)
                .withLane(1, secondHalf)
                .reinterpretAsBytes();
        int i = data.eq((byte)ch)
                .and(BYTE_INDEX_VECTOR.compare(VectorOperators.GE, (byte)fromIndex))
                .firstTrue();
        return i < length ? i : -1;
    }

    /**
     * 0 <= length <= 16
     */
    public static int lastIndexOf(long firstHalf, long secondHalf, int length, int ch, int fromIndex) {
        if (!StringLatin1.canEncode(ch)) {
            return -1;
        }
        int limit = Math.min(Math.max(0, fromIndex + 1), length);
        var data = LongVector.zero(LongVector.SPECIES_128)
                .withLane(0, firstHalf)
                .withLane(1, secondHalf)
                .reinterpretAsBytes();
        int i = data.eq((byte)ch)
                .and(BYTE_INDEX_VECTOR.compare(VectorOperators.LT, (byte)limit))
                .lastTrue();
        return i;
    }

    /**
     * 0 <= index <= index + len <= 16
     */
    public static InlineString newString(long firstHalf, long secondHalf, int index, int len) {
        var data = LongVector.zero(LongVector.SPECIES_128)
                .withLane(0, firstHalf)
                .withLane(1, secondHalf)
                .reinterpretAsBytes()
                .rearrange(BYTE_SLICE[index & (BYTE_SLICE.length - 1)])
                .blend((byte)0, BYTE_INDEX_VECTOR.compare(VectorOperators.GE, (byte)len))
                .reinterpretAsLongs();
        return new InlineString(InlineString.SMALL_STRING_VALUE, len, data.lane(0), data.lane(1));
    }

    /**
     * Concat 2 compressed strings into a compressed string
     *
     * <p> length1 + length2 <= 16
     */
    public static InlineString concat(InlineString s1, InlineString s2) {
        var data1 = LongVector.zero(LongVector.SPECIES_128)
                .withLane(0, s1.firstHalf())
                .withLane(1, s1.secondHalf())
                .reinterpretAsBytes();
        var data2 = LongVector.zero(LongVector.SPECIES_128)
                .withLane(0, s2.firstHalf())
                .withLane(1, s2.secondHalf())
                .reinterpretAsBytes();
        var data = data1.or(data2.rearrange(BYTE_SLICE[-s1.length() & (BYTE_SLICE.length - 1)]))
                .reinterpretAsLongs();
        return new InlineString(InlineString.SMALL_STRING_VALUE, s1.length() + s2.length(), data.lane(0), data.lane(1));
    }

    public static InlineString replace(long firstHalf, long secondHalf, int length, char oldChar, char newChar) {
        if (StringLatin1.canEncode(oldChar)) {
            var data = LongVector.zero(LongVector.SPECIES_128)
                    .withLane(0, firstHalf)
                    .withLane(1, secondHalf)
                    .reinterpretAsBytes();
            var mask = data.compare(VectorOperators.EQ, (byte)oldChar)
                    .and(BYTE_INDEX_VECTOR.compare(VectorOperators.LT, length));
            if (StringLatin1.canEncode(newChar)) {
                var dataAsLongs = data.blend((byte)newChar, mask)
                        .reinterpretAsLongs();
                return new InlineString(InlineString.SMALL_STRING_VALUE, length, dataAsLongs.lane(0), dataAsLongs.lane(1));
            } else {
                if (mask.anyTrue()) {
                    byte[] buf = StringUTF16.newBytesFor(length);
                    var dataFirst = LongVector.zero(LongVector.SPECIES_128)
                            .withLane(0, firstHalf)
                            .reinterpretAsBytes()
                            .convertShape(VectorOperators.ZERO_EXTEND_B2S, ShortVector.SPECIES_128, 0)
                            .reinterpretAsShorts();
                    var maskFirst = dataFirst.compare(VectorOperators.EQ, (short)oldChar);
                    if (length > Long.BYTES) {
                        var dataSecond = LongVector.zero(LongVector.SPECIES_128)
                                .withLane(0, secondHalf)
                                .reinterpretAsBytes()
                                .convertShape(VectorOperators.ZERO_EXTEND_B2S, ShortVector.SPECIES_128, 0)
                                .reinterpretAsShorts();
                        var maskSecond = dataSecond.compare(VectorOperators.EQ, (short)oldChar)
                                .and(SHORT_INDEX_VECTOR_128.compare(VectorOperators.LT, (short)(length - Long.BYTES)));
                        var inflatedSecond = dataSecond.blend((short)newChar, maskSecond)
                                .reinterpretAsLongs();
                        decompress(inflatedSecond.lane(0), inflatedSecond.lane(1),
                                buf, Long.BYTES * Short.BYTES, (length - Long.BYTES) * Short.BYTES);
                    } else {
                        maskFirst = maskFirst.and(SHORT_INDEX_VECTOR_128.compare(VectorOperators.LT, (short)length));
                    }
                    var inflatedFirst = dataFirst.blend((short)newChar, maskFirst);
                    decompress(inflatedFirst.lane(0), inflatedFirst.lane(1), buf, 0, length);
                    return new InlineString(buf, length, Utils.UTF16, 0);
                }
            }
        }
        return new InlineString(InlineString.SMALL_STRING_VALUE, length, firstHalf, secondHalf);
    }

    public static InlineString trim(long firstHalf, long secondHalf) {
        var data = LongVector.zero(LongVector.SPECIES_128)
                .withLane(0, firstHalf)
                .withLane(1, secondHalf)
                .reinterpretAsBytes();
        var mask = data.compare(VectorOperators.UNSIGNED_GT, (byte)' ');
        int st = mask.firstTrue();
        if (st == ByteVector.SPECIES_128.length()) {
            return InlineString.EMPTY_STRING;
        }
        int len = mask.lastTrue() + 1;
        data = data.rearrange(BYTE_SLICE[st & (BYTE_SLICE.length - 1)])
                .blend((byte)0, BYTE_INDEX_VECTOR.compare(VectorOperators.GE, (byte)(len - st)));
        var dataAsLongs = data.reinterpretAsLongs();
        return new InlineString(InlineString.SMALL_STRING_VALUE, len - st, dataAsLongs.lane(0), dataAsLongs.lane(1));
    }

    public static InlineString strip(long firstHalf, long secondHalf, int length) {
        int left = indexOfNonWhitespace(firstHalf, secondHalf, length);
        if (left == length) {
            return InlineString.EMPTY_STRING;
        }
        int right = lastIndexOfNonWhitespace(firstHalf, secondHalf, length);
        boolean ifChanged = (left > 0) || (right < length);
        return ifChanged
                ? newString(firstHalf, secondHalf, left, right - left)
                : new InlineString(InlineString.SMALL_STRING_VALUE, length, firstHalf, secondHalf);
    }

    public static InlineString stripLeading(long firstHalf, long secondHalf, int length) {
        int left = indexOfNonWhitespace(firstHalf, secondHalf, length);
        return (left != 0)
                ? newString(firstHalf, secondHalf, left, length - left)
                : new InlineString(InlineString.SMALL_STRING_VALUE, length, firstHalf, secondHalf);
    }

    public static InlineString stripTrailing(long firstHalf, long secondHalf, int length) {
        int right = lastIndexOfNonWhitespace(firstHalf, secondHalf, length);
        return (right != length)
                ? newString(firstHalf, secondHalf, 0, right)
                : new InlineString(InlineString.SMALL_STRING_VALUE, length, firstHalf, secondHalf);
    }

    public static Stream<InlineString.ref> lines(long firstHalf, long secondHalf, int length) {
        return StreamSupport.stream(LinesSpliterator.spliterator(firstHalf, secondHalf, length), false);
    }

    public static Spliterator.OfInt charSpliterator(long firstHalf, long secondHalf, int length) {
        return new CharsSpliterator(firstHalf, secondHalf, 0, length);
    }

    public static int indexOfNonWhitespace(long firstHalf, long secondHalf, int length) {
        long temp = firstHalf;
        int i = 0;
        for (; i < Math.min(Long.BYTES, length); i++) {
            if (!Character.isWhitespace(Byte.toUnsignedInt((byte)temp))) {
                return i;
            }
            temp >>>= Byte.SIZE;
        }
        temp = secondHalf;
        for (; i < length; i++) {
            if (!Character.isWhitespace(Byte.toUnsignedInt((byte)temp))) {
                return i;
            }
            temp >>>= Byte.SIZE;
        }
        return length;
    }

    public static int lastIndexOfNonWhitespace(long firstHalf, long secondHalf, int length) {
        long temp = Long.reverseBytes(secondHalf);
        int i = length;
        temp >>>= (Long.SIZE * 2 - length * Byte.SIZE);
        for (; i > Long.BYTES; i--) {
            if (!Character.isWhitespace(Byte.toUnsignedInt((byte)temp))) {
                return i;
            }
            temp >>>= Byte.SIZE;
        }
        temp = Long.reverseBytes(firstHalf);
        temp >>>= (Long.SIZE - Math.min(Long.BYTES, length) * Byte.SIZE);
        for (; i > 0; i--) {
            if (!Character.isWhitespace(Byte.toUnsignedInt((byte)temp))) {
                return i;
            }
            temp >>>= Byte.SIZE;
        }
        return 0;
    }

    static boolean compressible(int length, byte coder) {
        return COMPRESSED_STRINGS && coder == Utils.LATIN1 && length <= COMPRESS_THRESHOLD;
    }

    static boolean compressible(int length) {
        return compressible(length, Utils.LATIN1);
    }

    static SmallStringByteData compress(byte[] value) {
        return compress(value, 0, value.length);
    }

    /**
     * Caller needs to ensure:
     *
     * 0 <= offset <= offset + length <= value.length
     */
    static SmallStringByteData compress(byte[] value, int offset, int length) {
        final long firstHalf, secondHalf;
        // index >= 16
        if (length >= Long.BYTES * 2) {
            firstHalf = UNSAFE.getLong(value, offset + Unsafe.ARRAY_BYTE_BASE_OFFSET);
            secondHalf = UNSAFE.getLong(value, offset + Long.BYTES + Unsafe.ARRAY_BYTE_BASE_OFFSET);
        } else {
            long temp = 0;
            int tempOffset = offset + length;
            if ((length & Byte.BYTES) != 0) {
                tempOffset--;
                temp = Byte.toUnsignedLong(UNSAFE.getByte(value, tempOffset + Unsafe.ARRAY_BYTE_BASE_OFFSET));
            }
            if ((length & Short.BYTES) != 0) {
                tempOffset -= Short.BYTES;
                temp = (temp << Short.SIZE) | Short.toUnsignedLong(UNSAFE.getShort(value, tempOffset + Unsafe.ARRAY_BYTE_BASE_OFFSET));
            }
            if ((length & Integer.BYTES) != 0) {
                tempOffset -= Integer.BYTES;
                temp = (temp << Integer.SIZE) | Integer.toUnsignedLong(UNSAFE.getInt(value, tempOffset + Unsafe.ARRAY_BYTE_BASE_OFFSET));
            }
            if ((length & Long.BYTES) != 0) {
                firstHalf = UNSAFE.getLong(value, offset + Unsafe.ARRAY_BYTE_BASE_OFFSET);
                secondHalf = temp;
            } else {
                firstHalf = temp;
                secondHalf = 0;
            }
        }
        return new SmallStringByteData(firstHalf, secondHalf);
    }

    /**
     * Caller needs to ensure:
     *
     * 0 <= offset <= 0ffset + length <= dst.length
     */
    static void decompress(long firstHalf, long secondHalf, byte[] dst, int offset, int length) {
        // index >= 16
        if (length >= Long.BYTES * 2) {
            UNSAFE.putLong(dst, offset + Unsafe.ARRAY_BYTE_BASE_OFFSET, firstHalf);
            UNSAFE.putLong(dst, offset + Long.BYTES + Unsafe.ARRAY_BYTE_BASE_OFFSET, secondHalf);
        } else {
            long temp = firstHalf;
            int tempOffset = offset;
            if ((length & Long.BYTES) != 0) {
                UNSAFE.putLong(dst, tempOffset + Unsafe.ARRAY_BYTE_BASE_OFFSET, temp);
                temp = secondHalf;
                tempOffset += Long.BYTES;
            }
            if ((length & Integer.BYTES) != 0) {
                UNSAFE.putInt(dst, tempOffset + Unsafe.ARRAY_BYTE_BASE_OFFSET, (int)temp);
                temp >>>= Integer.SIZE;
                tempOffset += Integer.BYTES;
            }
            if ((length & Short.BYTES) != 0) {
                UNSAFE.putShort(dst, tempOffset + Unsafe.ARRAY_BYTE_BASE_OFFSET, (short)temp);
                temp >>>= Short.SIZE;
                tempOffset += Short.BYTES;
            }
            if ((length & Byte.BYTES) != 0) {
                UNSAFE.putByte(dst, tempOffset + Unsafe.ARRAY_BYTE_BASE_OFFSET, (byte)temp);
            }
        }
    }

    static byte[] decompress(long firstHalf, long secondHalf, int length) {
        byte[] result = StringConcatHelper.newArray(length, Utils.LATIN1);
        decompress(firstHalf, secondHalf, result, 0, length);
        return result;
    }

    /**
     * Caller needs to ensure:
     *
     * 0 <= offset <= offset + length <= value.length
     */
    static SmallStringCharData compress(char[] value, int offset, int length) {
        offset *= Short.BYTES;
        final long firstQuarter, secondQuarter, thirdQuarter, fourthQuarter;
        // index >= 16
        if (length >= Long.BYTES * 2) {
            firstQuarter = UNSAFE.getLong(value, Unsafe.ARRAY_CHAR_BASE_OFFSET + offset);
            secondQuarter = UNSAFE.getLong(value, Unsafe.ARRAY_CHAR_BASE_OFFSET + offset + Long.BYTES);
            thirdQuarter = UNSAFE.getLong(value, Unsafe.ARRAY_CHAR_BASE_OFFSET + offset + Long.BYTES * 2);
            fourthQuarter = UNSAFE.getLong(value, Unsafe.ARRAY_CHAR_BASE_OFFSET + offset + Long.BYTES * 3);
        } else {
            long temp = 0;
            int tempOffset = offset + length * Short.BYTES;
            if ((length & Byte.BYTES) != 0) {
                tempOffset -= Short.BYTES;
                temp = Short.toUnsignedLong(UNSAFE.getShort(value, Unsafe.ARRAY_CHAR_BASE_OFFSET + tempOffset));
            }
            if ((length & Short.BYTES) != 0) {
                tempOffset -= Integer.BYTES;
                temp = (temp << Integer.SIZE) | Integer.toUnsignedLong(UNSAFE.getInt(value, Unsafe.ARRAY_CHAR_BASE_OFFSET + tempOffset));
            }
            final long first, second;
            if ((length & Integer.BYTES) != 0) {
                tempOffset -= Long.BYTES;
                first = UNSAFE.getLong(value, Unsafe.ARRAY_CHAR_BASE_OFFSET + tempOffset);
                second = temp;
            } else {
                first = temp;
                second = 0;
            }
            if ((length & Long.BYTES) != 0) {
                firstQuarter = UNSAFE.getLong(value, Unsafe.ARRAY_CHAR_BASE_OFFSET + offset);
                secondQuarter = UNSAFE.getLong(value, Unsafe.ARRAY_CHAR_BASE_OFFSET + offset + Long.BYTES);
                thirdQuarter = first;
                fourthQuarter = second;
            } else {
                firstQuarter = first;
                secondQuarter = second;
                thirdQuarter = 0;
                fourthQuarter = 0;
            }
        }
        return new SmallStringCharData(firstQuarter, secondQuarter, thirdQuarter, fourthQuarter);
    }

    /**
     * Caller needs to ensure:
     *
     * 0 <= offset <= offset + length <= dst.length
     */
    static void decompress(SmallStringCharData value, char[] dst, int offset, int length) {
        offset *= Short.BYTES;
        // index >= 16
        if (length >= Long.BYTES * 2) {
            UNSAFE.putLong(dst, Unsafe.ARRAY_CHAR_BASE_OFFSET + offset, value.firstQuarter());
            UNSAFE.putLong(dst, Unsafe.ARRAY_CHAR_BASE_OFFSET + offset + Long.BYTES, value.secondQuarter());
            UNSAFE.putLong(dst, Unsafe.ARRAY_CHAR_BASE_OFFSET + offset + Long.BYTES * 2, value.thirdQuarter());
            UNSAFE.putLong(dst, Unsafe.ARRAY_CHAR_BASE_OFFSET + offset + Long.BYTES * 3, value.thirdQuarter());
        } else {
            int tempOffset = offset;
            long first = value.firstQuarter(), second = value.secondQuarter();
            if ((length & Long.BYTES) != 0) {
                UNSAFE.putLong(dst, Unsafe.ARRAY_CHAR_BASE_OFFSET + tempOffset, first);
                UNSAFE.putLong(dst, Unsafe.ARRAY_CHAR_BASE_OFFSET + tempOffset + Long.BYTES, second);
                tempOffset += Long.BYTES * 2;
                first = value.thirdQuarter();
                second = value.fourthQuarter();
            }
            long temp = first;
            if ((length & Integer.BYTES) != 0) {
                UNSAFE.putLong(dst, Unsafe.ARRAY_CHAR_BASE_OFFSET + tempOffset, temp);
                tempOffset += Long.BYTES;
                temp = second;
            }
            if ((length & Short.BYTES) != 0) {
                UNSAFE.putInt(dst, Unsafe.ARRAY_CHAR_BASE_OFFSET + tempOffset, (int)temp);
                tempOffset += Integer.BYTES;
                temp >>>= Integer.SIZE;
            }
            if ((length & Byte.BYTES) != 0) {
                UNSAFE.putShort(dst, Unsafe.ARRAY_CHAR_BASE_OFFSET + tempOffset, (short)temp);
            }
        }
    }

    static void inflate(long firstHalf, long secondHalf, byte[] dst, int dstOff, int len) {
        len *= Short.BYTES;
        dstOff *= Short.BYTES;
        var dataFirst = LongVector.zero(LongVector.SPECIES_128)
                .withLane(0, firstHalf)
                .reinterpretAsBytes()
                .convertShape(VectorOperators.ZERO_EXTEND_B2S, ShortVector.SPECIES_128, 0)
                .reinterpretAsLongs();
        decompress(dataFirst.lane(0), dataFirst.lane(1), dst, dstOff, len);
        len -= Long.BYTES * Short.BYTES;
        if (len > 0) {
            var dataSecond = LongVector.zero(LongVector.SPECIES_128)
                    .withLane(0, secondHalf)
                    .reinterpretAsBytes()
                    .convertShape(VectorOperators.ZERO_EXTEND_B2S, ShortVector.SPECIES_128, 0)
                    .reinterpretAsLongs();
            decompress(dataSecond.lane(0), dataSecond.lane(1), dst, dstOff + Long.BYTES * Short.BYTES, len);
        }
    }

    private static long getCharHelper(long firstHalf, long secondHalf, int index) {
        long chosenHalf = (index & Long.BYTES) != 0 ? secondHalf : firstHalf;
        return chosenHalf >>> ((index & (Long.BYTES - 1)) * Byte.SIZE);
    }

    private static final class LinesSpliterator implements Spliterator<InlineString.ref> {
        private long firstHalf, secondHalf;
        private int index;        // current index, modified on advance/split
        private final int fence;  // one past last index

        private LinesSpliterator(long firstHalf, long secondHalf, int start, int length) {
            this.firstHalf = firstHalf;
            this.secondHalf = secondHalf;
            this.index = start;
            this.fence = start + length;
        }

        private int indexOfLineSeparator(int start) {
            long temp = getCharHelper(firstHalf, secondHalf, start);
            int current = start;
            for (; current < Math.min(fence, Long.BYTES + (start & Long.BYTES)); current++, temp >>>= Byte.SIZE) {
                int ch = Byte.toUnsignedInt((byte)temp);
                if (ch == '\n' || ch == '\r') {
                    return current;
                }
            }
            temp = secondHalf;
            for (; current < fence; current++, temp >>>= Byte.SIZE) {
                int ch = Byte.toUnsignedInt((byte)temp);
                if (ch == '\n' || ch == '\r') {
                    return current;
                }
            }
            return fence;
        }

        private int skipLineSeparator(int start) {
            if (start < fence) {
                long temp = getCharHelper(firstHalf, secondHalf, start);
                if (Byte.toUnsignedInt((byte)temp) == '\r') {
                    int next = start + 1;
                    if (next < fence) {
                        if (next == Long.BYTES) {
                            temp = secondHalf;
                        } else {
                            temp >>>= Byte.SIZE;
                        }
                        if (Byte.toUnsignedInt((byte)temp) == '\n') {
                            return next + 1;
                        }
                    }
                }
            }
            return fence;
        }

        private InlineString next() {
            int start = index;
            int end = indexOfLineSeparator(start);
            index = skipLineSeparator(end);
            return newString(firstHalf, secondHalf, start, end - start);
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
                return new LinesSpliterator(firstHalf, secondHalf, start, mid - start);
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

        static LinesSpliterator spliterator(long firstHalf, long secondHalf, int length) {
            return new LinesSpliterator(firstHalf, secondHalf, 0, length);
        }
    }

    private static class CharsSpliterator implements Spliterator.OfInt {
        long firstHalf, secondHalf;
        int index;
        int fence;

        CharsSpliterator(long firstHalf, long secondHalf, int index, int fence) {
            this.firstHalf = firstHalf;
            this.secondHalf = secondHalf;
            this.index = index;
            this.fence = fence;
        }

        @Override
        public OfInt trySplit() {
            int lo = index;
            int mid = (lo + fence) >>> 1;
            return mid > lo
                    ? new CharsSpliterator(firstHalf, secondHalf, lo, index = mid):
                    null;
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            Objects.requireNonNull(action);
            if (index < fence) {
                long temp = getCharHelper(firstHalf, secondHalf, index++);
                action.accept(Byte.toUnsignedInt((byte)temp));
                return true;
            }
            return false;
        }

        @Override
        public void forEachRemaining(IntConsumer action) {
            Objects.requireNonNull(action);
            long temp = getCharHelper(firstHalf, secondHalf, index);
            int start = index;
            for (; index < Math.min(fence, Long.BYTES + (start & Long.BYTES)); index++, temp >>>= Byte.SIZE) {
                action.accept(Byte.toUnsignedInt((byte)temp));
            }
            temp = secondHalf;
            for (; index < fence; index++, temp >>>= Byte.SIZE) {
                action.accept(Byte.toUnsignedInt((byte)temp));
            }
        }

        @Override
        public long estimateSize() {
            return fence - index;
        }

        @Override
        public int characteristics() {
            return Spliterator.IMMUTABLE | Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
        }
    }
}
