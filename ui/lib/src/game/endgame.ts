import type { DrawShape } from '@lichess-org/chessground/draw';

import { endgameGlyphs } from './glyphs';
import type { StatusName } from './status';

export function findKingSquare(fen: FEN, color: Color): Key | undefined {
  const board = fen.split(' ')[0].split('/'),
    king = color === 'white' ? 'K' : 'k';

  for (let rank = 0; rank < board.length; rank++) {
    let file = 0;
    for (const char of board[rank]) {
      if (/\d/.test(char)) file += Number(char);
      else {
        if (char === king) return `${String.fromCharCode(97 + file)}${8 - rank}` as Key;
        file++;
      }
    }
  }
  return undefined;
}

type EndgameGlyph = keyof typeof endgameGlyphs;

const loserGlyph = (status: StatusName | undefined): EndgameGlyph =>
  status === 'mate'
    ? 'mate'
    : status === 'resign'
      ? 'resign'
      : status === 'timeout' || status === 'outoftime' || status === 'noStart'
        ? 'timeout'
        : 'unknown';

const isDraw = (status: StatusName | undefined): boolean =>
  status === 'draw' ||
  status === 'stalemate' ||
  status === 'insufficientMaterialClaim' ||
  status === 'unknownFinish' ||
  status === 'variantEnd';

export function endgameShapes(
  fen: FEN,
  winner: Color | undefined,
  status: StatusName | undefined,
): DrawShape[] {
  const shapes: DrawShape[] = [];
  const add = (color: Color, glyph: EndgameGlyph) => {
    const king = findKingSquare(fen, color);
    if (king) shapes.push({ orig: king, customSvg: { html: endgameGlyphs[glyph](0) } });
  };

  if (winner) {
    add(winner, 'win');
    add(winner === 'white' ? 'black' : 'white', loserGlyph(status));
  } else if (isDraw(status)) {
    add('white', 'draw');
    add('black', 'draw');
  }
  return shapes;
}
