package org.lichess.compression.game;

import java.util.ArrayList;

import org.lichess.compression.BitReader;
import org.lichess.compression.BitWriter;
import org.lichess.compression.VarIntEncoder;

public class Encoder {
    public static byte[] encode(String pgnMoves[]) {
        BitWriter writer = new BitWriter();

        Board board = new Board();
        ArrayList<Move> legals = new ArrayList<Move>(255);

        VarIntEncoder.writeUnsigned(pgnMoves.length, writer);

        for (String pgnMove: pgnMoves) {
            board.legalMoves(legals);
            legals.sort(new MoveComparator());

            for (int code = 0; code < legals.size(); code++) {
                Move legal = legals.get(code);
                // TODO: Optimize SAN parsing
                if (san(legal, legals).equals(pgnMove.replace("+", "").replace("#", ""))) {
                    Huffman.write(code, writer);
                    board.play(legal);
                }
            }
        }

        return writer.toArray();
    }

    public static ArrayList<String> decode(byte input[]) {
        BitReader reader = new BitReader(input);

        int length = VarIntEncoder.readUnsigned(reader);
        ArrayList<String> output = new ArrayList<String>(length);

        Board board = new Board();
        ArrayList<Move> legals = new ArrayList<Move>(255);

        for (int i = 0; i < length + 1; i++) {
            board.legalMoves(legals);

            if (i > 0) {
                if (board.isCheck()) output.set(i - 1, output.get(i - 1) + (legals.isEmpty() ? "#" : "+"));
            }

            if (i < length) {
                legals.sort(new MoveComparator());
                Move move = legals.get(Huffman.read(reader));
                output.add(san(move, legals));
                board.play(move);
            }
        }

        return output;
    }

    private static String san(Move move, ArrayList<Move> legals) {
        switch (move.type) {
            case Move.NORMAL:
            case Move.EN_PASSANT:
                StringBuilder builder = new StringBuilder(6);
                builder.append(move.role.symbol);

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

                if (move.capture) builder.append('x');

                builder.append((char) (Square.file(move.to) + 'a'));
                builder.append((char) (Square.rank(move.to) + '1'));

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
