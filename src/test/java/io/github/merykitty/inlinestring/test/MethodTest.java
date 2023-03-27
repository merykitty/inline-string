package io.github.merykitty.inlinestring.test;

import io.github.merykitty.inlinestring.InlineString;
import static io.github.merykitty.inlinestring.test.Utils.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;

public class MethodTest {
    record EmptyData(String str, InlineString inlStr) {}

    public static Stream<EmptyData> emptyData() {
        return DATA.stream().map(s -> new EmptyData(s, new InlineString(s)));
    }

    @ParameterizedTest
    @MethodSource("emptyData")
    public void length(EmptyData data) {
        assertEquals(data.str().length(), data.inlStr().length());
    }

    @ParameterizedTest
    @MethodSource("emptyData")
    public void isEmpty(EmptyData data) {
        assertEquals(data.str().isEmpty(), data.inlStr().isEmpty());
    }

    record IndexData(String str, InlineString inlStr, int i) {}
    public static Stream<IndexData> indexData() {
        var random = RandomGenerator.getDefault();
        return DATA.stream().map(s -> {
            int index = random.nextInt(s.length() + 1);
            return new IndexData(s, new InlineString(s), index);
        });
    }

    @ParameterizedTest
    @MethodSource("indexData")
    public void charAt(IndexData data) {
        int index = data.i();
        assertSimilarExecutions(() -> data.str().charAt(index),
                () -> data.inlStr().charAt(index));
    }

    @ParameterizedTest
    @MethodSource("indexData")
    public void codePointAt(IndexData data) {
        int index = data.i();
        assertSimilarExecutions(() -> data.str().codePointAt(index),
                () -> data.inlStr().codePointAt(index));
    }

    @ParameterizedTest
    @MethodSource("indexData")
    public void codePointBefore(IndexData data) {
        int index = data.i();
        assertSimilarExecutions(() -> data.str().codePointBefore(index),
                () -> data.inlStr().codePointBefore(index));
    }

    record IndexIndexData(String str, InlineString inlStr, int beginIndex, int endIndex) {}
    public static Stream<IndexIndexData> indexIndexData() {
        var random = RandomGenerator.getDefault();
        return DATA.stream().map(s -> {
            int i0 = random.nextInt(s.length() + 1);
            int i1 = random.nextInt(s.length() + 1);
            int beginIndex = Math.min(i0, i1);
            int endIndex = Math.max(i0, i1);
            return new IndexIndexData(s, new InlineString(s), beginIndex, endIndex);
        });
    }

    @ParameterizedTest
    @MethodSource("indexIndexData")
    public void codePointCount(IndexIndexData data) {
        int beginIndex = data.beginIndex();
        int endIndex = data.endIndex();
        assertSimilarExecutions(() -> data.str().codePointCount(beginIndex, endIndex),
                () -> data.inlStr().codePointCount(beginIndex, endIndex));
    }

    record OffsetByCodePointsData(String str, InlineString inlStr, int index, int codePointOffset) {}
    public static Stream<OffsetByCodePointsData> offsetByCodePoints() {
        var random = RandomGenerator.getDefault();
        return DATA.stream().map(s -> {
            int i0 = random.nextInt(s.length() + 1);
            int i1 = random.nextInt(s.length() + 1);
            int beginIndex = Math.min(i0, i1);
            int endIndex = Math.max(i0, i1);
            return new OffsetByCodePointsData(s, new InlineString(s), beginIndex, endIndex);
        });
    }

    @ParameterizedTest
    @MethodSource
    public void offsetByCodePoints(OffsetByCodePointsData data) {
        int index = data.index();
        int codePointOffset = data.codePointOffset();
        assertSimilarExecutions(() -> data.str().offsetByCodePoints(index, codePointOffset),
                () -> data.inlStr().offsetByCodePoints(index, codePointOffset));
    }

