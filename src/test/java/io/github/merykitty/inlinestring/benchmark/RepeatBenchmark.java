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
public class RepeatBenchmark {
    @Param({"Hi!", "Hello world!", "This is an example of a Latin1 long string", "Đây là một UTF16 String"})
    private String param;
    @Param({"1", "3", "7"})
    private int count;

    String str;
    InlineString inlStr;

    @Setup(Level.Iteration)
    public void setUp() {
        this.str = new String(param);
        this.inlStr = new InlineString(param);
    }

    @Benchmark
    public void str(Blackhole bh) {
        bh.consume(str.repeat(count));
    }

    @Benchmark
    public void inlStr(Blackhole bh) {
        bh.consume(inlStr.repeat(count));
    }
}
