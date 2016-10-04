var m = require('mithril');

function fenToUci(fen) {
  // convert three check fens
  // lichess: rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1 +0+0
  // stockfish: rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 3+3 0 1
  var m = fen.match(/^(.+) (w|b) (.+) (.+) (\d+) (\d+) \+(\d+)\+(\d+)$/);
  if (!m) return fen;
  else {
    var w = parseInt(m[7], 10);
    var b = parseInt(m[8], 10);
    var checks = (3 - w) + '+' + (3 - b);
    var fen = [m[1], m[2], m[3], m[4], checks, m[5], m[6]].join(' ');
    return fen;
  }
}

module.exports = function(worker, opts) {

  var work = null;
  var state = null;
  var minLegalMoves = 0;
  var stopped = m.deferred();

  var emit = function() {
    if (!work || !state) return;
    minLegalMoves = Math.max(minLegalMoves, state.eval.pvs.length);
    if (state.eval.pvs.length < minLegalMoves) return;
    work.emit(state);
    state = null;
  };

  if (opts.hashSize)
    worker.send('setoption name Hash value 128');

  var threads = Math.ceil((navigator.hardwareConcurrency || 1) / 2);
  if (threads > 1) worker.send('setoption name Threads value ' + threads);

  if (opts.variant.key === 'fromPosition' || opts.variant.key === 'chess960')
    worker.send('setoption name UCI_Chess960 value true');
  else if (opts.variant.key === 'antichess')
    worker.send('setoption name UCI_Variant value giveaway');
  else if (opts.variant.key !== 'standard')
    worker.send('setoption name UCI_Variant value ' + opts.variant.key.toLowerCase());
  else
    worker.send('isready'); // warm up the webworker

  if (opts.multiPv > 1) worker.send('setoption name MultiPV value ' + opts.multiPv);

  var processOutput = function(text) {
    if (text.indexOf('bestmove ') === 0) {
      emit();
      stopped.resolve(true);
      return;
    }
    if (!work) return;

    if (text.indexOf('currmovenumber') !== -1) return;
    var matches = text.match(/depth (\d+) .*multipv (\d+) .*score (cp|mate) ([-\d]+) .*nps (\d+) .*pv (.+)/);
    if (!matches) {
      emit();
      return;
    }

    var depth = parseInt(matches[1]);
    if (depth < opts.minDepth) return;
    var multiPv = parseInt(matches[2]);
    var cp, mate;
    if (matches[3] === 'cp') cp = parseFloat(matches[4]);
    else mate = parseFloat(matches[4]);
    if (work.ply % 2 === 1) {
      if (matches[3] === 'cp') cp = -cp;
      else mate = -mate;
    }

    if (multiPv === 1) {
      emit();
      state = {
        work: work,
        eval: {
          depth: depth,
          nps: parseInt(matches[5]),
          best: matches[6].split(' ')[0],
          cp: cp,
          mate: mate,
          pvs: []
        }
      };
    }
    else if (!state || depth < state.eval.depth) return; // multipv progress

    state.eval.pvs[multiPv - 1] = {
      cp: cp,
      mate: mate,
      pv: matches[6],
      best: matches[6].split(' ')[0]
    };

    if (multiPv === opts.multiPv) emit();
  };

  return {
    start: function(w) {
      work = w;
      state = null;
      minLegalMoves = 0;
      worker.send(['position', 'fen', fenToUci(work.initialFen), 'moves'].concat(work.moves).join(' '));
      worker.send('go depth ' + work.maxDepth);
    },
    stop: function(s) {
      if (!work) s.resolve(true);
      else {
        work = null;
        stopped = s;
        worker.send('stop');
      }
      return s.promise;
    },
    received: processOutput
  };
};
