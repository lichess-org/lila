package org.lichess.clockencoder;

import java.util.Arrays;

public class Encoder {
    public static byte[] encode(int[] centis, int startTime, int endTime) {
        int[] encoded = Arrays.copyOf(centis, centis.length);
        int truncatedStart = LowBitTruncator.truncate(startTime);
        int truncatedEnd = LowBitTruncator.truncate(endTime);

        LowBitTruncator.truncate(encoded);
        LinearEstimator.encode(encoded, truncatedStart, truncatedEnd);

        BitWriter writer = new BitWriter();
        VarIntEncoder.write(encoded, writer);
        LowBitTruncator.writeDigits(centis, writer);

        return writer.toArray();
    }

    public static int[] decode(byte[] bytes, int numValues, int startTime, int endTime) {
        BitReader reader = new BitReader(bytes);
        int truncatedStart = LowBitTruncator.truncate(startTime);
        int truncatedEnd = LowBitTruncator.truncate(endTime);

        int[] decoded = VarIntEncoder.read(reader, numValues);

        LinearEstimator.decode(decoded, truncatedStart, truncatedEnd);
        LowBitTruncator.decode(decoded, reader);

        return decoded;
    }
}
