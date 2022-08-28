import Lpv from 'lichess-pgn-viewer';
import { loadCssPath } from './component/assets';
import { Opts } from 'lichess-pgn-viewer/interfaces';
import { text as xhrText } from 'common/xhr';

export function autostart() {
  const lpvs = new Map<Element, Element>();

  $('.lpv--autostart').each(function (this: HTMLElement) {
    lpvs.set(this.parentElement!, this);
    Lpv(this, {
      pgn: this.dataset['pgn']!.replace(/<br>/g, '\n'),
      orientation: this.dataset['orientation'] as Color | undefined,
      lichess: location.origin,
      initialPly: this.dataset['ply'] as number | 'last',
    });
  });
  if (lpvs.size == 0) return;

  function bindKeyGoto(key: string, goto: string): void {
    window.Mousetrap.bind(key, () => keyboardTarget()?.dispatchEvent(new Event(goto)));
  }

  function keyboardTarget(): Element | undefined {
    let maxVisibleY = 0,
      targetLpv;
    lpvs.forEach((lpv, div, __) => {
      const r = div.getBoundingClientRect();
      const intersectionY = Math.min(window.innerHeight, r.bottom) - Math.max(0, r.top);
      if (intersectionY > maxVisibleY) {
        maxVisibleY = intersectionY;
        targetLpv = lpv;
      }
    });
    return targetLpv;
  }

  bindKeyGoto('left', 'prev');
  bindKeyGoto('right', 'next');
  bindKeyGoto('up', 'first');
  bindKeyGoto('down', 'last');
  bindKeyGoto('f', 'flip');
}

export const loadPgnAndStart = async (el: HTMLElement, url: string, opts: Opts) => {
  await loadCssPath('lpv');
  const pgn = await xhrText(url, {
    headers: {
      Accept: 'application/x-chess-pgn',
    },
  });
  return Lpv(el, { ...opts, pgn });
};