    record GetCharsData(String str, InlineString inlStr, int srcBegin, int srcEnd, char[] dst, int dstBegin) {}
    public static Stream<GetCharsData> getChars() {
        var random = RandomGenerator.getDefault();
        return DATA.stream().map(s -> {
            int i0 = random.nextInt(s.length() + 1);
            int i1 = random.nextInt(s.length() + 1);
            int srcBegin = Math.min(i0, i1);
            int srcEnd = Math.max(i0, i1);
            int dstBegin = random.nextInt(10);
            char[] dst = new char[Math.max(dstBegin + srcEnd - srcBegin + random.nextInt(-1, 1), 0)];
            return new GetCharsData(s, new InlineString(s), srcBegin, srcEnd, dst, dstBegin);
        });
    }

    @ParameterizedTest
    @MethodSource
    public void getChars(GetCharsData data) {
        assertSimilarExecutions(() -> {
                char[] dst = Arrays.copyOf(data.dst(), data.dst().length);
                data.str().getChars(data.srcBegin(), data.srcEnd(), dst, data.dstBegin());
                return dst;
            }, () -> {
                char[] dst = Arrays.copyOf(data.dst(), data.dst().length);
                data.inlStr().getChars(data.srcBegin(), data.srcEnd(), dst, data.dstBegin());
                return dst;
            });
    }

    record GetBytesStringData(String str, InlineString inlStr, String charsetName) {}
    public static Stream<GetBytesStringData> getBytesString() {
        var random = RandomGenerator.getDefault();
        return DATA.stream().map(s -> new GetBytesStringData(s, new InlineString(s),
                CHARSETS.get(random.nextInt(CHARSETS.size())).name()));
    }

    @ParameterizedTest
    @MethodSource("getBytesString")
    public void getBytes(GetBytesStringData data) throws UnsupportedEncodingException {
        assertArrayEquals(data.str().getBytes(data.charsetName()),
                data.inlStr().getBytes(data.charsetName()));
    }

    record GetBytesCharsetData(String str, InlineString inlStr, Charset charset) {}
    public static Stream<GetBytesCharsetData> getBytesCharset() {
        var random = RandomGenerator.getDefault();
        return DATA.stream().map(s -> new GetBytesCharsetData(s, new InlineString(s),
                CHARSETS.get(random.nextInt(CHARSETS.size()))));
    }

    @ParameterizedTest
    @MethodSource("getBytesCharset")
    public void getBytes(GetBytesCharsetData data) {
        assertArrayEquals(data.str().getBytes(data.charset()),
                data.inlStr().getBytes(data.charset()));
    }

    @ParameterizedTest
    @MethodSource("emptyData")
    public void getBytes(EmptyData data) {
        assertArrayEquals(data.str().getBytes(), data.inlStr().getBytes());
    }

    record CompareData(String str0, String str1, InlineString inlStr0, InlineString inlStr1) {}
    public static Stream<CompareData> compareData() {
        return DATA.stream().mapMulti((s0, c) -> DATA.forEach(s1 -> {
            var random = RandomGenerator.getDefault();
            if (random.nextBoolean()) {
                s1 = s1.toLowerCase();
            }
            c.accept(new CompareData(s0, s1, new InlineString(s0), new InlineString(s1)));
        }));
    }

    @ParameterizedTest
    @MethodSource("compareData")
    public void equals(CompareData data) {
        assertEquals(data.str0().equals(data.str1()), data.inlStr0().equals(data.inlStr1()));
    }

    record ContentEqualsData(String str, InlineString inlStr, CharSequence cs) {}
    public static Stream<ContentEqualsData> contentEquals() {
        var random = RandomGenerator.getDefault();
        return DATA.stream().mapMulti((s0, c) -> DATA.forEach(s1 -> {
            int type = random.nextInt(4);
            CharSequence cs;
            if (type == 0) {
                cs = s1;
            } else if (type == 1) {
                cs = new StringBuffer(s1);
            } else if (type == 2) {
                cs = new StringBuilder(s1);
            } else {
                cs = new InlineString(s1);
            }
            c.accept(new ContentEqualsData(s0, new InlineString(s0), cs));
        }));
    }

    @ParameterizedTest
    @MethodSource
    public void contentEquals(ContentEqualsData data) {
        assertEquals(data.str().contentEquals(data.cs()),
                data.inlStr().contentEquals(data.cs()));
    }

