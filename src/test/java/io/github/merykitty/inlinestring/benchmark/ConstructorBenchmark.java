package io.github.merykitty.inlinestring.benchmark;

import io.github.merykitty.inlinestring.InlineString;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
public class ConstructorBenchmark {
    @Param({"Hello world!", "This is an example of a Latin1 long string", "Đây là một UTF16 String"})
    private String param;

    char[] charArray;
    @Setup(Level.Trial)
    public void charArray() {
        charArray = param.toCharArray();
    }

    @Benchmark
    public void emptyStr(Blackhole bh) {
        bh.consume(new String(charArray));
    }

    @Benchmark
    public void emptyInl(Blackhole bh) {
        bh.consume(new InlineString(charArray));
    }
}
