package io.github.merykitty.inlinestring.benchmark;

import io.github.merykitty.inlinestring.InlineString;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;

@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
public class IndexOfCharBenchmark {
    @Param({"Hello world!", "This is an example of a Latin1 long string", "Đây là một UTF16 String"})
    private String param;

    String str;
    InlineString inlStr;
    int ch;
    int fromIndex;

    int dumpStr;
    int dumpInlStr;

    @Setup(Level.Trial)
    public void warmup() {
        var random = RandomGenerator.getDefault();
        for (int i = 0; i < 100_000; i++) {
            int length = random.nextInt(16 * 2);
            char[] array = new char[length];
            for (int j = 0; j < length; j++) {
                array[j] = (char)random.nextInt('a', 'z');
            }
            str = new String(array);
            inlStr = new InlineString(array);
            ch = random.nextInt('a', 'z');
            fromIndex = random.nextInt(16);
            dumpStr = str.indexOf(ch, fromIndex);
            dumpInlStr = str.indexOf(ch, fromIndex);
        }
    }

    @Setup(Level.Iteration)
    public void setup() {
        str = new String(param);
        inlStr = new InlineString(param);
        ch = 'o';
        fromIndex = 3;
    }

    @Benchmark
    public void str() {
        dumpStr = str.indexOf(ch, fromIndex);
    }

    @Benchmark
    public void inlStr() {
        dumpInlStr = inlStr.indexOf(ch, fromIndex);
    }
}
