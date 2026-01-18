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
  const rank2 = 'PPPPPPPP';
  const rank7 = 'pppppppp';
  const rank8 = rank1.toLowerCase();

  const board = `${rank8}/${rank7}/8/8/8/8/${rank2}/${rank1}`;

  return `${board} w KQkq - 0 1`;
}

interface CastlingSquares {
  king: string;
  rookQ: string;
  rookK: string;
}

export function chess960CastlingSquares(id: number | undefined): ByColor<CastlingSquares> {
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

export const randomPositionId = (): number => Math.floor(Math.random() * 960);

export const isValidPositionId = (id: number): boolean => Number.isInteger(id) && id >= 0 && id <= 959;

export function fenToChess960Id(fen: FEN): number | undefined {
  const parts = fen.split(' ');
  if (parts.length < 1) return undefined;
  const ranks = parts[0].split('/');
  if (ranks.length !== 8) return undefined;
  const rank1 = ranks[7];
  const rank8 = ranks[0];
  if (rank1.toLowerCase() !== rank8 || rank1.length !== 8 || rank1 !== rank1.toUpperCase()) return undefined;

  const king = rank1.indexOf('K');
  const queen = rank1.indexOf('Q');
  const rook1 = rank1.indexOf('R');
  const rook2 = rank1.lastIndexOf('R');
  const bishop1 = rank1.indexOf('B');
  const bishop2 = rank1.lastIndexOf('B');
  const knight1 = rank1.indexOf('N');
  const knight2 = rank1.lastIndexOf('N');
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
  const occupiedAfterBishops = [lightBishopFile, darkBishopFile];
  const freeSquaresAfterBishops = [...Array(8).keys()].filter(i => !occupiedAfterBishops.includes(i));
  const queenFile = queen;
  const queenIndex = freeSquaresAfterBishops.indexOf(queenFile);
  if (queenIndex === -1 || queenIndex >= 6) return undefined;

  // Remaining pieces: K, R, R, N, N
  const occupiedAfterQueenBishops = [lightBishopFile, darkBishopFile, queenFile];
  const freeSquaresAfterQueenBishops = [...Array(8).keys()].filter(
    i => !occupiedAfterQueenBishops.includes(i),
  );
  const remainingPieces = freeSquaresAfterQueenBishops.map(file => rank1[file]);

  const krnIndex = KRN.findIndex(arr => {
    for (let i = 0; i < arr.length; i++) {
      if (arr[i] !== remainingPieces[i]) return false;
    }
    return true;
  });
  if (krnIndex === -1) return undefined;

  return lightBishopIndex + 4 * darkBishopIndex + 16 * queenIndex + 96 * krnIndex;
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
