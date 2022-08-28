import Lpv from 'lichess-pgn-viewer';
import { loadCssPath } from './component/assets';
import { Opts } from 'lichess-pgn-viewer/interfaces';
import { text as xhrText } from 'common/xhr';

export function autostart() {
  const lpvs = new Map<Element, Element>();
  var mouseTarget: HTMLElement | undefined;

  $('.lpv--autostart').each(function (this: HTMLElement) {
    const parent = this.parentElement;
    if (parent) {
      lpvs.set(parent, this);
      parent.onmouseenter = _ => {
        mouseTarget = parent;
        mouseTarget.setAttribute('class', 'lpv-mouseover');
      };
      parent.onmouseleave = _ => {
        if (mouseTarget) mouseTarget.removeAttribute('class');
        mouseTarget = undefined; // no need for fanciness, lpvs don't overlap
      };
    }
    Lpv(this, {
      pgn: this.dataset['pgn']!.replace(/<br>/g, '\n'),
      orientation: this.dataset['orientation'] as Color | undefined,
      lichess: location.origin,
      initialPly: this.dataset['ply'] as number | 'last',
    });
  });
  if (lpvs.size == 0) return;

  function bindKeyGoto(key: string, goto: string): void {
    window.Mousetrap.bind(key, () => eventTarget()?.dispatchEvent(new Event(goto)));
  }

  function eventTarget(): Element | undefined {
    let targetLpv = mouseTarget ? lpvs.get(mouseTarget) : undefined;
    if (targetLpv) return targetLpv;

    let maxVisibleY = 0;
    lpvs.forEach((lpv, div, __) => {
      const r = div.getBoundingClientRect();
      const visibleY = Math.min(window.innerHeight, r.bottom) - Math.max(0, r.top); // negative values fine
      if (visibleY > maxVisibleY) {
        maxVisibleY = visibleY;
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
