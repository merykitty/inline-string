package io.github.merykitty.inlinestring.utf8;

import jdk.incubator.vector.*;

public class Decoder {
    public static void decodeLatin1(byte[] dst, byte[] src) {
        var speciesPreferred = ByteVector.SPECIES_PREFERRED;
        var continuationCheck = ByteVector.broadcast(speciesPreferred, (byte)0xC0);
        var lastByteMask = ByteVector.broadcast(speciesPreferred, 0x7F);
        int i = 0, j = 0;
        // Use j because LENGTH remaining code points implies more than LENGTH remaining input
        for (; j <= dst.length - speciesPreferred.length();) {
            var input = ByteVector.fromArray(speciesPreferred, src, i);

            // Shift to take into account 2-byte code points
            var continuations = input.compare(VectorOperators.LT, continuationCheck);
            var cumulative = continuations.toVector();
            cumulative = cumulative.lanewise(VectorOperators.ADD,
                    cumulative.rearrange(CUMULATIVE_SHUFFLE_1)
                            .lanewise(VectorOperators.AND, CUMULATIVE_MASK_1));
            cumulative = cumulative.lanewise(VectorOperators.ADD,
                    cumulative.rearrange(CUMULATIVE_SHUFFLE_2)
                            .lanewise(VectorOperators.AND, CUMULATIVE_MASK_2));
            cumulative = cumulative.lanewise(VectorOperators.ADD,
                    cumulative.reinterpretAsInts()
                            .rearrange(CUMULATIVE_SHUFFLE_4)
                            .lanewise(VectorOperators.AND, CUMULATIVE_MASK_4)
                            .reinterpretAsBytes());
            cumulative = cumulative.lanewise(VectorOperators.ADD,
                    cumulative.reinterpretAsInts()
                            .rearrange(CUMULATIVE_SHUFFLE_8)
                            .lanewise(VectorOperators.AND, CUMULATIVE_MASK_8)
                            .reinterpretAsBytes());
            if (speciesPreferred.length() >= ByteVector.SPECIES_256.length()) {
                cumulative = cumulative.lanewise(VectorOperators.ADD,
                        cumulative.reinterpretAsInts()
                                .rearrange(CUMULATIVE_SHUFFLE_16)
                                .lanewise(VectorOperators.AND, CUMULATIVE_MASK_16)
                                .reinterpretAsBytes());
            }
            if (speciesPreferred.length() >= ByteVector.SPECIES_512.length()) {
                cumulative = cumulative.lanewise(VectorOperators.ADD,
                        cumulative.reinterpretAsInts()
                                .rearrange(CUMULATIVE_SHUFFLE_32)
                                .lanewise(VectorOperators.AND, CUMULATIVE_MASK_32)
                                .reinterpretAsBytes());
            }
            var shuffle = IOTA_VECTOR.lanewise(VectorOperators.ADD, cumulative);

            // Combine the 2 bytes of 2-byte code points
            var concat2Bytes = input.reinterpretAsInts().lanewise(VectorOperators.LSHL, 6)
                    .reinterpretAsBytes()
                    .lanewise(VectorOperators.OR, input.lanewise(VectorOperators.AND, lastByteMask));
            var resolved = input.blend(concat2Bytes, continuations);
            resolved = resolved.rearrange(shuffle.toShuffle());
            resolved.intoArray(dst, j);
            long continuationBits = continuations.toLong();
            i += speciesPreferred.length() - (int)(continuationBits >>> (speciesPreferred.length() - 1));
            j += speciesPreferred.length() - Long.bitCount(continuationBits);
        }

        // We have less than LENGTH code points here
        var species64 = ByteVector.SPECIES_64;
        if (speciesPreferred.length() > species64.length()) {
            var cop = VectorOperators.Conversion.ofCast(byte.class, byte.class);
            for (; j <= dst.length - species64.length();) {
                var input = ByteVector.fromArray(species64, src, i);

                var continuations = input.compare(VectorOperators.LT, continuationCheck.convertShape(cop, species64, 0));
                var cumulative = continuations.toVector();
                cumulative = cumulative.lanewise(VectorOperators.ADD,
                        cumulative.rearrange(CUMULATIVE_SHUFFLE_1.cast(species64))
                                .lanewise(VectorOperators.AND, CUMULATIVE_MASK_1.convertShape(cop, species64, 0)));
                cumulative = cumulative.lanewise(VectorOperators.ADD,
                        cumulative.rearrange(CUMULATIVE_SHUFFLE_2.cast(species64))
                                .lanewise(VectorOperators.AND, CUMULATIVE_MASK_2.convertShape(cop, species64, 0)));
                var cumulativeAsInts = cumulative.reinterpretAsInts();
                cumulativeAsInts = cumulativeAsInts.withLane(1, cumulativeAsInts.lane(0) + cumulativeAsInts.lane(1));
                cumulative = cumulativeAsInts.reinterpretAsBytes();
                var shuffle = IOTA_VECTOR.convertShape(cop, species64, 0)
                        .lanewise(VectorOperators.ADD, cumulative);

                var concat2Bytes = input.reinterpretAsInts().lanewise(VectorOperators.LSHL, 6)
                        .reinterpretAsBytes()
                        .lanewise(VectorOperators.OR, input.lanewise(VectorOperators.AND, lastByteMask.convertShape(cop, species64, 0)));
                var resolved = input.blend(concat2Bytes, continuations);
                resolved = resolved.rearrange(shuffle.toShuffle());
                resolved.intoArray(dst, j);
                long continuationBits = continuations.toLong();
                i += speciesPreferred.length() - (int)(continuationBits >>> (speciesPreferred.length() - 1));
                j += speciesPreferred.length() - Long.bitCount(continuationBits);
            }
        }

        // We have less than 8 bytes here, just proceed
        for (; j < dst.length;) {
            byte current = src[i];
            if (current >= 0) {
                dst[j] = current;
                i++; j++;
            } else {
                byte next = src[i + 1];
                dst[j] = (byte)((current << 6) | (next & 0x7F));
                i += 2; j++;
            }
        }
    }

