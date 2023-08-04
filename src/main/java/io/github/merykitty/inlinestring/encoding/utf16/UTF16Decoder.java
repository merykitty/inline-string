package io.github.merykitty.inlinestring.encoding.utf16;

import io.github.merykitty.inlinestring.internal.StringCompressed;
import io.github.merykitty.inlinestring.internal.StringData;
import java.lang.foreign.SegmentAllocator;
import java.nio.ByteOrder;
import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorOperators;

import static io.github.merykitty.inlinestring.internal.StringUTF16.GLOBAL;

public class UTF16Decoder {
    public static StringData apply(char[] data, int offset,
                                   int count, SegmentAllocator allocator) {
        var validResult = Validator.validate(data, offset, count);
        if (validResult.isInvalid()) {
            return StringData.defaultInstance();
        }

        if (validResult.compressible()) {
            var species = ShortVector.SPECIES_256;
            var mask = species.indexInRange(0, count);
            var payload = ShortVector.fromCharArray(species, data, offset, mask)
                    .convertShape(VectorOperators.S2B, ByteVector.SPECIES_128, 0)
                    .reinterpretAsBytes()
                    .withLane(StringCompressed.LENGTH_LANE, (byte)count)
                    .reinterpretAsLongs();
            return new StringData(payload.lane(0), payload.lane(1), null);
        }
        var buffer = allocator.allocate(validResult.utf16Length() * Character.BYTES);
        long address = buffer.address();
        var species = ShortVector.SPECIES_PREFERRED;
        for (int i = 0; i < count; i += species.length()) {
            var mask = species.indexInRange(i, count);
            ShortVector.fromCharArray(species, data, offset + i, mask)
                    .intoMemorySegment(GLOBAL,
                            address + (long)i * Character.BYTES,
                            ByteOrder.nativeOrder(), mask);
        }
        return new StringData(address, -count, buffer.scope());
    }
}
