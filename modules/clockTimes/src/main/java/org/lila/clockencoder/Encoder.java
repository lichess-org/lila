package org.lila.clockencoder;

public class Encoder {
    public static byte[] encode(int[] centis, int startTime) {
        IntArrayList lowBits = new IntArrayList();
        int[] trunced = LowBitTruncator.lossyEncode(centis, lowBits);
        LinearEstimator.encode(trunced, startTime);

        BitWriter writer = new BitWriter();
        VarIntEncoder.encode(trunced, writer);
        LowBitTruncator.writeDigits(lowBits, writer);
        return writer.toArray();
    }

    public static int[] decode(byte[] bytes, int numMoves, int startTime) {
        BitReader reader = new BitReader(bytes);

        int[] trunced = VarIntEncoder.decode(reader, numMoves);
        LinearEstimator.decode(trunced, startTime);

        return LowBitTruncator.decode(trunced, reader);
    }
}