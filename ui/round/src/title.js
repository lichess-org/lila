var game = require('game').game;
var status = require('game').status;
var partial = require('chessground').util.partial;

var initialTitle = document.title;
var tickDelay = 400;

var tick = function(ctrl) {
  if (status.started(ctrl.data) && game.isPlayerTurn(ctrl.data)) {
    document.title = document.title.indexOf('/\\/') === 0 ? '\\/\\ ' + document.title.replace(/\/\\\/ /, '') : '/\\/ ' + document.title.replace(/\\\/\\ /, '');
  }
  setTimeout(partial(tick, ctrl), tickDelay);
};

var init = function(ctrl) {
  if (!ctrl.data.opponent.ai && !ctrl.data.player.spectator) setTimeout(partial(tick, ctrl), tickDelay);
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
