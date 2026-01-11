// id_to_FEN

// FEN_to_id

// computeCastlingSquares(from position id)

export function chess960IdToFEN(id: number): string {
  if (id < 0 || id > 959) {
    throw new Error('Chess960 id must be between 0 and 959');
  }

  // Back rank files 0..7 (a..h)
  const backRank: string[] = Array(8).fill('');

  const place = (piece: string, file: number) => {
    backRank[file] = piece;
  };

  // Bishops
  const darkSquares = [0, 2, 4, 6];
  const lightSquares = [1, 3, 5, 7];

  let n = id;

  const b1 = lightSquares[n % 4];
  n = Math.floor(n / 4);

  const b2 = darkSquares[n % 4];
  n = Math.floor(n / 4);

  place('B', b1);
  place('B', b2);

  // Queen
  const freeSquares = () => [...Array(8).keys()].filter(i => backRank[i] === '');

  const q = freeSquares()[n % 6];
  n = Math.floor(n / 6);
  place('Q', q);

  // King, knigts and rooks
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

  // Build FEN ranks
  const rank1 = backRank.join('');
  const rank2 = 'PPPPPPPP';
  const rank7 = 'pppppppp';
  const rank8 = rank1.toLowerCase();

  const board = `${rank8}/${rank7}/8/8/8/8/${rank2}/${rank1}`;

  return `${board} w KQkq - 0 1`;
}