    @ParameterizedTest
    @MethodSource("compareData")
    public void equalsIgnoreCase(CompareData data) {
        assertEquals(data.str0().equalsIgnoreCase(data.str1()),
                data.inlStr0().equalsIgnoreCase(data.inlStr1()));
    }

    @ParameterizedTest
    @MethodSource("compareData")
    public void compareTo(CompareData data) {
        assertEquals(data.str0().compareTo(data.str1()),
                data.inlStr0().compareTo(data.inlStr1()),
                "Parameters: <" + data.str0() + ">, <" + data.str1() + ">");
    }

    @ParameterizedTest
    @MethodSource("compareData")
    public void compareToIgnoreCase(CompareData data) {
        assertEquals(data.str0().compareToIgnoreCase(data.str1()),
                data.inlStr0().compareToIgnoreCase(data.inlStr1()));
    }

    record RegionMatchesData(String str0, String str1, InlineString inlStr0, InlineString inlStr1,
            boolean ignoreCase, int toffset, int ooffset, int len) {}
    public static Stream<RegionMatchesData> regionMatches() {
        return DATA.stream().mapMulti((s0, c) -> DATA.forEach(s1 -> {
            var random = RandomGenerator.getDefault();
            boolean ignoreCase = random.nextBoolean();
            int i0 = random.nextInt(s0.length() + 1);
            int i1 = random.nextInt(s0.length() + 1);
            int toffset = Math.min(i0, i1);
            int len = Math.abs(i0 - i1);
            int ooffset = Math.min(random.nextInt(s1.length() + 1),
                    random.nextInt(s1.length() + 1));
            c.accept(new RegionMatchesData(s0, s1, new InlineString(s0), new InlineString(s1),
                    ignoreCase, toffset, ooffset, len));
        }));
    }

    @ParameterizedTest
    @MethodSource
    public void regionMatches(RegionMatchesData data) {
        assertEquals(data.str0().regionMatches(data.ignoreCase(),
                data.toffset(), data.str1(), data.ooffset(), data.len()),
                data.inlStr0().regionMatches(data.ignoreCase(),
                data.toffset(), data.inlStr1(), data.ooffset(), data.len()),
                "Parameters: <" + data.str0() + ">, <" + data.str1() + ">");
    }

    record StartsWithStringIntData(String str0, String str1,
                                   InlineString inlStr0, InlineString inlStr1, int toffset) {}
    public static Stream<StartsWithStringIntData> startsWithStringInt() {
        return DATA.stream().mapMulti((s0, c) -> DATA.forEach(s1 -> {
            var random = RandomGenerator.getDefault();
            int i0 = random.nextInt(s1.length() + 1);
            int i1 = random.nextInt(s1.length() + 1);
            int beginIndex = Math.min(i0, i1);
            int endIndex = Math.max(i0, i1);
            String s2 = s1.substring(beginIndex, endIndex);
            int toffset = random.nextInt(s0.length() + 1);
            if (s0.equals(s1)) {
                if (random.nextBoolean()) {
                    toffset = beginIndex;
                }
            }
            c.accept(new StartsWithStringIntData(s0, s2, new InlineString(s0), new InlineString(s2),
                    toffset));
        }));
    }

    @ParameterizedTest
    @MethodSource("startsWithStringInt")
    public void startsWith(StartsWithStringIntData data) {
        assertEquals(data.str0().startsWith(data.str1(), data.toffset()),
                data.inlStr0().startsWith(data.inlStr1(), data.toffset()),
                "Parameters: <" + data.str0() + ">, <" + data.str1() + ">");
    }

    record StartsWithStringData(String str0, String str1, InlineString inlStr0, InlineString inlStr1) {}
    public static Stream<StartsWithStringData> startsWithString() {
        return DATA.stream().mapMulti((s0, c) -> DATA.forEach(s1 -> {
            var random = RandomGenerator.getDefault();
            int i0 = random.nextInt(s1.length() + 1);
            int i1 = random.nextInt(s1.length() + 1);
            int beginIndex = Math.min(i0, i1);
            int endIndex = Math.max(i0, i1);
            if (s0.equals(s1)) {
                if (random.nextBoolean()) {
                    beginIndex = 0;
                }
            }
            String s2 = s1.substring(beginIndex, endIndex);
            c.accept(new StartsWithStringData(s0, s2, new InlineString(s0), new InlineString(s2)));
        }));
    }

