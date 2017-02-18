package org.lila.clockencoder;

// Simple list wrapper for primative int array.
class IntArrayList {
    private int[] elements = null;
    private int index = 0;

    private static final int DEFAULT_CAPACITY = 10;
    
    IntArrayList() {
      this(DEFAULT_CAPACITY);
    }

    IntArrayList(int initialCapacity) {
      elements = new int[initialCapacity];
    }

    void add(int elt) {
        if (index == elements.length) {
            int[] oldData = elements;
            elements = new int[(index * 3)/2 + 1];
            System.arraycopy(oldData, 0, elements, 0, index);
        }
        elements[index++] = elt;
    }

    void clear() {
        index = 0;
    }

    int size() {
       return index;
    }

    int[] elements() {
        int[] a = new int[index];
        System.arraycopy(elements, 0, a, 0, index);
        return a;
    }
} 