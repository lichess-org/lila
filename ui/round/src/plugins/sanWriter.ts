type Board = { pieces: { [key: number]: string }; turn: boolean };
export type SanToUci = { [key: string]: Uci };

function fixCrazySan(san: string) {
  return san[0] === 'P' ? san.slice(1) : san;
}

function decomposeUci(uci: string) {
  return [uci.slice(0, 2), uci.slice(2, 4), uci.slice(4, 5)];
}

function square(name: string) {
  return name.charCodeAt(0) - 97 + (name.charCodeAt(1) - 49) * 8;
}

function squareDist(a: number, b: number) {
  const x1 = a & 7,
    x2 = b & 7;
  const y1 = a >> 3,
    y2 = b >> 3;
  return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
}

function isBlack(p: string) {
  return p === p.toLowerCase();
}

function readFen(fen: string) {
  const parts = fen.split(' '),
    board: Board = {
      pieces: {},
      turn: parts[1] === 'w',
    };

  parts[0]
    .split('/')
    .slice(0, 8)
    .forEach((row, y) => {
      let x = 0;
      row.split('').forEach(v => {
        if (v == '~') return;
        const nb = parseInt(v, 10);
        if (nb) x += nb;
        else {
          board.pieces[(7 - y) * 8 + x] = v;
          x++;
        }
      });
    });

  return board;
}

function kingMovesTo(s: number) {
  return [s - 1, s - 9, s - 8, s - 7, s + 1, s + 9, s + 8, s + 7].filter(function (o) {
    return o >= 0 && o < 64 && squareDist(s, o) === 1;
  });
}

function knightMovesTo(s: number) {
  return [s + 17, s + 15, s + 10, s + 6, s - 6, s - 10, s - 15, s - 17].filter(function (o) {
    return o >= 0 && o < 64 && squareDist(s, o) <= 2;
  });
}

const ROOK_DELTAS = [8, 1, -8, -1];
const BISHOP_DELTAS = [9, -9, 7, -7];
const QUEEN_DELTAS = ROOK_DELTAS.concat(BISHOP_DELTAS);

function slidingMovesTo(s: number, deltas: number[], board: Board): number[] {
  const result: number[] = [];
  deltas.forEach(function (delta) {
    for (
      let square = s + delta;
      square >= 0 && square < 64 && squareDist(square, square - delta) === 1;
      square += delta
    ) {
      result.push(square);
      if (board.pieces[square]) break;
    }
  });
  return result;
}

function sanOf(board: Board, uci: string) {
  if (uci.includes('@')) return fixCrazySan(uci);

  const move = decomposeUci(uci);
  const from = square(move[0]);
  const to = square(move[1]);
  const p = board.pieces[from];
  const d = board.pieces[to];
  const pt = board.pieces[from].toLowerCase();

  // pawn moves
  if (pt === 'p') {
    let san: string;
    if (uci[0] === uci[2]) san = move[1];
    else san = uci[0] + 'x' + move[1];
    if (move[2]) san += '=' + move[2].toUpperCase();
    return san;
  }

  // castling
  if (pt == 'k' && ((d && isBlack(p) === isBlack(d)) || squareDist(from, to) > 1)) {
    if (to < from) return 'O-O-O';
    else return 'O-O';
  }

  let san = pt.toUpperCase();

  // disambiguate normal moves
  let candidates: number[] = [];
  if (pt == 'k') candidates = kingMovesTo(to);
  else if (pt == 'n') candidates = knightMovesTo(to);
  else if (pt == 'r') candidates = slidingMovesTo(to, ROOK_DELTAS, board);
  else if (pt == 'b') candidates = slidingMovesTo(to, BISHOP_DELTAS, board);
  else if (pt == 'q') candidates = slidingMovesTo(to, QUEEN_DELTAS, board);

  let rank = false,
    file = false;
  for (let i = 0; i < candidates.length; i++) {
    if (candidates[i] === from || board.pieces[candidates[i]] !== p) continue;
    if (from >> 3 === candidates[i] >> 3) file = true;
    if ((from & 7) === (candidates[i] & 7)) rank = true;
    else file = true;
  }
  if (file) san += uci[0];
  if (rank) san += uci[1];

  // target
  if (d) san += 'x';
  san += move[1];
  return san;
}

export function sanWriter(fen: string, ucis: string[]): SanToUci {
  const board = readFen(fen);
  const sans: SanToUci = {};
  ucis.forEach(function (uci) {
    const san = sanOf(board, uci);
    sans[san] = uci;
    if (san.includes('x')) sans[san.replace('x', '')] = uci;
  });
  return sans;
}
