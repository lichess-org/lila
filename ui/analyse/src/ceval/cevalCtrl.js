var m = require('mithril');
var makePool = require('./cevalPool');

module.exports = function(allow, emit) {

  var storageKey = 'client-eval-enabled';
  var allowed = m.prop(allow);
  var enabled = m.prop(allow && lichess.storage.get(storageKey) === '1');
  var pool = makePool({
    path: '/assets/vendor/stockfish6.js', // Can't CDN because same-origin policy
    minDepth: 8,
    maxDepth: 18
  }, 3);

  var start = function(path, steps) {
    if (!enabled()) return;
    var step = steps[steps.length -1];
    pool.start({
      position: steps[0].fen,
      moves: steps.map(function(s) {
        return fixCastle(s.uci, s.san);
      }).join(' '),
      path: path,
      steps: steps,
      ply: step.ply,
      emit: function(res) {
        if (enabled()) emit(res);
      }
    });
  };

  var stop = function() {
    if (!enabled()) return;
    pool.stop();
  };

  var fixCastle = function(uci, san) {
    if (san !== 'O-O' && san !== 'O-O-O') return uci;
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
  };

  return {
    start: start,
    stop: stop,
    allowed: allowed,
    enabled: enabled,
    toggle: function() {
      if (!allowed()) return;
      stop();
      enabled(!enabled());
      lichess.storage.set(storageKey, enabled() ? '1' : '0');
    }
  };
};
