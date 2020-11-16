package org.lishogi.compression.game;

import java.util.Set;
import java.util.HashSet;

class Bitboard {
    public static final long ALL = -1L;

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

    public static final long BETWEEN[][] = new long[64][64];
    public static final long RAYS[][] = new long[64][64];

    // Large overlapping attack table indexed using magic multiplication.
    private static final long ATTACKS[] = new long[88772];

    // Slow attack set generation. Used only to bootstrap the attack tables.
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

    public static int lsb(long b) {
        assert b != 0;
        return Long.numberOfTrailingZeros(b);
    }

    public static int msb(long b) {
        assert b != 0;
        return 63 - Long.numberOfLeadingZeros(b);
    }

    public static boolean moreThanOne(long b) {
        return (b & (b - 1L)) != 0;
    }

    public static boolean contains(long b, int sq) {
        return (b & (1L << sq)) != 0;
    }

    public static Set<Integer> squareSet(long b) {
        HashSet<Integer> set = new HashSet<Integer>();
        while (b != 0) {
            int sq = lsb(b);
            set.add(sq);
            b &= b - 1L;
        }
        return set;
    }
}
