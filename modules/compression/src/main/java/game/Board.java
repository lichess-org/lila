package org.lishogi.compression.game;

import java.util.Map;
import java.util.HashMap;

final class Board {
    long pawns;
    long knights;
    long bishops;
    long rooks;
    long queens;
    long kings;

    long white;
    long black;
    long occupied;

    boolean turn;
    int epSquare;
    long castlingRights;

    int incrementalHash;

    public Board() {
        this.pawns = 0xff00000000ff00L;
        this.knights = 0x4200000000000042L;
        this.bishops = 0x2400000000000024L;
        this.rooks = 0x8100000000000081L;
        this.queens = 0x800000000000008L;
        this.kings = 0x1000000000000010L;

        this.white = 0xffffL;
        this.black = 0xffff000000000000L;
        this.occupied = 0xffff00000000ffffL;

        this.turn = true;
        this.epSquare = 0;
        this.castlingRights = this.rooks;

        this.incrementalHash = ZobristHash.hashPieces(this) ^ ZobristHash.hashTurn(this);
    }

    public Board(Board board) {
        this.pawns = board.pawns;
        this.knights = board.knights;
        this.bishops = board.bishops;
        this.rooks = board.rooks;
        this.queens = board.queens;
        this.kings = board.kings;

        this.white = board.white;
        this.black = board.black;
        this.occupied = board.occupied;

        this.turn = board.turn;
        this.epSquare = board.epSquare;
        this.castlingRights = board.castlingRights;

        this.incrementalHash = ZobristHash.hashPieces(this) ^ ZobristHash.hashTurn(this);
    }

    Board(long pawns, long knights, long bishops, long rooks, long queens, long kings,
          long white, long black,
          boolean turn, int epSquare, long castlingRights) {

        this.pawns = pawns;
        this.knights = knights;
        this.bishops = bishops;
        this.rooks = rooks;
        this.queens = queens;
        this.kings = kings;

        this.white = white;
        this.black = black;
        this.occupied = white | black;

        this.turn = turn;
        this.epSquare = epSquare;
        this.castlingRights = castlingRights;

        this.incrementalHash = ZobristHash.hashPieces(this) ^ ZobristHash.hashTurn(this);
    }

    private boolean isOccupied(int square) {
        return Bitboard.contains(this.occupied, square);
    }

    private void discard(int square) {
        if (!isOccupied(square)) return;
        Role role = roleAt(square);
        long mask = 1L << square;

        switch (role) {
            case PAWN: this.pawns ^= mask; break;
            case KNIGHT: this.knights ^= mask; break;
            case BISHOP: this.bishops ^= mask; break;
            case ROOK: this.rooks ^= mask; break;
            case QUEEN: this.queens ^= mask; break;
            case KING: this.kings ^= mask; break;
        }

        boolean color = whiteAt(square);
        if (color) this.white ^= mask;
        else this.black ^= mask;

        this.occupied ^= mask;
        this.incrementalHash ^= ZobristHash.hashPiece(square, color, role);
    }

    private void put(int square, boolean color, Role role) {
        discard(square);

        long mask = 1L << square;

        switch (role) {
            case PAWN: this.pawns ^= mask; break;
            case KNIGHT: this.knights ^= mask; break;
            case BISHOP: this.bishops ^= mask; break;
            case ROOK: this.rooks ^= mask; break;
            case QUEEN: this.queens ^= mask; break;
            case KING: this.kings ^= mask; break;
        }

        if (color) this.white ^= mask;
        else this.black ^= mask;

        this.occupied ^= mask;
        this.incrementalHash ^= ZobristHash.hashPiece(square, color, role);
    }

    public Role roleAt(int square) {
        if (Bitboard.contains(this.pawns, square)) return Role.PAWN;
        if (Bitboard.contains(this.knights, square)) return Role.KNIGHT;
        if (Bitboard.contains(this.bishops, square)) return Role.BISHOP;
        if (Bitboard.contains(this.rooks, square)) return Role.ROOK;
        if (Bitboard.contains(this.queens, square)) return Role.QUEEN;
        if (Bitboard.contains(this.kings, square)) return Role.KING;
        return null;
    }

