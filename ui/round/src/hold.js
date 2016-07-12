var holds = [];
var nb = 8;
var was = false;
var sent = false;
var premoved = false;
var variants = ['standard', 'crazyhouse'];

function register(socket, meta, ply) {
  if (meta.premove) {
    premoved = true;
    return;
  }
  if (premoved || !meta.holdTime || ply > 30) return;
  holds.push(meta.holdTime);
  var set = false;
  if (holds.length > nb) {
    holds.shift();
    var mean = holds.reduce(function(a, b) {
      return a + b;
    }) / nb;
    if (mean > 2 && mean < 100) {
      var diffs = holds.map(function(a) {
        return Math.pow(a - mean, 2);
      });
      var sd = Math.sqrt(diffs.reduce(function(a, b) {
        return a + b;
      }) / nb);
      set = sd < 16;
    }
  }
  if (set || was) $('.manipulable .cg-board').toggleClass('hold', set);
  if (set && !sent) {
    socket.send('hold', {
      mean: Math.round(mean),
      sd: Math.round(sd)
    });
    sent = true;
  }
  was = set;
}

function find(el, d) {
  try {
    var prev, w, done = false;
    [].forEach.call(el.querySelectorAll('square'), function(n) {
      w = n.offsetWidth;
      if (!done && prev && w !== prev) {
        if (window.getComputedStyle(n, null).getPropertyValue("border")[0] !== '0' && !n.classList.contains('last-move')) {
          done = true;
          $.post('/jslog/' + d.game.id + d.player.id);
        }
      }
      prev = w;
    });
  } catch (e) {}
}

module.exports = {
  applies: function(data) {
    return variants.indexOf(data.game.variant.key) !== -1;
  },
  register: register,
  find: find
};
