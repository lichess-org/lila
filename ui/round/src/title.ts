import { game, status } from 'game';
import RoundController from './ctrl';

const initialTitle = document.title;

let curFavicon: string;
const faviconElem = document.getElementById('favicon') as HTMLAnchorElement;
class Favicon {
  constructor(readonly path: string) {}

  isSet = () => curFavicon === this.path;

  set() {
    if (curFavicon !== this.path) {
        faviconElem.href = this.path;
        curFavicon = this.path;
    }
  }
}

const favWhite = new Favicon('/assets/images/favicon-32-white.png'),
      favBlack = new Favicon('/assets/images/favicon-32-black.png'),
      favBlackFlip = new Favicon('/assets/images/favicon-32-black-flip.png'),
      favGrey = new Favicon('/assets/images/favicon-32-grey.png');

let tickerTimer: number | undefined;
function resetTicker() {
  if (tickerTimer !== undefined) {
    clearTimeout(tickerTimer);
    tickerTimer = undefined;
  }
}

function ticker() {
  (favGrey.isSet() ? favWhite : favGrey).set();
  tickerTimer = setTimeout(ticker, 1000);
}

export function init() {
  window.addEventListener('focus', resetTicker);
}

export function set(ctrl: RoundController, text?: string) {
  if (ctrl.data.player.spectator) return;
  if (!text) {
    if (status.finished(ctrl.data)) {
      text = ctrl.trans('gameOver');
      resetTicker();
      favBlack.set();
    } else if (game.isPlayerTurn(ctrl.data)) {
      text = ctrl.trans('yourTurn');
      favWhite.set();
      if (!document.hasFocus() && !tickerTimer)
        tickerTimer = setTimeout(ticker, 500);
    } else {
      text = ctrl.trans('waitingForOpponent');
      resetTicker();
      favBlackFlip.set();
    }
  }
  document.title = text + " - " + initialTitle;
}
