package io.github.merykitty.inlinestring;

import io.github.merykitty.inlinestring.internal.SmallStringByteData;
import io.github.merykitty.inlinestring.internal.SmallStringCharData;
import io.github.merykitty.inlinestring.internal.Utils;
import jdk.incubator.vector.*;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Objects;

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
    static final Unsafe UNSAFE;

    static {
        try {
            var unsafe = Unsafe.class.getDeclaredField("theUnsafe");
            unsafe.setAccessible(true);
            UNSAFE = (Unsafe) unsafe.get(null);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    static char charAt(long firstHalf, long secondHalf, int index, int length) {
        InlineString.checkIndex(index, length);
        if (index < 8) {
            return (char)((firstHalf >> (index * Byte.SIZE)) & 0xff);
        } else {
            return (char)((secondHalf >> (index * Byte.SIZE - Long.SIZE)) & 0xff);
        }
    }

    static void getChars(long firstHalf, long secondHalf, int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        var longSpecies = LongVector.SPECIES_128;
        var charSpecies = ShortVector.SPECIES_256;
        var data = LongVector.zero(longSpecies)
                .withLane(0, firstHalf)
                .withLane(1, secondHalf)
                .reinterpretAsBytes()
                .slice(srcBegin);
        int len = srcEnd - srcBegin;
        var inflatedAsLongs = data.convertShape(VectorOperators.ZERO_EXTEND_B2S, charSpecies, 0)
                .reinterpretAsLongs();
        decompress(dst, dstBegin, len, new SmallStringCharData(inflatedAsLongs.lane(0),
                inflatedAsLongs.lane(1), inflatedAsLongs.lane(2), inflatedAsLongs.lane(3)));
    }

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
            data2 = ByteVector.fromArray(ByteVector.SPECIES_128, s2.value(), 0);
        }
        var inflated1 = data1.convertShape(VectorOperators.ZERO_EXTEND_B2S, ShortVector.SPECIES_256, 0)
                .reinterpretAsShorts();
        var inflated2 = data2.convertShape(VectorOperators.ZERO_EXTEND_B2S, ShortVector.SPECIES_256, 0)
                .reinterpretAsShorts();
        var diff = inflated1.sub(inflated2);
        int i = diff.compare(VectorOperators.NE, (short) 0)
                .firstTrue();
        if (i < Math.min(s1.length(), s2.length())) {
            return diff.lane(i);
        } else {
            return s1.length() - s2.length();
        }
    }

    static int compareToUTF16(InlineString s1, InlineString s2) {
        var data1 = LongVector.zero(LongVector.SPECIES_128)
                .withLane(0, s1.firstHalf())
                .withLane(1, s1.secondHalf())
                .reinterpretAsBytes();
        int length = s2.length();
        ShortVector data2;
        if (length >= ShortVector.SPECIES_256.length()) {
            data2 = ByteVector.fromArray(ByteVector.SPECIES_256, s2.value(), 0)
                    .reinterpretAsShorts();
        } else {
            var data2Compressed = StringCompressed.compressInflated(s2.value(), 0, length);
            data2 = LongVector.zero(LongVector.SPECIES_256)
                    .withLane(0, data2Compressed.thirdQuarter())
                    .withLane(1, data2Compressed.fourthQuarter())
                    .unslice(2)
                    .withLane(0, data2Compressed.firstQuarter())
                    .withLane(1, data2Compressed.secondQuarter())
                    .reinterpretAsShorts();
        }
        if (IntVector.SPECIES_PREFERRED.vectorBitSize() >= 512) {
            var intSpecies = IntVector.SPECIES_512;
            var inflated1 = data1.convertShape(VectorOperators.ZERO_EXTEND_B2I, intSpecies, 0)
                    .reinterpretAsInts();
            var inflated2 = data2.convertShape(VectorOperators.ZERO_EXTEND_S2I, intSpecies, 0)
                    .reinterpretAsInts();
            var diff = inflated1.sub(inflated2);
            int i = diff.compare(VectorOperators.NE, 0)
                    .firstTrue();
            if (i < Math.min(s1.length(), s2.length())) {
                return diff.lane(i);
            } else {
                return s1.length() - s2.length();
            }
        } else {
            var intSpecies = IntVector.SPECIES_256;
            var inflated1 = data1.convertShape(VectorOperators.ZERO_EXTEND_B2I, intSpecies, 0)
                    .reinterpretAsInts();
            var inflated2 = data2.convertShape(VectorOperators.ZERO_EXTEND_S2I, intSpecies, 0)
                    .reinterpretAsInts();
            var diff = inflated1.sub(inflated2);
            int i = diff.compare(VectorOperators.NE, 0)
                    .firstTrue();
            if (i < intSpecies.length()) {
                if (i < Math.min(s1.length(), s2.length())) {
                    return diff.lane(i);
                } else {
                    return s1.length() - s2.length();
                }
            }
            inflated1 = data1.convertShape(VectorOperators.ZERO_EXTEND_B2I, intSpecies, 1)
                    .reinterpretAsInts();
            inflated2 = data2.convertShape(VectorOperators.ZERO_EXTEND_S2I, intSpecies, 1)
                    .reinterpretAsInts();
            diff = inflated1.sub(inflated2);
            i = diff.compare(VectorOperators.NE, 0)
                    .firstTrue();
            if (i + intSpecies.length() < Math.min(s1.length(), s2.length())) {
                return diff.lane(i);
            } else {
                return s1.length() - s2.length();
            }
        }
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

    static SmallStringByteData compress(byte[] value, int offset, int length) {
        Objects.checkFromIndexSize(offset, length, value.length);
        final long firstHalf, secondHalf;
        if (length == Long.BYTES * 2) {
            firstHalf = (long)BYTE_ARRAY_AS_LONGS.get(value, offset);
            secondHalf = (long)BYTE_ARRAY_AS_LONGS.get(value, offset + Long.BYTES);
        } else {
            long temp = 0;
            int tempOffset = offset + length;
            if ((length & Byte.BYTES) != 0) {
                tempOffset--;
                temp = Byte.toUnsignedLong((byte)BYTE_ARRAY_AS_BYTES.get(value, tempOffset));
            }
            if ((length & Short.BYTES) != 0) {
                tempOffset -= Short.BYTES;
                temp = (temp << Short.SIZE) | Short.toUnsignedLong((short)BYTE_ARRAY_AS_SHORTS.get(value, tempOffset));
            }
            if ((length & Integer.BYTES) != 0) {
                tempOffset -= Integer.BYTES;
                temp = (temp << Integer.SIZE) | Integer.toUnsignedLong((int)BYTE_ARRAY_AS_INTS.get(value, tempOffset));
            }
            if ((length & Long.BYTES) != 0) {
                firstHalf = (long)BYTE_ARRAY_AS_LONGS.get(value, offset);
                secondHalf = temp;
            } else {
                firstHalf = temp;
                secondHalf = 0;
            }
        }
        return new SmallStringByteData(firstHalf, secondHalf);
    }

    static void decompress(byte[] dst, int offset, int length, SmallStringByteData value) {
        Objects.checkFromIndexSize(offset, length, dst.length);
        if (length == Long.BYTES * 2) {
            BYTE_ARRAY_AS_LONGS.set(dst, offset, value.firstHalf());
            BYTE_ARRAY_AS_LONGS.set(dst, offset + Long.BYTES, value.secondHalf());
        } else {
            long temp = value.firstHalf();
            int tempOffset = offset;
            if ((length & Long.BYTES) != 0) {
                BYTE_ARRAY_AS_LONGS.set(dst, tempOffset, temp);
                temp = value.secondHalf();
                tempOffset += Long.BYTES;
            }
            if ((length & Integer.BYTES) != 0) {
                BYTE_ARRAY_AS_INTS.set(dst, tempOffset, (int)temp);
                temp >>>= Integer.SIZE;
                tempOffset += Integer.BYTES;
            }
            if ((length & Short.BYTES) != 0) {
                BYTE_ARRAY_AS_SHORTS.set(dst, tempOffset, (short)temp);
                temp >>>= Short.SIZE;
                tempOffset += Short.BYTES;
            }
            if ((length & Byte.BYTES) != 0) {
                BYTE_ARRAY_AS_BYTES.set(dst, tempOffset, (byte)temp);
            }
        }
    }

    static byte[] decompress(int length, SmallStringByteData value) {
        byte[] result = new byte[length];
        decompress(result, 0, length, value);
        return result;
    }

    static SmallStringCharData compress(char[] value, int offset, int length) {
        offset *= Short.BYTES;
        final long firstQuarter, secondQuarter, thirdQuarter, fourthQuarter;
        if (length == Long.BYTES * 2) {
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

    static void decompress(char[] dst, int offset, int length, SmallStringCharData value) {
        offset *= Short.BYTES;
        if (length == Long.BYTES * 2) {
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

    static SmallStringCharData compressInflated(byte[] value, int offset, int length) {
        offset *= Short.BYTES;
        final long firstQuarter, secondQuarter, thirdQuarter, fourthQuarter;
        if (length == Long.BYTES * 2) {
            firstQuarter = (long)BYTE_ARRAY_AS_LONGS.get(value, offset);
            secondQuarter = (long)BYTE_ARRAY_AS_LONGS.get(value, offset + Long.BYTES);
            thirdQuarter = (long)BYTE_ARRAY_AS_LONGS.get(value, offset + Long.BYTES * 2);
            fourthQuarter = (long)BYTE_ARRAY_AS_LONGS.get(value, offset + Long.BYTES * 3);
        } else {
            long temp = 0;
            int tempOffset = offset + length * Short.BYTES;
            if ((length & Byte.BYTES) != 0) {
                tempOffset -= Short.BYTES;
                temp = Short.toUnsignedLong((short)BYTE_ARRAY_AS_SHORTS.get(value, tempOffset));
            }
            if ((length & Short.BYTES) != 0) {
                tempOffset -= Integer.BYTES;
                temp = (temp << Integer.SIZE) | Integer.toUnsignedLong((int)BYTE_ARRAY_AS_INTS.get(value, tempOffset));
            }
            final long first, second;
            if ((length & Integer.BYTES) != 0) {
                tempOffset -= Long.BYTES;
                first = (long)BYTE_ARRAY_AS_LONGS.get(value, tempOffset);
                second = temp;
            } else {
                first = temp;
                second = 0;
            }
            if ((length & Long.BYTES) != 0) {
                firstQuarter = (long)BYTE_ARRAY_AS_LONGS.get(value, offset);
                secondQuarter = (long)BYTE_ARRAY_AS_LONGS.get(value, offset + Long.BYTES);
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
}
