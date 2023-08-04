package io.github.merykitty.inlinestring.internal;

import io.github.merykitty.inlinestring.encoding.Unicode;
import jdk.incubator.vector.*;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySegment.Scope;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.ValueLayout;
import java.lang.ref.Reference;
import java.nio.ByteOrder;

public class StringUTF16 {
    public static final MemorySegment GLOBAL = MemorySegment.NULL.reinterpret(Long.MAX_VALUE);
    public static final VectorSpecies<Short> INT_PREFERRED_LENGTH_SHORT_SPECIES =
            VectorSpecies.of(short.class,
                    VectorShape.forBitSize(IntVector.SPECIES_PREFERRED
                            .length() * Character.BYTES));
    public static final ShortVector SHORT_PREFERRED_IOTA =
            ShortVector.zero(ShortVector.SPECIES_PREFERRED)
                    .addIndex(1);
    public static final ShortVector SHORT_256_IOTA =
            ShortVector.zero(ShortVector.SPECIES_256)
                    .addIndex(1);
    public static final ShortVector INT_PREFERRED_LENGTH_SHORT_IOTA =
            ShortVector.zero(INT_PREFERRED_LENGTH_SHORT_SPECIES)
                    .addIndex(1);

    public static final short SURROGATE_PREFIX_MASK = (short)0xFC00;
    public static final short SURROGATE_VALUE_MASK = 0x3FF;
    public static final short HIGH_SURROGATE_PREFIX = (short)0xD800;
    public static final short LOW_SURROGATE_PREFIX = (short)0xDC00;
    public static final short HIGH_SURROGATE_SHIFT = 10;
    public static final int COMPLEMENTARY_OFFSET = 0x10000;

    public static char charAt(long address, Scope scope, int index) {
        char result = GLOBAL.get(ValueLayout.JAVA_CHAR_UNALIGNED,
                address + (long)index * Character.BYTES);
        Reference.reachabilityFence(scope);
        return result;
    }

    public static int codePointAt(long address, int length, Scope scope,
                                  int index) {
        char result = GLOBAL.get(ValueLayout.JAVA_CHAR_UNALIGNED,
                address + index * CHAR_SIZE);
        Reference.reachabilityFence(scope);
        if (!Character.isHighSurrogate(result) || index == length - 1) {
            return result;
        }

        char lowHalf = GLOBAL.get(ValueLayout.JAVA_CHAR_UNALIGNED,
                address + index * CHAR_SIZE + Character.BYTES);
        Reference.reachabilityFence(scope);
        if (!Character.isLowSurrogate(lowHalf)) {
            return result;
        }
        return Character.toCodePoint(result, lowHalf);
    }

    public static int codePointBefore(long address, Scope scope,
                                      int index) {
        char result = GLOBAL.get(ValueLayout.JAVA_CHAR_UNALIGNED,
                address + index * CHAR_SIZE);
        Reference.reachabilityFence(scope);
        if (!Character.isLowSurrogate(result) || index == 0) {
            return result;
        }

        char highHalf = GLOBAL.get(ValueLayout.JAVA_CHAR_UNALIGNED,
                address + index * CHAR_SIZE - CHAR_SIZE);
        Reference.reachabilityFence(scope);
        if (!Character.isHighSurrogate(highHalf)) {
            return result;
        }
        return Character.toCodePoint(highHalf, result);
    }

    public static int codePointCount(long address, Scope scope,
                                     int beginIndex, int endIndex) {
        var species = ShortVector.SPECIES_PREFERRED;
        ShortVector previousMasked = ShortVector.zero(species);
        int result = 0;
        for (int offset = beginIndex; offset < endIndex; offset += species.length()) {
            var mask = species.indexInRange(offset, endIndex);
            var data = ShortVector.fromMemorySegment(species, GLOBAL,
                    address + offset * CHAR_SIZE, ByteOrder.nativeOrder(), mask);
            Reference.reachabilityFence(scope);

            var dataMasked = data.lanewise(VectorOperators.AND, SURROGATE_PREFIX_MASK);
            var prev1Masked = previousMasked.slice(species.length() - 1, dataMasked);
            var lowSurrogates = dataMasked.compare(VectorOperators.EQ, LOW_SURROGATE_PREFIX);
            var highSurrogates = prev1Masked.compare(VectorOperators.EQ, HIGH_SURROGATE_PREFIX);
            var surrogatePairs = lowSurrogates.and(highSurrogates);
            result += (mask.andNot(surrogatePairs).trueCount());
            previousMasked = dataMasked;
        }
        return result;
    }

