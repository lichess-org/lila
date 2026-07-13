import {
  attacks,
  ray,
  between,
  kingAttacks,
  knightAttacks,
  pawnAttacks,
  rookAttacks,
  bishopAttacks,
} from 'chessops/attacks';
import { Board } from 'chessops/board';
import { Chess } from 'chessops/chess';
import { chessgroundDests } from 'chessops/compat';
import { SquareSet } from 'chessops/squareSet';
import { type Role, type Color, type Piece, type NormalMove, COLORS, type Square } from 'chessops/types';
import { parseSquare, opposite, squareRank, squareFile, squareFromCoords } from 'chessops/util';

import type { Pin, Undefended, Checkable } from './interfaces';

export const boardAnalysisVariants = [
  'standard',
  'chess960',
  'fromPosition',
  'kingOfTheHill',
  'threeCheck',
  'racingKings',
];

const values: Record<Role, number> = { pawn: 1, knight: 3, bishop: 3, rook: 5, queen: 9, king: 100 };

const isSquareAttacked = (square: Square, byColor: Color, cb: Board): boolean =>
  knightAttacks(square).intersects(cb[byColor].intersect(cb.knight)) ||
  pawnAttacks(opposite(byColor), square).intersects(cb[byColor].intersect(cb.pawn)) ||
  kingAttacks(square).intersects(cb[byColor].intersect(cb.king)) ||
  rookAttacks(square, cb.occupied).intersects(cb[byColor].intersect(cb.rooksAndQueens())) ||
  bishopAttacks(square, cb.occupied).intersects(cb[byColor].intersect(cb.bishopsAndQueens()));

interface PieceOnSquare {
  piece: Piece;
  square: Square;
}

function getAttackers(square: Square, byColor: Color, cb: Board, byRole?: Role): PieceOnSquare[] {
  const attackers: PieceOnSquare[] = [];
  const colorSet = cb[byColor];

  const add = (squares: SquareSet) => {
    for (const square of squares) {
      const piece = cb.get(square);
      if (piece && (!byRole || piece.role === byRole)) attackers.push({ piece, square });
    }
  };

  add(knightAttacks(square).intersect(colorSet).intersect(cb.knight));
  add(pawnAttacks(opposite(byColor), square).intersect(colorSet).intersect(cb.pawn));
  add(kingAttacks(square).intersect(colorSet).intersect(cb.king));
  add(rookAttacks(square, cb.occupied).intersect(colorSet).intersect(cb.rooksAndQueens()));
  add(bishopAttacks(square, cb.occupied).intersect(colorSet).intersect(cb.bishopsAndQueens()));

  const pins = detectPins(cb);
  const usableAttacker = (attacker: PieceOnSquare): boolean => {
    if (attacker.piece.role === 'king' && isSquareAttacked(square, opposite(byColor), cb)) return false;
    const pin = pins.find(p => p.pinned === attacker.square);
    return (
      !pin ||
      cb.get(pin.target)?.role !== 'king' ||
      square === pin.pinner ||
      between(pin.pinner, pin.target).has(square)
    );
  };
  return attackers.filter(usableAttacker);
}

export function detectPins(cb: Board): Pin[] {
  const pins: Pin[] = [];
  const { occupied } = cb;

  for (const square of occupied) {
    const piece = cb.get(square);
    if (!piece) continue;
    if (piece.role !== 'bishop' && piece.role !== 'rook' && piece.role !== 'queen') continue;

    const attackSet = attacks(piece, square, occupied);
    const pinnedCandidates = attackSet.intersect(cb[opposite(piece.color)]);

    for (const pinned of pinnedCandidates) {
      const raySet = ray(square, pinned);
      const xray = attacks(piece, square, occupied.without(pinned)).intersect(raySet);
      const targets = xray.intersect(occupied).without(pinned);

      for (const target of targets) {
        if (!between(square, target).has(pinned)) continue;

        const targetPiece = cb.get(target);
        if (!targetPiece || targetPiece.color === piece.color) continue;

        const pinnedPiece = cb.get(pinned);
        if (!pinnedPiece) continue;

        if (targetPiece.role === 'king') {
          // Absolute pin
          pins.push({ pinned, pinner: square, target });
        } else {
          // Relative pin
          const valTarget = values[targetPiece.role],
            valPinned = values[pinnedPiece.role],
            valAttacker = values[piece.role];

          if (
            valTarget > valPinned && // Back piece is worth more than front piece
            (!isSquareAttacked(target, targetPiece.color, cb) || valTarget > valAttacker) // Back piece is undefended OR worth more than the attacker
          ) {
            pins.push({ pinned, pinner: square, target });
          }
        }
        break;
      }
    }
  }
  return pins;
}

const epTargetPawnSq = (epSquare: Square): Square =>
  squareFromCoords(squareFile(epSquare), squareRank(epSquare) === 2 ? 3 : 4)!;

