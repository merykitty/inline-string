package io.github.merykitty.inlinestring.internal;

import java.lang.invoke.MethodHandle;

import static java.lang.invoke.MethodType.methodType;

public class ArrayDecoders {
    public static boolean isInstance(Object d) {
        return ARRAY_DECODER_CLASS.isInstance(d);
    }

    public static int decode(Object ad, byte[] src, int off, int len, char[] dst) {
        try {
            return (int) DECODE.invokeExact(ad, src, off, len, dst);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    public static boolean isAsciiCompatible(Object ad) {
        try {
            return (boolean) IS_ASCII_COMPATIBLE.invokeExact(ad);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    public static boolean isLatin1Decodable(Object ad) {
        try {
            return (boolean) IS_LATIN1_DECODABLE.invokeExact(ad);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    public static int decodeToLatin1(Object ad, byte[] src, int sp, int len, byte[] dst) {
        try {
            return (int) DECODE_TO_LATIN1.invokeExact(ad, src, sp, len, dst);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    private static final Class<?> ARRAY_DECODER_CLASS;
    private static final MethodHandle DECODE;
    private static final MethodHandle IS_ASCII_COMPATIBLE;
    private static final MethodHandle IS_LATIN1_DECODABLE;
    private static final MethodHandle DECODE_TO_LATIN1;

    static {
        try {
            var lookup = Utils.STRING_LOOKUP;
            ARRAY_DECODER_CLASS = lookup.findClass("sun.nio.cs.ArrayDecoder");
            DECODE = lookup.findVirtual(ARRAY_DECODER_CLASS, "decode", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE, Character.TYPE.arrayType()))
                    .asType(methodType(Integer.TYPE, Object.class, Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE, Character.TYPE.arrayType()));
            IS_ASCII_COMPATIBLE = lookup.findVirtual(ARRAY_DECODER_CLASS, "isASCIICompatible", methodType(Boolean.TYPE))
                    .asType(methodType(Boolean.TYPE, Object.class));
            IS_LATIN1_DECODABLE = lookup.findVirtual(ARRAY_DECODER_CLASS, "isLatin1Decodable", methodType(Boolean.TYPE))
                    .asType(methodType(Boolean.TYPE, Object.class));
            DECODE_TO_LATIN1 = lookup.findVirtual(ARRAY_DECODER_CLASS, "decodeToLatin1", methodType(Integer.TYPE,
                    Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE, Byte.TYPE.arrayType()))
                    .asType(methodType(Integer.TYPE, Object.class, Byte.TYPE.arrayType(), Integer.TYPE, Integer.TYPE, Byte.TYPE.arrayType()));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
