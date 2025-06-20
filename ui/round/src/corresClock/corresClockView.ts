import { hl, type VNode } from 'lib/snabbdom';
import type { TopOrBottom } from 'lib/game/game';
import type { CorresClockController } from './corresClockCtrl';
import { moretime } from '../view/button';
import { formatClockTimeVerbal } from 'lib/game/clock/clockView';

const prefixInteger = (num: number, length: number): string =>
  (num / Math.pow(10, length)).toFixed(length).slice(2);

const bold = (x: string) => `<b>${x}</b>`;

function formatClockTime(time: Millis) {
  const date = new Date(time),
    minutes = prefixInteger(date.getUTCMinutes(), 2),
    seconds = prefixInteger(date.getSeconds(), 2);
  let hours: number,
    str = '';
  if (time >= 86400 * 1000) {
    // days : hours
    const days = date.getUTCDate() - 1;
    hours = date.getUTCHours();
    str += (days === 1 ? i18n.site.oneDay : i18n.site.nbDays(days)) + ' ';
    if (hours !== 0) str += i18n.site.nbHours(hours);
  } else if (time >= 3600 * 1000) {
    // hours : minutes
    hours = date.getUTCHours();
    str += bold(prefixInteger(hours, 2)) + ':' + bold(minutes);
  } else {
    // minutes : seconds
    str += bold(minutes) + ':' + bold(seconds);
  }
  return str;
}

export default function (
  ctrl: CorresClockController,
  color: Color,
  position: TopOrBottom,
  runningColor: Color,
): VNode {
  const millis = ctrl.millisOf(color),
    update = (el: HTMLElement) => {
      el.innerHTML = site.blindMode ? formatClockTimeVerbal(millis) : formatClockTime(millis);
    },
    isPlayer = ctrl.root.data.player.color === color,
    direction = document.dir === 'rtl' && millis < 86400 * 1000 ? 'ltr' : undefined;
  return hl(
    'div.rclock.rclock-correspondence.rclock-' + position,
    { class: { outoftime: millis <= 0, running: runningColor === color } },
    [
      ctrl.data.showBar &&
        hl('div.bar', [hl('span', { attrs: { style: `width: ${ctrl.timePercent(color)}%` } })]),
      hl('div.time', {
        attrs: direction && { style: `direction: ${direction}` },
        hook: {
          insert: vnode => update(vnode.elm as HTMLElement),
          postpatch: (_, vnode) => update(vnode.elm as HTMLElement),
        },
      }),
      !isPlayer && moretime(ctrl.root),
    ],
  );
}
