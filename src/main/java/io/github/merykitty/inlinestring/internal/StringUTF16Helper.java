package io.github.merykitty.inlinestring.internal;

import java.lang.foreign.MemorySegment.Scope;
import java.lang.ref.Reference;
import java.nio.ByteOrder;
import jdk.incubator.vector.*;

public class StringUTF16Helper {

    public static boolean equals(long address1, Scope scope1,
                                 long address2, Scope scope2, int len) {
        var species = ShortVector.SPECIES_PREFERRED;
        int loopBound = species.loopBound(len);
        for (int i = 0; i < loopBound; i += species.length()) {
            var data1 = ShortVector.fromMemorySegment(species, StringUTF16.GLOBAL,
                    address1 + i * CHAR_SIZE,
                    ByteOrder.nativeOrder());
            Reference.reachabilityFence(scope1);
            var data2 = ShortVector.fromMemorySegment(species, StringUTF16.GLOBAL,
                    address2 + i * CHAR_SIZE,
                    ByteOrder.nativeOrder());
            Reference.reachabilityFence(scope2);
            var diff = data1.compare(VectorOperators.EQ, data2);
            if (!diff.allTrue()) {
                return false;
            }
        }
        int eleLeft = len & (species.length() - 1);
        if (eleLeft == 0) {
            return true;
        }
        var mask = species.indexInRange(0, eleLeft);
        var data1 = ShortVector.fromMemorySegment(species, StringUTF16.GLOBAL,
                address1 + loopBound * CHAR_SIZE,
                ByteOrder.nativeOrder(), mask);
        Reference.reachabilityFence(scope1);
        var data2 = ShortVector.fromMemorySegment(species, StringUTF16.GLOBAL,
                address2 + loopBound * CHAR_SIZE,
                ByteOrder.nativeOrder(), mask);
        Reference.reachabilityFence(scope2);
        var diff = data1.compare(VectorOperators.EQ, data2);
        
        return diff.allTrue();
    }

    public static int compareToUTF16(long address1, int length1, Scope scope1,
                                     long address2, int length2, Scope scope2) {
        var shortSpecies = StringUTF16.INT_PREFERRED_LENGTH_SHORT_SPECIES;
        var intSpecies = IntVector.SPECIES_PREFERRED;
        int len = Math.min(length1, length2);
        int loopBound = shortSpecies.loopBound(len);
        for (int i = 0; i < loopBound; i += shortSpecies.length()) {
            var data1 = ShortVector.fromMemorySegment(shortSpecies, StringUTF16.GLOBAL,
                            address1 + i * CHAR_SIZE,
                            ByteOrder.nativeOrder())
                    .convertShape(VectorOperators.ZERO_EXTEND_S2I, intSpecies, 0)
                    .reinterpretAsInts();
            Reference.reachabilityFence(scope1);
            var data2 = ShortVector.fromMemorySegment(shortSpecies, StringUTF16.GLOBAL,
                            address2 + i * CHAR_SIZE,
                            ByteOrder.nativeOrder())
                    .convertShape(VectorOperators.ZERO_EXTEND_S2I, intSpecies, 0)
                    .reinterpretAsInts();
            Reference.reachabilityFence(scope2);
            var diff = data1.lanewise(VectorOperators.SUB, data2);
            int firstDiff = diff.reduceLanes(VectorOperators.FIRST_NONZERO);
            if (firstDiff != 0) {
                return firstDiff;
            }
        }
        int eleLeft = len & (shortSpecies.length() - 1);
        if (eleLeft == 0) {
            return length1 - length2;
        }
        var mask = shortSpecies.indexInRange(0, eleLeft);
        var data1 = ShortVector.fromMemorySegment(shortSpecies, StringUTF16.GLOBAL,
                        address1 + loopBound * CHAR_SIZE,
                        ByteOrder.nativeOrder(), mask)
                .convertShape(VectorOperators.ZERO_EXTEND_S2I, intSpecies, 0)
                .reinterpretAsInts();
        Reference.reachabilityFence(scope1);
        var data2 = ShortVector.fromMemorySegment(shortSpecies, StringUTF16.GLOBAL,
                        address2 + loopBound * CHAR_SIZE,
                        ByteOrder.nativeOrder(), mask)
                .convertShape(VectorOperators.ZERO_EXTEND_S2I, intSpecies, 0)
                .reinterpretAsInts();
        Reference.reachabilityFence(scope2);
        var diff = data1.lanewise(VectorOperators.SUB, data2);
        int firstDiff = diff.reduceLanes(VectorOperators.FIRST_NONZERO);
        if (firstDiff != 0) {
            return firstDiff;
        }
        return length1 - length2;
    }

