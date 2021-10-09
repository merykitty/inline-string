package io.github.merykitty.inlinestring;

/*
 * Copyright (c) 1994, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import java.io.UnsupportedEncodingException;
import java.lang.constant.Constable;
import java.lang.constant.DynamicConstantDesc;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.*;

import io.github.merykitty.inlinestring.internal.Helper;
import io.github.merykitty.inlinestring.internal.Utils;
import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.vector.*;

/**
 * The {@code String} class represents character strings. All
 * string literals in Java programs, such as {@code "abc"}, are
 * implemented as instances of this class.
 * <p>
 * Strings are constant; their values cannot be changed after they
 * are created. String buffers support mutable strings.
 * Because String objects are immutable they can be shared. For example:
 * <blockquote><pre>
 *     String str = "abc";
 * </pre></blockquote><p>
 * is equivalent to:
 * <blockquote><pre>
 *     char data[] = {'a', 'b', 'c'};
 *     String str = new String(data);
 * </pre></blockquote><p>
 * Here are some more examples of how strings can be used:
 * <blockquote><pre>
 *     System.out.println("abc");
 *     String cde = "cde";
 *     System.out.println("abc" + cde);
 *     String c = "abc".substring(2, 3);
 *     String d = cde.substring(1, 2);
 * </pre></blockquote>
 * <p>
 * The class {@code String} includes methods for examining
 * individual characters of the sequence, for comparing strings, for
 * searching strings, for extracting substrings, and for creating a
 * copy of a string with all characters translated to uppercase or to
 * lowercase. Case mapping is based on the Unicode Standard version
 * specified by the {@link java.lang.Character Character} class.
 * <p>
 * The Java language provides special support for the string
 * concatenation operator (&nbsp;+&nbsp;), and for conversion of
 * other objects to strings. For additional information on string
 * concatenation and conversion, see <i>The Java Language Specification</i>.
 *
 * <p> Unless otherwise noted, passing a {@code null} argument to a constructor
 * or method in this class will cause a {@link NullPointerException} to be
 * thrown.
 *
 * <p>A {@code String} represents a string in the UTF-16 format
 * in which <em>supplementary characters</em> are represented by <em>surrogate
 * pairs</em> (see the section <a href="Character.html#unicode">Unicode
 * Character Representations</a> in the {@code Character} class for
 * more information).
 * Index values refer to {@code char} code units, so a supplementary
 * character uses two positions in a {@code String}.
 * <p>The {@code String} class provides methods for dealing with
 * Unicode code points (i.e., characters), in addition to those for
 * dealing with Unicode code units (i.e., {@code char} values).
 *
 * <p>Unless otherwise noted, methods for comparing Strings do not take locale
 * into account.  The {@link java.text.Collator} class provides methods for
 * finer-grain, locale-sensitive String comparison.
 *
 * @implNote The implementation of the string concatenation operator is left to
 * the discretion of a Java compiler, as long as the compiler ultimately conforms
 * to <i>The Java Language Specification</i>. For example, the {@code javac} compiler
 * may implement the operator with {@code StringBuffer}, {@code StringBuilder},
 * or {@code java.lang.invoke.StringConcatFactory} depending on the JDK version. The
 * implementation of string conversion is typically through the method {@code toString},
 * defined by {@code Object} and inherited by all classes in Java.
 *
 * @author  Lee Boynton
 * @author  Arthur van Hoff
 * @author  Martin Buchholz
 * @author  Ulf Zibis
 * @see     java.lang.Object#toString()
 * @see     java.lang.StringBuffer
 * @see     java.lang.StringBuilder
 * @see     java.nio.charset.Charset
 * @since   1.0
 * @jls     15.18.1 String Concatenation Operator +
 */

