package io.github.merykitty.inlinestring.encoding.utf8;

import io.github.merykitty.inlinestring.encoding.ValidationResult;
import java.util.Objects;
import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorOperators;

class Validator {
    public static ValidationResult validate(byte[] data, int offset, int count) {
        Objects.checkFromIndexSize(offset, count, data.length);
        var species = ByteVector.SPECIES_PREFERRED;

        long utf16Length = 0;
        boolean isAscii = true;
        var prev = ByteVector.zero(species);
        var prevIncomplete = species.maskAll(false);

        for (int i = 0; i < count; i += species.length()) {
            var mask = species.indexInRange(i, count);
            var input = ByteVector.fromArray(species, data, offset + i, mask);

            // Fast path, all bytes are positive
            var nonAsciis = input.compare(VectorOperators.LT, 0);
            if (!nonAsciis.or(prevIncomplete).anyTrue()) {
                utf16Length += mask.trueCount();
                continue;
            }

            // Slow path, some bytes are negative
            var prev1 = prev.slice(species.length() - 1, input);
            var prev2 = prev.slice(species.length() - 2, input);
            var prev3 = prev.slice(species.length() - 3, input);

            // Look up errors
            // avx do not have vector shift for bytes
            var hi2 = input.reinterpretAsInts()
                    .lanewise(VectorOperators.LSHR, 4)
                    .reinterpretAsBytes()
                    .lanewise(VectorOperators.AND, LOW_NIBBLE_MASK);
            var lo1 = prev1.lanewise(VectorOperators.AND, LOW_NIBBLE_MASK);
            var hi1 = prev1.reinterpretAsInts()
                    .lanewise(VectorOperators.LSHR, 4)
                    .reinterpretAsBytes()
                    .lanewise(VectorOperators.AND, LOW_NIBBLE_MASK);
            hi2 = hi2.selectFrom(HI2_LUT);
            lo1 = lo1.selectFrom(LO1_LUT);
            hi1 = hi1.selectFrom(HI1_LUT);
            var tempError = hi2.lanewise(VectorOperators.AND, lo1)
                    .lanewise(VectorOperators.AND, hi1);

            // Mask out valid 2 consecutive continuations
            var threeBytes = prev2.compare(VectorOperators.UNSIGNED_GE, THREE_BYTES_CHECK);
            var fourBytes = prev3.compare(VectorOperators.UNSIGNED_GE, FOUR_BYTES_CHECK);
            var maskedErrorMask = threeBytes.or(fourBytes);
            var maskedError = ByteVector.zero(species).blend(HIGHEST_BIT_MASK, maskedErrorMask);

            if(!tempError.lanewise(VectorOperators.XOR, maskedError)
                    .compare(VectorOperators.EQ, 0).allTrue()) {
                return ValidationResult.invalid();
            }

            // Body epilogue
            var startBytes = input.compare(VectorOperators.GE, TWO_BYTES_CHECK, mask);
            utf16Length += (startBytes.trueCount() + fourBytes.trueCount());
            isAscii = false;
            prevIncomplete = input.compare(VectorOperators.UNSIGNED_GE, INCOMPLETE_CHECK);
            prev = input;
        }

        if (prevIncomplete.anyTrue()) {
            return ValidationResult.invalid();
        }
        return new ValidationResult(utf16Length, isAscii);
    }

    private static final byte LOW_NIBBLE_MASK = (byte)0b00001111;
    private static final byte FOUR_BYTES_CHECK = (byte)0b11110000;
    private static final byte TWO_BYTES_CHECK = (byte)0b11000000;
    private static final byte THREE_BYTES_CHECK = (byte)0b11100000;
    private static final byte HIGHEST_BIT_MASK = (byte)0b10000000;

    private static final ByteVector HI1_LUT;
    private static final ByteVector LO1_LUT;
    private static final ByteVector HI2_LUT;
    private static final ByteVector INCOMPLETE_CHECK;

