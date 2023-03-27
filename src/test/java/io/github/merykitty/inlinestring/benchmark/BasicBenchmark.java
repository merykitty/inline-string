package io.github.merykitty.inlinestring.benchmark;

import jdk.incubator.vector.*;
import jdk.internal.misc.Unsafe;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;

@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
public class BasicBenchmark {
    int index, len;
    long firstHalf, secondHalf;
    static final long[] ARRAY = new long[16];
    static byte[] dst = new byte[100];

    ArrayList<Object> list = new ArrayList<>(Arrays.asList(new Object[100]));
    
    PairLong dump;

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();

    private static final ByteVector VECTOR = ByteVector.zero(ByteVector.SPECIES_128).addIndex(1);
    private static final ByteVector COMPARE = ByteVector.broadcast(ByteVector.SPECIES_128, (byte)4);

    @__primitive__
    record PairLong(long x, long y){}

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public static long test(PairLong arg0, PairLong arg1) {
        return arg0 == arg1 ? 1 : 0;
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public static long dummy1() { return 1; }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public static long dummy2() { return 2; }

    @Benchmark
    public void run(Blackhole bh) {
        test(new PairLong(0, 1), new PairLong(0, 1));
        test(new PairLong(0, 1), new PairLong(0, 2));
    }
}
