package io.github.merykitty.inlinestring.internal;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorSpecies;

import java.nio.ByteOrder;
import java.util.function.Supplier;

public class Helper {
    public static final byte[] BYTE_IOTA;
    public static final int[] HASH_COEF;

    static {
        if (Utils.COMPRESSED_STRINGS) {
            BYTE_IOTA = new byte[ByteVector.SPECIES_128.length() * 2];
            for (int i = 0; i < BYTE_IOTA.length; i++) {
                BYTE_IOTA[i] = (byte)(i % ByteVector.SPECIES_128.length());
            }

            HASH_COEF = new int[IntVector.SPECIES_PREFERRED.length()];
            for (int i = 1, temp = 1; i <= HASH_COEF.length; i++) {
                HASH_COEF[HASH_COEF.length - i] = temp;
                temp *= 31;
            }
        } else {
            HASH_COEF = null;
            BYTE_IOTA = null;
        }
    }
}
