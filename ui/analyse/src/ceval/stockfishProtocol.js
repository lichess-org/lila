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
  var stopped = m.deferred();

  if (opts.variant.key === 'fromPosition' || opts.variant.key === 'chess960')
    worker.send('setoption name UCI_Chess960 value true');
  else if (opts.variant.key === 'antichess')
    worker.send('setoption name UCI_Variant value giveaway');
  else if (opts.variant.key !== 'standard')
    worker.send('setoption name UCI_Variant value ' + opts.variant.key.toLowerCase());
  else
    worker.send('uci');

  var processOutput = function(text) {
    if (text.indexOf('bestmove ') === 0) {
      stopped.resolve(true);
      return;
    }
    if (!work) return;
    if (/currmovenumber|lowerbound|upperbound/.test(text)) return;
    var matches = text.match(/depth (\d+) .*score (cp|mate) ([-\d]+) .*nps (\d+) .*pv (.+)/);
    if (!matches) return;
    var depth = parseInt(matches[1]);
    if (depth < opts.minDepth) return;
    var cp, mate;
    if (matches[2] === 'cp') cp = parseFloat(matches[3]);
    else mate = parseFloat(matches[3]);
    if (work.ply % 2 === 1) {
      if (matches[2] === 'cp') cp = -cp;
      else mate = -mate;
    }
    var best = matches[5].split(' ')[0];
    work.emit({
      work: work,
      eval: {
        depth: depth,
        cp: cp,
        mate: mate,
        best: best,
        nps: parseInt(matches[4])
      }
    });
  };

  return {
    start: function(w) {
      work = w;
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
