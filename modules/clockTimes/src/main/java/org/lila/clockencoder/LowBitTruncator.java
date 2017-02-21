package org.lila.clockencoder;

public class LowBitTruncator {
    // Truncate 3 bits from centisecs, but preserve precision for low values.
    // CENTI_CUTOFF must be a multiple of 8 (the truncation divisor)
    private static final int CENTI_CUTOFF = 1000;

    public static void truncate(int[] centis) {
        int moves = centis.length;
        for (int i = 0; i < moves; i++) {
            // NOTE: this is a sign extending shift. This shift
            // is an optimized divide and so should preserve sign.
            centis[i] >>= 3;
        }
    }

    public static void writeDigits(int[] centis, BitWriter writer) {
        for (int cs : centis) {
            if (cs < CENTI_CUTOFF)
                writer.writeBits(cs, 3);
        }
    }

    public static void decode(int[] trunced, BitReader reader) {
        int moves = trunced.length;
        for (int i = 0; i < moves; i++) {
            int rounded = trunced[i] << 3;
            if (rounded < CENTI_CUTOFF) {
                trunced[i] = rounded | reader.readBits(3);
            } else {
                // Truncation cuts off 3.5 on average.
                trunced[i] = rounded | 3;
            }
        }
    }
}