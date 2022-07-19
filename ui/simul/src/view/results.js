var m = require('mithril');
var status = require('game/status');

var NumberFirstRegex = /^(\d+)\s(.+)$/;
var NumberLastRegex = /^(.+)\s(\d+)$/;

function splitNumber(s) {
  var found;
  if ((found = s.match(NumberFirstRegex))) return [m('div.number', found[1]), m('div.text', found[2])];
  if ((found = s.match(NumberLastRegex))) return [m('div.number', found[2]), m('div.text', found[1])];
  return m('div.text', s);
}

function trans(ctrl, key, cond) {
  return splitNumber(ctrl.trans.plural(key, ctrl.data.pairings.filter(cond).length));
}

module.exports = function (ctrl) {
  return m('div.results', [
    m(
      'div',
      trans(ctrl, 'nbPlaying', function (p) {
        return p.game.status < status.ids.aborted;
      })
    ),
    m(
      'div',
      trans(ctrl, 'nbWins', function (p) {
        return p.wins === false;
      })
    ),
    m(
      'div',
      trans(ctrl, 'nbDraws', function (p) {
        return p.game.status >= status.ids.mate && p.wins === null;
      })
    ),
    m(
      'div',
      trans(ctrl, 'nbLosses', function (p) {
        return p.wins === true;
      })
    ),
  ]);
};