    public boolean whiteAt(int square) {
        return Bitboard.contains(this.white, square);
    }

    public int zobristHash() {
        return this.incrementalHash ^ ZobristHash.hashCastling(this) ^ ZobristHash.hashEnPassant(this);
    }

    public Map<Integer, Piece> pieceMap() {
        HashMap<Integer, Piece> map = new HashMap<Integer, Piece>();
        long occupied = this.occupied;
        while (occupied != 0) {
            int sq = Bitboard.lsb(occupied);
            map.put(sq, new Piece(whiteAt(sq), roleAt(sq)));
            occupied &= occupied - 1L;
        }
        return map;
    }

    public void play(Move move) {
        this.epSquare = 0;

        switch (move.type) {
            case Move.NORMAL:
                if (move.role == Role.PAWN && Math.abs(move.from - move.to) == 16) {
                    long theirPawns = them() & this.pawns;
                    if (theirPawns != 0) {
                        int sq = move.from + (this.turn ? 8 : -8);
                        if ((Bitboard.pawnAttacks(this.turn, sq) & theirPawns) != 0) {
                            this.epSquare = sq;
                        }
                    }
                }

                if (this.castlingRights != 0) {
                    if (move.role == Role.KING) {
                        this.castlingRights &= Bitboard.RANKS[this.turn ? 7 : 0];
                    } else if (move.role == Role.ROOK) {
                        this.castlingRights &= ~(1L << move.from);
                    }

                    if (move.capture) {
                        this.castlingRights &= ~(1L << move.to);
                    }
                }

                discard(move.from);
                put(move.to, this.turn, move.promotion != null ? move.promotion : move.role);
                break;

            case Move.CASTLING:
                this.castlingRights &= Bitboard.RANKS[this.turn ? 7 : 0];
                int rookTo = Square.combine(move.to < move.from ? Square.D1 : Square.F1, move.to);
                int kingTo = Square.combine(move.to < move.from ? Square.C1 : Square.G1, move.from);
                discard(move.from);
                discard(move.to);
                put(rookTo, this.turn, Role.ROOK);
                put(kingTo, this.turn, Role.KING);
                break;

            case Move.EN_PASSANT:
                discard(Square.combine(move.to, move.from));
                discard(move.from);
                put(move.to, this.turn, Role.PAWN);
                break;
        }

        this.turn = !this.turn;
        this.incrementalHash ^= ZobristHash.POLYGLOT[780];
    }

    long us() {
        return byColor(this.turn);
    }

    long them() {
        return byColor(!this.turn);
    }

    long byColor(boolean white) {
        return white ? this.white : this.black;
    }

    private int king(boolean white) {
        return Bitboard.lsb(this.kings & byColor(white));
    }

    private long sliderBlockers(int king) {
        long snipers = them() & (
            Bitboard.rookAttacks(king, 0) & (this.rooks ^ this.queens) |
            Bitboard.bishopAttacks(king, 0) & (this.bishops ^ this.queens));

        long blockers = 0;

        while (snipers != 0) {
            int sniper = Bitboard.lsb(snipers);
            long between = Bitboard.BETWEEN[king][sniper] & this.occupied;
            if (!Bitboard.moreThanOne(between)) blockers |= between;
            snipers &= snipers - 1L;
        }

        return blockers;
    }

    public boolean isCheck() {
        return attacksTo(king(this.turn), !this.turn) != 0;
    }

    private long attacksTo(int sq, boolean attacker) {
        return attacksTo(sq, attacker, this.occupied);
    }

    private long attacksTo(int sq, boolean attacker, long occupied) {
        return byColor(attacker) & (
            Bitboard.rookAttacks(sq, occupied) & (this.rooks ^ this.queens) |
            Bitboard.bishopAttacks(sq, occupied) & (this.bishops ^ this.queens) |
            Bitboard.KNIGHT_ATTACKS[sq] & this.knights |
            Bitboard.KING_ATTACKS[sq] & this.kings |
            Bitboard.pawnAttacks(!attacker, sq) & this.pawns);
    }

