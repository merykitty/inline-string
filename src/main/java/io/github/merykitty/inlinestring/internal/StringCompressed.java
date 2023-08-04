package io.github.merykitty.inlinestring.internal;

import io.github.merykitty.inlinestring.encoding.Unicode;
import java.lang.foreign.MemorySegment.Scope;
import java.lang.ref.Reference;
import java.nio.ByteOrder;

import jdk.incubator.vector.*;

import static io.github.merykitty.inlinestring.internal.StringUTF16.GLOBAL;

public class StringCompressed {
    /**
     * The lane index of the length field in a compressed string.
     */
    public static final int LENGTH_LANE = Long.BYTES * 2 - 1;

    /**
     * The lane index of the length field in a compressed string, count from
     * the second half.
     */
    public static final int LENGTH_LANE_SECOND_HALF = Long.BYTES - 1;

    /**
     * The secondHalf field needs to be right-shifted this amount to get the
     * length of the compressed string.
     */
    public static final int LENGTH_SHIFT = LENGTH_LANE_SECOND_HALF * Byte.SIZE;

    public static final long LENGTH_UNSET_MASK = (1L << LENGTH_SHIFT) - 1;

    public static char charAt(long firstHalf, long secondHalf, int index) {
        long chosenHalf = index >= Long.BYTES ? secondHalf : firstHalf;
        long value = chosenHalf >>> (index * Byte.SIZE);
        return (char)Byte.toUnsignedInt((byte)value);
    }

    public static int compareToCompressed(long firstHalf1, long secondHalf1, long firstHalf2, long secondHalf2) {
        // Fast path, compare the first character
        // If one is empty, then its first byte is zero, the result is still correct
        int c1 = charAt(firstHalf1, secondHalf2, 0);
        int c2 = charAt(firstHalf2, secondHalf2, 0);
        if (c1 != c2) {
            return c1 - c2;
        }

        var byteSpecies = LongVector.SPECIES_128;
        var shortSpecies = ShortVector.SPECIES_256;
        var data1 = LongVector.zero(byteSpecies)
                .withLane(0, firstHalf1)
                .withLane(1, secondHalf1)
                .reinterpretAsBytes()
                .convertShape(VectorOperators.ZERO_EXTEND_B2S, shortSpecies, 0)
                .reinterpretAsShorts();
        var data2 = LongVector.zero(byteSpecies)
                .withLane(0, firstHalf2)
                .withLane(1, secondHalf2)
                .reinterpretAsBytes()
                .convertShape(VectorOperators.ZERO_EXTEND_B2S, shortSpecies, 0)
                .reinterpretAsShorts();

        var diff = data1.lanewise(VectorOperators.SUB, data2);
        return diff.reduceLanes(VectorOperators.FIRST_NONZERO);
    }

    public static int compareToUTF16(long firstHalf1, long secondHalf1, long address2, int length2, Scope scope2) {
        // Fast path, compare the first character
        // If the first one is empty, then its first byte is zero, the result is still correct
        int c1 = charAt(firstHalf1, secondHalf1, 0);
        int c2 = StringUTF16.charAt(address2, scope2, 0);
        if (c1 != c2) {
            return c1 - c2;
        }

        int length1 = (int)(secondHalf1 >>> LENGTH_SHIFT);
        secondHalf1 &= LENGTH_UNSET_MASK;
        if (IntVector.SPECIES_PREFERRED.vectorBitSize() >= 512) {
            var byteSpecies = LongVector.SPECIES_128;
            var shortSpecies = ShortVector.SPECIES_256;
            var intSpecies = IntVector.SPECIES_512;
            var data1 = LongVector.zero(byteSpecies)
                    .withLane(0, firstHalf1)
                    .withLane(1, secondHalf1)
                    .reinterpretAsBytes()
                    .convertShape(VectorOperators.ZERO_EXTEND_B2I, intSpecies, 0)
                    .reinterpretAsInts();
            var mask2 = shortSpecies.indexInRange(0, length2);
            var data2 = ShortVector.fromMemorySegment(shortSpecies, GLOBAL,
                    address2, ByteOrder.nativeOrder(), mask2)
                    .convertShape(VectorOperators.ZERO_EXTEND_S2I, intSpecies, 0)
                    .reinterpretAsInts();
            Reference.reachabilityFence(scope2);

            var diff = data1.lanewise(VectorOperators.SUB, data2);
            int firstDiff = diff.reduceLanes(VectorOperators.FIRST_NONZERO);
            if (firstDiff != 0) {
                return firstDiff;
            }
            return length1 - length2;
        } else {
            var byteSpecies = LongVector.SPECIES_64;
            var shortSpecies = ShortVector.SPECIES_128;
            var intSpecies = IntVector.SPECIES_256;
            var data11 = LongVector.zero(byteSpecies)
                    .withLane(0, firstHalf1)
                    .reinterpretAsBytes()
                    .convertShape(VectorOperators.ZERO_EXTEND_B2I, intSpecies, 0)
                    .reinterpretAsInts();
            var mask21 = shortSpecies.indexInRange(0, length2);
            var data21 = ShortVector.fromMemorySegment(shortSpecies, GLOBAL,
                    address2, ByteOrder.nativeOrder(), mask21)
                    .convertShape(VectorOperators.ZERO_EXTEND_S2I, intSpecies, 0)
                    .reinterpretAsInts();
            Reference.reachabilityFence(scope2);

            var diff1 = data11.lanewise(VectorOperators.SUB, data21);
            int firstDiff1 = diff1.reduceLanes(VectorOperators.FIRST_NONZERO);
            if (firstDiff1 != 0) {
                return firstDiff1;
            }

            var data12 = LongVector.zero(byteSpecies)
                    .withLane(1, secondHalf1)
                    .reinterpretAsBytes()
                    .convertShape(VectorOperators.ZERO_EXTEND_B2I, intSpecies, 0)
                    .reinterpretAsInts();
            var mask22 = shortSpecies.indexInRange(shortSpecies.length(), length2);
            var data22 = ShortVector.fromMemorySegment(shortSpecies, GLOBAL,
                    address2 + shortSpecies.vectorByteSize(),
                    ByteOrder.nativeOrder(), mask22)
                    .convertShape(VectorOperators.ZERO_EXTEND_S2I, intSpecies, 0)
                    .reinterpretAsInts();
            Reference.reachabilityFence(scope2);

            var diff2 = data12.lanewise(VectorOperators.SUB, data22);
            int firstDiff2 = diff2.reduceLanes(VectorOperators.FIRST_NONZERO);
            if (firstDiff2 != 0) {
                return firstDiff2;
            }
            return length1 - length2;
        }
    }

