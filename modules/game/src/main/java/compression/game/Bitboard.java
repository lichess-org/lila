package org.lichess.compression.game;

class Bitboard {
    public static final long ALL = -1;

    public static final long RANKS[] = new long[8];
    public static final long FILES[] = new long[8];

    private static final int KNIGHT_DELTAS[] = { 17, 15, 10, 6, -17, -15, -10, -6 };
    private static final int BISHOP_DELTAS[] = { 7, -7, 9, -9 };
    private static final int ROOK_DELTAS[] = { 1, -1, 8, -8 };
    private static final int KING_DELTAS[] = { 1, 7, 8, 9, -1, -7, -8, -9 };
    private static final int WHITE_PAWN_DELTAS[] = { 7, 9 };
    private static final int BLACK_PAWN_DELTAS[] = { -7, -9 };

    public static final long KNIGHT_ATTACKS[] = new long[64];
    public static final long KING_ATTACKS[] = new long[64];
    public static final long WHITE_PAWN_ATTACKS[] = new long[64];
    public static final long BLACK_PAWN_ATTACKS[] = new long[64];

    private static final long ATTACKS[] = new long[88772];

    public static final long BETWEEN[][] = new long[64][64];
    public static final long RAYS[][] = new long[64][64];

    private static long slidingAttacks(int square, long occupied, int[] deltas) {
        long attacks = 0;
        for (int delta: deltas) {
            int sq = square;
            do {
                sq += delta;
                if (sq < 0 || 64 <= sq || Square.distance(sq, sq - delta) > 2) break;
                attacks |= 1L << sq;
            } while (!Bitboard.contains(occupied,  sq));
        }
        return attacks;
    }

    private static void initMagics(int square, Magic magic, int shift, int[] deltas) {
        long subset = 0;
        do {
            long attack = slidingAttacks(square, subset, deltas);
            int idx = (int) ((magic.factor * subset) >>> (64 - shift)) + magic.offset;
            assert ATTACKS[idx] == 0 || ATTACKS[idx] == attack;
            ATTACKS[idx] = attack;

            // Carry-rippler trick for enumerating subsets.
            subset = (subset - magic.mask) & magic.mask;
        } while (subset != 0);
    }

    static {
        for (int i = 0; i < 8; i++) {
            RANKS[i] = 0xffL << (i * 8);
            FILES[i] = 0x0101010101010101L << i;
        }

        for (int sq = 0; sq < 64; sq++) {
            KNIGHT_ATTACKS[sq] = slidingAttacks(sq, Bitboard.ALL, KNIGHT_DELTAS);
            KING_ATTACKS[sq] = slidingAttacks(sq, Bitboard.ALL, KING_DELTAS);
            WHITE_PAWN_ATTACKS[sq] = slidingAttacks(sq, Bitboard.ALL, WHITE_PAWN_DELTAS);
            BLACK_PAWN_ATTACKS[sq] = slidingAttacks(sq, Bitboard.ALL, BLACK_PAWN_DELTAS);

            initMagics(sq, Magic.ROOK[sq], 12, ROOK_DELTAS);
            initMagics(sq, Magic.BISHOP[sq], 9, BISHOP_DELTAS);
        }

        for (int a = 0; a < 64; a++) {
            for (int b = 0; b < 64; b++) {
                if (Bitboard.contains(slidingAttacks(a, 0, ROOK_DELTAS), b)) {
                    BETWEEN[a][b] =
                        slidingAttacks(a, 1L << b, ROOK_DELTAS) &
                        slidingAttacks(b, 1L << a, ROOK_DELTAS);
                    RAYS[a][b] =
                        (1L << a) | (1L << b) |
                        slidingAttacks(a, 0, ROOK_DELTAS) &
                        slidingAttacks(b, 0, ROOK_DELTAS);
                } else if (Bitboard.contains(slidingAttacks(a, 0, BISHOP_DELTAS), b) ) {
                    BETWEEN[a][b] =
                        slidingAttacks(a, 1L << b, BISHOP_DELTAS) &
                        slidingAttacks(b, 1L << a, BISHOP_DELTAS);
                    RAYS[a][b] =
                        (1L << a) | (1L << b) |
                        slidingAttacks(a, 0, BISHOP_DELTAS) &
                        slidingAttacks(b, 0, BISHOP_DELTAS);
                }
            }
        }
    }

    public static long bishopAttacks(int square, long occupied) {
        Magic magic = Magic.BISHOP[square];
        return ATTACKS[((int) (magic.factor * (occupied & magic.mask) >>> (64 - 9)) + magic.offset)];
    }

    public static long rookAttacks(int square, long occupied) {
        Magic magic = Magic.ROOK[square];
        return ATTACKS[((int) (magic.factor * (occupied & magic.mask) >>> (64 - 12)) + magic.offset)];
    }

    public static long queenAttacks(int square, long occupied) {
        return bishopAttacks(square, occupied) ^ rookAttacks(square, occupied);
    }

    public static long pawnAttacks(boolean white, int square) {
        return (white ? WHITE_PAWN_ATTACKS : BLACK_PAWN_ATTACKS)[square];
    }

    static private final int[] LSB_TABLE = {
         0,  1, 48,  2, 57, 49, 28,  3,
        61, 58, 50, 42, 38, 29, 17,  4,
        62, 55, 59, 36, 53, 51, 43, 22,
        45, 39, 33, 30, 24, 18, 12,  5,
        63, 47, 56, 27, 60, 41, 37, 16,
        54, 35, 52, 21, 44, 32, 23, 11,
        46, 26, 40, 15, 34, 20, 31, 10,
        25, 14, 19,  9, 13,  8,  7,  6,
    };

    public static int lsb(long b) {
        assert b != 0;

        // De-Bruijn multiplication to extract the least significant bit.
        return LSB_TABLE[(int)(((b & -b) * 0x03f79d71b4cb0a89L) >>> 58)];
    }

    public static int msb(long b) {
        assert b != 0;

        // Floating point trick by Gerd Isenberg.
        double x = (double) (b & ~(b >>> 32));
        int exp = (int) (Double.doubleToLongBits(x) >> 52);
        int sign = (exp >> 11) & 63; // 63 if < 0 else 0
        exp = (exp & 2047) - 1023;
        return exp | sign;
    }

    public static boolean moreThanOne(long b) {
        return (b & (b - 1L)) != 0;
    }

    public static boolean contains(long b, int sq) {
        return (b & (1L << sq)) != 0;
    }
}
