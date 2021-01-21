import * as button from '../view/button';
import * as game from 'game';
import RoundController from '../ctrl';
import { bind, justIcon } from '../util';
import { ClockElements, ClockController, Millis } from './clockCtrl';
import { h } from 'snabbdom'
import { Hooks } from 'snabbdom/hooks'
import { Player } from 'game';
import { Position } from '../interfaces';

export function renderClock(ctrl: RoundController, player: Player, position: Position) {
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
  }
  const timeHook: Hooks = {
    insert: (vnode) => update(vnode.elm as HTMLElement),
    postpatch: (_, vnode) => update(vnode.elm as HTMLElement)
  };
  return h('div.rclock.rclock-' + position, {
    class: {
      outoftime: millis <= 0,
      running: isRunning,
      emerg: millis < clock.emergMs
    }
  }, clock.opts.nvui ? [
    h('div.time', {
      attrs: { role: 'timer' },
      hook: timeHook
    })
  ] : [
      clock.showBar && game.bothPlayersHavePlayed(ctrl.data) ? showBar(ctrl, player.color) : undefined,
      h('div.time', {
        class: {
          hour: millis > 3600 * 1000
        },
        hook: timeHook
      }),
      renderBerserk(ctrl, player.color, position),
      isPlayer ? goBerserk(ctrl) : button.moretime(ctrl),
      tourRank(ctrl, player.color, position)
    ]);
}

const pad2 = (num: number): string => (num < 10 ? '0' : '') + num;
const sepHigh = '<sep>:</sep>';
const sepLow = '<sep class="low">:</sep>';

function formatClockTime(time: Millis, showTenths: boolean, isRunning: boolean, nvui: boolean) {
  const date = new Date(time);
  if (nvui) return (time >= 3600000 ? Math.floor(time / 3600000) + 'H:' : '') +
    date.getUTCMinutes() + 'M:' + date.getUTCSeconds() + 'S';
  const millis = date.getUTCMilliseconds(),
    sep = (isRunning && millis < 500) ? sepLow : sepHigh,
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
      if (anim === undefined || !anim.effect ||
        (anim.effect as KeyframeEffect).target !== el) {
        anim = el.animate(
          [
            { transform: 'scale(1)' },
            { transform: 'scale(0, 1)' }
          ], {
          duration: clock.barTime,
          fill: "both"
        }
        );
        clock.elements[color].barAnim = anim;
      }
      const remaining = clock.millisOf(color)
      anim.currentTime = clock.barTime - remaining;
      if (color === clock.times.activeColor) {
        // Calling play after animations finishes restarts anim
        if (remaining > 0) anim.play();
      } else anim.pause();
    } else {
      clock.elements[color].bar = el;
      el.style.transform = "scale(" + clock.timeRatio(clock.millisOf(color)) + ",1)";
    }
  };
  return h('div.bar', {
    class: { berserk: !!ctrl.goneBerserk[color] },
    hook: {
      insert: vnode => update(vnode.elm as HTMLElement),
      postpatch: (_, vnode) => update(vnode.elm as HTMLElement)
    }
  });
}

export function updateElements(clock: ClockController, els: ClockElements, millis: Millis) {
  if (els.time) els.time.innerHTML = formatClockTime(millis, clock.showTenths(millis), true, clock.opts.nvui);
  if (els.bar) els.bar.style.transform = "scale(" + clock.timeRatio(millis) + ",1)";
  if (els.clock) {
    const cl = els.clock.classList;
    if (millis < clock.emergMs) cl.add('emerg');
    else if (cl.contains('emerg')) cl.remove('emerg');
  }
}

function showBerserk(ctrl: RoundController, color: Color): boolean {
  return !!ctrl.goneBerserk[color] && ctrl.data.game.turns <= 1 && game.playable(ctrl.data);
}

function renderBerserk(ctrl: RoundController, color: Color, position: Position) {
  return showBerserk(ctrl, color) ? h('div.berserked.' + position, justIcon('`')) : null;
}

function goBerserk(ctrl: RoundController) {
  if (!game.berserkableBy(ctrl.data)) return;
  if (ctrl.goneBerserk[ctrl.data.player.color]) return;
  return h('button.fbt.go-berserk', {
    attrs: {
      title: 'GO BERSERK! Half the time, no increment, bonus point',
      'data-icon': '`'
    },
    hook: bind('click', ctrl.goBerserk)
  });
}

function tourRank(ctrl: RoundController, color: Color, position: Position) {
  const d = ctrl.data, ranks = d.tournament?.ranks || d.swiss?.ranks;
  return (ranks && !showBerserk(ctrl, color)) ?
    h('div.tour-rank.' + position, {
      attrs: { title: 'Current tournament rank' }
    }, '#' + ranks[color]) : null;
}
