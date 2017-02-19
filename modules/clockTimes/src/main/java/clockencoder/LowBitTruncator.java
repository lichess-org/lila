package org.lila.clockencoder;

public class LowBitTruncator {
    // Truncate 3 bits from centisecs, but preserve precision for low values.
    // CENTI_CUTOFF must be a multiple of 8 (the truncation divisor)
    private static final int CENTI_CUTOFF = 1000;
    private static final int TRUNC_CUTOFF = CENTI_CUTOFF >>> 3;

    public static TruncPair lossyEncode(int[] centis) {
        IntArrayList truncDigits = new IntArrayList();
        int moves = centis.length;
        int[] trunced = new int[moves];

        for (int i = 0; i < moves; i++) {
            int cs = centis[i];
            trunced[i] = cs >>> 3;
            if (cs < CENTI_CUTOFF) truncDigits.add(cs & 0x07);
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
                // Truncation cuts off 3.5 on average, so roughly alternate
                // between +3 and +4 to avoid skew.
                centis[i] = rounded + 3 + (i & 0x01);
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