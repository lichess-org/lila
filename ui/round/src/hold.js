// Register move hold times and send socket alerts

var holds = [];
var nb = 6;
var was = false;

var register = function(socket, hold) {
  if (!hold) return;
  holds.push(hold);
  var set = false;
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
      set = sd < 15;
    }
  }
  if (set || was) $('.manipulable .cg-board').toggleClass('sha', set);
  if (set) socket.send('hold', {
    mean: Math.round(mean),
    sd: Math.round(sd)
  });
  was = set;
};

module.exports = {
  register: register
};
