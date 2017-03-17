var game = require('game').game;
var status = require('game').status;

var initialTitle = document.title;

var curFaviconIdx = 0;
var F = [
  '/assets/images/favicon-32-white.png',
  '/assets/images/favicon-32-black.png'
].map(function(path, i) {
  return function() {
    if (curFaviconIdx !== i) {
      document.getElementById('favicon').href = path;
      curFaviconIdx = i;
    }
  };
});

var tickerTimer = undefined;
function resetTicker() {
  tickerTimer = clearTimeout(tickerTimer);
  F[0]();
}

function startTicker() {
  function tick() {
    if (!document.hasFocus()) {
      F[1 - curFaviconIdx]();
      tickerTimer = setTimeout(tick, 1000);
    }
  }
  if (!tickerTimer) tickerTimer = setTimeout(tick, 200);
};

module.exports = {
  init: function(ctrl) { window.addEventListener('focus', resetTicker); },
  set: function(ctrl, text) {
    if (ctrl.data.player.spectator) return;
    if (!text) {
      if (status.finished(ctrl.data)) {
        text = ctrl.trans('gameOver');
      } else if (game.isPlayerTurn(ctrl.data)) {
        text = ctrl.trans('yourTurn');
        if (!document.hasFocus()) startTicker();
      } else {
        text = ctrl.trans('waitingForOpponent');
        resetTicker();
      }
    }
    document.title = text + " - " + initialTitle;
  }
};
