package org.lila.clockencoder;

public class LowBitTruncator {
    // Truncate 3 bits from centisecs, but preserve precision for low values.
    private static final int CENTI_CUTOFF = 1000;
    private static final int TRUNC_CUTOFF = CENTI_CUTOFF >>> 3;

    public static TruncPair lossyEncode(int[] centis) {
        IntArrayList truncDigits = new IntArrayList();
        int moves = centis.length;
        int[] trunced = new int[moves];

        for (int i = 0; i < moves; i++) {
            int cs = centis[i];
            if (cs < CENTI_CUTOFF) {
                truncDigits.add(cs % 8);
                trunced[i] = cs >>> 3;
            } else {
                // Round to ensure tenths of second will be accurate.
                // It's impossible for these values to be below TRUNC_CUTOFF.
                trunced[i] = (cs + 4) >>> 3;
            }
        }
        return new TruncPair(trunced, truncDigits.elements());
    }

    public static void writeDigits(int[] truncDigits, BitWriter writer) {
        for (int lowBit : truncDigits) {
            writer.writeBits(lowBit, 3);
        }
    }

    public static int[] decode(int[] trunced, BitReader reader) {
        int moves = trunced.length;
        int[] centis = new int[moves];
        for (int i = 0; i < moves; i++) {
            int rounded = trunced[i] << 3;
            if (rounded < TRUNC_CUTOFF) {
                centis[i] = rounded | reader.readBits(3);
            } else {
                // Value was above cutoff, so we didn't save precision bits.
                // Instead, we rounded the number to the nearest 8/100 of a second.
                // We can restore the tenths digit by rounding to the closest 10th.
                int mod = rounded % 10;
                centis[i] = rounded - mod + (mod > 4 ? 10 : 0);
            }
        }
        return centis;
    }

    public static class TruncPair {
        final int[] trunced;
        final int[] lowBits;
        TruncPair(int[] trunced, int[] lowBits) {
            this.trunced = trunced;
            this.lowBits = lowBits;
        }
    }
}