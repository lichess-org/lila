import { Dests, DropDests } from 'shogiground/types';
import { Position } from 'shogiops/shogi';
import { SquareSet } from 'shogiops/squareSet';
import { NormalMove, Role, Square } from 'shogiops/types';
import { makeSquare, makeUsi, opposite } from 'shogiops/util';
import { backrank, handRoles, secondBackrank } from 'shogiops/variantUtil';

export function illegalShogigroundDests(pos: Position): Dests {
  const result = new Map();
  const illegalDests = pos.allDests({
    king: undefined,
    blockers: SquareSet.empty(),
    checkers: SquareSet.empty(),
    variantEnd: false,
    mustCapture: false,
  });
  for (const [from, squares] of illegalDests) {
    if (squares.nonEmpty()) {
      const d = Array.from(squares, s => makeSquare(s));
      result.set(makeSquare(from), d);
    }
  }
  return result;
}

export function illegalShogigroundDropDests(pos: Position): DropDests {
  // From https://github.com/WandererXII/shogiops/blob/master/src/shogi.ts
  const result: Map<Role, Key[]> = new Map(),
    notOccuppied = pos.board.occupied
      .complement()
      .intersect(new SquareSet([0x1ff01ff, 0x1ff01ff, 0x1ff01ff, 0x1ff01ff, 0x1ff, 0x0, 0x0, 0x0]));

  for (const role of handRoles('standard')) {
    if (pos.hands[pos.turn][role] > 0) {
      let squares = notOccuppied;
      if (role === 'pawn' || role === 'lance' || role === 'knight')
        squares = squares.diff(backrank('standard')(pos.turn));
      if (role === 'knight') squares = squares.diff(secondBackrank('standard')(pos.turn));
      const d: Key[] = Array.from(squares, s => makeSquare(s) as Key);
      result.set(role, d);
    } else result.set(role, []);
  }

  return result;
}

export function findRandomMove(pos: Position): Usi | undefined {
  const moveDests = pos.allDests(),
    origs: Square[] = Array.from(moveDests.keys()).filter(sq => moveDests.get(sq)?.nonEmpty()),
    randomOrig: Square | undefined = origs[Math.floor(Math.random() * origs.length)],
    dests: Square[] = Array.from(moveDests.get(randomOrig)!),
    randomDest: Square | undefined = dests[Math.floor(Math.random() * dests.length)];

  if (randomOrig !== undefined && randomDest !== undefined) return makeUsi({ from: randomOrig, to: randomDest });
  return;
}

export function findCaptures(pos: Position): NormalMove[] {
  const captures: NormalMove[] = [];
  for (const [orig, dests] of pos.allDests()) {
    for (const dest of dests) {
      if (pos.board[opposite(pos.turn)].has(dest)) {
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
    for (const [_orig, dests] of clone.allDests()) {
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
