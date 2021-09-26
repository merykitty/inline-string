package io.github.merykitty.inlinestring.benchmark;

import io.github.merykitty.inlinestring.InlineString;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
public class HashCodeBenchmark {
    @Param({"Hello world!", "This is an example of a Latin1 long string", "Đây là một UTF16 String"})
    private String param;

    String str;
    InlineString inlStr;

    @Setup(Level.Trial)
    public void setUp() {
        this.str = new String(param);
        this.inlStr = new InlineString(param);
    }

    @Benchmark
    public int str() {
        return str.hashCode();
    }

    @Benchmark
    public int inlStr() {
        return inlStr.hashCode();
    }
}
