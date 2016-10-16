var m = require('mithril');

module.exports = function(worker, opts) {

  var work = null;
  var stopped = m.deferred();
  stopped.resolve(true);

  worker.send('xboard');
  worker.send('reset crazyhouse');

  var processOutput = function(text) {
    // console.warn("<-- %s", text);
    if (text === 'tellics stopped') {
      if (stopped) stopped.resolve(true);
      return;
    }
    if (!work) return;

    var depth, cp, mate, best, pv;

    var matches = text.match(/^(\d+) ([-+]?\d+) \d+ \d+ (.*)$/);
    if (matches) {
      depth = parseInt(matches[1], 10);
      cp = parseInt(matches[2], 10);
      pv = matches[3];
      best = pv.split(' ')[0];
    }

    // transform mate scores
    if (Math.abs(cp) > 20000) {
      mate = Math.sign(cp) * Math.floor(30000 - Math.abs(cp));
      cp = undefined;
    }

    work.emit({
      work: work,
      eval: {
        depth: depth,
        cp: cp,
        mate: mate,
        best: best,
        pvs: [{
          cp: cp,
          mate: mate,
          best: best,
          pv: pv
        }]
      }
    });
  };

  return {
    start: function(w) {
      stopped = null;
      work = w;
      worker.send('setboard ' + work.initialFen);
      worker.send('exit');
      for (var i = 0; i < work.moves.length; i++) {
        worker.send(work.moves[i]);
      }
      worker.send('analyze');
    },
    stop: function() {
      if (!stopped) {
        stopped = m.deferred();
        work = null;
        aiMoves = 0;
        best = undefined;
        pv = undefined;
        worker.send('exit');
        worker.send('tellics stopped');
      }
      return stopped;
    },
    received: processOutput
  };
};
