package org.lichess.compression.game;

import java.util.Comparator;

public class MoveComparator implements Comparator<Move> {
    @Override
    public int compare(Move a, Move b) {
        int cmpFrom = Integer.compare(a.from, b.from);
        if (cmpFrom != 0) return cmpFrom;

        int cmpTo = Integer.compare(a.to, b.to);
        if (cmpTo != 0) return cmpTo;

        if (a.promotion == null && b.promotion == null) return 0;
        if (a.promotion == null && b.promotion != null) return -1;
        if (a.promotion != null && b.promotion == null) return 1;
        return Integer.compare(a.promotion.index, b.promotion.index);
    }
}
