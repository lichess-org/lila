var m = require('mithril');
var makeWorker = require('./cevalWorker');

module.exports = function(opts, nb) {

  var workers = [];
  var token = -1;

  var getWorker = function() {
    if (workers.length >= nb) {
      token = (token + 1) % workers.length;
      return workers[token];
    } else {
      var worker = makeWorker(opts, 'W' + (workers.length + 1));
      workers.push(worker);
      return worker;
    }
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
