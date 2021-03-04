import { defined } from 'common';
import { getNow } from '../util';
import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import { Run, TimeMod } from '../interfaces';

type OnFlag = () => void;

let refreshInterval: Timeout;
let lastText: string;

export default function renderClock(run: Run, onFlag: OnFlag): VNode {
  const malus = run.modifier.malus;
  const bonus = run.modifier.bonus;
  return h('div.puz-clock', [
    h('div.puz-clock__time', {
      hook: {
        insert(node) {
          const el = node.elm as HTMLDivElement;
          el.innerText = formatMs(run.clock.millis());
          refreshInterval = setInterval(() => renderIn(run, onFlag, el), 100);
        },
        destroy() {
          if (refreshInterval) clearInterval(refreshInterval);
        },
      },
    }),
    !!malus && malus.at > getNow() - 900 ? h('div.puz-clock__malus', '-' + malus.seconds) : null,
    !!bonus && bonus.at > getNow() - 900 ? h('div.puz-clock__bonus', '+' + bonus.seconds) : null,
  ]);
}

function renderIn(run: Run, onFlag: OnFlag, el: HTMLElement) {
  if (!run.clock.startAt) return;
  const mods = run.modifier;
  const now = getNow();
  const millis = run.clock.millis();
  const diffs = computeModifierDiff(now, mods.bonus) - computeModifierDiff(now, mods.malus);
  const text = formatMs(millis - diffs);
  if (text != lastText) el.innerText = text;
  lastText = text;
  if (millis < 1 && !run.endAt) onFlag();
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