    public static int hashCodeNoUnrolled(long address, int length, Scope scope) {
        var shortSpecies = StringUTF16.INT_PREFERRED_LENGTH_SHORT_SPECIES;
        var intSpecies = IntVector.SPECIES_PREFERRED;
        int loopStart = length & (shortSpecies.length() - 1);
        IntVector accumulator;
        if (loopStart == 0) {
            accumulator = ShortVector.fromMemorySegment(shortSpecies, StringUTF16.GLOBAL,
                            address,
                            ByteOrder.nativeOrder())
                    .convertShape(VectorOperators.ZERO_EXTEND_S2I, intSpecies, 0)
                    .reinterpretAsInts();
            Reference.reachabilityFence(scope);
            loopStart = shortSpecies.length();
        } else {
            short bound = (short)((shortSpecies.length() - 1) - loopStart);
            var mask = StringUTF16.INT_PREFERRED_LENGTH_SHORT_IOTA.compare(VectorOperators.GT, bound);
            accumulator = ShortVector.fromMemorySegment(shortSpecies, StringUTF16.GLOBAL,
                            address + (loopStart - shortSpecies.length()) * CHAR_SIZE,
                            ByteOrder.nativeOrder(), mask)
                    .convertShape(VectorOperators.ZERO_EXTEND_S2I, intSpecies, 0)
                    .reinterpretAsInts();
            Reference.reachabilityFence(scope);
        }
        for (int i = loopStart; i < length; i += shortSpecies.length()) {
            var data = ShortVector.fromMemorySegment(shortSpecies, StringUTF16.GLOBAL,
                            address + i * CHAR_SIZE,
                            ByteOrder.nativeOrder())
                    .convertShape(VectorOperators.ZERO_EXTEND_S2I, intSpecies, 0)
                    .reinterpretAsInts();
            Reference.reachabilityFence(scope);
            accumulator = accumulator.mul(HASH_ACCUMULATION_COEF_1).add(data);
        }
        return accumulator.mul(HASH_COEF).reduceLanes(VectorOperators.ADD);
    }

