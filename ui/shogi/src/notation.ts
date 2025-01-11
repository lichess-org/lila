import type { Notation as sgNotation } from 'shogiground/types';
import { makeJapaneseMoveOrDrop } from 'shogiops/notation/japanese';
import { makeKifMoveOrDrop } from 'shogiops/notation/kif';
import { makeKitaoKawasakiMoveOrDrop } from 'shogiops/notation/kitao-kawasaki';
import { roleToFullKanji, roleToKanji, roleToWestern } from 'shogiops/notation/util';
import { makeWesternMoveOrDrop } from 'shogiops/notation/western';
import { makeWesternEngineMoveOrDrop } from 'shogiops/notation/western-engine';
import { makeYorozuyaMoveOrDrop } from 'shogiops/notation/yorozuya';
import { parseSfen, roleToForsyth } from 'shogiops/sfen';
import type { MoveOrDrop, Role, Rules, Square } from 'shogiops/types';
import { makeUsi, parseUsi, toBW } from 'shogiops/util';
import type { Position } from 'shogiops/variant/position';
import { plyColor } from './common';

export const Notation = {
  Western: 0,
  Kawasaki: 1,
  Japanese: 2,
  WesternEngine: 3,
  Kif: 4,
  Usi: 5,
  Yorozuya: 6,
} as const;
export type Notation = (typeof Notation)[keyof typeof Notation];

const notationPref = Number.parseInt(document.body.dataset.notation || '0') as Notation;

// Notations, that should be displayed with ☖/☗
export function notationsWithColor(): boolean {
  const colorNotations: Notation[] = [Notation.Kawasaki, Notation.Japanese, Notation.Kif];
  return colorNotations.includes(notationPref);
}

export function notationFiles(): sgNotation {
  if (notationPref === Notation.Western) return 'hex';
  else if (notationPref === Notation.Yorozuya) return 'dizhi';
  else return 'numeric';
}

export function notationRanks(): sgNotation {
  switch (notationPref) {
    case Notation.Japanese:
    case Notation.Kif:
    case Notation.Yorozuya:
      return 'japanese';
    case Notation.WesternEngine:
    case Notation.Usi:
      return 'engine';
    default:
      return 'hex';
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
  lastMoveOrDrop?: MoveOrDrop | { to: Square },
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
    case Notation.Yorozuya:
      return makeYorozuyaMoveOrDrop(pos, md, lastMoveOrDrop?.to)!;
    default:
      return makeWesternMoveOrDrop(pos, md)!;
  }
}

export function makeNotationLineWithPosition(
  pos: Position,
  mds: MoveOrDrop[],
  lastMoveOrDrop?: MoveOrDrop | { to: Square },
): MoveNotation[] {
  pos = pos.clone();
  const line: MoveNotation[] = [];
  for (const md of mds) {
    line.push(makeNotationWithPosition(pos, md, lastMoveOrDrop));
    lastMoveOrDrop = md;
    pos.play(md);
  }
  return line;
}

export function makeNotation(
  sfen: Sfen,
  variant: VariantKey,
  usi: Usi,
  lastUsi?: Usi,
): MoveNotation {
  const pos = createPosition(sfen, variant);
  const lastMoveOrDrop = lastUsi ? parseUsi(lastUsi) : undefined;
  return makeNotationWithPosition(pos, parseUsi(usi)!, lastMoveOrDrop);
}

export function makeNotationLine(
  sfen: Sfen,
  variant: VariantKey,
  usis: Usi[],
  lastUsi?: Usi,
): MoveNotation[] {
  return makeNotationLineWithPosition(
    createPosition(sfen, variant),
    usis.map(u => parseUsi(u)!),
    lastUsi ? parseUsi(lastUsi) : undefined,
  );
}

function createPosition(sfen: Sfen, variant: VariantKey): Position {
  return parseSfen(variant, sfen, false).unwrap();
}

// create move notation in reference to node or parent node
export function usiToNotation(
  node: Tree.Node,
  parentNode: Tree.Node | undefined,
  variant: VariantKey,
  text: string,
): string {
  const matches = text.match(/\[usi:(\d*)\.?((?:\d\w|\w\*)\d\w(?:\+|=)?)\]/g);
  if (matches?.length) {
    for (const mText of matches) {
      const match = mText.match(/usi:(\d*)\.?((?:\d\w|\w\*)\d\w(?:\+|=)?)/);
      if (match) {
        const textPlyColor = plyColor(Number.parseInt(match[1]) || node.ply);
        const useParentNode = plyColor(node.ply) !== textPlyColor;
        const refNode = useParentNode && parentNode ? parentNode : node;
        const refSfen =
          !node.usi && useParentNode
            ? refNode.sfen.replace(/ (b|w) /, ` ${toBW(textPlyColor)} `)
            : refNode.sfen; // for initial node
        const moveOrDrop = match[2] && parseUsi(match[2]); // to make sure we have valid usi
        const notation =
          moveOrDrop && makeNotation(refSfen, variant, makeUsi(moveOrDrop), refNode.usi);
        if (notation) text = text.replace(mText, notation);
        else text = text.replace(mText, 'Invalid move');
      }
    }
  }
  return text;
}