const lookupKey = ({ occupied }: Board, target: Piece) => `${occupied.hi},${occupied.lo},${target.role}`;

interface SEEResult {
  balance: number;
  firstAttacker?: Square;
}

function getSEE(
  square: Square,
  isEpSquare: boolean,
  target: Piece,
  cb: Board,
  lookupTable: Map<string, SEEResult>,
  recursiveCall: boolean,
): SEEResult {
  if (!recursiveCall) {
    cb = cb.clone();
    cb.take(square);
    if (isEpSquare) cb.take(epTargetPawnSq(square)); // e.g., Qf3 would control f6 ep square
  }
  const key = lookupKey(cb, target);
  if (lookupTable.has(key)) return lookupTable.get(key)!;
  const attackers = getAttackers(square, opposite(target.color), cb, isEpSquare ? 'pawn' : undefined);
  // LVA
  attackers.sort((a, b) => values[a.piece.role] - values[b.piece.role]);
  let bestChoiceBalance = 0;
  let firstAttacker: Square | undefined = undefined;
  for (const attacker of attackers.slice(0, 2)) {
    const simulationBoard = cb.clone();
    simulationBoard.take(attacker.square);
    const opponentRecaptureBalance = getSEE(
      square,
      false,
      attacker.piece,
      simulationBoard,
      lookupTable,
      true,
    ).balance;
    const currChoiceBalance = values[target.role] - opponentRecaptureBalance;
    if (currChoiceBalance > bestChoiceBalance) {
      bestChoiceBalance = currChoiceBalance;
      firstAttacker = attacker.square;
    }
  }
  const result = { balance: bestChoiceBalance, firstAttacker };
  lookupTable.set(key, result);
  return result;
}

export function detectUndefended(board: Board, epSquare?: Square): Undefended[] {
  const undefended: Undefended[] = [];
  const cb = board;
  for (let i = 0; i < 64; i++) {
    const p = board.get(i === epSquare ? epTargetPawnSq(epSquare) : i);
    if (p && p.role !== 'king' && isSquareAttacked(i, opposite(p.color), cb)) {
      const { balance, firstAttacker } = getSEE(i, i === epSquare, p, cb, new Map(), false);
      if (balance > 0 && firstAttacker !== undefined) {
        undefended.push({
          square: i,
          materialLoss: balance,
          principalAttacker: firstAttacker,
        });
      }
    }
  }
  return undefended;
}

export function detectCheckable(
  cb: Board,
  epSquare: Square | undefined,
  castlingRights: SquareSet,
): Checkable[] {
  const checkable: Checkable[] = [];

  for (const color of COLORS) {
    const kSq = cb.kingOf(color);

    // Skip if King is already in check
    if (kSq === undefined || isSquareAttacked(kSq, opposite(color), cb)) continue;

    const oppColor = opposite(color);
    const res = Chess.fromSetup({
      board: cb,
      turn: oppColor,
      castlingRights,
      epSquare,
      halfmoves: 0,
      fullmoves: 1,
      pockets: undefined,
      remainingChecks: undefined,
    });

    if ('error' in res) continue;
    const legalPos = res.value;

    const dests = chessgroundDests(legalPos);
    let checkFound: NormalMove | undefined;

    const enemies = cb[oppColor];
    const enemyRooksQueens = enemies.intersect(cb.rooksAndQueens());
    const enemyBishopsQueens = enemies.intersect(cb.bishopsAndQueens());

    for (const [fromStr, tos] of dests) {
      if (checkFound) break;

      const from = parseSquare(fromStr);
      const piece = cb.get(from);

      if (!piece) continue;

      for (const toStr of tos) {
        const to = parseSquare(toStr);
        const rank = squareRank(to);

        const isPromo = piece.role === 'pawn' && (rank === 0 || rank === 7);
        const isCastling = piece.role === 'king' && Math.abs(to - from) > 1;
        const isEp = piece.role === 'pawn' && to === epSquare;

        if (isPromo || isCastling || isEp) {
          const candidates: (Role | undefined)[] = isPromo ? ['queen', 'knight'] : [undefined];

          for (const promotion of candidates) {
            const testPos = legalPos.clone();
            testPos.play({ from, to, promotion });
            if (testPos.isCheck()) {
              checkFound = { from, to, promotion };
              break;
            }
          }
        } else {
          const occupied = cb.occupied.without(from).with(to);

          // Direct check or discovered check
          if (
            attacks(piece, to, occupied).has(kSq) ||
            rookAttacks(kSq, occupied).intersects(enemyRooksQueens.without(from)) ||
            bishopAttacks(kSq, occupied).intersects(enemyBishopsQueens.without(from))
          ) {
            checkFound = { from, to };
          }
        }

        if (checkFound) break;
      }
    }

    if (checkFound) checkable.push({ king: kSq, check: checkFound });
  }
  return checkable;
}
