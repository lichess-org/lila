package org.lichess.clockencoder;

public class VarIntEncoder {
    public static void write(int[] values, BitWriter writer) {
        for (int n : values) {
            n = (n << 1) ^ (n >> 31); // zigzag encode

            if ((n & ~0x1F) == 0) {
                writer.writeBits(n, 6);
            } else {
                writer.writeBits((n | 0x20), 6);
                n >>>= 5;
                while ((n & ~0x07) != 0) {
                    writer.writeBits((n | 0x08), 4);
                    n >>>= 3;
                }
                // While loop terminated, so 4th bit is 0.
                writer.writeBits(n, 4);
            }
        }
    }

    public static int[] read(BitReader reader, int numMoves) {
        int[] values = new int[numMoves];

        for (int moveIdx = 0; moveIdx < numMoves; moveIdx++) {
            int n = reader.readBits(6);
            if ((n & 0x20) != 0) {
                n ^= 0x20;
                int curShift = 5;
                int curVal;
                while (((curVal = reader.readBits(4)) & 0x08) != 0) {
                    n |= (curVal & 0x07) << curShift;
                    curShift += 3;
                }
                n |= curVal << curShift;
            }

            // zigzag decode and save
            values[moveIdx] = (n >>> 1) ^ -(n & 1);
        }

        return values;
    }
}
