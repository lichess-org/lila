package org.lichess.clockencoder;

public class LowBitTruncator {
    // Truncate 3 bits from centisecs, but preserve precision for low values.
    // CENTI_CUTOFF must be a multiple of 8 (the truncation divisor)
    private static final int CENTI_CUTOFF = 1000;

    public static void truncate(int[] centis) {
        int moves = centis.length;
        for (int i = 0; i < moves; i++) {
            centis[i] >>= 3;
        }
    }

    public static int truncate(int centi) {
        return centi >> 3;
    }

    public static void writeDigits(int[] centis, BitWriter writer) {
        int maxIdx = centis.length - 1;
        for (int i = 0; i < maxIdx; i++) {
            if (centis[i] < CENTI_CUTOFF)
                writer.writeBits(centis[i], 3);
        }
        // Always store full precision end.
        writer.writeBits(centis[maxIdx], 3);
    }

    public static void decode(int[] trunced, BitReader reader) {
        int maxIdx = trunced.length - 1;
        for (int i = 0; i <= maxIdx; i++) {
            int rounded = trunced[i] << 3;
            if (rounded < CENTI_CUTOFF || i == maxIdx) {
                trunced[i] = rounded | reader.readBits(3);
            } else {
                // Truncation cuts off 3.5 on average.
                trunced[i] = rounded | 3;
            }
        }
    }
}
