var m = require('mithril');

module.exports = function(allow, emit) {

  var storageKey = 'client-eval-enabled';
  var allowed = m.prop(allow);
  var enabled = m.prop(allow && lichess.storage.get(storageKey) === '1');
  var minDepth = 9; // min depth to start displaying eval and bestmove
  var maxDepth = 18; // stop computing after this depth
  var current; // last (or current) input
  var switching = false; // when switching to new step, info for previous can be emited
  var processing = m.prop(false);

  var instance;
  var worker = function() {
    if (!instance) {
      console.log('new instance!');
      instance = new Worker('/assets/vendor/stockfish6.js');
      instance.onmessage = function(msg) {
        if (!enabled() || !current) return;
        if (/currmovenumber|lowerbound|upperbound/.test(msg.data)) return;
        var matches = msg.data.match(/depth (\d+) .*score (cp|mate) ([-\d]+) .*pv (.+)/);
        if (!matches) return;
        var depth = parseInt(matches[1]);
        console.log(depth);
        if (switching && depth > 1) return; // stale info for previous step
        switching = false; // got depth 1, it's now computing the current step
        if (depth < minDepth) return;
        var cp = parseFloat(matches[3]);
        if (current.ply % 2 === 1) cp = -cp;
        var uci = matches[4].split(' ')[0];
        emit(current.path, {
          depth: depth,
          cp: cp,
          uci: uci
        });
        if (depth === maxDepth) processing(false);
      };
    }
    return instance;
  };

  var send = function(text) {
    console.log(text);
    worker().postMessage(text);
  };

  var start = function(path, steps) {
    if (!enabled()) return;
    stop();
    current = {
      path: path,
      steps: steps,
      ply: steps[steps.length -1].ply
    };
    switching = true;
    send('position startpos moves ' + steps.map(function(step) {
      return fixCastle(step.uci);
    }).join(' '));
    send('go depth ' + maxDepth);
    processing(true);
  };

  var stop = function() {
    if (!enabled() || !instance) return;
    instance.terminate();
    instance = null;
    processing(false);
  };

  var fixCastle = function(uci) {
    switch (uci) {
      case 'e1h1':
        return 'e1g1';
      case 'e1a1':
        return 'e1c1';
      case 'e8h8':
        return 'e8g8';
      case 'e8a8':
        return 'e8c8';
    }
    return uci;
  }

  return {
    start: start,
    stop: stop,
    allowed: allowed,
    enabled: enabled,
    processing: processing,
    toggle: function(path, steps) {
      if (!allowed()) return;
      stop();
      enabled(!enabled());
      if (enabled()) start(path, steps);
      lichess.storage.set(storageKey, enabled() ? '1' : '0');
    }
  };
};
