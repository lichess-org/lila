function decomposeUsi(usi) {
  return [usi.slice(0, 2), usi.slice(2, 4), usi.slice(4, 5)];
}

function square(name) {
  return name.charCodeAt(0) - 49 + (name.charCodeAt(1) - 49) * 9;
}

function squareDist(a, b) {
  var x1 = a % 9,
    x2 = b % 9;
  var y1 = Math.trunc(a / 9),
    y2 = Math.trunc(b / 9);
  return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
}

function readSfen(sfen) {
  var parts = sfen.split(' ');
  var board = {
    pieces: {},
    turn: parts[1] === 'b',
  };

  parts[0]
    .split('/')
    .slice(0, 9)
    .forEach(function (row, y) {
      var x = 8;
      var p = '';
      row.split('').forEach(function (v) {
        if (v == '+') {
          p = '+';
        } else {
          var nb = parseInt(v, 10);
          if (nb) x -= nb;
          else {
            var square = y * 9 + x;
            board.pieces[square] = p + v;
            if (v === 'k' || v === 'K') board[v] = square;
            x--;
          }
          p = '';
        }
      });
    });

  return board;
}

function knightMovesTo(s, c) {
  return (c ? [s - 19, s - 17] : [s + 17, s + 19]).filter(function (o) {
    return o >= 0 && o < 81 && squareDist(s, o) <= 2;
  });
}

function silverMovesTo(s, c) {
  return (c ? [s - 10, s - 9, s - 8, s + 8, s + 10] : [s - 10, s - 8, s + 8, s + 9, s + 10]).filter(function (o) {
    return o >= 0 && o < 81 && squareDist(s, o) === 1;
  });
}

function goldMovesTo(s, c) {
  return (c ? [s - 10, s - 9, s - 8, s - 1, s + 1, s + 9] : [s - 9, s - 1, s + 1, s + 8, s + 9, s + 10]).filter(
    function (o) {
      return o >= 0 && o < 81 && squareDist(s, o) === 1;
    }
  );
}

var ROOK_DELTAS = [9, 1, -9, -1];
var BISHOP_DELTAS = [10, -10, 8, -8];

function slidingMovesTo(s: number, deltas: number[], board, promotedDeltas: number[] = []): number[] {
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
  promotedDeltas.forEach(function (delta) {
    var square = s + delta;
    if (square >= 0 && square < 81 && squareDist(square, square - delta) === 1) {
      result.push(square);
    }
  });
  return result;
}

function sanOf(board, usi) {
  if (usi.includes('*')) return usi;

  var move = decomposeUsi(usi);
  var from = square(move[0]);
  var to = square(move[1]);
  var p = board.pieces[from];
  var d = board.pieces[to];
  var pt = board.pieces[from].toLowerCase();
  var san = pt.toUpperCase();

  // disambiguate non-drop moves
  var candidates: number[] = [];
  if (pt == 'k' || pt == 'p') candidates = [];
  else if (pt == 'n') candidates = knightMovesTo(to, p !== pt);
  else if (pt == 's') candidates = silverMovesTo(to, p !== pt);
  else if (pt == 'r') candidates = slidingMovesTo(to, ROOK_DELTAS, board);
  else if (pt == '+r') candidates = slidingMovesTo(to, ROOK_DELTAS, board, BISHOP_DELTAS);
  else if (pt == 'b') candidates = slidingMovesTo(to, BISHOP_DELTAS, board);
  else if (pt == '+b') candidates = slidingMovesTo(to, BISHOP_DELTAS, board, ROOK_DELTAS);
  else candidates = goldMovesTo(to, p !== pt);

  var rank = false,
    file = false;
  for (var i = 0; i < candidates.length; i++) {
    // If candidates are all on different files, disambiguate by file
    // Otherwise disambiguate by both file and rank
    if (candidates[i] === from || board.pieces[candidates[i]] !== p) continue;
    if (from % 9 === candidates[i] % 9) rank = true;
    file = true;
  }
  if (file) san += usi[0];
  if (rank) san += usi[1];

  // target
  san += d ? 'x' : '-';
  san += move[1];

  // (un)promotion
  if (move[2]) san += move[2];
  if ('plnsbr'.includes(pt) && (board.turn ? from < 27 || to < 27 : from >= 54 || to >= 54)) san += '+=';
  if ('pl'.includes(pt) && (board.turn ? to < 9 : to >= 72)) san = san.slice(0, -1);
  if (pt == 'n' && (board.turn ? to < 18 : to >= 63)) san = san.slice(0, -1);
  return san;
}

// psuedo-short algebraic notation (contains 'x' or '-')
export default function sanWriter(sfen, usis) {
  var board = readSfen(sfen);
  var lans = {};
  usis.forEach(function (usi) {
    var san = sanOf(board, usi);
    if (san.endsWith('+=')) {
      lans[san.slice(0, -2) + '='] = usi;
      san = san.slice(0, -1);
    }
    lans[san] = usi;
  });
  return lans;
}
