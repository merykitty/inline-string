package io.github.merykitty.inlinestring.encoding.utf32;

import io.github.merykitty.inlinestring.encoding.Unicode;
import io.github.merykitty.inlinestring.internal.StringCompressed;
import io.github.merykitty.inlinestring.internal.StringData;
import io.github.merykitty.inlinestring.internal.StringUTF16;
import jdk.incubator.vector.*;

import java.lang.foreign.SegmentAllocator;
import java.nio.ByteOrder;

import static io.github.merykitty.inlinestring.internal.StringUTF16.GLOBAL;

public class UTF32Decoder {
    public static StringData apply(int[] data, int offset, int count, SegmentAllocator allocator) {
        var validResult = Validator.validate(data, offset, count);
        if (validResult.isInvalid()) {
            return StringData.defaultInstance();
        }

        return validResult.isAscii() ? compressed(data, offset, count)
                : nonCompressed(data, offset, count, allocator, validResult.utf16Length());
    }

    private static final int REMOVE_UPPER_TWO_BYTES = 0xFFFF0000;

    private static StringData compressed(int[] data, int offset, int count) {
        return IntVector.SPECIES_PREFERRED.vectorBitSize() >= 512
                ? compressed512(data, offset, count)
                : compressed256(data, offset, count);
    }

    private static StringData compressed512(int[] data, int offset, int count) {
        var species = IntVector.SPECIES_512;
        var mask = species.indexInRange(0, count);
        var payload = IntVector.fromArray(species, data, offset, mask)
                .convertShape(VectorOperators.I2B, ByteVector.SPECIES_128, 0)
                .reinterpretAsBytes()
                .withLane(StringCompressed.LENGTH_LANE, (byte)count)
                .reinterpretAsLongs();
        return new StringData(payload.lane(0), payload.lane(1), null);
    }

    private static StringData compressed256(int[] data, int offset, int count) {
        var species = IntVector.SPECIES_256;
        var mask0 = species.indexInRange(0, count);
        var payload0 = IntVector.fromArray(species, data, offset, mask0)
                .convertShape(VectorOperators.I2B, ByteVector.SPECIES_64, 0)
                .reinterpretAsLongs()
                .lane(0);
        var mask1 = species.indexInRange(species.length(), count);
        var payload1 = IntVector.fromArray(species, data, offset + species.length(), mask1)
                .convertShape(VectorOperators.I2B, ByteVector.SPECIES_64, 0)
                .reinterpretAsBytes()
                .withLane(StringCompressed.LENGTH_LANE_SECOND_HALF, (byte)count)
                .reinterpretAsLongs()
                .lane(0);
        return new StringData(payload0, payload1, null);
    }

    private static StringData nonCompressed(int[] data, int offset, int count,
                                            SegmentAllocator allocator,
                                            long utf16Length) {
        var buffer = allocator.allocate(utf16Length * Character.BYTES);
        long address = buffer.address();
        var intSpecies = IntVector.SPECIES_PREFERRED;
        int i = 0;
        long j = 0;
        for (; i < count; i += intSpecies.length()) {
            var loadMask = intSpecies.indexInRange(i, count);
            var input = IntVector.fromArray(intSpecies, data, offset + i, loadMask);
            var supplementaries = input.compare(VectorOperators.UNSIGNED_GT, Unicode.MAX_BMP);

            if (!supplementaries.anyTrue()) {
                var shortSpecies = intSpecies.vectorBitSize() == 512
                        ? ShortVector.SPECIES_256
                        : ShortVector.SPECIES_128;
                input.convertShape(VectorOperators.I2S, shortSpecies, 0)
                        .reinterpretAsShorts()
                        .intoMemorySegment(GLOBAL,
                                address + j,
                                ByteOrder.nativeOrder(), loadMask.cast(shortSpecies));
                j += shortSpecies.vectorByteSize();
                continue;
            }

            var correctedSup = input.lanewise(VectorOperators.SUB, StringUTF16.COMPLEMENTARY_OFFSET);
            var highSurrogates = correctedSup.lanewise(VectorOperators.LSHR, StringUTF16.HIGH_SURROGATE_SHIFT);
            var lowSurrogates = correctedSup.lanewise(VectorOperators.LSHL, Integer.SIZE - StringUTF16.COMPLEMENTARY_OFFSET)
                    .lanewise(VectorOperators.LSHR, Character.SIZE);
            var precompressed = input.blend(highSurrogates.lanewise(VectorOperators.OR, lowSurrogates), supplementaries)
                    .reinterpretAsShorts();
            var compressedMask = IntVector.broadcast(intSpecies, -1)
                    .blend(IntVector.broadcast(intSpecies, REMOVE_UPPER_TWO_BYTES).blend(0, supplementaries), loadMask)
                    .reinterpretAsShorts()
                    .compare(VectorOperators.EQ, 0);
            var storeMask = compressedMask.compress();
            precompressed.compress(compressedMask)
                    .intoMemorySegment(GLOBAL, address + j,
                            ByteOrder.nativeOrder(), storeMask);
            j += storeMask.trueCount();
        }
        return new StringData(address, -utf16Length, buffer.scope());
    }
}