    static {
        var species = ByteVector.SPECIES_PREFERRED;
        if (species.length() < ByteVector.SPECIES_128.length()) {
            throw new AssertionError();
        }

        byte TOO_SHORT   = 1<<0;         // 11______ 0_______
        // 11______ 11______
        byte TOO_LONG    = 1<<1;         // 0_______ 10______
        byte OVERLONG_3  = 1<<2;         // 11100000 100_____
        byte SURROGATE   = 1<<4;         // 11101101 101_____
        byte OVERLONG_2  = 1<<5;         // 1100000_ 10______
        byte TWO_CONTS   = (byte)(1<<7); // 10______ 10______
        byte TOO_LARGE   = 1<<3;         // 11110100 1001____
        // 11110100 101_____
        // 11110101 1001____
        // 11110101 101_____
        // 1111011_ 1001____
        // 1111011_ 101_____
        // 11111___ 1001____
        // 11111___ 101_____
        byte TOO_LARGE_1000 = 1<<6;      // 11110101 1000____
        // 1111011_ 1000____
        // 11111___ 1000____
        byte OVERLONG_4 = 1<<6;          // 11110000 1000____
        byte CARRY = (byte)(TOO_SHORT | TOO_LONG | TWO_CONTS); // These all have ____ in byte 1
        byte[] data;

        // HI1_LUT
        data = new byte[]{
                TOO_LONG, TOO_LONG, TOO_LONG, TOO_LONG, TOO_LONG, TOO_LONG, TOO_LONG, TOO_LONG,
                TWO_CONTS, TWO_CONTS, TWO_CONTS, TWO_CONTS,
                (byte) (TOO_SHORT | OVERLONG_2),
                TOO_SHORT,
                (byte) (TOO_SHORT | OVERLONG_3 | SURROGATE),
                (byte) (TOO_SHORT | TOO_LARGE | TOO_LARGE_1000 | OVERLONG_4),
        };
        HI1_LUT = ByteVector.fromArray(ByteVector.SPECIES_128, data, 0)
                .convertShape(VectorOperators.Conversion.ofCast(byte.class, byte.class), species, 0)
                .reinterpretAsBytes();

        // LO1_LUT
        data = new byte[] {
                (byte) (CARRY | OVERLONG_3 | OVERLONG_2 | OVERLONG_4),
                (byte) (CARRY | OVERLONG_2),
                CARRY, CARRY,
                (byte) (CARRY | TOO_LARGE),
                (byte) (CARRY | TOO_LARGE_1000),
                (byte) (CARRY | TOO_LARGE_1000),
                (byte) (CARRY | TOO_LARGE_1000),
                (byte) (CARRY | TOO_LARGE_1000),
                (byte) (CARRY | TOO_LARGE_1000),
                (byte) (CARRY | TOO_LARGE_1000),
                (byte) (CARRY | TOO_LARGE_1000),
                (byte) (CARRY | TOO_LARGE_1000),
                (byte) (CARRY | TOO_LARGE_1000 | SURROGATE),
                (byte) (CARRY | TOO_LARGE_1000),
                (byte) (CARRY | TOO_LARGE_1000),
        };
        LO1_LUT = ByteVector.fromArray(ByteVector.SPECIES_128, data, 0)
                .convertShape(VectorOperators.Conversion.ofCast(byte.class, byte.class), species, 0)
                .reinterpretAsBytes();

        // HI2_LUT
        data = new byte[] {
                TOO_SHORT, TOO_SHORT, TOO_SHORT, TOO_SHORT, TOO_SHORT, TOO_SHORT, TOO_SHORT, TOO_SHORT,
                (byte) (TOO_LONG | OVERLONG_2 | TWO_CONTS | OVERLONG_3 | TOO_LARGE_1000 | OVERLONG_4),
                (byte) (TOO_LONG | OVERLONG_2 | TWO_CONTS | OVERLONG_3 | TOO_LARGE),
                (byte) (TOO_LONG | OVERLONG_2 | TWO_CONTS | SURROGATE  | TOO_LARGE),
                (byte) (TOO_LONG | OVERLONG_2 | TWO_CONTS | SURROGATE  | TOO_LARGE),
                TOO_SHORT, TOO_SHORT, TOO_SHORT, TOO_SHORT,
        };
        HI2_LUT = ByteVector.fromArray(ByteVector.SPECIES_128, data, 0)
                .convertShape(VectorOperators.Conversion.ofCast(byte.class, byte.class), species, 0)
                .reinterpretAsBytes();

        // Incomplete check verify if the final vector ends with incomplete sequence
        data = new byte[species.length()];
        data[species.length() - 1] = TWO_BYTES_CHECK;
        data[species.length() - 2] = THREE_BYTES_CHECK;
        data[species.length() - 3] = FOUR_BYTES_CHECK;
        for (int i = species.length() - 4; i >= 0; i--) {
            data[i] = -1;
        }
        INCOMPLETE_CHECK = ByteVector.fromArray(species, data, 0);
    }
}
