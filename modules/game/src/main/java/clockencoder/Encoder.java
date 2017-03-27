package org.lichess.clockencoder;

import java.util.Arrays;

/*
 * startTime is the initial clock time in centiseconds
 * (e.g. 6000 in a 1+0 or 1+2 game)
 */
public class Encoder {

    public static byte[] encode(int[] centis, int startTime) {
        if (centis.length == 0) return new byte[0];

        int[] encoded = Arrays.copyOf(centis, centis.length);
        int truncatedStart = LowBitTruncator.truncate(startTime);

        LowBitTruncator.truncate(encoded);
        LinearEstimator.encode(encoded, truncatedStart);
        EndTimeEstimator.encode(encoded, truncatedStart);

        BitWriter writer = new BitWriter();
        VarIntEncoder.writeUnsigned(encoded.length - 1, writer);
        VarIntEncoder.writeSigned(encoded, writer);
        LowBitTruncator.writeDigits(centis, writer);

        return writer.toArray();
    }

    public static int[] decode(byte[] bytes, int startTime) {
        if (bytes.length == 0) return new int[0];

        BitReader reader = new BitReader(bytes);
        int truncatedStart = LowBitTruncator.truncate(startTime);

        int numMoves = VarIntEncoder.readUnsigned(reader) + 1;
        int[] decoded = VarIntEncoder.readSigned(reader, numMoves);

        EndTimeEstimator.decode(decoded, truncatedStart);
        LinearEstimator.decode(decoded, truncatedStart);
        LowBitTruncator.decode(decoded, reader);

        return decoded;
    }
}
