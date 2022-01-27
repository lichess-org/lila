import type { Piece, Square as Key } from 'chess.js';
import { AssertData } from './level';
import { readKeys } from './util';

interface Matcher {
  type: string;
  color: 'b' | 'w';
}

function pieceMatch(piece: Piece | null, matcher: Matcher) {
  if (!piece) return false;
  return piece.type === matcher.type && piece.color === matcher.color;
}

function pieceOnAnyOf(matcher: Matcher, keys: Key[]) {
  return function (level: AssertData) {
    for (const i in keys) if (pieceMatch(level.chess.get(keys[i]), matcher)) return true;
    return false;
  };
}

function fenToMatcher(fenPiece: string): Matcher {
  return {
    type: fenPiece.toLowerCase(),
    color: fenPiece.toLowerCase() === fenPiece ? 'b' : 'w',
  };
}

export function pieceOn(fenPiece: string, key: Key) {
  return function (level: AssertData) {
    return pieceMatch(level.chess.get(key), fenToMatcher(fenPiece));
  };
}

export function pieceNotOn(fenPiece: string, key: Key) {
  return function (level: AssertData) {
    return !pieceMatch(level.chess.get(key), fenToMatcher(fenPiece));
  };
}

export function noPieceOn(keys: string | Key[]) {
  keys = readKeys(keys);
  return function (level: AssertData) {
    for (const key in level.chess.occupation()) if (!keys.includes(key as Key)) return true;
    return false;
  };
}

export function whitePawnOnAnyOf(keys: string | Key[]) {
  return pieceOnAnyOf(fenToMatcher('P'), readKeys(keys));
}

export function extinct(color: string) {
  return function (level: AssertData) {
    const fen = level.chess.fen().split(' ')[0].replace(/\//g, '');
    return fen === (color === 'white' ? fen.toLowerCase() : fen.toUpperCase());
  };
}

export function check(level: AssertData) {
  return level.chess.instance.in_check();
}

export function mate(level: AssertData) {
  return level.chess.instance.in_checkmate();
}

export function lastMoveSan(san: string) {
  return function (level: AssertData) {
    const moves = level.chess.instance.history();
    return moves[moves.length - 1] === san;
  };
}

export function checkIn(nbMoves: number) {
  return function (level: AssertData) {
    return level.vm.nbMoves <= nbMoves && level.chess.instance.in_check();
  };
}

export function noCheckIn(nbMoves: number) {
  return function (level: AssertData) {
    return level.vm.nbMoves >= nbMoves && !level.chess.instance.in_check();
  };
}

type Assert = (level: AssertData) => boolean;

export function not(assert: Assert) {
  return function (level: AssertData) {
    return !assert(level);
  };
}

export function and(...asserts: Assert[]) {
  return function (level: AssertData) {
    return asserts.every(function (a) {
      return a(level);
    });
  };
}

export function or(...asserts: Assert[]) {
  return function (level: AssertData) {
    return asserts.some(function (a) {
      return a(level);
    });
  };
}

export function scenarioComplete(level: AssertData) {
  return level.scenario.isComplete();
}

export function scenarioFailed(level: AssertData) {
  return level.scenario.isFailed();
}
