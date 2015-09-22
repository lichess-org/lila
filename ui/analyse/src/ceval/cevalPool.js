var m = require('mithril');
var makeWorker = require('./cevalWorker');

module.exports = function(opts, nb) {

  var workers = [];
  var token = -1;

  var getWorker = function() {
    if (!workers.length)
      for (var i = 1; i <= nb; i++)
        workers.push(makeWorker(opts, 'W' + i));
    token = (token + 1) % workers.length;
    return workers[token];
  };

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
    stop: stopAll
  };
};