    public static int hashCodeUnrolled(long address, int length, Scope scope) {
        var shortSpecies = StringUTF16.INT_PREFERRED_LENGTH_SHORT_SPECIES;
        var intSpecies = IntVector.SPECIES_PREFERRED;
        int mainLoopStart = length & (shortSpecies.length() * HASH_UNROLLED_FACTOR - 1);

        int preLoopStart = mainLoopStart & (shortSpecies.length() - 1);
        IntVector accumulator3;
        if (preLoopStart == 0) {
            accumulator3 = IntVector.zero(intSpecies);
        } else {
            short bound = (short)((shortSpecies.length() - 1) - preLoopStart);
            var mask = StringUTF16.INT_PREFERRED_LENGTH_SHORT_IOTA.compare(VectorOperators.GT, bound);
            accumulator3 = ShortVector.fromMemorySegment(shortSpecies, StringUTF16.GLOBAL,
                            address + (preLoopStart - shortSpecies.length()) * CHAR_SIZE,
                            ByteOrder.nativeOrder(), mask)
                    .convertShape(VectorOperators.ZERO_EXTEND_S2I, intSpecies, 0)
                    .reinterpretAsInts();
            Reference.reachabilityFence(scope);
        }
        for (int i = preLoopStart; i < mainLoopStart; i += shortSpecies.length()) {
            var data = ShortVector.fromMemorySegment(shortSpecies, StringUTF16.GLOBAL,
                            address + i * CHAR_SIZE,
                            ByteOrder.nativeOrder())
                    .convertShape(VectorOperators.ZERO_EXTEND_S2I, intSpecies, 0)
                    .reinterpretAsInts();
            Reference.reachabilityFence(scope);
            accumulator3 = accumulator3.mul(HASH_ACCUMULATION_COEF_1).add(data);
        }

        var accumulator0 = IntVector.zero(intSpecies);
        var accumulator1 = IntVector.zero(intSpecies);
        var accumulator2 = IntVector.zero(intSpecies);
        for (int i = mainLoopStart; i < length; i += shortSpecies.length() * HASH_UNROLLED_FACTOR) {
            var data0 = ShortVector.fromMemorySegment(shortSpecies, StringUTF16.GLOBAL,
                            address + i * CHAR_SIZE,
                            ByteOrder.nativeOrder())
                    .convertShape(VectorOperators.ZERO_EXTEND_S2I, intSpecies, 0)
                    .reinterpretAsInts();
            var data1 = ShortVector.fromMemorySegment(shortSpecies, StringUTF16.GLOBAL,
                            address + (i + shortSpecies.length()) * CHAR_SIZE,
                            ByteOrder.nativeOrder())
                    .convertShape(VectorOperators.ZERO_EXTEND_S2I, intSpecies, 0)
                    .reinterpretAsInts();
            var data2 = ShortVector.fromMemorySegment(shortSpecies, StringUTF16.GLOBAL,
                            address + (i + shortSpecies.length() * 2L) * CHAR_SIZE,
                            ByteOrder.nativeOrder())
                    .convertShape(VectorOperators.ZERO_EXTEND_S2I, intSpecies, 0)
                    .reinterpretAsInts();
            var data3 = ShortVector.fromMemorySegment(shortSpecies, StringUTF16.GLOBAL,
                            address + (i + shortSpecies.length() * 3L) * CHAR_SIZE,
                            ByteOrder.nativeOrder())
                    .convertShape(VectorOperators.ZERO_EXTEND_S2I, intSpecies, 0)
                    .reinterpretAsInts();
            Reference.reachabilityFence(scope);
            accumulator0 = accumulator0.mul(HASH_LOOP_ITERATION_COEF).add(data0);
            accumulator1 = accumulator0.mul(HASH_LOOP_ITERATION_COEF).add(data1);
            accumulator2 = accumulator0.mul(HASH_LOOP_ITERATION_COEF).add(data2);
            accumulator3 = accumulator0.mul(HASH_LOOP_ITERATION_COEF).add(data3);
        }
        return accumulator0.mul(HASH_ACCUMULATION_COEF_3)
                .add(accumulator1.mul(HASH_ACCUMULATION_COEF_2))
                .add(accumulator2.mul(HASH_ACCUMULATION_COEF_1)
                        .add(accumulator3))
                .mul(HASH_COEF)
                .reduceLanes(VectorOperators.ADD);
    }

    public static int indexOfBMP(long address, int length, Scope scope, short ch) {
        var species = ShortVector.SPECIES_PREFERRED;
        int loopBound = species.loopBound(length);
        for (int i = 0; i < loopBound; i += species.length()) {
            var data = ShortVector.fromMemorySegment(species, StringUTF16.GLOBAL,
                    address + i * CHAR_SIZE,
                    ByteOrder.nativeOrder());
            Reference.reachabilityFence(scope);
            var found = data.compare(VectorOperators.EQ, ch);
            if (found.anyTrue()) {
                return i + found.firstTrue();
            }
        }
        int eleLeft = length & (species.length() - 1);
        if (eleLeft == 0) {
            return -1;
        }
        var mask = species.indexInRange(0, eleLeft);
        var data = ShortVector.fromMemorySegment(species, StringUTF16.GLOBAL,
                address + loopBound * CHAR_SIZE,
                ByteOrder.nativeOrder(), mask);
        Reference.reachabilityFence(scope);
        var found = data.compare(VectorOperators.EQ, ch, mask);
        if (found.anyTrue()) {
            return loopBound + found.firstTrue();
        }
        return -1;
    }

