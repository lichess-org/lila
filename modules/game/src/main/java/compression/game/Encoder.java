package org.lichess.compression.game;

import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.lichess.compression.BitReader;
import org.lichess.compression.BitWriter;

public class Encoder {
    public static byte[] encode(String pgnMoves[]) {
        BitWriter writer = new BitWriter();

        Board board = new Board();
        ArrayList<Move> legals = new ArrayList<Move>(255);

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

                if (matcher.group(1) == null) role = Role.PAWN;
                else if (matcher.group(1).equals("N")) role = Role.KNIGHT;
                else if (matcher.group(1).equals("B")) role = Role.BISHOP;
                else if (matcher.group(1).equals("R")) role = Role.ROOK;
                else if (matcher.group(1).equals("Q")) role = Role.QUEEN;
                else if (matcher.group(1).equals("K")) role = Role.KING;

                if (matcher.group(2) != null) from &= Bitboard.FILES[matcher.group(2).charAt(0) - 'a'];
                if (matcher.group(3) != null) from &= Bitboard.RANKS[matcher.group(3).charAt(0) - '1'];

                to = (matcher.group(4).charAt(0) - 'a') ^ ((matcher.group(4).charAt(1) - '1') << 3);

                if (matcher.group(5) != null) {
                    if (matcher.group(5).endsWith("Q")) promotion = Role.QUEEN;
                    else if (matcher.group(5).endsWith("R")) promotion = Role.ROOK;
                    else if (matcher.group(5).endsWith("B")) promotion = Role.BISHOP;
                    else if (matcher.group(5).endsWith("N")) promotion = Role.KNIGHT;
                    else if (matcher.group(5).endsWith("K")) promotion = Role.KING;
                }
            }

            // Find index in legal moves.
            board.legalMoves(legals);
            legals.sort(null);

            int code = -1;

            for (int i = 0; i < legals.size(); i++) {
                Move legal = legals.get(i);
                if (legal.role == role && legal.to == to && legal.promotion == promotion && Bitboard.contains(from, legal.from)) {
                    if (code == -1) code = i;
                    else return null;
                }
            }

            if (code == -1) return null;

            // Encode and play.
            Huffman.write(code, writer);
            board.play(legals.get(code));
        }

        return writer.toArray();
    }

    public static String[] decode(byte input[], int plies) {
        BitReader reader = new BitReader(input);

        String output[] = new String[plies];

        Board board = new Board();
        ArrayList<Move> legals = new ArrayList<Move>(255);

        for (int i = 0; i < plies + 1; i++) {
            board.legalMoves(legals);

            if (i > 0) {
                if (board.isCheck()) output[i - 1] += (legals.isEmpty() ? "#" : "+");
            }

            if (i < plies) {
                legals.sort(null);
                Move move = legals.get(Huffman.read(reader));
                output[i] = san(move, legals);
                board.play(move);
            }
        }

        return output;
    }

    private static Pattern SAN_PATTERN = Pattern.compile("^([NBKRQ])?([a-h])?([1-8])?[x-]?([a-h][1-8])(=?[NBRQK])?[\\+#]?$");

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
}
