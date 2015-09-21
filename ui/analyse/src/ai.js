module.exports = function(emit) {

  var worker = new Worker('/assets/vendor/stockfish6.js');
  var depth = 15;
  var currentPath;

  window.worker = worker;

  var send = function(text) {
    worker.postMessage(text);
  };

  worker.onmessage = function(msg) {
    var matches = msg.data.match(/depth (\d+) .*score (cp|mate) ([-\d]+) .*pv (.+)/);
    if (!matches) return;
    emit(currentPath, {
      depth: parseInt(matches[1]),
      cp: parseFloat(matches[3])
    });
    // info depth 16 seldepth 23 multipv 1 score cp 18 nodes 642324 nps 143568 time 4474 pv g1f3 g8f6 d2d4 d7d5 c1f4 e7e6 e2e3 b8c6 f1b5 c8d7 e1g1 f8d6 b1d2 d6f4 e3f4 e8g8 a2a3
  };

  var stop = function() {
    send('stop');
  };

  return {
    start: function(path, steps) {
      stop();
      currentPath = path;
      send('position startpos moves ' + steps.map(function(step) {
        return step.uci;
      }).join(' '));
      send('go depth ' + depth);
    },
    stop: stop
  };
};
