import * as cg from 'chessground/types';
import { Chess, Piece, Square as Key, Move, ChessInstance, PieceType } from 'chess.js';
import { CgMove } from './chessground';
import { isRole, PromotionChar, PromotionRole, roleToSan, setFenTurn } from './util';

export interface ChessCtrl {
  dests(opts?: { illegal?: boolean }): cg.Dests;
  color(c: Color): void;
  color(): Color;
  fen(): string;
  move(orig: Key, dest: Key, prom?: PromotionRole | PromotionChar | ''): Move | null;
  occupation(): Partial<Record<Key, Piece>>;
  kingKey(color: Color): Key | undefined;
  findCapture(): CgMove;
  findUnprotectedCapture(): CgMove | undefined;
  checks(): CgMove[] | null;
  playRandomMove(): CgMove | undefined;
  get(square: Key): Piece | null;
  undo(): Move | null;
  instance: ChessInstance;
}

declare module 'chess.js' {
  interface ChessInstance {
    moves(opts: { square: Key; verbose: boolean; legal: boolean }): Move[];
  }
}

export default function (fen: string, appleKeys: Key[]): ChessCtrl {
  const chess = new Chess(fen);

  // adds enemy pawns on apples, for collisions
  if (appleKeys) {
    const color = chess.turn() === 'w' ? 'b' : 'w';
    appleKeys.forEach(key => {
      chess.put({ type: 'p', color: color }, key);
    });
  }

  const getColor = () => (chess.turn() == 'w' ? 'white' : 'black');

  const setColor = (c: Color) => {
    const turn = c === 'white' ? 'w' : 'b';
    let newFen = setFenTurn(chess.fen(), turn);
    chess.load(newFen);
    if (getColor() !== c) {
      // the en passant square prevents setting color
      newFen = newFen.replace(/ (w|b) ([kKqQ-]{1,4}) \w\d /, ' ' + turn + ' $2 - ');
      chess.load(newFen);
    }
  };

  const findCaptures = () =>
    chess
      .moves({ verbose: true })
      .filter(move => move.captured)
      .map(move => ({ orig: move.from, dest: move.to }));

  function color(): Color;
  function color(c: Color): void;
  function color(c?: Color) {
    return c ? setColor(c) : getColor();
  }

  return {
    dests: (opts?: { illegal?: boolean }) => {
      const dests: cg.Dests = new Map();
      chess.SQUARES.forEach(s => {
        const ms = chess.moves({
          square: s,
          verbose: true,
          legal: !opts?.illegal,
        });
        if (ms.length)
          dests.set(
            s,
            ms.map(m => m.to),
          );
      });
      return dests;
    },
    color,
    fen: chess.fen,
    move: (orig: Key, dest: Key, prom?: PromotionChar | PromotionRole | '') =>
      chess.move({
        from: orig,
        to: dest,
        promotion: prom ? (isRole(prom) ? roleToSan[prom] : prom) : undefined,
      }),
    occupation: () => {
      const map: Partial<Record<Key, Piece>> = {};
      chess.SQUARES.forEach(s => {
        const p = chess.get(s);
        if (p) map[s] = p;
      });
      return map;
    },
    kingKey: (color: Color) => {
      for (const i in chess.SQUARES) {
        const p = chess.get(chess.SQUARES[i]);
        if (p && p.type === 'k' && p.color === (color === 'white' ? 'w' : 'b')) return chess.SQUARES[i];
      }
      return undefined;
    },
    findCapture: () => findCaptures()[0],
    findUnprotectedCapture: () =>
      findCaptures().find(capture => {
        const clone = new Chess(chess.fen());
        clone.move({ from: capture.orig, to: capture.dest });
        return !clone.moves({ verbose: true }).some(m => m.captured && m.to === capture.dest);
      }),
    checks: () => {
      if (!chess.in_check()) return null;
      const color = getColor();
      setColor(color === 'white' ? 'black' : 'white');
      const checks = chess
        .moves({ verbose: true })
        .filter(move => (move.captured as PieceType) === 'k')
        .map(move => ({ orig: move.from, dest: move.to }));
      setColor(color);
      if (checks.length === 0) return null;
      return checks;
    },
    playRandomMove: () => {
      const moves = chess.moves({ verbose: true });
      if (moves.length) {
        const move = moves[Math.floor(Math.random() * moves.length)];
        chess.move(move);
        return { orig: move.from, dest: move.to };
      }
      return undefined;
    },
    get: chess.get,
    undo: chess.undo,
    instance: chess,
  };
}
