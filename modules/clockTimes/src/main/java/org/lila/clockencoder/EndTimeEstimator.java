package org.lila.clockencoder;

public class EndTimeEstimator {
    public static void encode(int[] vals, int startTime) {
        int moves = vals.length;
        // TODO: Test this.
        vals[moves - 1] -= (startTime >>> (moves >>> 3));
    }

    public static void decode(int[] vals, int startTime) {
        int moves = vals.length;
        // TODO: Test this.
        vals[moves - 1] += (startTime >>> (moves >>> 3));
    }
}