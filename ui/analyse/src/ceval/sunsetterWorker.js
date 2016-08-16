var m = require('mithril');

module.exports = function(opts, name) {

  var instance = null;
  var busy = false;
  var stopping = false;

  // Sunsetter always plays only moves right away. Count the number of played
  // moves to show the correct mate in #n.
  var onlyMoves = 0;
  var best;

  var send = function(text) {
    instance.postMessage(text);
  };

  var processOutput = function(text, work) {
    if (text === 'tellics stopped') {
      busy = false;
      stopping = false;
      onlyMoves = 0;
      best = undefined;
      return;
    }
    if (stopping) return;

    if (text.indexOf('move ') == 0) {
      onlyMoves++;
      best = text.split(' ')[1];
      send('analyze');
      return;
    }

    var depth, cp, mate;

    var matches = text.match(/(\d+)\s+([-+]?\d+)\s+(\d+)\s+(\d+)\s+([a-h1-8=@PNBRQK]+).*/);
    if (matches) {
      depth = parseInt(matches[1], 10);
      cp = parseInt(matches[2], 10);
      if (!onlyMoves) best = matches[5];
    } else {
      matches = text.match(/Found move:\s+([a-h1-8=@PNBRQK]+)\s+([-+]?\d+)\s.*/);
      if (matches) {
        depth = opts.maxDepth;
        cp = parseInt(matches[2], 10);
        if (!onlyMoves) best = matches[1];
        stopping = true;
        send('force');
        send('tellics stopped');
      } else {
        return;
      }
    }

    if (!onlyMoves && depth < opts.minDepth) return;

    if ((work.ply + onlyMoves) % 2 == 1) {
      if (cp) cp = -cp;
      if (mate) mate = -mate;
    }

    // transform mate scores
    if (cp > 20000) {
      mate = Math.floor((30000 - cp) / 10);
      cp = undefined;
    } else if (cp < -20000) {
      mate = Math.floor((-30000 - cp) / 10);
      cp = undefined;
    }

    if (mate) {
      if (mate > 0) mate += onlyMoves;
      else mate -= onlyMoves;
    }

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
    instance = new Worker('/assets/vendor/Sunsetter/sunsetter.js');
    busy = false;
    stopping = false;
    onlyMoves = 0;
    send('xboard');
  };

  reboot();

  return {
    start: function(work) {
      if (busy) reboot();
      busy = true;
      send('variant ' + opts.variant.key);
      send('setboard ' + work.position);
      send('force');
      for (var i = 0; i < work.moves.length; i++) {
        send(work.moves[i]);
      }
      send('analyze');
      send('go');
      instance.onmessage = function(msg) {
        processOutput(msg.data, work);
      };
    },
    stop: function() {
      if (!busy) return;
      stopping = true;
      onlyMoves = 0;
      best = undefined;
      send('exit');
      send('tellics stopped');
    }
  };
};
