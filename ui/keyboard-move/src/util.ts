import { defined } from 'common/common';
import { makeJapaneseMoveOrDrop } from 'shogiops/notation/japanese';
import { aimingAt, csaToRole, fromKanjiDigit, kanjiToRole } from 'shogiops/notation/util';
import { forsythToRole } from 'shogiops/sfen';
import type { SquareSet } from 'shogiops/square-set';
import type { MoveOrDrop, Role, Square } from 'shogiops/types';
import { parseSquareName } from 'shogiops/util';
import type { Position } from 'shogiops/variant/position';
import { pieceCanPromote } from 'shogiops/variant/util';

export const fileR: RegExp = /(?:[１２３４５６７８９]|[一二三四五六七八九]|[1-9])/,
  rankR: RegExp = /(?:[１２３４５６７８９]|[一二三四五六七八九]|[1-9]|[a-i])/,
  keyR: RegExp = new RegExp(`${fileR.source}${rankR.source}`, 'g'),
  japaneseAmbiguitiesR: RegExp = /左|右|上|行|引|寄|直/,
  allRolesR: RegExp =
    /fu|kyou|kyoo|kyo|ky|kei|ke|gin|gi|kin|ki|kaku|ka|hi|to|ny|nk|ng|uma|um|ryuu|ryu|ry|gyoku|ou|p|l|n|s|g|b|r|k|\+p|\+l|\+n|\+s|\+b|\+r|歩|香|桂|銀|金|角|飛|と|成香|成桂|成銀|馬|龍|王|玉|d|h|t|o/,
  KKlastDestR: RegExp = new RegExp(`^(?:${allRolesR.source})x$`);

export function toMoveOrDrop(str: string, pos: Position): MoveOrDrop | undefined {
  const sqs = regexMatchAllSquares(str),
    unpromotion = str.includes('不成') || str.endsWith('='),
    forceDrop = str.includes('*') || str.includes('打');

  if (sqs.length) {
    if (sqs.length === 2) {
      const piece = pos.board.get(sqs[0]);
      if (piece) {
        const move = {
          from: sqs[0],
          to: sqs[1],
          promotion: pieceCanPromote(pos.rules)(piece, sqs[0], sqs[1], undefined) && !unpromotion,
        };
        return pos.isLegal(move) ? move : undefined;
      }
      const pieceReversed = pos.board.get(sqs[1]);
      if (pieceReversed) {
        const move = {
          from: sqs[1],
          to: sqs[0],
          promotion:
            pieceCanPromote(pos.rules)(pieceReversed, sqs[1], sqs[0], undefined) && !unpromotion,
        };
        return pos.isLegal(move) ? move : undefined;
      }
    } else if (sqs.length === 1) {
      const keyChar = str.match(keyR)![0],
        roleChar = str.replace(keyChar, '').match(allRolesR),
        role = roleChar && toRole(pos.rules, roleChar[0]);

      if (role) {
        const candidates = allCandidates(sqs[0], role, pos),
          piece = { color: pos.turn, role };
        if ((forceDrop || candidates.isEmpty()) && pos.dropDests(piece).has(sqs[0])) {
          const drop = { role, to: sqs[0] };
          return pos.isLegal(drop) ? drop : undefined;
        } else if (candidates.isSingleSquare())
          return {
            from: candidates.first()!,
            to: sqs[0],
            promotion:
              pieceCanPromote(pos.rules)(piece, candidates.first()!, sqs[0], undefined) &&
              !unpromotion,
          };
        else if (japaneseAmbiguitiesR.test(str)) {
          const amb = str.match(japaneseAmbiguitiesR)!;

          for (const c of candidates) {
            const jpMove = makeJapaneseMoveOrDrop(pos, { from: c, to: sqs[0] })!;
            if (amb.every(a => jpMove.includes(a)))
              return {
                from: c,
                to: sqs[0],
                promotion: pieceCanPromote(pos.rules)(piece, c, sqs[0], undefined) && !unpromotion,
              };
          }
        }
      }
    }
  }
  return;
}

export function regexMatchAllSquares(str: string): Square[] {
  const matches: Square[] = [];
  let match = keyR.exec(str);
  while (match) {
    const sq = toSquare(match[0]);
    if (defined(sq)) matches.push(sq);
    match = keyR.exec(str);
  }
  return matches;
}

export function fixDigits(str: string): string {
  return str
    .split('')
    .map(c => {
      const charCode = c.charCodeAt(0);
      if (charCode >= 0xff10 && charCode <= 0xff19) {
        return String.fromCharCode(charCode - 0xfee0);
      }
      return fromKanjiDigit(c) || c;
    })
    .join('');
}

export function toSquare(str: string): Square | undefined {
  if (str.length !== 2) return;
  const mapped = fixDigits(str),
    secondDigit = Number.parseInt(mapped[1]);

  const numberLetter = secondDigit ? mapped[0] + String.fromCharCode(96 + secondDigit) : mapped,
    parsed = parseSquareName(numberLetter);
  if (parsed !== undefined) return parsed;
  else return;
}

export function toRole(variant: VariantKey, str: string): Role | undefined {
  if (str.length >= 3) {
    switch (str) {
      case 'kyo':
      case 'kyoo':
      case 'kyou':
        return 'lance';
      case 'kei':
        return 'knight';
      case 'gin':
        return 'silver';
      case 'kin':
        return 'gold';
      case 'kaku':
        return 'bishop';
      case 'uma':
        return 'horse';
      case 'ryu':
      case 'ryuu':
        return 'dragon';
      case 'gyoku':
        return 'king';
    }
  } else if (str.length === 1) {
    str = str.replace('h', '+b').replace('d', '+r').replace('o', 'k');
    if (variant !== 'kyotoshogi') str.replace('t', '+p');
  }
  return forsythToRole(variant)(str) || kanjiToRole(str)[0] || csaToRole(str.toUpperCase());
}

export function allCandidates(dest: Square, role: Role, pos: Position): SquareSet {
  return aimingAt(pos, pos.board.pieces(pos.turn, role), dest);
}
