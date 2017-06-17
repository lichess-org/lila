import * as button from '../view/button';
import { bind, dataIcon } from '../util';
import { game } from 'game';

import { h } from 'snabbdom'

export function renderClock(ctrl, player, position) {
  const millis = ctrl.clock.millisOf(player.color);
  const isPlayer = ctrl.data.player.color === player.color;
  let isRunning = false;
  if (ctrl.vm.justMoved) isRunning = !isPlayer;
  else if (player.color === ctrl.data.game.player && ctrl.isClockRunning()) isRunning = true;
  const update = (el: HTMLElement) => {
    ctrl.clock.elements[player.color].time = el;
    el.innerHTML = formatClockTime(ctrl.clock.data, millis, isRunning);
  }
  return h('div.clock.clock_' + player.color + '.clock_' + position, {
    class: {
      outoftime: millis <= 0,
      running: isRunning,
      emerg: millis < ctrl.clock.emergMs
    }
  }, [
    showBar(ctrl.clock, player.color, ctrl.vm.goneBerserk[player.color]),
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

function pad2(num) {
  return (num < 10 ? '0' : '') + num;
}

var sepHigh = '<sep>:</sep>';
var sepLow = '<sep class="low">:</sep>';

function formatClockTime(data, time, isRunning) {
  var date = new Date(time);
  var millis = date.getUTCMilliseconds();
  var sep = (isRunning && millis < 500) ? sepLow : sepHigh;
  var baseStr = pad2(date.getUTCMinutes()) + sep + pad2(date.getUTCSeconds());
  if (time >= 3600000) {
    var hours = pad2(Math.floor(time / 3600000));
    return hours + sepHigh + baseStr;
  } else if (time >= 10000 && data.showTenths != 2 || data.showTenths == 0) {
    return baseStr;
  } else {
    var tenthsStr = Math.floor(millis / 100).toString();
    if (!isRunning && time < 1000) {
      tenthsStr += '<huns>' + (Math.floor(millis / 10) % 10) + '</huns>';
    }

    return baseStr + '<tenths><sep>.</sep>' + tenthsStr + '</tenths>';
  }
}

function showBar(ctrl, color, berserk) {
  const update = (el: HTMLElement) => {
    ctrl.elements[color].bar = el;
    el.style.width = ctrl.timePercent(color) + '%';
  };
  return ctrl.data.showBar ? h('div.bar', {
    class: { berserk: berserk }
  }, [
    h('span', {
      hook: {
        insert: vnode => update(vnode.elm as HTMLElement),
        postpatch: (_, vnode) => update(vnode.elm as HTMLElement)
      }
    })
  ]) : null;
}

export function updateElements(ctrl, color) {
  var els = ctrl.clock.elements[color];
  if (els) {
    els.time.innerHTML = formatClockTime(ctrl.clock.data, ctrl.clock.millisOf(color), true);
    if (els.bar) els.bar.style.width = ctrl.clock.timePercent(color) + '%';
  } else ctrl.redraw();
}

function showBerserk(ctrl, color) {
  return ctrl.vm.goneBerserk[color] &&
  ctrl.data.game.turns <= 1 &&
  game.playable(ctrl.data);
}

function renderBerserk(ctrl, color, position) {
  return showBerserk(ctrl, color) ? h('div.berserk_alert.' + position, {
    attrs: dataIcon('`')
  }) : null;
}

function goBerserk(ctrl) {
  if (!game.berserkableBy(ctrl.data)) return;
  if (ctrl.vm.goneBerserk[ctrl.data.player.color]) return;
  return h('button.fbt.berserk.hint--bottom-left', {
    attrs: { 'data-hint': "GO BERSERK! Half the time, bonus point" },
    hook: bind('click', ctrl.goBerserk)
  }, [
    h('span', { attrs: dataIcon('`') })
  ]);
}

function tourRank(ctrl, color, position) {
  var d = ctrl.data;
  return (d.tournament && d.tournament.ranks && !showBerserk(ctrl, color)) ?
  h('div.tournament_rank.' + position, {
    attrs: {title: 'Current tournament rank'}
  }, '#' + d.tournament.ranks[color]) : null;
}
