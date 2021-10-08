package io.github.merykitty.inlinestring.benchmark;

import io.github.merykitty.inlinestring.InlineString;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;

@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
public class ConstructorCharArrayBenchmark {
    @Param({"Hello world!", "This is an example of a Latin1 long string", "Đây là một UTF16 String"})
    private String param;

    char[] charArray;

    String dumpStr;
    InlineString dumpInlStr;

    @Setup(Level.Trial)
    public void warmup() {
        for (int i = 0; i < 100_000; i++) {
            var random = RandomGenerator.getDefault();
            int length = random.nextInt(16 * 2);
            char[] array = new char[length];
            for (int j = 0; j < length; j++) {
                array[j] = (char)random.nextInt('a', 'z');
            }
            dumpStr = new String(array);
            dumpInlStr = new InlineString(array);
        }
    }

    @Setup(Level.Iteration)
    public void charArray() {
        charArray = param.toCharArray();
    }

    @Benchmark
    public String charArrayStr() {
        return new String(charArray);
    }

    @Benchmark
    public InlineString charArrayInl() {
        return new InlineString(charArray);
    }
}
