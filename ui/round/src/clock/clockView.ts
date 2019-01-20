import { h } from 'snabbdom'
import { Hooks } from 'snabbdom/hooks'
import * as button from '../view/button';
import { bind, justIcon } from '../util';
import { game } from 'game';
import RoundController from '../ctrl';
import { ClockElements, ClockController, Millis } from './clockCtrl';
import { Player } from 'game';
import { Position } from '../interfaces';

export function renderClock(ctrl: RoundController, player: Player, position: Position) {
  const clock = ctrl.clock!,
    millis = clock.millisOf(player.color),
    isPlayer = ctrl.data.player.color === player.color,
    isRunning = player.color === clock.times.activeColor;
  const update = (el: HTMLElement) => {
    const els = clock.elements[player.color];
    els.time = el;
    els.clock = el.parentElement!;
    el.innerHTML = formatClockTime(millis, clock.showTenths(millis), isRunning, clock.blind);
  }
  const timeHook: Hooks = {
    insert: (vnode) => update(vnode.elm as HTMLElement),
    postpatch: (_, vnode) => update(vnode.elm as HTMLElement)
  };
  return h('div.clock.clock_' + player.color + '.clock_' + position, {
    class: {
      outoftime: millis <= 0,
      running: isRunning,
      emerg: millis < clock.emergMs
    }
  }, clock.blind ? [
    h('div.time', {
      attrs: { role: 'timer' },
      hook: timeHook
    })
  ] : [
    clock.showBar ? showBar(clock, clock.elements[player.color], millis, !!ctrl.goneBerserk[player.color]) : undefined,
    h('div.time', {
      attrs: { title: `${player.color} clock` },
      hook: timeHook
    }),
    renderBerserk(ctrl, player.color, position),
    isPlayer ? goBerserk(ctrl) : button.moretime(ctrl),
    tourRank(ctrl, player.color, position)
  ]);
}

function pad2(num: number): string {
  return (num < 10 ? '0' : '') + num;
}

const sepHigh = '<sep>:</sep>';
const sepLow = '<sep class="low">:</sep>';

function formatClockTime(time: Millis, showTenths: boolean, isRunning: boolean, blind: boolean) {
  const date = new Date(time);
  if (blind) return (time >= 3600000 ? Math.floor(time / 3600000) + 'H:' : '') +
    (date.getUTCMinutes() + 'M:') +
    date.getUTCSeconds() + 'S';
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

function showBar(ctrl: ClockController, els: ClockElements, millis: Millis, berserk: boolean) {
  const update = (el: HTMLElement) => {
    els.bar = el;
    el.style.transform = "scale(" + ctrl.timeRatio(millis) + ",1)";
  };
  return h('div.bar', {
    class: { berserk },
    hook: {
      insert: vnode => update(vnode.elm as HTMLElement),
      postpatch: (_, vnode) => update(vnode.elm as HTMLElement)
    }
  });
}

export function updateElements(clock: ClockController, els: ClockElements, millis: Millis) {
  if (els.time) els.time.innerHTML = formatClockTime(millis, clock.showTenths(millis), true, clock.blind);
  if (els.bar) els.bar.style.transform = "scale(" + clock.timeRatio(millis) + ",1)";
  if (els.clock) {
    if (millis < clock.emergMs) els.clock.classList.add("emerg");
    else els.clock.classList.remove("emerg");
  }
}

function showBerserk(ctrl: RoundController, color: Color): boolean {
  return !!ctrl.goneBerserk[color] && ctrl.data.game.turns <= 1 && game.playable(ctrl.data);
}

function renderBerserk(ctrl: RoundController, color: Color, position: Position) {
  return showBerserk(ctrl, color) ? h('div.berserk_alert.' + position, justIcon('`')) : null;
}

function goBerserk(ctrl: RoundController) {
  if (!game.berserkableBy(ctrl.data)) return;
  if (ctrl.goneBerserk[ctrl.data.player.color]) return;
  return h('button.fbt.berserk.hint--bottom-left', {
    attrs: { 'data-hint': "GO BERSERK! Half the time, no increment, bonus point" },
    hook: bind('click', ctrl.goBerserk)
  }, [
    h('span', justIcon('`'))
  ]);
}

function tourRank(ctrl: RoundController, color: Color, position: Position) {
  const d = ctrl.data;
  return (d.tournament && d.tournament.ranks && !showBerserk(ctrl, color)) ?
    h('div.tournament_rank.' + position, {
      attrs: {title: 'Current tournament rank'}
    }, '#' + d.tournament.ranks[color]) : null;
}
