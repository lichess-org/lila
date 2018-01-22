package org.lichess.compression.game;

import java.util.Arrays;
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

    public static class DecodeResult {
        public final String pgnMoves[];
        public final Map<Integer, Piece> pieces;
        public final Set<Integer> unmovedRooks;
        public final byte positionHashes[];
        public final String lastUci;

        public DecodeResult(String pgnMoves[], Map<Integer, Piece> pieces, Set<Integer> unmovedRooks, byte positionHashes[], String lastUci) {
            this.pgnMoves = pgnMoves;
            this.pieces = pieces;
            this.unmovedRooks = unmovedRooks;
            this.positionHashes = positionHashes;
            this.lastUci = lastUci;
        }
    }

    public static DecodeResult decode(byte input[], int plies) {
        BitReader reader = new BitReader(input);

        String output[] = new String[plies];

        Board board = new Board();
        ArrayList<Move> legals = new ArrayList<Move>(80);

        String lastUci = null;

        // Collect the position hashes (3 bytes each) since the last capture
        // or pawn move.
        ByteBuffer positionHashes = ByteBuffer.allocate(3 * (plies + 1));
        appendHash(positionHashes, board.zobristHash());

        for (int i = 0; i <= plies; i++) {
            board.legalMoves(legals);

            // Append check or checkmate suffix to previous move.
            if (i > 0) {
                if (board.isCheck()) output[i - 1] += (legals.isEmpty() ? "#" : "+");
            }

            // Decode and play next move.
            if (i < plies) {
                legals.sort(null);
                Move move = legals.get(Huffman.read(reader));
                output[i] = san(move, legals);
                board.play(move);

                if (move.isZeroing()) positionHashes.clear();
                appendHash(positionHashes, board.zobristHash());

                if (i + 1 == plies) lastUci = move.uci();
            }
        }

        positionHashes.flip();

        return new DecodeResult(
            output,
            board.pieceMap(),
            Bitboard.squareSet(board.castlingRights),
            Arrays.copyOf(positionHashes.array(), positionHashes.limit()),
            lastUci);
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
        buffer.put((byte) (hash >>> 16));
        buffer.put((byte) (hash >>> 8));
        buffer.put((byte) hash);
    }
}