    public static int compareToCICompressed(long firstHalf1, long secondHalf1, long firstHalf2, long secondHalf2) {
        var byteSpecies = LongVector.SPECIES_128;
        var shortSpecies = ShortVector.SPECIES_256;
        var data1 = LongVector.zero(byteSpecies)
                .withLane(0, firstHalf1)
                .withLane(1, secondHalf1)
                .reinterpretAsBytes()
                .convertShape(VectorOperators.ZERO_EXTEND_B2S, shortSpecies, 0)
                .reinterpretAsShorts();
        var data1UpperCaseMask = data1.lanewise(VectorOperators.SUB, 'A')
                .compare(VectorOperators.UNSIGNED_LE, 'Z' - 'A');
        data1 = data1.lanewise(VectorOperators.ADD, 'a' - 'A', data1UpperCaseMask);

        var data2 = LongVector.zero(byteSpecies)
                .withLane(0, firstHalf2)
                .withLane(1, secondHalf2)
                .reinterpretAsBytes()
                .convertShape(VectorOperators.ZERO_EXTEND_B2S, shortSpecies, 0)
                .reinterpretAsShorts();
        var data2UpperCaseMask = data2.lanewise(VectorOperators.SUB, 'A')
                .compare(VectorOperators.UNSIGNED_LE, 'Z' - 'A');
        data2 = data2.lanewise(VectorOperators.ADD, 'a' - 'A', data2UpperCaseMask);

        // Length cannot fall into the upper case range so we are safe
        var diff = data1.lanewise(VectorOperators.SUB, data2);
        return diff.reduceLanes(VectorOperators.FIRST_NONZERO);
    }

    public static boolean regionMatchesCompressed(long firstHalf1, long secondHalf1, int offset1,
                                                  long firstHalf2, long secondHalf2, int offset2, int len) {
        var data1 = LongVector.zero(LongVector.SPECIES_128)
                .withLane(0, firstHalf1)
                .withLane(1, secondHalf1)
                .reinterpretAsBytes()
                .compress(BYTE_IOTA.compare(VectorOperators.GE, offset1));

        var data2 = LongVector.zero(LongVector.SPECIES_128)
                .withLane(0, firstHalf2)
                .withLane(1, secondHalf2)
                .reinterpretAsBytes()
                .compress(BYTE_IOTA.compare(VectorOperators.GE, offset2));

        var diff = data1.compare(VectorOperators.NE, data2);
        return diff.firstTrue() >= len;
    }