    public static int indexOfSupplementary(long address, int length, Scope scope, short hi, short lo) {
        var species = ShortVector.SPECIES_PREFERRED;
        long hiFoundTestMask = species.length() == 64 ? -1L : (-1L << species.length()) - 1;
        int loopBound = species.loopBound(length);
        long lastHiFound = 0;
        for (int i = 0; i < loopBound; i += species.length()) {
            var data = ShortVector.fromMemorySegment(species, StringUTF16.GLOBAL,
                    address + i * CHAR_SIZE,
                    ByteOrder.nativeOrder());
            Reference.reachabilityFence(scope);
            long hiFound = data.compare(VectorOperators.EQ, hi).toLong();
            long hiFoundAligned = ((hiFound << 1) | (lastHiFound >>> (species.length() - 1)));
            if ((hiFoundAligned & hiFoundTestMask) == 0) {
                lastHiFound = hiFound;
                continue;
            }
            long loFound = data.compare(VectorOperators.EQ, lo).toLong();
            long found = hiFoundAligned & loFound;
            if (found != 0) {
                return i + Long.numberOfTrailingZeros(found) - 1;
            }
        }
        int eleLeft = length & (species.length() - 1);
        if (eleLeft == 0) {
            return -1;
        }
        var mask = species.indexInRange(0, eleLeft);
        var data = ShortVector.fromMemorySegment(species, StringUTF16.GLOBAL,
                address + loopBound * CHAR_SIZE,
                ByteOrder.nativeOrder(), mask);
        Reference.reachabilityFence(scope);
        long hiFound = data.compare(VectorOperators.EQ, hi, mask).toLong();
        long hiFoundAligned = ((hiFound << 1) | (lastHiFound >>> (species.length() - 1)));
        if ((hiFoundAligned & hiFoundTestMask) == 0) {
            return -1;
        }
        long loFound = data.compare(VectorOperators.EQ, lo, mask).toLong();
        long found = hiFoundAligned & loFound;
        if (found != 0) {
            return loopBound + Long.numberOfTrailingZeros(found) - 1;
        }
        return -1;
    }

    public static int lastIndexOfBMP(long address, int length, Scope scope, short ch) {
        var species = ShortVector.SPECIES_PREFERRED;
        for (int i = length - species.length(); i >= 0; i -= species.length()) {
            var data = ShortVector.fromMemorySegment(species, StringUTF16.GLOBAL,
                    address + i * CHAR_SIZE,
                    ByteOrder.nativeOrder());
            Reference.reachabilityFence(scope);
            var found = data.compare(VectorOperators.EQ, ch);
            if (found.anyTrue()) {
                return i + found.lastTrue();
            }
        }
        int eleLeft = length & (species.length() - 1);
        if (eleLeft == 0) {
            return -1;
        }
        var mask = species.indexInRange(0, eleLeft);
        var data = ShortVector.fromMemorySegment(species, StringUTF16.GLOBAL,
                address,
                ByteOrder.nativeOrder(), mask);
        Reference.reachabilityFence(scope);
        var found = data.compare(VectorOperators.EQ, ch, mask);
        
        return found.lastTrue();
    }

    public static int lastIndexOfSupplementary(long address, int length, Scope scope, short hi, short lo) {
        var species = ShortVector.SPECIES_PREFERRED;
        long loFoundMask = species.length() == 64 ? -1L : (-1L << species.length()) - 1;
        long lastLoFound = 0;
        int i = length - species.length();
        for (; i >= 0; i -= species.length()) {
            var data = ShortVector.fromMemorySegment(species, StringUTF16.GLOBAL,
                    address + i * CHAR_SIZE,
                    ByteOrder.nativeOrder());
            Reference.reachabilityFence(scope);
            long loFound = data.compare(VectorOperators.EQ, lo).toLong();
            long loFoundAligned = (loFound >>> 1) | (lastLoFound << (species.length() - 1));
            if ((loFoundAligned & loFoundMask) == 0) {
                lastLoFound = loFound;
                continue;
            }
            long hiFound = data.compare(VectorOperators.EQ, hi).toLong();
            long found = hiFound & loFoundAligned;
            if (found != 0) {
                return i + (63 - Long.numberOfLeadingZeros(found));
            }
            lastLoFound = loFound;
        }
        if (i == -species.length()) {
            return -1;
        }
        var mask = species.indexInRange(i, species.length());
        var data = ShortVector.fromMemorySegment(species, StringUTF16.GLOBAL,
                address + i * CHAR_SIZE,
                ByteOrder.nativeOrder(), mask);
        Reference.reachabilityFence(scope);
        long loFound = data.compare(VectorOperators.EQ, lo, mask).toLong();
        long loFoundAligned = (loFound >>> 1) | (lastLoFound << (species.length() - 1));
        if ((loFoundAligned & loFoundMask) == 0) {
            return -1;
        }
        long hiFound = data.compare(VectorOperators.EQ, hi, mask).toLong();
        long found = hiFound & loFoundAligned;
        if (found != 0) {
            return i + (63 - Long.numberOfLeadingZeros(found));
        }
        
        return -1;
    }

