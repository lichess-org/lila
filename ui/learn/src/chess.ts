import * as cg from 'chessground/types';
import { parseFen, makeFen } from 'chessops/fen';
import {
  parseSquare,
  makeSquare,
  Piece,
  SquareSet,
  Chess,
  NormalMove as Move,
  SquareName as Key,
} from 'chessops';
import { Antichess, Context } from 'chessops/variant';
import { chessgroundDests } from 'chessops/compat';
import { CgMove } from './chessground';
import { makeSan } from 'chessops/san';
import { oppColor, PromotionChar, promotionCharToRole } from './util';

type LearnVariant = Chess | Antichess;

export interface ChessCtrl {
  dests(opts?: { illegal?: boolean }): cg.Dests;
  setColor(c: Color): void;
  getColor(): Color;
  fen(): string;
  move(orig: Key, dest: Key, prom?: PromotionChar | ''): Move | null;
  moves(pos: LearnVariant): Move[];
  occupiedKeys(): Key[];
  kingKey(color: Color): Key | undefined;
  findCapture(): CgMove;
  findUnprotectedCapture(): CgMove | undefined;
  checks(): CgMove[] | undefined;
  playRandomMove(): CgMove | undefined;
  get(square: Key): Piece | undefined;
  instance: LearnVariant;
  sanHistory(): string[];
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

  const history: string[] = [];

  const moves = (pos: LearnVariant): Move[] =>
    Array.from(chessgroundDests(pos)).flatMap(([orig, dests]) =>
      dests.map((dest): Move => ({ from: parseSquare(orig), to: parseSquare(dest) })),
    );

  const findCaptures = (pos: LearnVariant): Move[] => moves(pos).filter(move => pos.board.get(move.to));

  const setColor = (c: Color) => {
    pos.turn = c;
  };

  const moveToCgMove = (move: Move): CgMove => ({ orig: makeSquare(move.from), dest: makeSquare(move.to) });

  return {
    dests: () => chessgroundDests(pos),
    getColor: () => pos.turn,
    setColor: setColor,
    fen: () => makeFen(setup),
    moves: moves,
    move: (orig: Key, dest: Key, prom?: PromotionChar) => {
      const move: Move = {
        from: parseSquare(orig),
        to: parseSquare(dest),
        promotion: prom ? promotionCharToRole[prom] : undefined,
      };
      pos.play(move);
      if (pos.isCheck()) return null;
      history.push(makeSan(pos, move));
      return move;
    },
    occupiedKeys: () => Array.from(pos.board.occupied).map(s => makeSquare(s)),
    kingKey: (color: Color) => {
      const kingSq = pos.board.kingOf(color);
      return kingSq ? makeSquare(kingSq) : undefined;
    },
    findCapture: () => moveToCgMove(findCaptures(pos)[0]),
    findUnprotectedCapture: () => {
      const maybeCapture = findCaptures(pos).find(capture => {
        const clone = pos.clone();
        clone.play({ from: capture.from, to: capture.to });
        return !findCaptures(clone).length;
      });
      return maybeCapture ? moveToCgMove(maybeCapture) : undefined;
    },
    checks: () => {
      if (!pos.isCheck()) return undefined;
      const color = pos.turn;
      setColor(oppColor(color));
      const checks = moves(pos)
        .filter(m => pos.board.get(m.to)?.role == 'king')
        .map(moveToCgMove);
      setColor(color);
      return checks.length == 0 ? undefined : checks;
    },
    playRandomMove: () => {
      const all = moves(pos);
      if (all.length) {
        const move = all[Math.floor(Math.random() * all.length)];
        pos.play(move);
        return moveToCgMove(move);
      }
      return undefined;
    },
    get: (key: Key) => setup.board.get(parseSquare(key)),
    instance: pos,
    sanHistory: () => history,
  };
}
