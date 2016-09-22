var recorder = require('./recorder');
var holds = [];
var nb = 8;
var was = false;
var sent = {};
var premoved = false;
var variants = ['standard', 'crazyhouse'];

function register(ctrl, meta) {
  if (meta.premove && ctrl.vm.ply > 1) premoved = true;
  if (premoved || !meta.holdTime || ctrl.vm.ply > 30) {
    recorder.stop();
    return;
  }
  holds.push(meta.holdTime);
  var set = false;
  if (holds.length > nb) {
    holds.shift();
    var mean = holds.reduce(function(a, b) {
      return a + b;
    }) / nb;
    if (mean > 1 && mean < 140) {
      var diffs = holds.map(function(a) {
        return Math.pow(a - mean, 2);
      });
      var sd = Math.sqrt(diffs.reduce(function(a, b) {
        return a + b;
      }) / nb);
      set = sd < 14;
    }
  }
  if (set || was) {
    var mc = recorder.stop();
    mc = mc && mc > 2 && mc < 6;
    $('.manipulable .cg-board').toggleClass('bh1', set);
    $('.manipulable .cg-board').toggleClass('bh2', mc);
    if (set) recorder.start();
    if (set && !sent.hold) {
      ctrl.socket.send('hold', {
        mean: Math.round(mean),
        sd: Math.round(sd)
      });
      sent.hold = true;
    }
    if (set && !sent.ick) {
      post(ctrl.data, 'ick');
      sent.ick = true;
    }
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