    public static int indexOf(long address, int length, Scope scope, short c0, short c1,
                              long subFirstHalf, long subSecondHalf, Scope subScope, int subLength) {
        var species = ShortVector.SPECIES_PREFERRED;
        int loopBound = species.loopBound(length);
        long lastC0Found = 0;
        for (int i = 0; i < loopBound; i += species.length()) {
            var data = ShortVector.fromMemorySegment(species, StringUTF16.GLOBAL,
                    address + i * CHAR_SIZE,
                    ByteOrder.nativeOrder());
            Reference.reachabilityFence(scope);
            long c0Found = data.compare(VectorOperators.EQ, c0).toLong();
            long c0FoundAligned = (c0Found << 1) | (lastC0Found >>> (species.length() - 1));
            if (c0FoundAligned == 0) {
                lastC0Found = c0Found;
                continue;
            }
            long c1Found = data.compare(VectorOperators.EQ, c1).toLong();
            long found = c1Found & c0FoundAligned;
            while (found != 0) {
                int index = Long.numberOfTrailingZeros(found) + i - 1;
                long taddress = address + (index + 2) * CHAR_SIZE;
                if (subSecondHalf >= 0) {
                    if (StringCompressed.regionMatchesUTF16(subFirstHalf, subSecondHalf, 2, taddress, scope, subLength - 2)) {
                        return index;
                    }
                } else {
                    long subAddress = subFirstHalf + 2 * CHAR_SIZE;
                    if (StringUTF16.equals(taddress, scope, subAddress, subScope, subLength - 2)) {
                        return index;
                    }
                }
                found &= found - 1;
            }
        }
        int eleLeft = length & (species.length() - 1);
        if (eleLeft == 0) {
            return -1;
        }
        var mask = species.indexInRange(0, eleLeft);
        var data = ShortVector.fromMemorySegment(species, StringUTF16.GLOBAL,
                address + loopBound * CHAR_SIZE,
                ByteOrder.nativeOrder(), mask);
        Reference.reachabilityFence(scope);
        long c0Found = data.compare(VectorOperators.EQ, c0, mask).toLong();
        long c0FoundAligned = (c0Found << 1) | (lastC0Found >>> (species.length() - 1));
        if (c0FoundAligned == 0) {
            return -1;
        }
        long c1Found = data.compare(VectorOperators.EQ, c1, mask).toLong();
        long found = c1Found & c0FoundAligned;
        while (found != 0) {
            int index = Long.numberOfTrailingZeros(found) + loopBound - 1;
            long taddress = address + (index + 2) * CHAR_SIZE;
            if (subSecondHalf >= 0) {
                if (StringCompressed.regionMatchesUTF16(subFirstHalf, subSecondHalf, 2, taddress, scope, subLength - 2)) {
                    return index;
                }
            } else {
                long subAddress = subFirstHalf + 2 * CHAR_SIZE;
                if (StringUTF16.equals(taddress, scope, subAddress, subScope, subLength - 2)) {
                    return index;
                }
            }
            found &= found - 1;
        }
        return -1;
    }

    public static void copy(long dstAddress, Scope dstScope, long srcAddress, Scope srcScope, int length) {
        var species = ShortVector.SPECIES_PREFERRED;
        int loopBound = species.loopBound(length);
        for (int i = 0; i < loopBound; i += species.length()) {
            var data = ShortVector.fromMemorySegment(species, StringUTF16.GLOBAL,
                    srcAddress + i * CHAR_SIZE,
                    ByteOrder.nativeOrder());
            Reference.reachabilityFence(srcScope);
            data.intoMemorySegment(StringUTF16.GLOBAL,
                    dstAddress + i * CHAR_SIZE,
                    ByteOrder.nativeOrder());
            Reference.reachabilityFence(dstScope);
        }
        int eleLeft = length & (species.length() - 1);
        if (eleLeft == 0) {
            return;
        }
        var mask = species.indexInRange(0, eleLeft);
        var data = ShortVector.fromMemorySegment(species, StringUTF16.GLOBAL,
                srcAddress + loopBound * CHAR_SIZE,
                ByteOrder.nativeOrder(), mask);
        Reference.reachabilityFence(srcScope);
        data.intoMemorySegment(StringUTF16.GLOBAL,
                dstAddress + loopBound * CHAR_SIZE,
                ByteOrder.nativeOrder(), mask);
        Reference.reachabilityFence(dstScope);
    }

