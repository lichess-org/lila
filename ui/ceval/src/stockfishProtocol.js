var m = require('mithril');

var EVAL_REGEX = new RegExp(''
  + /^info depth (\d+) seldepth \d+ multipv (\d+) /.source
  + /score (cp|mate) ([-\d]+) /.source
  + /(?:(upper|lower)bound )?nodes (\d+) nps \S+ /.source
  + /(?:hashfull \d+ )?tbhits \d+ time (\S+) /.source
  + /pv (.+)/.source);

module.exports = function(worker, opts) {
  var work = null;
  var curEval = null;
  var expectedPvs = 0;

  var stopped = m.deferred();
  stopped.resolve(true);

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
        eval = parseInt(matches[4]),
        evalType = matches[5],
        nodes = parseInt(matches[6]),
        elapsedMs = parseInt(matches[7]),
        moves = matches[8].split(' ', 10);

    // time is negative on safari
    if (!elapsedMs || elapsedMs < 0) elapsedMs = (new Date() - work.startedAt)

    // Track max pv index to determine when pv prints are done.
    if (expectedPvs < multiPv) expectedPvs = multiPv;

    if (depth < opts.minDepth) return;
    if (work.ply % 2 === 1) eval = -eval;

    // For now, ignore most upperbound/lowerbound messages.
    // The exception is for multiPV, sometimes non-primary PVs
    // only have an upperbound. See: ddugovic/Stockfish#228
    if (evalType && multiPv === 1) return;

    var pvData = {
      moves: moves,
      cp: isMate ? undefined : eval,
      mate: isMate ? eval : undefined,
      depth: depth,
    };

    if (multiPv === 1) {
      curEval = {
        fen: work.currentFen,
        maxDepth: work.maxDepth,
        depth: depth,
        knps: nodes / elapsedMs,
        nodes: nodes,
        cp: pvData.cp,
        mate: pvData.mate,
        pvs: [pvData],
        millis: elapsedMs
      };
    } else if (curEval) {
      curEval.pvs.push(pvData);
      curEval.depth = Math.min(curEval.depth, depth);
    }

    if (multiPv === expectedPvs && curEval) {
      work.emit(curEval);
      curEval = null;
    }
  };

  return {
    start: function(w) {
      work = w;
      work.startedAt = new Date();
      curEval = null;
      stopped = null;
      expectedPvs = 1;
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
