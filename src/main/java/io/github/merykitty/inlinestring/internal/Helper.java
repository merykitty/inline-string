package io.github.merykitty.inlinestring.internal;

import jdk.incubator.vector.*;

import java.util.function.Function;

@SuppressWarnings("unchecked")
public class Helper {
    public static final VectorShuffle<Byte>[] BYTE_SLICE;
    public static final VectorShuffle<Byte> BYTE_SLICE_ONE;
    public static final VectorShuffle<Byte> BYTE_SLICE_HALF;
    public static final VectorShuffle<Long> LONG_SLICE_HALF_256;
    public static final VectorShuffle<Short>[] SHORT_SLICE_128;
    public static final VectorShuffle<Integer>[] INT_SLICE_256;
    public static final VectorShuffle<Integer>[] INT_SLICE_512;
    public static final IntVector HASH_COEF;
    public static final int[] HASH_INVERSION_COEF;
    public static final ByteVector BYTE_INDEX_VECTOR;
    public static final ShortVector SHORT_INDEX_VECTOR_128;

    static {
        if (Utils.COMPRESSED_STRINGS) {
            {
                BYTE_SLICE = (VectorShuffle<Byte>[]) new VectorShuffle[ByteVector.SPECIES_128.length()];
                for (int i = 0; i < BYTE_SLICE.length; i++) {
                    BYTE_SLICE[i] = ByteVector.broadcast(ByteVector.SPECIES_128, i)
                            .addIndex(1)
                            .and((byte)(BYTE_SLICE.length - 1))
                            .toShuffle();
                }
                BYTE_SLICE_ONE = BYTE_SLICE[1];
                BYTE_SLICE_HALF = BYTE_SLICE[Long.BYTES];
            }
            {
                LONG_SLICE_HALF_256 = VectorShuffle.fromValues(LongVector.SPECIES_256, 2, 3, 0, 1);
            }
            {
                SHORT_SLICE_128 = (VectorShuffle<Short>[]) new VectorShuffle[ShortVector.SPECIES_128.length()];
                for (int i = 0; i < SHORT_SLICE_128.length; i++) {
                    SHORT_SLICE_128[i] = ShortVector.broadcast(ShortVector.SPECIES_128, i)
                            .addIndex(1)
                            .and((short)(SHORT_SLICE_128.length - 1))
                            .toShuffle();
                }
            }
            {
                if (IntVector.SPECIES_PREFERRED.vectorBitSize() >= 512) {
                    INT_SLICE_256 = null;
                    INT_SLICE_512 = (VectorShuffle<Integer>[]) new VectorShuffle[IntVector.SPECIES_512.length()];
                    for (int i = 0; i < INT_SLICE_512.length; i++) {
                        INT_SLICE_512[i] = IntVector.broadcast(IntVector.SPECIES_512, i)
                                .addIndex(1)
                                .and(INT_SLICE_512.length - 1)
                                .toShuffle();
                    }
                } else {
                    INT_SLICE_512 = null;
                    INT_SLICE_256 = (VectorShuffle<Integer>[]) new VectorShuffle[IntVector.SPECIES_256.length()];
                    for (int i = 0; i < INT_SLICE_256.length; i++) {
                        INT_SLICE_256[i] = IntVector.broadcast(IntVector.SPECIES_256, i)
                                .addIndex(1)
                                .and(INT_SLICE_256.length - 1)
                                .toShuffle();
                    }
                }
            }
            {
                var intSpecies = IntVector.SPECIES_PREFERRED.vectorBitSize() >= 512 ? IntVector.SPECIES_512 : IntVector.SPECIES_256;
                int length = intSpecies.length();
                var hashCoefArray = new int[length];
                for (int i = 1, temp = 1; i <= length; i++) {
                    hashCoefArray[length - i] = temp;
                    temp *= 31;
                }
                HASH_COEF = IntVector.fromArray(intSpecies, hashCoefArray, 0);
            }
            {
                HASH_INVERSION_COEF = new int[32];
                Function<Integer, Integer> power = new Function<>() {
                    @Override
                    public Integer apply(Integer p) {
                        if (p == 0) {
                            return 1;
                        }
                        int recurse = this.apply(p >>> 1);
                        if ((p & 1) != 0) {
                            return 31 * recurse * recurse;
                        } else {
                            return recurse * recurse;
                        }
                    }
                };
                for (int i = 0; i < HASH_INVERSION_COEF.length; i++) {
                    HASH_INVERSION_COEF[i] = power.apply(Integer.MAX_VALUE - 15 + i);
                }
            }
            {
                BYTE_INDEX_VECTOR = ByteVector.zero(ByteVector.SPECIES_128).addIndex(1);
            }
            {
                SHORT_INDEX_VECTOR_128 = ShortVector.zero(ShortVector.SPECIES_128).addIndex(1);
            }
        } else {
            BYTE_SLICE = null;
            BYTE_SLICE_ONE = null;
            BYTE_SLICE_HALF = null;
            LONG_SLICE_HALF_256 = null;
            SHORT_SLICE_128 = null;
            INT_SLICE_256 = null;
            INT_SLICE_512 = null;
            HASH_COEF = null;
            HASH_INVERSION_COEF = null;
            BYTE_INDEX_VECTOR = null;
            SHORT_INDEX_VECTOR_128 = null;
        }
    }
}
