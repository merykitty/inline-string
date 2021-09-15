package io.github.merykitty.inlinestring.internal;

import java.lang.invoke.MethodHandle;
import static java.lang.invoke.MethodType.methodType;

public class ArrayEncoders {
    public static boolean isInstance(Object ce) {
        return ARRAY_ENCODER_CLASS.isInstance(ce);
    }

    public static boolean isAsciiCompatible(Object ae) {
        try {
            return (boolean) IS_ASCII_COMPATIBLE.invokeExact(ae);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    public static int encodeFromLatin1(Object ae, byte[] src, int sp, int len, byte[] dst) {
        try {
            return (int) ENCODE_FROM_LATIN1.invokeExact(ae, src, sp, len, dst);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    public static int encodeFromUTF16(Object ae, byte[] src, int sp, int len, byte[] dst) {
        try {
            return (int) ENCODE_FROM_UTF16.invokeExact(ae, src, sp, len, dst);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    private static final Class<?> ARRAY_ENCODER_CLASS;
    private static final MethodHandle IS_ASCII_COMPATIBLE;
    private static final MethodHandle ENCODE_FROM_LATIN1;
    private static final MethodHandle ENCODE_FROM_UTF16;

    static {
        try {
            var lookup = Utils.STRING_LOOKUP;
            ARRAY_ENCODER_CLASS = lookup.findClass("sun.nio.cs.ArrayEncoder");
            IS_ASCII_COMPATIBLE = lookup.findVirtual(ARRAY_ENCODER_CLASS, "isASCIICompatible", methodType(Boolean.TYPE))
                    .asType(methodType(Boolean.TYPE, Object.class));
            ENCODE_FROM_LATIN1 = lookup.findVirtual(ARRAY_ENCODER_CLASS, "encodeFromLatin1", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE, Byte.TYPE.arrayType()))
                    .asType(methodType(Integer.TYPE, Object.class, Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE, Byte.TYPE.arrayType()));
            ENCODE_FROM_UTF16 = lookup.findVirtual(ARRAY_ENCODER_CLASS, "encodeFromUTF16", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE, Byte.TYPE.arrayType()))
                    .asType(methodType(Integer.TYPE, Object.class, Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE, Byte.TYPE.arrayType()));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
