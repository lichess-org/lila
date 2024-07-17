import * as cg from 'chessground/types';
import { Square as Key, Move, ChessInstance, PieceType } from 'chess.js';
import { parseFen, makeFen } from 'chessops/fen';
import { parseSquare, makeSquare, Piece, Setup, SquareSet, Chess } from 'chessops';
import { Antichess, Context } from 'chessops/variant';
import { chessgroundDests } from 'chessops/compat';
import { CgMove } from './chessground';
import { isRole, PromotionChar, PromotionRole, roleToSan, setFenTurn } from './util';

export interface ChessCtrl {
  dests(opts?: { illegal?: boolean }): cg.Dests;
  setColor(c: Color): void;
  getColor(): Color;
  fen(): string;
  move(orig: Key, dest: Key, prom?: PromotionRole): void;
  occupation(): Partial<Record<Key, Piece>>;
  kingKey(color: Color): Key | undefined;
  findUnprotectedCapture(): CgMove | undefined;
  checks(): CgMove[] | null;
  playRandomMove(): CgMove | undefined;
  get(square: Key): Piece | undefined;
  instance: Chess | Antichess;
}

export default function (fen: string, appleKeys: Key[]): ChessCtrl {
  let setup = parseFen(fen).unwrap();
  const chess = Chess.fromSetup(setup);
  // Use antichess when there are less than 2 kings
  const pos = chess.isOk ? chess.unwrap() : Antichess.fromSetup(setup).unwrap();

  // adds enemy pawns on apples, for collisions
  if (appleKeys) {
    const color = pos.turn === 'white' ? 'black' : 'white';
    appleKeys.forEach(key => {
      pos.board.set(parseSquare(key), { color: color, role: 'pawn' });
    });
  }

  const context = (): Context => ({
    blockers: setup.board.occupied,
    checkers: SquareSet.empty(), //Revisit
    king: undefined,
    mustCapture: false,
    variantEnd: false,
  });
  if (pos instanceof Antichess) pos.ctx = context;

  const findCaptures = () => pos.allDests();

  const moves = (pos: Chess | Antichess) => {};

  return {
    dests: () => chessgroundDests(pos),
    getColor: () => pos.turn,
    setColor: (c: Color) => {
      pos.turn = c;
    },
    fen: () => makeFen(setup),
    move: (orig: Key, dest: Key, prom?: Piece) =>
      pos.play({
        from: parseSquare(orig),
        to: parseSquare(dest),
        promotion: prom?.role,
      }),
    occupation: () => {
      const map: Partial<Record<Key, Piece>> = {};
      Array.from(pos.board.occupied).map(s => {
        const p = pos.board.get(s);
        if (p) map[makeSquare(s)] = p;
      });
      return map;
    },
    kingKey: (color: Color) => {
      const kingSq = pos.board.kingOf(color);
      return kingSq ? makeSquare(kingSq) : undefined;
    },
    findUnprotectedCapture: () =>
      findCaptures().find(capture => {
        const clone = pos.clone();
        clone.play({ from: capture.orig, to: capture.dest });
        return !clone.moves({ verbose: true }).some(m => m.captured && m.to === capture.dest);
      }),
    checks: () => {
      if (!setup.in_check()) return null;
      const color = getColor();
      setColor(color === 'white' ? 'black' : 'white');
      const checks = setup
        .moves({ verbose: true })
        .filter(move => (move.captured as PieceType) === 'k')
        .map(move => ({ orig: move.from, dest: move.to }));
      setColor(color);
      if (checks.length === 0) return null;
      return checks;
    },
    playRandomMove: () => {
      const moves = pos.moves({ verbose: true });
      if (moves.length) {
        const move = moves[Math.floor(Math.random() * moves.length)];
        setup.move(move);
        return { orig: move.from, dest: move.to };
      }
      return undefined;
    },
    get: (key: Key) => setup.board.get(parseSquare(key)),
    instance: pos,
  };
}
