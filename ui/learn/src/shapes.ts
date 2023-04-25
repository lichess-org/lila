import { DrawShape, SquareHighlight } from 'shogiground/draw';
import { opposite } from 'shogiground/util';
import { attacks } from 'shogiops/attacks';
import { SquareSet } from 'shogiops/squareSet';
import { Piece } from 'shogiops/types';
import { makeSquareName, parseSquareName } from 'shogiops/util';
import { Level, Shape, UsiWithColor, VmEvaluation } from './interfaces';
import { findCaptures, inCheck } from './shogi';
import { currentPosition } from './util';

export function arrow(orig: Key | Piece, dest: Key | Piece, brush?: 'green' | 'red'): DrawShape {
  return {
    orig,
    dest,
    brush: brush || 'green',
  };
}

export function circle(key: Key | Piece, brush?: 'green' | 'red'): DrawShape {
  return {
    orig: key,
    dest: key,
    brush: brush || 'green',
  };
}

export function custom(key: Key, svg: string): DrawShape {
  return {
    orig: key,
    dest: key,
    customSvg: svg,
    brush: '',
  };
}

export function concat<T extends Shape>(...advices: VmEvaluation<T[]>[]): VmEvaluation<T[]> {
  return (level: Level, usiCList: UsiWithColor[]): T[] =>
    advices.reduce((acc, cur) => acc.concat(cur(level, usiCList) || []), [] as T[]);
}

export function onPly<T extends Shape>(ply: number, shapes: T[]): VmEvaluation<T[]> {
  return (_level: Level, usiCList: UsiWithColor[]): T[] => {
    if (ply === usiCList.length) return shapes;
    else return [];
  };
}

export function initial<T extends Shape>(shapes: T[]): VmEvaluation<T[]> {
  return onPly<T>(0, shapes);
}

export function onUsi<T extends Shape>(usi: Usi, shapes: T[]): VmEvaluation<T[]> {
  return (_level: Level, usiCList: UsiWithColor[]): T[] => {
    if (usiCList.length && usi === usiCList[usiCList.length - 1].usi) return shapes;
    else return [];
  };
}

export function onDest<T extends Shape>(dest: Key, shapes: T[]): VmEvaluation<T[]> {
  return (_level: Level, usiCList: UsiWithColor[]): T[] => {
    if (usiCList.length && dest === usiCList[usiCList.length - 1].usi.slice(2, 4)) return shapes;
    else return [];
  };
}

export function onSuccess<T extends Shape>(shapes: T[]): VmEvaluation<T[]> {
  return (level: Level, usiCList: UsiWithColor[]): T[] => {
    const usiCListTrim =
      usiCList.length && usiCList[usiCList.length - 1].color !== level.color ? usiCList.slice(0, -1) : usiCList;
    if (level.success(level, usiCListTrim)) return shapes;
    else return [];
  };
}

export function onFailure<T extends Shape>(shapes: T[]): VmEvaluation<T[]> {
  return (level: Level, usiCList: UsiWithColor[]): T[] => {
    if (usiCList.length && level.failure && level.failure(level, usiCList)) return shapes;
    else return [];
  };
}

export function checkShapes(level: Level, usiCList: UsiWithColor[]): DrawShape[] {
  if (!usiCList.length || !level.failure || !level.failure(level, usiCList)) return [];
  const pos = currentPosition(level, usiCList);
  const sideInCheck = inCheck(pos);
  if (sideInCheck) {
    const kingSq = pos.board.pieces(sideInCheck, 'king').singleSquare();
    pos.turn = opposite(sideInCheck);
    const kingAttacks = findCaptures(pos);
    return kingAttacks
      .filter(m => m.to === kingSq)
      .map(m => arrow(makeSquareName(m.from), makeSquareName(m.to), 'red'));
  } else return [];
}

export function pieceMovesHighlihts(piece: Piece, key: Key): SquareHighlight[] {
  const keys: Key[] = [],
    squares = attacks(piece, parseSquareName(key), SquareSet.empty());
  for (const s of squares) {
    keys.push(makeSquareName(s));
  }
  return keys.map(k => {
    return { key: k, className: 'help' };
  });
}
