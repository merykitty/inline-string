package io.github.merykitty.inlinestring.encoding.string;

import io.github.merykitty.inlinestring.encoding.Unicode;
import io.github.merykitty.inlinestring.internal.StringCompressed;
import io.github.merykitty.inlinestring.internal.StringData;
import java.lang.foreign.SegmentAllocator;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;
import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorOperators;

import static io.github.merykitty.inlinestring.internal.StringUTF16.GLOBAL;

public class StringDecoder {
    public static StringData apply(String original, SegmentAllocator allocator) {
        int length = original.length();
        if (!StringData.compressible(length)) {
            var buffer = allocator.allocate((long)length * Character.BYTES);
            long address = buffer.address();
            for (int i = 0; i < length; i++) {
                GLOBAL.set(ValueLayout.JAVA_CHAR_UNALIGNED,
                        address + (long)i * Character.BYTES, original.charAt(i));
            }
            return new StringData(address, -length, buffer.scope());
        }

        var species = ShortVector.SPECIES_256;
        char[] tempBuffer = new char[16];
        original.getChars(0, length, tempBuffer, 0);
        var data = ShortVector.fromCharArray(species, tempBuffer, 0);

        if (data.compare(VectorOperators.UNSIGNED_LE, Unicode.MAX_ASCII).allTrue()) {
            var dataCompressed = data
                    .convertShape(VectorOperators.S2B, ByteVector.SPECIES_128, 0)
                    .reinterpretAsLongs();
            dataCompressed = dataCompressed.withLane(StringCompressed.LENGTH_LANE, length);
            return new StringData(dataCompressed.lane(0), dataCompressed.lane(1), null);
        }

        var buffer = allocator.allocate((long)length * Character.BYTES);
        long address = buffer.address();
        var mask = species.indexInRange(0, length);
        data.intoMemorySegment(GLOBAL, address, ByteOrder.nativeOrder(), mask);
        return new StringData(address, -length, buffer.scope());
    }
}
