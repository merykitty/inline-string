package io.github.merykitty.inlinestring.test;

import io.github.merykitty.inlinestring.InlineString;
import static io.github.merykitty.inlinestring.test.Utils.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.Charset;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ConstructorTest {

    @Test
    public void empty() {
        assertEquals("", new InlineString().toString());
    }

    public static Stream<String> str() {
        return DATA.stream();
    }

    @ParameterizedTest
    @MethodSource
    public void str(String str) {
        var inlStr = new InlineString(str);
        assertEquals(str, inlStr.toString());
    }

    public static Stream<char[]> charArray() {
        return DATA.stream().map(String::toCharArray);
    }

    @ParameterizedTest
    @MethodSource
    public void charArray(char[] data) {
        assertEquals(new String(data), new InlineString(data).toString());
    }

    record CharArrayIntInt(char[] value, int offset, int count) {}
    public static Stream<CharArrayIntInt> charArrayIntInt() {
        var random = RandomGenerator.getDefault();
        return DATA.stream().map(String::toCharArray)
                .map(s -> {
                    var offset = random.nextInt(s.length + 1);
                    int count = random.nextInt(s.length + 1 - offset);
                    return new CharArrayIntInt(s, offset, count);
                });
    }

    @ParameterizedTest
    @MethodSource
    public void charArrayIntInt(CharArrayIntInt data) {
        var str = new String(data.value(), data.offset(), data.count());
        var inlStr = new InlineString(data.value(), data.offset(), data.count());
        assertEquals(str, inlStr.toString());
    }

    record IntArrayIntInt(int[] codePoints, int offset, int count) {}
    public static Stream<IntArrayIntInt> intArrayIntInt() {
        var random = RandomGenerator.getDefault();
        return DATA.stream().map(String::codePoints)
                .map(IntStream::toArray)
                .map(s -> {
                    int offset = random.nextInt(s.length + 1);
                    int count = random.nextInt(s.length + 1 - offset);
                    return new IntArrayIntInt(s, offset, count);
                });
    }

    @ParameterizedTest
    @MethodSource
    public void intArrayIntInt(IntArrayIntInt data) {
        var str = new String(data.codePoints(), data.offset(), data.count());
        var inlStr = new InlineString(data.codePoints(), data.offset(), data.count());
        assertEquals(str, inlStr.toString());
    }

    record ByteArrayIntIntString(byte[] bytes, int offset, int length, String charsetName) {}
    public static Stream<ByteArrayIntIntString> byteArrayIntIntString() {
        var random = RandomGenerator.getDefault();
        return DATA.stream().map(s -> {
            var charset = CHARSETS.get(random.nextInt(CHARSETS.size()));
            var bytes = s.getBytes(charset);
            int offset = random.nextInt(bytes.length + 1);
            int count = random.nextInt(bytes.length + 1 - offset);
            return new ByteArrayIntIntString(bytes, offset, count, charset.name());
        });
    }

    @ParameterizedTest
    @MethodSource
    public void byteArrayIntIntString(ByteArrayIntIntString data) {
        assertSimilarExecutions(() -> new String(data.bytes(), data.offset(), data.length(), data.charsetName()),
                () -> new InlineString(data.bytes(), data.offset(), data.length(), data.charsetName()).toString());
    }

    record ByteArrayIntIntCharset(byte[] bytes, int offset, int length, Charset charset) {}
    public static Stream<ByteArrayIntIntCharset> byteArrayIntIntCharset() {
        var random = RandomGenerator.getDefault();
        return DATA.stream().map(s -> {
            var charset = CHARSETS.get(random.nextInt(CHARSETS.size()));
            var bytes = s.getBytes(charset);
            int offset = random.nextInt(bytes.length + 1);
            int count = random.nextInt(bytes.length + 1 - offset);
            return new ByteArrayIntIntCharset(bytes, offset, count, charset);
        });
    }

    @ParameterizedTest
    @MethodSource
    public void byteArrayIntIntCharset(ByteArrayIntIntCharset data) {
        assertSimilarExecutions(() -> new String(data.bytes(), data.offset(), data.length(), data.charset()),
                () -> new InlineString(data.bytes(), data.offset(), data.length(), data.charset()).toString());
    }

    record ByteArrayString(byte[] bytes, String charsetName) {}
    public static Stream<ByteArrayString> byteArrayString() {
        var random = RandomGenerator.getDefault();
        return DATA.stream().map(s -> {
            var charset = CHARSETS.get(random.nextInt(CHARSETS.size()));
            var bytes = s.getBytes(charset);
            return new ByteArrayString(bytes, charset.name());
        });
    }

    @ParameterizedTest
    @MethodSource
    public void byteArrayString(ByteArrayString data) {
        assertSimilarExecutions(() -> new String(data.bytes(), data.charsetName()),
                () -> new InlineString(data.bytes(), data.charsetName()).toString());
    }

    record ByteArrayCharset(byte[] bytes, Charset charset) {}
    public static Stream<ByteArrayCharset> byteArrayCharset() {
        var random = RandomGenerator.getDefault();
        return DATA.stream().map(s -> {
            var charset = CHARSETS.get(random.nextInt(CHARSETS.size()));
            var bytes = s.getBytes(charset);
            return new ByteArrayCharset(bytes, charset);
        });
    }

    @ParameterizedTest
    @MethodSource
    public void byteArrayCharset(ByteArrayCharset data) {
        assertSimilarExecutions(() -> new String(data.bytes(), data.charset()),
                () -> new InlineString(data.bytes(), data.charset()).toString());
    }

    public static Stream<byte[]> byteArray() {
        return DATA.stream().map(String::getBytes);
    }

    @ParameterizedTest
    @MethodSource
    public void byteArray(byte[] bytes) {
        assertEquals(new String(bytes), new InlineString(bytes).toString());
    }

    public static Stream<StringBuffer> strBuffer() {
        return DATA.stream().map(StringBuffer::new);
    }

    @ParameterizedTest
    @MethodSource
    public void strBuffer(StringBuffer buffer) {
        assertEquals(new String(buffer), new InlineString(buffer).toString());
    }

    public static Stream<StringBuilder> strBuilder() {
        return DATA.stream().map(StringBuilder::new);
    }

    @ParameterizedTest
    @MethodSource
    public void strBuilder(StringBuilder builder) {
        assertEquals(new String(builder), new InlineString(builder).toString());
    }
}
