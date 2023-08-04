package io.github.merykitty.inlinestring.encoding.utf8;

import io.github.merykitty.inlinestring.internal.StringCompressed;
import io.github.merykitty.inlinestring.internal.StringData;
import jdk.incubator.vector.*;

import java.lang.foreign.SegmentAllocator;
import java.nio.ByteOrder;

import static io.github.merykitty.inlinestring.internal.StringUTF16.GLOBAL;

public class UTF8Decoder {
    public static StringData apply(byte[] data, int offset, int count, SegmentAllocator allocator) {
        var validResult = Validator.validate(data, offset, count);
        if (validResult.isInvalid()) {
            return StringData.defaultInstance();
        }

        return validResult.isAscii() ? compressed(data, offset, count)
                : nonCompressed(data, offset, count, allocator, validResult.utf16Length());
    }

    public static StringData compressed(byte[] data, int offset, int count) {
        var species = ByteVector.SPECIES_128;
        var mask = species.indexInRange(0, count);
        var payload = ByteVector.fromArray(species, data, offset, mask)
                .withLane(StringCompressed.LENGTH_LANE, (byte)count)
                .reinterpretAsLongs();
        return new StringData(payload.lane(0), payload.lane(1), null);
    }

    private static StringData nonCompressed(byte[] data, int offset, int count,
                                               SegmentAllocator allocator,
                                               long utf16Length) {
        var buffer = allocator.allocate(utf16Length * Character.BYTES);
        long address = buffer.address();

        var shortSpecies = ShortVector.SPECIES_PREFERRED;
        var byteSpecies = PREFERRED_SHORT_LENGTH_BYTE_SPECIES;

        int i = 0;
        int j = 0;
        for (; i < count;) {
            var loadMask = byteSpecies.indexInRange(i, count);
            var input = ByteVector.fromArray(byteSpecies, data,
                    offset + i, loadMask)
                    .convertShape(VectorOperators.B2S, shortSpecies, 0)
                    .reinterpretAsShorts();
            var asciis = input.compare(VectorOperators.GE, 0);
            if (asciis.allTrue()) {
                input.intoMemorySegment(GLOBAL, address + j,
                        ByteOrder.nativeOrder(), loadMask.cast(shortSpecies));
                i += shortSpecies.length();
                j += shortSpecies.vectorByteSize();
                continue;
            }

            var next1 = input.slice(1);
            var next2 = input.slice(2);
            var prev2 = input.slice(shortSpecies.length() - 2);

            var twoBytesMask = input.lanewise(VectorOperators.AND, TWO_BYTES_MASK)
                    .compare(VectorOperators.EQ, TWO_BYTES_CHECK);
            var threeBytesMask = input.lanewise(VectorOperators.AND, THREE_BYTES_MASK)
                    .compare(VectorOperators.EQ, THREE_BYTES_CHECK);
            var fourBytes0Mask = input.compare(VectorOperators.UNSIGNED_GE, FOUR_BYTES_CHECK);
            var fourBytes2Mask = prev2.compare(VectorOperators.UNSIGNED_GE, FOUR_BYTES_CHECK);

            var next1Aligned = next1.lanewise(VectorOperators.LSHL, CONTINUATION_PREFIX_LEN);
            var next2Aligned = next2.lanewise(VectorOperators.LSHL, CONTINUATION_PREFIX_LEN);

            var twoBytes = input.lanewise(VectorOperators.LSHL, TWO_BYTES_PREFIX_LEN)
                    .lanewise(VectorOperators.LSHR, TWO_BYTES_PREFIX_LEN - CONTINUATION_DATA_LEN)
                    .lanewise(VectorOperators.OR, next1.lanewise(VectorOperators.AND, CONTINUATION_DATA_MASK));
            var precompressed = input.blend(twoBytes, twoBytesMask);

            var threeBytes = input.lanewise(VectorOperators.LSHL, THREE_BYTES_PREFIX_LEN)
                    .lanewise(VectorOperators.OR, next1Aligned
                            .lanewise(VectorOperators.LSHR, CONTINUATION_PREFIX_LEN - CONTINUATION_DATA_LEN))
                    .lanewise(VectorOperators.OR, next2Aligned
                            .lanewise(VectorOperators.LSHR, CONTINUATION_PREFIX_LEN));
            precompressed = precompressed.blend(threeBytes, threeBytesMask);

            var fourBytes0 = input.lanewise(VectorOperators.LSHL, FOUR_BYTES_PREFIX_LEN)
                    .lanewise(VectorOperators.LSHR, 5)
                    .lanewise(VectorOperators.OR, next1Aligned
                            .lanewise(VectorOperators.LSHR, THIRD_BYTE_OFFSET - CONTINUATION_DATA_LEN))
                    .lanewise(VectorOperators.OR, next2Aligned
                            .lanewise(VectorOperators.LSHR, THIRD_BYTE_OFFSET))
                    .lanewise(VectorOperators.SUB, SUPPLEMENTARY_HIGH_ADJUSTMENT);
            precompressed = precompressed.blend(fourBytes0, fourBytes0Mask);

            var fourBytes2 = input.lanewise(VectorOperators.LSHL, 12)
                    .lanewise(VectorOperators.LSHR, Character.SIZE - CONTINUATION_DATA_LEN)
                    .lanewise(VectorOperators.OR, next1Aligned
                            .lanewise(VectorOperators.LSHR, CONTINUATION_PREFIX_LEN));
            precompressed = precompressed.blend(fourBytes2, fourBytes2Mask);

            var incompletes = input.compare(VectorOperators.UNSIGNED_GE, INCOMPLETE_CHECK);
            var incompletesAsLong = incompletes.toLong();
            var completes = VectorMask.fromLong(shortSpecies, incompletesAsLong - 1);
            var compressMask = asciis.or(twoBytesMask.or(threeBytesMask))
                    .or(fourBytes0Mask.or(fourBytes2Mask))
                    .and(completes.and(loadMask.cast(shortSpecies)));
            var compressed = precompressed.compress(compressMask);
            var storeMask = compressMask.compress();

            compressed.intoMemorySegment(GLOBAL, address + j,
                    ByteOrder.nativeOrder(), storeMask);
            i += Long.numberOfTrailingZeros(incompletesAsLong);
            j += storeMask.trueCount();
        }

        return new StringData(address, -utf16Length, buffer.scope());
    }

