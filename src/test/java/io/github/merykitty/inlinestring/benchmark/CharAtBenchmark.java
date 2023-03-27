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
public class CharAtBenchmark {
    @Param({"Hello World!", "This is an example of a Latin1 long string", "Đây là một UTF16 String"})
    private String param;

    String str;
    InlineString inlStr;
    int index;

    int dumpStr;
    int dumpInlStr;

    @Setup(Level.Trial)
    public void warmup() {
        for (int i = 0; i < 100_000; i++) {
            var random = RandomGenerator.getDefault();
            int length = random.nextInt(16 * 2);
            char[] array = new char[length];
            for (int j = 0; j < length; j++) {
                array[j] = (char)random.nextInt('a', 'z');
            }
            index = random.nextInt(length);
            str = new String(array);
            inlStr = new InlineString(array);
            dumpStr = str();
            dumpInlStr = inlStr();
        }
    }

    @Setup(Level.Iteration)
    public void setup() {
        str = new String(param);
        inlStr = new InlineString(param);
        index = 0;
    }

    @Benchmark
    public int str() {
        if (++index == str.length()) {
            index = 0;
        }
        return str.charAt(index);
    }

    @Benchmark
    public int inlStr() {
        if (++index == inlStr.length()) {
            index = 0;
        }
        return inlStr.charAt(index);
    }

}
