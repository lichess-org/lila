var m = require('mithril');

module.exports = function(opts, name) {

  var instance = null;
  var busy = false;
  var stopping = false;

  // Sunsetter always plays only moves right away. Count the number of played
  // moves to show the correct mate in #n.
  // When aiMoves is >0, sunsetter is in analysis mode, which uses an absolute
  // eval score instead of relative to current color :-\.
  var aiMoves = 0;
  var best;

  var send = function(text) {
    instance.postMessage(text);
  };

  var processOutput = function(text, work) {
    if (text === 'tellics stopped') {
      busy = false;
      stopping = false;
      aiMoves = 0;
      best = undefined;
      return;
    }
    if (stopping) return;

    if (text.indexOf('move ') == 0) {
      aiMoves++;
      best = text.split(' ')[1];
      send('analyze');
      return;
    }

    var depth, cp, mate;

    var matches = text.match(/(\d+)\s+([-+]?\d+)\s+(\d+)\s+(\d+)\s+([a-h1-8=@PNBRQK]+).*/);
    if (matches) {
      depth = parseInt(matches[1], 10);
      cp = parseInt(matches[2], 10);
      if (!aiMoves) {
        best = matches[5];
        if (depth < opts.minDepth) return;
      }
    } else {
      matches = text.match(/Found move:\s+([a-h1-8=@PNBRQK]+)\s+([-+]?\d+)\s.*/);
      if (matches) {
        cp = parseInt(matches[2], 10);
        if (!aiMoves) best = matches[1];
        send('analyze');
      } else {
        return;
      }
    }

    if (cp && !aiMoves && work.ply % 2) {
      cp = -cp;
    }

    // transform mate scores
    if (Math.abs(cp) > 20000) {
      mate = Math.floor((30000 - Math.abs(cp)) / 10);
      // approx depth for searches that end early with mate.
      depth = Math.max(depth || 0, mate * 2 - 1);
      // correct sign and add sunsetter played moves
      mate = Math.sign(cp) * (mate + aiMoves)
      cp = undefined;
    }

    if (mate) {
      stopping = true
      send('force');
      send('tellics stopped');
    }

    work.emit({
      work: work,
      eval: {
        depth: depth + aiMoves,
        cp: cp,
        mate: mate,
        best: best
      },
      name: name
    });
  };

  var reboot = function() {
    if (instance) instance.terminate();
    instance = new Worker('/assets/vendor/Sunsetter/sunsetter.js');
    busy = false;
    stopping = false;
    aiMoves = 0;
    send('xboard');
  };

  reboot();

  return {
    start: function(work) {
      if (busy) reboot();
      busy = true;
      send('variant ' + opts.variant.key);
      send('setboard ' + work.initialFen);
      send('force');
      for (var i = 0; i < work.moves.length; i++) {
        send(work.moves[i]);
      }
      send('go');
      instance.onmessage = function(msg) {
        processOutput(msg.data, work);
      };
    },
    stop: function() {
      if (!busy) return;
      stopping = true;
      aiMoves = 0;
      best = undefined;
      send('exit');
      send('tellics stopped');
    }
  };
};
