package io.github.merykitty.inlinestring;

import io.github.merykitty.inlinestring.internal.SmallStringByteData;
import io.github.merykitty.inlinestring.internal.SmallStringCharData;
import io.github.merykitty.inlinestring.internal.Utils;
import static io.github.merykitty.inlinestring.internal.Helper.*;
import static io.github.merykitty.inlinestring.internal.Utils.COMPRESSED_STRINGS;

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

    public static final boolean COMPRESSED_STRINGS = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN &&
            VectorShape.preferredShape().vectorBitSize() >= 256;
    /**
     * A Latin1 string with less than this character is compressed.
     */
    public static final int COMPRESS_THRESHOLD = Long.BYTES * 2 - 1;
    public static final int LENGTH_LANE = Long.BYTES * 2 - 1;
    public static final int LENGTH_LANE_SECOND_HALF = Long.BYTES - 1;
    public static final int LENGTH_SHIFT = LENGTH_LANE_SECOND_HALF * Byte.SIZE;

    public static boolean compressible(long length) {
        return COMPRESSED_STRINGS && length <= COMPRESS_THRESHOLD;
    }

    public static int codePointAt(long firstHalf, long secondHalf, long index) {
        long chosenHalf = index >= Long.BYTES ? secondHalf : firstHalf;
        long value = chosenHalf >>> (index * Byte.SIZE);
        return Byte.toUnsignedInt((byte)value);
    }

    /**
     * 0 <= length <= 16
     */
    public static int hashCode(long firstHalf, long secondHalf) {
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
        return tempResult * HASH_INVERSION_COEF[length];
    }


}
