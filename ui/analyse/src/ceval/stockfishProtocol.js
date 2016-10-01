var m = require('mithril');

var legacyVariantMap = {
  fromPosition: 'Chess960',
  chess960: 'Chess960',
  atomic: 'Atomic',
  horde: 'Horde',
  crazyhouse: 'House',
  kingOfTheHill: 'KingOfTheHill',
  racingKings: 'Race',
  threeCheck: '3Check',
  antichess: 'Anti'
};

module.exports = function(worker, opts) {

  var work = null;
  var stopped = m.deferred();

  var legacyVariant = legacyVariantMap[opts.variant.key];
  if (legacyVariant) worker.send('setoption name UCI_' + legacyVariant + ' value true');
  else worker.send('uci');

  // TODO: Modern variant selector

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
      },
      name: name
    });
  };

  return {
    start: function(w) {
      work = w;
      worker.send(['position', 'fen', work.initialFen, 'moves'].concat(work.moves).join(' '));
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
