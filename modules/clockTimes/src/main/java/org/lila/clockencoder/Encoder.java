package org.lila.clockencoder;

import java.util.Arrays;

public class Encoder {
    public static byte[] encode(int[] centis, int startTime) {
        int[] encoded = Arrays.copyOf(centis, centis.length);

        LowBitTruncator.truncate(encoded);
        LinearEstimator.encode(encoded, startTime >> 3);
        EndTimeEstimator.encode(encoded, startTime >> 3);

        BitWriter writer = new BitWriter();
        VarIntEncoder.write(encoded, writer);
        LowBitTruncator.writeDigits(centis, writer);

        return writer.toArray();
    }

    public static int[] decode(byte[] bytes, int numMoves, int startTime) {
        BitReader reader = new BitReader(bytes);

        int[] decoded = VarIntEncoder.read(reader, numMoves);

        EndTimeEstimator.decode(decoded, startTime >> 3);
        LinearEstimator.decode(decoded, startTime >> 3);
        LowBitTruncator.decode(decoded, reader);

        return decoded;
    }
}