function fixCrazySan(san) {
  return san[0] === 'P' ? san.slice(1) : san;
}

function decomposeUci(uci) {
  return [uci.slice(0, 2), uci.slice(2, 4), uci.slice(4, 5)];
}

function square(name) {
  return name.charCodeAt(0) - 97 + (name.charCodeAt(1) - 49) * 8;
}

function squareDist(a, b) {
  var x1 = a & 7,
    x2 = b & 7;
  var y1 = a >> 3,
    y2 = b >> 3;
  return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
}

function isBlack(p) {
  return p === p.toLowerCase();
}

function readFen(fen) {
  var parts = fen.split(' ');
  var board = {
    pieces: {},
    turn: parts[1] === 'w'
  };

  parts[0].split('/').slice(0, 8).forEach(function(row, y) {
    var x = 0;
    row.split('').forEach(function(v) {
      if (v == '~') return;
      var nb = parseInt(v, 10);
      if (nb) x += nb;
      else {
        var square = (7 - y) * 8 + x;
        board.pieces[square] = v;
        if (v === 'k' || v === 'K') board[v] = square;
        x++;
      }
    });
  });

  return board;
}

function kingMovesTo(s) {
  return [s - 1, s - 9, s - 8, s - 7, s + 1, s + 9, s + 8, s + 7].filter(function(o) {
    return o >= 0 && o < 64 && squareDist(s, o) === 1;
  });
}

function knightMovesTo(s) {
  return [s + 17, s + 15, s + 10, s + 6, s - 6, s - 10, s - 15, s - 17].filter(function(o) {
    return o >= 0 && o < 64 && squareDist(s, o) <= 2;
  });
}

var ROOK_DELTAS = [8, 1, -8, -1];
var BISHOP_DELTAS = [9, -9, 7, -7];
var QUEEN_DELTAS = ROOK_DELTAS.concat(BISHOP_DELTAS);

function slidingMovesTo(s, deltas, board) {
  var result = [];
  deltas.forEach(function(delta) {
    for (var square = s + delta; square >= 0 && square < 64 && squareDist(square, square - delta) === 1; square += delta) {
      result.push(square);
      if (board.pieces[square]) break;
    }
  });
  return result;
}

function sanOf(board, uci) {
  if (uci.includes('@')) return fixCrazySan(uci);

  var move = decomposeUci(uci);
  var from = square(move[0]);
  var to = square(move[1]);
  var p = board.pieces[from];
  var d = board.pieces[to];
  var pt = board.pieces[from].toLowerCase();

  // pawn moves
  if (pt === 'p') {
    var san;
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
  var candidates = [];
  if (pt == 'k') candidates = kingMovesTo(to);
  else if (pt == 'n') candidates = knightMovesTo(to);
  else if (pt == 'r') candidates = slidingMovesTo(to, ROOK_DELTAS, board);
  else if (pt == 'b') candidates = slidingMovesTo(to, BISHOP_DELTAS, board);
  else if (pt == 'q') candidates = slidingMovesTo(to, QUEEN_DELTAS, board);

  var rank = false,
    file = false;
  for (var i = 0; i < candidates.length; i++) {
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

export default function sanWriter(fen, ucis) {
  var board = readFen(fen);
  var sans = {}
  ucis.forEach(function(uci) {
    var san = sanOf(board, uci);
    sans[san] = uci;
    if (san.includes('x')) sans[san.replace('x', '')] = uci;
  });
  return sans;
}
