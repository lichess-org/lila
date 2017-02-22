package org.lichess.clockencoder;

import java.util.Arrays;

public class Encoder {
    public static byte[] encode(int[] centis, int startTime) {
        int[] encoded = Arrays.copyOf(centis, centis.length);
        int truncatedStart = LowBitTruncator.truncate(startTime);

        LowBitTruncator.truncate(encoded);
        LinearEstimator.encode(encoded, truncatedStart);
        EndTimeEstimator.encode(encoded, truncatedStart);

        BitWriter writer = new BitWriter();
        VarIntEncoder.write(encoded, writer);
        LowBitTruncator.writeDigits(centis, writer);

        return writer.toArray();
    }

    public static int[] decode(byte[] bytes, int numMoves, int startTime) {
        BitReader reader = new BitReader(bytes);
        int truncatedStart = LowBitTruncator.truncate(startTime);

        int[] decoded = VarIntEncoder.read(reader, numMoves);

        EndTimeEstimator.decode(decoded, truncatedStart);
        LinearEstimator.decode(decoded, truncatedStart);
        LowBitTruncator.decode(decoded, reader);

        return decoded;
    }
}
