package io.github.merykitty.inlinestring.internal;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.stream.Collector;

import static java.lang.invoke.MethodType.methodType;

public final class Utils {
    public static final MethodHandles.Lookup STRING_LOOKUP;
    public static final boolean COMPACT_STRINGS;
    public static final byte LATIN1;
    public static final byte UTF16;

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

    public static byte[] stringBuilderGetValue(StringBuilder arg) {
        try {
            return (byte[]) STRING_BUILDER_GET_VALUE.invokeExact(arg);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            // Can't reach here
            throw new AssertionError(e);
        }
    }

    public static byte stringBuilderGetCoder(StringBuilder arg) {
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
            STRING_BUILDER_GET_VALUE = lookup.findVirtual(StringBuilder.class, "getValue", methodType(Byte.TYPE.arrayType()));
            STRING_BUILDER_GET_CODER = lookup.findVirtual(StringBuilder.class, "getCoder", methodType(Byte.TYPE));
            STRING_CHECK_INDEX = lookup.findStatic(String.class, "checkIndex", methodType(Void.TYPE,
                    Integer.TYPE, Integer.TYPE));
            STRING_CHECK_OFFSET = lookup.findStatic(String.class, "checkOffset", methodType(Void.TYPE,
                    Integer.TYPE, Integer.TYPE));
            STRING_CHECK_BOUNDS_OFF_COUNT = lookup.findStatic(String.class, "checkBoundsOffCount", methodType(Void.TYPE,
                    Integer.TYPE, Integer.TYPE, Integer.TYPE));
            STRING_CHECK_BOUNDS_BEGIN_END = lookup.findStatic(String.class, "checkBoundsBeginEnd", methodType(Void.TYPE,
                    Integer.TYPE, Integer.TYPE, Integer.TYPE));
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }
}
