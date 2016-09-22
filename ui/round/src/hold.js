var recorder = require('./recorder');
var holds = [];
// var nb = 9;
var nb = 2;
var was = false;
var sent = false;
var premoved = false;
var variants = ['standard', 'crazyhouse'];

function register(socket, meta, ply) {
  console.log(ply, meta.holdTime);
  if (meta.premove && ply > 1) {
    console.log('premove!');
    premoved = true;
    return;
  }
  if (premoved || !meta.holdTime || ply > 30) return;
  holds.push(holds.length);
  var set = false;
  if (holds.length > nb) {
    holds.shift();
    var mean = holds.reduce(function(a, b) {
      return a + b;
    }) / nb;
    console.log(mean, 'mean');
    if (mean > 1 && mean < 110) {
      var diffs = holds.map(function(a) {
        return Math.pow(a - mean, 2);
      });
      var sd = Math.sqrt(diffs.reduce(function(a, b) {
        return a + b;
      }) / nb);
      console.log(sd, 'sd');
      set = sd < 16;
    }
  }
  if (set || was) {
    var mc = recorder.stop();
    mc = mc && mc < 6; 
    console.log('bh1: ' + set + ', bh2: ' + mc);
    $('.manipulable .cg-board').toggleClass('bh1', set);
    $('.manipulable .cg-board').toggleClass('bh2', mc);
    if (set) recorder.start();
  }
  if (set && !sent) {
    socket.send('hold', {
      mean: Math.round(mean),
      sd: Math.round(sd)
    });
    sent = true;
  }
  was = set;
}

function post(d, n) {
  $.post('/jslog/' + d.game.id + d.player.id + '?n=' + n);
}

function find(ctrl) {
  if (document.getElementById('robot_link')) post(ctrl.data, 'rcb');
}

module.exports = {
  applies: function(data) {
    return variants.indexOf(data.game.variant.key) !== -1;
  },
  register: register,
  find: find
};
