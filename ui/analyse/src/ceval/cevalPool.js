var m = require('mithril');

module.exports = function(opts, makeWorker, nb) {

  var workers = [];
  var token = -1;

  var getWorker = function() {
    initWorkers();
    token = (token + 1) % workers.length;
    return workers[token];
  };

  var initWorkers = function() {
    if (!workers.length)
      for (var i = 1; i <= nb; i++)
        workers.push(makeWorker(opts, 'W' + i));
  }

  var stopAll = function() {
    workers.forEach(function(i) {
      i.stop();
    });
  };

  return {
    start: function(work) {
      stopAll();
      getWorker().start(work);
    },
    stop: stopAll,
    warmup: initWorkers
  };
};
