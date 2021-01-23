import StormCtrl from '../ctrl';
import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import {defined} from 'common';

export default function renderClock(ctrl: StormCtrl): VNode {
  return h('div.storm__clock', {
    hook: {
      insert(node) {
        const el: any = node.elm;
        renderIn(ctrl, el);
        const schedule = () => {
          const millis = ctrl.clockMillis();
          if (millis) el.refreshTimeout = setTimeout(() => {
            renderIn(ctrl, el);
            if (ctrl.clockMillis()) schedule();
          }, millis % 1000);
        };
        schedule();
      },
      destroy(node) {
        const el: any = node.elm;
        if (el.refreshTimeout) clearTimeout(el.refreshTimeout);
      }
    }
  });
}

function renderIn(ctrl: StormCtrl, el: HTMLElement) {
  const millis = ctrl.clockMillis();
  el.innerText = formatMs(defined(millis) ? millis : ctrl.vm.clockBudget);
}

const pad = (x: number): string => (x < 10 ? '0' : '') + x;

const formatMs = (msTime: number): string => {
  const date = new Date(Math.max(0, msTime + 500)),
    minutes = date.getUTCMinutes(),
    seconds = date.getUTCSeconds();
  return minutes + ':' + pad(seconds);
}
