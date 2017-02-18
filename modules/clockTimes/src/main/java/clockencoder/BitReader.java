package org.lila.clockencoder;

import java.nio.ByteBuffer;

public class BitReader {
    private static final int[] BITMASK = BitOps.getBitMasks();

    private final ByteBuffer bb;
    private int numRemainingBits = 0;
    private int pendingBits = 0;

    public BitReader(byte[] bytes) {
        bb = ByteBuffer.wrap(bytes);
    }

    private void readNext() {
        int r = bb.remaining();
        if (r > 3) {
            numRemainingBits = 32;
            pendingBits = bb.getInt();
        } else {
            numRemainingBits = r * 8;
            pendingBits = bb.get();
            for (int i = 1; i < r; i++) {
                pendingBits = (pendingBits << 8) | bb.get();
            }
        }
    }

    public int readBits(int numReqBits) {
        if (numReqBits > numRemainingBits) {
            int neededBits = numReqBits - numRemainingBits;
            int res = pendingBits & BITMASK[numRemainingBits];
            readNext();
            return (res << neededBits) | readBits(neededBits);
        } else {
            numRemainingBits -= numReqBits;
            return (pendingBits >>> numRemainingBits) & BITMASK[numReqBits];
        }
    }
}