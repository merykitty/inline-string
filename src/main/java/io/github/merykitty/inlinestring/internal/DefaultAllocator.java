package io.github.merykitty.inlinestring.internal;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

public class DefaultAllocator implements SegmentAllocator {
    public static DefaultAllocator get() {
        return INSTANCE;
    }

    @Override
    public MemorySegment allocate(long byteSize, long byteAlignment) {
        if (byteAlignment > POINTER_SIZE ||
                (byteAlignment & (byteAlignment - 1)) != 0) {
            throw new IllegalArgumentException("Invalid alignment");
        }

        long address = malloc(byteSize);
        if (address == 0) {
            throw new OutOfMemoryError("fail to malloc " + byteSize + " bytes");
        }

        var scope = SegmentScope.auto();
        return MemorySegment.ofAddress(
                address,
                byteSize,
                scope,
                () -> free(address)
        );
    }

    private static final DefaultAllocator INSTANCE = new DefaultAllocator();
    private static final long POINTER_SIZE = ValueLayout.ADDRESS.bitSize();
    private static final MethodHandle MALLOC;
    private static final MethodHandle FREE;

    static {
        if (POINTER_SIZE != 64 && POINTER_SIZE != 32) {
            throw new AssertionError();
        }
        var linker = Linker.nativeLinker();
        var lib = linker.defaultLookup();
        var mallocSymbol = lib.find("malloc").get();
        var freeSymbol = lib.find("free").get();
        if (POINTER_SIZE == 64) {
            MALLOC = linker.downcallHandle(mallocSymbol,
                    FunctionDescriptor.of(ValueLayout.JAVA_LONG,
                            ValueLayout.JAVA_LONG));
            FREE = linker.downcallHandle(freeSymbol,
                    FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG));
        } else {
            MALLOC = linker.downcallHandle(mallocSymbol,
                    FunctionDescriptor.of(ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT));
            FREE = linker.downcallHandle(freeSymbol,
                    FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT));
        }
    }

    private DefaultAllocator() {}

    private static long malloc(long byteSize) {
        try {
            if (POINTER_SIZE == 64) {
                return (long)MALLOC.invokeExact(byteSize);
            } else {
                return (int)MALLOC.invokeExact((int)byteSize);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static void free(long ptr) {
        try {
            if (POINTER_SIZE == 64) {
                FREE.invokeExact(ptr);
            } else {
                FREE.invokeExact((int)ptr);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}