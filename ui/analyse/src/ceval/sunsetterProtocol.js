var m = require('mithril');

module.exports = function(worker, opts) {

  var work = null;
  var stopped = m.deferred();
  stopped.resolve(true);

  worker.send('xboard');

  // Sunsetter always plays only moves right away. Count the number of played
  // moves to show the correct mate in #n.
  // When aiMoves is > 0 Sunsetter is in analysis mode, which uses an absolute
  // eval score instead of relative to current color.
  var aiMoves = 0;
  var best;

  var processOutput = function(text) {
    if (text === 'tellics stopped') {
      aiMoves = 0;
      best = undefined;
      if (stopped) stopped.resolve(true);
      return;
    }
    if (!work) return;

    if (text.indexOf('move ') == 0) {
      aiMoves++;
      best = text.split(' ')[1];
      if (work) worker.send('analyze');
      return;
    }

    var depth, cp, mate;

    var matches = text.match(/(\d+)\s+([-+]?\d+)\s+(\d+)\s+(\d+)\s+([a-h1-8=@PNBRQK]+).*/);
    if (matches) {
      depth = parseInt(matches[1], 10);
      cp = parseInt(matches[2], 10);
      if (!aiMoves) {
        best = matches[5];
      }
    } else {
      matches = text.match(/Found move:\s+([a-h1-8=@PNBRQK]+)\s+([-+]?\d+)\s.*/);
      if (matches) {
        cp = parseInt(matches[2], 10);
        if (!aiMoves) best = matches[1];
        if (work) worker.send('analyze');
      } else {
        return;
      }
    }

    if (cp && !aiMoves && work.ply % 2) {
      cp = -cp;
    }

    // transform mate scores
    if (Math.abs(cp) > 20000) {
      mate = Math.floor((30000 - Math.abs(cp)) / 10) + 1;
      // approx depth for searches that end early with mate
      depth = Math.max(depth || 0, mate * 2 - 1);
      // correct sign and add sunsetter played moves
      mate = Math.sign(cp) * (mate + aiMoves);
      cp = undefined;
    }

    work.emit({
      work: work,
      eval: {
        depth: depth + aiMoves,
        cp: cp,
        mate: mate,
        best: best
      }
    });
  };

  return {
    start: function(w) {
      stopped = null;
      work = w;
      worker.send('reset crazyhouse');
      worker.send('setboard ' + work.initialFen);
      worker.send('easy');
      worker.send('force');
      for (var i = 0; i < work.moves.length; i++) {
        worker.send(work.moves[i]);
      }
      worker.send('go');
    },
    stop: function() {
      if (!stopped) {
        stopped = m.deferred();
        work = null;
        aiMoves = 0;
        best = undefined;
        worker.send('exit');
        worker.send('tellics stopped');
      }
      return stopped;
    },
    received: processOutput
  };
};
