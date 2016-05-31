var m = require('mithril');

var variantMap = {
  fromPosition: 'Chess960',
  chess960: 'Chess960',
  atomic: 'Atomic',
  horde: 'Horde',
  crazyhouse: 'House',
  kingOfTheHill: 'KingOfTheHill',
  racingKings: 'Race',
  threeCheck: '3Check'
};

module.exports = function(opts, name) {

  var instance = null;
  var busy = false;
  var stopping = false;

  var send = function(text) {
    instance.postMessage(text);
  };

  var processOutput = function(text, work) {
    if (text.indexOf('bestmove ') === 0) {
      busy = false;
      stopping = false;
      return;
    }
    if (stopping) return;
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

  var reboot = function() {
    if (instance) instance.terminate();
    instance = new Worker(opts.path);
    busy = false;
    stopping = false;
    var uciVariant = variantMap[opts.variant.key];
    if (uciVariant) send('setoption name UCI_' + uciVariant + ' value true');
    else send('uci'); // send something to warm up
  };

  reboot();

  return {
    start: function(work) {
      if (busy) reboot();
      busy = true;
      send(['position', 'fen', work.position, 'moves'].concat(work.moves).join(' '));
      send('go depth ' + opts.maxDepth);
      instance.onmessage = function(msg) {
        processOutput(msg.data, work);
      };
    },
    stop: function() {
      if (!busy) return;
      stopping = true;
      send('stop');
    }
  };
};