@__primitive__
public class InlineString
        implements /*Comparable<InlineString.ref>,*/ CharSequence, Constable {

    /**
     * The value which a compressed string point at.
     */
    static final byte[] SMALL_STRING_VALUE = null;

    /**
     * The value is used for character storage.
     */
    private final byte[] value;

    /**
     * The length of the string.
     */
    private final int length;

    /**
     * If the string is compressible, this field contains the first half of the
     * value.
     *
     * Else, it is the identifier of the encoding used to encode the bytes in
     * {@code value}. The supported values in this implementation are
     *
     * String.LATIN1
     * String.UTF16
     */
    private final long firstHalf;

    /**
     * If the string is compressible, this field contains the second half of the
     * value.
     */
    private final long secondHalf;

    static final InlineString EMPTY_STRING = new InlineString("");

    /**
     * Initializes an {@code InlineString} object so that it represents
     * an empty character sequence. The result is the same as the one received
     * by calling {@code new InlineString("")}.
     */
    public InlineString() {
        this.value = EMPTY_STRING.value;
        this.length = EMPTY_STRING.length;
        this.firstHalf = EMPTY_STRING.firstHalf;
        this.secondHalf = EMPTY_STRING.secondHalf;
    }

    /**
     * Initializes an {@code InlineString} object so that it represents
     * the same sequence of characters as a {@code String} object.
     *
     * @param  original
     *         A {@code String}
     */
    public InlineString(String original) {
        this(Utils.stringValue(original), Utils.stringCoder(original));
    }

    /**
     * Initializes an {@code InlineString} so that it represents the sequence of
     * characters currently contained in the character array argument. The
     * contents of the character array are copied; subsequent modification of
     * the character array does not affect the newly created string.
     *
     * @param  value
     *         The initial value of the string
     */
    public InlineString(char[] value) {
        this(value, 0, value.length, null);
    }

    /**
     * Initialize an {@code InlineString} that contains characters from a subarray
     * of the character array argument. The {@code offset} argument is the
     * index of the first character of the subarray and the {@code count}
     * argument specifies the length of the subarray. The contents of the
     * subarray are copied; subsequent modification of the character array does
     * not affect the newly created string.
     *
     * @param  value
     *         Array that is the source of characters
     *
     * @param  offset
     *         The initial offset
     *
     * @param  count
     *         The length
     *
     * @throws  IndexOutOfBoundsException
     *          If {@code offset} is negative, {@code count} is negative, or
     *          {@code offset} is greater than {@code value.length - count}
     */
    public InlineString(char[] value, int offset, int count) {
        this(value, offset, count, rangeCheck(value, offset, count));
    }

    private static Void rangeCheck(char[] value, int offset, int count) {
        checkBoundsOffCount(offset, count, value.length);
        return null;
    }

    /**
     * Initialize an {@code InlineString} that contains characters from a subarray
     * of the <a href="Character.html#unicode">Unicode code point</a> array
     * argument.  The {@code offset} argument is the index of the first code
     * point of the subarray and the {@code count} argument specifies the
     * length of the subarray.  The contents of the subarray are converted to
     * {@code char}s; subsequent modification of the {@code int} array does not
     * affect the newly created string.
     *
     * @param  codePoints
     *         Array that is the source of Unicode code points
     *
     * @param  offset
     *         The initial offset
     *
     * @param  count
     *         The length
     *
     * @throws  IllegalArgumentException
     *          If any invalid Unicode code point is found in {@code
     *          codePoints}
     *
     * @throws  IndexOutOfBoundsException
     *          If {@code offset} is negative, {@code count} is negative, or
     *          {@code offset} is greater than {@code codePoints.length - count}
     *
     * @since  1.5
     */
    public InlineString(int[] codePoints, int offset, int count) {
        checkBoundsOffCount(offset, count, codePoints.length);
        if (count == 0) {
            this.value = EMPTY_STRING.value;
            this.length = EMPTY_STRING.length;
            this.firstHalf = EMPTY_STRING.firstHalf;
            this.secondHalf = EMPTY_STRING.secondHalf;
            return;
        }
        if (Utils.COMPACT_STRINGS) {
            byte[] val = StringLatin1.toBytes(codePoints, offset, count);
            if (val != null) {
                if (StringCompressed.compressible(val.length)) {
                    var compressedValue = StringCompressed.compress(val);
                    this.value = SMALL_STRING_VALUE;
                    this.length = val.length;
                    this.firstHalf = compressedValue.firstHalf();
                    this.secondHalf = compressedValue.secondHalf();
                    return;
                } else {
                    this.value = val;
                    this.length = val.length;
                    this.firstHalf = Utils.LATIN1;
                    this.secondHalf = 0;
                    return;
                }
            }
        }
        var val = StringUTF16.toBytes(codePoints, offset, count);
        this.value = val;
        this.length = val.length >>> Utils.UTF16;
        this.firstHalf = Utils.UTF16;
        this.secondHalf = 0;

    }

    /**
     * Initialize an {@code InlineString} by decoding the specified subarray of
     * bytes using the specified charset.  The length of the new
     * {@code InlineString} is a function of the charset, and hence may not be
     * equal to the length of the subarray.
     *
     * <p> The behavior of this constructor when the given bytes are not valid
     * in the given charset is unspecified.  The {@link
     * java.nio.charset.CharsetDecoder} class should be used when more control
     * over the decoding process is required.
     *
     * @param  bytes
     *         The bytes to be decoded into characters
     *
     * @param  offset
     *         The index of the first byte to decode
     *
     * @param  length
     *         The number of bytes to decode
     *
     * @param  charsetName
     *         The name of a supported {@linkplain java.nio.charset.Charset
     *         charset}
     *
     * @throws  UnsupportedEncodingException
     *          If the named charset is not supported
     *
     * @throws  IndexOutOfBoundsException
     *          If {@code offset} is negative, {@code length} is negative, or
     *          {@code offset} is greater than {@code bytes.length - length}
     *
     * @since  1.1
     */
    public InlineString(byte[] bytes, int offset, int length, String charsetName)
            throws UnsupportedEncodingException {
        this(bytes, offset, length, lookupCharset(charsetName));
    }

    /**
     * Initialize an {@code InlineString} by decoding the specified subarray of
     * bytes using the specified {@linkplain java.nio.charset.Charset charset}.
     * The length of the new {@code InlineString} is a function of the charset, and
     * hence may not be equal to the length of the subarray.
     *
     * <p> This method always replaces malformed-input and unmappable-character
     * sequences with this charset's default replacement string.  The {@link
     * java.nio.charset.CharsetDecoder} class should be used when more control
     * over the decoding process is required.
     *
     * @param  bytes
     *         The bytes to be decoded into characters
     *
     * @param  offset
     *         The index of the first byte to decode
     *
     * @param  length
     *         The number of bytes to decode
     *
     * @param  charset
     *         The {@linkplain java.nio.charset.Charset charset} to be used to
     *         decode the {@code bytes}
     *
     * @throws  IndexOutOfBoundsException
     *          If {@code offset} is negative, {@code length} is negative, or
     *          {@code offset} is greater than {@code bytes.length - length}
     *
     * @since  1.6
     */
    public InlineString(byte[] bytes, int offset, int length, Charset charset) {
        this(new String(bytes, offset, length, charset));
    }

    /**
     * Initialize an {@code InlineString} by decoding the specified array of bytes
     * using the specified {@linkplain java.nio.charset.Charset charset}.  The
     * length of the new {@code InlineString} is a function of the charset, and
     * hence may not be equal to the length of the byte array.
     *
     * <p> The behavior of this constructor when the given bytes are not valid
     * in the given charset is unspecified.  The {@link
     * java.nio.charset.CharsetDecoder} class should be used when more control
     * over the decoding process is required.
     *
     * @param  bytes
     *         The bytes to be decoded into characters
     *
     * @param  charsetName
     *         The name of a supported {@linkplain java.nio.charset.Charset
     *         charset}
     *
     * @throws  UnsupportedEncodingException
     *          If the named charset is not supported
     *
     * @since  1.1
     */
    public InlineString(byte[] bytes, String charsetName)
            throws UnsupportedEncodingException {
        this(bytes, 0, bytes.length, charsetName);
    }

    /**
     * Initialize an {@code InlineString} by decoding the specified array of
     * bytes using the specified {@linkplain java.nio.charset.Charset charset}.
     * The length of the new {@code InlineString} is a function of the charset,
     * and hence may not be equal to the length of the byte array.
     *
     * <p> This method always replaces malformed-input and unmappable-character
     * sequences with this charset's default replacement string.  The {@link
     * java.nio.charset.CharsetDecoder} class should be used when more control
     * over the decoding process is required.
     *
     * @param  bytes
     *         The bytes to be decoded into characters
     *
     * @param  charset
     *         The {@linkplain java.nio.charset.Charset charset} to be used to
     *         decode the {@code bytes}
     *
     * @since  1.6
     */
    public InlineString(byte[] bytes, Charset charset) {
        this(bytes, 0, bytes.length, charset);
    }

    /**
     * Initialize an {@code InlineString} by decoding the specified subarray of
     * bytes using the platform's default charset.  The length of the new
     * {@code InlineString} is a function of the charset, and hence may not be
     * equal to the length of the subarray.
     *
     * <p> The behavior of this constructor when the given bytes are not valid
     * in the default charset is unspecified.  The {@link
     * java.nio.charset.CharsetDecoder} class should be used when more control
     * over the decoding process is required.
     *
     * @param  bytes
     *         The bytes to be decoded into characters
     *
     * @param  offset
     *         The index of the first byte to decode
     *
     * @param  length
     *         The number of bytes to decode
     *
     * @throws  IndexOutOfBoundsException
     *          If {@code offset} is negative, {@code length} is negative, or
     *          {@code offset} is greater than {@code bytes.length - length}
     *
     * @since  1.1
     */
    public InlineString(byte[] bytes, int offset, int length) {
        this(bytes, offset, length, Charset.defaultCharset());
    }

    /**
     * Initialize an {@code InlineString} by decoding the specified array of bytes
     * using the platform's default charset.  The length of the new {@code
     * InlineString} is a function of the charset, and hence may not be equal to
     * the length of the byte array.
     *
     * <p> The behavior of this constructor when the given bytes are not valid
     * in the default charset is unspecified.  The {@link
     * java.nio.charset.CharsetDecoder} class should be used when more control
     * over the decoding process is required.
     *
     * @param  bytes
     *         The bytes to be decoded into characters
     *
     * @since  1.1
     */
    public InlineString(byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    /**
     * Initialize an {@code InlineString} that contains the sequence of
     * characters currently contained in the string buffer argument. The
     * contents of the string buffer are copied; subsequent modification of the
     * string buffer does not affect the newly created string.
     *
     * @param  buffer
     *         A {@code StringBuffer}
     */
    public InlineString(StringBuffer buffer) {
        this(buffer.toString());
    }

    /**
     * Initialize an {@code InlineString} that contains the sequence of
     * characters currently contained in the string builder argument. The
     * contents of the string builder are copied; subsequent modification of the
     * string builder does not affect the newly created string.
     *
     * @param   builder
     *          A {@code StringBuilder}
     *
     * @since  1.5
     */
    public InlineString(StringBuilder builder) {
        this(builder.toString());
    }

    /**
     * Returns the length of this string.
     * The length is equal to the number of <a href="Character.html#unicode">Unicode
     * code units</a> in the string.
     *
     * @return  the length of the sequence of characters represented by this
     *          object.
     */
    @Override
    public int length() {
        return length;
    }

    /**
     * Returns {@code true} if, and only if, {@link #length()} is {@code 0}.
     *
     * @return {@code true} if {@link #length()} is {@code 0}, otherwise
     * {@code false}
     *
     * @since 1.6
     */
    @Override
    public boolean isEmpty() {
        return length() == 0;
    }

    /**
     * Returns the {@code char} value at the
     * specified index. An index ranges from {@code 0} to
     * {@code length() - 1}. The first {@code char} value of the sequence
     * is at index {@code 0}, the next at index {@code 1},
     * and so on, as for array indexing.
     *
     * <p>If the {@code char} value specified by the index is a
     * <a href="Character.html#unicode">surrogate</a>, the surrogate
     * value is returned.
     *
     * @param      index   the index of the {@code char} value.
     * @return     the {@code char} value at the specified index of this string.
     *             The first {@code char} value is at index {@code 0}.
     * @throws     IndexOutOfBoundsException  if the {@code index}
     *             argument is negative or not less than the length of this
     *             string.
     */
    public char charAt(int index) {
        if (isCompressed()) {
            return StringCompressed.charAt(firstHalf, secondHalf, length(), index);
        } else if (isLatin1()) {
            return StringLatin1.charAt(value, index);
        } else {
            return StringUTF16.charAt(value, index);
        }
    }

    /**
     * Returns the character (Unicode code point) at the specified
     * index. The index refers to {@code char} values
     * (Unicode code units) and ranges from {@code 0} to
     * {@link #length()}{@code  - 1}.
     *
     * <p> If the {@code char} value specified at the given index
     * is in the high-surrogate range, the following index is less
     * than the length of this {@code String}, and the
     * {@code char} value at the following index is in the
     * low-surrogate range, then the supplementary code point
     * corresponding to this surrogate pair is returned. Otherwise,
     * the {@code char} value at the given index is returned.
     *
     * @param      index the index to the {@code char} values
     * @return     the code point value of the character at the
     *             {@code index}
     * @throws     IndexOutOfBoundsException  if the {@code index}
     *             argument is negative or not less than the length of this
     *             string.
     * @since      1.5
     */
    public int codePointAt(int index) {
        checkIndex(index, length());
        if (isCompressed()) {
            return StringCompressed.codePointAt(this.firstHalf, this.secondHalf, index);
        } else if (isLatin1()) {
            return Byte.toUnsignedInt(value[index]);
        } else {
            return StringUTF16.codePointAt(value, index, length());
        }
    }

    /**
     * Returns the character (Unicode code point) before the specified
     * index. The index refers to {@code char} values
     * (Unicode code units) and ranges from {@code 1} to {@link
     * CharSequence#length() length}.
     *
     * <p> If the {@code char} value at {@code (index - 1)}
     * is in the low-surrogate range, {@code (index - 2)} is not
     * negative, and the {@code char} value at {@code (index -
     * 2)} is in the high-surrogate range, then the
     * supplementary code point value of the surrogate pair is
     * returned. If the {@code char} value at {@code index -
     * 1} is an unpaired low-surrogate or a high-surrogate, the
     * surrogate value is returned.
     *
     * @param     index the index following the code point that should be returned
     * @return    the Unicode code point value before the given index.
     * @throws    IndexOutOfBoundsException if the {@code index}
     *            argument is less than 1 or greater than the length
     *            of this string.
     * @since     1.5
     */
    public int codePointBefore(int index) {
        int i = index - 1;
        checkIndex(i, length());
        if (isCompressed()) {
            return StringCompressed.codePointAt(this.firstHalf, this.secondHalf, i);
        } else if (isLatin1()) {
            return Byte.toUnsignedInt(value[i]);
        } else {
            return StringUTF16.codePointBefore(value, index);
        }
    }

    /**
     * Returns the number of Unicode code points in the specified text
     * range of this {@code String}. The text range begins at the
     * specified {@code beginIndex} and extends to the
     * {@code char} at index {@code endIndex - 1}. Thus the
     * length (in {@code char}s) of the text range is
     * {@code endIndex-beginIndex}. Unpaired surrogates within
     * the text range count as one code point each.
     *
     * @param beginIndex the index to the first {@code char} of
     * the text range.
     * @param endIndex the index after the last {@code char} of
     * the text range.
     * @return the number of Unicode code points in the specified text
     * range
     * @throws    IndexOutOfBoundsException if the
     * {@code beginIndex} is negative, or {@code endIndex}
     * is larger than the length of this {@code String}, or
     * {@code beginIndex} is larger than {@code endIndex}.
     * @since  1.5
     */
    public int codePointCount(int beginIndex, int endIndex) {
        Objects.checkFromToIndex(beginIndex, endIndex, length());
        if (isLatin1()) {
            return endIndex - beginIndex;
        } else {
            return StringUTF16.codePointCount(value, beginIndex, endIndex);
        }
    }

    /**
     * Returns the index within this {@code String} that is
     * offset from the given {@code index} by
     * {@code codePointOffset} code points. Unpaired surrogates
     * within the text range given by {@code index} and
     * {@code codePointOffset} count as one code point each.
     *
     * @param index the index to be offset
     * @param codePointOffset the offset in code points
     * @return the index within this {@code String}
     * @throws    IndexOutOfBoundsException if {@code index}
     *   is negative or larger then the length of this
     *   {@code String}, or if {@code codePointOffset} is positive
     *   and the substring starting with {@code index} has fewer
     *   than {@code codePointOffset} code points,
     *   or if {@code codePointOffset} is negative and the substring
     *   before {@code index} has fewer than the absolute value
     *   of {@code codePointOffset} code points.
     * @since 1.5
     */
    public int offsetByCodePoints(int index, int codePointOffset) {
        if (index < 0 || index > length()) {
            throw new IndexOutOfBoundsException();
        }
        return Character.offsetByCodePoints(this, index, codePointOffset);
    }

    /**
     * Copies characters from this string into the destination character
     * array.
     * <p>
     * The first character to be copied is at index {@code srcBegin};
     * the last character to be copied is at index {@code srcEnd-1}
     * (thus the total number of characters to be copied is
     * {@code srcEnd-srcBegin}). The characters are copied into the
     * subarray of {@code dst} starting at index {@code dstBegin}
     * and ending at index:
     * <blockquote><pre>
     *     dstBegin + (srcEnd-srcBegin) - 1
     * </pre></blockquote>
     *
     * @param      srcBegin   index of the first character in the string
     *                        to copy.
     * @param      srcEnd     index after the last character in the string
     *                        to copy.
     * @param      dst        the destination array.
     * @param      dstBegin   the start offset in the destination array.
     * @throws    IndexOutOfBoundsException If any of the following
     *            is true:
     *            <ul><li>{@code srcBegin} is negative.
     *            <li>{@code srcBegin} is greater than {@code srcEnd}
     *            <li>{@code srcEnd} is greater than the length of this
     *                string
     *            <li>{@code dstBegin} is negative
     *            <li>{@code dstBegin+(srcEnd-srcBegin)} is larger than
     *                {@code dst.length}</ul>
     */
    public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        checkBoundsBeginEnd(srcBegin, srcEnd, length());
        checkBoundsOffCount(dstBegin, srcEnd - srcBegin, dst.length);
        if (isCompressed()) {
            StringCompressed.getChars(firstHalf, secondHalf, srcBegin, srcEnd, dst, dstBegin);
        } else if (isLatin1()) {
            StringLatin1.getChars(value, srcBegin, srcEnd, dst, dstBegin);
        } else {
            StringUTF16.getChars(value, srcBegin, srcEnd, dst, dstBegin);
        }
    }

    /**
     * Encodes this {@code String} into a sequence of bytes using the named
     * charset, storing the result into a new byte array.
     *
     * <p> The behavior of this method when this string cannot be encoded in
     * the given charset is unspecified.  The {@link
     * java.nio.charset.CharsetEncoder} class should be used when more control
     * over the encoding process is required.
     *
     * @param  charsetName
     *         The name of a supported {@linkplain java.nio.charset.Charset
     *         charset}
     *
     * @return  The resultant byte array
     *
     * @throws  UnsupportedEncodingException
     *          If the named charset is not supported
     *
     * @since  1.1
     */
    public byte[] getBytes(String charsetName)
            throws UnsupportedEncodingException {
        if (charsetName == null) throw new NullPointerException();
        return Utils.stringEncode(lookupCharset(charsetName), coder(), content());
    }

    /**
     * Encodes this {@code String} into a sequence of bytes using the given
     * {@linkplain java.nio.charset.Charset charset}, storing the result into a
     * new byte array.
     *
     * <p> This method always replaces malformed-input and unmappable-character
     * sequences with this charset's default replacement byte array.  The
     * {@link java.nio.charset.CharsetEncoder} class should be used when more
     * control over the encoding process is required.
     *
     * @param  charset
     *         The {@linkplain java.nio.charset.Charset} to be used to encode
     *         the {@code String}
     *
     * @return  The resultant byte array
     *
     * @since  1.6
     */
    public byte[] getBytes(Charset charset) {
        if (charset == null) throw new NullPointerException();
        return Utils.stringEncode(charset, coder(), content());
    }

    /**
     * Encodes this {@code String} into a sequence of bytes using the
     * platform's default charset, storing the result into a new byte array.
     *
     * <p> The behavior of this method when this string cannot be encoded in
     * the default charset is unspecified.  The {@link
     * java.nio.charset.CharsetEncoder} class should be used when more control
     * over the encoding process is required.
     *
     * @return  The resultant byte array
     *
     * @since      1.1
     */
    public byte[] getBytes() {
        return Utils.stringEncode(Charset.defaultCharset(), coder(), content());
    }

    /**
     * Compares this string to another string.  The result is {@code
     * true} if and only if the argument represents the same sequence
     * of characters as this object.
     *
     * <p>For finer-grained String comparison, refer to
     * {@link java.text.Collator}.
     *
     * @param  aString
     *         The string to compare this {@code InlineString} against
     *
     * @return  {@code true} if the given string is equivalent to this
     * string, {@code false} otherwise
     *
     * @see  #compareTo(InlineString)
     * @see  #equalsIgnoreCase(InlineString)
     */
    public boolean equals(InlineString aString) {
        if (Utils.COMPACT_STRINGS) {
            if (isCompressed() || aString.isCompressed()) {
                return this == aString;
            } else {
                return this.firstHalf == aString.firstHalf
                        && StringLatin1.equals(this.value, aString.value);
            }
        } else {
            return this.value == aString.value || StringLatin1.equals(this.value, aString.value);
        }
    }

    /**
     * Compares this string to the specified object.  The result is {@code
     * true} if and only if the argument is not {@code null} and is a {@code
     * InlineString} object that represents the same sequence of characters
     * as this object.
     *
     * <p>For finer-grained String comparison, refer to
     * {@link java.text.Collator}.
     *
     * @param  anObject
     *         The object to compare this {@code InlineString} against
     *
     * @return  {@code true} if the given object represents a {@code String}
     *          equivalent to this string, {@code false} otherwise
     *
     * @see  #compareTo(InlineString)
     * @see  #equalsIgnoreCase(InlineString)
     */
    public boolean equals(Object anObject) {
        if (anObject instanceof InlineString aString) {
            return equals(aString);
        } else {
            return false;
        }
    }

    // sb must be a StringBuilder or a StringBuffer
    private boolean nonSyncContentEquals(CharSequence sb) {
        int len = length();
        if (len != sb.length()) {
            return false;
        }
        byte[] sbv = Utils.stringBuilderGetValue(sb);
        byte sbc = Utils.stringBuilderGetCoder(sb);
        if (coder() == sbc) {
            if (isCompressed()) {
                var data = StringCompressed.compress(sbv, 0, len);
                return data.firstHalf() == firstHalf && data.secondHalf() == secondHalf;
            } else {
                // since the lengths are equal the check will only occur in 1 iteration
                return StringLatin1.indexOf(sbv, value) == 0;
            }
        } else {
            if (isLatin1()) {  // utf16 str and latin1 sb can never be "equal"
                return false;
            } else {
                return StringUTF16.contentEquals(value, sbv, len);
            }
        }
    }

    /**
     * Compares this string to the specified {@code CharSequence}.  The
     * result is {@code true} if and only if this {@code String} represents the
     * same sequence of char values as the specified sequence. Note that if the
     * {@code CharSequence} is a {@code StringBuffer} then the method
     * synchronizes on it.
     *
     * <p>For finer-grained String comparison, refer to
     * {@link java.text.Collator}.
     *
     * @param  cs
     *         The sequence to compare this {@code String} against
     *
     * @return  {@code true} if this {@code String} represents the same
     *          sequence of char values as the specified sequence, {@code
     *          false} otherwise
     *
     * @since  1.5
     */
    public boolean contentEquals(CharSequence cs) {
        if (cs instanceof InlineString ps) {
            // Argument is an InlineString
            return equals(ps);
        } else if (cs instanceof String s) {
            // Argument is a String
            return equals(new InlineString(s));
        } else if (cs instanceof StringBuilder sb) {
            // Argument is a StringBuilder
            return nonSyncContentEquals(sb);
        } else if (cs instanceof StringBuffer sb) {
            // Argument is a StringBuffer
            synchronized (sb) {
                return nonSyncContentEquals(sb);
            }
        } else {
            // Argument is a generic CharSequence
            int n = cs.length();
            if (n != length()) {
                return false;
            }
            if (isLatin1()) {
                var val = content();
                for (int i = 0; i < n; i++) {
                    if ((val[i] & 0xff) != cs.charAt(i)) {
                        return false;
                    }
                }
                return true;
            } else {
                return StringUTF16.contentEquals(value, cs, n);
            }
        }
    }

    /**
     * Compares this {@code String} to another {@code String}, ignoring case
     * considerations.  Two strings are considered equal ignoring case if they
     * are of the same length and corresponding Unicode code points in the two
     * strings are equal ignoring case.
     *
     * <p> Two Unicode code points are considered the same
     * ignoring case if at least one of the following is true:
     * <ul>
     *   <li> The two Unicode code points are the same (as compared by the
     *        {@code ==} operator)
     *   <li> Calling {@code Character.toLowerCase(Character.toUpperCase(int))}
     *        on each Unicode code point produces the same result
     * </ul>
     *
     * <p>Note that this method does <em>not</em> take locale into account, and
     * will result in unsatisfactory results for certain locales.  The
     * {@link java.text.Collator} class provides locale-sensitive comparison.
     *
     * @param  anotherString
     *         The {@code String} to compare this {@code String} against
     *
     * @return  {@code true} if the argument is not {@code null} and it
     *          represents an equivalent {@code String} ignoring case; {@code
     *          false} otherwise
     *
     * @see  #equals(Object)
     * @see  #codePoints()
     */
    public boolean equalsIgnoreCase(InlineString anotherString) {
        return (anotherString.length() == length()
                        && regionMatches(true, 0, anotherString, 0, length()));
    }

    /**
     * Compares two strings lexicographically.
     * The comparison is based on the Unicode value of each character in
     * the strings. The character sequence represented by this
     * {@code String} object is compared lexicographically to the
     * character sequence represented by the argument string. The result is
     * a negative integer if this {@code String} object
     * lexicographically precedes the argument string. The result is a
     * positive integer if this {@code String} object lexicographically
     * follows the argument string. The result is zero if the strings
     * are equal; {@code compareTo} returns {@code 0} exactly when
     * the {@link #equals(Object)} method would return {@code true}.
     * <p>
     * This is the definition of lexicographic ordering. If two strings are
     * different, then either they have different characters at some index
     * that is a valid index for both strings, or their lengths are different,
     * or both. If they have different characters at one or more index
     * positions, let <i>k</i> be the smallest such index; then the string
     * whose character at position <i>k</i> has the smaller value, as
     * determined by using the {@code <} operator, lexicographically precedes the
     * other string. In this case, {@code compareTo} returns the
     * difference of the two character values at position {@code k} in
     * the two string -- that is, the value:
     * <blockquote><pre>
     * this.charAt(k)-anotherString.charAt(k)
     * </pre></blockquote>
     * If there is no index position at which they differ, then the shorter
     * string lexicographically precedes the longer string. In this case,
     * {@code compareTo} returns the difference of the lengths of the
     * strings -- that is, the value:
     * <blockquote><pre>
     * this.length()-anotherString.length()
     * </pre></blockquote>
     *
     * <p>For finer-grained String comparison, refer to
     * {@link java.text.Collator}.
     *
     * @param   anotherString   the {@code String} to be compared.
     * @return  the value {@code 0} if the argument string is equal to
     *          this string; a value less than {@code 0} if this string
     *          is lexicographically less than the string argument; and a
     *          value greater than {@code 0} if this string is
     *          lexicographically greater than the string argument.
     */
    // Wait for universal tvars
//    @Override
    public int compareTo(InlineString anotherString) {
        if (this.isCompressed() || anotherString.isCompressed()) {
            InlineString s1, s2;
            int coef;
            if (this.isCompressed()) {
                s1 = this; s2 = anotherString; coef = 1;
            } else {
                s1 = anotherString; s2 = this; coef = -1;
            }
            if (s2.isLatin1()) {
                return StringCompressed.compareToLatin1(s1, s2) * coef;
            } else {
                return StringCompressed.compareToUTF16(s1, s2) * coef;
            }
        } else {
            var v1 = this.value;
            var v2 = anotherString.value;
            if (this.firstHalf == anotherString.firstHalf) {
                return isLatin1() ? StringLatin1.compareTo(v1, v2)
                        : StringUTF16.compareTo(v1, v2);
            } else {
                return isLatin1() ? StringLatin1.compareToUTF16(v1, v2)
                        : StringUTF16.compareToLatin1(v1, v2);
            }
        }
    }

    /**
     * A Comparator that orders {@code InlineString} objects as by
     * {@link #compareToIgnoreCase(InlineString) compareToIgnoreCase}.
     * <p>
     * Note that this Comparator does <em>not</em> take locale into account,
     * and will result in an unsatisfactory ordering for certain locales.
     * The {@link java.text.Collator} class provides locale-sensitive comparison.
     *
     * @see     java.text.Collator
     * @since   1.2
     */
    public static final Comparator<InlineString.ref> CASE_INSENSITIVE_ORDER
            = (s1, s2) -> {
        byte[] v1 = s1.content();
        byte[] v2 = s2.content();
        if (s1.coder() == s2.coder()) {
            return s1.isLatin1() ? StringLatin1.compareToCI(v1, v2)
                    : StringUTF16.compareToCI(v1, v2);
        } else {
            return s1.isLatin1() ? StringLatin1.compareToCI_UTF16(v1, v2)
                    : StringUTF16.compareToCI_Latin1(v1, v2);
        }
    };

    /**
     * Compares two strings lexicographically, ignoring case
     * differences. This method returns an integer whose sign is that of
     * calling {@code compareTo} with case folded versions of the strings
     * where case differences have been eliminated by calling
     * {@code Character.toLowerCase(Character.toUpperCase(int))} on
     * each Unicode code point.
     * <p>
     * Note that this method does <em>not</em> take locale into account,
     * and will result in an unsatisfactory ordering for certain locales.
     * The {@link java.text.Collator} class provides locale-sensitive comparison.
     *
     * @param   str   the {@code String} to be compared.
     * @return  a negative integer, zero, or a positive integer as the
     *          specified String is greater than, equal to, or less
     *          than this String, ignoring case considerations.
     * @see     java.text.Collator
     * @see     #codePoints()
     * @since   1.2
     */
    public int compareToIgnoreCase(InlineString str) {
        return CASE_INSENSITIVE_ORDER.compare(this, str);
    }

    /**
     * Tests if two string regions are equal.
     * <p>
     * A substring of this {@code String} object is compared to a substring
     * of the argument other. The result is true if these substrings
     * represent identical character sequences. The substring of this
     * {@code String} object to be compared begins at index {@code toffset}
     * and has length {@code len}. The substring of other to be compared
     * begins at index {@code ooffset} and has length {@code len}. The
     * result is {@code false} if and only if at least one of the following
     * is true:
     * <ul><li>{@code toffset} is negative.
     * <li>{@code ooffset} is negative.
     * <li>{@code toffset+len} is greater than the length of this
     * {@code String} object.
     * <li>{@code ooffset+len} is greater than the length of the other
     * argument.
     * <li>There is some nonnegative integer <i>k</i> less than {@code len}
     * such that:
     * {@code this.charAt(toffset + }<i>k</i>{@code ) != other.charAt(ooffset + }
     * <i>k</i>{@code )}
     * </ul>
     *
     * <p>Note that this method does <em>not</em> take locale into account.  The
     * {@link java.text.Collator} class provides locale-sensitive comparison.
     *
     * @param   toffset   the starting offset of the subregion in this string.
     * @param   other     the string argument.
     * @param   ooffset   the starting offset of the subregion in the string
     *                    argument.
     * @param   len       the number of characters to compare.
     * @return  {@code true} if the specified subregion of this string
     *          exactly matches the specified subregion of the string argument;
     *          {@code false} otherwise.
     */
    public boolean regionMatches(int toffset, InlineString other, int ooffset, int len) {
        // Note: toffset, ooffset, or len might be near -1>>>1.
        if ((ooffset < 0) || (toffset < 0) ||
                (toffset > (long)length() - len) ||
                (ooffset > (long)other.length() - len)) {
            return false;
        } else if (len <= 0) {
            return true;
        }
        if (this.isCompressed() || other.isCompressed()) {
            InlineString s1, s2;
            int offset1, offset2;
            if (this.isCompressed()) {
                s1 = this; s2 = other; offset1 = toffset; offset2 = ooffset;
            } else {
                s1 = other; s2 = this; offset1 = ooffset; offset2 = toffset;
            }
            if (s2.isLatin1()) {
                return StringCompressed.regionMatchesLatin1(s1, offset1, s2, offset2, len);
            } else {
                return StringCompressed.regionMatchesUTF16(s1, offset1, s2, offset2, len);
            }
        } else {
            byte[] tv = value;
            byte[] ov = other.value;
            long tc = firstHalf;
            long oc = other.firstHalf;
            if (tc == oc) {
                if (!isLatin1()) {
                    toffset = toffset << 1;
                    ooffset = ooffset << 1;
                    len = len << 1;
                }
                while (len-- > 0) {
                    if (tv[toffset++] != ov[ooffset++]) {
                        return false;
                    }
                }
            } else {
                if (tc == Utils.LATIN1) {
                    while (len-- > 0) {
                        if (StringLatin1.getChar(tv, toffset++) !=
                                StringUTF16.getChar(ov, ooffset++)) {
                            return false;
                        }
                    }
                } else {
                    while (len-- > 0) {
                        if (StringUTF16.getChar(tv, toffset++) !=
                                StringLatin1.getChar(ov, ooffset++)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
    }

    /**
     * Tests if two string regions are equal.
     * <p>
     * A substring of this {@code String} object is compared to a substring
     * of the argument {@code other}. The result is {@code true} if these
     * substrings represent Unicode code point sequences that are the same,
     * ignoring case if and only if {@code ignoreCase} is true.
     * The sequences {@code tsequence} and {@code osequence} are compared,
     * where {@code tsequence} is the sequence produced as if by calling
     * {@code this.substring(toffset, toffset + len).codePoints()} and
     * {@code osequence} is the sequence produced as if by calling
     * {@code other.substring(ooffset, ooffset + len).codePoints()}.
     * The result is {@code true} if and only if all of the following
     * are true:
     * <ul><li>{@code toffset} is non-negative.
     * <li>{@code ooffset} is non-negative.
     * <li>{@code toffset+len} is less than or equal to the length of this
     * {@code String} object.
     * <li>{@code ooffset+len} is less than or equal to the length of the other
     * argument.
     * <li>if {@code ignoreCase} is {@code false}, all pairs of corresponding Unicode
     * code points are equal integer values; or if {@code ignoreCase} is {@code true},
     * {@link Character#toLowerCase(int) Character.toLowerCase(}
     * {@link Character#toUpperCase(int)}{@code )} on all pairs of Unicode code points
     * results in equal integer values.
     * </ul>
     *
     * <p>Note that this method does <em>not</em> take locale into account,
     * and will result in unsatisfactory results for certain locales when
     * {@code ignoreCase} is {@code true}.  The {@link java.text.Collator} class
     * provides locale-sensitive comparison.
     *
     * @param   ignoreCase   if {@code true}, ignore case when comparing
     *                       characters.
     * @param   toffset      the starting offset of the subregion in this
     *                       string.
     * @param   other        the string argument.
     * @param   ooffset      the starting offset of the subregion in the string
     *                       argument.
     * @param   len          the number of characters (Unicode code units -
     *                       16bit {@code char} value) to compare.
     * @return  {@code true} if the specified subregion of this string
     *          matches the specified subregion of the string argument;
     *          {@code false} otherwise. Whether the matching is exact
     *          or case insensitive depends on the {@code ignoreCase}
     *          argument.
     * @see     #codePoints()
     */
    public boolean regionMatches(boolean ignoreCase, int toffset,
                                 InlineString other, int ooffset, int len) {
        if (!ignoreCase) {
            return regionMatches(toffset, other, ooffset, len);
        }
        // Note: toffset, ooffset, or len might be near -1>>>1.
        if ((ooffset < 0) || (toffset < 0)
                || (toffset > (long)length() - len)
                || (ooffset > (long)other.length() - len)) {
            return false;
        }
        byte[] tv = content();
        byte[] ov = other.content();
        if (coder() == other.coder()) {
            return isLatin1()
                    ? StringLatin1.regionMatchesCI(tv, toffset, ov, ooffset, len)
                    : StringUTF16.regionMatchesCI(tv, toffset, ov, ooffset, len);
        }
        return isLatin1()
                ? StringLatin1.regionMatchesCI_UTF16(tv, toffset, ov, ooffset, len)
                : StringUTF16.regionMatchesCI_Latin1(tv, toffset, ov, ooffset, len);
    }

    /**
     * Tests if the substring of this string beginning at the
     * specified index starts with the specified prefix.
     *
     * @param   prefix    the prefix.
     * @param   toffset   where to begin looking in this string.
     * @return  {@code true} if the character sequence represented by the
     *          argument is a prefix of the substring of this object starting
     *          at index {@code toffset}; {@code false} otherwise.
     *          The result is {@code false} if {@code toffset} is
     *          negative or greater than the length of this
     *          {@code String} object; otherwise the result is the same
     *          as the result of the expression
     *          <pre>
     *          this.substring(toffset).startsWith(prefix)
     *          </pre>
     */
    public boolean startsWith(InlineString prefix, int toffset) {
        // Note: toffset might be near -1>>>1.
        if (toffset < 0 || toffset > this.length() - prefix.length()) {
            return false;
        } else if (prefix.isEmpty()) {
            return true;
        }
        if (prefix.isCompressed()) {
            if (this.isLatin1()) {
                return StringCompressed.regionMatchesLatin1(prefix, 0, this, toffset, prefix.length());
            } else {
                return StringCompressed.regionMatchesUTF16(prefix, 0, this, toffset, prefix.length());
            }
        } else if (this.isCompressed()) {
            return false;
        } else {
            byte ta[] = value;
            byte pa[] = prefix.value;
            int po = 0;
            int pc = pa.length;
            long coder = this.firstHalf;
            if (coder == prefix.firstHalf) {
                int to = (coder == Utils.LATIN1) ? toffset : toffset << 1;
                while (po < pc) {
                    if (ta[to++] != pa[po++]) {
                        return false;
                    }
                }
            } else {
                if (coder == Utils.LATIN1) {  // && pcoder == UTF16
                    return false;
                }
                // coder == UTF16 && pcoder == LATIN1)
                while (po < pc) {
                    if (StringUTF16.getChar(ta, toffset++) != (pa[po++] & 0xff)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    /**
     * Tests if this string starts with the specified prefix.
     *
     * @param   prefix   the prefix.
     * @return  {@code true} if the character sequence represented by the
     *          argument is a prefix of the character sequence represented by
     *          this string; {@code false} otherwise.
     *          Note also that {@code true} will be returned if the
     *          argument is an empty string or is equal to this
     *          {@code String} object as determined by the
     *          {@link #equals(Object)} method.
     * @since   1.0
     */
    public boolean startsWith(InlineString prefix) {
        return startsWith(prefix, 0);
    }

    /**
     * Tests if this string ends with the specified suffix.
     *
     * @param   suffix   the suffix.
     * @return  {@code true} if the character sequence represented by the
     *          argument is a suffix of the character sequence represented by
     *          this object; {@code false} otherwise. Note that the
     *          result will be {@code true} if the argument is the
     *          empty string or is equal to this {@code String} object
     *          as determined by the {@link #equals(Object)} method.
     */
    public boolean endsWith(InlineString suffix) {
        return startsWith(suffix, length() - suffix.length());
    }

    /**
     * Returns a hash code for this string. The hash code for a
     * {@code String} object is computed as
     * <blockquote><pre>
     * s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]
     * </pre></blockquote>
     * using {@code int} arithmetic, where {@code s[i]} is the
     * <i>i</i>th character of the string, {@code n} is the length of
     * the string, and {@code ^} indicates exponentiation.
     * (The hash value of the empty string is zero.)
     *
     * @return  a hash code value for this object.
     */
    public int hashCode() {
        if (isCompressed()) {
            return StringCompressed.hashCode(firstHalf, secondHalf, length);
        } else if (isLatin1()) {
            return StringLatin1.hashCode(value);
        } else {
            return StringUTF16.hashCode(value);
        }
    }

    /**
     * Returns the index within this string of the first occurrence of
     * the specified character. If a character with value
     * {@code ch} occurs in the character sequence represented by
     * this {@code String} object, then the index (in Unicode
     * code units) of the first such occurrence is returned. For
     * values of {@code ch} in the range from 0 to 0xFFFF
     * (inclusive), this is the smallest value <i>k</i> such that:
     * <blockquote><pre>
     * this.charAt(<i>k</i>) == ch
     * </pre></blockquote>
     * is true. For other values of {@code ch}, it is the
     * smallest value <i>k</i> such that:
     * <blockquote><pre>
     * this.codePointAt(<i>k</i>) == ch
     * </pre></blockquote>
     * is true. In either case, if no such character occurs in this
     * string, then {@code -1} is returned.
     *
     * @param   ch   a character (Unicode code point).
     * @return  the index of the first occurrence of the character in the
     *          character sequence represented by this object, or
     *          {@code -1} if the character does not occur.
     */
    public int indexOf(int ch) {
        return indexOf(ch, 0);
    }

    /**
     * Returns the index within this string of the first occurrence of the
     * specified character, starting the search at the specified index.
     * <p>
     * If a character with value {@code ch} occurs in the
     * character sequence represented by this {@code String}
     * object at an index no smaller than {@code fromIndex}, then
     * the index of the first such occurrence is returned. For values
     * of {@code ch} in the range from 0 to 0xFFFF (inclusive),
     * this is the smallest value <i>k</i> such that:
     * <blockquote><pre>
     * (this.charAt(<i>k</i>) == ch) {@code &&} (<i>k</i> &gt;= fromIndex)
     * </pre></blockquote>
     * is true. For other values of {@code ch}, it is the
     * smallest value <i>k</i> such that:
     * <blockquote><pre>
     * (this.codePointAt(<i>k</i>) == ch) {@code &&} (<i>k</i> &gt;= fromIndex)
     * </pre></blockquote>
     * is true. In either case, if no such character occurs in this
     * string at or after position {@code fromIndex}, then
     * {@code -1} is returned.
     *
     * <p>
     * There is no restriction on the value of {@code fromIndex}. If it
     * is negative, it has the same effect as if it were zero: this entire
     * string may be searched. If it is greater than the length of this
     * string, it has the same effect as if it were equal to the length of
     * this string: {@code -1} is returned.
     *
     * <p>All indices are specified in {@code char} values
     * (Unicode code units).
     *
     * @param   ch          a character (Unicode code point).
     * @param   fromIndex   the index to start the search from.
     * @return  the index of the first occurrence of the character in the
     *          character sequence represented by this object that is greater
     *          than or equal to {@code fromIndex}, or {@code -1}
     *          if the character does not occur.
     */
    public int indexOf(int ch, int fromIndex) {
        if (isCompressed()) {
            return StringCompressed.indexOf(firstHalf, secondHalf, length(), ch, fromIndex);
        } else if (isLatin1()) {
            return StringLatin1.indexOf(value, ch, fromIndex);
        } else {
            return StringUTF16.indexOf(value, ch, fromIndex);
        }
    }

    /**
     * Returns the index within this string of the last occurrence of
     * the specified character. For values of {@code ch} in the
     * range from 0 to 0xFFFF (inclusive), the index (in Unicode code
     * units) returned is the largest value <i>k</i> such that:
     * <blockquote><pre>
     * this.charAt(<i>k</i>) == ch
     * </pre></blockquote>
     * is true. For other values of {@code ch}, it is the
     * largest value <i>k</i> such that:
     * <blockquote><pre>
     * this.codePointAt(<i>k</i>) == ch
     * </pre></blockquote>
     * is true.  In either case, if no such character occurs in this
     * string, then {@code -1} is returned.  The
     * {@code String} is searched backwards starting at the last
     * character.
     *
     * @param   ch   a character (Unicode code point).
     * @return  the index of the last occurrence of the character in the
     *          character sequence represented by this object, or
     *          {@code -1} if the character does not occur.
     */
    public int lastIndexOf(int ch) {
        return lastIndexOf(ch, length() - 1);
    }

    /**
     * Returns the index within this string of the last occurrence of
     * the specified character, searching backward starting at the
     * specified index. For values of {@code ch} in the range
     * from 0 to 0xFFFF (inclusive), the index returned is the largest
     * value <i>k</i> such that:
     * <blockquote><pre>
     * (this.charAt(<i>k</i>) == ch) {@code &&} (<i>k</i> &lt;= fromIndex)
     * </pre></blockquote>
     * is true. For other values of {@code ch}, it is the
     * largest value <i>k</i> such that:
     * <blockquote><pre>
     * (this.codePointAt(<i>k</i>) == ch) {@code &&} (<i>k</i> &lt;= fromIndex)
     * </pre></blockquote>
     * is true. In either case, if no such character occurs in this
     * string at or before position {@code fromIndex}, then
     * {@code -1} is returned.
     *
     * <p>All indices are specified in {@code char} values
     * (Unicode code units).
     *
     * @param   ch          a character (Unicode code point).
     * @param   fromIndex   the index to start the search from. There is no
     *          restriction on the value of {@code fromIndex}. If it is
     *          greater than or equal to the length of this string, it has
     *          the same effect as if it were equal to one less than the
     *          length of this string: this entire string may be searched.
     *          If it is negative, it has the same effect as if it were -1:
     *          -1 is returned.
     * @return  the index of the last occurrence of the character in the
     *          character sequence represented by this object that is less
     *          than or equal to {@code fromIndex}, or {@code -1}
     *          if the character does not occur before that point.
     */
    public int lastIndexOf(int ch, int fromIndex) {
        if (isCompressed()) {
            return StringCompressed.lastIndexOf(firstHalf, secondHalf, length(), ch, fromIndex);
        } else if (isLatin1()) {
            return StringLatin1.lastIndexOf(value, ch, fromIndex);
        } else {
            return StringUTF16.lastIndexOf(value, ch, fromIndex);
        }
    }

    /**
     * Returns the index within this string of the first occurrence of the
     * specified substring.
     *
     * <p>The returned index is the smallest value {@code k} for which:
     * <pre>{@code
     * this.startsWith(str, k)
     * }</pre>
     * If no such value of {@code k} exists, then {@code -1} is returned.
     *
     * @param   str   the substring to search for.
     * @return  the index of the first occurrence of the specified substring,
     *          or {@code -1} if there is no such occurrence.
     */
    public int indexOf(InlineString str) {
        if (str.isEmpty()) {
            return 0;
        }
        if (str.isCompressed()) {
            return indexOfCompressedStr(str, 0);
        } else if (this.isCompressed()) {
            return -1;
        } else {
            long coder = this.firstHalf;
            if (coder == str.firstHalf) {
                return this.isLatin1() ? StringLatin1.indexOf(this.value, str.value)
                        : StringUTF16.indexOf(this.value, str.value);
            } else {
                if (coder == Utils.LATIN1) {  // str.coder == String.UTF16
                    return -1;
                } else {
                    return StringUTF16.indexOfLatin1(this.value, str.value);
                }
            }
        }
    }

    /**
     * Returns the index within this string of the first occurrence of the
     * specified substring, starting at the specified index.
     *
     * <p>The returned index is the smallest value {@code k} for which:
     * <pre>{@code
     *     k >= Math.min(fromIndex, this.length()) &&
     *                   this.startsWith(str, k)
     * }</pre>
     * If no such value of {@code k} exists, then {@code -1} is returned.
     *
     * @param   str         the substring to search for.
     * @param   fromIndex   the index from which to start the search.
     * @return  the index of the first occurrence of the specified substring,
     *          starting at the specified index,
     *          or {@code -1} if there is no such occurrence.
     */
    public int indexOf(InlineString str, int fromIndex) {
        fromIndex = Math.max(0, fromIndex);
        if (str.isEmpty()) {
            return Math.min(fromIndex, this.length());
        }
        if (str.isCompressed()) {
            return indexOfCompressedStr(str, fromIndex);
        } else if (this.isCompressed()) {
            return -1;
        } else {
            long srcCoder = this.firstHalf;
            byte[] src = this.value;
            int srcCount = this.length();
            byte[] tgt = str.value;
            int tgtCount = str.length();

            if (srcCoder == str.firstHalf) {
                return srcCoder == Utils.LATIN1
                        ? StringLatin1.indexOf(src, srcCount, tgt, tgtCount, fromIndex)
                        : StringUTF16.indexOf(src, srcCount, tgt, tgtCount, fromIndex);
            } else {
                if (srcCoder == Utils.LATIN1) {    //  && tgtCoder == String.UTF16
                    return -1;
                } else {
                    // srcCoder == String.UTF16 && tgtCoder == String.LATIN1) {
                    return StringUTF16.indexOfLatin1(src, srcCount, tgt, tgtCount, fromIndex);
                }
            }
        }
    }

    /**
     * str is a nonempty compressed string
     */
    private int indexOfCompressedStr(InlineString str, int fromIndex) {
        int c = StringCompressed.codePointAt(str.firstHalf, str.secondHalf, 0);
        if (this.isCompressed()) {
            int i = StringCompressed.indexOf(this.firstHalf, this.secondHalf, this.length, c, fromIndex);
            for (; i >= 0 && i <= this.length() - str.length(); i = StringCompressed.indexOf(this.firstHalf, this.secondHalf, this.length, c, i + 1)) {
                if (StringCompressed.regionMatchesLatin1(str, 0, this, i, str.length())) {
                    return i;
                }
            }
        } else if (this.isLatin1()) {
            int i = StringLatin1.indexOf(this.value, c, fromIndex);
            for (; i >= 0 && i <= this.length() - str.length(); i = StringLatin1.indexOf(this.value, c, i + 1)) {
                if (StringCompressed.regionMatchesLatin1(str, 0, this, i, str.length())) {
                    return i;
                }
            }
        } else {
            int i = StringUTF16.indexOf(this.value, c, fromIndex);
            for (; i >= 0 && i <= this.length() - str.length(); i = StringUTF16.indexOf(this.value, c, i + 1)) {
                if (StringCompressed.regionMatchesUTF16(str, 0, this, i, str.length())) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Returns the index within this string of the last occurrence of the
     * specified substring.  The last occurrence of the empty string ""
     * is considered to occur at the index value {@code this.length()}.
     *
     * <p>The returned index is the largest value {@code k} for which:
     * <pre>{@code
     * this.startsWith(str, k)
     * }</pre>
     * If no such value of {@code k} exists, then {@code -1} is returned.
     *
     * @param   str   the substring to search for.
     * @return  the index of the last occurrence of the specified substring,
     *          or {@code -1} if there is no such occurrence.
     */
    public int lastIndexOf(InlineString str) {
        return lastIndexOf(str, length());
    }

    /**
     * Returns the index within this string of the last occurrence of the
     * specified substring, searching backward starting at the specified index.
     *
     * <p>The returned index is the largest value {@code k} for which:
     * <pre>{@code
     *     k <= Math.min(fromIndex, this.length()) &&
     *                   this.startsWith(str, k)
     * }</pre>
     * If no such value of {@code k} exists, then {@code -1} is returned.
     *
     * @param   str         the substring to search for.
     * @param   fromIndex   the index to start the search from.
     * @return  the index of the last occurrence of the specified substring,
     *          searching backward from the specified index,
     *          or {@code -1} if there is no such occurrence.
     */
    public int lastIndexOf(InlineString str, int fromIndex) {
        if (fromIndex < 0) {
            return -1;
        } else if (str.isEmpty()) {
            return Math.min(fromIndex, this.length());
        }
        fromIndex = Math.min(fromIndex, this.length() - str.length());
        if (str.isCompressed()) {
            int c = StringCompressed.codePointAt(str.firstHalf, str.secondHalf, 0);
            if (this.isCompressed()) {
                int i = StringCompressed.lastIndexOf(this.firstHalf, this.secondHalf, this.length(), c, fromIndex);
                for (; i >= 0; i = StringCompressed.lastIndexOf(this.firstHalf, this.secondHalf, this.length(), c, i - 1)) {
                    if (StringCompressed.regionMatchesLatin1(str, 0, this, i, str.length())) {
                        return i;
                    }
                }
            } else if (this.isLatin1()) {
                int i = StringLatin1.lastIndexOf(this.value, c, fromIndex);
                for (; i >= 0; i = StringLatin1.lastIndexOf(this.value, c, i - 1)) {
                    if (StringCompressed.regionMatchesLatin1(str, 0, this, i, str.length())) {
                        return i;
                    }
                }
            } else {
                int i = StringUTF16.lastIndexOf(this.value, c, fromIndex);
                for (; i >= 0; i = StringUTF16.lastIndexOf(this.value, c, i - 1)) {
                    if (StringCompressed.regionMatchesUTF16(str, 0, this, i, str.length())) {
                        return i;
                    }
                }
            }
            return -1;
        } else if (this.isCompressed()) {
            return -1;
        } else {
            byte[] src = this.value;
            long srcCoder = this.firstHalf;
            int srcCount = this.length();
            byte[] tgt = str.value;
            long tgtCoder = str.firstHalf;
            int tgtCount = str.length();
            if (srcCoder == tgtCoder) {
                return srcCoder == Utils.LATIN1
                        ? StringLatin1.lastIndexOf(src, srcCount, tgt, tgtCount, fromIndex)
                        : StringUTF16.lastIndexOf(src, srcCount, tgt, tgtCount, fromIndex);
            } else {
                if (srcCoder == Utils.LATIN1) {    // && tgtCoder == String.UTF16
                    return -1;
                } else {
                    // srcCoder == String.UTF16 && tgtCoder == String.LATIN1
                    return StringUTF16.lastIndexOfLatin1(src, srcCount, tgt, tgtCount, fromIndex);
                }
            }
        }
    }

    /**
     * Returns a string that is a substring of this string. The
     * substring begins with the character at the specified index and
     * extends to the end of this string. <p>
     * Examples:
     * <blockquote><pre>
     * "unhappy".substring(2) returns "happy"
     * "Harbison".substring(3) returns "bison"
     * "emptiness".substring(9) returns "" (an empty string)
     * </pre></blockquote>
     *
     * @param      beginIndex   the beginning index, inclusive.
     * @return     the specified substring.
     * @throws     IndexOutOfBoundsException  if
     *             {@code beginIndex} is negative or larger than the
     *             length of this {@code String} object.
     */
    public InlineString substring(int beginIndex) {
        return substring(beginIndex, length());
    }

    /**
     * Returns a string that is a substring of this string. The
     * substring begins at the specified {@code beginIndex} and
     * extends to the character at index {@code endIndex - 1}.
     * Thus the length of the substring is {@code endIndex-beginIndex}.
     * <p>
     * Examples:
     * <blockquote><pre>
     * "hamburger".substring(4, 8) returns "urge"
     * "smiles".substring(1, 5) returns "mile"
     * </pre></blockquote>
     *
     * @param      beginIndex   the beginning index, inclusive.
     * @param      endIndex     the ending index, exclusive.
     * @return     the specified substring.
     * @throws     IndexOutOfBoundsException  if the
     *             {@code beginIndex} is negative, or
     *             {@code endIndex} is larger than the length of
     *             this {@code String} object, or
     *             {@code beginIndex} is larger than
     *             {@code endIndex}.
     */
    public InlineString substring(int beginIndex, int endIndex) {
        int length = length();
        checkBoundsBeginEnd(beginIndex, endIndex, length);
        if (beginIndex == 0 && endIndex == length) {
            return this;
        }
        int subLen = endIndex - beginIndex;
        if (this.isCompressed()) {
            return StringCompressed.newString(firstHalf, secondHalf, beginIndex, subLen);
        } else if (isLatin1()) {
            return StringLatin1.newString(value, beginIndex, subLen);
        } else {
            return StringUTF16.newString(value, beginIndex, subLen);
        }
    }

    /**
     * Returns a character sequence that is a subsequence of this sequence.
     *
     * <p> An invocation of this method of the form
     *
     * <blockquote><pre>
     * str.subSequence(begin,&nbsp;end)</pre></blockquote>
     *
     * behaves in exactly the same way as the invocation
     *
     * <blockquote><pre>
     * str.substring(begin,&nbsp;end)</pre></blockquote>
     *
     * @apiNote
     * This method is defined so that the {@code String} class can implement
     * the {@link CharSequence} interface.
     *
     * @param   beginIndex   the begin index, inclusive.
     * @param   endIndex     the end index, exclusive.
     * @return  the specified subsequence.
     *
     * @throws  IndexOutOfBoundsException
     *          if {@code beginIndex} or {@code endIndex} is negative,
     *          if {@code endIndex} is greater than {@code length()},
     *          or if {@code beginIndex} is greater than {@code endIndex}
     *
     * @since 1.4
     */
    @Override
    public CharSequence subSequence(int beginIndex, int endIndex) {
        return this.substring(beginIndex, endIndex);
    }

    /**
     * Concatenates the specified string to the end of this string.
     * <p>
     * If the length of the argument string is {@code 0}, then this
     * {@code String} object is returned. Otherwise, a
     * {@code String} object is returned that represents a character
     * sequence that is the concatenation of the character sequence
     * represented by this {@code String} object and the character
     * sequence represented by the argument string.<p>
     * Examples:
     * <blockquote><pre>
     * "cares".concat("s") returns "caress"
     * "to".concat("get").concat("her") returns "together"
     * </pre></blockquote>
     *
     * @param   str   the {@code String} that is concatenated to the end
     *                of this {@code String}.
     * @return  a string that represents the concatenation of this object's
     *          characters followed by the string argument's characters.
     */
    public InlineString concat(InlineString str) {
        return StringConcatHelper.simpleConcat(this, str);
    }

    /**
     * Returns a string resulting from replacing all occurrences of
     * {@code oldChar} in this string with {@code newChar}.
     * <p>
     * If the character {@code oldChar} does not occur in the
     * character sequence represented by this {@code String} object,
     * then a reference to this {@code String} object is returned.
     * Otherwise, a {@code String} object is returned that
     * represents a character sequence identical to the character sequence
     * represented by this {@code String} object, except that every
     * occurrence of {@code oldChar} is replaced by an occurrence
     * of {@code newChar}.
     * <p>
     * Examples:
     * <blockquote><pre>
     * "mesquite in your cellar".replace('e', 'o')
     *         returns "mosquito in your collar"
     * "the war of baronets".replace('r', 'y')
     *         returns "the way of bayonets"
     * "sparring with a purple porpoise".replace('p', 't')
     *         returns "starring with a turtle tortoise"
     * "JonL".replace('q', 'x') returns "JonL" (no change)
     * </pre></blockquote>
     *
     * @param   oldChar   the old character.
     * @param   newChar   the new character.
     * @return  a string derived from this string by replacing every
     *          occurrence of {@code oldChar} with {@code newChar}.
     */
    public InlineString replace(char oldChar, char newChar) {
        if (oldChar != newChar) {
            if (isCompressed()) {
                return StringCompressed.replace(firstHalf, secondHalf, length(), oldChar, newChar);
            } else if (isLatin1()) {
                return StringLatin1.replace(content(), oldChar, newChar);
            } else {
                return StringUTF16.replace(content(), oldChar, newChar);
            }
        } else {
            return this;
        }
    }

    /**
     * Tells whether or not this string matches the given <a
     * href="../util/regex/Pattern.html#sum">regular expression</a>.
     *
     * <p> An invocation of this method of the form
     * <i>str</i>{@code .matches(}<i>regex</i>{@code )} yields exactly the
     * same result as the expression
     *
     * <blockquote>
     * {@link java.util.regex.Pattern}.{@link java.util.regex.Pattern#matches(String,CharSequence)
     * matches(<i>regex</i>, <i>str</i>)}
     * </blockquote>
     *
     * @param   regex
     *          the regular expression to which this string is to be matched
     *
     * @return  {@code true} if, and only if, this string matches the
     *          given regular expression
     *
     * @throws  PatternSyntaxException
     *          if the regular expression's syntax is invalid
     *
     * @see java.util.regex.Pattern
     *
     * @since 1.4
     */
    public boolean matches(String regex) {
        return Pattern.matches(regex, this.toString());
    }

    /**
     * Returns true if and only if this string contains the specified
     * substring.
     *
     * @param s the string to search for
     * @return true if this string contains {@code s}, false otherwise
     * @since 1.5
     */
    public boolean contains(InlineString s) {
        return indexOf(s) >= 0;
    }

    /**
     * Returns true if and only if this string contains the specified
     * sequence of char values.
     *
     * @param s the sequence to search for
     * @return true if this string contains {@code s}, false otherwise
     * @since 1.5
     */
    public boolean contains(CharSequence s) {
        return contains(new InlineString(s.toString()));
    }

    /**
     * Replaces the first substring of this string that matches the given <a
     * href="../util/regex/Pattern.html#sum">regular expression</a> with the
     * given replacement.
     *
     * <p> An invocation of this method of the form
     * <i>str</i>{@code .replaceFirst(}<i>regex</i>{@code ,} <i>repl</i>{@code )}
     * yields exactly the same result as the expression
     *
     * <blockquote>
     * <code>
     * {@link java.util.regex.Pattern}.{@link
     * java.util.regex.Pattern#compile(String) compile}(<i>regex</i>).{@link
     * java.util.regex.Pattern#matcher(java.lang.CharSequence) matcher}(<i>str</i>).{@link
     * java.util.regex.Matcher#replaceFirst(String) replaceFirst}(<i>repl</i>)
     * </code>
     * </blockquote>
     *
     *<p>
     * Note that backslashes ({@code \}) and dollar signs ({@code $}) in the
     * replacement string may cause the results to be different than if it were
     * being treated as a literal replacement string; see
     * {@link java.util.regex.Matcher#replaceFirst}.
     * Use {@link java.util.regex.Matcher#quoteReplacement} to suppress the special
     * meaning of these characters, if desired.
     *
     * @param   regex
     *          the regular expression to which this string is to be matched
     * @param   replacement
     *          the string to be substituted for the first match
     *
     * @return  The resulting {@code String}
     *
     * @throws  PatternSyntaxException
     *          if the regular expression's syntax is invalid
     *
     * @see java.util.regex.Pattern
     *
     * @since 1.4
     */
    public InlineString replaceFirst(String regex, String replacement) {
        return new InlineString(Pattern.compile(regex).matcher(this.toString()).replaceFirst(replacement));
    }

    /**
     * Replaces each substring of this string that matches the given <a
     * href="../util/regex/Pattern.html#sum">regular expression</a> with the
     * given replacement.
     *
     * <p> An invocation of this method of the form
     * <i>str</i>{@code .replaceAll(}<i>regex</i>{@code ,} <i>repl</i>{@code )}
     * yields exactly the same result as the expression
     *
     * <blockquote>
     * <code>
     * {@link java.util.regex.Pattern}.{@link
     * java.util.regex.Pattern#compile(String) compile}(<i>regex</i>).{@link
     * java.util.regex.Pattern#matcher(java.lang.CharSequence) matcher}(<i>str</i>).{@link
     * java.util.regex.Matcher#replaceAll(String) replaceAll}(<i>repl</i>)
     * </code>
     * </blockquote>
     *
     *<p>
     * Note that backslashes ({@code \}) and dollar signs ({@code $}) in the
     * replacement string may cause the results to be different than if it were
     * being treated as a literal replacement string; see
     * {@link java.util.regex.Matcher#replaceAll Matcher.replaceAll}.
     * Use {@link java.util.regex.Matcher#quoteReplacement} to suppress the special
     * meaning of these characters, if desired.
     *
     * @param   regex
     *          the regular expression to which this string is to be matched
     * @param   replacement
     *          the string to be substituted for each match
     *
     * @return  The resulting {@code String}
     *
     * @throws  PatternSyntaxException
     *          if the regular expression's syntax is invalid
     *
     * @see java.util.regex.Pattern
     *
     * @since 1.4
     */
    public InlineString replaceAll(String regex, String replacement) {
        return new InlineString(Pattern.compile(regex).matcher(this.toString()).replaceAll(replacement));
    }

    /**
     * Replaces each substring of this string that matches the literal target
     * sequence with the specified literal replacement sequence. The
     * replacement proceeds from the beginning of the string to the end, for
     * example, replacing "aa" with "b" in the string "aaa" will result in
     * "ba" rather than "ab".
     *
     * @param  target The sequence of char values to be replaced
     * @param  replacement The replacement sequence of char values
     * @return  The resulting string
     * @since 1.5
     */
    public InlineString replace(InlineString trgtStr, InlineString replStr) {
        int thisLen = length();
        int trgtLen = trgtStr.length();
        int replLen = replStr.length();

        if (trgtLen > 0) {
            if (trgtLen == 1 && replLen == 1) {
                return replace(trgtStr.charAt(0), replStr.charAt(0));
            }

            boolean thisIsLatin1 = this.isLatin1();
            boolean trgtIsLatin1 = trgtStr.isLatin1();
            boolean replIsLatin1 = replStr.isLatin1();
            return (thisIsLatin1 && trgtIsLatin1 && replIsLatin1)
                    ? StringLatin1.replace(content(), thisLen,
                    trgtStr.content(), trgtLen,
                    replStr.content(), replLen)
                    : StringUTF16.replace(content(), thisLen, thisIsLatin1,
                    trgtStr.content(), trgtLen, trgtIsLatin1,
                    replStr.content(), replLen, replIsLatin1);
        } else { // trgtLen == 0
            long resultLen = thisLen + (thisLen + 1L) * replLen;
            if (resultLen > Integer.MAX_VALUE) {
                throw new OutOfMemoryError("Required index exceeds implementation limit");
            }

            var sb = new StringBuilder((int)resultLen).append(replStr.toString());
            this.chars().forEach(c -> {
                sb.append((char)c).append(replStr.toString());
            });
            return new InlineString(sb);
        }
    }

    /**
     * Replaces each substring of this string that matches the literal target
     * sequence with the specified literal replacement sequence. The
     * replacement proceeds from the beginning of the string to the end, for
     * example, replacing "aa" with "b" in the string "aaa" will result in
     * "ba" rather than "ab".
     *
     * @param  target The sequence of char values to be replaced
     * @param  replacement The replacement sequence of char values
     * @return  The resulting string
     * @since 1.5
     */
    public InlineString replace(CharSequence target, CharSequence replacement) {
        return this.replace(new InlineString(target.toString()), new InlineString(replacement.toString()));
    }

    /**
     * Splits this string around matches of the given
     * <a href="../util/regex/Pattern.html#sum">regular expression</a>.
     *
     * <p> The array returned by this method contains each substring of this
     * string that is terminated by another substring that matches the given
     * expression or is terminated by the end of the string.  The substrings in
     * the array are in the order in which they occur in this string.  If the
     * expression does not match any part of the input then the resulting array
     * has just one element, namely this string.
     *
     * <p> When there is a positive-width match at the beginning of this
     * string then an empty leading substring is included at the beginning
     * of the resulting array. A zero-width match at the beginning however
     * never produces such empty leading substring.
     *
     * <p> The {@code limit} parameter controls the number of times the
     * pattern is applied and therefore affects the length of the resulting
     * array.
     * <ul>
     *    <li><p>
     *    If the <i>limit</i> is positive then the pattern will be applied
     *    at most <i>limit</i>&nbsp;-&nbsp;1 times, the array's length will be
     *    no greater than <i>limit</i>, and the array's last entry will contain
     *    all input beyond the last matched delimiter.</p></li>
     *
     *    <li><p>
     *    If the <i>limit</i> is zero then the pattern will be applied as
     *    many times as possible, the array can have any length, and trailing
     *    empty strings will be discarded.</p></li>
     *
     *    <li><p>
     *    If the <i>limit</i> is negative then the pattern will be applied
     *    as many times as possible and the array can have any length.</p></li>
     * </ul>
     *
     * <p> The string {@code "boo:and:foo"}, for example, yields the
     * following results with these parameters:
     *
     * <blockquote><table class="plain">
     * <caption style="display:none">Split example showing regex, limit, and result</caption>
     * <thead>
     * <tr>
     *     <th scope="col">Regex</th>
     *     <th scope="col">Limit</th>
     *     <th scope="col">Result</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr><th scope="row" rowspan="3" style="font-weight:normal">:</th>
     *     <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">2</th>
     *     <td>{@code { "boo", "and:foo" }}</td></tr>
     * <tr><!-- : -->
     *     <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">5</th>
     *     <td>{@code { "boo", "and", "foo" }}</td></tr>
     * <tr><!-- : -->
     *     <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">-2</th>
     *     <td>{@code { "boo", "and", "foo" }}</td></tr>
     * <tr><th scope="row" rowspan="3" style="font-weight:normal">o</th>
     *     <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">5</th>
     *     <td>{@code { "b", "", ":and:f", "", "" }}</td></tr>
     * <tr><!-- o -->
     *     <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">-2</th>
     *     <td>{@code { "b", "", ":and:f", "", "" }}</td></tr>
     * <tr><!-- o -->
     *     <th scope="row" style="font-weight:normal; text-align:right; padding-right:1em">0</th>
     *     <td>{@code { "b", "", ":and:f" }}</td></tr>
     * </tbody>
     * </table></blockquote>
     *
     * <p> An invocation of this method of the form
     * <i>str.</i>{@code split(}<i>regex</i>{@code ,}&nbsp;<i>n</i>{@code )}
     * yields the same result as the expression
     *
     * <blockquote>
     * <code>
     * {@link java.util.regex.Pattern}.{@link
     * java.util.regex.Pattern#compile(String) compile}(<i>regex</i>).{@link
     * java.util.regex.Pattern#split(java.lang.CharSequence,int) split}(<i>str</i>,&nbsp;<i>n</i>)
     * </code>
     * </blockquote>
     *
     *
     * @param  regex
     *         the delimiting regular expression
     *
     * @param  limit
     *         the result threshold, as described above
     *
     * @return  the array of strings computed by splitting this string
     *          around matches of the given regular expression
     *
     * @throws  PatternSyntaxException
     *          if the regular expression's syntax is invalid
     *
     * @see java.util.regex.Pattern
     *
     * @since 1.4
     */
    public InlineString[] split(String regex, int limit) {
        /* fastpath if the regex is a
         * (1) one-char String and this character is not one of the
         *     RegEx's meta characters ".$|()[{^?*+\\", or
         * (2) two-char String and the first char is the backslash and
         *     the second is not the ascii digit or ascii letter.
         */
        char ch;
        if (((regex.length() == 1 &&
                ".$|()[{^?*+\\".indexOf(ch = regex.charAt(0)) == -1) ||
                (regex.length() == 2 &&
                        regex.charAt(0) == '\\' &&
                        (((ch = regex.charAt(1))-'0')|('9'-ch)) < 0 &&
                        ((ch-'a')|('z'-ch)) < 0 &&
                        ((ch-'A')|('Z'-ch)) < 0)) &&
                (ch < Character.MIN_HIGH_SURROGATE ||
                        ch > Character.MAX_LOW_SURROGATE))
        {
            int off = 0;
            int next = 0;
            boolean limited = limit > 0;
            int i = 0;
            InlineString[] list = new InlineString[10];
            while ((next = indexOf(ch, off)) != -1) {
                if (!limited || i < limit - 1) {
                    if (i == list.length) {
                        var newList = new InlineString[i << 1];
                        System.arraycopy(list, 0, newList, 0, i);
                        list = newList;
                    }
                    list[i++] = substring(off, next);
                    off = next + 1;
                } else {    // last one
                    //assert (list.size() == limit - 1);
                    int last = length();
                    if (i == list.length) {
                        var newList = new InlineString[i + 1];
                        System.arraycopy(list, 0, newList, 0, i);
                        list = newList;
                    }
                    list[i++] = substring(off, last);
                    off = last;
                    break;
                }
            }
            // If no match was found, return this
            if (off == 0)
                return new InlineString[]{this};

            // Add remaining segment
            if (!limited || i < limit) {
                if (i == list.length) {
                    var newList = new InlineString[i + 1];
                    System.arraycopy(list, 0, newList, 0, i);
                    list = newList;
                }
                list[i++] = substring(off, length());
            }

            // Construct result
            int resultSize = i;
            if (limit == 0) {
                while (resultSize > 0 && list[resultSize - 1].isEmpty()) {
                    resultSize--;
                }
            }
            if (resultSize == list.length) {
                return list;
            } else {
                var newList = new InlineString[resultSize];
                System.arraycopy(list, 0, newList, 0, resultSize);
                return newList;
            }
        }
        var temp = Pattern.compile(regex).split(this, limit);
        var result = new InlineString[temp.length];
        for (int i = 0; i < temp.length; i++) {
            result[i] = new InlineString(temp[i]);
        }
        return result;
    }

    /**
     * Splits this string around matches of the given <a
     * href="../util/regex/Pattern.html#sum">regular expression</a>.
     *
     * <p> This method works as if by invoking the two-argument {@link
     * #split(String, int) split} method with the given expression and a limit
     * argument of zero.  Trailing empty strings are therefore not included in
     * the resulting array.
     *
     * <p> The string {@code "boo:and:foo"}, for example, yields the following
     * results with these expressions:
     *
     * <blockquote><table class="plain">
     * <caption style="display:none">Split examples showing regex and result</caption>
     * <thead>
     * <tr>
     *  <th scope="col">Regex</th>
     *  <th scope="col">Result</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr><th scope="row" style="text-weight:normal">:</th>
     *     <td>{@code { "boo", "and", "foo" }}</td></tr>
     * <tr><th scope="row" style="text-weight:normal">o</th>
     *     <td>{@code { "b", "", ":and:f" }}</td></tr>
     * </tbody>
     * </table></blockquote>
     *
     *
     * @param  regex
     *         the delimiting regular expression
     *
     * @return  the array of strings computed by splitting this string
     *          around matches of the given regular expression
     *
     * @throws  PatternSyntaxException
     *          if the regular expression's syntax is invalid
     *
     * @see java.util.regex.Pattern
     *
     * @since 1.4
     */
    public InlineString[] split(String regex) {
        return split(regex, 0);
    }

    /**
     * Returns a new String composed of copies of the
     * {@code CharSequence elements} joined together with a copy of
     * the specified {@code delimiter}.
     *
     * <blockquote>For example,
     * <pre>{@code
     *     String message = String.join("-", "Java", "is", "cool");
     *     // message returned is: "Java-is-cool"
     * }</pre></blockquote>
     *
     * Note that if an element is null, then {@code "null"} is added.
     *
     * @param  delimiter the delimiter that separates each element
     * @param  elements the elements to join together.
     *
     * @return a new {@code String} that is composed of the {@code elements}
     *         separated by the {@code delimiter}
     *
     * @throws NullPointerException If {@code delimiter} or {@code elements}
     *         is {@code null}
     *
     * @see java.util.StringJoiner
     * @since 1.8
     */
    public static InlineString join(CharSequence delimiter, CharSequence... elements) {
        var delim = new InlineString(delimiter.toString());
        var elems = new InlineString[elements.length];
        for (int i = 0; i < elements.length; i++) {
            elems[i] = InlineString.valueOf(elements[i]);
        }
        return join(EMPTY_STRING, EMPTY_STRING, delim, elems, elems.length);
    }

    /**
     * Designated join routine.
     *
     * @param prefix the non-null prefix
     * @param suffix the non-null suffix
     * @param delimiter the non-null delimiter
     * @param elements the non-null array of non-null elements
     * @param size the number of elements in the array (<= elements.length)
     * @return the joined string
     */
    static InlineString join(InlineString prefix, InlineString suffix, InlineString delimiter, InlineString[] elements, int size) {
        int icoder = prefix.coder() | suffix.coder();
        long len = (long) prefix.length() + suffix.length();
        if (size > 1) { // when there are more than one element, size - 1 delimiters will be emitted
            len += (long) (size - 1) * delimiter.length();
            icoder |= delimiter.coder();
        }
        // assert len > 0L; // max: (long) Integer.MAX_VALUE << 32
        // following loop wil add max: (long) Integer.MAX_VALUE * Integer.MAX_VALUE to len
        // so len can overflow at most once
        for (int i = 0; i < size; i++) {
            var el = elements[i];
            len += el.length();
            icoder |= el.coder();
        }
        // long len overflow check, char -> byte index, int len overflow check
        long bufLen = len << icoder;
        if (len < 0 || bufLen != (int)bufLen) {
            throw new OutOfMemoryError("Requested string index exceeds VM limit");
        }
        int ilen = (int)len;
        byte coder = (byte) icoder;
        if (StringCompressed.compressible(ilen, coder)) {
            var value = LongVector.zero(LongVector.SPECIES_128)
                    .withLane(0, prefix.firstHalf)
                    .withLane(1, prefix.secondHalf)
                    .reinterpretAsBytes();
            int off = prefix.length();
            if (size > 0) {
                var el = elements[0];
                value = value.or(LongVector.zero(LongVector.SPECIES_128)
                        .withLane(0, el.firstHalf)
                        .withLane(1, el.secondHalf)
                        .reinterpretAsBytes()
                        .rearrange(Helper.BYTE_SLICE[-off & (Helper.BYTE_SLICE.length - 1)]));
                off += el.length();
                for (int i = 1; i < size; i++) {
                    value = value.or(LongVector.zero(LongVector.SPECIES_128)
                            .withLane(0, delimiter.firstHalf)
                            .withLane(1, delimiter.secondHalf)
                            .reinterpretAsBytes()
                            .rearrange(Helper.BYTE_SLICE[-off & (Helper.BYTE_SLICE.length - 1)]));
                    off += delimiter.length();
                    el = elements[i];
                    value = value.or(LongVector.zero(LongVector.SPECIES_128)
                            .withLane(0, el.firstHalf)
                            .withLane(1, el.secondHalf)
                            .reinterpretAsBytes()
                            .rearrange(Helper.BYTE_SLICE[-off & (Helper.BYTE_SLICE.length - 1)]));
                    off += el.length();
                }
            }
            value = value.or(LongVector.zero(LongVector.SPECIES_128)
                    .withLane(0, suffix.firstHalf)
                    .withLane(1, suffix.secondHalf)
                    .reinterpretAsBytes()
                    .rearrange(Helper.BYTE_SLICE[-off & (Helper.BYTE_SLICE.length - 1)]));
            var valueAsLongs = value.reinterpretAsLongs();
            return new InlineString(SMALL_STRING_VALUE, ilen, valueAsLongs.lane(0), valueAsLongs.lane(1));
        } else {
            byte[] value = StringConcatHelper.newArray(ilen, coder);

            int off = 0;
            prefix.getBytes(value, off, coder);
            off += prefix.length();
            if (size > 0) {
                var el = elements[0];
                el.getBytes(value, off, coder);
                off += el.length();
                for (int i = 1; i < size; i++) {
                    delimiter.getBytes(value, off, coder);
                    off += delimiter.length();
                    el = elements[i];
                    el.getBytes(value, off, coder);
                    off += el.length();
                }
            }
            suffix.getBytes(value, off, coder);
            return new InlineString(value, ilen, coder, 0);
        }
    }

    /**
     * Returns a new {@code String} composed of copies of the
     * {@code CharSequence elements} joined together with a copy of the
     * specified {@code delimiter}.
     *
     * <blockquote>For example,
     * <pre>{@code
     *     List<String> strings = List.of("Java", "is", "cool");
     *     String message = String.join(" ", strings);
     *     // message returned is: "Java is cool"
     *
     *     Set<String> strings =
     *         new LinkedHashSet<>(List.of("Java", "is", "very", "cool"));
     *     String message = String.join("-", strings);
     *     // message returned is: "Java-is-very-cool"
     * }</pre></blockquote>
     *
     * Note that if an individual element is {@code null}, then {@code "null"} is added.
     *
     * @param  delimiter a sequence of characters that is used to separate each
     *         of the {@code elements} in the resulting {@code String}
     * @param  elements an {@code Iterable} that will have its {@code elements}
     *         joined together.
     *
     * @return a new {@code String} that is composed from the {@code elements}
     *         argument
     *
     * @throws NullPointerException If {@code delimiter} or {@code elements}
     *         is {@code null}
     *
     * @see    #join(CharSequence,CharSequence...)
     * @see    java.util.StringJoiner
     * @since 1.8
     */
    public static InlineString join(CharSequence delimiter,
                                    Iterable<? extends CharSequence> elements) {
        Objects.requireNonNull(delimiter);
        Objects.requireNonNull(elements);
        var delim = new InlineString(delimiter.toString());
        var elems = new InlineString[8];
        int size = 0;
        for (CharSequence cs: elements) {
            if (size == elems.length) {
                var newElems = new InlineString[elems.length << 1];
                System.arraycopy(elems, 0, newElems, 0, size);
                elems = newElems;
            }
            elems[size++] = InlineString.valueOf(cs);
        }
        return join(EMPTY_STRING, EMPTY_STRING, delim, elems, size);
    }

    /**
     * Converts all of the characters in this {@code String} to lower
     * case using the rules of the given {@code Locale}.  Case mapping is based
     * on the Unicode Standard version specified by the {@link java.lang.Character Character}
     * class. Since case mappings are not always 1:1 char mappings, the resulting
     * {@code String} may be a different length than the original {@code String}.
     * <p>
     * Examples of lowercase  mappings are in the following table:
     * <table class="plain">
     * <caption style="display:none">Lowercase mapping examples showing language code of locale, upper case, lower case, and description</caption>
     * <thead>
     * <tr>
     *   <th scope="col">Language Code of Locale</th>
     *   <th scope="col">Upper Case</th>
     *   <th scope="col">Lower Case</th>
     *   <th scope="col">Description</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr>
     *   <td>tr (Turkish)</td>
     *   <th scope="row" style="font-weight:normal; text-align:left">&#92;u0130</th>
     *   <td>&#92;u0069</td>
     *   <td>capital letter I with dot above -&gt; small letter i</td>
     * </tr>
     * <tr>
     *   <td>tr (Turkish)</td>
     *   <th scope="row" style="font-weight:normal; text-align:left">&#92;u0049</th>
     *   <td>&#92;u0131</td>
     *   <td>capital letter I -&gt; small letter dotless i </td>
     * </tr>
     * <tr>
     *   <td>(all)</td>
     *   <th scope="row" style="font-weight:normal; text-align:left">French Fries</th>
     *   <td>french fries</td>
     *   <td>lowercased all chars in String</td>
     * </tr>
     * <tr>
     *   <td>(all)</td>
     *   <th scope="row" style="font-weight:normal; text-align:left">
     *       &Iota;&Chi;&Theta;&Upsilon;&Sigma;</th>
     *   <td>&iota;&chi;&theta;&upsilon;&sigma;</td>
     *   <td>lowercased all chars in String</td>
     * </tr>
     * </tbody>
     * </table>
     *
     * @param locale use the case transformation rules for this locale
     * @return the {@code String}, converted to lowercase.
     * @see     java.lang.String#toLowerCase()
     * @see     java.lang.String#toUpperCase()
     * @see     java.lang.String#toUpperCase(Locale)
     * @since   1.1
     */
    public InlineString toLowerCase(Locale locale) {
        return isLatin1() ? StringLatin1.toLowerCase(this, content(), locale)
                : StringUTF16.toLowerCase(this, content(), locale);
    }

    /**
     * Converts all of the characters in this {@code String} to lower
     * case using the rules of the default locale. This is equivalent to calling
     * {@code toLowerCase(Locale.getDefault())}.
     * <p>
     * <b>Note:</b> This method is locale sensitive, and may produce unexpected
     * results if used for strings that are intended to be interpreted locale
     * independently.
     * Examples are programming language identifiers, protocol keys, and HTML
     * tags.
     * For instance, {@code "TITLE".toLowerCase()} in a Turkish locale
     * returns {@code "t\u005Cu0131tle"}, where '\u005Cu0131' is the
     * LATIN SMALL LETTER DOTLESS I character.
     * To obtain correct results for locale insensitive strings, use
     * {@code toLowerCase(Locale.ROOT)}.
     *
     * @return  the {@code String}, converted to lowercase.
     * @see     java.lang.String#toLowerCase(Locale)
     */
    public InlineString toLowerCase() {
        return toLowerCase(Locale.getDefault());
    }

    /**
     * Converts all of the characters in this {@code String} to upper
     * case using the rules of the given {@code Locale}. Case mapping is based
     * on the Unicode Standard version specified by the {@link java.lang.Character Character}
     * class. Since case mappings are not always 1:1 char mappings, the resulting
     * {@code String} may be a different length than the original {@code String}.
     * <p>
     * Examples of locale-sensitive and 1:M case mappings are in the following table.
     *
     * <table class="plain">
     * <caption style="display:none">Examples of locale-sensitive and 1:M case mappings. Shows Language code of locale, lower case, upper case, and description.</caption>
     * <thead>
     * <tr>
     *   <th scope="col">Language Code of Locale</th>
     *   <th scope="col">Lower Case</th>
     *   <th scope="col">Upper Case</th>
     *   <th scope="col">Description</th>
     * </tr>
     * </thead>
     * <tbody>
     * <tr>
     *   <td>tr (Turkish)</td>
     *   <th scope="row" style="font-weight:normal; text-align:left">&#92;u0069</th>
     *   <td>&#92;u0130</td>
     *   <td>small letter i -&gt; capital letter I with dot above</td>
     * </tr>
     * <tr>
     *   <td>tr (Turkish)</td>
     *   <th scope="row" style="font-weight:normal; text-align:left">&#92;u0131</th>
     *   <td>&#92;u0049</td>
     *   <td>small letter dotless i -&gt; capital letter I</td>
     * </tr>
     * <tr>
     *   <td>(all)</td>
     *   <th scope="row" style="font-weight:normal; text-align:left">&#92;u00df</th>
     *   <td>&#92;u0053 &#92;u0053</td>
     *   <td>small letter sharp s -&gt; two letters: SS</td>
     * </tr>
     * <tr>
     *   <td>(all)</td>
     *   <th scope="row" style="font-weight:normal; text-align:left">Fahrvergn&uuml;gen</th>
     *   <td>FAHRVERGN&Uuml;GEN</td>
     *   <td></td>
     * </tr>
     * </tbody>
     * </table>
     * @param locale use the case transformation rules for this locale
     * @return the {@code String}, converted to uppercase.
     * @see     java.lang.String#toUpperCase()
     * @see     java.lang.String#toLowerCase()
     * @see     java.lang.String#toLowerCase(Locale)
     * @since   1.1
     */
    public InlineString toUpperCase(Locale locale) {
        return isLatin1() ? StringLatin1.toUpperCase(this, content(), locale)
                : StringUTF16.toUpperCase(this, content(), locale);
    }

    /**
     * Converts all of the characters in this {@code String} to upper
     * case using the rules of the default locale. This method is equivalent to
     * {@code toUpperCase(Locale.getDefault())}.
     * <p>
     * <b>Note:</b> This method is locale sensitive, and may produce unexpected
     * results if used for strings that are intended to be interpreted locale
     * independently.
     * Examples are programming language identifiers, protocol keys, and HTML
     * tags.
     * For instance, {@code "title".toUpperCase()} in a Turkish locale
     * returns {@code "T\u005Cu0130TLE"}, where '\u005Cu0130' is the
     * LATIN CAPITAL LETTER I WITH DOT ABOVE character.
     * To obtain correct results for locale insensitive strings, use
     * {@code toUpperCase(Locale.ROOT)}.
     *
     * @return  the {@code String}, converted to uppercase.
     * @see     java.lang.String#toUpperCase(Locale)
     */
    public InlineString toUpperCase() {
        return toUpperCase(Locale.getDefault());
    }

    /**
     * Returns a string whose value is this string, with all leading
     * and trailing space removed, where space is defined
     * as any character whose codepoint is less than or equal to
     * {@code 'U+0020'} (the space character).
     * <p>
     * If this {@code String} object represents an empty character
     * sequence, or the first and last characters of character sequence
     * represented by this {@code String} object both have codes
     * that are not space (as defined above), then a
     * reference to this {@code String} object is returned.
     * <p>
     * Otherwise, if all characters in this string are space (as
     * defined above), then a  {@code String} object representing an
     * empty string is returned.
     * <p>
     * Otherwise, let <i>k</i> be the index of the first character in the
     * string whose code is not a space (as defined above) and let
     * <i>m</i> be the index of the last character in the string whose code
     * is not a space (as defined above). A {@code String}
     * object is returned, representing the substring of this string that
     * begins with the character at index <i>k</i> and ends with the
     * character at index <i>m</i>-that is, the result of
     * {@code this.substring(k, m + 1)}.
     * <p>
     * This method may be used to trim space (as defined above) from
     * the beginning and end of a string.
     *
     * @return  a string whose value is this string, with all leading
     *          and trailing space removed, or this string if it
     *          has no leading or trailing space.
     */
    public InlineString trim() {
        if (isCompressed()) {
            return StringCompressed.trim(firstHalf, secondHalf);
        } else if (isLatin1()) {
            return StringLatin1.trim(value);
        } else {
            return StringUTF16.trim(value);
        }
    }

    /**
     * Returns a string whose value is this string, with all leading
     * and trailing {@linkplain Character#isWhitespace(int) white space}
     * removed.
     * <p>
     * If this {@code String} object represents an empty string,
     * or if all code points in this string are
     * {@linkplain Character#isWhitespace(int) white space}, then an empty string
     * is returned.
     * <p>
     * Otherwise, returns a substring of this string beginning with the first
     * code point that is not a {@linkplain Character#isWhitespace(int) white space}
     * up to and including the last code point that is not a
     * {@linkplain Character#isWhitespace(int) white space}.
     * <p>
     * This method may be used to strip
     * {@linkplain Character#isWhitespace(int) white space} from
     * the beginning and end of a string.
     *
     * @return  a string whose value is this string, with all leading
     *          and trailing white space removed
     *
     * @see Character#isWhitespace(int)
     *
     * @since 11
     */
    public InlineString strip() {
        if (isCompressed()) {
            return StringCompressed.strip(firstHalf, secondHalf, length());
        } else if (isLatin1()) {
            return StringLatin1.strip(value);
        } else {
            return StringUTF16.strip(value);
        }
    }

    /**
     * Returns a string whose value is this string, with all leading
     * {@linkplain Character#isWhitespace(int) white space} removed.
     * <p>
     * If this {@code String} object represents an empty string,
     * or if all code points in this string are
     * {@linkplain Character#isWhitespace(int) white space}, then an empty string
     * is returned.
     * <p>
     * Otherwise, returns a substring of this string beginning with the first
     * code point that is not a {@linkplain Character#isWhitespace(int) white space}
     * up to and including the last code point of this string.
     * <p>
     * This method may be used to trim
     * {@linkplain Character#isWhitespace(int) white space} from
     * the beginning of a string.
     *
     * @return  a string whose value is this string, with all leading white
     *          space removed
     *
     * @see Character#isWhitespace(int)
     *
     * @since 11
     */
    public InlineString stripLeading() {
        if (isCompressed()) {
            return StringCompressed.stripLeading(firstHalf, secondHalf, length());
        } else if (isLatin1()) {
            return StringLatin1.stripLeading(value);
        } else {
            return StringUTF16.stripLeading(value);
        }
    }

    /**
     * Returns a string whose value is this string, with all trailing
     * {@linkplain Character#isWhitespace(int) white space} removed.
     * <p>
     * If this {@code String} object represents an empty string,
     * or if all characters in this string are
     * {@linkplain Character#isWhitespace(int) white space}, then an empty string
     * is returned.
     * <p>
     * Otherwise, returns a substring of this string beginning with the first
     * code point of this string up to and including the last code point
     * that is not a {@linkplain Character#isWhitespace(int) white space}.
     * <p>
     * This method may be used to trim
     * {@linkplain Character#isWhitespace(int) white space} from
     * the end of a string.
     *
     * @return  a string whose value is this string, with all trailing white
     *          space removed
     *
     * @see Character#isWhitespace(int)
     *
     * @since 11
     */
    public InlineString stripTrailing() {
        if (isCompressed()) {
            return StringCompressed.stripTrailing(firstHalf, secondHalf, length());
        } else if (isLatin1()) {
            return StringLatin1.stripTrailing(value);
        } else {
            return StringUTF16.stripTrailing(value);
        }
    }

    /**
     * Returns {@code true} if the string is empty or contains only
     * {@linkplain Character#isWhitespace(int) white space} codepoints,
     * otherwise {@code false}.
     *
     * @return {@code true} if the string is empty or contains only
     *         {@linkplain Character#isWhitespace(int) white space} codepoints,
     *         otherwise {@code false}
     *
     * @see Character#isWhitespace(int)
     *
     * @since 11
     */
    public boolean isBlank() {
        return indexOfNonWhitespace() == length();
    }

    /**
     * Returns a stream of lines extracted from this string,
     * separated by line terminators.
     * <p>
     * A <i>line terminator</i> is one of the following:
     * a line feed character {@code "\n"} (U+000A),
     * a carriage return character {@code "\r"} (U+000D),
     * or a carriage return followed immediately by a line feed
     * {@code "\r\n"} (U+000D U+000A).
     * <p>
     * A <i>line</i> is either a sequence of zero or more characters
     * followed by a line terminator, or it is a sequence of one or
     * more characters followed by the end of the string. A
     * line does not include the line terminator.
     * <p>
     * The stream returned by this method contains the lines from
     * this string in the order in which they occur.
     *
     * @apiNote This definition of <i>line</i> implies that an empty
     *          string has zero lines and that there is no empty line
     *          following a line terminator at the end of a string.
     *
     * @implNote This method provides better performance than
     *           split("\R") by supplying elements lazily and
     *           by faster search of new line terminators.
     *
     * @return  the stream of lines extracted from this string
     *
     * @since 11
     */
    public Stream<InlineString.ref> lines() {
        if (isCompressed()) {
            return StringCompressed.lines(firstHalf, secondHalf, length());
        } else if (isLatin1()) {
            return StringLatin1.lines(value);
        } else {
            return StringUTF16.lines(value);
        }
    }

    /**
     * Adjusts the indentation of each line of this string based on the value of
     * {@code n}, and normalizes line termination characters.
     * <p>
     * This string is conceptually separated into lines using
     * {@link String#lines()}. Each line is then adjusted as described below
     * and then suffixed with a line feed {@code "\n"} (U+000A). The resulting
     * lines are then concatenated and returned.
     * <p>
     * If {@code n > 0} then {@code n} spaces (U+0020) are inserted at the
     * beginning of each line.
     * <p>
     * If {@code n < 0} then up to {@code n}
     * {@linkplain Character#isWhitespace(int) white space characters} are removed
     * from the beginning of each line. If a given line does not contain
     * sufficient white space then all leading
     * {@linkplain Character#isWhitespace(int) white space characters} are removed.
     * Each white space character is treated as a single character. In
     * particular, the tab character {@code "\t"} (U+0009) is considered a
     * single character; it is not expanded.
     * <p>
     * If {@code n == 0} then the line remains unchanged. However, line
     * terminators are still normalized.
     *
     * @param n  number of leading
     *           {@linkplain Character#isWhitespace(int) white space characters}
     *           to add or remove
     *
     * @return string with indentation adjusted and line endings normalized
     *
     * @see String#lines()
     * @see String#isBlank()
     * @see Character#isWhitespace(int)
     *
     * @since 12
     */
    public InlineString indent(int n) {
        if (isEmpty()) {
            return EMPTY_STRING;
        }
        Stream<InlineString.ref> stream = lines();
        if (n > 0) {
            final var spaces = valueOf(' ').repeat(n);
            stream = stream.map(s -> spaces.concat(s));
        } else if (n == Integer.MIN_VALUE) {
            stream = stream.map(s -> s.stripLeading());
        } else if (n < 0) {
            stream = stream.map(s -> s.substring(Math.min(-n, s.indexOfNonWhitespace())));
        }
        return stream.collect(Collector.of(StringBuilder::new,
                (sb, ele) -> sb.append(ele.toString()).append('\n'),
                StringBuilder::append,
                InlineString::new,
                Collector.Characteristics.CONCURRENT));
    }

    /**
     * Returns a string whose value is this string, with incidental
     * {@linkplain Character#isWhitespace(int) white space} removed from
     * the beginning and end of every line.
     * <p>
     * Incidental {@linkplain Character#isWhitespace(int) white space}
     * is often present in a text block to align the content with the opening
     * delimiter. For example, in the following code, dots represent incidental
     * {@linkplain Character#isWhitespace(int) white space}:
     * <blockquote><pre>
     * String html = """
     * ..............&lt;html&gt;
     * ..............    &lt;body&gt;
     * ..............        &lt;p&gt;Hello, world&lt;/p&gt;
     * ..............    &lt;/body&gt;
     * ..............&lt;/html&gt;
     * ..............""";
     * </pre></blockquote>
     * This method treats the incidental
     * {@linkplain Character#isWhitespace(int) white space} as indentation to be
     * stripped, producing a string that preserves the relative indentation of
     * the content. Using | to visualize the start of each line of the string:
     * <blockquote><pre>
     * |&lt;html&gt;
     * |    &lt;body&gt;
     * |        &lt;p&gt;Hello, world&lt;/p&gt;
     * |    &lt;/body&gt;
     * |&lt;/html&gt;
     * </pre></blockquote>
     * First, the individual lines of this string are extracted. A <i>line</i>
     * is a sequence of zero or more characters followed by either a line
     * terminator or the end of the string.
     * If the string has at least one line terminator, the last line consists
     * of the characters between the last terminator and the end of the string.
     * Otherwise, if the string has no terminators, the last line is the start
     * of the string to the end of the string, in other words, the entire
     * string.
     * A line does not include the line terminator.
     * <p>
     * Then, the <i>minimum indentation</i> (min) is determined as follows:
     * <ul>
     *   <li><p>For each non-blank line (as defined by {@link String#isBlank()}),
     *   the leading {@linkplain Character#isWhitespace(int) white space}
     *   characters are counted.</p>
     *   </li>
     *   <li><p>The leading {@linkplain Character#isWhitespace(int) white space}
     *   characters on the last line are also counted even if
     *   {@linkplain String#isBlank() blank}.</p>
     *   </li>
     * </ul>
     * <p>The <i>min</i> value is the smallest of these counts.
     * <p>
     * For each {@linkplain String#isBlank() non-blank} line, <i>min</i> leading
     * {@linkplain Character#isWhitespace(int) white space} characters are
     * removed, and any trailing {@linkplain Character#isWhitespace(int) white
     * space} characters are removed. {@linkplain String#isBlank() Blank} lines
     * are replaced with the empty string.
     *
     * <p>
     * Finally, the lines are joined into a new string, using the LF character
     * {@code "\n"} (U+000A) to separate lines.
     *
     * @apiNote
     * This method's primary purpose is to shift a block of lines as far as
     * possible to the left, while preserving relative indentation. Lines
     * that were indented the least will thus have no leading
     * {@linkplain Character#isWhitespace(int) white space}.
     * The result will have the same number of line terminators as this string.
     * If this string ends with a line terminator then the result will end
     * with a line terminator.
     *
     * @implSpec
     * This method treats all {@linkplain Character#isWhitespace(int) white space}
     * characters as having equal width. As long as the indentation on every
     * line is consistently composed of the same character sequences, then the
     * result will be as described above.
     *
     * @return string with incidental indentation removed and line
     *         terminators normalized
     *
     * @see String#lines()
     * @see String#isBlank()
     * @see String#indent(int)
     * @see Character#isWhitespace(int)
     *
     * @since 15
     *
     */
    public InlineString stripIndent() {
        int length = length();
        if (length == 0) {
            return EMPTY_STRING;
        }
        char lastChar = charAt(length - 1);
        boolean optOut = lastChar == '\n' || lastChar == '\r';
        var lines = lines().toList();
        final int outdent = optOut ? 0 : outdent(lines);
        return lines.stream().map(line -> {
                    int firstNonWhitespace = line.indexOfNonWhitespace();
                    int lastNonWhitespace = line.lastIndexOfNonWhitespace();
                    int incidentalWhitespace = Math.min(outdent, firstNonWhitespace);
                    return firstNonWhitespace > lastNonWhitespace
                            ? "" : line.substring(incidentalWhitespace, lastNonWhitespace);
                })
                .collect(Collector.of(StringBuilder::new,
                        (sb, ele) -> sb.append(ele.toString()).append('\n'),
                        StringBuilder::append,
                        sb -> new InlineString((optOut ? sb : sb.deleteCharAt(sb.length() - 1))),
                        Collector.Characteristics.CONCURRENT));
    }

    private static int outdent(List<InlineString.ref> lines) {
        // Note: outdent is guaranteed to be zero or positive number.
        // If there isn't a non-blank line then the last must be blank
        int outdent = Integer.MAX_VALUE;
        for (var line : lines) {
            int leadingWhitespace = line.indexOfNonWhitespace();
            if (leadingWhitespace != line.length()) {
                outdent = Integer.min(outdent, leadingWhitespace);
            }
        }
        var lastLine = lines.get(lines.size() - 1);
        if (lastLine.isBlank()) {
            outdent = Integer.min(outdent, lastLine.length());
        }
        return outdent;
    }

    /**
     * Returns a string whose value is this string, with escape sequences
     * translated as if in a string literal.
     * <p>
     * Escape sequences are translated as follows;
     * <table class="striped">
     *   <caption style="display:none">Translation</caption>
     *   <thead>
     *   <tr>
     *     <th scope="col">Escape</th>
     *     <th scope="col">Name</th>
     *     <th scope="col">Translation</th>
     *   </tr>
     *   </thead>
     *   <tbody>
     *   <tr>
     *     <th scope="row">{@code \u005Cb}</th>
     *     <td>backspace</td>
     *     <td>{@code U+0008}</td>
     *   </tr>
     *   <tr>
     *     <th scope="row">{@code \u005Ct}</th>
     *     <td>horizontal tab</td>
     *     <td>{@code U+0009}</td>
     *   </tr>
     *   <tr>
     *     <th scope="row">{@code \u005Cn}</th>
     *     <td>line feed</td>
     *     <td>{@code U+000A}</td>
     *   </tr>
     *   <tr>
     *     <th scope="row">{@code \u005Cf}</th>
     *     <td>form feed</td>
     *     <td>{@code U+000C}</td>
     *   </tr>
     *   <tr>
     *     <th scope="row">{@code \u005Cr}</th>
     *     <td>carriage return</td>
     *     <td>{@code U+000D}</td>
     *   </tr>
     *   <tr>
     *     <th scope="row">{@code \u005Cs}</th>
     *     <td>space</td>
     *     <td>{@code U+0020}</td>
     *   </tr>
     *   <tr>
     *     <th scope="row">{@code \u005C"}</th>
     *     <td>double quote</td>
     *     <td>{@code U+0022}</td>
     *   </tr>
     *   <tr>
     *     <th scope="row">{@code \u005C'}</th>
     *     <td>single quote</td>
     *     <td>{@code U+0027}</td>
     *   </tr>
     *   <tr>
     *     <th scope="row">{@code \u005C\u005C}</th>
     *     <td>backslash</td>
     *     <td>{@code U+005C}</td>
     *   </tr>
     *   <tr>
     *     <th scope="row">{@code \u005C0 - \u005C377}</th>
     *     <td>octal escape</td>
     *     <td>code point equivalents</td>
     *   </tr>
     *   <tr>
     *     <th scope="row">{@code \u005C<line-terminator>}</th>
     *     <td>continuation</td>
     *     <td>discard</td>
     *   </tr>
     *   </tbody>
     * </table>
     *
     * @implNote
     * This method does <em>not</em> translate Unicode escapes such as "{@code \u005cu2022}".
     * Unicode escapes are translated by the Java compiler when reading input characters and
     * are not part of the string literal specification.
     *
     * @throws IllegalArgumentException when an escape sequence is malformed.
     *
     * @return String with escape sequences translated.
     *
     * @jls 3.10.7 Escape Sequences
     *
     * @since 15
     */
    public InlineString translateEscapes() {
        if (isEmpty()) {
            return EMPTY_STRING;
        }
        char[] chars = toCharArray();
        int length = chars.length;
        int from = 0;
        int to = 0;
        while (from < length) {
            char ch = chars[from++];
            if (ch == '\\') {
                ch = from < length ? chars[from++] : '\0';
                switch (ch) {
                    case 'b':
                        ch = '\b';
                        break;
                    case 'f':
                        ch = '\f';
                        break;
                    case 'n':
                        ch = '\n';
                        break;
                    case 'r':
                        ch = '\r';
                        break;
                    case 's':
                        ch = ' ';
                        break;
                    case 't':
                        ch = '\t';
                        break;
                    case '\'':
                    case '\"':
                    case '\\':
                        // as is
                        break;
                    case '0': case '1': case '2': case '3':
                    case '4': case '5': case '6': case '7':
                        int limit = Integer.min(from + (ch <= '3' ? 2 : 1), length);
                        int code = ch - '0';
                        while (from < limit) {
                            ch = chars[from];
                            if (ch < '0' || '7' < ch) {
                                break;
                            }
                            from++;
                            code = (code << 3) | (ch - '0');
                        }
                        ch = (char)code;
                        break;
                    case '\n':
                        continue;
                    case '\r':
                        if (from < length && chars[from] == '\n') {
                            from++;
                        }
                        continue;
                    default: {
                        String msg = String.format(
                                "Invalid escape sequence: \\%c \\\\u%04X",
                                ch, (int)ch);
                        throw new IllegalArgumentException(msg);
                    }
                }
            }

            chars[to++] = ch;
        }

        return new InlineString(chars, 0, to);
    }

    /**
     * This method allows the application of a function to {@code this}
     * string. The function should expect a single String argument
     * and produce an {@code R} result.
     * <p>
     * Any exception thrown by {@code f.apply()} will be propagated to the
     * caller.
     *
     * @param f    a function to apply
     *
     * @param <R>  the type of the result
     *
     * @return     the result of applying the function to this string
     *
     * @see java.util.function.Function
     *
     * @since 12
     */
    public <R> R transform(Function<? super InlineString.ref, ? extends R> f) {
        return f.apply(this);
    }

    /**
     * Return a {@code String} object similar to this object.
     *
     * @return  the equivalent {@code String} object.
     */
    public String toString() {
        assert isValid();
        return Utils.newStringValueCoder(content(), coder());
    }

    /**
     * Returns a stream of {@code int} zero-extending the {@code char} values
     * from this sequence.  Any char which maps to a <a
     * href="{@docRoot}/java.base/java/lang/Character.html#unicode">surrogate code
     * point</a> is passed through uninterpreted.
     *
     * @return an IntStream of char values from this sequence
     * @since 9
     */
    @Override
    public IntStream chars() {
        Spliterator.OfInt charsSpliterator;
        if (isCompressed()) {
            charsSpliterator = StringCompressed.charSpliterator(firstHalf, secondHalf, length());
        } else if (isLatin1()) {
            charsSpliterator = StringLatin1.charsSpliterator(value, Spliterator.IMMUTABLE);
        } else {
            charsSpliterator = StringUTF16.charsSpliterator(value, Spliterator.IMMUTABLE);
        }
        return StreamSupport.intStream(charsSpliterator, false);
    }


    /**
     * Returns a stream of code point values from this sequence.  Any surrogate
     * pairs encountered in the sequence are combined as if by {@linkplain
     * Character#toCodePoint Character.toCodePoint} and the result is passed
     * to the stream. Any other code units, including ordinary BMP characters,
     * unpaired surrogates, and undefined code units, are zero-extended to
     * {@code int} values which are then passed to the stream.
     *
     * @return an IntStream of Unicode code points from this sequence
     * @since 9
     */
    @Override
    public IntStream codePoints() {
        Spliterator.OfInt charsSpliterator;
        if (isCompressed()) {
            charsSpliterator = StringCompressed.charSpliterator(firstHalf, secondHalf, length());
        } else if (isLatin1()) {
            charsSpliterator = StringLatin1.charsSpliterator(value, Spliterator.IMMUTABLE);
        } else {
            charsSpliterator = StringUTF16.codePointsSpliterator(value, Spliterator.IMMUTABLE);
        }
        return StreamSupport.intStream(charsSpliterator, false);
    }

    /**
     * Converts this string to a new character array.
     *
     * @return  a newly allocated character array whose length is the length
     *          of this string and whose contents are initialized to contain
     *          the character sequence represented by this string.
     */
    public char[] toCharArray() {
        if (isCompressed()) {
            char[] res = new char[length()];
            StringCompressed.getChars(firstHalf, secondHalf, 0, length(), res, 0);
            return res;
        } else if (isLatin1()) {
            return StringLatin1.toChars(value);
        } else {
            return StringUTF16.toChars(value);
        }
    }

    /**
     * Returns a formatted string using the specified format string and
     * arguments.
     *
     * <p> The locale always used is the one returned by {@link
     * java.util.Locale#getDefault(java.util.Locale.Category)
     * Locale.getDefault(Locale.Category)} with
     * {@link java.util.Locale.Category#FORMAT FORMAT} category specified.
     *
     * @param  format
     *         A <a href="../util/Formatter.html#syntax">format string</a>
     *
     * @param  args
     *         Arguments referenced by the format specifiers in the format
     *         string.  If there are more arguments than format specifiers, the
     *         extra arguments are ignored.  The number of arguments is
     *         variable and may be zero.  The maximum number of arguments is
     *         limited by the maximum dimension of a Java array as defined by
     *         <cite>The Java Virtual Machine Specification</cite>.
     *         The behaviour on a
     *         {@code null} argument depends on the <a
     *         href="../util/Formatter.html#syntax">conversion</a>.
     *
     * @throws  java.util.IllegalFormatException
     *          If a format string contains an illegal syntax, a format
     *          specifier that is incompatible with the given arguments,
     *          insufficient arguments given the format string, or other
     *          illegal conditions.  For specification of all possible
     *          formatting errors, see the <a
     *          href="../util/Formatter.html#detail">Details</a> section of the
     *          formatter class specification.
     *
     * @return  A formatted string
     *
     * @see  java.util.Formatter
     * @since  1.5
     */
    public static InlineString format(InlineString format, Object... args) {
        return new InlineString(new Formatter().format(format.toString(), args).toString());
    }

    /**
     * Returns a formatted string using the specified locale, format string,
     * and arguments.
     *
     * @param  l
     *         The {@linkplain java.util.Locale locale} to apply during
     *         formatting.  If {@code l} is {@code null} then no localization
     *         is applied.
     *
     * @param  format
     *         A <a href="../util/Formatter.html#syntax">format string</a>
     *
     * @param  args
     *         Arguments referenced by the format specifiers in the format
     *         string.  If there are more arguments than format specifiers, the
     *         extra arguments are ignored.  The number of arguments is
     *         variable and may be zero.  The maximum number of arguments is
     *         limited by the maximum dimension of a Java array as defined by
     *         <cite>The Java Virtual Machine Specification</cite>.
     *         The behaviour on a
     *         {@code null} argument depends on the
     *         <a href="../util/Formatter.html#syntax">conversion</a>.
     *
     * @throws  java.util.IllegalFormatException
     *          If a format string contains an illegal syntax, a format
     *          specifier that is incompatible with the given arguments,
     *          insufficient arguments given the format string, or other
     *          illegal conditions.  For specification of all possible
     *          formatting errors, see the <a
     *          href="../util/Formatter.html#detail">Details</a> section of the
     *          formatter class specification
     *
     * @return  A formatted string
     *
     * @see  java.util.Formatter
     * @since  1.5
     */
    public static InlineString format(Locale l, InlineString format, Object... args) {
        return new InlineString(new Formatter(l).format(format.toString(), args).toString());
    }

    /**
     * Formats using this string as the format string, and the supplied
     * arguments.
     *
     * @implSpec This method is equivalent to {@code String.format(this, args)}.
     *
     * @param  args
     *         Arguments referenced by the format specifiers in this string.
     *
     * @return  A formatted string
     *
     * @see  java.lang.String#format(String,Object...)
     * @see  java.util.Formatter
     *
     * @since 15
     *
     */
    public InlineString formatted(Object... args) {
        return new InlineString(new Formatter().format(this.toString(), args).toString());
    }

    /**
     * Returns the string representation of the {@code Object} argument.
     *
     * @param   obj   an {@code Object}.
     * @return  if the argument is {@code null}, then a string equal to
     *          {@code "null"}; otherwise, the value of
     *          {@code obj.toString()} is returned.
     * @see     java.lang.Object#toString()
     */
    public static InlineString valueOf(Object obj) {
        return (obj == null) ? new InlineString("null") : new InlineString(obj.toString());
    }

    /**
     * Returns the string representation of the {@code char} array
     * argument. The contents of the character array are copied; subsequent
     * modification of the character array does not affect the returned
     * string.
     *
     * @param   data     the character array.
     * @return  a {@code String} that contains the characters of the
     *          character array.
     */
    public static InlineString valueOf(char[] data) {
        return new InlineString(data);
    }

    /**
     * Returns the string representation of a specific subarray of the
     * {@code char} array argument.
     * <p>
     * The {@code offset} argument is the index of the first
     * character of the subarray. The {@code count} argument
     * specifies the length of the subarray. The contents of the subarray
     * are copied; subsequent modification of the character array does not
     * affect the returned string.
     *
     * @param   data     the character array.
     * @param   offset   initial offset of the subarray.
     * @param   count    length of the subarray.
     * @return  a {@code String} that contains the characters of the
     *          specified subarray of the character array.
     * @throws    IndexOutOfBoundsException if {@code offset} is
     *          negative, or {@code count} is negative, or
     *          {@code offset+count} is larger than
     *          {@code data.length}.
     */
    public static InlineString valueOf(char[] data, int offset, int count) {
        return new InlineString(data, offset, count);
    }

    /**
     * Equivalent to {@link #valueOf(char[], int, int)}.
     *
     * @param   data     the character array.
     * @param   offset   initial offset of the subarray.
     * @param   count    length of the subarray.
     * @return  a {@code String} that contains the characters of the
     *          specified subarray of the character array.
     * @throws    IndexOutOfBoundsException if {@code offset} is
     *          negative, or {@code count} is negative, or
     *          {@code offset+count} is larger than
     *          {@code data.length}.
     */
    public static InlineString copyValueOf(char[] data, int offset, int count) {
        return new InlineString(data, offset, count);
    }

    /**
     * Equivalent to {@link #valueOf(char[])}.
     *
     * @param   data   the character array.
     * @return  a {@code String} that contains the characters of the
     *          character array.
     */
    public static InlineString copyValueOf(char[] data) {
        return new InlineString(data);
    }

    /**
     * Returns the string representation of the {@code boolean} argument.
     *
     * @param   b   a {@code boolean}.
     * @return  if the argument is {@code true}, a string equal to
     *          {@code "true"} is returned; otherwise, a string equal to
     *          {@code "false"} is returned.
     */
    public static InlineString valueOf(boolean b) {
        return b ? new InlineString("true") : new InlineString("false");
    }

    /**
     * Returns the string representation of the {@code char}
     * argument.
     *
     * @param   c   a {@code char}.
     * @return  a string of length {@code 1} containing
     *          as its single character the argument {@code c}.
     */
    public static InlineString valueOf(char c) {
        if (Utils.COMPACT_STRINGS && StringLatin1.canEncode(c)) {
            return new InlineString(SMALL_STRING_VALUE, 1, c, 0);
        } else {
            return new InlineString(StringUTF16.toBytes(c), 1, Utils.UTF16, 0);
        }
    }

    /**
     * Returns the string representation of the {@code int} argument.
     * <p>
     * The representation is exactly the one returned by the
     * {@code Integer.toString} method of one argument.
     *
     * @param   i   an {@code int}.
     * @return  a string representation of the {@code int} argument.
     * @see     java.lang.Integer#toString(int, int)
     */
    public static InlineString valueOf(int i) {
        return new InlineString(Integer.toString(i));
    }

    /**
     * Returns the string representation of the {@code long} argument.
     * <p>
     * The representation is exactly the one returned by the
     * {@code Long.toString} method of one argument.
     *
     * @param   l   a {@code long}.
     * @return  a string representation of the {@code long} argument.
     * @see     java.lang.Long#toString(long)
     */
    public static InlineString valueOf(long l) {
        return new InlineString(Long.toString(l));
    }

    /**
     * Returns the string representation of the {@code float} argument.
     * <p>
     * The representation is exactly the one returned by the
     * {@code Float.toString} method of one argument.
     *
     * @param   f   a {@code float}.
     * @return  a string representation of the {@code float} argument.
     * @see     java.lang.Float#toString(float)
     */
    public static InlineString valueOf(float f) {
        return new InlineString(Float.toString(f));
    }

    /**
     * Returns the string representation of the {@code double} argument.
     * <p>
     * The representation is exactly the one returned by the
     * {@code Double.toString} method of one argument.
     *
     * @param   d   a {@code double}.
     * @return  a  string representation of the {@code double} argument.
     * @see     java.lang.Double#toString(double)
     */
    public static InlineString valueOf(double d) {
        return new InlineString(Double.toString(d));
    }

    /**
     * Returns a string whose value is the concatenation of this
     * string repeated {@code count} times.
     * <p>
     * If this string is empty or count is zero then the empty
     * string is returned.
     *
     * @param   count number of times to repeat
     *
     * @return  A string composed of this string repeated
     *          {@code count} times or the empty string if this
     *          string is empty or count is zero
     *
     * @throws  IllegalArgumentException if the {@code count} is
     *          negative.
     *
     * @since 11
     */
    public InlineString repeat(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count is negative: " + count);
        }
        if (count == 1) {
            return this;
        }
        if (isEmpty() || count == 0) {
            return EMPTY_STRING;
        }
        final int dataLen = isCompressed() ? length : value.length;
        if (Integer.MAX_VALUE / count < dataLen) {
            throw new OutOfMemoryError("Required index exceeds implementation limit");
        }
        int limit = dataLen * count;
        byte[] multiple;
        if (isCompressed()) {
            if (StringCompressed.compressible(limit)) {
                // since count > 1 and limit <= 16, length <= 8
                long firstHalf = 0;
                int i = 0;
                for (long temp = this.firstHalf; i < Math.min(limit, Long.BYTES);
                        i += length(), temp >>>= ((long)length() * Byte.SIZE)) {
                    firstHalf |= temp;
                }
                // if i == 8 the first value need to be zero
                long secondHalf = (this.firstHalf << 1) << ((Long.BYTES - i) * Byte.SIZE - 1);
                for (long temp = this.firstHalf >>> ((i - Long.BYTES) * Byte.SIZE); i < limit;
                        i += length(), temp >>>= ((long)length() * Byte.SIZE)) {
                    secondHalf |= temp;
                }
                return new InlineString(SMALL_STRING_VALUE, limit, firstHalf, secondHalf);
            } else {
                multiple = StringConcatHelper.newArray(limit, Utils.LATIN1);
                StringCompressed.decompress(firstHalf, secondHalf, multiple, 0, Long.BYTES);
            }
        } else {
            multiple = StringConcatHelper.newArray(limit, Utils.LATIN1);
            System.arraycopy(value, 0, multiple, 0, dataLen);
        }
        int copied = dataLen;
        for (; copied < limit - copied; copied <<= 1) {
            System.arraycopy(multiple, 0, multiple, copied, copied);
        }
        System.arraycopy(multiple, 0, multiple, copied, limit - copied);
        long coder = isCompressed() ? Utils.LATIN1 : firstHalf;
        return new InlineString(multiple, limit >>> coder, coder, 0);
    }

    ////////////////////////////////////////////////////////////////

    /**
     * Copy character bytes from this string into dst starting at dstBegin.
     * This method doesn't perform any range checking.
     *
     * Invoker guarantees: dst is in UTF16 (inflate itself for asb), if two
     * coders are different, and dst is big enough (range check)
     *
     * @param dstBegin  the char index, not offset of byte[]
     * @param coder     the coder of dst[]
     */
    void getBytes(byte[] dst, int dstBegin, byte coder) {
        if (coder() == coder) {
            if (isCompressed()) { // must be LATIN1
                StringCompressed.decompress(this.firstHalf, this.secondHalf, dst, dstBegin, this.length());
            } else {
                System.arraycopy(value, 0, dst, dstBegin << coder, value.length);
            }
        } else {    // this.coder == LATIN && coder == String.UTF16
            if (isCompressed()) {
                StringCompressed.inflate(firstHalf, secondHalf, dst, dstBegin, length());
            } else {
                StringLatin1.inflate(value, 0, dst, dstBegin, value.length);
            }
        }
    }

    /*
     * Package private constructor. Trailing Void argument is there for
     * disambiguating it against other (public) constructors.
     *
     * Stores the char[] index into a byte[] that each byte represents
     * the 8 low-order bits of the corresponding character, if the char[]
     * contains only latin1 character. Or a byte[] that stores all
     * characters in their byte sequences defined by the {@code StringUTF16}.
     */
    private InlineString(char[] value, int off, int len, Void sig) {
        if (len == 0) {
            this.value = EMPTY_STRING.value;
            this.length = EMPTY_STRING.length;
            this.firstHalf = EMPTY_STRING.firstHalf;
            this.secondHalf = EMPTY_STRING.secondHalf;
            return;
        }
        if (Utils.COMPACT_STRINGS) {
            if (StringCompressed.compressible(len)) {
                var temp = StringCompressed.compress(value, off, len);
                var inflatedFirst = LongVector.zero(LongVector.SPECIES_128)
                        .withLane(0, temp.firstQuarter())
                        .withLane(1, temp.secondQuarter())
                        .reinterpretAsShorts();
                if (inflatedFirst.compare(VectorOperators.UNSIGNED_LT, (short)0x100).allTrue()) {
                    var inflatedSecond = LongVector.zero(LongVector.SPECIES_128)
                            .withLane(0, temp.thirdQuarter())
                            .withLane(1, temp.fourthQuarter())
                            .reinterpretAsShorts();
                    if (inflatedSecond.compare(VectorOperators.UNSIGNED_LT, (short)0x100).allTrue()) {
                        this.value = SMALL_STRING_VALUE;
                        this.length = len;
                        this.firstHalf = inflatedFirst.castShape(ByteVector.SPECIES_128, 0)
                                .reinterpretAsLongs()
                                .lane(0);
                        this.secondHalf = inflatedSecond.castShape(ByteVector.SPECIES_128, 0)
                                .reinterpretAsLongs()
                                .lane(0);
                        return;
                    }
                }
            } else {
                byte[] val = StringUTF16.compress(value, off, len);
                if (val != null) {
                    this.value = val;
                    this.length = len;
                    this.firstHalf = Utils.LATIN1;
                    this.secondHalf = 0;
                    return;
                }
            }
        }
        this.value = StringUTF16.toBytes(value, off, len);
        this.length = len;
        this.firstHalf = Utils.UTF16;
        this.secondHalf = 0;
    }

    /*
     * Package private constructor which shares index array for speed.
     */
    InlineString(byte[] value, byte coder) {
        if (StringCompressed.compressible(value.length, coder)) {
            var compressedValue = StringCompressed.compress(value);
            this.value = SMALL_STRING_VALUE;
            this.length = value.length;
            this.firstHalf = compressedValue.firstHalf();
            this.secondHalf = compressedValue.secondHalf();
        } else {
            this.value = value;
            this.length = value.length >>> coder;
            this.firstHalf = coder;
            this.secondHalf = 0;
        }
    }

    InlineString(byte[] value, int length, long firstHalf, long secondHalf) {
        this.value = value;
        this.length = length;
        this.firstHalf = firstHalf;
        this.secondHalf = secondHalf;
    }

    /**
     * Return an array contains the content of the string, may need to allocate for compressed ones
     *
     * @return an array contains the content of the string
     */
    byte[] content() {
        if (isCompressed()) {
            return StringCompressed.decompress(firstHalf, secondHalf, length());
        } else {
            return value;
        }
    }

    byte coder() {
        if (Utils.COMPACT_STRINGS) {
            if (isCompressed()) {
                return Utils.LATIN1;
            } else {
                return (byte) firstHalf;
            }
        } else {
            return Utils.UTF16;
        }
    }

    /**
     * Return the raw value (maybe equals to a dummy value with compressed strings)
     *
     * @return the raw value field
     */
    byte[] value() {
        return this.value;
    }

    long firstHalf() {
        return this.firstHalf;
    }

    long secondHalf() {
        return this.secondHalf;
    }

    boolean isLatin1() {
        return Utils.COMPACT_STRINGS && (isCompressed() || firstHalf == Utils.LATIN1);
    }

    boolean isCompressed() {
        return Utils.COMPRESSED_STRINGS && value == SMALL_STRING_VALUE;
    }

    private boolean isValid() {
        if (isCompressed()) {
            boolean first = length() <= StringCompressed.COMPRESS_THRESHOLD && length() >= 0;
            boolean second = LongVector.zero(LongVector.SPECIES_128)
                    .withLane(0, firstHalf)
                    .withLane(1, secondHalf)
                    .reinterpretAsBytes()
                    .compare(VectorOperators.EQ, (byte)0)
                    .or(Helper.BYTE_INDEX_VECTOR.compare(VectorOperators.LT, (byte)length()))
                    .allTrue();
            return first && second;
        } else if (isLatin1()) {
            return value.length == length() && length() > StringCompressed.COMPRESS_THRESHOLD && this.secondHalf == 0;
        } else {
            return value.length == (length() << 1) && this.firstHalf == Utils.UTF16 && this.secondHalf == 0;
        }
    }

    private static Charset lookupCharset(String csn) throws UnsupportedEncodingException {
        Objects.requireNonNull(csn);
        try {
            return Charset.forName(csn);
        } catch (UnsupportedCharsetException | IllegalCharsetNameException x) {
            throw new UnsupportedEncodingException(csn);
        }
    }

    /*
     * StringIndexOutOfBoundsException  if {@code index} is
     * negative or greater than or equal to {@code length}.
     */
    static void checkIndex(int index, int length) {
        Utils.stringCheckIndex(index, length);
    }

    /*
     * StringIndexOutOfBoundsException  if {@code offset}
     * is negative or greater than {@code length}.
     */
    static void checkOffset(int offset, int length) {
        Utils.stringCheckOffset(offset, length);
    }

    /*
     * Check {@code offset}, {@code count} against {@code 0} and {@code length}
     * bounds.
     *
     * @throws  StringIndexOutOfBoundsException
     *          If {@code offset} is negative, {@code count} is negative,
     *          or {@code offset} is greater than {@code length - count}
     */
    static void checkBoundsOffCount(int offset, int count, int length) {
        Utils.stringCheckBoundsOffCount(offset, count, length);
    }

    /*
     * Check {@code begin}, {@code end} against {@code 0} and {@code length}
     * bounds.
     *
     * @throws  StringIndexOutOfBoundsException
     *          If {@code begin} is negative, {@code begin} is greater than
     *          {@code end}, or {@code end} is greater than {@code length}.
     */
    static void checkBoundsBeginEnd(int begin, int end, int length) {
        Utils.stringCheckBoundsBeginEnd(begin, end, length);
    }

    private int indexOfNonWhitespace() {
        if (isCompressed()) {
            return StringCompressed.indexOfNonWhitespace(firstHalf, secondHalf, length());
        } else if (isLatin1()) {
            return StringLatin1.indexOfNonWhitespace(content());
        } else {
            return StringUTF16.indexOfNonWhitespace(value);
        }
    }

    private int lastIndexOfNonWhitespace() {
        if (isCompressed()) {
            return StringCompressed.lastIndexOfNonWhitespace(firstHalf, secondHalf, length());
        } else if (isLatin1()) {
            return StringLatin1.lastIndexOfNonWhitespace(content());
        } else {
            return StringUTF16.lastIndexOfNonWhitespace(value);
        }
    }

    /**
     * Returns an {@link Optional} containing the nominal descriptor for this
     * instance.
     *
     * @return an {@link Optional} describing the {@linkplain InlineString} instance
     */
    @Override
    public Optional<DynamicConstantDesc<InlineString.ref>> describeConstable() {
        var str = this.toString();
        return Optional.of(DynamicConstantDesc.<InlineString.ref>ofNamed(Utils.INLINE_STRING_CONSTRUCTOR_STRING,
                str,
                Utils.INLINE_STRING_CLASS,
                str));
    }
}