    @ParameterizedTest
    @MethodSource("startsWithString")
    public void startsWith(StartsWithStringData data) {
        assertEquals(data.str0().startsWith(data.str1()), data.inlStr0().startsWith(data.inlStr1()), "Parameters: <" + data.str0() + ">, <" + data.str1() + ">");
    }

    record EndsWithData(String str0, String str1, InlineString inlStr0, InlineString inlStr1) {}
    public static Stream<EndsWithData> endsWith() {
        return DATA.stream().mapMulti((s0, c) -> DATA.forEach(s1 -> {
            var random = RandomGenerator.getDefault();
            int i0 = random.nextInt(s1.length() + 1);
            int i1 = random.nextInt(s1.length() + 1);
            int beginIndex = Math.min(i0, i1);
            int endIndex = Math.max(i0, i1);
            if (s0.equals(s1)) {
                if (random.nextBoolean()) {
                    endIndex = s1.length();
                }
            }
            String s2 = s1.substring(beginIndex, endIndex);
            c.accept(new EndsWithData(s0, s2, new InlineString(s0), new InlineString(s2)));
        }));
    }

    @ParameterizedTest
    @MethodSource
    public void endsWith(EndsWithData data) {
        assertEquals(data.str0().endsWith(data.str1()), data.inlStr0().endsWith(data.inlStr1()));
    }

    @ParameterizedTest
    @MethodSource("emptyData")
    public void hashCode(EmptyData data) {
        assertEquals(data.str().hashCode(), data.inlStr().hashCode());
    }

    record CodePointData(String str, InlineString inlStr, int ch) {}
    public static Stream<CodePointData> codePointData() {
        return DATA.stream().map(s -> {
            var random = RandomGenerator.getDefault();
            int ch;
            if (random.nextBoolean() && !s.isEmpty()) {
                int index = random.nextInt(s.length());
                ch = s.charAt(index);
            } else {
                ch = random.nextInt();
            }
            return new CodePointData(s, new InlineString(s), ch);
        });
    }

    @ParameterizedTest
    @MethodSource("codePointData")
    public void indexOf(CodePointData data) {
        assertEquals(data.str().indexOf(data.ch()), data.inlStr().indexOf(data.ch()),
                "Parameters: <" + data.str() + ">, <" + data.inlStr() + ">, <" + (char)data.ch() + ">");
    }

    record CodePointIndexData(String str, InlineString inlStr, int ch, int index) {}
    public static Stream<CodePointIndexData> codePointIndexData() {
        return DATA.stream().map(s -> {
            var random = RandomGenerator.getDefault();
            int index = random.nextInt(s.length() + 1);
            int ch;
            if (random.nextBoolean() && !s.isEmpty()) {
                int i = random.nextInt(s.length());
                ch = s.charAt(i);
            } else {
                ch = random.nextInt();
            }
            return new CodePointIndexData(s, new InlineString(s), ch, index);
        });
    }

    @ParameterizedTest
    @MethodSource("codePointIndexData")
    public void indexOf(CodePointIndexData data) {
        assertEquals(data.str().indexOf(data.ch(), data.index()),
                data.inlStr().indexOf(data.ch(), data.index()),
                "Parameters: <" + data.str() + ">, <" + data.inlStr() + ">, <" + (char)data.ch() + ">, <" + data.index() + ">");
    }

    @ParameterizedTest
    @MethodSource("codePointData")
    public void lastIndexOf(CodePointData data) {
        assertEquals(data.str().lastIndexOf(data.ch()), data.inlStr().lastIndexOf(data.ch()),
                "Parameters: <" + data.str() + ">, <" + data.inlStr() + ">, <" + (char)data.ch() + ">");
    }

    @ParameterizedTest
    @MethodSource("codePointIndexData")
    public void lastIndexOf(CodePointIndexData data) {
        assertEquals(data.str().lastIndexOf(data.ch(), data.index()),
                data.inlStr().lastIndexOf(data.ch(), data.index()),
                "Parameters: <" + data.str() + ">, <" + data.inlStr() + ">, <" + (char)data.ch() + ">, <" + data.index() + ">");
    }

