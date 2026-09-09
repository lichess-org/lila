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
