package org.lila.clockencoder;

public class LinearEstimator {
    // Input: Array of absolute clock times for a players move
    // Output: Encoded array of clock times.

    public static void encode(int[] dest, int startTime) {
        int size = dest.length;
        encode(dest, -1, size - 1, startTime, dest[size - 1]);
    }

    public static void decode(int[] dest, int startTime) {
        int size = dest.length;
        decode(dest, -1, size - 1, startTime, dest[size - 1]);
    }

    private static void encode(int[] dest, int startIdx, int endIdx,
                               int start, int end) {
        int l = endIdx - startIdx;
        if (l < 2) return;

        int midIdx = startIdx + (l >>> 1);
        int mid = dest[midIdx];

        dest[midIdx] = ((start + end) >>> 1) - mid;

        encode(dest, startIdx, midIdx, start, mid);
        encode(dest, midIdx, endIdx, mid, end);
    }

    private static void decode(int[] dest, int startIdx, int endIdx,
                               int start, int end) {
        int l = endIdx - startIdx;
        if (l < 2) return;

        int midIdx = startIdx + (l >>> 1);
        int mid = ((start + end) >>> 1) - dest[midIdx];

        dest[midIdx] = mid;

        decode(dest, startIdx, midIdx, start, mid);
        decode(dest, midIdx, endIdx, mid, end);
    }
}