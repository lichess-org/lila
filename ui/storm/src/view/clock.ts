import StormCtrl from '../ctrl';
import { defined } from 'common';
import { getNow } from 'puz/util';
import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import { TimeMod } from 'puz/interfaces';

let refreshInterval: Timeout;
let lastText: string;

export default function renderClock(ctrl: StormCtrl): VNode {
  const malus = ctrl.run.modifier.malus;
  const bonus = ctrl.run.modifier.bonus;
  return h('div.storm__clock', [
    h('div.storm__clock__time', {
      hook: {
        insert(node) {
          const el = node.elm as HTMLDivElement;
          el.innerText = formatMs(ctrl.run.clockMs);
          refreshInterval = setInterval(() => renderIn(ctrl, el), 100);
        },
        destroy() {
          if (refreshInterval) clearInterval(refreshInterval);
        },
      },
    }),
    !!malus && malus.at > getNow() - 900 ? h('div.storm__clock__malus', '-' + malus.seconds) : null,
    !!bonus && bonus.at > getNow() - 900 ? h('div.storm__clock__bonus', '+' + bonus.seconds) : null,
  ]);
}

function renderIn(ctrl: StormCtrl, el: HTMLElement) {
  if (!ctrl.run.startAt) return;
  const clock = ctrl.run.clockMs;
  const mods = ctrl.run.modifier;
  const now = getNow();
  const millis = ctrl.run.startAt + clock - getNow();
  const diffs = computeModifierDiff(now, mods.bonus) - computeModifierDiff(now, mods.malus);
  const text = formatMs(millis - diffs);
  if (text != lastText) el.innerText = text;
  lastText = text;
  if (millis < 1 && !ctrl.run.endAt) ctrl.naturalFlag();
}

const pad = (x: number): string => (x < 10 ? '0' : '') + x;

const formatMs = (millis: number): string => {
  const date = new Date(Math.max(0, Math.ceil(millis / 1000) * 1000)),
    minutes = date.getUTCMinutes(),
    seconds = date.getUTCSeconds();
  return minutes + ':' + pad(seconds);
};

function computeModifierDiff(now: number, mod?: TimeMod) {
  const millisSince: number | undefined = mod && (now - mod.at < 1000 ? now - mod.at : undefined);
  return defined(millisSince) ? mod!.seconds * 1000 * (1 - millisSince / 1000) : 0;
}
