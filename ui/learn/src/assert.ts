import { Piece } from 'shogiground/types';
import { samePiece } from 'shogiground/util';
import { opposite, parseSquareName, parseUsi } from 'shogiops/util';
import { Assertion, Level, UsiWithColor } from './interfaces';
import { findCapture, findUnprotectedCapture } from './shogi';
import { currentPosition } from './util';

export function not(assert: Assertion): Assertion {
  return (level: Level, usiCList: UsiWithColor[]): boolean => !assert(level, usiCList);
}

export function and(...asserts: Assertion[]): Assertion {
  return (level: Level, usiCList: UsiWithColor[]): boolean => asserts.every(a => a(level, usiCList));
}

export function or(...asserts: Assertion[]): Assertion {
  return (level: Level, usiCList: UsiWithColor[]): boolean => asserts.some(a => a(level, usiCList));
}

export function obstaclesCaptured(level: Level, usiCList: UsiWithColor[]): boolean {
  const obstacles = level.obstacles;
  if (!obstacles) return true;

  return obstacles.every(ob => usiCList.some(uc => parseUsi(uc.usi)!.to === parseSquareName(ob)!));
}

export function scenarioSuccess(level: Level, usiCList: UsiWithColor[]): boolean {
  const scenario = level.scenario;
  return !!scenario?.every((usiC, i) => usiC.usi === usiCList[i]?.usi && usiC.color === usiCList[i]?.color);
}

export function scenarioFailure(level: Level, usiCList: UsiWithColor[]): boolean {
  const scenario = level.scenario;
  if (!scenario || usiCList.length > scenario.length) return false;
  return usiCList.some((uc, i) => uc.usi !== scenario[i].usi || uc.color !== scenario[i].color);
}

export function extinct(color: Color): Assertion {
  return (level: Level, usiCList: UsiWithColor[]) => {
    const shogi = currentPosition(level, usiCList);
    return shogi.board.color(color).isEmpty();
  };
}

export function check(color: Color): Assertion {
  return (level: Level, usiCList: UsiWithColor[]) => {
    const shogi = currentPosition(level, usiCList);
    shogi.turn = color;
    return shogi.isCheck();
  };
}

export function checkmate(level: Level, usiCList: UsiWithColor[]): boolean {
  const shogi = currentPosition(level, usiCList);
  return shogi.isCheckmate();
}

export function unprotectedCapture(level: Level, usiCList: UsiWithColor[]): boolean {
  const pos = currentPosition(level, usiCList);
  pos.turn = opposite(level.color);
  return !!findUnprotectedCapture(pos);
}

export function anyCapture(level: Level, usiCList: UsiWithColor[]): boolean {
  const pos = currentPosition(level, usiCList);
  pos.turn = opposite(level.color);
  return !!findCapture(pos);
}

export function pieceOn(piece: Piece, key: Key): Assertion {
  return (level: Level, usiCList: UsiWithColor[]) => {
    const shogi = currentPosition(level, usiCList);
    const boardPiece = shogi.board.get(parseSquareName(key));
    return !!boardPiece && samePiece(piece, boardPiece);
  };
}

export function colorOn(color: Color, key: Key): Assertion {
  return (level: Level, usiCList: UsiWithColor[]) => {
    const shogi = currentPosition(level, usiCList);
    const boardColor = shogi.board.getColor(parseSquareName(key));
    return !!boardColor && color === boardColor;
  };
}
