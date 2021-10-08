package io.github.merykitty.inlinestring;

import io.github.merykitty.inlinestring.internal.SmallStringByteData;
import io.github.merykitty.inlinestring.internal.SmallStringCharData;
import io.github.merykitty.inlinestring.internal.Utils;
import jdk.incubator.vector.*;
import jdk.internal.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

import static io.github.merykitty.inlinestring.internal.Helper.*;
import static io.github.merykitty.inlinestring.internal.Utils.COMPRESSED_STRINGS;

class StringCompressed {
    /**
     * A Latin1 string with less than this character is compressed.
     */
    static final int COMPRESS_THRESHOLD = Long.BYTES * 2;

    private static final VarHandle BYTE_ARRAY_AS_LONGS = MethodHandles.byteArrayViewVarHandle(Long.TYPE.arrayType(), ByteOrder.nativeOrder()).withInvokeExactBehavior();
    private static final VarHandle BYTE_ARRAY_AS_INTS = MethodHandles.byteArrayViewVarHandle(Integer.TYPE.arrayType(), ByteOrder.nativeOrder()).withInvokeExactBehavior();
    private static final VarHandle BYTE_ARRAY_AS_SHORTS = MethodHandles.byteArrayViewVarHandle(Short.TYPE.arrayType(), ByteOrder.nativeOrder()).withInvokeExactBehavior();
    private static final VarHandle BYTE_ARRAY_AS_BYTES = MethodHandles.arrayElementVarHandle(Byte.TYPE.arrayType()).withInvokeExactBehavior();
    static final Unsafe UNSAFE = Unsafe.getUnsafe();

    static char charAt(long firstHalf, long secondHalf, int length, int index) {
        InlineString.checkIndex(index, length);
        return (char)codePointAt(firstHalf, secondHalf, index);
    }

    static int codePointAt(long firstHalf, long secondHalf, int index) {
        long chosenHalf = ((index & Long.BYTES) != 0) ? secondHalf : firstHalf;
        return (int)((chosenHalf >> ((index & (Long.BYTES - 1)) * Byte.SIZE)) & 0xff);
    }

    /**
     * Caller needs to perform range checks beforehand
     *
     * 0 <= srcBegin <= srcEnd <= length <= 16
     * 0 <= dstBegin <= dstBegin + (srcEnd - srcBegin) <= dst.length
     */
    static void getChars(long firstHalf, long secondHalf, int srcBegin, int srcEnd, char[] dst, int dstBegin) {
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
    static int compareToLatin1(InlineString s1, InlineString s2) {
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
    static int compareToUTF16(InlineString s1, InlineString s2) {
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
    static boolean regionMatchesLatin1(InlineString s1, int offset1, InlineString s2, int offset2, int len) {
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
    static boolean regionMatchesUTF16(InlineString s1, int offset1, InlineString s2, int offset2, int len) {
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
    static int hashCode(long firstHalf, long secondHalf, int length) {
        final int tempResult;
        if (IntVector.SPECIES_PREFERRED.length() >= StringCompressed.COMPRESS_THRESHOLD) {
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
    static int indexOf(long firstHalf, long secondHalf, int length, int ch, int fromIndex) {
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
    static int lastIndexOf(long firstHalf, long secondHalf, int length, int ch, int fromIndex) {
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
    static InlineString newString(long firstHalf, long secondHalf, int index, int len) {
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
     * <p>
     * length1 + length2 <= 16
     */
    static InlineString concat(InlineString s1, InlineString s2) {
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

    /**
     * 0 <= length
     */
    static InlineString replace(long firstHalf, long secondHalf, int length, char oldChar, char newChar) {
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
}
