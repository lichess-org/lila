import * as licon from 'common/licon';
import { moretime } from '../view/button';
import { playable, berserkableBy, bothPlayersHavePlayed, type Player } from 'game';
import type RoundController from '../ctrl';
import { justIcon } from '../util';
import type { ClockElements, ClockController } from './clockCtrl';
import type { Hooks } from 'snabbdom';
import { looseH as h, type VNode, bind } from 'common/snabbdom';
import type { Position } from '../interfaces';
import { isAndroid, isCol1 } from 'common/device';

export function renderClock(ctrl: RoundController, player: Player, position: Position): VNode {
  const clock = ctrl.clock!,
    millis = clock.millisOf(player.color),
    isPlayer = ctrl.data.player.color === player.color,
    isRunning = player.color === clock.times.activeColor;
  const update = (el: HTMLElement) => {
    const els = clock.elements[player.color],
      millis = clock.millisOf(player.color),
      isRunning = player.color === clock.times.activeColor;
    els.time = el;
    els.clock = el.parentElement!;
    el.innerHTML = formatClockTime(millis, clock.showTenths(millis), isRunning, clock.opts.nvui);
  };
  const timeHook: Hooks = {
    insert: vnode => update(vnode.elm as HTMLElement),
    postpatch: (_, vnode) => update(vnode.elm as HTMLElement),
  };
  return h(
    // the player.color class ensures that when the board is flipped, the clock is redrawn. solves bug where clock
    // would be incorrectly latched to red color: https://github.com/lichess-org/lila/issues/10774
    `div.rclock.rclock-${position}.rclock-${player.color}`,
    { class: { outoftime: millis <= 0, running: isRunning, emerg: millis < clock.emergMs } },
    clock.opts.nvui
      ? [h('div.time', { attrs: { role: 'timer' }, hook: timeHook })]
      : [
          clock.showBar && bothPlayersHavePlayed(ctrl.data) ? showBar(ctrl, player.color) : undefined,
          h('div.time', { class: { hour: millis > 3600 * 1000 }, hook: timeHook }),
          renderBerserk(ctrl, player.color, position),
          isPlayer ? goBerserk(ctrl) : moretime(ctrl),
          clockSide(ctrl, player.color, position),
        ],
  );
}

const pad2 = (num: number): string => (num < 10 ? '0' : '') + num;
const sepHigh = '<sep>:</sep>';
const sepLow = '<sep class="low">:</sep>';

function formatClockTime(time: Millis, showTenths: boolean, isRunning: boolean, nvui: boolean) {
  const date = new Date(time);
  if (nvui)
    return (
      (time >= 3600000 ? Math.floor(time / 3600000) + 'H:' : '') +
      date.getUTCMinutes() +
      'M:' +
      date.getUTCSeconds() +
      'S'
    );
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

function showBar(ctrl: RoundController, color: Color) {
  const clock = ctrl.clock!;
  const update = (el: HTMLElement) => {
    if (el.animate !== undefined) {
      let anim = clock.elements[color].barAnim;
      if (anim === undefined || !anim.effect || (anim.effect as KeyframeEffect).target !== el) {
        anim = el.animate([{ transform: 'scale(1)' }, { transform: 'scale(0, 1)' }], {
          duration: clock.barTime,
          fill: 'both',
        });
        clock.elements[color].barAnim = anim;
      }
      const remaining = clock.millisOf(color);
      anim.currentTime = clock.barTime - remaining;
      if (color === clock.times.activeColor) {
        if (remaining > clock.barTime) {
          // Player was given more time than the duration of the animation. So we update the duration to reflect this.
          el.style.animationDuration = String(remaining / 1000) + 's';
        } else if (remaining > 0) {
          // Calling play after animations finishes restarts anim
          anim.play();
        }
      } else anim.pause();
    } else {
      clock.elements[color].bar = el;
      el.style.transform = 'scale(' + clock.timeRatio(clock.millisOf(color)) + ',1)';
    }
  };
  return h('div.bar', {
    class: { berserk: !!ctrl.goneBerserk[color] },
    hook: {
      insert: vnode => update(vnode.elm as HTMLElement),
      postpatch: (_, vnode) => update(vnode.elm as HTMLElement),
    },
  });
}

export function updateElements(clock: ClockController, els: ClockElements, millis: Millis): void {
  if (els.time) els.time.innerHTML = formatClockTime(millis, clock.showTenths(millis), true, clock.opts.nvui);
  // 12/02/2025 Brave 1.74.51 android flickers the bar oninline transforms, even though .bar is display: none
  if (els.bar)
    if (!isAndroid() || !isCol1() || window.getComputedStyle(els.bar).display === 'block')
      els.bar.style.transform = 'scale(' + clock.timeRatio(millis) + ',1)';
  if (els.clock) {
    const cl = els.clock.classList;
    if (millis < clock.emergMs) cl.add('emerg');
    else if (cl.contains('emerg')) cl.remove('emerg');
  }
}

const showBerserk = (ctrl: RoundController, color: Color): boolean =>
  !!ctrl.goneBerserk[color] && ctrl.data.game.turns <= 1 && playable(ctrl.data);

const renderBerserk = (ctrl: RoundController, color: Color, position: Position) =>
  showBerserk(ctrl, color) ? h('div.berserked.' + position, justIcon(licon.Berserk)) : null;

const goBerserk = (ctrl: RoundController) => {
  if (!berserkableBy(ctrl.data)) return;
  if (ctrl.goneBerserk[ctrl.data.player.color]) return;
  return h('button.fbt.go-berserk', {
    attrs: { title: 'GO BERSERK! Half the time, no increment, bonus point', 'data-icon': licon.Berserk },
    hook: bind('click', ctrl.goBerserk),
  });
};

const clockSide = (ctrl: RoundController, color: Color, position: Position) => {
  const d = ctrl.data,
    ranks = d.tournament?.ranks || d.swiss?.ranks;
  return (
    ranks &&
    !showBerserk(ctrl, color) &&
    h('div.tour-rank.' + position, { attrs: { title: 'Current tournament rank' } }, '#' + ranks[color])
  );
};
