var m = require('mithril');
var simul = require('../simul');
var util = require('./util');

function pad2(num) {
  return (num < 10 ? '0' : '') + num;
}

function formatClockTime(seconds) {
  var date = new Date(seconds * 1000),
    millis = date.getUTCMilliseconds(),
    sep = (millis < 500) ? '<sep class="low">:</sep>' : '<sep>:</sep>',
  baseStr = pad2(date.getUTCMinutes()) + sep + pad2(date.getUTCSeconds());
  if (seconds >= 3600) {
    var hours = pad2(Math.floor(seconds / 3600));
    return hours + '<sep>:</sep>' + baseStr;
  }
  return baseStr;
}

module.exports = function(ctrl) {
  return (ctrl.toggleArbiter && ctrl.arbiterData && simul.amArbiter(ctrl)) ? m('div.arbiter-panel', [
    m('table.slist.user_list',
      m('thead', m('tr', [
        m('th', { colspan: 2 }, 'Arbiter control panel'),
        m('th', 'Clock'),
        m('th', 'Host clock'),
        m('th', m('span.hint--top-left', { 'data-hint': 'The percentage of moves in which the user left the game page.' }, 'Blurs')),
        m('th', m('span.hint--top-left', { 'data-hint': 'Stop the game by settling it as a win, draw or loss.' }, 'Stop game'))
      ])),
      m('tbody', ctrl.data.pairings.map(function(pairing) {
      var variant = util.playerVariant(ctrl, pairing.player);
      var data = ctrl.arbiterData.find( (d) => d.id == pairing.player.id)
      return m('tr', [
        m('td', util.player(pairing.player)),
        m('td.variant', { 'data-icon': variant.icon }),
        m('td', (data && data.clock !== undefined) ? m('div.time', m.trust(formatClockTime(data.clock))) : '-'),
        m('td', (data && data.hostClock !== undefined) ? m('div.time', m.trust(formatClockTime(data.hostClock))) : '-'),
        m('td', (data && data.blurs !== undefined) ? (data.blurs + '%') : '-' ),
        m('td.action', m('a.button.hint--top-left', {
          'data-icon': '2',
          'title': 'Stop game',
          onclick: function(e) { }
        }))
      ]);
    })))
  ]) : null;
}