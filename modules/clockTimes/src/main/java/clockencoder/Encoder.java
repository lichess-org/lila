package org.lila.clockencoder;

public class Encoder {
    public static byte[] encode(int[] centis) {
        LowBitTruncator.TruncPair trunc = LowBitTruncator.lossyEncode(centis);
        int[] encodedRounds = LinearEstimator.encode(trunc.trunced);

        BitWriter writer = new BitWriter();
        VarIntEncoder.encode(encodedRounds, writer);
        LowBitTruncator.writeDigits(trunc.lowBits, writer);
        return writer.toArray();
    }

    public static int[] decode(byte[] bytes, int numMoves) {
        BitReader reader = new BitReader(bytes);

        int[] encodedRounds = VarIntEncoder.decode(reader, numMoves);
        int[] rounded = LinearEstimator.decode(encodedRounds);

        return LowBitTruncator.decode(rounded, reader);
    }
}