    record IndexOfStringData(String str0, String str1, InlineString inlStr0, InlineString inlStr1) {}
    public static Stream<IndexOfStringData> indexOfStringData() {
        return DATA.stream().mapMulti((s0, c) -> DATA.forEach(s1 -> {
            var random = RandomGenerator.getDefault();
            int i0 = random.nextInt(s1.length() + 1);
            int i1 = random.nextInt(s1.length() + 1);
            int beginIndex = Math.min(i0, i1);
            int endIndex = Math.max(i0, i1);
            String s2 = s1.substring(beginIndex, endIndex);
            c.accept(new IndexOfStringData(s0, s2, new InlineString(s0), new InlineString(s2)));
        }));
    }

    @ParameterizedTest
    @MethodSource("indexOfStringData")
    public void indexOf(IndexOfStringData data) {
        assertEquals(data.str0().indexOf(data.str1()), data.inlStr0().indexOf(data.inlStr1()),
                "Parameters: <" + data.str0() + ">, <" + data.str1() + ">");
    }

    record IndexOfStringIntData(String str0, String str1, InlineString inlStr0, InlineString inlStr1, int fromIndex) {}
    public static Stream<IndexOfStringIntData> indexOfStringIntData() {
        return DATA.stream().mapMulti((s0, c) -> DATA.forEach(s1 -> {
            var random = RandomGenerator.getDefault();
            int i0 = random.nextInt(s1.length() + 1);
            int i1 = random.nextInt(s1.length() + 1);
            int beginIndex = Math.min(i0, i1);
            int endIndex = Math.max(i0, i1);
            int fromIndex = Math.min(random.nextInt(s0.length() + 1),
                    random.nextInt(s0.length() + 1));
            String s2 = s1.substring(beginIndex, endIndex);
            c.accept(new IndexOfStringIntData(s0, s2, new InlineString(s0), new InlineString(s2), fromIndex));
        }));
    }

    @ParameterizedTest
    @MethodSource("indexOfStringIntData")
    public void indexOf(IndexOfStringIntData data) {
        assertEquals(data.str0().indexOf(data.str1(), data.fromIndex()),
                data.inlStr0().indexOf(data.inlStr1(), data.fromIndex()),
                "Parameters: <" + data.str0() + ">, <" + data.str1() + ">, <" + data.fromIndex() + ">");
    }

    @ParameterizedTest
    @MethodSource("indexOfStringData")
    public void lastIndexOf(IndexOfStringData data) {
        assertEquals(data.str0().lastIndexOf(data.str1()), data.inlStr0().lastIndexOf(data.inlStr1()));
    }

    @ParameterizedTest
    @MethodSource("indexOfStringIntData")
    public void lastIndexOf(IndexOfStringIntData data) {
        assertEquals(data.str0().lastIndexOf(data.str1(), data.fromIndex()),
                data.inlStr0().lastIndexOf(data.inlStr1(), data.fromIndex()));
    }

    @ParameterizedTest
    @MethodSource("indexData")
    public void substring(IndexData data) {
        assertEquals(data.str().substring(data.i()), data.inlStr().substring(data.i()).toString());
    }

    @ParameterizedTest
    @MethodSource("indexIndexData")
    public void substring(IndexIndexData data) {
        assertEquals(data.str().substring(data.beginIndex(), data.endIndex()),
                data.inlStr().substring(data.beginIndex(), data.endIndex()).toString());
    }

    @ParameterizedTest
    @MethodSource("indexIndexData")
    public void subSequence(IndexIndexData data) {
        assertEquals(data.str().subSequence(data.beginIndex(), data.endIndex()),
                data.inlStr().subSequence(data.beginIndex(), data.endIndex()).toString());
    }

    record ConcatData(String str0, String str1, InlineString inlStr0, InlineString inlStr1) {}
    public static Stream<ConcatData> concat() {
        return DATA.stream().mapMulti((s0, c) -> DATA.forEach(s1 ->
                c.accept(new ConcatData(s0, s1, new InlineString(s0), new InlineString(s1)))));
    }

