package io.github.merykitty.inlinestring.encoding;

import io.github.merykitty.inlinestring.encoding.string.StringDecoder;
import io.github.merykitty.inlinestring.encoding.utf8.UTF8Decoder;
import io.github.merykitty.inlinestring.internal.StringData;

import java.lang.foreign.SegmentAllocator;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Unicode {
    public static StringData decode(byte[] data, int offset, int count, Charset charset, SegmentAllocator allocator) {
        if (charset == StandardCharsets.UTF_8) {
            return UTF8Decoder.apply(data, offset, count, allocator);
        } else {
            return StringDecoder.apply(new String(data, offset, count, charset), allocator);
        }
    }
    public static final int MAX_ASCII = 0x7F;
    public static final int MIN_SURROGATE = 0xD800;
    public static final int MAX_SURROGATE = 0xDFFF;
    public static final int MAX_BMP = 0xFFFF;
    public static final int MAX_UNICODE = 0x10FFFF;
}
