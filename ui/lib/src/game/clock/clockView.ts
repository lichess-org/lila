import type { ClockElements, ClockCtrl } from './clockCtrl';
import type { Hooks } from 'snabbdom';
import { hl, type VNode, LooseVNodes } from '@/snabbdom';
import { TopOrBottom } from '../game';
import { displayColumns } from '@/device';

export function renderClock(
  ctrl: ClockCtrl,
  color: Color,
  position: TopOrBottom,
  onTheSide: (color: Color, position: TopOrBottom) => LooseVNodes,
): VNode {
  const millis = ctrl.millisOf(color),
    isRunning = color === ctrl.times.activeColor;
  const update = (el: HTMLElement) => {
    const els = ctrl.elements[color],
      millis = ctrl.millisOf(color),
      isRunning = color === ctrl.times.activeColor;
    els.time = el;
    els.clock = el.parentElement!;
    el.innerHTML = formatClockTime(millis, ctrl.showTenths(millis), isRunning);
  };
  const timeHook: Hooks = {
    insert: vnode => update(vnode.elm as HTMLElement),
    postpatch: (_, vnode) => update(vnode.elm as HTMLElement),
  };
  return hl(
    // the player.color class ensures that when the board is flipped, the clock is redrawn. solves bug where clock
    // would be incorrectly latched to red color: https://github.com/lichess-org/lila/issues/10774
    `div.rclock.rclock-${position}.rclock-${color}`,
    { class: { outoftime: millis <= 0, running: isRunning, emerg: millis < ctrl.emergMs } },
    site.blindMode
      ? [hl('div.time', { attrs: { role: 'timer' }, hook: timeHook })]
      : [
          ctrl.showBar && ctrl.opts.bothPlayersHavePlayed() ? showBar(ctrl, color) : undefined,
          hl('div.time', { class: { hour: millis > 3600 * 1000 }, hook: timeHook }),
          onTheSide(color, position),
        ],
  );
}

const pad2 = (num: number): string => (num < 10 ? '0' : '') + num;
const sepHigh = '<sep>:</sep>';
const sepLow = '<sep class="low">:</sep>';

export function formatClockTimeVerbal(time: Millis): string {
  const totalSeconds = Math.floor(time / 1000);
  const days = Math.floor(totalSeconds / (3600 * 24));
  const hours = Math.floor((totalSeconds % (3600 * 24)) / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;

  const parts: string[] = [];
  if (days > 0) {
    parts.push(i18n.site.nbDays(days));
    if (hours > 0) parts.push(i18n.site.nbHours(hours));
  } else if (hours > 0) {
    parts.push(i18n.site.nbHours(hours));
    if (minutes > 0) parts.push(i18n.site.nbMinutes(minutes));
  } else {
    if (minutes > 0) parts.push(i18n.site.nbMinutes(minutes));
    if (seconds > 0 || parts.length === 0) parts.push(i18n.site.nbSeconds(seconds));
  }

  return parts.join(' ');
}

function formatClockTime(time: Millis, showTenths: boolean, isRunning: boolean) {
  const date = new Date(time);
  if (site.blindMode) return formatClockTimeVerbal(time);
  const millis = date.getUTCMilliseconds(),
    sep = isRunning && millis < 500 ? sepLow : sepHigh,
    baseStr = pad2(date.getUTCMinutes()) + sep + pad2(date.getUTCSeconds());
  if (time >= 3600000) {
    const hours = pad2(Math.floor(time / 3600000));
    return hours + sepHigh + baseStr;
  } else if (showTenths) {
    let tenthsStr = Math.floor(millis / 100).toString();
    if (!isRunning && time < 1000) {
      tenthsStr += '<huns>' + (Math.floor(millis / 10) % 10) + '</huns>';
    }

    return baseStr + '<tenths><sep>.</sep>' + tenthsStr + '</tenths>';
  } else {
    return baseStr;
  }
}

function showBar(ctrl: ClockCtrl, color: Color) {
  const update = (el: HTMLElement) => {
    if (el.animate !== undefined) {
      let anim = ctrl.elements[color].barAnim;
      if (anim === undefined || !anim.effect || (anim.effect as KeyframeEffect).target !== el) {
        anim = el.animate([{ transform: 'scale(1)' }, { transform: 'scale(0, 1)' }], {
          duration: ctrl.barTime,
          fill: 'both',
        });
        ctrl.elements[color].barAnim = anim;
      }
      const remaining = ctrl.millisOf(color);
      anim.currentTime = ctrl.barTime - remaining;
      if (color === ctrl.times.activeColor) {
        if (remaining > ctrl.barTime) {
          // Player was given more time than the duration of the animation. So we update the duration to reflect this.
          el.style.animationDuration = String(remaining / 1000) + 's';
        } else if (remaining > 0) {
          // Calling play after animations finishes restarts anim
          anim.play();
        }
      } else anim.pause();
    } else {
      ctrl.elements[color].bar = el;
      el.style.transform = 'scale(' + ctrl.timeRatio(ctrl.millisOf(color)) + ',1)';
    }
  };
  return displayColumns() === 1
    ? undefined
    : hl('div.bar', {
        class: { berserk: ctrl.opts.hasGoneBerserk(color) },
        hook: {
          insert: vnode => update(vnode.elm as HTMLElement),
          postpatch: (_, vnode) => update(vnode.elm as HTMLElement),
        },
      });
}

export function updateElements(clock: ClockCtrl, els: ClockElements, millis: Millis): void {
  if (els.time) els.time.innerHTML = formatClockTime(millis, clock.showTenths(millis), true);
  // 12/02/2025 Brave 1.74.51 android flickers the bar oninline transforms, even though .bar is display: none
  if (els.bar) els.bar.style.transform = 'scale(' + clock.timeRatio(millis) + ',1)';
  if (els.clock) {
    const cl = els.clock.classList;
    if (millis < clock.emergMs) cl.add('emerg');
    else if (cl.contains('emerg')) cl.remove('emerg');
  }
}
