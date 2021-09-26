package io.github.merykitty.inlinestring.internal;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorSpecies;

import java.nio.ByteOrder;
import java.util.function.Supplier;

public class Helper {
    public static final boolean COMPRESSED_STRINGS;
    public static final int[] HASH_COEF;
    static {
        Supplier<Boolean> compressedCalculation = () -> {
            try {
                var preferredSpecies = VectorSpecies.ofPreferred(Byte.TYPE);
                // raise to 256?
                return ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN && Utils.COMPACT_STRINGS && preferredSpecies.vectorBitSize() >= 128;
            } catch (IllegalArgumentException e) {
                return false;
            }
        };
        COMPRESSED_STRINGS = compressedCalculation.get();

        if (COMPRESSED_STRINGS) {
            var intSpecies = IntVector.SPECIES_PREFERRED;
            HASH_COEF = new int[intSpecies.length()];
            for (int i = 1, temp = 1; i <= HASH_COEF.length; i++) {
                HASH_COEF[HASH_COEF.length - i] = temp;
                temp *= 31;
            }
        } else {
            HASH_COEF = null;
        }
    }
}
