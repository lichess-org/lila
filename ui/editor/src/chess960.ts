import { FILE_NAMES } from 'chessops';

export function chess960IdToFEN(id: number): string {
  const rank1 = chess960IdToRank(id);
  const rank2 = 'PPPPPPPP';
  const rank7 = 'pppppppp';
  const rank8 = rank1.toLowerCase();

  const board = `${rank8}/${rank7}/8/8/8/8/${rank2}/${rank1}`;

  return `${board} w KQkq - 0 1`;
}

export function chess960CastlingSquares(id: number | undefined): {
  white: { king: string; rookQ: string; rookK: string };
  black: { king: string; rookQ: string; rookK: string };
} {
  const rank1 = chess960IdToRank(id === undefined ? 518 : id); // default to standard chess

  const kingFile = rank1.indexOf('K');
  const rookKFile = rank1.lastIndexOf('R');
  const rookQFile = rank1.indexOf('R');

  return {
    white: {
      king: FILE_NAMES[kingFile] + '1',
      rookK: FILE_NAMES[rookKFile] + '1',
      rookQ: FILE_NAMES[rookQFile] + '1',
    },
    black: {
      king: FILE_NAMES[kingFile] + '8',
      rookK: FILE_NAMES[rookKFile] + '8',
      rookQ: FILE_NAMES[rookQFile] + '8',
    },
  };
}

function chess960IdToRank(id: number): string {
  if (id < 0 || id > 959) {
    throw new Error('Chess960 id must be between 0 and 959');
  }

  // Back rank files 0..7 (a..h)
  const backRank: string[] = Array(8).fill('');

  const place = (piece: string, file: number) => {
    backRank[file] = piece;
  };

  const darkSquares = [0, 2, 4, 6];
  const lightSquares = [1, 3, 5, 7];

  let n = id;

  // White bishop
  const b1 = lightSquares[n % 4];
  n = Math.floor(n / 4);
  place('B', b1);

  // Black bishop
  const b2 = darkSquares[n % 4];
  n = Math.floor(n / 4);
  place('B', b2);

  // Queen
  const freeSquares = () => [...Array(8).keys()].filter(i => backRank[i] === '');
  const q = freeSquares()[n % 6];
  n = Math.floor(n / 6);
  place('Q', q);

  // King, rooks and knights
  const KRN = [
    ['N', 'N', 'R', 'K', 'R'],
    ['N', 'R', 'N', 'K', 'R'],
    ['N', 'R', 'K', 'N', 'R'],
    ['N', 'R', 'K', 'R', 'N'],
    ['R', 'N', 'N', 'K', 'R'],
    ['R', 'N', 'K', 'N', 'R'],
    ['R', 'N', 'K', 'R', 'N'],
    ['R', 'K', 'N', 'N', 'R'],
    ['R', 'K', 'N', 'R', 'N'],
    ['R', 'K', 'R', 'N', 'N'],
  ];

  const remaining = freeSquares();
  for (let i = 0; i < 5; i++) {
    place(KRN[n][i], remaining[i]);
  }

  return backRank.join('');
}
