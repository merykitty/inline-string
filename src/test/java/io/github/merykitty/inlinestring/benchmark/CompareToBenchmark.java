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
public class CompareToBenchmark {
    @Param({"Hi!", "This is an example of a Latin1 long string", "Đây là một UTF16 String"}) //
    private String param1;

    @Param({"Hello World!", "This is a very longgg Latin1 String", "UTF16 ngắn"})
    private String param2;

    private String str1, str2;
    private InlineString inlStr1, inlStr2;

    static int dumpStr;
    static int dumpInlStr;

    @Setup(Level.Trial)
    public void warmup() {
        for (int i = 0; i < 100_000; i++) {
            var random = RandomGenerator.getDefault();
            int length = random.nextInt(16 * 2);
            char[] array1 = new char[length];
            for (int j = 0; j < length; j++) {
                array1[j] = (char)random.nextInt('a', 'z');
            }
            length = random.nextInt(16 * 2);
            char[] array2 = new char[length];
            for (int j = 0; j < length; j++) {
                array2[j] = (char)random.nextInt('a', 'z');
            }
            str1 = new String(array1);
            str2 = new String(array2);
            inlStr1 = new InlineString(array1);
            inlStr2 = new InlineString(array2);
            dumpStr = str();
            dumpInlStr = inlStr();
            length = random.nextInt(16 * 2);
            array2 = new char[length];
            for (int j = 0; j < length; j++) {
                array2[j] = (char)random.nextInt(1000);
            }
            str2 = new String(array2);
            inlStr2 = new InlineString(array2);
            dumpStr = str();
            dumpInlStr = inlStr();
        }
    }

    @Setup(Level.Iteration)
    public void setup() {
        str1 = new String(param1);
        str2 = new String(param2);
        inlStr1 = new InlineString(param1);
        inlStr2 = new InlineString(param2);
    }

    @Benchmark
    public int str() {
        return str1.compareTo(str2);
    }

    @Benchmark
    public int inlStr() {
        return inlStr1.compareTo(inlStr2);
    }
}