    public static boolean regionMatchesUTF16(long firstHalf1, long secondHalf1, int offset1,
                                             long address2, Scope scope2, int len) {
        var shortSpecies = ShortVector.SPECIES_256;
        var data1 = LongVector.zero(LongVector.SPECIES_128)
                .withLane(0, firstHalf1)
                .withLane(1, secondHalf1)
                .reinterpretAsBytes()
                .compress(BYTE_IOTA.compare(VectorOperators.GE, offset1))
                .convertShape(VectorOperators.ZERO_EXTEND_B2S, shortSpecies, 0);
        var mask2 = shortSpecies.indexInRange(0, len);
        var data2 = ShortVector.fromMemorySegment(shortSpecies, GLOBAL,
                address2,
                ByteOrder.nativeOrder(), mask2);
        Reference.reachabilityFence(scope2);

        var diff = data1.compare(VectorOperators.NE, data2);
        return diff.firstTrue() >= len;
    }

    public static boolean regionMatchesCICompressed(long firstHalf1, long secondHalf1, int offset1,
                                                    long firstHalf2, long secondHalf2, int offset2, int len) {
        var data1 = LongVector.zero(LongVector.SPECIES_128)
                .withLane(0, firstHalf1)
                .withLane(1, secondHalf1)
                .reinterpretAsBytes()
                .compress(BYTE_IOTA.compare(VectorOperators.GE, offset1));
        var data1UpperCaseMask = data1.lanewise(VectorOperators.SUB, 'A')
                .compare(VectorOperators.UNSIGNED_LE, 'Z' - 'A');
        data1 = data1.lanewise(VectorOperators.ADD, 'a' - 'A', data1UpperCaseMask);

        var data2 = LongVector.zero(LongVector.SPECIES_128)
                .withLane(0, firstHalf2)
                .withLane(1, secondHalf2)
                .reinterpretAsBytes()
                .compress(BYTE_IOTA.compare(VectorOperators.GE, offset2));
        var data2UpperCaseMask = data2.lanewise(VectorOperators.SUB, 'A')
                .compare(VectorOperators.UNSIGNED_LE, 'Z' - 'A');
        data2 = data2.lanewise(VectorOperators.ADD, 'a' - 'A', data2UpperCaseMask);

        var diff = data1.compare(VectorOperators.NE, data2);
        return diff.firstTrue() >= len;
    }

    /**
     * 0 <= length <= 15
     */
    public static int hashCode(long firstHalf, long secondHalf) {
        int length = (int)(secondHalf >>> LENGTH_SHIFT);
        secondHalf &= LENGTH_UNSET_MASK;
        final int tempResult;
        if (IntVector.SPECIES_PREFERRED.vectorBitSize() >= 512) {
            // bitSize == 512
            var intSpecies = IntVector.SPECIES_512;
            var data = LongVector.zero(LongVector.SPECIES_128)
                    .withLane(0, firstHalf)
                    .withLane(1, secondHalf)
                    .reinterpretAsBytes()
                    .convertShape(VectorOperators.ZERO_EXTEND_B2I, intSpecies, 0)
                    .reinterpretAsInts();
            tempResult = HASH_COEF.mul(data).reduceLanes(VectorOperators.ADD);
        } else {
            // bitSize == 256
            var intSpecies = IntVector.SPECIES_256;
            var data1 = LongVector.zero(LongVector.SPECIES_128)
                    .withLane(0, firstHalf)
                    .reinterpretAsBytes()
                    .convertShape(VectorOperators.ZERO_EXTEND_B2I, intSpecies, 0)
                    .reinterpretAsInts();
            var data2 = LongVector.zero(LongVector.SPECIES_128)
                    .withLane(0, secondHalf)
                    .reinterpretAsBytes()
                    .convertShape(VectorOperators.ZERO_EXTEND_B2I, intSpecies, 0)
                    .reinterpretAsInts();
            var computedFirst = HASH_COEF.mul(data1);
            var computedSecond = HASH_COEF.mul(data2);
            int partCoef = 31 * 31 * 31 * 31 * 31 * 31 * 31 * 31;
            tempResult = computedFirst.mul(partCoef).add(computedSecond).reduceLanes(VectorOperators.ADD);
        }
        // Need to divide result by 31**(16 - index) in integer arithmetic
        // tempResult = result * 31**(16 - index) (mod 2**32)
        // result = tempResult * 31**(2**31 - 16 + index) (mod 2**32)
        return tempResult * HASH_INVERSION_COEF[length];
    }

    public static int indexOf(long firstHalf, long secondHalf, int length, byte ch, long foundMask) {
        var species = LongVector.SPECIES_128;
        var data = LongVector.zero(species)
                .withLane(0, firstHalf)
                .withLane(1, secondHalf)
                .reinterpretAsBytes();
        long found = data.compare(VectorOperators.EQ, ch).toLong();
        found &= foundMask;
        int result = Long.numberOfTrailingZeros(found);
        return result < length ? result : -1;
    }

