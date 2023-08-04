package io.github.merykitty.inlinestring.internal;

import java.lang.foreign.MemorySegment.Scope;
import java.lang.ref.Reference;
import java.nio.ByteOrder;
import jdk.incubator.vector.*;

public class StringUTF16Helper {

    public static boolean equals(long address1, Scope scope1,
                                 long address2, Scope scope2, int len) {
define(`IMPL',
`var data1 = ShortVector.fromMemorySegment(species, StringUTF16.GLOBAL,
$3        address1 + $2 * CHAR_SIZE,
$3        ByteOrder.nativeOrder()ifelse($1, 1, `, mask', `'));
$3`'Reference.reachabilityFence(scope1);
$3`'var data2 = ShortVector.fromMemorySegment(species, StringUTF16.GLOBAL,
$3        address2 + $2 * CHAR_SIZE,
$3        ByteOrder.nativeOrder()ifelse($1, 1, `, mask', `'));
$3`'Reference.reachabilityFence(scope2);
$3`'var diff = data1.compare(VectorOperators.EQ, data2);
$3`'ifelse($1, 1, `', `if (!diff.allTrue()) {
$3    return false;
$3`'}')dnl
')dnl
        var species = ShortVector.SPECIES_PREFERRED;
        int loopBound = species.loopBound(len);
        for (int i = 0; i < loopBound; i += species.length()) {
            IMPL(0, i, `            ')
        }
        int eleLeft = len & (species.length() - 1);
        if (eleLeft == 0) {
            return true;
        }
        var mask = species.indexInRange(0, eleLeft);
        IMPL(1, loopBound, `        ')
        return diff.allTrue();
undefine(`IMPL')dnl
    }

    public static int compareToUTF16(long address1, int length1, Scope scope1,
                                     long address2, int length2, Scope scope2) {
define(`IMPL',
`var data1 = ShortVector.fromMemorySegment(shortSpecies, StringUTF16.GLOBAL,
$3                address1 + $2 * CHAR_SIZE,
$3                ByteOrder.nativeOrder()ifelse($1, 1, `, mask', `'))
$3        .convertShape(VectorOperators.ZERO_EXTEND_S2I, intSpecies, 0)
$3        .reinterpretAsInts();
$3`'Reference.reachabilityFence(scope1);
$3`'var data2 = ShortVector.fromMemorySegment(shortSpecies, StringUTF16.GLOBAL,
$3                address2 + $2 * CHAR_SIZE,
$3                ByteOrder.nativeOrder()ifelse($1, 1, `, mask', `'))
$3        .convertShape(VectorOperators.ZERO_EXTEND_S2I, intSpecies, 0)
$3        .reinterpretAsInts();
$3`'Reference.reachabilityFence(scope2);
$3`'var diff = data1.lanewise(VectorOperators.SUB, data2);
$3`'int firstDiff = diff.reduceLanes(VectorOperators.FIRST_NONZERO);
$3`'if (firstDiff != 0) {
$3    return firstDiff;
$3}dnl
')dnl
        var shortSpecies = StringUTF16.INT_PREFERRED_LENGTH_SHORT_SPECIES;
        var intSpecies = IntVector.SPECIES_PREFERRED;
        int len = Math.min(length1, length2);
        int loopBound = shortSpecies.loopBound(len);
        for (int i = 0; i < loopBound; i += shortSpecies.length()) {
            IMPL(0, i, `            ')
        }
        int eleLeft = len & (shortSpecies.length() - 1);
        if (eleLeft == 0) {
            return length1 - length2;
        }
        var mask = shortSpecies.indexInRange(0, eleLeft);
        IMPL(1, loopBound, `        ')
        return length1 - length2;
undefine(`IMPL')dnl
    }

define(`IMPL',
`$4 = ShortVector.fromMemorySegment(shortSpecies, StringUTF16.GLOBAL,
$3                address`'ifelse($2, 0, `', ` + $2 * CHAR_SIZE'),
$3                ByteOrder.nativeOrder()ifelse($1, 1, `, mask', `'))
$3        .convertShape(VectorOperators.ZERO_EXTEND_S2I, intSpecies, 0)
$3        .reinterpretAsInts();dnl
')dnl
dnl
    public static int hashCodeNoUnrolled(long address, int length, Scope scope) {
        var shortSpecies = StringUTF16.INT_PREFERRED_LENGTH_SHORT_SPECIES;
        var intSpecies = IntVector.SPECIES_PREFERRED;
        int loopStart = length & (shortSpecies.length() - 1);
        IntVector accumulator;
        if (loopStart == 0) {
            IMPL(0, 0, `            ', accumulator)
            Reference.reachabilityFence(scope);
            loopStart = shortSpecies.length();
        } else {
            short bound = (short)((shortSpecies.length() - 1) - loopStart);
            var mask = StringUTF16.INT_PREFERRED_LENGTH_SHORT_IOTA.compare(VectorOperators.GT, bound);
            IMPL(1, (loopStart - shortSpecies.length()), `            ', accumulator)
            Reference.reachabilityFence(scope);
        }
        for (int i = loopStart; i < length; i += shortSpecies.length()) {
            IMPL(0, i, `            ', `var data')
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
            IMPL(1, (preLoopStart - shortSpecies.length()), `            ', accumulator3)
            Reference.reachabilityFence(scope);
        }
        for (int i = preLoopStart; i < mainLoopStart; i += shortSpecies.length()) {
            IMPL(0, i, `            ', `var data')
            Reference.reachabilityFence(scope);
            accumulator3 = accumulator3.mul(HASH_ACCUMULATION_COEF_1).add(data);
        }

        var accumulator0 = IntVector.zero(intSpecies);
        var accumulator1 = IntVector.zero(intSpecies);
        var accumulator2 = IntVector.zero(intSpecies);
        for (int i = mainLoopStart; i < length; i += shortSpecies.length() * HASH_UNROLLED_FACTOR) {
            IMPL(0, i, `            ', `var data0')
            IMPL(0, (i + shortSpecies.length()), `            ', `var data1')
            IMPL(0, (i + shortSpecies.length() * 2L), `            ', `var data2')
            IMPL(0, (i + shortSpecies.length() * 3L), `            ', `var data3')
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
undefine(`IMPL')dnl

    public static int indexOfBMP(long address, int length, Scope scope, short ch) {
define(`IMPL',
`var data = ShortVector.fromMemorySegment(species, StringUTF16.GLOBAL,
$3        address + $2 * CHAR_SIZE,
$3        ByteOrder.nativeOrder()ifelse($1, 1, `, mask', `'));
$3`'Reference.reachabilityFence(scope);
$3`'var found = data.compare(VectorOperators.EQ, ch`'ifelse($1, 1, `, mask', `'));
$3`'if (found.anyTrue()) {
$3    return $2 + found.firstTrue();
$3`'}dnl
')dnl
        var species = ShortVector.SPECIES_PREFERRED;
        int loopBound = species.loopBound(length);
        for (int i = 0; i < loopBound; i += species.length()) {
            IMPL(0, i, `            ')
        }
        int eleLeft = length & (species.length() - 1);
        if (eleLeft == 0) {
            return -1;
        }
        var mask = species.indexInRange(0, eleLeft);
        IMPL(1, loopBound, `        ')
        return -1;
undefine(`IMPL')dnl
    }

    public static int indexOfSupplementary(long address, int length, Scope scope, short hi, short lo) {
define(`IMPL',
`var data = ShortVector.fromMemorySegment(species, StringUTF16.GLOBAL,
$3        address + $2 * CHAR_SIZE,
$3        ByteOrder.nativeOrder()ifelse($1, 1, `, mask', `'));
$3`'Reference.reachabilityFence(scope);
$3`'long hiFound = data.compare(VectorOperators.EQ, hi`'ifelse($1, 1, `, mask', `')).toLong();
$3`'long hiFoundAligned = ((hiFound << 1) | (lastHiFound >>> (species.length() - 1)));
$3`'if ((hiFoundAligned & hiFoundTestMask) == 0) {
$3    ifelse($1, 1, `return -1;', `lastHiFound = hiFound;
$3    continue;')
$3`'}
$3`'long loFound = data.compare(VectorOperators.EQ, lo`'ifelse($1, 1, `, mask', `')).toLong();
$3`'long found = hiFoundAligned & loFound;
$3`'if (found != 0) {
$3    return $2 + Long.numberOfTrailingZeros(found) - 1;
$3`'}dnl
')dnl
        var species = ShortVector.SPECIES_PREFERRED;
        long hiFoundTestMask = species.length() == 64 ? -1L : (-1L << species.length()) - 1;
        int loopBound = species.loopBound(length);
        long lastHiFound = 0;
        for (int i = 0; i < loopBound; i += species.length()) {
            IMPL(0, i, `            ')
        }
        int eleLeft = length & (species.length() - 1);
        if (eleLeft == 0) {
            return -1;
        }
        var mask = species.indexInRange(0, eleLeft);
        IMPL(1, loopBound, `        ')
        return -1;
undefine(`IMPL')dnl
    }

    public static int lastIndexOfBMP(long address, int length, Scope scope, short ch) {
define(`IMPL',
`var data = ShortVector.fromMemorySegment(species, StringUTF16.GLOBAL,
$3        address`'ifelse($2, 0, `', ` + $2 * CHAR_SIZE'),
$3        ByteOrder.nativeOrder()ifelse($1, 1, `, mask', `'));
$3`'Reference.reachabilityFence(scope);
$3`'var found = data.compare(VectorOperators.EQ, ch`'ifelse($1, 1, `, mask', `'));
$3`'ifelse($1, 1, `', `if (found.anyTrue()) {
$3    return $2 + found.lastTrue();
$3`'}')dnl
')dnl
        var species = ShortVector.SPECIES_PREFERRED;
        for (int i = length - species.length(); i >= 0; i -= species.length()) {
            IMPL(0, i, `            ')
        }
        int eleLeft = length & (species.length() - 1);
        if (eleLeft == 0) {
            return -1;
        }
        var mask = species.indexInRange(0, eleLeft);
        IMPL(1, 0, `        ')
        return found.lastTrue();
undefine(`IMPL')dnl
    }

    public static int lastIndexOfSupplementary(long address, int length, Scope scope, short hi, short lo) {
define(`IMPL',
`var data = ShortVector.fromMemorySegment(species, StringUTF16.GLOBAL,
$3        address + $2 * CHAR_SIZE,
$3        ByteOrder.nativeOrder()ifelse($1, 1, `, mask', `'));
$3`'Reference.reachabilityFence(scope);
$3`'long loFound = data.compare(VectorOperators.EQ, lo`'ifelse($1, 1, `, mask', `')).toLong();
$3`'long loFoundAligned = (loFound >>> 1) | (lastLoFound << (species.length() - 1));
$3`'if ((loFoundAligned & loFoundMask) == 0) {
$3    ifelse($1, 1, `return -1;', `lastLoFound = loFound;
$3    continue;')
$3`'}
$3`'long hiFound = data.compare(VectorOperators.EQ, hi`'ifelse($1, 1, `, mask', `')).toLong();
$3`'long found = hiFound & loFoundAligned;
$3`'if (found != 0) {
$3    return $2 + (63 - Long.numberOfLeadingZeros(found));
$3`'}
$3`'ifelse($1, 1, `', `lastLoFound = loFound;')dnl
')dnl
        var species = ShortVector.SPECIES_PREFERRED;
        long loFoundMask = species.length() == 64 ? -1L : (-1L << species.length()) - 1;
        long lastLoFound = 0;
        int i = length - species.length();
        for (; i >= 0; i -= species.length()) {
            IMPL(0, i, `            ')
        }
        if (i == -species.length()) {
            return -1;
        }
        var mask = species.indexInRange(i, species.length());
        IMPL(1, i, `        ')
        return -1;
undefine(`IMPL')dnl
    }

    public static int indexOf(long address, int length, Scope scope, short c0, short c1,
                              long subFirstHalf, long subSecondHalf, Scope subScope, int subLength) {
define(`IMPL',
`var data = ShortVector.fromMemorySegment(species, StringUTF16.GLOBAL,
$3        address + $2 * CHAR_SIZE,
$3        ByteOrder.nativeOrder()ifelse($1, 1, `, mask', `'));
$3`'Reference.reachabilityFence(scope);
$3`'long c0Found = data.compare(VectorOperators.EQ, c0`'ifelse($1, 1, `, mask', `')).toLong();
$3`'long c0FoundAligned = (c0Found << 1) | (lastC0Found >>> (species.length() - 1));
$3`'if (c0FoundAligned == 0) {
$3    ifelse($1, 1, `return -1;', `lastC0Found = c0Found;
$3    continue;')
$3`'}
$3`'long c1Found = data.compare(VectorOperators.EQ, c1`'ifelse($1, 1, `, mask', `')).toLong();
$3`'long found = c1Found & c0FoundAligned;
$3`'while (found != 0) {
$3    int index = Long.numberOfTrailingZeros(found) + $2 - 1;
$3    long taddress = address + (index + 2) * CHAR_SIZE;
$3    if (subSecondHalf >= 0) {
$3        if (StringCompressed.regionMatchesUTF16(subFirstHalf, subSecondHalf, 2, taddress, scope, subLength - 2)) {
$3            return index;
$3        }
$3    } else {
$3        long subAddress = subFirstHalf + 2 * CHAR_SIZE;
$3        if (StringUTF16.equals(taddress, scope, subAddress, subScope, subLength - 2)) {
$3            return index;
$3        }
$3    }
$3    found &= found - 1;
$3`'}dnl
')dnl
        var species = ShortVector.SPECIES_PREFERRED;
        int loopBound = species.loopBound(length);
        long lastC0Found = 0;
        for (int i = 0; i < loopBound; i += species.length()) {
            IMPL(0, i, `            ')
        }
        int eleLeft = length & (species.length() - 1);
        if (eleLeft == 0) {
            return -1;
        }
        var mask = species.indexInRange(0, eleLeft);
        IMPL(1, loopBound, `        ')
        return -1;
undefine(`IMPL')dnl
    }

    public static void copy(long dstAddress, Scope dstScope, long srcAddress, Scope srcScope, int length) {
define(`IMPL',
`var data = ShortVector.fromMemorySegment(species, StringUTF16.GLOBAL,
$3        srcAddress + $2 * CHAR_SIZE,
$3        ByteOrder.nativeOrder()ifelse($1, 1, `, mask', `'));
$3`'Reference.reachabilityFence(srcScope);
$3`'data.intoMemorySegment(StringUTF16.GLOBAL,
$3        dstAddress + $2 * CHAR_SIZE,
$3        ByteOrder.nativeOrder()ifelse($1, 1, `, mask', `'));
$3`'Reference.reachabilityFence(dstScope);dnl
')dnl
        var species = ShortVector.SPECIES_PREFERRED;
        int loopBound = species.loopBound(length);
        for (int i = 0; i < loopBound; i += species.length()) {
            IMPL(0, i, `            ')
        }
        int eleLeft = length & (species.length() - 1);
        if (eleLeft == 0) {
            return;
        }
        var mask = species.indexInRange(0, eleLeft);
        IMPL(1, loopBound, `        ')
undefine(`IMPL')dnl
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
define(`IMPL',
`var data = ShortVector.fromMemorySegment(species, StringUTF16.GLOBAL,
$3        srcAddress + $2 * CHAR_SIZE,
$3        ByteOrder.nativeOrder()ifelse($1, 1, `, mask', `'));
$3`'Reference.reachabilityFence(srcScope);
$3`'var found = data.compare(VectorOperators.EQ, oldChar);
$3`'data.blend(newChar, found).intoMemorySegment(StringUTF16.GLOBAL,
$3        dstAddress + $2 * CHAR_SIZE,
$3        ByteOrder.nativeOrder()ifelse($1, 1, `, mask', `'));
$3`'Reference.reachabilityFence(dstScope);dnl
')dnl
        var species = ShortVector.SPECIES_PREFERRED;
        int loopBound = species.loopBound(length);
        for (int i = 0; i < loopBound; i += species.length()) {
            IMPL(0, i, `            ')
        }
        int eleLeft = length & (species.length() - 1);
        if (eleLeft == 0) {
            return;
        }
        var mask = species.indexInRange(0, eleLeft);
        IMPL(1, loopBound, `        ')
undefine(`IMPL')dnl
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