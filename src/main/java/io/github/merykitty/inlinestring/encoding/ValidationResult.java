package io.github.merykitty.inlinestring.encoding;

import io.github.merykitty.inlinestring.internal.StringData;

public record ValidationResult(long utf16Length, boolean isAscii) {
    public static ValidationResult invalid() {
        return INVALID;
    }

    public boolean isInvalid() {
        return utf16Length < 0;
    }

    public boolean compressible() {
        return StringData.compressible(utf16Length()) && isAscii();
    }

    private static final ValidationResult INVALID = new ValidationResult(-1, false);
}