    @ParameterizedTest
    @MethodSource
    public void concat(ConcatData data) {
        assertEquals(data.str0().concat(data.str1()), data.inlStr0().concat(data.inlStr1()).toString());
    }

    record ReplaceCharData(String str, InlineString inlStr, char oldChar, char newChar) {}
    public static Stream<ReplaceCharData> replaceChar() {
        return DATA.stream().map(s -> {
            var random = RandomGenerator.getDefault();
            char oldChar = (char)random.nextInt('a', 'z' + 1);
            char newChar = (char)random.nextInt('a', 'z' + 1);
            return new ReplaceCharData(s, new InlineString(s), oldChar, newChar);
        });
    }

    @ParameterizedTest
    @MethodSource("replaceChar")
    public void replace(ReplaceCharData data) {
        assertEquals(data.str().replace(data.oldChar(), data.newChar()),
                data.inlStr().replace(data.oldChar(), data.newChar()).toString());
    }

    @ParameterizedTest
    @MethodSource("indexOfStringData")
    public void contains(IndexOfStringData data) {
        assertEquals(data.str0().contains(data.str1()), data.inlStr0().contains(data.str1()));
        assertEquals(data.str0().contains(data.inlStr1()), data.inlStr0().contains(data.inlStr1()));
        assertEquals(data.str0().contains(data.str1()), data.inlStr0().contains(data.inlStr1()));
        assertEquals(data.str0().contains(data.inlStr1()), data.inlStr0().contains(data.str1()));
    }

    record ReplaceCharSequenceData(String str, InlineString inlStr, CharSequence target, CharSequence replacement) {}
    public static Stream<ReplaceCharSequenceData> replaceCharSequenceData() {
        return DATA.stream().mapMulti((s0, c) -> DATA.forEach(s1 -> {
            var random = RandomGenerator.getDefault();
            int i0 = random.nextInt(s1.length() + 1);
            int i1 = random.nextInt(s1.length() + 1);
            int beginIndex = Math.min(i0, i1);
            int endIndex = Math.max(i0, i1);
            var target = s1.substring(beginIndex, endIndex);
            String replacement;
            if (random.nextBoolean()) {
                replacement = "";
            } else {
                replacement = "Hello";
            }
            c.accept(new ReplaceCharSequenceData(s0, new InlineString(s0), target, replacement));
        }));
    }

    @ParameterizedTest
    @MethodSource("replaceCharSequenceData")
    public void replace(ReplaceCharSequenceData data) {
        assertEquals(data.str().replace(data.target(), data.replacement()),
                data.inlStr().replace(data.target(), data.replacement()).toString());
    }

    record SplitStringIntData(String str, InlineString inlStr, String regex, int limit) {}
    public static Stream<SplitStringIntData> splitStringIntData() {
        return DATA.stream().mapMulti((s, c) -> {
            var random = RandomGenerator.getDefault();
            String oneChar = String.valueOf((char)random.nextInt('a', 'z' + 1));
            String twoChar = String.valueOf((char)random.nextInt('a', 'z' + 1)) +
                    (char)random.nextInt('a', 'z' + 1);
            var regexes = List.of(" ", ".", oneChar, twoChar);
            regexes.forEach(regex ->
                    c.accept(new SplitStringIntData(s, new InlineString(s), regex, random.nextInt(2))));
        });
    }

    @ParameterizedTest
    @MethodSource("splitStringIntData")
    public void split(SplitStringIntData data) {
        assertArrayEquals(data.str().split(data.regex(), data.limit()),
                Arrays.stream(data.inlStr().split(data.regex(), data.limit()))
                        .map(s -> s.toString())
                        .toArray());
    }

    record SplitStringData(String str, InlineString inlStr, String regex) {}
    public static Stream<SplitStringData> splitStringData() {
        return DATA.stream().mapMulti((s, c) -> {
            var random = RandomGenerator.getDefault();
            String oneChar = String.valueOf((char)random.nextInt('a', 'z' + 1));
            String twoChar = String.valueOf((char)random.nextInt('a', 'z' + 1)) +
                    (char)random.nextInt('a', 'z' + 1);
            var regexes = List.of(" ", ".", oneChar, twoChar);
            regexes.forEach(regex ->
                    c.accept(new SplitStringData(s, new InlineString(s), regex)));
        });
    }

