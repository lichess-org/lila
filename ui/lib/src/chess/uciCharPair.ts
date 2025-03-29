import * as co from 'chessops';

// from UciCharPair.scala in https://github.com/lichess-org/scalachess

export type UciCharPair = string;

export function pathToUcis(path: Tree.Path): Uci[] {
  const ucis: Uci[] = [];
  for (let i = 0; i < path.length; i += 2) {
    ucis.push(pairToUci(path.slice(i, i + 2)));
  }
  return ucis;
}

export function pairToUci(p: UciCharPair): Uci {
  const from = fromSquareChar(p[0])!;
  const second = p[1];

  const squareChar = fromSquareChar(second);
  if (squareChar) return from + squareChar;

  const [file, prom] = fromPromotionChar(second);
  if (file && prom) return from + file + (from[1] === '2' ? '1' : '8') + prom[0];
  return `${fromDropRoleChar(second)}@${from}`;
}

const promotableRoles: co.Role[] = ['queen', 'rook', 'bishop', 'knight', 'king'];
const dropRoles: co.Role[] = ['queen', 'rook', 'bishop', 'knight', 'pawn'];
const CHAR_SHIFT = 35;

function fromSquareChar(char: string): Uci | undefined {
  const index = char.charCodeAt(0) - CHAR_SHIFT;
  const rank = Math.floor(index / 8);
  const file = index % 8;
  return rank < 0 || rank > 7 ? undefined : co.FILE_NAMES[file] + co.RANK_NAMES[rank];
}

function fromPromotionChar(char: string): [co.FileName, co.Role] {
  const index = char.charCodeAt(0) - (CHAR_SHIFT + 64);
  const roleIndex = Math.floor(index / 8);
  const fileIndex = index % 8;
  return [co.FILE_NAMES[fileIndex], promotableRoles[roleIndex]];
}

function fromDropRoleChar(char: string): co.Role {
  const dropRoleOffset = CHAR_SHIFT + 64 + promotableRoles.length * 8;
  const index = char.charCodeAt(0) - dropRoleOffset;
  return dropRoles[index];
}
