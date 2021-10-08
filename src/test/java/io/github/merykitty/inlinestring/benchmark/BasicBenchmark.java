package io.github.merykitty.inlinestring.benchmark;

import jdk.incubator.vector.LongVector;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;

@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
public class BasicBenchmark {
    private static final int[] ARRAY = new int[16];

    int index;
    long[] array = new long[100];

    @Setup(Level.Iteration)
    public void setup() {
        index = RandomGenerator.getDefault().nextInt();
    }

    @Benchmark
    public void test() {
        var first = LongVector.zero(LongVector.SPECIES_128)
                .withLane(0, 4)
                .withLane(1, 8)
                .castShape(LongVector.SPECIES_256, 0)
                .reinterpretAsLongs();
        var second = LongVector.zero(LongVector.SPECIES_128)
                .withLane(0, 12)
                .withLane(1, 16)
                .castShape(LongVector.SPECIES_256, -1)
                .reinterpretAsLongs();
        first.or(second).intoArray(array, 0);
    }
}
