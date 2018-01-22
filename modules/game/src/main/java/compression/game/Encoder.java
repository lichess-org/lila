package org.lichess.compression.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.nio.ByteBuffer;

import org.lichess.compression.BitReader;
import org.lichess.compression.BitWriter;

public class Encoder {
    private static Pattern SAN_PATTERN = Pattern.compile(
        "([NBKRQ])?([a-h])?([1-8])?x?([a-h][1-8])(?:=([NBRQK]))?[\\+#]?");

    private static Role charToRole(char c) {
        switch (c) {
            case 'N': return Role.KNIGHT;
            case 'B': return Role.BISHOP;
            case 'R': return Role.ROOK;
            case 'Q': return Role.QUEEN;
            case 'K': return Role.KING;
            default: throw new IllegalArgumentException();
        }
    }

    public static byte[] encode(String pgnMoves[]) {
        BitWriter writer = new BitWriter();

        Board board = new Board();
        ArrayList<Move> legals = new ArrayList<Move>(80);

        for (String pgnMove: pgnMoves) {
            // Parse SAN.
            Role role = null, promotion = null;
            long from = Bitboard.ALL;
            int to;

            if (pgnMove.startsWith("O-O-O")) {
                role = Role.KING;
                from = board.kings;
                to = Bitboard.lsb(board.rooks & Bitboard.RANKS[board.turn ? 0 : 7]);
            } else if (pgnMove.startsWith("O-O")) {
                role = Role.KING;
                from = board.kings;
                to = Bitboard.msb(board.rooks & Bitboard.RANKS[board.turn ?  0 : 7]);
            } else {
                Matcher matcher = SAN_PATTERN.matcher(pgnMove);
                if (!matcher.matches()) return null;

                String roleStr = matcher.group(1);
                role = roleStr == null ? Role.PAWN : charToRole(roleStr.charAt(0));

                if (matcher.group(2) != null) from &= Bitboard.FILES[matcher.group(2).charAt(0) - 'a'];
                if (matcher.group(3) != null) from &= Bitboard.RANKS[matcher.group(3).charAt(0) - '1'];

                to = Square.square(matcher.group(4).charAt(0) - 'a', matcher.group(4).charAt(1) - '1');

                if (matcher.group(5) != null) {
                    promotion = charToRole(matcher.group(5).charAt(0));
                }
            }

            // Find index in legal moves.
            board.legalMoves(legals);
            legals.sort(null);

            boolean foundMatch = false;
            int size = legals.size();

            for (int i = 0; i < size; i++) {
                Move legal = legals.get(i);
                if (legal.role == role && legal.to == to && legal.promotion == promotion && Bitboard.contains(from, legal.from)) {
                    if (!foundMatch) {
                        // Encode and play.
                        Huffman.write(i, writer);
                        board.play(legal);
                        foundMatch = true;
                    }
                    else return null;
                }
            }

            if (!foundMatch) return null;
        }

        return writer.toArray();
    }

    public static scala.Tuple4<String[], Map<chess.Pos, chess.Piece>, Set<chess.Pos>, byte[]> decode(byte input[], int plies) {
        BitReader reader = new BitReader(input);

        String output[] = new String[plies];

        Board board = new Board();
        ArrayList<Move> legals = new ArrayList<Move>(80);

        ByteBuffer positionHashBuffer = ByteBuffer.allocate(3 * (plies + 1));
        appendHash(positionHashBuffer, board.zobristHash());

        for (int i = 0; i <= plies; i++) {
            board.legalMoves(legals);

            if (i > 0) {
                if (board.isCheck()) output[i - 1] += (legals.isEmpty() ? "#" : "+");
            }

            if (i < plies) {
                legals.sort(null);
                Move move = legals.get(Huffman.read(reader));
                output[i] = san(move, legals);
                board.play(move);

                if (move.isZeroing()) positionHashBuffer.clear();
                appendHash(positionHashBuffer, board.zobristHash());
            }
        }

        positionHashBuffer.flip();
        byte positionHashes[] = new byte[positionHashBuffer.remaining()];
        positionHashBuffer.get(positionHashes);

        return new scala.Tuple4<String[], Map<chess.Pos, chess.Piece>, Set<chess.Pos>, byte[]>(
            output,
            chessPieceMap(board),
            chessPosSet(board.castlingRights & board.rooks),
            positionHashes);
    }

    private static String san(Move move, ArrayList<Move> legals) {
        switch (move.type) {
            case Move.NORMAL:
            case Move.EN_PASSANT:
                StringBuilder builder = new StringBuilder(6);
                builder.append(move.role.symbol);

                // From.
                if (move.role != Role.PAWN) {
                    boolean file = false, rank = false;
                    long others = 0;

                    for (Move other: legals) {
                        if (other.role == move.role && other.to == move.to && other.from != move.from) {
                            others |= 1L << other.from;
                        }
                    }

                    if (others != 0) {
                        if ((others & Bitboard.RANKS[Square.rank(move.from)]) != 0) file = true;
                        if ((others & Bitboard.FILES[Square.file(move.from)]) != 0) rank = true;
                        else file = true;
                    }

                    if (file) builder.append((char) (Square.file(move.from) + 'a'));
                    if (rank) builder.append((char) (Square.rank(move.from) + '1'));
                } else if (move.capture) {
                    builder.append((char) (Square.file(move.from) + 'a'));
                }

                // Capture.
                if (move.capture) builder.append('x');

                // To.
                builder.append((char) (Square.file(move.to) + 'a'));
                builder.append((char) (Square.rank(move.to) + '1'));

                // Promotion.
                if (move.promotion != null) {
                    builder.append('=');
                    builder.append(move.promotion.symbol);
                }

                return builder.toString();

            case Move.CASTLING:
                return move.from < move.to ? "O-O" : "O-O-O";
        }

        return "--";
    }

    private static void appendHash(ByteBuffer buffer, long hash) {
        buffer.put((byte) (hash & 0xff));
        buffer.put((byte) ((hash >> 8) & 0xff));
        buffer.put((byte) ((hash >> 16) & 0xff));
    }

    private static chess.Role chessRole(Role role) {
        switch (role) {
            case PAWN: return chess.Pawn$.MODULE$;
            case KNIGHT: return chess.Knight$.MODULE$;
            case BISHOP: return chess.Bishop$.MODULE$;
            case ROOK: return chess.Rook$.MODULE$;
            case QUEEN: return chess.Queen$.MODULE$;
            case KING: return chess.King$.MODULE$;
            default: throw new IllegalArgumentException();
        }
    }

    private static chess.Pos chessPos(int square) {
        return chess.Pos.posAt(Square.file(square) + 1, Square.rank(square) + 1).get();
    }

    private static Set<chess.Pos> chessPosSet(long b) {
        HashSet<chess.Pos> set = new HashSet<chess.Pos>();
        while (b != 0) {
            int sq = Bitboard.lsb(b);
            set.add(chessPos(sq));
            b ^= 1L << sq;
        }
        return set;
    }

    private static Map<chess.Pos, chess.Piece> chessPieceMap(Board board) {
        HashMap<chess.Pos, chess.Piece> map = new HashMap<chess.Pos, chess.Piece>();

        long occupied = board.occupied;
        while (occupied != 0) {
            int sq = Bitboard.lsb(occupied);
            chess.Color color = chess.Color$.MODULE$.apply(board.whiteAt(sq));
            chess.Piece piece = new chess.Piece(color, chessRole(board.roleAt(sq)));
            map.put(chessPos(sq), piece);
            occupied ^= 1L << sq;
        }

        return map;
    }
}
