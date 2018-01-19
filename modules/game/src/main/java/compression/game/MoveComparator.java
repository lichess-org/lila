package org.lichess.compression.game;

import java.util.Comparator;

public class MoveComparator implements Comparator<Move> {

    private final Board board;

    public MoveComparator(Board board) {
        this.board = board;
    }

    @Override
    public int compare(Move a, Move b) {
        // Promotions (of higher value) first.
        if (a.promotion != b.promotion) {
            if (a.promotion == null) return 1;
            else if (b.promotion == null) return -1;
            else return Integer.compare(b.promotion.index, a.promotion.index);
        }

        // Then captures.
        if (a.capture != b.capture) {
            if (a.capture) return -1;
            else return 1;
        }

        // Then positional value of the move.
        int cmpMoveValue = Integer.compare(moveValue(b), moveValue(a));
        if (cmpMoveValue != 0) return cmpMoveValue;

        // Disambiguate.
        if (a.to != b.to) return Integer.compare(a.to, b.to);
        return Integer.compare(a.from, b.from);
    }

    private int moveValue(Move move) {
        return pieceValue(move.role, move.to) - pieceValue(move.role, move.from);
    }

    private int pieceValue(Role role, int square) {
        if (this.board.turn) square ^= 0x38; // mirror
        return PSQT[role.index][square];
    }

    // Piece-Square table with some manual tweaks (breaking symmetry).
    //
    // Original table taken from:
    // https://github.com/flok99/feeks/blob/f02e4897555ac08497a5fea43f241bad30f2ecff/psq.py#L8-L67

    private static int PSQT[][] = {
        {   0,  0,  0,  0,  0,  0,  0,  0,
           50, 50, 50, 50, 50, 50, 50, 50,
           10, 10, 20, 30, 30, 20, 10, 10,
            5,  5, 10, 25, 25, 10,  5,  5,
            0,  0,  0, 20, 21,  0,  0,  0,
            5, -5,-10,  0,  0,-10, -5,  5,
            5, 10, 10,-31,-31, 10, 10,  5,
            0,  0,  0,  0,  0,  0,  0,  0 },

        { -50,-40,-30,-30,-30,-30,-40,-50,
          -40,-20,  0,  0,  0,  0,-20,-40,
          -30,  0, 10, 15, 15, 10,  0,-30,
          -30,  5, 15, 20, 20, 15,  5,-30,
          -30,  0, 15, 20, 20, 15,  0,-30,
          -30,  5, 10, 15, 15, 11,  5,-30,
          -40,-20,  0,  5,  5,  0,-20,-40,
          -50,-40,-30,-30,-30,-30,-40,-50 },

        { -20,-10,-10,-10,-10,-10,-10,-20,
          -10,  0,  0,  0,  0,  0,  0,-10,
          -10,  0,  5, 10, 10,  5,  0,-10,
          -10,  5,  5, 10, 10,  5,  5,-10,
          -10,  0, 10, 10, 10, 10,  0,-10,
          -10, 10, 10, 10, 10, 10, 10,-10,
          -10,  5,  0,  0,  0,  0,  5,-10,
          -20,-10,-10,-10,-10,-10,-10,-20 },

        {   0,  0,  0,  0,  0,  0,  0,  0,
            5, 10, 10, 10, 10, 10, 10,  5,
           -5,  0,  0,  0,  0,  0,  0, -5,
           -5,  0,  0,  0,  0,  0,  0, -5,
           -5,  0,  0,  0,  0,  0,  0, -5,
           -5,  0,  0,  0,  0,  0,  0, -5,
           -5,  0,  0,  0,  0,  0,  0, -5,
            0,  0,  0,  5,  5,  0,  0,  0 },

        { -20,-10,-10, -5, -5,-10,-10,-20,
          -10,  0,  0,  0,  0,  0,  0,-10,
          -10,  0,  5,  5,  5,  5,  0,-10,
           -5,  0,  5,  5,  5,  5,  0, -5,
            0,  0,  5,  5,  5,  5,  0, -5,
          -10,  5,  5,  5,  5,  5,  0,-10,
          -10,  0,  5,  0,  0,  0,  0,-10,
          -20,-10,-10, -5, -5,-10,-10,-20 },

        { -30,-40,-40,-50,-50,-40,-40,-30,
          -30,-40,-40,-50,-50,-40,-40,-30,
          -30,-40,-40,-50,-50,-40,-40,-30,
          -30,-40,-40,-50,-50,-40,-40,-30,
          -20,-30,-30,-40,-40,-30,-30,-20,
          -10,-20,-20,-20,-20,-20,-20,-10,
           20, 20,  0,  0,  0,  0, 20, 20,
            0, 30, 10,  0,  0, 10, 30,  0 }
    };
}
