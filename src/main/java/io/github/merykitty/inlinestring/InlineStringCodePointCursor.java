package io.github.merykitty.inlinestring;

import io.github.merykitty.inlinestring.internal.StringCompressed;
import io.github.merykitty.inlinestring.internal.StringUTF16;

public class InlineStringCodePointCursor {
    private InlineString str;
    private long index;
    private long length;
    private long inc;

    InlineStringCodePointCursor(InlineString str) {
        this.str = str;
        this.index = 0;
        this.length = str.length();
        this.inc = computeInc(str, 0, length);
    }

    private InlineStringCodePointCursor(InlineString str, long index, long length) {
        this.str = str;
        this.index = index;
        this.length = length;
        this.inc = computeInc(str, index, length);
    }

    public boolean valid() {
        return index < length;
    }

    public int get() {
        if (!valid()) {
            throw new IllegalStateException("Dereferencing invalid cursor");
        }

        if (str.isCompressed()) {
            return StringCompressed.charAt(str.firstHalf(), str.secondHalf(), index);
        } else if (inc == 1) {
            return StringUTF16.charAt(str.address(), str.scope(), index);
        } else {
            return Character.toCodePoint(StringUTF16.charAt(str.address(), str.scope(), index),
                    StringUTF16.charAt(str.address(), str.scope(), index + 1));
        }
    }

    public InlineStringCodePointCursor next() {
        if (!valid()) {
            throw new IllegalStateException("Advancing invalid cursor");
        }

        return new InlineStringCodePointCursor(str, index + inc, length);
    }

    private static long computeInc(InlineString str, long index, long length) {
        if (index == length) {
            return 0;
        }

        if (str.isCompressed()) {
            return 1;
        } else if (!Character.isHighSurrogate(StringUTF16.charAt(
                str.address(), str.scope(), index)) ||
                index == length - 1 ||
                !Character.isLowSurrogate(StringUTF16.charAt(
                        str.address(), str.scope(), index + 1))) {
            return 1;
        } else {
            return 2;
        }
    }
}
