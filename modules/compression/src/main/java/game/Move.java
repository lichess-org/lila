package org.lishogi.compression.game;

final class Move implements Comparable<Move> {
    public static final int NORMAL = 0;
    public static final int EN_PASSANT = 1;
    public static final int CASTLING = 2;

    public int type;
    public Role role;
    public int from;
    public boolean capture;
    public int to;
    public Role promotion;

    private int score;

    void set(Board board, int type, Role role, int from, boolean capture, int to, Role promotion) {
        // Overwrite the current move. This is like a constructor, but reuses
        // an existing object.

        this.type = type;
        this.role = role;
        this.from = from;
        this.capture = capture;
        this.to = to;
        this.promotion = promotion;

        // Assign higher scores to moves that are more likely to be played.
        //
        // Scores must be unique for every move in the position, because
        // move ordering should never depend on implementation details of the
        // generator.

        long defendingPawns =
            Bitboard.pawnAttacks(board.turn, to) &
            board.pawns &
            board.them();

        int moveValue = pieceValue(board, role, to) - pieceValue(board, role, from);

        this.score =
            (promotion == null ? 0 : promotion.index << 26) +
            (capture ? 1 << 25 : 0) +
            ((defendingPawns == 0 ? 6 : (5 - role.index)) << 22) +
            (512 + moveValue << 12) +
            (to << 6) +
            from;
    }

    public int compareTo(Move other) {
        return other.score - this.score;
    }

    public String uci() {
        int to = this.to;

        // Select the king target square instead.
        if (this.type == CASTLING) {
            to = Square.combine(this.to < this.from ? Square.C1 : Square.G1, this.from);
        }

        StringBuilder builder = new StringBuilder(this.promotion == null ? 4 : 5);
        builder.append((char) (Square.file(this.from) + 'a'));
        builder.append((char) (Square.rank(this.from) + '1'));
        builder.append((char) (Square.file(to) + 'a'));
        builder.append((char) (Square.rank(to) + '1'));
        if (this.promotion != null) builder.append(this.promotion.symbol.toLowerCase());
        return builder.toString();
    }

    public boolean isZeroing() {
        return this.capture || this.role == Role.PAWN;
    }

    public boolean isIrreversible() {
        return this.isZeroing() || this.type == CASTLING;
    }

    // Piece-Square table with some manual tweaks (breaking symmetry).
    //
    // Original table taken from:
    // https://github.com/flok99/feeks/blob/f02e4897555ac08497a5fea43f241bad30f2ecff/psq.py#L8-L67

    private static int pieceValue(Board board, Role role, int square) {
        return PSQT[role.index][board.turn ? Square.mirror(square) : square];
    }

    private static final int PSQT[][] = {
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
