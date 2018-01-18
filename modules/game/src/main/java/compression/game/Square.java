package org.lichess.compression.game;

class Square {
    public static final int C1 = 2;
    public static final int D1 = 3;
    public static final int F1 = 5;
    public static final int G1 = 6;

    public static int file(int square) {
        return square & 7;
    }

    public static int rank(int square) {
        return square >> 3;
    }

    public static int combine(int file, int rank) {
        return file(file) ^ (rank(rank) << 3);
    }

    public static int distance(int a, int b) {
        return Integer.max(Math.abs(file(a) - file(b)),
                           Math.abs(rank(a) - rank(b)));
    }

    public static boolean aligned(int a, int b, int c) {
        return Bitboard.contains(Bitboard.RAYS[a][b], c);
    }
}
