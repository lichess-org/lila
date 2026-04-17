// Best description is found at https://fr.wikipedia.org/wiki/%C3%89checs_al%C3%A9atoires_Fischer#Identification_des_positions_initiales

import { FILE_NAMES } from 'chessops';

// Square on rank 1
const darkSquares = [0, 2, 4, 6];
const lightSquares = [1, 3, 5, 7];
// King, rooks and knights
const KRN: ('K' | 'R' | 'N')[][] = [
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

export function chess960IdToFEN(id: number): FEN {
  const rank1 = chess960IdToRank(id);
  return `${rank1.toLowerCase()}/pppppppp/8/8/8/8/PPPPPPPP/${rank1} w KQkq - 0 1`;
}

interface CastlingSquares {
  king: string;
  rookQ: string;
  rookK: string;
}

export function chess960CastlingSquares(id: number | undefined): ByColor<CastlingSquares> {
  const rank1 = chess960IdToRank(id ?? 518); // default to standard chess

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

export const randomPositionId = (): number => Math.floor(Math.random() * 960);

export const isValidPositionId = (id: number): boolean => Number.isInteger(id) && id >= 0 && id <= 959;

export function fenToChess960Id(fen: FEN): number | undefined {
  const parts = fen.split(' ');
  if (parts.length < 1) return undefined;
  const ranks = parts[0].split('/');
  if (ranks.length !== 8) return undefined;
  const rank = ranks[7];
  if (rank.toLowerCase() !== ranks[0] || rank.length !== 8 || rank !== rank.toUpperCase()) return undefined;

  const king = rank.indexOf('K');
  const queen = rank.indexOf('Q');
  const rook1 = rank.indexOf('R');
  const rook2 = rank.lastIndexOf('R');
  const bishop1 = rank.indexOf('B');
  const bishop2 = rank.lastIndexOf('B');
  const knight1 = rank.indexOf('N');
  const knight2 = rank.lastIndexOf('N');
  if ([king, queen, rook1, rook2, bishop1, bishop2, knight1, knight2].includes(-1)) {
    return undefined;
  }
  // Bishops
  const lightBishopFile = bishop1 % 2 === 1 ? bishop1 : bishop2;
  const darkBishopFile = bishop1 % 2 === 0 ? bishop1 : bishop2;
  const lightBishopIndex = lightSquares.indexOf(lightBishopFile);
  const darkBishopIndex = darkSquares.indexOf(darkBishopFile);
  if (lightBishopIndex === -1 || darkBishopIndex === -1) return undefined;

  // Queen
  const freeSquaresAfterBishops = [...Array(8).keys()].filter(
    i => ![lightBishopFile, darkBishopFile].includes(i),
  );
  const queenFile = queen;
  const queenIndex = freeSquaresAfterBishops.indexOf(queenFile);
  if (queenIndex === -1 || queenIndex >= 6) return undefined;

  // Remaining pieces: K, R, R, N, N
  const remainingPieces = [...Array(8).keys()]
    .filter(i => ![lightBishopFile, darkBishopFile, queenFile].includes(i))
    .map(file => rank[file]);
  const krnIndex = KRN.findIndex(arr => arr.every((p, i) => p === remainingPieces[i]));
  return krnIndex === -1
    ? undefined
    : lightBishopIndex + 4 * darkBishopIndex + 16 * queenIndex + 96 * krnIndex;
}

function chess960IdToRank(id: number): string {
  if (!isValidPositionId(id)) {
    throw new Error('Chess960 id must be between 0 and 959');
  }

  // Back rank files 0..7 (a..h)
  const backRank: string[] = Array(8).fill('');

  const place = (piece: string, file: number) => {
    backRank[file] = piece;
  };

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

  const remaining = freeSquares();
  for (let i = 0; i < 5; i++) {
    place(KRN[n][i], remaining[i]);
  }

  return backRank.join('');
}
