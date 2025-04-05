import { throttle } from 'lib/async';
import PlayCtrl from '../playCtrl';
import { displayColumns } from 'lib/device';

const scrollMax = 99999;

export const autoScroll = throttle(100, (movesEl: HTMLElement, ctrl: PlayCtrl) =>
  window.requestAnimationFrame(() => {
    let st: number | undefined;
    if (ctrl.board.onPly < 3) st = 0;
    else if (ctrl.isOnLastPly()) st = scrollMax;
    else {
      const plyEl = movesEl.querySelector('.current') as HTMLElement | undefined;
      if (plyEl)
        st =
          displayColumns() === 1
            ? plyEl.offsetLeft - movesEl.offsetWidth / 2 + plyEl.offsetWidth / 2
            : plyEl.offsetTop - movesEl.offsetHeight / 2 + plyEl.offsetHeight / 2;
    }
    if (typeof st === 'number') {
      if (st === scrollMax) movesEl.scrollLeft = movesEl.scrollTop = st;
      else if (displayColumns() === 1) movesEl.scrollLeft = st;
      else movesEl.scrollTop = st;
    }
  }),
);
