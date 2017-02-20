package org.lila.clockencoder;

public class LowBitTruncator {
    // Truncate 3 bits from centisecs, but preserve precision for low values.
    // CENTI_CUTOFF must be a multiple of 8 (the truncation divisor)
    private static final int CENTI_CUTOFF = 1000;

    public static int[] lossyEncode(int[] centis, IntArrayList truncDigits) {
        int moves = centis.length;
        int[] trunced = new int[moves];

        for (int i = 0; i < moves; i++) {
            int cs = centis[i];

            // NOTE: this is a sign extending shift. This shift
            // is an optimized divide and so should preserve sign.
            trunced[i] = cs >> 3;

            // We'll only use the 3 bottom bits, but don't bother to mask.
            if (cs < CENTI_CUTOFF) truncDigits.add(cs);
        }
        return trunced;
    }

    public static void writeDigits(IntArrayList digits, BitWriter writer) {
        int[] data = digits.data;
        int size = digits.index;
        for (int i = 0; i < size; i++) {
            writer.writeBits(data[i], 3);
        }
    }

    public static int[] decode(int[] trunced, BitReader reader) {
        int moves = trunced.length;
        int[] centis = new int[moves];
        for (int i = 0; i < moves; i++) {
            int rounded = trunced[i] << 3;
            if (rounded < CENTI_CUTOFF) {
                centis[i] = rounded | reader.readBits(3);
            } else {
                // Truncation cuts off 3.5 on average.
                centis[i] = rounded + 3;
            }
        }
        return centis;
    }
}