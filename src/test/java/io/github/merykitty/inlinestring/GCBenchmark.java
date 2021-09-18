package io.github.merykitty.inlinestring;

import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.LongVector;
import jdk.incubator.vector.VectorSpecies;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.annotations.Benchmark;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;

@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
public class GCBenchmark {
    static final VectorSpecies<Byte> BYTE_VECTOR_SPECIES = ByteVector.SPECIES_PREFERRED;
    static final VectorSpecies<Integer> INTEGER_VECTOR_SPECIES = IntVector.SPECIES_PREFERRED;
    static final VectorSpecies<Long> LONG_VECTOR_SPECIES = LongVector.SPECIES_PREFERRED;

    int[] array1;
    int length;
    long firstHalf;
    long secondHalf;

    @Setup(Level.Trial)
    public void setUp() {
        var random = RandomGenerator.getDefault();
        length = 3;
        array1 = random.ints(length).toArray();
    }

    @Benchmark
    public void vectorBytesToLong() {
        var mask = INTEGER_VECTOR_SPECIES.indexInRange(0, length);
        var loadVector = IntVector.fromArray(INTEGER_VECTOR_SPECIES, array1, 0, mask);
        var convertedVector = loadVector.reinterpretAsLongs();
        firstHalf = convertedVector.lane(0);
        secondHalf = convertedVector.lane(1);
    }

    @Benchmark
    public void vectorLongToBytes() {
        var mask = INTEGER_VECTOR_SPECIES.indexInRange(0, length);
        var loadVector = LongVector.broadcast(LONG_VECTOR_SPECIES, 0)
                .withLane(0, firstHalf)
                .withLane(1, secondHalf);
        var convertedVector = loadVector.reinterpretAsInts();
        convertedVector.intoArray(array1, 0, mask);
    }

    public void foreignBytesToLong() {
        var temp = MemorySegment.ofArray(Arrays.copyOf(array1, 16));
        firstHalf = MemoryAccess.getLongAtIndex(temp, 0);
        secondHalf = MemoryAccess.getLongAtIndex(temp, 1);
    }

    public void foreignLongToBytes() {
        var tempArray = new byte[16];
        var temp = MemorySegment.ofArray(tempArray);
        MemoryAccess.setLongAtIndex(temp, 0, firstHalf);
        MemoryAccess.setLongAtIndex(temp, 1, secondHalf);
        System.arraycopy(tempArray, 0, array1, 0, length);
    }
}