    public void legalMoves(MoveList moves) {
        moves.clear();

        if (this.epSquare != 0) {
            genEnPassant(moves);
        }

        int king = king(this.turn);
        long checkers = attacksTo(king, !this.turn);
        if (checkers == 0) {
            long target = ~us();
            genNonKing(target, moves);
            genSafeKing(king, target, moves);
            genCastling(king, moves);
        } else {
            genEvasions(king, checkers, moves);
        }

        long blockers = sliderBlockers(king);
        if (blockers != 0 || this.epSquare != 0) {
            moves.retain(m -> isSafe(king, m, blockers));
        }
    }

    public boolean hasLegalEnPassant() {
        // Like legalMoves(), but generate only en passant captures to see if
        // there are any legal en passant moves in the position.

        if (this.epSquare == 0) return false; // shortcut

        MoveList moves = new MoveList(2);
        genEnPassant(moves);

        int king = king(this.turn);
        long blockers = sliderBlockers(king);
        return moves.anyMatch(m -> isSafe(king, m, blockers));
    }

    private void genNonKing(long mask, MoveList moves) {
        genPawn(mask, moves);

        // Knights.
        long knights = us() & this.knights;
        while (knights != 0) {
            int from = Bitboard.lsb(knights);
            long targets = Bitboard.KNIGHT_ATTACKS[from] & mask;
            while (targets != 0) {
                int to = Bitboard.lsb(targets);
                moves.pushNormal(this, Role.KNIGHT, from, isOccupied(to), to);
                targets &= targets - 1L;
            }
            knights &= knights - 1L;
        }

        // Bishops.
        long bishops = us() & this.bishops;
        while (bishops != 0) {
            int from = Bitboard.lsb(bishops);
            long targets = Bitboard.bishopAttacks(from, this.occupied) & mask;
            while (targets != 0) {
                int to = Bitboard.lsb(targets);
                moves.pushNormal(this, Role.BISHOP, from, isOccupied(to), to);
                targets &= targets - 1L;
            }
            bishops &= bishops - 1L;
        }

        // Rooks.
        long rooks = us() & this.rooks;
        while (rooks != 0) {
            int from = Bitboard.lsb(rooks);
            long targets = Bitboard.rookAttacks(from, this.occupied) & mask;
            while (targets != 0) {
                int to = Bitboard.lsb(targets);
                moves.pushNormal(this, Role.ROOK, from, isOccupied(to), to);
                targets &= targets - 1L;
            }
            rooks &= rooks - 1L;
        }

        // Queens.
        long queens = us() & this.queens;
        while (queens != 0) {
            int from = Bitboard.lsb(queens);
            long targets = Bitboard.queenAttacks(from, this.occupied) & mask;
            while (targets != 0) {
                int to = Bitboard.lsb(targets);
                moves.pushNormal(this, Role.QUEEN, from, isOccupied(to), to);
                targets &= targets - 1L;
            }
            queens &= queens - 1L;
        }
    }

    private void genSafeKing(int king, long mask, MoveList moves) {
        long targets = Bitboard.KING_ATTACKS[king] & mask;
        while (targets != 0) {
            int to = Bitboard.lsb(targets);
            if (attacksTo(to, !this.turn) == 0) {
                moves.pushNormal(this, Role.KING, king, isOccupied(to), to);
            }
            targets &= targets - 1L;
        }
    }

    private void genEvasions(int king, long checkers, MoveList moves) {
        // Checks by these sliding pieces can maybe be blocked.
        long sliders = checkers & (this.bishops ^ this.rooks ^ this.queens);

        // Collect attacked squares that the king can not escape to.
        long attacked = 0;
        while (sliders != 0) {
            int slider = Bitboard.lsb(sliders);
            attacked |= Bitboard.RAYS[king][slider] ^ (1L << slider);
            sliders &= sliders - 1L;
        }

        genSafeKing(king, ~us() & ~attacked, moves);

        if (checkers != 0 && !Bitboard.moreThanOne(checkers)) {
            int checker = Bitboard.lsb(checkers);
            long target = Bitboard.BETWEEN[king][checker] | checkers;
            genNonKing(target, moves);
        }
    }

