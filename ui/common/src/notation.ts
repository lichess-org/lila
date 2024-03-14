import { Notation as sgNotation } from 'shogiground/types';
import { makeJapaneseMoveOrDrop } from 'shogiops/notation/japanese';
import { makeKifMoveOrDrop } from 'shogiops/notation/kif/kif';
import { makeKitaoKawasakiMoveOrDrop } from 'shogiops/notation/kitaoKawasaki';
import { roleToFullKanji, roleToKanji, roleToWestern } from 'shogiops/notation/util';
import { makeWesternMoveOrDrop } from 'shogiops/notation/western';
import { makeWesternEngineMoveOrDrop } from 'shogiops/notation/westernEngine';
import { parseSfen, roleToForsyth } from 'shogiops/sfen';
import { MoveOrDrop, Role, Rules, Square } from 'shogiops/types';
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

export function makeNotationWithPosition(
  pos: Position,
  md: MoveOrDrop,
  lastMoveOrDrop?: MoveOrDrop | { to: Square }
): string {
  switch (notationPref) {
    case Notation.Kawasaki:
      return makeKitaoKawasakiMoveOrDrop(pos, md, lastMoveOrDrop?.to)!;
    case Notation.Japanese:
      return makeJapaneseMoveOrDrop(pos, md, lastMoveOrDrop?.to)!;
    case Notation.WesternEngine:
      return makeWesternEngineMoveOrDrop(pos, md)!;
    case Notation.Kif:
      return makeKifMoveOrDrop(pos, md, lastMoveOrDrop?.to)!;
    case Notation.Usi:
      return makeUsi(md);
    default:
      return makeWesternMoveOrDrop(pos, md)!;
  }
}

export function makeNotationLineWithPosition(
  pos: Position,
  mds: MoveOrDrop[],
  lastMoveOrDrop?: MoveOrDrop | { to: Square }
): MoveNotation[] {
  pos = pos.clone();
  const line = [];
  for (const md of mds) {
    line.push(makeNotationWithPosition(pos, md, lastMoveOrDrop));
    lastMoveOrDrop = md;
    pos.play(md);
  }
  return line;
}

export function makeNotation(sfen: Sfen, variant: VariantKey, usi: Usi, lastUsi?: Usi): MoveNotation {
  const pos = createPosition(sfen, variant),
    lastMoveOrDrop = lastUsi ? parseUsi(lastUsi) : undefined;
  return makeNotationWithPosition(pos, parseUsi(usi)!, lastMoveOrDrop);
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
