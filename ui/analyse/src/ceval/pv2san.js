var util = require('../util');

function square(name) {
  return name.charCodeAt(0) - 97 + (name.charCodeAt(1) - 49) * 8;
}

function squareDist(a, b) {
  var x1 = a & 7, x2 = b & 7;
  var y1 = a >> 3, y2 = b >> 3;
  return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
}

function isBlack(p) {
  return p === p.toLowerCase();
}

function readFen(fen) {
  var parts = fen.split(' ');
  var board = {
    pieces: {},
    turn: parts[1] === 'w',
    fmvn: parseInt(parts[5], 10) || 1
  };

  parts[0].split('/').forEach(function(row, y) {
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
  return [s - 1, s - 9, s - 8, s - 7, s + 1, s + 9, s + 8, s + 7].filter(function (o) {
    return o >= 0 && o < 64 && squareDist(s, o) === 1;
  });
}

function knightMovesTo(s) {
  return [s + 17, s + 15, s + 10, s + 6, s - 6, s - 10, s - 15, s - 17].filter(function (o) {
    return o >= 0 && o < 64 && squareDist(s, o) <= 2;
  });
}

var ROOK_DELTAS = [8, 1, -8, -1];
var BISHOP_DELTAS = [9, -9, 7, -7];
var QUEEN_DELTAS = ROOK_DELTAS.concat(BISHOP_DELTAS);

function slidingMovesTo(s, deltas, board) {
  var result = [];
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

function isCheck(variant, board) {
  if (variant === 'antichess' || variant == 'racingKings') return false;

  var ksq = board.turn ? board.K : board.k;
  if (typeof ksq !== 'number') return false;

  // no check when kings are touching in atomic
  if (variant === 'atomic' && squareDist(board.k, board.K) <= 1) return false;

  var n = board.turn ? 'n' : 'N';
  var r = board.turn ? 'r' : 'R';
  var b = board.turn ? 'b' : 'B';
  var q = board.turn ? 'q' : 'Q';

  return (knightMovesTo(ksq).some(function(o) {
      return board.pieces[o] === n;
  }) || slidingMovesTo(ksq, ROOK_DELTAS, board).some(function (o) {
      return board.pieces[o] === r || board.pieces[o] === q;
  }) || slidingMovesTo(ksq, BISHOP_DELTAS, board).some(function (o) {
      return board.pieces[o] === b || board.pieces[o] === q;
  }));
}

function makeMove(variant, board, uci) {
  if (!board.turn) board.fmvn++;
  board.turn = !board.turn;

  if (uci.indexOf('@') !== -1) {
    board.pieces[square(uci.slice(2, 4))] = board.turn ? uci[0].toLowerCase() : uci[0];
    return;
  }

  var move = util.decomposeUci(uci);
  var from = square(move[0]);
  var to = square(move[1]);
  var p = board.pieces[from];
  var capture = board.pieces[to];

  if (p === 'p' || p === 'P') {
    if (uci[0] !== uci[2] && !capture) {
      // en passant
      delete board.pieces[to + (board.turn ? 8 : -8)];
      capture = true;
    }
  }

  if (p === 'k' || p === 'K') {
    // castling
    var frCastle = capture && isBlack(p) === isBlack(capture);
    if (frCastle || squareDist(from, to) > 1) {
      delete board.pieces[from];
      if (frCastle) delete board.pieces[to];
      if (to < from) {
        if (!frCastle) delete board.pieces[board.turn ? square('a8') : square('a1')];
        to = board.turn ? square('c8') : square('c1');
        board.pieces[to + 1] = board.turn ? 'r' : 'R';
      }
      else {
        if (!frCastle) delete board.pieces[board.turn ? square('h8') : square('h1')];
        to = board.turn ? square('g8') : square('g1');
        board.pieces[to - 1] = board.turn ? 'r' : 'R';
      }
      board.pieces[to] = p;
      board[p] = to;
      return;
    }
    board[p] = to;
  }

  if (move[2]) board.pieces[to] = board.turn ? move[2] : move[2].toUpperCase();
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

function san(board, uci) {
  if (uci.indexOf('@') !== -1) return util.fixCrazySan(uci);

  var move = util.decomposeUci(uci);
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

  var rank = false, file = false;
  for (var i = 0; i < candidates.length; i++) {
    if (candidates[i] === from || board.pieces[candidates[i]] !== p) continue;
    if (from & 7 === candidates[i] & 7) rank = true;
    if (from >> 3 === candidates[i] >> 3) file = true;
  }
  if (file) san += uci[0];
  if (rank) san += uci[1];

  // target
  if (d) san += 'x';
  san += move[1];
  return san;
}

module.exports = function(variant, fen, threat, pv, mate) {
  var board = readFen(fen);
  if (threat) board.turn = !board.turn;
  var turn = board.turn;
  var moves = pv.split(' ').slice(0, 10);

  var first = true;
  var line = moves.map(function(uci) {
    var s = '';
    if (board.turn) s = board.fmvn + '. ';
    else if (first) s = board.fmvn + '... ';
    first = false;
    s += san(board, uci);
    makeMove(variant, board, uci);
    if (isCheck(variant, board)) s += '+';
    return s;
  }).join(' ');

  if (mate) {
    var matePlies = mate * 2;
    if (mate > 0 === turn) matePlies--;
    if (moves.length >= matePlies) line = line.replace(/\+?$/, '#');
  }

  return line;
}
