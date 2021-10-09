package io.github.merykitty.inlinestring;

import io.github.merykitty.inlinestring.internal.Utils;
import jdk.internal.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import static java.lang.invoke.MethodType.methodType;

final class StringConcatHelper {
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();

    @__primitive__
    record IndexCoder(int index, byte coder) {}

    public static IndexCoder mix(IndexCoder current, InlineString value) {
        try {
            return new IndexCoder(Math.addExact(current.index(), value.length()), (byte)(current.coder() | value.coder()));
        } catch (ArithmeticException e) {
            throw new OutOfMemoryError("Overflow: String index out of range");
        }
    }

    public static InlineString simpleConcat(InlineString first, InlineString second) {
        if (first.isEmpty()) {
            return second;
        }
        if (second.isEmpty()) {
            return first;
        }
        // start "mixing" in index and coder or arguments, order is not
        // important
        var indexCoder = mix(initialCoder(), first);
        indexCoder = mix(indexCoder, second);
        if (StringCompressed.compressible(indexCoder.index(), indexCoder.coder())) {
            // first and second are both compressed
            return StringCompressed.concat(first, second);
        } else {
            byte[] buf = newArray(indexCoder.index(), indexCoder.coder());
            // prepend each argument in reverse order, since we prepending
            // from the end of the byte array
            indexCoder = prepend(indexCoder, buf, second);
            indexCoder = prepend(indexCoder, buf, first);
            return newString(buf, indexCoder);
        }
    }

    /**
     * Allocate an uninitialized byte array data of a string
     *
     * @param length the length of the string
     * @param coder the coder, LATIN1 or UTF16
     * @return the allocated byte array
     */
    public static byte[] newArray(int length, byte coder) {
        return (byte[])UNSAFE.allocateUninitializedArray(Byte.TYPE, length << coder);
    }

    /**
     * Prepends the string representation of String index into buffer,
     * given the coder and final index. Index is measured in chars, not in bytes!
     *
     * @param indexCoder final char index in the buffer, along with coder packed
     *                   into higher bits.
     * @param buf        buffer to append to
     * @param value      String index to encode
     * @return           updated index (coder index retained)
     */
    private static IndexCoder prepend(IndexCoder indexCoder, byte[] buf, InlineString value) {
        indexCoder = new IndexCoder(indexCoder.index() - value.length(), indexCoder.coder());
        value.getBytes(buf, indexCoder.index(), indexCoder.coder());
        return indexCoder;
    }

    /**
     * Instantiates a uncompressed string with given buffer and coder
     * @param buf           buffer to use
     * @param indexCoder    remaining index (should be zero) and coder
     * @return String       resulting string
     */
    private static InlineString newString(byte[] buf, IndexCoder indexCoder) {
        return new InlineString(buf, buf.length >>> indexCoder.coder(), indexCoder.coder(), 0);
    }

    /**
     * Provides the initial coder for the String.
     * @return initial coder, adjusted into the upper half
     */
    private static IndexCoder initialCoder() {
        return Utils.COMPACT_STRINGS ? new IndexCoder(0, Utils.LATIN1) : new IndexCoder(0, Utils.UTF16);
    }
}
