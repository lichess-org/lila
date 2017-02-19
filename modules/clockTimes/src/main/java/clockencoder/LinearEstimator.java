package org.lila.clockencoder;

public class LinearEstimator {
    // Input: Array of absolute clock times for a players move
    // Output: Encoded array of clock times.
    public static int[] encode(int[] src) {
       return encodeDecode(src, true);
    }

    public static int[] decode(int[] src) {
        return encodeDecode(src, false);
    }

    private static int[] encodeDecode(int[] src, boolean isEncoding) {
        int size = src.length;
        int[] dest = new int[size];
        dest[0] = src[0];
        dest[size - 1] = src[size - 1];

        int[] realValues = isEncoding ? src : dest;
        return encodeHelper(src, dest, realValues, 0, size - 1);
    }

    private static int[] encodeHelper(int[] src, int[] dest, int[] realValues,
                                     int startIdx, int endIdx) {
        int l = endIdx - startIdx;
        if (l == 0) return dest;

        // It's important to save estimate in fixed precision to ensure
        // the encode and decode math behaves identically.
        int estimate = (realValues[startIdx] + realValues[endIdx]) >>> 1;

        int midIdx = startIdx + (l >>> 1);

        dest[midIdx] = estimate - src[midIdx];

        encodeHelper(src, dest, realValues, startIdx, midIdx);
        return encodeHelper(src, dest, realValues, midIdx, endIdx);
    }
}