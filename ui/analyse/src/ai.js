module.exports = function(emit) {

  var worker = new Worker('/assets/vendor/stockfish6.js');
  var minDepth = 10;
  var maxDepth = 18;
  var currentPath;
  var currentStep;

  var send = function(text) {
    console.log(text);
    worker.postMessage(text);
  };

  worker.onmessage = function(msg) {
    if (/currmovenumber|lowerbound|upperbound/.test(msg.data)) return;
    var matches = msg.data.match(/depth (\d+) .*score (cp|mate) ([-\d]+) .*pv (.+)/);
    if (!matches) return;
    console.log(msg.data);
    var depth = parseInt(matches[1]);
    if (depth < minDepth) return;
    var cp = parseFloat(matches[3]);
    if (currentStep.ply % 2 === 1) cp = -cp;
    var uci = matches[4].split(' ')[0];
    emit(currentPath, {
      depth: depth,
      cp: cp,
      uci: uci
    });
  };

  var stop = function() {
    send('stop');
  };

  return {
    start: function(path, steps) {
      stop();
      currentPath = path;
      currentStep = steps[steps.length -1];
      send('position startpos moves ' + steps.map(function(step) {
        return step.uci;
      }).join(' '));
      send('go depth ' + maxDepth);
    },
    stop: stop
  };
};