    private static final short TWO_BYTES_MASK = (short)0xFFE0;
    private static final short TWO_BYTES_CHECK = (short)0xFFC0;
    private static final short THREE_BYTES_MASK = (short)0xFFF0;
    private static final short THREE_BYTES_CHECK = (short)0xFFE0;
    private static final short FOUR_BYTES_CHECK = (short)0xFFF0;
    private static final short CONTINUATION_DATA_MASK = (short)0x3F;
    private static final short TWO_BYTES_PREFIX_LEN = 3 + Byte.SIZE;
    private static final short THREE_BYTES_PREFIX_LEN = 4 + Byte.SIZE;
    private static final short FOUR_BYTES_PREFIX_LEN = 5 + Byte.SIZE;
    private static final short THIRD_BYTE_OFFSET = 14;
    private static final short CONTINUATION_PREFIX_LEN = 2 + Byte.SIZE;
    private static final short CONTINUATION_DATA_LEN = 6;
    private static final short SUPPLEMENTARY_HIGH_ADJUSTMENT = (short)(0x10000 >> 10);
    private static final ShortVector INCOMPLETE_CHECK;
    private static final VectorSpecies<Byte> PREFERRED_SHORT_LENGTH_BYTE_SPECIES =
            VectorSpecies.of(byte.class,
                    VectorShape.forBitSize(ShortVector.SPECIES_PREFERRED.vectorBitSize() / Character.BYTES));

    static {
        var species = ShortVector.SPECIES_PREFERRED;
        short[] data = new short[species.length()];
        for (int i = 0; i < species.length() - 3; i++) {
            data[i] = -1;
        }
        data[species.length() - 3] = FOUR_BYTES_CHECK;
        data[species.length() - 2] = THREE_BYTES_CHECK;
        data[species.length() - 1] = TWO_BYTES_CHECK;
        INCOMPLETE_CHECK = ShortVector.fromArray(species, data, 0);
    }
}
