package org.lichess.clockencoder;

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
        if (bb.remaining() >= 4) {
            pendingBits = bb.getInt();
            numRemainingBits = 32;
        } else {
            numRemainingBits = bb.remaining() * 8;
            pendingBits = bb.get() << (numRemainingBits - 8);
            for (int s = numRemainingBits - 16; s >= 0; s -= 8) {
                pendingBits |= (bb.get() & 0xFF) << s;
            }
        }
    }

    public int readBits(int numReqBits) {
        if (numRemainingBits >= numReqBits) {
            numRemainingBits -= numReqBits;
            return (pendingBits >>> numRemainingBits) & BITMASK[numReqBits];
        } else {
            int res = pendingBits & BITMASK[numRemainingBits];
            int neededBits = numReqBits - numRemainingBits;
            readNext();
            return (res << neededBits) | readBits(neededBits);
        }
    }
}
