package io.github.merykitty.inlinestring;

import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorOperators;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentScope;
import java.lang.foreign.ValueLayout;
import java.lang.ref.Reference;
import java.nio.ByteOrder;

class StringUTF16 {
    public static final MemorySegment GLOBAL =
            MemorySegment.ofAddress(0, Long.MAX_VALUE);

    public static final int SURROGATE_PREFIX_MASK = 0xFC00;
    public static final int SURROGATE_VALUE_MASK = 0x3FF;
    public static final int HIGH_SURROGATE_PREFIX = 0xD800;
    public static final int LOW_SURROGATE_PREFIX = 0xDC00;
    public static final int HIGH_SURROGATE_SHIFT = 10;
    public static final int COMPLEMENTARY_OFFSET = 0x10000;

    public static char charAt(long address, SegmentScope scope, long index) {
        char result = GLOBAL.get(ValueLayout.JAVA_CHAR_UNALIGNED,
                address + index * Character.BYTES);
        Reference.reachabilityFence(scope);
        return result;
    }

    public static int codePointAt(long address, long length, SegmentScope scope,
                                  long index) {
        char result = GLOBAL.get(ValueLayout.JAVA_CHAR_UNALIGNED,
                address + index * Character.BYTES);
        if (!Character.isHighSurrogate(result) || index == length - 1) {
            Reference.reachabilityFence(scope);
            return result;
        }

        char lowHalf = GLOBAL.get(ValueLayout.JAVA_CHAR_UNALIGNED,
                address + index * Character.BYTES + Character.BYTES);
        Reference.reachabilityFence(scope);
        if (!Character.isLowSurrogate(lowHalf)) {
            return result;
        }
        return Character.toCodePoint(result, lowHalf);
    }

    public static int codePointBefore(long address, SegmentScope scope,
                                      long index) {
        char result = GLOBAL.get(ValueLayout.JAVA_CHAR_UNALIGNED,
                address + index * Character.BYTES);
        if (!Character.isLowSurrogate(result) || index == 0) {
            Reference.reachabilityFence(scope);
            return result;
        }

        char highHalf = GLOBAL.get(ValueLayout.JAVA_CHAR_UNALIGNED,
                address + index * Character.BYTES - Character.BYTES);
        Reference.reachabilityFence(scope);
        if (!Character.isHighSurrogate(highHalf)) {
            return result;
        }
        return Character.toCodePoint(highHalf, result);
    }

    public static long codePointCount(long address, SegmentScope scope,
                                      long beginIndex, long endIndex) {
        var species = ShortVector.SPECIES_PREFERRED;
        ShortVector previous = ShortVector.zero(species);
        long result = 0;
        long offset = beginIndex;
        for (; offset < endIndex - (species.length() - 1);
                offset += species.length()) {
            var data = ShortVector.fromMemorySegment(species, GLOBAL,
                    address + offset, ByteOrder.nativeOrder());
            var shiftOne = previous.slice(species.length() - 1, data);
            var lowSurrogates = data.and((short)SURROGATE_PREFIX_MASK)
                    .compare(VectorOperators.EQ, (short)LOW_SURROGATE_PREFIX);
            var highSurrogates = shiftOne.and((short)SURROGATE_PREFIX_MASK)
                    .compare(VectorOperators.EQ, (short)HIGH_SURROGATE_PREFIX);
            var surrogatePairs = lowSurrogates.and(highSurrogates);
            result += (species.length() - surrogatePairs.trueCount());
            previous = data;
        }

        var mask = species.indexInRange(offset, endIndex);
        var data = ShortVector.fromMemorySegment(species, GLOBAL,
                address + offset, ByteOrder.nativeOrder(), mask);
        Reference.reachabilityFence(scope);

        var shiftOne = previous.slice(species.length() - 1, data);
        var lowSurrogates = data.and((short)SURROGATE_PREFIX_MASK)
                .compare(VectorOperators.EQ, (short)LOW_SURROGATE_PREFIX);
        var highSurrogates = shiftOne.and((short)SURROGATE_PREFIX_MASK)
                .compare(VectorOperators.EQ, (short)HIGH_SURROGATE_PREFIX);
        var surrogatePairs = lowSurrogates.and(highSurrogates);
        return (result + mask.trueCount() - surrogatePairs.trueCount());
    }

    public static long offsetByCodePoints(long address, long length,
                                          SegmentScope scope, long baseOffset,
                                          long codePointCount) {
        var species = ShortVector.SPECIES_PREFERRED;
        if (codePointCount >= 0) {
            long offset = baseOffset;
            ShortVector previous = ShortVector.zero(species);
            for (;; offset += species.length()) {
                if (codePointCount < species.length() ||
                        offset > length - (species.length() - 1)) {
                    break;
                }
            }
        }
    }
}
