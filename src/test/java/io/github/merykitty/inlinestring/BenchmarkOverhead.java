package io.github.merykitty.inlinestring;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
public class BenchmarkOverhead {
    Random random = new Random();

    HashMap<String, Integer> normalMap;
    InlineStringHashMap<Integer> valMap;
    FastStringMap<Integer> fastMap;

    char[][] keys;
    int[] values;

    char[] existKey;
    char[] nonExistKey;
    char[] putKey;
    int putValue;
    int stuff;

    String initString;
    InlineString initPrimitiveString;

    @Setup(Level.Trial)
    public void setUp() {
        normalMap = new HashMap<>();
        valMap = new InlineStringHashMap<>();
        fastMap = new FastStringMap<>();
        keys = new char[1000][10];
        for (int i = 0; i < 1000; i++) {
            for (int j = 0; j < 10; j++) {
                keys[i][j] = (char)(random.nextInt(26) + 'a');
            }
        }

        values = new int[1000];
        for (int i = 0; i < 1000; i++) {
            values[i] = random.nextInt();
        }

        for (int i = 0; i < 1000; i++) {
            normalMap.put(new String(keys[i]), values[i]);
        }

        for (int i = 0; i < 1000; i++) {
            valMap.putPrimitive(new InlineString(keys[i]), values[i]);
        }

        for (int i = 0; i < 1000; i++) {
            fastMap.putInline(new InlineString(keys[i]), values[i]);
        }
    }

    @Setup(Level.Trial)
    public void setUpGetKey() {
        int index = random.nextInt(1000);
        existKey = keys[index];
    }

    @Setup(Level.Trial)
    public void setUpPutKey() {
        putKey = new char[10];
        for (int i = 0; i < 10; i++) {
            putKey[i] = (char)(random.nextInt(26) + 'a');
            putValue = random.nextInt();
        }
    }

    @Benchmark
    public void getNormalExist() {
        stuff = normalMap.get(new String(existKey));
    }

    @Benchmark
    public void getValExist() {
        stuff = valMap.getPrimitive(new InlineString(existKey));
    }

    @Benchmark
    public void getRefExist() {
        stuff = fastMap.getInline(new InlineString(existKey));
    }

    @Benchmark
    public void putNormal() {
        normalMap.put(new String(putKey), putValue);
    }

    @Benchmark
    public void putVal() {
        valMap.putPrimitive(new InlineString(putKey), putValue);
    }

    @Benchmark
    public void putRef() {
        fastMap.putInline(new InlineString(putKey), putValue);
    }

    @Benchmark
    public void initNormal() {
        initString = new String(putKey);
    }

    @Benchmark
    public void initVal() {
        initPrimitiveString = new InlineString(putKey);
    }

    public boolean normalNonExist() {
        return normalMap.get(new String(nonExistKey)) == null;
    }

    public boolean valNonExist() {
        return valMap.getPrimitive(new InlineString(nonExistKey)) == null;
    }

    public static void main(String[] args) throws IOException {
        org.openjdk.jmh.Main.main(args);
    }
}
