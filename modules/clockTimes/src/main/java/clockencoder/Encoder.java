package org.lila.clockencoder;

public class Encoder {
    public static byte[] encode(int[] centis) {
        LowBitTruncator.TruncPair truncPair = LowBitTruncator.lossyEncode(centis);
        int[] encodedRounds = LinearEstimator.encode(truncPair.trunced);

        BitWriter writer = new BitWriter();
        VarIntEncoder.encode(encodedRounds, writer);
        for (int lowBit : truncPair.lowBits) {
            writer.writeBits(lowBit, 3);
        }
        return writer.toArray();
    }

    public static int[] decode(byte[] bytes, int numMoves) {
        BitReader reader = new BitReader(bytes);

        int[] encodedRounds = VarIntEncoder.decode(reader, numMoves);
        int[] rounded = LinearEstimator.decode(encodedRounds);

        return LowBitTruncator.decode(rounded, reader);
    }
}