    @__primitive__
    public record StringSize(int length, boolean isLatin1) {}

    public static StringSize calculateSize(byte[] src) {
        var speciesPreferred = ByteVector.SPECIES_PREFERRED;

        var highestBitMask = ByteVector.broadcast(speciesPreferred, (byte)0b10000000);
        var utf16Check = ByteVector.broadcast(speciesPreferred, (byte)0b01000011);
        var startByteCheck = ByteVector.broadcast(speciesPreferred, (byte)0b10111111);
        var fourBytesCheck = ByteVector.broadcast(speciesPreferred, (byte)0b11110000);
        long isUtf16 = 0;
        int length = 0;
        int i = 0;
        for (; i < speciesPreferred.loopBound(src.length); i += speciesPreferred.length()) {
            var input = ByteVector.fromArray(speciesPreferred, src, 0);
            // Calculate encoding, do this since avx do not have unsigned comparison, see Integer.compareUnsigned
            var utf16s = input.lanewise(VectorOperators.XOR, highestBitMask)
                    .compare(VectorOperators.GT, utf16Check);
            isUtf16 = utf16s.toLong() | isUtf16;

            // Length += codePoints + 4-byte encoded codePoints
            length += input.compare(VectorOperators.GT, startByteCheck).trueCount();
            length += input.lanewise(VectorOperators.AND, fourBytesCheck).compare(VectorOperators.EQ, fourBytesCheck).trueCount();
        }

        // We have less than LENGTH bytes here
        var species64 = ByteVector.SPECIES_64;
        var cop = VectorOperators.Conversion.ofCast(byte.class, byte.class);
        highestBitMask = highestBitMask.convertShape(cop, species64, 0).reinterpretAsBytes();
        utf16Check = utf16Check.convertShape(cop, species64, 0).reinterpretAsBytes();
        startByteCheck = startByteCheck.convertShape(cop, species64, 0).reinterpretAsBytes();
        fourBytesCheck = fourBytesCheck.convertShape(cop, species64, 0).reinterpretAsBytes();
        for (; i < species64.loopBound(src.length); i += species64.length()) {
            var input = ByteVector.fromArray(species64, src, 0);
            // Calculate encoding, do this since avx do not have unsigned comparison, see Integer.compareUnsigned
            var utf16s = input.lanewise(VectorOperators.XOR, highestBitMask)
                    .compare(VectorOperators.GT, utf16Check);
            isUtf16 = utf16s.toLong() | isUtf16;

            // Length += codePoints + 4-byte encoded codePoints
            length += input.compare(VectorOperators.GT, startByteCheck).trueCount();
            length += input.lanewise(VectorOperators.AND, fourBytesCheck).compare(VectorOperators.EQ, fourBytesCheck).trueCount();
        }

        // We have less than 8 bytes here
        for (; i < src.length; i++) {
            byte current = src[i];
            isUtf16 = Byte.toUnsignedInt(current) > 0b11000011 ? 1 : isUtf16;
            length += current > (byte)0b10111111 ? 1 : 0;
            length += Byte.toUnsignedInt(current) >= 0b11110000 ? 1 : 0;
        }

        return new StringSize(length, isUtf16 == 0);
    }

