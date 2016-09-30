var m = require('mithril');

var variantMap = {
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

module.exports = function(opts, name) {

  var instance = null;
  var state = null;
  var busy = false;
  var stopping = false;

  var send = function(text) {
    instance.postMessage(text);
  };

  var emit = function() {
    if (state) state.work.emit(state);
    state = null;
  };

  var processOutput = function(text, work) {
    if (text.indexOf('bestmove ') === 0) {
      busy = false;
      stopping = false;
      return;
    }
    if (stopping) return;

    if (/currmovenumber|lowerbound|upperbound/.test(text)) return;
    var matches = text.match(/depth (\d+) .*multipv (\d+) .*score (cp|mate) ([-\d]+) .*nps (\d+) .*pv (.+)/);
    if (!matches) {
      emit();
      return;
    }

    var multipv = parseInt(matches[2]);
    var cp, mate;
    if (matches[3] === 'cp') cp = parseFloat(matches[4]);
    else mate = parseFloat(matches[4]);
    if (work.ply % 2 === 1) {
      if (matches[3] === 'cp') cp = -cp;
      else mate = -mate;
    }

    if (multipv === 1) {
      emit();
      var depth = parseInt(matches[1]);
      if (depth < opts.minDepth) return;
      state = {
        work: work,
        eval: {
          depth: depth,
          nps: parseInt(matches[5]),
          best: matches[6].split(' ')[0],
          cp: cp,
          mate: mate,
          pvs: []
        },
        name: name
      };
    }

    if (state) state.eval.pvs[multipv - 1] = {
      cp: cp,
      mate: mate,
      pv: matches[6],
      best: matches[6].split(' ')[0]
    };

    if (multipv === opts.multipv) emit();
  };

  var reboot = function() {
    if (instance) instance.terminate();
    instance = new Worker('/assets/vendor/stockfish.js/stockfish.js');
    busy = false;
    stopping = false;
    var uciVariant = variantMap[opts.variant.key];
    if (uciVariant) send('setoption name UCI_' + uciVariant + ' value true');
    else send('uci'); // send something to warm up
    send('setoption name MultiPV value ' + opts.multipv);
  };

  reboot();

  return {
    start: function(work) {
      if (busy) reboot();
      busy = true;
      state = null;
      send(['position', 'fen', work.initialFen, 'moves'].concat(work.moves).join(' '));
      send('go depth ' + work.maxDepth);
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
