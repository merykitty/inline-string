package io.github.merykitty.inlinestring;

import io.github.merykitty.inlinestring.internal.Utils;

import java.lang.invoke.MethodHandle;
import static java.lang.invoke.MethodType.methodType;

final class StringConcatHelper {
    public static long mix(long lengthCoder, InlineString value) {
        lengthCoder += value.length();
        if (value.coder() == Utils.UTF16) {
            lengthCoder |= UTF16;
        }
        return checkOverflow(lengthCoder);
    }
    public static InlineString simpleConcat(InlineString first, InlineString second) {
        if (first.isEmpty()) {
            return second;
        }
        if (second.isEmpty()) {
            return first;
        }
        // start "mixing" in length and coder or arguments, order is not
        // important
        long indexCoder = mix(initialCoder(), first);
        indexCoder = mix(indexCoder, second);
        byte[] buf = newArray(indexCoder);
        // prepend each argument in reverse order, since we prepending
        // from the end of the byte array
        indexCoder = prepend(indexCoder, buf, second);
        indexCoder = prepend(indexCoder, buf, first);
        return newString(buf, indexCoder);
    }

    public static byte[] newArray(long indexCoder) {
        try {
            return (byte[]) NEW_ARRAY.invokeExact(indexCoder);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static final long LATIN1 = (long)Utils.LATIN1 << 32;
    private static final long UTF16 = (long)Utils.UTF16 << 32;
    private static final MethodHandle NEW_ARRAY;

    private static long checkOverflow(long lengthCoder) {
        if ((int)lengthCoder >= 0) {
            return lengthCoder;
        }
        throw new OutOfMemoryError("Overflow: String length out of range");
    }

    /**
     * Prepends the stringly representation of String index into buffer,
     * given the coder and final index. Index is measured in chars, not in bytes!
     *
     * @param indexCoder final char index in the buffer, along with coder packed
     *                   into higher bits.
     * @param buf        buffer to append to
     * @param value      String index to encode
     * @return           updated index (coder index retained)
     */
    private static long prepend(long indexCoder, byte[] buf, InlineString value) {
        indexCoder -= value.length();
        if (indexCoder < UTF16) {
            value.getBytes(buf, (int)indexCoder, Utils.LATIN1);
        } else {
            value.getBytes(buf, (int)indexCoder, Utils.UTF16);
        }
        return indexCoder;
    }

    /**
     * Instantiates the String with given buffer and coder
     * @param buf           buffer to use
     * @param indexCoder    remaining index (should be zero) and coder
     * @return String       resulting string
     */
    private static InlineString newString(byte[] buf, long indexCoder) {
        // Use the private, non-copying constructor (unsafe!)
        if (indexCoder == LATIN1) {
            return new InlineString(buf, Utils.LATIN1);
        } else if (indexCoder == UTF16) {
            return new InlineString(buf, Utils.UTF16);
        } else {
            throw new InternalError("Storage is not completely initialized, " + (int)indexCoder + " bytes left");
        }
    }

    /**
     * Provides the initial coder for the String.
     * @return initial coder, adjusted into the upper half
     */
    private static long initialCoder() {
        return Utils.COMPACT_STRINGS ? LATIN1 : UTF16;
    }

    static {
        try {
            var lookup = Utils.STRING_LOOKUP;
            var klass = lookup.findClass("java.lang.StringConcatHelper");
            NEW_ARRAY = lookup.findStatic(klass, "newArray", methodType(Byte.TYPE.arrayType(),
                    Long.TYPE));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
