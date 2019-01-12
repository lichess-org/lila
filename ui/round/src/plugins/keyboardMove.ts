const keyRegex = /^[a-h][1-8]$/;
const fileRegex = /^[a-h]$/;

window.lichess.keyboardMove = function(opts) {
  if (opts.input.classList.contains('ready')) return;
  opts.input.classList.add('ready');
  const writer = sanWriter();
  let sans = null;
  const submit = function(v: string, force?: boolean) {
    // consider 0's as O's for castling
    v = v.replace(/0/g, 'O');
    var foundUci = v.length >= 2 && sans && sanToUci(v, sans);
    if (foundUci) {
      // ambiguous castle
      if (v.toLowerCase() === 'o-o' && sans['O-O-O'] && !force) return;
      // ambiguous UCI
      if (v.match(keyRegex) && opts.hasSelected()) opts.select(v);
      else opts.san(foundUci.slice(0, 2), foundUci.slice(2));
      clear();
    } else if (sans && v.match(keyRegex)) {
      opts.select(v);
      clear();
    } else if (sans && v.match(fileRegex)) {
      // do nothing
    } else
      opts.input.classList.toggle('wrong', v.length && sans && !sanCandidates(v, sans).length);
  };
  var clear = function() {
    opts.input.value = '';
    opts.input.classList.remove('wrong');
  };
  makeBindings(opts, submit, clear);
  return function(fen, dests) {
    sans = dests && Object.keys(dests).length ? writer(fen, destsToUcis(dests)) : null;
    submit(opts.input.value);
  };
}

function makeBindings(opts, submit, clear) {
  window.Mousetrap.bind('enter', function() {
    opts.input.focus();
  });
  /* keypress doesn't cut it here;
   * at the time it fires, the last typed char
   * is not available yet. Reported by:
   * https://lichess.org/forum/lichess-feedback/keyboard-input-changed-today-maybe-a-bug
   */
  opts.input.addEventListener('keyup', function(e) {
    var v = e.target.value;
    if (v.indexOf('/') > -1) {
      focusChat();
      clear();
    }
    else if (v === '' && e.which === 13) opts.confirmMove();
    else submit(v, e.which === 13);
  });
  opts.input.addEventListener('focus', function() {
    opts.setFocus(true);
  });
  opts.input.addEventListener('blur', function() {
    opts.setFocus(false);
  });
}

function sanToUci(san, sans) {
  if (san in sans) return sans[san];
  var lowered = san.toLowerCase();
  for (var i in sans)
    if (i.toLowerCase() === lowered) return sans[i];
}

function sanCandidates(san, sans) {
  var lowered = san.toLowerCase();
  return Object.keys(sans).filter(function(s) {
    return s.toLowerCase().indexOf(lowered) === 0;
  });
}

function destsToUcis(dests) {
  var ucis = [];
  Object.keys(dests).forEach(function(orig) {
    dests[orig].forEach(function(dest) {
      ucis.push(orig + dest);
    });
  });
  return ucis;
}

function sanWriter() {

  var fixCrazySan = function(san) {
    return san[0] === 'P' ? san.slice(1) : san;
  };
  var decomposeUci = function(uci) {
    return [uci.slice(0, 2), uci.slice(2, 4), uci.slice(4, 5)];
  };

  var square = function(name) {
    return name.charCodeAt(0) - 97 + (name.charCodeAt(1) - 49) * 8;
  }

  var squareDist = function(a, b) {
    var x1 = a & 7,
      x2 = b & 7;
    var y1 = a >> 3,
      y2 = b >> 3;
    return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
  }

  var isBlack = function(p) {
    return p === p.toLowerCase();
  }

  var readFen = function(fen) {
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

  var kingMovesTo = function(s) {
    return [s - 1, s - 9, s - 8, s - 7, s + 1, s + 9, s + 8, s + 7].filter(function(o) {
      return o >= 0 && o < 64 && squareDist(s, o) === 1;
    });
  }

  var knightMovesTo = function(s) {
    return [s + 17, s + 15, s + 10, s + 6, s - 6, s - 10, s - 15, s - 17].filter(function(o) {
      return o >= 0 && o < 64 && squareDist(s, o) <= 2;
    });
  }

  var pawnAttacksTo = function(turn, s) {
    var left = turn ? 7 : -7;
    var right = turn ? 9 : -9;
    return [s + left, s + right].filter(function(o) {
      return o >= 0 && o < 64 && squareDist(s, o) === 1;
    });
  }

  var ROOK_DELTAS = [8, 1, -8, -1];
  var BISHOP_DELTAS = [9, -9, 7, -7];
  var QUEEN_DELTAS = ROOK_DELTAS.concat(BISHOP_DELTAS);

  var slidingMovesTo = function(s, deltas, board) {
    var result = [];
    deltas.forEach(function(delta) {
      for (var square = s + delta; square >= 0 && square < 64 && squareDist(square, square - delta) === 1; square += delta) {
        result.push(square);
        if (board.pieces[square]) break;
      }
    });
    return result;
  }

  var sanOf = function(board, uci) {
    if (uci.indexOf('@') !== -1) return fixCrazySan(uci);

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

  return function(fen, ucis) {
    var board = readFen(fen);
    var sans = {}
    ucis.forEach(function(uci) {
      var san = sanOf(board, uci);
      sans[san] = uci;
      if (san.indexOf('x') !== -1)
        sans[san.replace('x', '')] = uci;
    });
    return sans;
  }
}

function focusChat() {
  var chatInput = document.querySelector('.mchat input.lichess_say') as HTMLInputElement;
  if (chatInput) chatInput.focus();
}
