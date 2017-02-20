package org.lila.clockencoder;

import java.nio.ByteBuffer;

public class BitWriter {
    private static final int[] BITMASK = BitOps.getBitMasks();

    IntArrayList buffer = new IntArrayList();
    int numRemainingBits = 32;
    int pendingBits = 0;

    public void writeBits(int data, int numBits) {
        data &= BITMASK[numBits];
        int extraBits = numRemainingBits - numBits;
        if (extraBits >= 0) {
            numRemainingBits = extraBits;
            pendingBits |= data << extraBits;
        } else {
            buffer.add(pendingBits | (data >>> -extraBits));
            numRemainingBits = 32 + extraBits;
            pendingBits = data << numRemainingBits;
        }
    }

    public byte[] toArray() {
        int numPendingBytes = (39 - numRemainingBits) / 8;
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