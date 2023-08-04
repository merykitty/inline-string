package io.github.merykitty.inlinestring.encoding.utf16;

import io.github.merykitty.inlinestring.InlineString;
import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.LongVector;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorOperators;

import java.lang.foreign.MemorySegment.Scope;
import java.lang.ref.Reference;
import java.nio.ByteOrder;

import static io.github.merykitty.inlinestring.internal.StringUTF16.GLOBAL;

public class UTF16Encoder {
    public static void applyCompressed(char[] dst, int dstBegin, long firstHalf, long secondHalf, long srcBegin, int count) {
        long newFirstHalf = srcBegin < Long.BYTES ? firstHalf : secondHalf;
        long newSecondHalf = srcBegin < Long.BYTES ? secondHalf : 0;
        var mask = ShortVector.SPECIES_256.indexInRange(0, count);
        LongVector.zero(LongVector.SPECIES_128)
                .withLane(0, newFirstHalf)
                .withLane(1, newSecondHalf)
                .reinterpretAsBytes()
                .convertShape(VectorOperators.ZERO_EXTEND_B2S, ShortVector.SPECIES_256, 0)
                .reinterpretAsShorts()
                .intoCharArray(dst, dstBegin, mask);
    }

    public static void applyNonCompressed(char[] dst, int dstBegin, long address, Scope scope, long srcBegin, int count) {
        var species = ShortVector.SPECIES_PREFERRED;
        for (int i = 0; i < count; i += species.length()) {
            var mask = species.indexInRange(i, count);
            ShortVector.fromMemorySegment(species, GLOBAL,
                            (address + srcBegin * Character.BYTES) + (long)i * Character.BYTES,
                            ByteOrder.nativeOrder(), mask)
                    .intoCharArray(dst, dstBegin + i, mask);
        }
        Reference.reachabilityFence(scope);
    }
}