    private void genPawn(long mask, MoveList moves) {
        // Pawn captures (except en passant).
        long capturers = us() & this.pawns;
        while (capturers != 0) {
            int from = Bitboard.lsb(capturers);
            long targets = Bitboard.pawnAttacks(this.turn, from) & them() & mask;
            while (targets != 0) {
                int to = Bitboard.lsb(targets);
                addPawnMoves(from, true, to, moves);
                targets &= targets - 1L;
            }
            capturers &= capturers - 1L;
        }

        // Normal pawn moves.
        long singleMoves =
            ~this.occupied & (this.turn ?
                ((this.white & this.pawns) << 8) :
                ((this.black & this.pawns) >>> 8));

        long doubleMoves =
            ~this.occupied &
            (this.turn ? (singleMoves << 8) : (singleMoves >>> 8)) &
            Bitboard.RANKS[this.turn ? 3 : 4];

        singleMoves &= mask;
        doubleMoves &= mask;

        while (singleMoves != 0) {
            int to = Bitboard.lsb(singleMoves);
            int from = to + (this.turn ? -8 : 8);
            addPawnMoves(from, false, to, moves);
            singleMoves &= singleMoves - 1L;
        }

        while (doubleMoves != 0) {
            int to = Bitboard.lsb(doubleMoves);
            int from = to + (this.turn ? -16: 16);
            moves.pushNormal(this, Role.PAWN, from, false, to);
            doubleMoves &= doubleMoves - 1L;
        }
    }

    private void addPawnMoves(int from, boolean capture, int to, MoveList moves) {
        if (Square.rank(to) == (this.turn ? 7 : 0)) {
            moves.pushPromotion(this, from, capture, to, Role.QUEEN);
            moves.pushPromotion(this, from, capture, to, Role.KNIGHT);
            moves.pushPromotion(this, from, capture, to, Role.ROOK);
            moves.pushPromotion(this, from, capture, to, Role.BISHOP);
        } else {
            moves.pushNormal(this, Role.PAWN, from, capture, to);
        }
    }

    private void genEnPassant(MoveList moves) {
        long pawns = us() & this.pawns & Bitboard.pawnAttacks(!this.turn, this.epSquare);
        while (pawns != 0) {
            int pawn = Bitboard.lsb(pawns);
            moves.pushEnPassant(this, pawn, this.epSquare);
            pawns &= pawns - 1L;
        }
    }

    private void genCastling(int king, MoveList moves) {
        long rooks = this.castlingRights & Bitboard.RANKS[this.turn ? 0 : 7];
        while (rooks != 0) {
            int rook = Bitboard.lsb(rooks);
            long path = Bitboard.BETWEEN[king][rook];
            if ((path & this.occupied) == 0) {
                int kingTo = Square.combine(rook < king ? Square.C1 : Square.G1, king);
                long kingPath = Bitboard.BETWEEN[king][kingTo] | (1L << kingTo) | (1L << king);
                while (kingPath != 0) {
                    int sq = Bitboard.lsb(kingPath);
                    if (attacksTo(sq, !this.turn, this.occupied ^ (1L << king)) != 0) {
                        break;
                    }
                    kingPath &= kingPath - 1L;
                }
                if (kingPath == 0) moves.pushCastle(this, king, rook);
            }
            rooks &= rooks - 1L;
        }
    }

    // Used for filtering candidate moves that would leave/put the king
    // in check.
    private boolean isSafe(int king, Move move, long blockers) {
        switch (move.type) {
            case Move.NORMAL:
                return
                    !Bitboard.contains(us() & blockers, move.from) ||
                    Square.aligned(move.from, move.to, king);

            case Move.EN_PASSANT:
                long occupied = this.occupied;
                occupied ^= (1L << move.from);
                occupied ^= (1L << Square.combine(move.to, move.from)); // captured pawn
                occupied |= (1L << move.to);
                return
                    (Bitboard.rookAttacks(king, occupied) & them() & (this.rooks ^ this.queens)) == 0 &&
                    (Bitboard.bishopAttacks(king, occupied) & them() & (this.bishops ^ this.queens)) == 0;

            default:
                return true;
        }
    }
}
