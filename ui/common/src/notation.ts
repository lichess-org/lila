import { Notation as sgNotation } from 'shogiground/types';
import { makeJapaneseMove } from 'shogiops/notation/japanese';
import { makeKifMove } from 'shogiops/notation/kif/kif';
import { makeKitaoKawasakiMove } from 'shogiops/notation/kitaoKawasaki';
import { makeWesternMove } from 'shogiops/notation/western';
import { makeWesternEngineMove } from 'shogiops/notation/westernEngine';
import { parseSfen } from 'shogiops/sfen';
import { Move, Square } from 'shogiops/types';
import { parseUsi } from 'shogiops/util';
import { Position } from 'shogiops/variant/position';

export const enum Notation {
  Western,
  Kawasaki,
  Japanese,
  WesternEngine,
  Kif,
}

const notationPref: Notation = parseInt(document.body.dataset.notation || '0');

// Notations, that should be displayed with ☖/☗
export function notationsWithColor() {
  return [Notation.Kawasaki, Notation.Japanese, Notation.Kif].includes(notationPref);
}

export function notationFiles() {
  if (notationPref === Notation.Western) return sgNotation.HEX;
  else return sgNotation.NUMERIC;
}

export function notationRanks() {
  switch (notationPref) {
    case Notation.Japanese:
    case Notation.Kif:
      return sgNotation.JAPANESE;
    case Notation.WesternEngine:
      return sgNotation.ENGINE;
    default:
      return sgNotation.HEX;
  }
}

export function makeNotationWithPosition(pos: Position, move: Move, lastMove?: Move | { to: Square }): string {
  switch (notationPref) {
    case Notation.Kawasaki:
      return makeKitaoKawasakiMove(pos, move, lastMove?.to)!;
    case Notation.Japanese:
      return makeJapaneseMove(pos, move, lastMove?.to)!;
    case Notation.WesternEngine:
      return makeWesternEngineMove(pos, move)!;
    case Notation.Kif:
      return makeKifMove(pos, move, lastMove?.to)!;
    default:
      return makeWesternMove(pos, move)!;
  }
}

export function makeNotationLineWithPosition(
  pos: Position,
  moves: Move[],
  lastMove?: Move | { to: Square }
): MoveNotation[] {
  pos = pos.clone();
  const moveLine = [];
  for (const move of moves) {
    moveLine.push(makeNotationWithPosition(pos, move, lastMove));
    lastMove = move;
    pos.play(move);
  }
  return moveLine;
}

export function makeNotation(sfen: Sfen, variant: VariantKey, usi: Usi, lastUsi?: Usi): MoveNotation {
  const pos = createPosition(sfen, variant);
  const lastMove = lastUsi ? parseUsi(lastUsi)! : undefined;
  return makeNotationWithPosition(pos, parseUsi(usi)!, lastMove);
}

export function makeNotationLine(sfen: Sfen, variant: VariantKey, usis: Usi[], lastUsi?: Usi): MoveNotation[] {
  return makeNotationLineWithPosition(
    createPosition(sfen, variant),
    usis.map(u => parseUsi(u)!),
    lastUsi ? parseUsi(lastUsi) : undefined
  );
}

function createPosition(sfen: Sfen, variant: VariantKey): Position {
  return parseSfen(variant, sfen, false).unwrap();
}
