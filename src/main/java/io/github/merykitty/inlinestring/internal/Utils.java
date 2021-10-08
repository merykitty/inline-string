package io.github.merykitty.inlinestring.internal;

import io.github.merykitty.inlinestring.InlineString;
import jdk.incubator.vector.VectorSpecies;

import java.lang.constant.ClassDesc;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.ByteOrder;
import java.util.function.Supplier;

import static java.lang.invoke.MethodType.methodType;

public final class Utils {
    public static final boolean COMPRESSED_STRINGS;
    public static final MethodHandles.Lookup STRING_LOOKUP;
    public static final boolean COMPACT_STRINGS;
    public static final byte LATIN1;
    public static final byte UTF16;
    public static final DirectMethodHandleDesc INLINE_STRING_CONSTRUCTOR_STRING;
    public static final ClassDesc INLINE_STRING_CLASS;

    public static String newStringValueCoder(byte[] value, byte coder) {
        try {
            return (String) NEW_STRING_VALUE_CODER.invokeExact(value, coder);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    public static byte[] stringValue(String arg) {
        try {
            return (byte[]) STRING_VALUE.invokeExact(arg);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static byte stringCoder(String arg) {
        try {
            return (byte) STRING_CODER.invokeExact(arg);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static byte[] stringBuilderGetValue(CharSequence arg) {
        try {
            return (byte[]) STRING_BUILDER_GET_VALUE.invokeExact(arg);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static byte stringBuilderGetCoder(CharSequence arg) {
        try {
            return (byte) STRING_BUILDER_GET_CODER.invokeExact(arg);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static void stringCheckIndex(int index, int length) {
        try {
            STRING_CHECK_INDEX.invokeExact(index, length);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static void stringCheckOffset(int offset, int length) {
        try {
            STRING_CHECK_OFFSET.invokeExact(offset, length);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static void stringCheckBoundsOffCount(int offset, int count, int length) {
        try {
            STRING_CHECK_BOUNDS_OFF_COUNT.invokeExact(offset, count, length);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static void stringCheckBoundsBeginEnd(int begin, int end, int length) {
        try {
            STRING_CHECK_BOUNDS_BEGIN_END.invokeExact(begin, end, length);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    private static final MethodHandle NEW_STRING_VALUE_CODER;
    private static final MethodHandle STRING_VALUE;
    private static final MethodHandle STRING_CODER;
    private static final MethodHandle STRING_BUILDER_GET_VALUE;
    private static final MethodHandle STRING_BUILDER_GET_CODER;
    private static final MethodHandle STRING_CHECK_INDEX;
    private static final MethodHandle STRING_CHECK_OFFSET;
    private static final MethodHandle STRING_CHECK_BOUNDS_OFF_COUNT;
    private static final MethodHandle STRING_CHECK_BOUNDS_BEGIN_END;

    static {
        try {
            var lookup = MethodHandles.privateLookupIn(String.class, MethodHandles.lookup());
            STRING_LOOKUP = lookup;
            COMPACT_STRINGS = (boolean) lookup.findStaticGetter(String.class, "COMPACT_STRINGS", Boolean.TYPE).invokeExact();
            LATIN1 = (byte) lookup.findStaticGetter(String.class, "LATIN1", Byte.TYPE).invokeExact();
            UTF16 = (byte) lookup.findStaticGetter(String.class, "UTF16", Byte.TYPE).invokeExact();
            NEW_STRING_VALUE_CODER = lookup.findConstructor(String.class, methodType(void.class,
                    Byte.TYPE.arrayType(), Byte.TYPE));
            STRING_VALUE = lookup.findVirtual(String.class, "value", methodType(Byte.TYPE.arrayType()));
            STRING_CODER = lookup.findVirtual(String.class, "coder", methodType(Byte.TYPE));
            var abstractBuilderKlass = lookup.findClass("java.lang.AbstractStringBuilder");
            STRING_BUILDER_GET_VALUE = lookup.findVirtual(abstractBuilderKlass, "getValue", methodType(Byte.TYPE.arrayType()))
                    .asType(methodType(Byte.TYPE.arrayType(), CharSequence.class));
            STRING_BUILDER_GET_CODER = lookup.findVirtual(abstractBuilderKlass, "getCoder", methodType(Byte.TYPE))
                    .asType(methodType(Byte.TYPE, CharSequence.class));
            STRING_CHECK_INDEX = lookup.findStatic(String.class, "checkIndex", methodType(Void.TYPE,
                    Integer.TYPE, Integer.TYPE));
            STRING_CHECK_OFFSET = lookup.findStatic(String.class, "checkOffset", methodType(Void.TYPE,
                    Integer.TYPE, Integer.TYPE));
            STRING_CHECK_BOUNDS_OFF_COUNT = lookup.findStatic(String.class, "checkBoundsOffCount", methodType(Void.TYPE,
                    Integer.TYPE, Integer.TYPE, Integer.TYPE));
            STRING_CHECK_BOUNDS_BEGIN_END = lookup.findStatic(String.class, "checkBoundsBeginEnd", methodType(Void.TYPE,
                    Integer.TYPE, Integer.TYPE, Integer.TYPE));

            var temp = false;
            try {
                var preferredSpecies = VectorSpecies.ofPreferred(Byte.TYPE);
                temp = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN && COMPACT_STRINGS && preferredSpecies.vectorBitSize() >= 256;
            } catch (Exception e) {}
            COMPRESSED_STRINGS = temp;

            INLINE_STRING_CONSTRUCTOR_STRING = (DirectMethodHandleDesc) MethodHandles.lookup().findStatic(InlineString.class,
                    "<init>",
                    methodType(InlineString.class.asValueType(), String.class))
                    .describeConstable()
                    .get();
            INLINE_STRING_CLASS = InlineString.class.describeConstable().get();
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }
}
