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
public class GetCharsBenchmark {
    @Param({"Hello world!", "This is an example of a Latin1 long string", "Đây là một UTF16 String"})
    private String param;

    String str;
    InlineString inlStr;
    char[] result = new char[256];
    int srcBegin, srcEnd;

    @Setup(Level.Trial)
    public void warmup() {
        for (int i = 0; i < 100_000; i++) {
            var random = RandomGenerator.getDefault();
            int length = random.nextInt(16 * 2);
            char[] array = new char[length];
            for (int j = 0; j < length; j++) {
                array[j] = (char)random.nextInt('a', 'z');
            }
            str = new String(array);
            inlStr = new InlineString(array);
            int first = random.nextInt(length);
            int second = random.nextInt(length);
            srcBegin = Math.min(first, second);
            srcEnd = Math.max(first, second);
            str();
            inlStr();
        }
    }

    @Setup(Level.Trial)
    public void setUp() {
        this.str = new String(param);
        this.inlStr = new InlineString(param);
        int first = RandomGenerator.getDefault().nextInt(param.length());
        int second = RandomGenerator.getDefault().nextInt(param.length());
        srcBegin = Math.min(first, second);
        srcEnd = Math.max(first, second);
    }

    @Benchmark
    public void str() {
        str.getChars(srcBegin, srcEnd, result, 3);
    }

    @Benchmark
    public void inlStr() {
        inlStr.getChars(srcBegin, srcEnd, result, 3);
    }
}
