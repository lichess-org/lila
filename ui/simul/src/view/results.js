var m = require('mithril');
var status = require('game').status;

module.exports = function(ctrl) {
  return m('div.results', [
    m('div', [
      m('div.number', ctrl.data.pairings.filter(function(p) {
        return p.game.status < status.ids.mate;
      }).length),
      m('div.text', 'Playing')
    ]),
    m('div', [
      m('div.number', ctrl.data.pairings.filter(function(p) {
        return p.wins === false;
      }).length),
      m('div.text', 'Wins')
    ]),
    m('div', [
      m('div.number', ctrl.data.pairings.filter(function(p) {
        return p.game.status >= status.ids.mate && p.wins === null;
      }).length),
      m('div.text', 'Draws')
    ]),
    m('div', [
      m('div.number', ctrl.data.pairings.filter(function(p) {
        return p.wins === true;
      }).length),
      m('div.text', 'Losses')
    ])
  ]);
};
