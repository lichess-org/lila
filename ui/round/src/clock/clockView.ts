import { h } from 'snabbdom'
import * as button from '../view/button';
import { bind, justIcon } from '../util';
import { game } from 'game';
import RoundController from '../ctrl';
import { ClockElements, ClockController, TenthsPref, Millis } from './clockCtrl';
import { Player } from 'game';
import { Position } from '../interfaces';

export function renderClock(ctrl: RoundController, player: Player, position: Position) {
  const clock = ctrl.clock!,
  millis = clock.millisOf(player.color),
  isPlayer = ctrl.data.player.color === player.color,
  isRunning = player.color === clock.times.activeColor;
  const update = (el: HTMLElement) => {
    const els = clock.elements[player.color]
    els.time = el;
    els.clock = el.parentElement!;
    el.innerHTML = formatClockTime(clock.showTenths, millis, isRunning);
  }
  return h('div.clock.clock_' + player.color + '.clock_' + position, {
    class: {
      outoftime: millis <= 0,
      running: isRunning,
      emerg: millis < clock.emergMs
    }
  }, [
    showBar(clock, clock.elements[player.color], millis, !!ctrl.goneBerserk[player.color]),
    h('div.time', {
      hook: {
        insert: vnode => update(vnode.elm as HTMLElement),
        postpatch: (_, vnode) => update(vnode.elm as HTMLElement)
      }
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

function formatClockTime(showTenths: TenthsPref, time: Millis, isRunning: boolean) {
  const date = new Date(time),
  millis = date.getUTCMilliseconds(),
  sep = (isRunning && millis < 500) ? sepLow : sepHigh,
  baseStr = pad2(date.getUTCMinutes()) + sep + pad2(date.getUTCSeconds());
  if (time >= 3600000) {
    const hours = pad2(Math.floor(time / 3600000));
    return hours + sepHigh + baseStr;
  } else if (time >= 10000 && showTenths !== 2 || showTenths === 0) {
    return baseStr;
  } else {
    let tenthsStr = Math.floor(millis / 100).toString();
    if (!isRunning && time < 1000) {
      tenthsStr += '<huns>' + (Math.floor(millis / 10) % 10) + '</huns>';
    }

    return baseStr + '<tenths><sep>.</sep>' + tenthsStr + '</tenths>';
  }
}

function showBar(ctrl: ClockController, els: ClockElements, millis: Millis, berserk: boolean) {
  const update = (el: HTMLElement) => {
    els.bar = el;
    el.style.width = ctrl.timePercent(millis) + '%';
  };
  return ctrl.showBar ? h('div.bar', {
    class: { berserk }
  }, [
    h('span', {
      hook: {
        insert: vnode => update(vnode.elm as HTMLElement),
        postpatch: (_, vnode) => update(vnode.elm as HTMLElement)
      }
    })
  ]) : null;
}

export function updateElements(clock: ClockController, els: ClockElements, millis: Millis) {
  if (els.time) els.time.innerHTML = formatClockTime(clock.showTenths, millis, true);
  if (els.bar) els.bar.style.width = clock.timePercent(millis) + '%';
  if (els.clock) els.clock.classList.toggle("emerg", millis < clock.emergMs);
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
    attrs: { 'data-hint': "GO BERSERK! Half the time, bonus point" },
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
