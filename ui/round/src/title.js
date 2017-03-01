var game = require('game').game;
var status = require('game').status;
var visible = require('./util').visible;

var initialTitle = document.title;
var tickDelay = 400;
var F = [
  '/assets/images/favicon-32-white.png',
  '/assets/images/favicon-32-black.png'
].map(function(path) {
  var link = document.createElement('link');
  link.type = 'image/x-icon';
  link.rel = 'shortcut icon';
  link.id = 'dynamic-favicon';
  link.href = path;
  return function() {
    var oldLink = document.getElementById('dynamic-favicon');
    if (oldLink) document.head.removeChild(oldLink);
    document.head.appendChild(link);
  };
});
var iteration = 0;
var tick = function(ctrl) {
  if (!visible() && status.started(ctrl.data) && game.isPlayerTurn(ctrl.data)) {
    F[++iteration % 2]();
  }
  setTimeout(lichess.partial(tick, ctrl), tickDelay);
};
visible(function(v) {
  if (v) F[0]();
});

var init = function(ctrl) {
  if (!ctrl.data.opponent.ai && !ctrl.data.player.spectator) setTimeout(lichess.partial(tick, ctrl), tickDelay);
};

var set = function(ctrl, text) {
  if (ctrl.data.player.spectator) return;
  if (!text) {
    if (status.finished(ctrl.data)) {
      text = ctrl.trans('gameOver');
    } else if (game.isPlayerTurn(ctrl.data)) {
      text = ctrl.trans('yourTurn');
    } else {
      text = ctrl.trans('waitingForOpponent');
    }
  }
  document.title = text + " - " + initialTitle;
};

module.exports = {
  init: init,
  set: set
};
