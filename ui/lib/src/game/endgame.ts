import type { DrawShape } from '@lichess-org/chessground/draw';
import type { Outcome } from 'chessops';

import { endgameGlyphs } from './glyphs';
import type { StatusName } from './status';

export interface EndgameResult {
  winner?: Color;
  status?: StatusName;
}

export function endgameResult(
  outcome: Outcome | undefined,
  isMate: boolean,
  mateWinner: Color,
  isGameEnd: boolean,
  gameWinner: Color | undefined,
  gameStatus: StatusName,
): EndgameResult {
  if (outcome) return { winner: outcome.winner, status: outcome.winner ? 'mate' : 'stalemate' };
  if (isMate) return { winner: mateWinner, status: 'mate' };
  if (isGameEnd) return { winner: gameWinner, status: gameStatus };
  return {};
}

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
      : status === 'timeout'
        ? 'abandoned'
        : status === 'outoftime'
          ? 'timeout'
          : 'unknown';

const drawGlyph = (status: StatusName | undefined): EndgameGlyph | undefined =>
  status === 'stalemate'
    ? 'stalemate'
    : status === 'draw' || status === 'insufficientMaterialClaim' || status === 'outoftime'
      ? 'draw'
      : undefined;

export function endgameShapes(
  fen: FEN,
  winner: Color | undefined,
  status: StatusName | undefined,
): DrawShape[] {
  if (status === 'aborted' || status === 'noStart') return [];

  const shapes: DrawShape[] = [];
  const add = (color: Color, glyph: EndgameGlyph) => {
    const king = findKingSquare(fen, color);
    if (king)
      shapes.push({
        orig: king,
        brush: '',
        customSvg: { html: endgameGlyphs[glyph](0), center: 'orig' },
      });
  };

  if (winner) {
    add(winner, 'win');
    add(winner === 'white' ? 'black' : 'white', loserGlyph(status));
  } else {
    const glyph = drawGlyph(status);
    if (glyph) {
      add('white', glyph);
      add('black', glyph);
    }
  }
  return shapes;
}
