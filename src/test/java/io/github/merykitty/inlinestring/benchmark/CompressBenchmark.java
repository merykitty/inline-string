package io.github.merykitty.inlinestring.benchmark;

import io.github.merykitty.inlinestring.InlineString;
import io.github.merykitty.inlinestring.internal.SmallStringByteData;
import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.SegmentAllocator;
import jdk.incubator.vector.*;
import jdk.internal.misc.Unsafe;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;

@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
public class CompressBenchmark {
    byte[] bytes;
    char[] chars;
    int length;
    int offset;
    long firstHalf;
    long secondHalf;

    private static final Unsafe UNSAFE = Unsafe.getUnsafe();

    @__primitive__
    record SmallString(long firstHalf, long secondHalf) {}

    @__primitive__
    private record SmallStringCharData(long firstQuarter, long secondQuarter, long thirdQuarter, long fourthQuarter) {}

    @Setup(Level.Trial)
    public void warmup() {
        var random = RandomGenerator.getDefault();
        for (int i = 0; i < 1_000_000; i++) {
            bytes = new byte[random.nextInt(1, 32)];
            random.nextBytes(bytes);
            chars = new String(bytes, StandardCharsets.US_ASCII).toCharArray();
            while (true) {
                int first = random.nextInt(bytes.length);
                int second = random.nextInt(bytes.length);
                offset = Math.min(first, second);
                length = Math.abs(first - second);
                if (length <= 16) {
                    break;
                }
            }
            bytesToLongs();
            longsToBytes();
        }
    }

    @Setup(Level.Iteration)
    public void setUp() {
        var random = RandomGenerator.getDefault();
        bytes = new byte[random.nextInt(64)];
        random.nextBytes(bytes);
        chars = new String(bytes, StandardCharsets.US_ASCII).toCharArray();
        while (true) {
            int first = random.nextInt(bytes.length);
            int second = random.nextInt(bytes.length);
            offset = Math.min(first, second);
            length = Math.abs(first - second);
            if (length <= 16) {
                break;
            }
        }
    }

    @Benchmark
    public void bytesToLongs() {
        var temp = compress(bytes, offset, length);
        firstHalf = temp.firstHalf();
        secondHalf = temp.secondHalf();
    }

    @Benchmark
    public void longsToBytes() {
        uncompress(bytes, offset, length, firstHalf, secondHalf);
    }

//    @Benchmark
    public SmallStringCharData charsToLong() {
        return compress(chars, offset, length);
    }

    private static SmallString compress(byte[] value, int offset, int length) {
        final long firstHalf, secondHalf;
        // index >= 16
        if (length >= Long.BYTES * 2) {
            firstHalf = UNSAFE.getLongUnaligned(value, (long)offset + Unsafe.ARRAY_BYTE_BASE_OFFSET);
            secondHalf = UNSAFE.getLongUnaligned(value, (long)offset + Long.BYTES + Unsafe.ARRAY_BYTE_BASE_OFFSET);
        } else {
            long temp = 0;
            int tempOffset = offset + length;
            if ((length & Byte.BYTES) != 0) {
                tempOffset--;
                temp = Byte.toUnsignedLong(UNSAFE.getByte(value, (long)tempOffset + Unsafe.ARRAY_BYTE_BASE_OFFSET));
            }
            if ((length & Short.BYTES) != 0) {
                tempOffset -= Short.BYTES;
                temp = (temp << Short.SIZE) | Short.toUnsignedLong(UNSAFE.getShortUnaligned(value, (long)tempOffset + Unsafe.ARRAY_BYTE_BASE_OFFSET));
            }
            if ((length & Integer.BYTES) != 0) {
                tempOffset -= Integer.BYTES;
                temp = (temp << Integer.SIZE) | Integer.toUnsignedLong(UNSAFE.getIntUnaligned(value, (long)tempOffset + Unsafe.ARRAY_BYTE_BASE_OFFSET));
            }
            if ((length & Long.BYTES) != 0) {
                firstHalf = UNSAFE.getLongUnaligned(value, (long)offset + Unsafe.ARRAY_BYTE_BASE_OFFSET);
                secondHalf = temp;
            } else {
                firstHalf = temp;
                secondHalf = 0;
            }
        }
        return new SmallString(firstHalf, secondHalf);
    }