    @ParameterizedTest
    @MethodSource("splitStringData")
    public void split(SplitStringData data) {
        assertArrayEquals(data.str().split(data.regex()),
                Arrays.stream(data.inlStr().split(data.regex()))
                        .map(s -> s.toString())
                        .toArray());
    }

    @ParameterizedTest
    @MethodSource("emptyData")
    public void toLowerCase(EmptyData data) {
        assertEquals(data.str().toLowerCase(), data.inlStr().toLowerCase().toString());
    }

    @ParameterizedTest
    @MethodSource("emptyData")
    public void toUpperCase(EmptyData data) {
        assertEquals(data.str().toUpperCase(), data.inlStr().toUpperCase().toString());
    }

    @ParameterizedTest
    @MethodSource("emptyData")
    public void trim(EmptyData data) {
        assertEquals(data.str().trim(), data.inlStr().trim().toString());
    }

    @ParameterizedTest
    @MethodSource("emptyData")
    public void strip(EmptyData data) {
        assertEquals(data.str().strip(), data.inlStr().strip().toString());
    }

    @ParameterizedTest
    @MethodSource("emptyData")
    public void stripLeading(EmptyData data) {
        assertEquals(data.str().stripLeading(), data.inlStr().stripLeading().toString());
    }

    @ParameterizedTest
    @MethodSource("emptyData")
    public void stripTrailing(EmptyData data) {
        assertEquals(data.str().stripTrailing(), data.inlStr().stripTrailing().toString());
    }

    @ParameterizedTest
    @MethodSource("emptyData")
    public void isBlank(EmptyData data) {
        assertEquals(data.str().isBlank(), data.inlStr().isBlank());
    }

    @ParameterizedTest
    @MethodSource("emptyData")
    public void lines(EmptyData data) {
        assertEquals(data.str().lines().toList(),
                data.inlStr().lines().map(s -> s.toString()).toList());
    }

    record CountData(String str, InlineString inlStr, int n) {}
    public static Stream<CountData> countData() {
        return DATA.stream().map(s -> {
            var random = RandomGenerator.getDefault();
            return new CountData(s, new InlineString(s), random.nextInt(-5, 5));
        });
    }

    @ParameterizedTest
    @MethodSource("countData")
    public void indent(CountData data) {
        assertEquals(data.str().indent(data.n()), data.inlStr().indent(data.n()).toString());
    }

    @ParameterizedTest
    @MethodSource("emptyData")
    public void stripIndent(EmptyData data) {
        assertEquals(data.str().stripIndent(), data.inlStr().stripIndent().toString());
    }

    @ParameterizedTest
    @MethodSource("emptyData")
    public void translateEscape(EmptyData data) {
        assertEquals(data.str().translateEscapes(), data.inlStr().translateEscapes().toString());
    }

    @ParameterizedTest
    @MethodSource("emptyData")
    public void chars(EmptyData data) {
        assertEquals(data.str().chars().boxed().toList(), data.inlStr().chars().boxed().toList());
    }

    @ParameterizedTest
    @MethodSource("emptyData")
    public void codePoints(EmptyData data) {
        assertEquals(data.str().codePoints().boxed().toList(), data.inlStr().codePoints().boxed().toList());
    }

    @ParameterizedTest
    @MethodSource("emptyData")
    public void toCharArray(EmptyData data) {
        assertArrayEquals(data.str().toCharArray(), data.inlStr().toCharArray());
    }

    @ParameterizedTest
    @MethodSource("countData")
    public void repeat(CountData data) {
        assertSimilarExecutions(() -> data.str().repeat(data.n()),
                () -> data.inlStr().repeat(data.n()).toString());
    }

    record JoinData(String str, InlineString inlStr, List<String> elements) {}
    public static Stream<JoinData> join() {
        return DATA.stream().map(s -> new JoinData(s, new InlineString(s), DATA));
    }

    @ParameterizedTest
    @MethodSource
    public void join(JoinData data) {
        assertEquals(String.join(data.str(), data.elements()),
                InlineString.join(data.inlStr(), data.elements()).toString());
    }
}
