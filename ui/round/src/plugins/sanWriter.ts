function decomposeUci(uci) {
  return [uci.slice(0, 2), uci.slice(2, 4), uci.slice(4, 5)];
}

function square(name) {
  return name.charCodeAt(0) - 49 + (name.charCodeAt(1) - 49) * 9;
}

function squareDist(a, b) {
  var x1 = a % 9,
    x2 = b % 9;
  var y1 = a / 9,
    y2 = b / 9;
  return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
}

function readFen(fen) {
  var parts = fen.split(' ');
  var board = {
    pieces: {},
    turn: parts[1] === 'b',
  };

  parts[0]
    .split('/')
    .slice(0, 9)
    .forEach(function (row, y) {
      var x = 8;
      row.split('').forEach(function (v) {
        var nb = parseInt(v, 10);
        if (nb) x -= nb;
        else {
          var square = y * 9 + x;
          board.pieces[square] = v;
          if (v === 'k' || v === 'K') board[v] = square;
          x--;
        }
      });
    });

  return board;
}

function kingMovesTo(s) {
  return [s - 1, s - 10, s - 9, s - 8, s + 1, s + 10, s + 9, s + 8].filter(function (o) {
    return o >= 0 && o < 81 && squareDist(s, o) === 1;
  });
}

function pawnMovesTo(s) {
  return [s + 9, s - 9].filter(function (o) {
    return o >= 0 && o < 81;
  });
}

function knightMovesTo(s) {
  return [s + 19, s + 17, s - 17, s - 19].filter(function (o) {
    return o >= 0 && o < 81 && squareDist(s, o) <= 2;
  });
}

var ROOK_DELTAS = [9, 1, -9, -1];
var BISHOP_DELTAS = [10, -10, 8, -8];

function slidingMovesTo(s: number, deltas: number[], board): number[] {
  var result: number[] = [];
  deltas.forEach(function (delta) {
    for (
      var square = s + delta;
      square >= 0 && square < 81 && squareDist(square, square - delta) === 1;
      square += delta
    ) {
      result.push(square);
      if (board.pieces[square]) break;
    }
  });
  return result;
}

function sanOf(board, uci) {
  if (uci.includes('*')) return uci;

  var move = decomposeUci(uci);
  var from = square(move[0]);
  var to = square(move[1]);
  var p = board.pieces[from];
  var d = board.pieces[to];
  var pt = board.pieces[from].toLowerCase();
  var san = pt.toUpperCase();

  // disambiguate normal moves
  var candidates: number[] = [];
  if (pt == 'k') candidates = kingMovesTo(to);
  else if (pt == 'p') candidates = pawnMovesTo(to);
  else if (pt == 'n') candidates = knightMovesTo(to);
  else if (pt == 'r') candidates = slidingMovesTo(to, ROOK_DELTAS, board);
  else if (pt == 'b') candidates = slidingMovesTo(to, BISHOP_DELTAS, board);

  var rank = false,
    file = false;
  for (var i = 0; i < candidates.length; i++) {
    if (candidates[i] === from || board.pieces[candidates[i]] !== p) continue;
    if (from / 9 === candidates[i] / 9) file = true;
    if ((from % 9) === (candidates[i] % 9)) rank = true;
    else file = true;
  }
  if (file) san += uci[0];
  if (rank) san += uci[1];

  // target
  if (d) san += 'x';
  san += move[1];

  // (un)promotion
  if (move[2]) san += move[2];
  return san;
}

export default function sanWriter(fen, ucis) {
  var board = readFen(fen);
  var sans = {};
  ucis.forEach(function (uci) {
    var san = sanOf(board, uci);
    sans[san] = uci;
    if (san.includes('x')) sans[san.replace('x', '')] = uci;
  });
  return sans;
}
