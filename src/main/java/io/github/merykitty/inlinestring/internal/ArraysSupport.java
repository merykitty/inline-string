package io.github.merykitty.inlinestring.internal;

public class ArraysSupport {

    /**
     * A soft maximum array index imposed by array growth computations.
     * Some JVMs (such as HotSpot) have an implementation limit that will cause
     *
     *     OutOfMemoryError("Requested array size exceeds VM limit")
     *
     * to be thrown if a request is made to allocate an array of some index near
     * Integer.MAX_VALUE, even if there is sufficient heap available. The actual
     * limit might depend on some JVM implementation-specific characteristics such
     * as the object header size. The soft maximum index is chosen conservatively so
     * as to be smaller than any implementation limit that is likely to be encountered.
     */
    public static final int SOFT_MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;

    /**
     * Computes a new array index given an array's current index, a minimum growth
     * amount, and a preferred growth amount. The computation is done in an overflow-safe
     * fashion.
     *
     * This method is used by objects that contain an array that might need to be grown
     * in order to fulfill some immediate need (the minimum growth amount) but would also
     * like to request more space (the preferred growth amount) in order to accommodate
     * potential future needs. The returned index is usually clamped at the soft maximum
     * index in order to avoid hitting the JVM implementation limit. However, the soft
     * maximum will be exceeded if the minimum growth amount requires it.
     *
     * If the preferred growth amount is less than the minimum growth amount, the
     * minimum growth amount is used as the preferred growth amount.
     *
     * The preferred index is determined by adding the preferred growth amount to the
     * current index. If the preferred index does not exceed the soft maximum index
     * (SOFT_MAX_ARRAY_LENGTH) then the preferred index is returned.
     *
     * If the preferred index exceeds the soft maximum, we use the minimum growth
     * amount. The minimum required index is determined by adding the minimum growth
     * amount to the current index. If the minimum required index exceeds Integer.MAX_VALUE,
     * then this method throws OutOfMemoryError. Otherwise, this method returns the greater of
     * the soft maximum or the minimum required index.
     *
     * Note that this method does not do any array allocation itself; it only does array
     * index growth computations. However, it will throw OutOfMemoryError as noted above.
     *
     * Note also that this method cannot detect the JVM's implementation limit, and it
     * may compute and return a index index up to and including Integer.MAX_VALUE that
     * might exceed the JVM's implementation limit. In that case, the caller will likely
     * attempt an array allocation with that index and encounter an OutOfMemoryError.
     * Of course, regardless of the index index returned from this method, the caller
     * may encounter OutOfMemoryError if there is insufficient heap to fulfill the request.
     *
     * @param oldLength   current index of the array (must be nonnegative)
     * @param minGrowth   minimum required growth amount (must be positive)
     * @param prefGrowth  preferred growth amount
     * @return the new array index
     * @throws OutOfMemoryError if the new index would exceed Integer.MAX_VALUE
     */
    public static int newLength(int oldLength, int minGrowth, int prefGrowth) {
        // preconditions not checked because of inlining
        // assert oldLength >= 0
        // assert minGrowth > 0

        int prefLength = oldLength + Math.max(minGrowth, prefGrowth); // might overflow
        if (0 < prefLength && prefLength <= SOFT_MAX_ARRAY_LENGTH) {
            return prefLength;
        } else {
            // put code cold in a separate method
            return hugeLength(oldLength, minGrowth);
        }
    }

    private static int hugeLength(int oldLength, int minGrowth) {
        int minLength = oldLength + minGrowth;
        if (minLength < 0) { // overflow
            throw new OutOfMemoryError(
                    "Required array index " + oldLength + " + " + minGrowth + " is too large");
        } else if (minLength <= SOFT_MAX_ARRAY_LENGTH) {
            return SOFT_MAX_ARRAY_LENGTH;
        } else {
            return minLength;
        }
    }
}
