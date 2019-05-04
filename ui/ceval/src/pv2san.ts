import { fixCrazySan, decomposeUci } from 'chess';

type Square = number;

type Piece = 'p' | 'n' | 'b' | 'r' | 'q' | 'k' |
  'P' | 'N' | 'B' | 'R' | 'Q' | 'K';

interface Board {
  turn: boolean;
  fmvn: number;
  pieces: {
    [s: number]: Piece
  };
  k?: Square;
  K?: Square;
}

function square(name: string): Square {
  return name.charCodeAt(0) - 97 + (name.charCodeAt(1) - 49) * 8;
}

function squareDist(a: Square, b: Square): number {
  let x1 = a & 7, x2 = b & 7;
  let y1 = a >> 3, y2 = b >> 3;
  return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
}

function isBlack(p: Piece): boolean {
  return p === p.toLowerCase();
}

function readFen(fen: string): Board {
  let parts = fen.split(' ');
  let board: Board = {
    pieces: {},
    turn: parts[1] === 'w',
    fmvn: parseInt(parts[5], 10) || 1
  };

  parts[0].split('/').slice(0, 8).forEach((row, y) => {
    let x = 0;
    row.split('').forEach(v => {
      if (v == '~') return;
      let nb = parseInt(v, 10);
      if (nb) x += nb;
      else {
        let square = (7 - y) * 8 + x;
        board.pieces[square] = v as Piece;
        if (v === 'k' || v === 'K') board[v] = square;
        x++;
      }
    });
  });

  return board;
}

function kingMovesTo(s: Square): Square[] {
  return [s - 1, s - 9, s - 8, s - 7, s + 1, s + 9, s + 8, s + 7].filter(
    o => o >= 0 && o < 64 && squareDist(s, o) === 1);
}

function knightMovesTo(s: Square): Square[] {
  return [s + 17, s + 15, s + 10, s + 6, s - 6, s - 10, s - 15, s - 17].filter(
    o => o >= 0 && o < 64 && squareDist(s, o) <= 2);
}

function pawnAttacksTo(turn: boolean, s: Square): Square[] {
  let left = turn ? 7 : -7;
  let right = turn ? 9 : -9;
  return [s + left, s + right].filter(
    o => o >= 0 && o < 64 && squareDist(s, o) === 1);
}

const ROOK_DELTAS = [8, 1, -8, -1];
const BISHOP_DELTAS = [9, -9, 7, -7];
const QUEEN_DELTAS = ROOK_DELTAS.concat(BISHOP_DELTAS);

function slidingMovesTo(s: Square, deltas: number[], board: Board): Square[] {
  var result: Square[] = [];
  deltas.forEach(function (delta) {
    for (var square = s + delta;
      square >= 0 && square < 64 && squareDist(square, square - delta) === 1;
      square += delta) {
      result.push(square);
      if (board.pieces[square]) break;
    }
  });
  return result;
}

function isCheck(variant: VariantKey, board: Board): boolean {
  if (variant === 'antichess' || variant == 'racingKings') return false;

  const turn = board.turn,
  ksq = turn ? board.K : board.k;

  if (typeof ksq !== 'number') return false;

  // no check when kings are touching in atomic
  if (variant === 'atomic' &&
    typeof board.k !== 'undefined' &&
    typeof board.K !== 'undefined' &&
    squareDist(board.k, board.K) <= 1)
  return false;

  const p = turn ? 'p' : 'P',
  n = turn ? 'n' : 'N',
  r = turn ? 'r' : 'R',
  b = turn ? 'b' : 'B',
  q = turn ? 'q' : 'Q';

  return (
    pawnAttacksTo(turn, ksq).some(o => board.pieces[o] === p) ||
      knightMovesTo(ksq).some(o => board.pieces[o] === n) ||
        slidingMovesTo(ksq, ROOK_DELTAS, board).some(o => board.pieces[o] === r || board.pieces[o] === q) ||
          slidingMovesTo(ksq, BISHOP_DELTAS, board).some(o => board.pieces[o] === b || board.pieces[o] === q));
}