    private static final ByteVector IOTA_VECTOR;
    // Use these because slice is implemented terribly
    private static final Vector<Byte> CUMULATIVE_MASK_1;
    private static final VectorShuffle<Byte> CUMULATIVE_SHUFFLE_1;
    private static final Vector<Byte> CUMULATIVE_MASK_2;
    private static final VectorShuffle<Byte> CUMULATIVE_SHUFFLE_2;
    // Promote to int because it is likely the most efficient type
    private static final Vector<Integer> CUMULATIVE_MASK_4;
    private static final VectorShuffle<Integer> CUMULATIVE_SHUFFLE_4;
    private static final Vector<Integer> CUMULATIVE_MASK_8;
    private static final VectorShuffle<Integer> CUMULATIVE_SHUFFLE_8;
    private static final Vector<Integer> CUMULATIVE_MASK_16;
    private static final VectorShuffle<Integer> CUMULATIVE_SHUFFLE_16;
    private static final Vector<Integer> CUMULATIVE_MASK_32;
    private static final VectorShuffle<Integer> CUMULATIVE_SHUFFLE_32;

    static {
        IOTA_VECTOR = ByteVector.zero(ByteVector.SPECIES_PREFERRED).addIndex(1);
        CUMULATIVE_MASK_1 = ByteVector.SPECIES_PREFERRED.indexInRange(0, 1).not().toVector();
        CUMULATIVE_SHUFFLE_1 = VectorShuffle.fromOp(ByteVector.SPECIES_PREFERRED,
                i -> Math.max(i - 1, 0));
        CUMULATIVE_MASK_2 = ByteVector.SPECIES_PREFERRED.indexInRange(0, 2).not().toVector();
        CUMULATIVE_SHUFFLE_2 = VectorShuffle.fromOp(ByteVector.SPECIES_PREFERRED,
                i -> Math.max(i - 2, 0));
        CUMULATIVE_MASK_4 = IntVector.SPECIES_PREFERRED.indexInRange(0, 1).not().toVector();
        CUMULATIVE_SHUFFLE_4 = VectorShuffle.fromOp(IntVector.SPECIES_PREFERRED,
                i -> Math.max(i - 1, 0));
        CUMULATIVE_MASK_8 = IntVector.SPECIES_PREFERRED.indexInRange(0, 2).not().toVector();
        CUMULATIVE_SHUFFLE_8 = VectorShuffle.fromOp(IntVector.SPECIES_PREFERRED,
                i -> Math.max(i - 2, 0));
        CUMULATIVE_MASK_16 = IntVector.SPECIES_PREFERRED.indexInRange(0, 4).not().toVector();
        CUMULATIVE_SHUFFLE_16 = VectorShuffle.fromOp(IntVector.SPECIES_PREFERRED,
                i -> Math.max(i - 4, 0));
        CUMULATIVE_MASK_32 = IntVector.SPECIES_PREFERRED.indexInRange(0, 8).not().toVector();
        CUMULATIVE_SHUFFLE_32 = VectorShuffle.fromOp(IntVector.SPECIES_PREFERRED,
                i -> Math.max(i - 8, 0));
    }
}
