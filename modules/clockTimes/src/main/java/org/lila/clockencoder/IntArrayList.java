package org.lila.clockencoder;

import java.nio.ByteBuffer;
import java.util.Arrays;

// Simple int array wrapper.
public class IntArrayList {
    int[] data = null;
    int index = 0;

    private static final int DEFAULT_CAPACITY = 10;
    
    public IntArrayList() {
      this(DEFAULT_CAPACITY);
    }

    public IntArrayList(int initialCapacity) {
      data = new int[initialCapacity];
    }

    public void add(int elt) {
        if (index == data.length) {
            data = Arrays.copyOf(data, (index * 3)/2 + 1);
        }
        data[index++] = elt;
    }

    public void pop() {
        if (index == 0) throw new IndexOutOfBoundsException();
        index--;
    }

    public void clear() {
        index = 0;
    }

    public void writeTo(ByteBuffer b) {
        for (int i = 0; i < index; i++) b.putInt(data[i]);
    }

    public int size() {
       return index;
    }

    public int[] toArray() {
        return Arrays.copyOf(data, index);
    }
} 