function makeMove(variant: VariantKey, board: Board, uci: string) {
  if (!board.turn) board.fmvn++;
  const turn = board.turn = !board.turn;

  if (uci.includes('@')) {
    board.pieces[square(uci.slice(2, 4))] = (turn ? uci[0].toLowerCase() : uci[0]) as Piece;
    return;
  }

  const move = decomposeUci(uci),
  from = square(move[0]),
  p = board.pieces[from];

  let to = square(move[1]),
  capture = board.pieces[to];

  if (p === 'p' || p === 'P') {
    if (uci[0] !== uci[2] && !capture) {
      // en passant
      delete board.pieces[to + (turn ? 8 : -8)];
      capture = turn ? 'p' : 'P';
    }
  }

  if (p === 'k' || p === 'K') {
    // castling
    var frCastle = capture && isBlack(p) === isBlack(capture);
    if (frCastle || squareDist(from, to) > 1) {
      delete board.pieces[from];
      if (frCastle) delete board.pieces[to];
      if (to < from) {
        if (!frCastle) delete board.pieces[turn ? square('a8') : square('a1')];
        to = turn ? square('c8') : square('c1');
        board.pieces[to + 1] = turn ? 'r' : 'R';
      }
      else {
        if (!frCastle) delete board.pieces[turn ? square('h8') : square('h1')];
        to = turn ? square('g8') : square('g1');
        board.pieces[to - 1] = turn ? 'r' : 'R';
      }
      board.pieces[to] = p;
      board[p] = to;
      return;
    }
    board[p] = to;
  }

  if (move[2]) board.pieces[to] = (turn ? move[2] : move[2].toUpperCase()) as Piece;
  else board.pieces[to] = p;
  delete board.pieces[from];

  // atomic explosion
  if (variant === 'atomic' && capture) {
    delete board.pieces[to];
    kingMovesTo(to).forEach(function (o) {
      if (board.pieces[o] !== 'p' && board.pieces[o] !== 'P') delete board.pieces[o];
    });
  }
}

function san(board: Board, uci: string): string  {
  if (uci.includes('@')) return fixCrazySan(uci);

  var move = decomposeUci(uci);
  var from = square(move[0]);
  var to = square(move[1]);
  var p = board.pieces[from];
  if (!p) return '--';
  var d = board.pieces[to];
  var pt = p.toLowerCase();

  // pawn moves
  if (pt === 'p') {
    let san;
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

  var san = pt.toUpperCase();

  // disambiguate normal moves
  var candidates: Square[] = [];
  if (pt == 'k') candidates = kingMovesTo(to);
  else if (pt == 'n') candidates = knightMovesTo(to);
  else if (pt == 'r') candidates = slidingMovesTo(to, ROOK_DELTAS, board);
  else if (pt == 'b') candidates = slidingMovesTo(to, BISHOP_DELTAS, board);
  else if (pt == 'q') candidates = slidingMovesTo(to, QUEEN_DELTAS, board);

  let rank = false, file = false, i: number;
  for (i = 0; i < candidates.length; i++) {
    if (candidates[i] === from || board.pieces[candidates[i]] !== p) continue;
    if ((from >> 3) === (candidates[i] >> 3)) file = true;
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

export default function(variant: VariantKey, fen: string, threat: boolean, moves: string[], mate?: number): string {
  const board = readFen(fen);
  if (threat) board.turn = !board.turn;
  const turn = board.turn;

  let first = true,
  s: string,
  line = moves.map(function(uci) {
    s = '';
    if (board.turn) s = board.fmvn + '. ';
    else if (first) s = board.fmvn + '... ';
    first = false;
    s += san(board, uci);
    makeMove(variant, board, uci);
    if (isCheck(variant, board)) s += '+';
    return s;
  }).join(' ');

  if (mate) {
    let matePlies = mate * 2;
    if (mate > 0 === turn) matePlies--;
    if (moves.length >= matePlies) line = line.replace(/\+?$/, '#');
  }

  return line;
}
