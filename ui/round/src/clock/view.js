var m = require('mithril');
var classSet = require('common').classSet;
var button = require('../view/button');
var game = require('game').game;

function renderClock(ctrl, player, position) {
  var time = ctrl.clock.data[player.color];
  var running = ctrl.isClockRunning() && ctrl.data.game.player === player.color;
  var isPlayer = ctrl.data.player.color === player.color;
  return [
    m('div', {
      class: 'clock clock_' + player.color + ' clock_' + position + ' ' + classSet({
        'outoftime': !time,
        'running': running,
        'emerg': time < ctrl.clock.data.emerg
      })
    }, [
      showBar(ctrl.clock, player.color, time, ctrl.vm.goneBerserk[player.color]),
      m('div.time', {
        config: function(el, isUpdate, ctx) {
          if (player.color !== ctx.color) {
            ctrl.clock.elements[player.color].time = el;
            ctx.color = player.color;
          }
          ctrl.clock.elements[player.color].time.innerHTML = formatClockTime(ctrl.clock.data, time * 1000, running);
        }
      }),
      renderBerserk(ctrl, player.color, position),
      isPlayer ? goBerserk(ctrl) : button.moretime(ctrl),
      tourRank(ctrl, player.color, position)
    ])
  ];
}

function prefixInteger(num, length) {
  return (num / Math.pow(10, length)).toFixed(length).substr(2);
}

var sepHigh = '<sep>:</sep>';
var sepLow = '<sep class="low">:</sep>';

function formatClockTime(data, time, running) {
  var date = new Date(time);
  var minutes = prefixInteger(date.getUTCMinutes(), 2);
  var secs = date.getSeconds();
  var seconds = prefixInteger(secs, 2);
  var tenths = Math.floor(date.getMilliseconds() / 100);
  var hundredths = function() {
    return Math.floor(date.getMilliseconds() / 10) - tenths * 10;
  };
  var sep = (running && tenths < 5) ? sepLow : sepHigh;
  if ((data.showTenths == 2 && time < 3600000) || (data.showTenths == 1 && time < 10000)) {
    var showHundredths = !running && secs < 1;
    return minutes + sep + seconds +
    '<tenths><sep>.</sep>' + tenths + (showHundredths ? '<huns>' + hundredths() + '</huns>' : '') + '</tenths>';
  } else if (time >= 3600000) {
    var hours = prefixInteger(date.getUTCHours(), 2);
    return hours + sepHigh + minutes + sep + seconds;
  } else
  return minutes + sep + seconds;
}

function barWidth(data, time) {
  return Math.max(0, Math.min(100, (time / data.barTime) * 100)) + '%';
}

function showBar(ctrl, color, time, berserk) {
  return ctrl.data.showBar ? m('div', {
    class: 'bar' + (berserk ? ' berserk' : '')
  }, m('span', {
    config: function(el, isUpdate, ctx) {
      if (color !== ctx.color) {
        ctrl.elements[color].bar = el;
        ctx.color = color;
      }
      ctrl.elements[color].bar.style.width = barWidth(ctrl.data, time);
    }
  })) : null;
}

function updateElements(data, elements, color) {
  var els = elements[color];
  if (els) {
    els.time.innerHTML = formatClockTime(data, data[color] * 1000, true);
    if (els.bar) els.bar.style.width = barWidth(data, data[color]);
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
