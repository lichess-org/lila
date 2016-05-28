var m = require('mithril');

module.exports = function(opts, name) {

  var busy = false;
  var instance = null;

  var send = function(text) {
    instance.postMessage(text);
  };

  var processOutput = function(text, work) {
    if (/bestmove .*/.test(text)) {
      busy = false;
      return;
    }
    if (/currmovenumber|lowerbound|upperbound/.test(text)) return;
    var matches = text.match(/depth (\d+) .*score (cp|mate) ([-\d]+) .*pv (.+)/);
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
    var best = matches[4].split(' ')[0];
    work.emit({
      work: work,
      eval: {
        depth: depth,
        cp: cp,
        mate: mate,
        best: best
      },
      name: name
    });
  };

  var stop = function() {
    if (busy && instance) {
      instance.terminate();
      instance = null;
    }
    if (!instance) {
      instance = new Worker(opts.path);
      if (opts.variant.key === 'fromPosition' || opts.variant.key === 'chess960')
        send('setoption name UCI_Chess960 value true');
      else if (opts.variant.key === 'atomic')
        send('setoption name UCI_Atomic value true');
      else if (opts.variant.key === 'horde')
        send('setoption name UCI_Horde value true');
      else if (opts.variant.key === 'crazyhouse')
        send('setoption name UCI_House value true');
      else if (opts.variant.key === 'kingOfTheHill')
        send('setoption name UCI_KingOfTheHill value true');
      else if (opts.variant.key === 'racingKings')
        send('setoption name UCI_Race value true');
      else if (opts.variant.key === 'threeCheck')
        send('setoption name UCI_3Check value true');
      else
        send('uci'); // send something to warm up
    }
    busy = false;
  };

  stop();

  return {
    start: function(work) {
      busy = true;
      send(['position', 'fen', work.position, 'moves'].concat(work.moves).join(' '));
      send('go depth ' + opts.maxDepth);
      instance.onmessage = function(msg) {
        processOutput(msg.data, work);
      };
    },
    stop: stop
  };
};
