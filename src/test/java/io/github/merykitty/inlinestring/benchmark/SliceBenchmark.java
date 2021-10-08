package io.github.merykitty.inlinestring.benchmark;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.LongVector;
import jdk.incubator.vector.VectorShuffle;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;

@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
public class SliceBenchmark {
    static final ByteVector VECTOR;
    static final VectorShuffle<Byte>[] IOTA;
    static final VectorShuffle<Byte> SLICE_1;

    static {
        var random = RandomGenerator.getDefault();
        long firstHalf = random.nextLong();
        long secondHalf = random.nextLong();
        VECTOR = LongVector.zero(LongVector.SPECIES_128)
                .withLane(0, firstHalf)
                .withLane(1, secondHalf)
                .reinterpretAsBytes();
        IOTA = (VectorShuffle<Byte>[]) new VectorShuffle[ByteVector.SPECIES_128.length()];
        for (int i = 0; i < IOTA.length; i++) {
            IOTA[i] = ByteVector.broadcast(ByteVector.SPECIES_128, i)
                    .addIndex(1)
                    .and((byte)(ByteVector.SPECIES_128.length() - 1))
                    .toShuffle();
        }
        SLICE_1 = IOTA[1];
    }

    int sliceAmount;

    @Setup(Level.Iteration)
    public void setup() {
        var random = RandomGenerator.getDefault();

        sliceAmount = random.nextInt(ByteVector.SPECIES_128.length());
    }

//    @Benchmark
    public long fastest() {
        return VECTOR.rearrange(SLICE_1)
                .reinterpretAsLongs()
                .lane(0);
    }

//    @Benchmark
    public long slice() {
        return VECTOR.slice(sliceAmount)
                .reinterpretAsLongs()
                .lane(0);
    }

//    @Benchmark
    public long iota() {
        return VECTOR.rearrange(VectorShuffle.iota(ByteVector.SPECIES_128, sliceAmount, 1, true))
                .reinterpretAsLongs()
                .lane(0);
    }

    @Benchmark
    public long savedShuffle() {
        return VECTOR.rearrange(IOTA[sliceAmount])
                .reinterpretAsLongs()
                .lane(0);
    }
}
