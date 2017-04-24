var m = require('mithril');
var classSet = require('common').classSet;
var button = require('../view/button');
var game = require('game').game;

function renderClock(ctrl, player, position) {
  var millis = ctrl.clock.millisOf(player.color);
  var running = ctrl.isClockRunning() && ctrl.data.game.player === player.color;
  var isPlayer = ctrl.data.player.color === player.color;
  return [
    m('div', {
      class: 'clock clock_' + player.color + ' clock_' + position + ' ' + classSet({
        'outoftime': millis <= 0,
        'running': running,
        'emerg': millis < ctrl.clock.emergMs
      })
    }, [
      showBar(ctrl.clock, player.color, millis, ctrl.vm.goneBerserk[player.color]),
      m('div.time', {
        config: function(el, isUpdate, ctx) {
          if (player.color !== ctx.color) {
            ctrl.clock.elements[player.color].time = el;
            ctx.color = player.color;
          }
          ctrl.clock.elements[player.color].time.innerHTML = formatClockTime(ctrl.clock.data, millis, running);
        }
      }),
      renderBerserk(ctrl, player.color, position),
      isPlayer ? goBerserk(ctrl) : button.moretime(ctrl),
      tourRank(ctrl, player.color, position)
    ])
  ];
}

function pad2(num) {
  return (num < 10 ? '0' : '') + num;
}

var sepHigh = '<sep>:</sep>';
var sepLow = '<sep class="low">:</sep>';

function formatClockTime(data, time, running) {
  var date = new Date(time);
  var millis = date.getUTCMilliseconds();
  var sep = (running && millis < 500) ? sepLow : sepHigh;
  var baseStr = pad2(date.getUTCMinutes()) + sep + pad2(date.getUTCSeconds());
  if (time >= 3600000) {
    var hours = pad2(Math.floor(time / 3600000));
    return hours + sepHigh + baseStr;
  } else if (time >= 10000 && data.showTenths != 2 || data.showTenths == 0) {
    return baseStr;
  } else {
    var tenthsStr = Math.floor(millis / 100).toString();
    if (!running && time < 1000) {
      tenthsStr += '<huns>' + (Math.floor(millis / 10) % 10) + '</huns>';
    }

    return baseStr + '<tenths><sep>.</sep>' + tenthsStr + '</tenths>';
  }
}

function showBar(ctrl, color, millis, berserk) {
  return ctrl.data.showBar ? m('div', {
    class: 'bar' + (berserk ? ' berserk' : '')
  }, m('span', {
    config: function(el, isUpdate, ctx) {
      if (color !== ctx.color) {
        ctrl.elements[color].bar = el;
        ctx.color = color;
      }
      ctrl.elements[color].bar.style.width = ctrl.timePercent(color) + '%';
    }
  })) : null;
}

function updateElements(ctrl, color) {
  var els = ctrl.clock.elements[color];
  if (els) {
    els.time.innerHTML = formatClockTime(ctrl.clock.data, ctrl.clock.millisOf(color), true);
    if (els.bar) els.bar.style.width = ctrl.clock.timePercent(color) + '%';
  } else {
    m.redraw();
  }
}

function showBerserk(ctrl, color) {
  return ctrl.vm.goneBerserk[color] &&
      ctrl.data.game.turns <= 1 &&
      game.playable(ctrl.data);
}

function renderBerserk(ctrl, color, position) {
  if (showBerserk(ctrl, color)) return m('div', {
    class: 'berserk_alert ' + position,
    'data-icon': '`'
  });
}

function goBerserk(ctrl) {
  if (!game.berserkableBy(ctrl.data)) return;
  if (ctrl.vm.goneBerserk[ctrl.data.player.color]) return;
  return m('button', {
    class: 'fbt berserk hint--bottom-left',
    'data-hint': "GO BERSERK! Half the time, bonus point",
    onclick: ctrl.goBerserk
  }, m('span', {
    'data-icon': '`'
  }));
}

function tourRank(ctrl, color, position) {
  var d = ctrl.data;
  if (d.tournament && d.tournament.ranks && !showBerserk(ctrl, color)) return m('div', {
    class: 'tournament_rank ' + position,
    title: 'Current tournament rank'
  }, '#' + d.tournament.ranks[color]);
}

module.exports = {
  renderClock: renderClock,
  updateElements: updateElements
};