    private static void uncompress(byte[] dst, int offset, int length, long firstHalf, long secondHalf) {
        // index >= 16
        if (length >= Long.BYTES * 2) {
            UNSAFE.putLongUnaligned(dst, offset + Unsafe.ARRAY_BYTE_BASE_OFFSET, firstHalf);
            UNSAFE.putLongUnaligned(dst, offset + Long.BYTES + Unsafe.ARRAY_BYTE_BASE_OFFSET, secondHalf);
        } else {
            long temp = firstHalf;
            int tempOffset = offset;
            if ((length & Long.BYTES) != 0) {
                UNSAFE.putLongUnaligned(dst, tempOffset + Unsafe.ARRAY_BYTE_BASE_OFFSET, temp);
                temp = secondHalf;
                tempOffset += Long.BYTES;
            }
            if ((length & Integer.BYTES) != 0) {
                UNSAFE.putIntUnaligned(dst, tempOffset + Unsafe.ARRAY_BYTE_BASE_OFFSET, (int)temp);
                temp >>>= Integer.SIZE;
                tempOffset += Integer.BYTES;
            }
            if ((length & Short.BYTES) != 0) {
                UNSAFE.putShortUnaligned(dst, tempOffset + Unsafe.ARRAY_BYTE_BASE_OFFSET, (short)temp);
                temp >>>= Short.SIZE;
                tempOffset += Short.BYTES;
            }
            if ((length & Byte.BYTES) != 0) {
                UNSAFE.putByte(dst, tempOffset + Unsafe.ARRAY_BYTE_BASE_OFFSET, (byte)temp);
            }
        }
    }

    private static SmallStringCharData compress(char[] value, int offset, int length) {
        offset *= Short.BYTES;
        final long firstQuarter, secondQuarter, thirdQuarter, fourthQuarter;
        if (length == Long.BYTES * 2) {
            firstQuarter = UNSAFE.getLong(value, Unsafe.ARRAY_CHAR_BASE_OFFSET + offset);
            secondQuarter = UNSAFE.getLong(value, Unsafe.ARRAY_CHAR_BASE_OFFSET + offset + Long.BYTES);
            thirdQuarter = UNSAFE.getLong(value, Unsafe.ARRAY_CHAR_BASE_OFFSET + offset + Long.BYTES * 2);
            fourthQuarter = UNSAFE.getLong(value, Unsafe.ARRAY_CHAR_BASE_OFFSET + offset + Long.BYTES * 3);
        } else {
            long temp = 0;
            int tempOffset = offset + length * Short.BYTES;
            if ((length & Byte.BYTES) != 0) {
                tempOffset -= Short.BYTES;
                temp = Short.toUnsignedLong(UNSAFE.getShort(value, Unsafe.ARRAY_CHAR_BASE_OFFSET + tempOffset));
            }
            if ((length & Short.BYTES) != 0) {
                tempOffset -= Integer.BYTES;
                temp = (temp << Integer.SIZE) | Integer.toUnsignedLong(UNSAFE.getInt(value, Unsafe.ARRAY_CHAR_BASE_OFFSET + tempOffset));
            }
            final long first, second;
            if ((length & Integer.BYTES) != 0) {
                tempOffset -= Long.BYTES;
                first = UNSAFE.getLong(value, Unsafe.ARRAY_CHAR_BASE_OFFSET + tempOffset);
                second = temp;
            } else {
                first = temp;
                second = 0;
            }
            if ((length & Long.BYTES) != 0) {
                firstQuarter = UNSAFE.getLong(value, Unsafe.ARRAY_CHAR_BASE_OFFSET + offset);
                secondQuarter = UNSAFE.getLong(value, Unsafe.ARRAY_CHAR_BASE_OFFSET + offset + Long.BYTES);
                thirdQuarter = first;
                fourthQuarter = second;
            } else {
                firstQuarter = first;
                secondQuarter = second;
                thirdQuarter = 0;
                fourthQuarter = 0;
            }
        }
        return new SmallStringCharData(firstQuarter, secondQuarter, thirdQuarter, fourthQuarter);
    }
}
