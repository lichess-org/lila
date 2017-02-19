package org.lila.clockencoder;

public class VarIntEncoder {
    public static void encode(int[] values, BitWriter writer) {
        for (int n : values) {
            n = (n << 1) ^ (n >> 31); // zigzagEncode

            if ((n & ~0x1F) == 0) {
                writer.writeBits(n, 6);
            } else {
                writer.writeBits((n | 0x20), 6);
                n >>>= 5;
                do {
                    if ((n & ~0x07) == 0) {
                        writer.writeBits(n, 4);
                    } else {
                        writer.writeBits((n | 0x08), 4);
                    }
                    n >>>= 3;
                } while (n != 0);
            }
        }
    }

    public static int[] decode(BitReader reader, int numMoves) {
        int[] values = new int[numMoves];

        int[] numBuffer = new int[16];
        
        for (int curMove = 0; curMove < numMoves; curMove++) {
            int base = reader.readBits(6);
            int unsignedNum;
            if ((base & 0x20) == 0) {
                unsignedNum = base;
            } else {
                int idx = -1;
                do {
                    numBuffer[++idx] = reader.readBits(4);
                } while ((numBuffer[idx] & 0x08) != 0);

                unsignedNum = numBuffer[idx--];
                for (; idx >= 0 ; idx--) {
                    unsignedNum = (unsignedNum << 3) | numBuffer[idx] & 0x07;
                }
                unsignedNum = (unsignedNum << 5) | base & 0x1F;
            }

            // zigzag decode and save
            values[curMove] = (unsignedNum >>> 1) ^ -(unsignedNum & 1);
        }

        return values;
    }
}