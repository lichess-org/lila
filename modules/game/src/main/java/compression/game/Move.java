package org.lichess.compression.game;

class Move {
    public static final int NORMAL = 0;
    public static final int EN_PASSANT = 1;
    public static final int CASTLING = 2;

    public int type;
    public Role role;
    public int from;
    public boolean capture;
    public int to;
    public Role promotion;

    public Move(Role role, int from, boolean capture, int to) {
        assert role != null;
        assert from != to;

        this.type = NORMAL;
        this.role = role;
        this.from = from;
        this.capture = capture;
        this.to = to;
    }

    public Move(Role role, int from, boolean capture, int to, Role promotion) {
        this(role, from, capture, to);
        this.promotion = promotion;
    }

    public static Move castle(int king, int rook) {
        Move move = new Move(Role.KING, king, false, rook);
        move.type = CASTLING;
        return move;
    }

    public static Move enPassant(int capturer, int to) {
        Move move = new Move(Role.PAWN, capturer, true, to);
        move.type = EN_PASSANT;
        return move;
    }
}
