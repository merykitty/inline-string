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
public class GetCharsBenchmark {
    @Param({"Hello world!", "This is an example of a Latin1 long string", "Đây là một UTF16 String"})
    private String param;

    String str;
    InlineString inlStr;
    char[] result;

    @Setup(Level.Trial)
    public void setUp() {
        this.str = new String(param);
        this.inlStr = new InlineString(param);
        result = new char[256];
    }

    @Benchmark
    public void str() {
        str.getChars(0, str.length(), result, 0);
    }

    @Benchmark
    public void inlStr() {
        inlStr.getChars(0, str.length(), result, 0);
    }
}
