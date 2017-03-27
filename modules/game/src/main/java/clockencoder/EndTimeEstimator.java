package org.lichess.clockencoder;

public class EndTimeEstimator {
    public static void encode(int[] vals, int startTime) {
        int maxIdx = vals.length - 1;
        // Linear estimate with move 0 == startTime and move 32 == 0.
        if (maxIdx < 32) {
            vals[maxIdx] -= startTime - ((startTime * maxIdx) >>> 5);
        }
    }

    public static void decode(int[] vals, int startTime) {
        int maxIdx = vals.length - 1;
        // Linear estimate with move 0 == startTime and move 32 == 0.
        if (maxIdx < 32) {
            vals[maxIdx] += startTime - ((startTime * maxIdx) >>> 5);
        }
    }
}