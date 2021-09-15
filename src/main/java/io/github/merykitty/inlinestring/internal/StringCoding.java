package io.github.merykitty.inlinestring.internal;

import java.lang.invoke.MethodHandle;

import static java.lang.invoke.MethodType.methodType;

public final class StringCoding {
    public static boolean hasNegatives(byte[] ba, int off, int len) {
        try {
            return (boolean) HAS_NEGATIVES.invokeExact(ba, off, len);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static int implEncodeISOArray(byte[] sa, int sp, byte[] da, int dp, int len) {
        try {
            return (int) IMPL_ENCODE_ISO_ARRAY.invokeExact(sa, sp, da, dp, len);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    private static final MethodHandle HAS_NEGATIVES;
    private static final MethodHandle IMPL_ENCODE_ISO_ARRAY;

    static {
        try {
            var lookup = Utils.STRING_LOOKUP;
            var klass = lookup.findClass("java.lang.StringCoding");
            HAS_NEGATIVES = lookup.findStatic(klass, "hasNegatives", methodType(Boolean.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE));
            IMPL_ENCODE_ISO_ARRAY = lookup.findStatic(klass, "implEncodeISOArray", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE, Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE));
        } catch (NoSuchMethodException | IllegalAccessException | ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }
}
