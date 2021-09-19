package io.github.merykitty.inlinestring.test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class Utils {
    public static final List<String> DATA = List.of("",
            " ",
            "\n",
            "Hello world!",
            "Ngồi nghịch thuii",
            "test\n".repeat(20),
            "  qwerty",
            "\t 1 + 1 = 2 \n",
            "Einstein's equations \t",
            "\\n\\t1 + 1 = 2\\0");
    public static final List<Charset> CHARSETS = List.of(StandardCharsets.UTF_8,
                    StandardCharsets.UTF_16,
                    StandardCharsets.ISO_8859_1,
                    StandardCharsets.US_ASCII);

    public interface SupplierExce<T, E extends Exception> {
        T get() throws E;
    }

    public static <T, E extends Exception> void assertSimilarExecutions(
            SupplierExce<T, E> expected, SupplierExce<T, E> actual) {
        Exception expectedExcep = null;
        Exception actualExcep = null;
        T expectedObj = null;
        T actualObj = null;
        try {
            expectedObj = expected.get();
        } catch (Exception e) {
            expectedExcep = e;
        }
        try {
            actualObj = actual.get();
        } catch (Exception e) {
            actualExcep = e;
        }
        if (expectedExcep != null) {
            assertEquals(expectedExcep.getClass(), actualExcep.getClass());
            assertEquals(expectedExcep.getMessage(), actualExcep.getMessage());
        } else {
            if (expectedObj instanceof byte[] array) {
                assertArrayEquals(array, (byte[]) actualObj);
            } else if (expectedObj instanceof char[] array) {
                assertArrayEquals(array, (char[]) actualObj);
            } else if (expectedObj instanceof int[] array) {
                assertArrayEquals(array, (int[]) actualObj);
            } else {
                assertEquals(expectedObj, actualObj);
            }
        }
    }
}
