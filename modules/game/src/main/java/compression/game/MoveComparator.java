package org.lichess.compression.game;

import java.util.Comparator;

public class MoveComparator implements Comparator<Move> {
    @Override
    public int compare(Move a, Move b) {
        if (a.promotion != b.promotion) {
            if (a.promotion == null) return 1;
            else if (b.promotion == null) return -1;
            else return Integer.compare(b.promotion.index, a.promotion.index);
        }

        if (a.capture != b.capture) {
            if (a.capture) return -1;
            else return 1;
        }

        int cmpFrom = Integer.compare(a.from, b.from);
        if (cmpFrom != 0) return cmpFrom;

        return Integer.compare(a.to, b.to);
    }
}
