import { h } from 'snabbdom';
import { Millis } from '../clock/clockCtrl';
import { Position } from '../interfaces';
import { CorresClockController } from './corresClockCtrl';
import { moretime } from '../view/button';

const prefixInteger = (num: number, length: number): string => (num / Math.pow(10, length)).toFixed(length).substr(2);

const bold = (x: string) => `<b>${x}</b>`;

function formatClockTime(trans: Trans, time: Millis) {
  const date = new Date(time),
    minutes = prefixInteger(date.getUTCMinutes(), 2),
    seconds = prefixInteger(date.getSeconds(), 2);
  let hours: number,
    str = '';
  if (time >= 86400 * 1000) {
    // days : hours
    const days = date.getUTCDate() - 1;
    hours = date.getUTCHours();
    str += (days === 1 ? trans('oneDay') : trans.plural('nbDays', days)) + ' ';
    if (hours !== 0) str += trans.plural('nbHours', hours);
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
  trans: Trans,
  color: Color,
  position: Position,
  runningColor: Color
) {
  const millis = ctrl.millisOf(color),
    update = (el: HTMLElement) => {
      el.innerHTML = formatClockTime(trans, millis);
    },
    isPlayer = ctrl.root.data.player.color === color;
  return h(
    'div.rclock.rclock-correspondence.rclock-' + position,
    {
      class: {
        outoftime: millis <= 0,
        running: runningColor === color,
      },
    },
    [
      ctrl.data.showBar
        ? h('div.bar', [
            h('span', {
              attrs: { style: `width: ${ctrl.timePercent(color)}%` },
            }),
          ])
        : null,
      h('div.time', {
        hook: {
          insert: vnode => update(vnode.elm as HTMLElement),
          postpatch: (_, vnode) => update(vnode.elm as HTMLElement),
        },
      }),
      isPlayer ? null : moretime(ctrl.root),
    ]
  );
}
