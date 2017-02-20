package org.lila.clockencoder;

public class VarIntEncoder {
    public static void encode(int[] values, BitWriter writer) {
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

    // Decode numMoves into an array padded by offset.
    public static int[] decode(BitReader reader, int numMoves) {
        int[] values = new int[numMoves];

        int[] numBuffer = new int[16];
        for (int moveIdx = 0; moveIdx < numMoves; moveIdx++) {
            int base = reader.readBits(6);
            int n;
            if ((base & 0x20) == 0) {
                n = base;
            } else {
                int idx = -1;
                do {
                    numBuffer[++idx] = reader.readBits(4);
                } while ((numBuffer[idx] & 0x08) != 0);

                n = numBuffer[idx];
                while(idx > 0) {
                    n = (n << 3) | numBuffer[--idx] & 0x07;
                }
                n = (n << 5) | base & 0x1F;
            }

            // zigzag decode and save
            values[moveIdx] = (n >>> 1) ^ -(n & 1);
        }

        return values;
    }
}