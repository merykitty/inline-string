package io.github.merykitty.inlinestring.internal;

import io.github.merykitty.inlinestring.encoding.Unicode;

import java.lang.foreign.MemorySegment.Scope;
import java.lang.foreign.SegmentAllocator;
import java.lang.ref.Reference;
import java.nio.ByteOrder;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.LongVector;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorOperators;

public class StringReplace {
    public static StringData replaceCompressed(long firstHalf, long secondHalf,
                                               byte oldChar, short newChar, SegmentAllocator allocator) {
        byte length = (byte)(secondHalf >>> StringCompressed.LENGTH_SHIFT);
        long secondHalfFixed = secondHalf & StringCompressed.LENGTH_UNSET_MASK;
        var data = LongVector.zero(LongVector.SPECIES_128)
                .withLane(0, firstHalf)
                .withLane(1, secondHalfFixed)
                .reinterpretAsBytes();
        var found = data.compare(VectorOperators.EQ, oldChar);
        if (!found.anyTrue()) {
            return new StringData(firstHalf, secondHalf, null);
        }

        if (Integer.compareUnsigned(newChar, Unicode.MAX_ASCII) <= 0) {
            var dataAsLongs = data.blend((byte)newChar, found)
                    .withLane(StringCompressed.LENGTH_LANE, length)
                    .reinterpretAsLongs();
            return new StringData(dataAsLongs.lane(0), dataAsLongs.lane(1), null);
        }

        var shortSpecies = ShortVector.SPECIES_256;
        var buffer = allocator.allocate(length * (long)Character.BYTES);
        long address = buffer.address();
        var scope = buffer.scope();
        var mask = StringUTF16.SHORT_256_IOTA.compare(VectorOperators.LT, length);
        data.convertShape(VectorOperators.ZERO_EXTEND_B2S, shortSpecies, 0)
                .blend((short)newChar, found.cast(shortSpecies))
                .intoMemorySegment(StringUTF16.GLOBAL, address, ByteOrder.nativeOrder(), mask);
        Reference.reachabilityFence(scope);
        return new StringData(address, -length, scope);
    }

    public static StringData replaceUTF16(long address, int length, Scope scope,
                                          short oldChar, short newChar, SegmentAllocator allocator) {
        if (!StringData.compressible(length)) {
            var buffer = allocator.allocate(length * (long)Character.BYTES);
            long newAddress = buffer.address();
            Scope newScope = buffer.scope();
            StringUTF16Helper.replace(newAddress, newScope, address, scope, length, oldChar, newChar);
            return new StringData(newAddress, -length, newScope);
        }

        var byteSpecies = ByteVector.SPECIES_128;
        var shortSpecies = ShortVector.SPECIES_256;
        var mask = StringUTF16.SHORT_256_IOTA.compare(VectorOperators.LT, length);
        var data = ShortVector.fromMemorySegment(shortSpecies, StringUTF16.GLOBAL,
                address, ByteOrder.nativeOrder(), mask);
        var found = data.compare(VectorOperators.EQ, oldChar, mask);
        data = data.blend(newChar, found);

        var nonAscii = data.compare(VectorOperators.UNSIGNED_GT, Unicode.MAX_ASCII, mask);
        if (!nonAscii.anyTrue()) {
            var dataCompressed = data.convertShape(VectorOperators.S2B, byteSpecies, 0)
                    .reinterpretAsLongs();
            return new StringData(dataCompressed.lane(0),
                    dataCompressed.lane(1) | ((long)length << StringCompressed.LENGTH_SHIFT),
                    null);
        }

        var buffer = allocator.allocate(length * (long)Character.BYTES);
        long newAddress = buffer.address();
        Scope newScope = buffer.scope();
        data.intoMemorySegment(StringUTF16.GLOBAL, newAddress, ByteOrder.nativeOrder(), mask);
        return new StringData(newAddress, -length, newScope);
    }
}
