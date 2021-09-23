package io.github.merykitty.inlinestring.benchmark;

import jdk.incubator.vector.*;
import org.openjdk.jmh.annotations.*;

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
    int rotation;
    long firstHalf;
    long secondHalf;

    static final VectorShuffle<Byte> INFLATION_SHUFFLE_0 = VectorShuffle.makeZip(ByteVector.SPECIES_PREFERRED, 0);
    static final VectorShuffle<Byte> INFLATION_SHUFFLE_1 = VectorShuffle.makeZip(ByteVector.SPECIES_PREFERRED, 1);
    static final VectorShuffle<Byte> COMPRESS_SHUFFLE = VectorShuffle.makeUnzip(ByteVector.SPECIES_PREFERRED, 0);
    static final ByteVector INDEX_VECTOR;
    static {
        var byteSpecies = ByteVector.SPECIES_PREFERRED;
        byte[] indexArray = new byte[byteSpecies.length()];
        for (int i = 0; i < indexArray.length; i++) {
            indexArray[i] = (byte)i;
        }
        INDEX_VECTOR = ByteVector.fromArray(byteSpecies, indexArray, 0);
    }

    @Setup(Level.Trial)
    public void setUp() {
        var random = RandomGenerator.getDefault();
        length = 14;
        rotation = 5;
        array = new byte[256];
        random.nextBytes(array);
    }

    @Benchmark
    public void vectorBytesToLong() {
        var byteSpecies = ByteVector.SPECIES_PREFERRED;
        var mask = INDEX_VECTOR.lt((byte)length);
        var data = ByteVector.fromArray(byteSpecies, array, 0, mask);
        var dataAsLongs = data.reinterpretAsLongs();
        firstHalf = dataAsLongs.lane(0);
        secondHalf = dataAsLongs.lane(1);
    }

    @Benchmark
    public void vectorLongToBytes() {
        var longSpecies = LongVector.SPECIES_PREFERRED;
        var mask = INDEX_VECTOR.lt((byte)length);
        var dataAsLongs = LongVector.zero(longSpecies)
                .withLane(0, firstHalf)
                .withLane(1, secondHalf);
        var data = dataAsLongs.reinterpretAsBytes();
        data.intoArray(array, 0, mask);
    }

    public byte vectorRotation() {
        var byteSpecies = ByteVector.SPECIES_PREFERRED;
        var data = ByteVector.fromArray(byteSpecies, array, 0);
        var shuffleVector = VectorShuffle.iota(byteSpecies, rotation, 1, true);
        data = data.rearrange(shuffleVector);
        return data.lane(0);
    }

    public short vectorInflation() {
        var byteSpecies = ByteVector.SPECIES_PREFERRED;
        var data = ByteVector.fromArray(byteSpecies, array, 0);
        var zero = ByteVector.zero(byteSpecies);
        data = data.rearrange(INFLATION_SHUFFLE_0, zero);
        var dataAsChars = data.reinterpretAsShorts();
        return dataAsChars.lane(0);
    }

    public byte vectorCompression() {
        var byteSpecies = ByteVector.SPECIES_PREFERRED;
        var data = ByteVector.fromArray(byteSpecies, array, 0);
        var zero = ByteVector.zero(byteSpecies);
        data = data.rearrange(COMPRESS_SHUFFLE, zero);
        return data.lane(0);
    }
}
