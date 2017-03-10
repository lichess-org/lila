var game = require('game').game;
var status = require('game').status;

function visible() {
  return !document[['hidden', 'webkitHidden', 'mozHidden', 'msHidden'].find(function(k) {
    return k in document;
  })];
}

var initialTitle = document.title;
var tickDelay = 400;
var F = [
  '/assets/images/favicon-32-white.png',
  '/assets/images/favicon-32-black.png'
].map(function(path) {
  return function() {
    document.getElementById('favicon').href = path;
  };
});
var iteration = 0;
var tick = function(ctrl) {
  if (visible()) {
    if (iteration) F[0]();
  }
  else if (status.started(ctrl.data) && game.isPlayerTurn(ctrl.data)) {
    F[++iteration % 2]();
  }
  setTimeout(lichess.partial(tick, ctrl), tickDelay);
};

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
