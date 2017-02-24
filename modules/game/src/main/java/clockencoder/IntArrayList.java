package org.lichess.clockencoder;

import java.nio.ByteBuffer;
import java.util.Arrays;

// Simple int array wrapper. Not threadsafe.
public final class IntArrayList {
    int[] data;
    int index = 0;

    public IntArrayList(int initialCapacity) {
        data = new int[initialCapacity];
    }

    public IntArrayList() {
        this(10);
    }

    public void add(int elt) {
        if (index == data.length) {
            data = Arrays.copyOf(data, index + (index >> 1) + 5);
        }

        data[index++] = elt;
    }

    public int size() {
       return index;
    }

    public void writeTo(ByteBuffer bb) {
        for (int i = 0; i < index; i++) bb.putInt(data[i]);
    }

    public int[] toArray() {
        return Arrays.copyOf(data, index);
    }
}
