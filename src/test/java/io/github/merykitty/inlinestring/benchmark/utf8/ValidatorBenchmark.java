package io.github.merykitty.inlinestring.benchmark.utf8;

import io.github.merykitty.inlinestring.utf8.Validator;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@State(Scope.Benchmark)
@Fork(value = 1)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
public class ValidatorBenchmark {
    @Param({"./twitter.json"})//, "./utf8-demo.txt", "./utf8-demo-invalid.txt", "./20k.txt"})
    String testFile;

    byte[] buf;

    @Setup(Level.Trial)
    public void setup() throws IOException {
        buf = Files.readAllBytes(Path.of(testFile));
    }

//    @Benchmark
    public String jdk() {
        return new String(buf, StandardCharsets.UTF_8);
    }


    @Benchmark
    public boolean vector() {
        return Validator.validate(buf);
    }

    public static void main(String[] args) throws IOException {
        String[] resources = {"./twitter.json", "./utf8-demo.txt", "./utf8-demo-invalid.txt", "./20k.txt"};
        for (String resource : resources) {
            var buf = Files.readAllBytes(Path.of(resource));
            System.out.println(resource + ": " + Validator.validate(buf));
        }
    }
}
