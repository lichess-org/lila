package org.lishogi.compression.game;

public class Square {
    public static final int A1 = 0;
    public static final int C1 = 2;
    public static final int D1 = 3;
    public static final int F1 = 5;
    public static final int G1 = 6;
    public static final int H1 = 7;
    public static final int A8 = 56;
    public static final int H8 = 63;

    public static int square(int file, int rank) {
        return file ^ (rank << 3);
    }

    public static int file(int square) {
        return square & 7;
    }

    public static int rank(int square) {
        return square >>> 3;
    }

    public static int mirror(int square) {
        return square ^ 0x38;
    }

    public static int combine(int a, int b) {
        return square(file(a), rank(b));
    }

    public static int distance(int a, int b) {
        return Math.max(Math.abs(file(a) - file(b)),
                        Math.abs(rank(a) - rank(b)));
    }

    public static boolean aligned(int a, int b, int c) {
        return Bitboard.contains(Bitboard.RAYS[a][b], c);
    }
}
