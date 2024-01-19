import { Notation as sgNotation } from 'shogiground/types';
import { makeJapaneseMove } from 'shogiops/notation/japanese';
import { makeKifMove } from 'shogiops/notation/kif/kif';
import { makeKitaoKawasakiMove } from 'shogiops/notation/kitaoKawasaki';
import { roleToFullKanji, roleToKanji, roleToWestern } from 'shogiops/notation/util';
import { makeWesternMove } from 'shogiops/notation/western';
import { makeWesternEngineMove } from 'shogiops/notation/westernEngine';
import { parseSfen, roleToForsyth } from 'shogiops/sfen';
import { Move, Role, Rules, Square } from 'shogiops/types';
import { makeUsi, parseUsi } from 'shogiops/util';
import { Position } from 'shogiops/variant/position';

export const enum Notation {
  Western,
  Kawasaki,
  Japanese,
  WesternEngine,
  Kif,
  Usi,
}

const notationPref: Notation = parseInt(document.body.dataset.notation || '0');

// Notations, that should be displayed with ☖/☗
export function notationsWithColor() {
  return [Notation.Kawasaki, Notation.Japanese, Notation.Kif].includes(notationPref);
}

export function notationFiles(): sgNotation {
  if (notationPref === Notation.Western) return sgNotation.HEX;
  else return sgNotation.NUMERIC;
}

export function notationRanks(): sgNotation {
  switch (notationPref) {
    case Notation.Japanese:
    case Notation.Kif:
      return sgNotation.JAPANESE;
    case Notation.WesternEngine:
    case Notation.Usi:
      return sgNotation.ENGINE;
    default:
      return sgNotation.HEX;
  }
}

export function roleName(rules: Rules, role: Role): string {
  switch (notationPref) {
    case Notation.Kawasaki:
      return roleToKanji(role).replace('成', '+');
    case Notation.Japanese:
      return roleToKanji(role);
    case Notation.Kif:
      return roleToFullKanji(role);
    case Notation.Usi:
      return roleToForsyth(rules)(role)!;
    default:
      return roleToWestern(rules)(role);
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
    case Notation.Usi:
      return makeUsi(move);
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
