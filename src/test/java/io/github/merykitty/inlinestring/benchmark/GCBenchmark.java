package io.github.merykitty.inlinestring.benchmark;

import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.vector.*;
import org.openjdk.jmh.annotations.*;

import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;

@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
public class GCBenchmark {
    byte[] array;
    int length;
    int[] intArray;
    int intLength;
    int rotation;
    long firstHalf;
    long secondHalf;

    static final ByteArrayCache BUFFER;
    static final VectorShuffle<Byte> INFLATION_SHUFFLE_0 = VectorShuffle.makeZip(ByteVector.SPECIES_PREFERRED, 0);
    static final VectorShuffle<Byte> INFLATION_SHUFFLE_1 = VectorShuffle.makeZip(ByteVector.SPECIES_PREFERRED, 1);
    static final VectorShuffle<Byte> COMPRESS_SHUFFLE = VectorShuffle.makeUnzip(ByteVector.SPECIES_PREFERRED, 0);
    static final ByteVector INDEX_VECTOR;
    static {
        BUFFER = new ByteArrayCache();
        var byteSpecies = ByteVector.SPECIES_PREFERRED;
        byte[] indexArray = new byte[byteSpecies.length()];
        for (int i = 0; i < indexArray.length; i++) {
            indexArray[i] = (byte)i;
        }
        INDEX_VECTOR = ByteVector.fromArray(byteSpecies, indexArray, 0);
    }

    static class ByteArrayCache extends ThreadLocal<byte[]> {
        @Override
        protected byte[] initialValue() {
            return new byte[ByteVector.SPECIES_MAX.length()];
        }
    }

    @Setup(Level.Trial)
    public void setUp() {
        var random = RandomGenerator.getDefault();
        length = 14;
        rotation = 5;
        array = new byte[256];
        random.nextBytes(array);
        intLength = 3;
        intArray = random.ints(intLength).toArray();
    }

    @Benchmark
    public void foreignBytesToLongs() {
        firstHalf = compressHelper(array, 0, length);
        secondHalf = compressHelper(array, 8, length - 8);
    }

    @Benchmark
    public void foreignLongsToBytes() {
        uncompressHelper(array, 0, length, firstHalf);
        uncompressHelper(array, 8, length - 8, secondHalf);
    }

//    @Benchmark
    public void vectorIntsToLongs() {
        var intSpecies = IntVector.SPECIES_PREFERRED;
        var mask = intSpecies.indexInRange(0, intLength);
        var dataAsInts = IntVector.fromArray(intSpecies, intArray, 0, mask);
        var dataAsLongs = dataAsInts.reinterpretAsLongs();
        firstHalf = dataAsLongs.lane(0);
        secondHalf = dataAsLongs.lane(1);
    }

//    @Benchmark
    public void vectorLongsToInts() {
        var longSpecies = LongVector.SPECIES_PREFERRED;
        var intSpecies = IntVector.SPECIES_PREFERRED;
        var mask = intSpecies.indexInRange(0, intLength);
        var dataAsLongs = LongVector.zero(longSpecies)
                .withLane(0, firstHalf)
                .withLane(1, secondHalf);
        var dataAsInts = dataAsLongs.reinterpretAsInts();
        dataAsInts.intoArray(intArray, 0, mask);
    }

    public byte vectorRotation() {
        var byteSpecies = ByteVector.SPECIES_128;
        var data = ByteVector.fromArray(byteSpecies, array, 0);
        var shuffleVector = VectorShuffle.iota(byteSpecies, rotation, 1, true);
        data = data.rearrange(shuffleVector);
        return data.lane(0);
    }

    public short vectorInflation() {
        var byteSpecies = ByteVector.SPECIES_128;
        var charSpecies = ShortVector.SPECIES_PREFERRED;
        var data = ByteVector.fromArray(byteSpecies, array, 0);
        var dataAsChars = data.castShape(charSpecies, 0).reinterpretAsShorts();
        return dataAsChars.lane(0);
    }

    public byte vectorCompression() {
        var byteSpecies = ByteVector.SPECIES_PREFERRED;
        var data = ByteVector.fromArray(byteSpecies, array, 0);
        var zero = ByteVector.zero(byteSpecies);
        data = data.rearrange(COMPRESS_SHUFFLE, zero);
        return data.lane(0);
    }

    private static long compressHelper(byte[] value, int offset, int length) {
        var seg = MemorySegment.ofArray(value);
        if (length <= 0) {
            return 0;
        } else if (length >= 8) {
            return MemoryAccess.getLongAtOffset(seg, offset);
        } else {
            return switch (length) {
                case 1 -> MemoryAccess.getByteAtOffset(seg, offset);
                case 2 -> MemoryAccess.getShortAtOffset(seg, offset);
                case 3 -> MemoryAccess.getShortAtOffset(seg, offset)
                        | (long) MemoryAccess.getByteAtOffset(seg, offset + 2) << (2 * 8);
                case 4 -> MemoryAccess.getIntAtOffset(seg, offset);
                case 5 -> MemoryAccess.getIntAtOffset(seg, offset)
                        | (long) MemoryAccess.getByteAtOffset(seg, offset + 4) << (4 * 8);
                case 6 -> MemoryAccess.getIntAtOffset(seg, offset)
                        | (long) MemoryAccess.getShortAtOffset(seg, offset + 4) << (4 * 8);
                default -> MemoryAccess.getIntAtOffset(seg, offset)
                        | (long) MemoryAccess.getShortAtOffset(seg, offset + 4) << (4 * 8)
                        | (long) MemoryAccess.getByteAtOffset(seg, offset + 6) << (6 * 8);
            };
        }
    }

    private static void uncompressHelper(byte[] dst, int offset, int length, long value) {
        var seg = MemorySegment.ofArray(dst);
        if (length >= 8) {
            MemoryAccess.setLongAtOffset(seg, offset, value);
        } else if (length > 0) {
            switch (length) {
                case 1 -> MemoryAccess.setByteAtOffset(seg, offset, (byte) value);
                case 2 -> MemoryAccess.setShortAtOffset(seg, offset, (short) value);
                case 3 -> {
                    MemoryAccess.setShortAtOffset(seg, offset, (short) value);
                    MemoryAccess.setByteAtOffset(seg, offset + 2, (byte) (value >> (2 * 8)));
                }
                case 4 -> MemoryAccess.setIntAtOffset(seg, offset, (int) value);
                case 5 -> {
                    MemoryAccess.setIntAtOffset(seg, offset, (int) value);
                    MemoryAccess.setByteAtOffset(seg, offset + 4, (byte) (value >> (4 * 8)));
                }
                case 6 -> {
                    MemoryAccess.setIntAtOffset(seg, offset, (int) value);
                    MemoryAccess.setShortAtOffset(seg, offset + 4, (short) (value >> (4 * 8)));
                }
                default -> {
                    MemoryAccess.setIntAtOffset(seg, offset, (int) value);
                    MemoryAccess.setShortAtOffset(seg, offset + 4, (short) (value >> (4 * 8)));
                    MemoryAccess.setByteAtOffset(seg, offset + 6, (byte) (value >> (6 * 8)));
                }
            }
        }
    }
}
