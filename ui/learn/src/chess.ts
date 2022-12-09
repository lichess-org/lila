import { Chess, Piece, Square as Key, Move, ChessInstance, PieceType } from 'chess.js';
import { CgMove, Dests } from './ground';
import { isRole, PromotionChar, PromotionRole, roleToSan, setFenTurn } from './util';

export interface ChessCtrl {
  dests(opts?: { illegal?: boolean }): Partial<Record<Key, Key[]>>;
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
    appleKeys.forEach(function (key) {
      chess.put(
        {
          type: 'p',
          color: color,
        },
        key
      );
    });
  }

  function getColor() {
    return chess.turn() == 'w' ? 'white' : 'black';
  }

  function setColor(c: Color) {
    const turn = c === 'white' ? 'w' : 'b';
    let newFen = setFenTurn(chess.fen(), turn);
    chess.load(newFen);
    if (getColor() !== c) {
      // the en passant square prevents setting color
      newFen = newFen.replace(/ (w|b) ([kKqQ-]{1,4}) \w\d /, ' ' + turn + ' $2 - ');
      chess.load(newFen);
    }
  }

  const findCaptures = function () {
    return chess
      .moves({
        verbose: true,
      })
      .filter(function (move) {
        return move.captured;
      })
      .map(function (move) {
        return {
          orig: move.from,
          dest: move.to,
        };
      });
  };

  function color(): Color;
  function color(c: Color): void;
  function color(c?: Color) {
    if (c) return setColor(c);
    else return getColor();
  }

  return {
    dests: function (opts?: { illegal?: boolean }) {
      const dests: Dests = {};
      chess.SQUARES.forEach(function (s) {
        const ms = chess.moves({
          square: s,
          verbose: true,
          legal: !opts?.illegal,
        });
        if (ms.length)
          dests[s] = ms.map(function (m) {
            return m.to;
          });
      });
      return dests;
    },
    color,
    fen: chess.fen,
    move: function (orig: Key, dest: Key, prom?: PromotionChar | PromotionRole | '') {
      return chess.move({
        from: orig,
        to: dest,
        promotion: prom ? (isRole(prom) ? roleToSan[prom] : prom) : undefined,
      });
    },
    occupation: function () {
      const map: Partial<Record<Key, Piece>> = {};
      chess.SQUARES.forEach(function (s) {
        const p = chess.get(s);
        if (p) map[s] = p;
      });
      return map;
    },
    kingKey: function (color: Color) {
      for (const i in chess.SQUARES) {
        const p = chess.get(chess.SQUARES[i]);
        if (p && p.type === 'k' && p.color === (color === 'white' ? 'w' : 'b')) return chess.SQUARES[i];
      }
      return undefined;
    },
    findCapture: function () {
      return findCaptures()[0];
    },
    findUnprotectedCapture: function () {
      return findCaptures().find(function (capture) {
        const clone = new Chess(chess.fen());
        clone.move({ from: capture.orig, to: capture.dest });
        return !clone
          .moves({
            verbose: true,
          })
          .some(function (m) {
            return m.captured && m.to === capture.dest;
          });
      });
    },
    checks: function () {
      if (!chess.in_check()) return null;
      const color = getColor();
      setColor(color === 'white' ? 'black' : 'white');
      const checks = chess
        .moves({
          verbose: true,
        })
        .filter(function (move) {
          return (move.captured as PieceType) === 'k';
        })
        .map(function (move) {
          return {
            orig: move.from,
            dest: move.to,
          };
        });
      setColor(color);
      return checks;
    },
    playRandomMove: function () {
      const moves = chess.moves({
        verbose: true,
      });
      if (moves.length) {
        const move = moves[Math.floor(Math.random() * moves.length)];
        chess.move(move);
        return {
          orig: move.from,
          dest: move.to,
        };
      }
      return undefined;
    },
    get: chess.get,
    undo: chess.undo,
    instance: chess,
  };
}