    public static int offsetByCodePoints(long address, int length,
                                         Scope scope, int baseIndex,
                                         int codePointCount) {
        var species = ShortVector.SPECIES_PREFERRED;
        if (codePointCount >= 0) {
            ShortVector previousMasked = ShortVector.zero(species);
            for (int i = baseIndex; i < length; i += species.length()) {
                var mask = species.indexInRange(i, length);
                var dataMasked = ShortVector.fromMemorySegment(species, GLOBAL,
                        address + i * CHAR_SIZE,
                        ByteOrder.nativeOrder(), mask)
                        .lanewise(VectorOperators.AND, SURROGATE_PREFIX_MASK);
                Reference.reachabilityFence(scope);

                var prev1Masked = previousMasked.slice(species.length() - 1, dataMasked);
                var highSurrogates = prev1Masked.compare(VectorOperators.EQ, HIGH_SURROGATE_PREFIX);
                var lowSurrogates = dataMasked.compare(VectorOperators.EQ, LOW_SURROGATE_PREFIX);
                var surrogatesPairs = lowSurrogates.and(highSurrogates);
                var codePoints = mask.andNot(surrogatesPairs);

                var currentCount = codePoints.trueCount();
                if (codePointCount >= currentCount) {
                    codePointCount -= currentCount;
                    previousMasked = dataMasked;
                    continue;
                }

                long setLanes = codePoints.toLong();
                for (int j = 0; j < codePointCount; j++) {
                    setLanes &= (setLanes - 1);
                }
                return i + Long.numberOfTrailingZeros(setLanes);
            }
        } else {
            ShortVector nextMasked = ShortVector.zero(species);
            for (int i = baseIndex - species.length(); i > -species.length(); i -= species.length()) {
                var mask = species.indexInRange(i, length);
                var dataMasked = ShortVector.fromMemorySegment(species, GLOBAL,
                        address + i * CHAR_SIZE,
                        ByteOrder.nativeOrder(), mask)
                        .lanewise(VectorOperators.AND, SURROGATE_PREFIX_MASK);
                Reference.reachabilityFence(scope);

                var next1Masked = dataMasked.slice(1, nextMasked);
                var highSurrogates = dataMasked.compare(VectorOperators.EQ, HIGH_SURROGATE_PREFIX);
                var lowSurrogates = next1Masked.compare(VectorOperators.EQ, LOW_SURROGATE_PREFIX);
                var surrogatesPairs = lowSurrogates.and(highSurrogates);
                var codePoints = mask.andNot(surrogatesPairs);

                var currentCount = codePoints.trueCount();
                if (codePointCount + currentCount < 0) {
                    codePointCount += currentCount;
                    nextMasked = dataMasked;
                    continue;
                }

                long setLanes = Long.reverse(codePoints.toLong());
                for (int j = -1; j > codePointCount; j--) {
                    setLanes &= (setLanes - 1);
                }
                return i + species.length() - Long.numberOfTrailingZeros(setLanes);
            }
        }

        throw new IndexOutOfBoundsException("There are not " + codePointCount + " code points from " + baseIndex);
    }

    public static boolean equals(long address1, Scope scope1,
                                 long address2, Scope scope2, int len) {
        return StringUTF16Helper.equals(address1, scope1,
                address2, scope2, len);
    }

    public static int compareToUTF16(long address1, int length1, Scope scope1, long address2, int length2, Scope scope2) {
        // Fast path
        int c1 = charAt(address1, scope1, 0);
        int c2 = charAt(address2, scope2, 0);
        if (c1 != c2) {
            return c1 - c2;
        }

        return StringUTF16Helper.compareToUTF16(address1, length1, scope1, address2, length2, scope2);
    }

