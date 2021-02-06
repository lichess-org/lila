import RoundController from './ctrl';
import { aborted, finished } from 'game/status';
import { isPlayerTurn } from 'game';

const initialTitle = document.title;

let curFaviconIdx = 0;

const F = ['/assets/logo/lichess-favicon-32.png', '/assets/logo/lichess-favicon-32-invert.png'].map((path, i) => () => {
  if (curFaviconIdx !== i) {
    (document.getElementById('favicon') as HTMLAnchorElement).href = path;
    curFaviconIdx = i;
  }
});

let tickerTimer: Timeout | undefined;
function resetTicker() {
  if (tickerTimer) clearTimeout(tickerTimer);
  tickerTimer = undefined;
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

export function set(ctrl: RoundController, text?: string) {
  if (ctrl.data.player.spectator) return;
  if (!text) {
    if (aborted(ctrl.data) || finished(ctrl.data)) {
      text = ctrl.noarg('gameOver');
    } else if (isPlayerTurn(ctrl.data)) {
      text = ctrl.noarg('yourTurn');
      if (!document.hasFocus()) startTicker();
    } else {
      text = ctrl.noarg('waitingForOpponent');
      resetTicker();
    }
  }
  document.title = `${text} - ${initialTitle}`;
}
