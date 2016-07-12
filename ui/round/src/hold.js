var holds = [];
var nb = 8;
var was = false;
var premoved = false;

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
    if (mean > 2 && mean < 150) {
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
  if (set) socket.send('hold', {
    mean: Math.round(mean),
    sd: Math.round(sd)
  });
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
    return data.game.variant.key === 'standard';
  },
  register: register,
  find: find
};