    public static int hashCode(long address, int length, Scope scope) {
        if (length < HASH_UNROLLED_THRESHOLD) {
            return StringUTF16Helper.hashCodeNoUnrolled(address, length, scope);
        } else {
            return StringUTF16Helper.hashCodeUnrolled(address, length, scope);
        }
    }

    public static int indexOf(long address, int length, Scope scope, int ch) {
        if (Character.isBmpCodePoint(ch)) {
            return StringUTF16Helper.indexOfBMP(address, length, scope, (short)ch);
        } else {
            if (!Character.isSupplementaryCodePoint(ch)) {
                return -1;
            }
            short hi = (short)Character.highSurrogate(ch);
            short lo = (short)Character.lowSurrogate(ch);
            return StringUTF16Helper.indexOfSupplementary(address, length, scope, hi, lo);
        }
    }

    public static int lastIndexOf(long address, int length, Scope scope, int ch) {
        if (Character.isBmpCodePoint(ch)) {
            return StringUTF16Helper.lastIndexOfBMP(address, length, scope, (short)ch);
        } else {
            if (!Character.isSupplementaryCodePoint(ch)) {
                return -1;
            }
            short hi = (short)Character.highSurrogate(ch);
            short lo = (short)Character.lowSurrogate(ch);
            return StringUTF16Helper.lastIndexOfSupplementary(address, length, scope, hi, lo);
        }
    }

    public static int indexOf(long address, int length, Scope scope, short c0, short c1,
                              long subFirstHalf, long subSecondHalf, Scope subScope, int subLength) {
        return StringUTF16Helper.indexOf(address, length, scope, c0, c1,
                subFirstHalf, subSecondHalf, subScope, subLength);
    }

    public static StringData substring(long address, Scope scope,
                                       int beginIndex, int newLength, SegmentAllocator allocator) {
        address += beginIndex * CHAR_SIZE;
        if (!StringData.compressible(newLength)) {
            var buffer = allocator.allocate(newLength * CHAR_SIZE);
            Scope newScope = buffer.scope();
            long newAddress = buffer.address();
            StringUTF16Helper.copy(newAddress, newScope, address, scope, newLength);
            return new StringData(newAddress, -newLength, newScope);
        }

        var species = ShortVector.SPECIES_256;
        var mask = species.indexInRange(0, newLength);
        var data = ShortVector.fromMemorySegment(species, GLOBAL, address, ByteOrder.nativeOrder(), mask);
        var nonAsciiMask = data.compare(VectorOperators.GT, Unicode.MAX_ASCII);
        if (nonAsciiMask.anyTrue()) {
            var buffer = allocator.allocate(newLength * CHAR_SIZE);
            data.intoMemorySegment(buffer, 0, ByteOrder.nativeOrder(), mask);
            return new StringData(buffer.address(), -newLength, buffer.scope());
        }

        var compressedData = data.convertShape(VectorOperators.S2B, ByteVector.SPECIES_128, 0)
                .reinterpretAsLongs();
        return new StringData(compressedData.lane(0),
                compressedData.lane(1) | ((long)newLength << StringCompressed.LENGTH_SHIFT),
                null);
    }

    public static StringData substringNoCopy(long address, Scope scope,
                                             int beginIndex, int newLength) {
        address += beginIndex * CHAR_SIZE;
        if (!StringData.compressible(newLength)) {
            return new StringData(address, -newLength, scope);
        }

        var species = ShortVector.SPECIES_256;
        var mask = species.indexInRange(0, newLength);
        var data = ShortVector.fromMemorySegment(species, GLOBAL, address, ByteOrder.nativeOrder(), mask);
        var nonAsciiMask = data.compare(VectorOperators.GT, Unicode.MAX_ASCII);
        if (nonAsciiMask.anyTrue()) {
            return new StringData(address, -newLength, scope);
        }

        var compressedData = data.convertShape(VectorOperators.S2B, ByteVector.SPECIES_128, 0)
                .reinterpretAsLongs();
        return new StringData(compressedData.lane(0),
                compressedData.lane(1) | ((long)newLength << StringCompressed.LENGTH_SHIFT),
                null);
    }

    private static final long CHAR_SIZE = 2;
    private static final int HASH_UNROLLED_THRESHOLD = IntVector.SPECIES_PREFERRED.length() * 16;
}
