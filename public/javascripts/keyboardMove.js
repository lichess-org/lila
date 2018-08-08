var keyRegex = /^\d{1,2}$/;
var uciRegex = /^\d{4}$/;

lidraughts.keyboardMove = function(opts) {
  if (opts.input.classList.contains('ready')) return;
  opts.input.classList.add('ready');
  var writer = sanWriter();
  var sans = null;
  var submit = function(v, force) {
    var foundUci = v.length >= 3 && sans && sanToUci(v, sans);
    if (foundUci) {
      opts.san(foundUci.slice(0, 2), foundUci.slice(2));
      clear();
    } else if (sans && v.match(keyRegex)) {
      if (force) {
        opts.select(v.length === 1 ? ("0" + v) : v);
        clear();
      } else
        opts.input.classList.remove('wrong');
    } else
      opts.input.classList.toggle('wrong', v.length && sans && !sanCandidates(v, sans).length);
  };
  var clear = function() {
    opts.input.value = '';
    opts.input.classList.remove('wrong');
  };
  makeBindings(opts, submit, clear);
  return function(fen, dests, captLen) {
    sans = dests && Object.keys(dests).length ? writer(fen, destsToUcis(dests), captLen) : null;
    submit(opts.input.value);
  };
}

function makeBindings(opts, submit, clear) {
  Mousetrap.bind('enter', function() {
    opts.input.focus();
  });
  /* keypress doesn't cut it here;
   * at the time it fires, the last typed char
   * is not available yet. Reported by:
   * https://lidraughts.org/forum/lidraughts-feedback/keyboard-input-changed-today-maybe-a-bug
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
  if (san.length === 4 && Object.values(sans).includes(san)) return san;
  var lowered = san.toLowerCase().replace("x0", "x").replace("-0", "-");
  if (lowered.slice(0, 1) === "0") lowered = lowered.slice(1)
  if (lowered in sans) return sans[lowered];
}

function sanCandidates(san, sans) {
  var lowered = san.toLowerCase();
  var cleanLowered = lowered.replace("x0", "x").replace("-0", "-");
  if (cleanLowered.slice(0, 1) === "0") cleanLowered = cleanLowered.slice(1)
  var filterKeys = Object.keys(sans).filter(function(s) {
    var sLowered = s.toLowerCase();
    return sLowered.indexOf(lowered) === 0 || sLowered.indexOf(cleanLowered) === 0;
  });
  return filterKeys.length ? filterKeys : Object.values(sans).filter(function(s) {
        return s.indexOf(lowered) === 0;
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

  var decomposeUci = function(uci) {
    return [uci.slice(0, 2), uci.slice(2, 4)];
  };

  var readFen = function(fen) {
    var fenParts = fen.split(':');
    var board = {
      pieces: {},
      turn: fenParts[0] === 'W'
    };

    for (var i = 0; i < fenParts.length; i++) {
        var clr = fenParts[i].slice(0, 1);
        if ((clr === "W" || clr === "B") && fenParts[i].length > 1) {
            var fenPieces = fenParts[i].slice(1).split(',');
            for (var k = 0; k < fenPieces.length; k++) {
                var fieldNumber = fenPieces[k].slice(1), role = fenPieces[k].slice(0, 1);
                if (fieldNumber.length !== 0 && role.length !== 0) {
                    if (fieldNumber.length == 1)
                        fieldNumber = "0" + fieldNumber;
                    board.pieces[fieldNumber] = role;
                }
            }
        }
    }

    return board;
  }

  var shorten = function(uci) {
    return (uci && uci.slice(0, 1) === "0") ? uci.slice(1) : uci;
  }

  var sanOf = function(board, uci, capture) {

    var move = decomposeUci(uci);
    if (capture)
        return shorten(move[0]) + "x" + shorten(move[1]);
    else
        return shorten(move[0]) + "-" + shorten(move[1]);

  }

  return function(fen, ucis, captLen) {
    var board = readFen(fen);
    var capture = captLen && captLen > 0;
    var sans = {}
    ucis.forEach(function(uci) {
      var san = sanOf(board, uci, capture);
      sans[san] = uci;
    });
    return sans;
  }
}

function focusChat() {
  var chatInput = document.querySelector('.mchat input.lidraughts_say');
  if (chatInput) chatInput.focus();
}
