import { game, status } from 'game';

const initialTitle = document.title;

var curFaviconIdx = 0;
const F = [
  '/assets/images/favicon-32-white.png',
  '/assets/images/favicon-32-black.png'
].map(function(path, i) {
  return function() {
    if (curFaviconIdx !== i) {
      const favicon = document.getElementById('favicon') as HTMLAnchorElement;
      favicon.href = path;
      curFaviconIdx = i;
    }
  };
});

var tickerTimer;
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
}

export function init() {
  window.addEventListener('focus', resetTicker);
}

export function set(ctrl, text) {
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
