package org.lichess.clockencoder;

import java.nio.ByteBuffer;

public class BitWriter {
    private static final int[] BITMASK = BitOps.getBitMasks();

    private final IntArrayList buffer = new IntArrayList();
    private int numRemainingBits = 32;
    private int pendingBits = 0;

    public void writeBits(int data, int numBits) {
        data &= BITMASK[numBits];
        numRemainingBits -= numBits;
        if (numRemainingBits >= 0) {
            pendingBits |= data << numRemainingBits;
        } else {
            buffer.add(pendingBits | (data >>> -numRemainingBits));
            numRemainingBits += 32;
            pendingBits = data << numRemainingBits;
        }
    }

    public byte[] toArray() {
        int numPendingBytes = (39 - numRemainingBits) >> 3;
        ByteBuffer bb = ByteBuffer.allocate(4 * buffer.size() + numPendingBytes);
        buffer.writeTo(bb);
        if (numPendingBytes == 4) {
            bb.putInt(pendingBits);
        } else {
            for (int i = 0; i < numPendingBytes; i++) {
                bb.put((byte)(pendingBits >>> (24 - i * 8)));
            }
        }
        return bb.array();
    }
}
