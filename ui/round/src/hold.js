var socket = require('./socket');

// Register move hold times and send socket alerts

var holds = [];
var nb = 10;

var register = function(hold) {
  if (!hold) return;
  holds.push(hold);
  if (holds.length > nb) {
    holds.shift();
    var mean = holds.reduce(function(a, b) {
      return a + b;
    }) / nb;
    if (mean > 3 && mean < 80) {
      var diffs = holds.map(function(a) {
        return Math.pow(a - mean, 2);
      });
      var sd = Math.sqrt(diffs.reduce(function(a, b) {
        return a + b;
      }) / nb);
      if (sd < 10) socket.send('hold', {
        mean: Math.round(mean),
        sd: Math.round(sd)
      });
    }
  }
};

module.exports = {
  register: register
};
