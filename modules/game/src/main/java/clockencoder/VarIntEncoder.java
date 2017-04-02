package org.lichess.clockencoder;

public class VarIntEncoder {
    public static void writeSigned(int[] values, BitWriter writer) {
        for (int n : values) {
            writeSigned(n, writer);
        }
    }

    public static void writeSigned(int n, BitWriter writer) {
        // zigzag encode
        writeUnsigned((n << 1) ^ (n >> 31), writer);
    }

    public static void writeUnsigned(int n, BitWriter writer) {
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

    public static int readUnsigned(BitReader reader) {
        int n = reader.readBits(6);
        if (n > 0x1F) {
            n &= 0x1F;
            int curShift = 5;
            int curVal;
            while ((curVal = reader.readBits(4)) > 0x07) {
                n |= (curVal & 0x07) << curShift;
                curShift += 3;
            }
            n |= curVal << curShift;
        }
        return n;
    }

    public static int readSigned(BitReader reader) {
        int n = readUnsigned(reader);
        return (n >>> 1) ^ -(n & 1);  // zigzag decode
    }

    public static int[] readSigned(BitReader reader, int numMoves) {
        int[] values = new int[numMoves];

        for (int i = 0; i < numMoves; i++) {
            int n = readUnsigned(reader);
            values[i] = (n >>> 1) ^ -(n & 1);  // zigzag decode
        }

        return values;
    }
}
