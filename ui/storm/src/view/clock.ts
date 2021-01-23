import StormCtrl from '../ctrl';
import { defined } from 'common';
import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import {getNow} from '../util';
import config from '../config';

let refreshInterval: Timeout;

export default function renderClock(ctrl: StormCtrl): VNode {
  return h('div.storm__clock', {
    hook: {
      insert(node) {
        const el = node.elm as HTMLDivElement;
        refreshInterval = setInterval(() => {
          renderIn(ctrl, el);
        }, 100);
      },
      destroy() {
        if (refreshInterval) clearInterval(refreshInterval);
      }
    }
  });
}

function renderIn(ctrl: StormCtrl, el: HTMLElement) {
  const clock = ctrl.vm.clock;
  const now = getNow();
  const millis = ctrl.clockMillis();
  const millisSinceMalus: number | undefined = clock.malusAt && (now - clock.malusAt < 1000 ? now - clock.malusAt : undefined);
  const showExtra = defined(millisSinceMalus) ? config.clock.malus * (1000 - millisSinceMalus) / 1000 : 0;
  el.innerText = formatMs(defined(millis) ? millis + showExtra : clock.budget);
  el.classList.toggle('malus', defined(millisSinceMalus));
}

const pad = (x: number): string => (x < 10 ? '0' : '') + x;

const formatMs = (millis: number): string => {
  const date = new Date(millis + 500),
    minutes = date.getUTCMinutes(),
    seconds = date.getUTCSeconds();
  return minutes + ':' + pad(seconds);
}
