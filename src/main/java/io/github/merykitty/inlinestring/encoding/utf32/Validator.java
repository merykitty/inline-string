package io.github.merykitty.inlinestring.encoding.utf32;

import io.github.merykitty.inlinestring.encoding.Unicode;
import io.github.merykitty.inlinestring.encoding.ValidationResult;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorOperators;

import java.util.Objects;

class Validator {
    public static ValidationResult validate(int[] data, int offset, int count) {
        Objects.checkFromIndexSize(offset, count, data.length);
        var species = IntVector.SPECIES_PREFERRED;

        long utf16Length = 0;
        boolean isAscii = true;
        for (int i = offset; i < count; i += species.length()) {
            var mask = species.indexInRange(i, count);
            var input = IntVector.fromArray(species, data, i, mask);
            var nonAsciis = input.compare(VectorOperators.UNSIGNED_GT, Unicode.MAX_ASCII);
            if (!nonAsciis.anyTrue()) {
                utf16Length += mask.trueCount();
                continue;
            }

            var surrogates = input.lanewise(VectorOperators.AND, SURROGATE_MASK)
                    .compare(VectorOperators.EQ, SURROGATE_CHECK);
            var tooLarges = input.compare(VectorOperators.UNSIGNED_GT, Unicode.MAX_UNICODE);
            if (surrogates.or(tooLarges).anyTrue()) {
                return ValidationResult.invalid();
            }

            var supplementaries = input.compare(VectorOperators.UNSIGNED_GT, Unicode.MAX_BMP);
            utf16Length += ((long)species.length() + supplementaries.trueCount());
            isAscii = false;
        }

        return new ValidationResult(utf16Length, isAscii);
    }

    private static final int SURROGATE_MASK = 0xFFFFF800;
    private static final int SURROGATE_CHECK = 0x0000D800;
}
