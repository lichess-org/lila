var m = require('mithril');

var EVAL_REGEX = new RegExp(''
  + /^info depth (\d+) seldepth \d+ multipv (\d+) /.source
  + /score (cp|mate) ([-\d]+) /.source
  + /(?:(upper|lower)bound )?nodes (\d+) nps (\d+) /.source
  + /(?:hashfull \d+ )?tbhits \d+ time (\d+) /.source
  + /pv (.+)/.source);

module.exports = function(worker, opts) {
  var emitTimer = null;
  var work = null;
  var curEval = null;

  var stopped = m.deferred();
  stopped.resolve(true);

  var emit = function() {
    emitTimer = clearTimeout(emitTimer);
    if (!work || !curEval) return;
    work.emit(curEval);
  };

  if (opts.variant.key === 'fromPosition' || opts.variant.key === 'chess960')
    worker.send('setoption name UCI_Chess960 value true');
  else if (opts.variant.key === 'antichess')
    worker.send('setoption name UCI_Variant value giveaway');
  else if (opts.variant.key !== 'standard')
    worker.send('setoption name UCI_Variant value ' + opts.variant.key.toLowerCase());
  else
    worker.send('isready'); // warm up the webworker

  var processOutput = function(text) {
    if (text.indexOf('bestmove ') === 0) {
      if (!stopped) stopped = m.deferred();
      stopped.resolve(true);
      return;
    }
    if (!work) return;

    var matches = text.match(EVAL_REGEX);
    if (!matches) return;

    var depth = parseInt(matches[1]),
        multiPv = parseInt(matches[2]),
        isMate = matches[3] === 'mate',
        eval = parseFloat(matches[4]),
        evalType = matches[5],
        nodes = parseInt(matches[6]),
        nps = parseInt(matches[7]),
        elapsedMs = parseInt(matches[8]),
        pv = matches[9];

    if (depth < opts.minDepth) return;
    if (work.ply % 2 === 1) eval = -eval;

    // For now, ignore most upperbound/lowerbound messages.
    // The exception is for multiPV, sometimes non-primary PVs
    // only have an upperbound. See: ddugovic/Stockfish#228
    if (evalType && (multiPv === 1 || evalType !== 'upper' || !curEval)) return;

    var pvData = {
      best: pv.split(' ', 2)[0],
      pv: pv,
      cp: isMate ? undefined : eval,
      mate: isMate ? eval : undefined,
      depth: depth,
    };

    if (multiPv === 1) {
      curEval = {
        fen: work.currentFen,
        maxDepth: work.maxDepth,
        depth: depth,
        nps: nps,
        best: pvData.best,
        cp: pvData.cp,
        mate: pvData.mate,
        pvs: [pvData],
        millis: elapsedMs,
      };
    } else if (curEval) {
      curEval.pvs.push(pvData);
      curEval.depth = Math.min(curEval.depth, depth);
    }

    if (multiPv === work.multiPv) {
      emit();
      curEval = null;
    } else {
      // emit timeout in case there aren't a full set of PVs.
      clearTimeout(emitTimer);
      emitTimer = setTimeout(emit, 50);
    }
  };

  return {
    start: function(w) {
      work = w;
      curEval = null;
      stopped = null;
      minLegalMoves = 0;
      if (opts.threads) worker.send('setoption name Threads value ' + opts.threads());
      if (opts.hashSize) worker.send('setoption name Hash value ' + opts.hashSize());
      worker.send('setoption name MultiPV value ' + work.multiPv);
      worker.send(['position', 'fen', work.initialFen, 'moves'].concat(work.moves).join(' '));
      worker.send('go depth ' + work.maxDepth);
    },
    stop: function() {
      if (!stopped) {
        work = null;
        stopped = m.deferred();
        worker.send('stop');
      }
      return stopped;
    },
    received: processOutput
  };
};
