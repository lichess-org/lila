import { useEffect, useRef } from 'react';
import { Chess } from 'chess.js';
import { Chessground } from '@lichess-org/chessground';
import type { Api } from '@lichess-org/chessground/api';
import type { Config } from '@lichess-org/chessground/config';
import type { Dests, Key } from '@lichess-org/chessground/types';

type ChessBoardProps = {
    fen: string;
    orientation: 'white' | 'black';
    onMove: (orig: string, dest: string, isLegal: boolean) => void;
    interactive: boolean;
};

function legalDests(chess: Chess): Dests {
    const dests: Dests = new Map();
    for (const square of chess.board().flatMap(rank => rank).filter(Boolean).map(piece => piece!.square)) {
        const moves = chess.moves({ square, verbose: true });
        if (moves.length) dests.set(square as Key, moves.map(move => move.to as Key));
    }
    return dests;
}

export function ChessBoard({ fen, orientation, onMove, interactive }: ChessBoardProps) {
    const elementRef = useRef<HTMLDivElement>(null);
    const groundRef = useRef<Api | null>(null);

    useEffect(() => {
        if (!elementRef.current) return;
        const chess = new Chess(fen);
        const config: Config = {
            fen,
            orientation,
            turnColor: chess.turn() === 'w' ? 'white' : 'black',
            check: chess.isCheck(),
            coordinates: true,
            blockTouchScroll: true,
            disableContextMenu: true,
            animation: { enabled: true, duration: 220 },
            movable: {
                free: false,
                color: interactive ? (chess.turn() === 'w' ? 'white' : 'black') : undefined,
                dests: interactive ? legalDests(chess) : new Map(),
                showDests: interactive,
                events: {
                    after: (orig, dest) => {
                        const attempt = new Chess(fen);
                        let isLegal = false;
                        try {
                            isLegal = Boolean(attempt.move({ from: orig, to: dest }));
                        } catch {
                            isLegal = false;
                        }
                        onMove(orig, dest, isLegal);
                    },
                },
            },
            draggable: { enabled: interactive, showGhost: true },
            selectable: { enabled: interactive },
            viewOnly: !interactive,
        };

        groundRef.current = Chessground(elementRef.current, config);
        return () => {
            groundRef.current?.destroy();
            groundRef.current = null;
        };
    }, [fen, orientation, interactive, onMove]);

    return <div className="chess-board cg-wrap" ref={elementRef} aria-label="Шахматная доска" />;
}
