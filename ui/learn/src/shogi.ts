import { DropDests, MoveDests } from 'shogiground/types';
import { SquareSet } from 'shogiops/squareSet';
import { NormalMove, PieceName, Square } from 'shogiops/types';
import { makeSquareName, makeUsi, opposite } from 'shogiops/util';
import { Position } from 'shogiops/variant/position';
import { fullSquareSet, handRoles } from 'shogiops/variant/util';

export function illegalShogigroundMoveDests(pos: Position): MoveDests {
  const result = new Map();
  const illegalDests = pos.allMoveDests({
    king: undefined,
    color: pos.turn,
    blockers: SquareSet.empty(),
    checkers: SquareSet.empty(),
  });
  for (const [from, squares] of illegalDests) {
    if (squares.nonEmpty()) {
      const d = Array.from(squares, s => makeSquareName(s));
      result.set(makeSquareName(from), d);
    }
  }
  return result;
}

function backrank(color: Color): SquareSet {
  return SquareSet.fromRank(color === 'sente' ? 0 : 8);
}

function secondBackrank(color: Color): SquareSet {
  return color === 'sente' ? SquareSet.ranksAbove(2) : SquareSet.ranksBelow(6);
}

export function illegalShogigroundDropDests(pos: Position): DropDests {
  // From https://github.com/WandererXII/shogiops/blob/master/src/shogi.ts
  const result: Map<PieceName, Key[]> = new Map(),
    notOccuppied = pos.board.occupied.complement().intersect(fullSquareSet('standard'));

  for (const role of handRoles('standard')) {
    const pieceName: PieceName = `${pos.turn} ${role}`;
    if (pos.hands.color(pos.turn).get(role) > 0) {
      let squares = notOccuppied;
      if (role === 'pawn' || role === 'lance' || role === 'knight') squares = squares.diff(backrank(pos.turn));
      if (role === 'knight') squares = squares.diff(secondBackrank(pos.turn));
      const d: Key[] = Array.from(squares, s => makeSquareName(s));
      result.set(pieceName, d);
    } else result.set(pieceName, []);
  }
  return result;
}

export function findRandomMove(pos: Position): Usi | undefined {
  const moveDests = pos.allMoveDests(),
    origs: Square[] = Array.from(moveDests.keys()).filter(sq => moveDests.get(sq)?.nonEmpty()),
    randomOrig: Square | undefined = origs[Math.floor(Math.random() * origs.length)],
    dests: Square[] = Array.from(moveDests.get(randomOrig)!),
    randomDest: Square | undefined = dests[Math.floor(Math.random() * dests.length)];

  if (randomOrig !== undefined && randomDest !== undefined) return makeUsi({ from: randomOrig, to: randomDest });
  return;
}

export function findCaptures(pos: Position): NormalMove[] {
  const captures: NormalMove[] = [];
  for (const [orig, dests] of pos.allMoveDests()) {
    for (const dest of dests) {
      if (pos.board.color(opposite(pos.turn)).has(dest)) {
        captures.push({ from: orig, to: dest });
      }
    }
  }
  return captures;
}

export function findCapture(pos: Position): Usi | undefined {
  const move = findCaptures(pos)[0];
  return move && makeUsi(move);
}

export function findUnprotectedCapture(pos: Position): Usi | undefined {
  const move = findCaptures(pos).find(move => {
    const clone = pos.clone();
    clone.play(move);
    for (const [_orig, dests] of clone.allMoveDests()) {
      if (dests.has(move.to)) return false;
    }
    return true;
  });
  return move && makeUsi(move);
}

export function inCheck(pos: Position): Color | undefined {
  if (pos.isCheck()) return pos.turn;
  const clone = pos.clone();
  clone.turn = opposite(pos.turn);
  if (clone.isCheck()) return clone.turn;
  return;
}
