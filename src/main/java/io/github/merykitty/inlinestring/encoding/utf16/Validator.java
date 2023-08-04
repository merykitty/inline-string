package io.github.merykitty.inlinestring.encoding.utf16;

import io.github.merykitty.inlinestring.encoding.Unicode;
import io.github.merykitty.inlinestring.encoding.ValidationResult;
import java.util.Objects;
import jdk.incubator.vector.ShortVector;
import jdk.incubator.vector.VectorOperators;

class Validator {
    public static ValidationResult validate(char[] data, int offset, int count) {
        Objects.checkFromIndexSize(offset, count, data.length);
        var species = ShortVector.SPECIES_PREFERRED;

        boolean isAscii = true;
        var prev = ShortVector.zero(species);
        var prevIncomplete = species.maskAll(false);

        for (int i = 0; i < count; i += species.length()) {
            var input = ShortVector.fromCharArray(species, data, offset + i,
                    species.indexInRange(i, count));

            var nonAsciis = input.compare(VectorOperators.UNSIGNED_GT, Unicode.MAX_ASCII);
            if (!nonAsciis.or(prevIncomplete).anyTrue()) {
                continue;
            }

            var prev1 = prev.slice(species.length() - 1, input);
            var highSurrogate = prev1.lanewise(VectorOperators.AND, SURROGATE_MASK)
                    .compare(VectorOperators.EQ, HIGH_SURROGATE_PREFIX);
            var lowSurrogate = input.lanewise(VectorOperators.AND, SURROGATE_MASK)
                    .compare(VectorOperators.EQ, LOW_SURROGATE_PREFIX);
            if (!highSurrogate.eq(lowSurrogate).allTrue()) {
                return ValidationResult.invalid();
            }

            isAscii = false;
            prevIncomplete = input.lanewise(VectorOperators.AND, INCOMPLETE_MASK)
                    .compare(VectorOperators.EQ, INCOMPLETE_CHECK);
            prev = input;
        }

        if (prevIncomplete.anyTrue()) {
            return ValidationResult.invalid();
        }
        return new ValidationResult(count, isAscii);
    }

    private static final short SURROGATE_MASK = (short)0b1111110000000000;
    private static final short HIGH_SURROGATE_PREFIX = (short)0b1101100000000000;
    private static final short LOW_SURROGATE_PREFIX = (short)0b1101110000000000;

    private static final ShortVector INCOMPLETE_MASK;
    private static final ShortVector INCOMPLETE_CHECK;

    static {
        var species = ShortVector.SPECIES_PREFERRED;
        short[] data = new short[species.length()];
        data[species.length() - 1] = SURROGATE_MASK;
        INCOMPLETE_MASK = ShortVector.fromArray(species, data, 0);
        data[species.length() - 1] = HIGH_SURROGATE_PREFIX;
        for (int i = species.length() - 2; i >= 0; i--) {
            data[i] = -1;
        }
        INCOMPLETE_CHECK = ShortVector.fromArray(species, data, 0);
    }
}