    public static void copy(long dstAddress, Scope dstScope, long srcFirstHalf, long srcSecondHalf, int length) {
        var shortSpecies = ShortVector.SPECIES_256;
        var data = LongVector.zero(LongVector.SPECIES_128)
                .withLane(0, srcFirstHalf)
                .withLane(1, srcSecondHalf)
                .reinterpretAsBytes()
                .convertShape(VectorOperators.ZERO_EXTEND_B2S, shortSpecies, 0);
        var mask = StringUTF16.SHORT_256_IOTA.compare(VectorOperators.LT, length);
        data.intoMemorySegment(StringUTF16.GLOBAL, dstAddress, ByteOrder.nativeOrder(), mask);
        Reference.reachabilityFence(dstScope);
    }

    public static void replace(long dstAddress, Scope dstScope, long srcAddress, Scope srcScope,
                               int length, short oldChar, short newChar) {
        var species = ShortVector.SPECIES_PREFERRED;
        int loopBound = species.loopBound(length);
        for (int i = 0; i < loopBound; i += species.length()) {
            var data = ShortVector.fromMemorySegment(species, StringUTF16.GLOBAL,
                    srcAddress + i * CHAR_SIZE,
                    ByteOrder.nativeOrder());
            Reference.reachabilityFence(srcScope);
            var found = data.compare(VectorOperators.EQ, oldChar);
            data.blend(newChar, found).intoMemorySegment(StringUTF16.GLOBAL,
                    dstAddress + i * CHAR_SIZE,
                    ByteOrder.nativeOrder());
            Reference.reachabilityFence(dstScope);
        }
        int eleLeft = length & (species.length() - 1);
        if (eleLeft == 0) {
            return;
        }
        var mask = species.indexInRange(0, eleLeft);
        var data = ShortVector.fromMemorySegment(species, StringUTF16.GLOBAL,
                srcAddress + loopBound * CHAR_SIZE,
                ByteOrder.nativeOrder(), mask);
        Reference.reachabilityFence(srcScope);
        var found = data.compare(VectorOperators.EQ, oldChar);
        data.blend(newChar, found).intoMemorySegment(StringUTF16.GLOBAL,
                dstAddress + loopBound * CHAR_SIZE,
                ByteOrder.nativeOrder(), mask);
        Reference.reachabilityFence(dstScope);
    }

    private static final long CHAR_SIZE = Character.BYTES;

    private static final int HASH_UNROLLED_FACTOR = 4;
    private static final int HASH_ACCUMULATION_COEF_1;
    private static final int HASH_ACCUMULATION_COEF_2;
    private static final int HASH_ACCUMULATION_COEF_3;
    private static final int HASH_LOOP_ITERATION_COEF;
    static {
        var intSpecies = IntVector.SPECIES_PREFERRED;
        int tempCoef = 1;
        for (int i = 0; i < intSpecies.length(); i++) {
            tempCoef *= 31;
        }
        HASH_ACCUMULATION_COEF_1 = tempCoef;
        HASH_ACCUMULATION_COEF_2 = tempCoef * tempCoef;
        HASH_ACCUMULATION_COEF_3 = tempCoef * tempCoef * tempCoef;
        HASH_LOOP_ITERATION_COEF = tempCoef * tempCoef * tempCoef * tempCoef;
    }

    private static final IntVector HASH_COEF;
    static {
        var intSpecies = IntVector.SPECIES_PREFERRED;
        int[] coefs = new int[intSpecies.length()];
        for (int i = coefs.length - 1, coef = 1; i >= 0; i--, coef *= 31) {
            coefs[i] = coef;
        }
        HASH_COEF = IntVector.fromArray(intSpecies, coefs, 0);
    }
}