    public static int lastIndexOf(long firstHalf, long secondHalf, byte ch, long foundMask) {
        var species = LongVector.SPECIES_128;
        var data = LongVector.zero(species)
                .withLane(0, firstHalf)
                .withLane(1, secondHalf)
                .reinterpretAsBytes();
        long found = data.compare(VectorOperators.EQ, ch).toLong();
        found &= foundMask;
        return (Long.SIZE - 1) - Long.numberOfLeadingZeros(found);
    }

    public static int indexOf(long firstHalf, long secondHalf, int length,
                              long subFirstHalf, long subSecondHalf, long foundMask) {
        var species = LongVector.SPECIES_128;
        var data = LongVector.zero(species)
                .withLane(0, firstHalf)
                .withLane(1, secondHalf)
                .reinterpretAsBytes();

        byte c0 = (byte)subFirstHalf;
        long firstFound = data.compare(VectorOperators.EQ, c0).toLong();
        firstFound &= foundMask;
        if (firstFound == 0) {
            return -1;
        }

        byte c1 = (byte)(subFirstHalf >>> Byte.SIZE);
        var secondFound = data.compare(VectorOperators.EQ, c1).toLong();
        long found = firstFound & (secondFound >>> 1);
        if (found == 0) {
            return -1;
        }

        // Do a regionMatches for the positions in which the first 2 characters match
        // only do bound check on exit to reduce overhead.
        int subLength = (int)(subSecondHalf >>> LENGTH_SHIFT);
        do {
            int index = Long.numberOfTrailingZeros(found);
            if (regionMatchesCompressed(firstHalf, secondHalf, index + 2,
                    subFirstHalf, subSecondHalf, 2, subLength - 2)) {
                return index + subLength <= length ? index : -1;
            }

            found &= (found - 1);
        } while (found != 0);

        return -1;
    }

    public static StringData substringBegin(long firstHalf, long secondHalf, int length, int beginIndex) {
        secondHalf &= LENGTH_UNSET_MASK;
        int newLength = length - beginIndex;
        var data = LongVector.zero(LongVector.SPECIES_128)
                .withLane(0, firstHalf)
                .withLane(1, secondHalf)
                .reinterpretAsBytes()
                .compress(BYTE_IOTA.compare(VectorOperators.GE, beginIndex))
                .reinterpretAsLongs();
        return new StringData(data.lane(0), data.lane(1) | ((long)newLength << LENGTH_SHIFT), null);
    }

    public static StringData substringBeginEnd(long firstHalf, long secondHalf, int beginIndex, int endIndex) {
        int newLength = endIndex - beginIndex;
        var mask = BYTE_IOTA.compare(VectorOperators.GE, beginIndex);
        mask = BYTE_IOTA.compare(VectorOperators.LT, endIndex, mask);
        var data = LongVector.zero(LongVector.SPECIES_128)
                .withLane(0, firstHalf)
                .withLane(1, secondHalf)
                .reinterpretAsBytes()
                .compress(mask)
                .reinterpretAsLongs();
        return new StringData(data.lane(0), data.lane(1) | ((long)newLength << LENGTH_SHIFT), null);
    }

    public static StringData concat(long firstHalf1, long secondHalf1,
                                    long firstHalf2, long secondHalf2, int length1, int length) {
        var data1 = LongVector.zero(LongVector.SPECIES_128)
                .withLane(0, firstHalf1)
                .withLane(1, secondHalf1)
                .reinterpretAsBytes();
        var data2 = LongVector.zero(LongVector.SPECIES_128)
                .withLane(0, firstHalf2)
                .withLane(1, secondHalf2)
                .reinterpretAsBytes()
                .expand(BYTE_IOTA.compare(VectorOperators.GE, (byte)length1));
        var data = data1.or(data2).withLane(LENGTH_LANE, (byte)length)
                .reinterpretAsLongs();
        return new StringData(data.lane(0), data.lane(1), null);
    }

    private static final IntVector HASH_COEF;
    static {
        var intSpecies = IntVector.SPECIES_PREFERRED.vectorBitSize() >= 512 ?
                IntVector.SPECIES_512 :
                IntVector.SPECIES_256;
        int[] coefs = new int[intSpecies.length()];
        for (int i = coefs.length - 1, coef = 1; i >= 0; i--, coef *= 31) {
            coefs[i] = coef;
        }
        HASH_COEF = IntVector.fromArray(intSpecies, coefs, 0);
    }

    private static final int[] HASH_INVERSION_COEF = new int[] {
            333062657,  1735007775, -2049333823,   895160927,
            1980184961, 1256191647,   287235393,   314362591,
            1155305729, 1454739231,  2147243201,  2140029791,
            1916414081, -720705633,  -867038143, -1108378657
    };

    private static final ByteVector BYTE_IOTA = ByteVector.zero(ByteVector.SPECIES_128)
            .addIndex(1);
}
