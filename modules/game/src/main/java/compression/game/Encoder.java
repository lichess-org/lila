package org.lichess.compression.game;

import java.util.ArrayList;

import org.lichess.compression.BitReader;
import org.lichess.compression.BitWriter;
import org.lichess.compression.VarIntEncoder;

public class Encoder {
    static byte[] encode(Move line[]) {
        ArrayList<Move> legals = new ArrayList<Move>(255);
        BitWriter writer = new BitWriter();
        Board board = new Board();

        VarIntEncoder.writeUnsigned(line.length, writer);

        for (int i = 0; i < line.length; i++) {
            board.legalMoves(legals);
            legals.sort(new MoveComparator());

            int code = -1;
            for (int j = 0; j < legals.size(); j++) {
                Move legal = legals.get(j);
                if (legal.from == line[i].from && legal.to == line[i].to && legal.promotion == line[i].promotion) {
                    code = j;
                    break;
                }
            }

            assert code != -1;

            Huffman.write(code, writer);

            board.play(legals.get(code));
        }

        return writer.toArray();
    }

    public static String decodePgn(byte input[]) {
        ArrayList<Move> legals = new ArrayList<Move>(255);
        StringBuilder output = new StringBuilder();
        Board board = new Board();

        BitReader reader = new BitReader(input);
        int numMoves = VarIntEncoder.readUnsigned(reader);

        for (int i = 0; i < numMoves + 1; i++) {
            board.legalMoves(legals);

            if (i > 0) {
                if (legals.isEmpty() && board.isCheck()) output.append('#');
                else if (board.isCheck()) output.append('+');
            }

            if (i < input.length) {
                if (i > 0) output.append(' ');
                legals.sort(new MoveComparator());
                Move move = legals.get(Huffman.read(reader));
                output.append(san(move, legals));
                board.play(move);
            }
        }

        return output.toString();
    }

    static String san(Move move, ArrayList<Move> legals) {
        StringBuilder builder;

        switch (move.type) {
            case Move.NORMAL:
                builder = new StringBuilder(6);
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

            case Move.EN_PASSANT:
                builder = new StringBuilder(4);
                builder.append((char) (Square.file(move.from) + 'a'));
                builder.append('x');
                builder.append((char) (Square.file(move.to) + 'a'));
                builder.append((char) (Square.rank(move.to) + '1'));
                return builder.toString();

            case Move.CASTLING:
                return move.from < move.to ? "O-O" : "O-O-O";
        }

        return "--";
    }
}
