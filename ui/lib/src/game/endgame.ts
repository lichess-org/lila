import type { DrawShape } from '@lichess-org/chessground/draw';
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

export function endgameHighlights(
  fen: FEN,
  winner: Color | undefined,
  status: StatusName | undefined,
): Map<Key, string> {
  const highlights = new Map<Key, string>();
  if (winner) {
    const winnerKing = findKingSquare(fen, winner);
    if (winnerKing) highlights.set(winnerKing, 'king-win');

    const loser = winner === 'white' ? 'black' : 'white',
      loserKing = findKingSquare(fen, loser);
    if (loserKing) {
      const loserClass =
        status === 'mate'
          ? 'king-lose-mate'
          : status === 'resign'
            ? 'king-lose-resign'
            : status === 'outoftime' || status === 'timeout'
              ? 'king-lose-timeout'
              : 'king-lose';
      highlights.set(loserKing, loserClass);
    }
  } else if (
    status === 'draw' ||
    status === 'stalemate' ||
    status === 'insufficientMaterialClaim' ||
    status === 'outoftime' ||
    status === 'timeout'
  ) {
    for (const color of ['white', 'black'] as const) {
      const king = findKingSquare(fen, color);
      if (king) highlights.set(king, 'king-draw');
    }
  }
  return highlights;
}

const endgameSvg = (background: string, icon: string): string =>
  `<g><circle cx="78" cy="22" r="20" fill="${background}" stroke="#fff" stroke-width="4"/>${icon}</g>`;

const endgameIcons = {
  win: '<path fill="#fff" d="M64 10h5v8h18v-8h5v12q0 8-12 10v5h7v5H69v-5h7v-5q-12-2-12-10zm5 23h18v4H69z"/>',
  mate: '<path fill="#fff" d="m65 14 4-4 9 9 9-9 4 4-9 9 9 9-4 4-9-9-9 9-4-4 9-9z"/>',
  lose: '<path fill="#fff" d="M68 10h7v7h7v7h-7v7h-7v-7h-7v-7h7z"/>',
  resign: '<path fill="#fff" d="M65 9h5v25h-5zM70 11h18l-7 7 7 7H70z"/>',
  timeout:
    '<path fill="#fff" d="M68 10h8v4h-8zM78 16l4 4-10 10-4-4zM66 17a11 11 0 1 0 16 16l-4-4a5 5 0 1 1-8-8z"/>',
  draw: '<path fill="#fff" d="M62 15h27v6H62zM62 27h27v6H62z"/>',
} as const;

const endgameSvgFor = (kind: keyof typeof endgameIcons): string =>
  endgameSvg(kind === 'win' ? '#22ac38' : kind === 'draw' ? '#c8a829' : '#df5353', endgameIcons[kind]);

export function endgameShapes(
  fen: FEN,
  winner: Color | undefined,
  status: StatusName | undefined,
): DrawShape[] {
  const shapes: DrawShape[] = [];
  const add = (color: Color, kind: keyof typeof endgameIcons) => {
    const king = findKingSquare(fen, color);
    if (king) shapes.push({ orig: king, customSvg: { html: endgameSvgFor(kind) } });
  };

  if (winner) {
    add(winner, 'win');
    add(
      winner === 'white' ? 'black' : 'white',
      status === 'mate'
        ? 'mate'
        : status === 'resign'
          ? 'resign'
          : status === 'timeout' || status === 'outoftime'
            ? 'timeout'
            : 'lose',
    );
  } else if (
    status === 'draw' ||
    status === 'stalemate' ||
    status === 'insufficientMaterialClaim' ||
    status === 'outoftime' ||
    status === 'timeout'
  ) {
    add('white', 'draw');
    add('black', 'draw');
  }
  return shapes;
}
