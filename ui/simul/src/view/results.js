var m = require('mithril');
var status = require('game/status');

var NumberFirstRegex = /^(\d+)\s(.+)$/;
var NumberLastRegex = /^(.+)\s(\d+)$/;

function splitNumber(s) {
  var found;
  if (found = s.match(NumberFirstRegex)) return [
    m('div.number', found[1]),
    m('div.text', found[2])
  ];
  if (found = s.match(NumberLastRegex)) return [
    m('div.number', found[2]),
    m('div.text', found[1])
  ];
  return m('div.text', s);
}

function trans(ctrl, key, cond) {
  return splitNumber(ctrl.trans.plural(key, ctrl.data.pairings.filter(cond).length));
}

function filterPlaying(p) { return p.game.status < status.ids.aborted; }
function filterWins(p) { return p.wins === false; }
function filterDraws(p) { return p.game.status >= status.ids.mate && p.wins === null; }
function filterLosses(p) { return p.wins === true; }

function currentPoints(pairings) {
  var wins = pairings.filter(filterWins).length,
    draws = pairings.filter(filterDraws).length;
  return wins + draws * 0.5;
}

function winningPercentage(pairings) {
  var finished = pairings.length - pairings.filter(filterPlaying).length;
  return finished == 0 ? 0 : (100 * currentPoints(pairings) / finished);
}

function requiredPoints(pairings, target) {
  return pairings.length * (target / 100.0);
}

function remainingPoints(pairings, target) {
  return pairings.length * (target / 100.0) - currentPoints(pairings);
}

function requiredWins(pairings, target) {
  var remaining = remainingPoints(pairings, target);
  if (remaining <= 0.5) return false;
  var remainingDec = remaining - Math.floor(remaining);
  return remainingDec > 0.5 ? Math.ceil(remaining) : Math.floor(remaining);
}

function requiredDraws(pairings, target) {
  var remaining = remainingPoints(pairings, target);
  if (remaining <= 0) return false;
  var remainingDec = remaining - Math.floor(remaining)
  return (remainingDec == 0 || remainingDec > 0.5) ? 0 : 1;
}

function targetDistance(pairings, target, trans) {
  var remaining = remainingPoints(pairings, target),
    targetReached = remaining <= 0,
    targetFailed = remaining > pairings.filter(filterPlaying).length;
  if (targetReached)
    return m('span.req.win', trans('succeeded'))
  else if (targetFailed)
    return m('span.req.loss', trans('failed'))
  var targetWins = requiredWins(pairings, target),
    targetDraws = requiredDraws(pairings, target);
  var targets = [];
  if (targetWins)
    targets.push(m('span.req.win', trans.plural('nbVictories', targetWins)))
  if (targetDraws)
    targets.push(m('span.req.draw', trans.plural('nbDraws', targetDraws)))
  return targets;
}

function toHalfPoints(points) {
  var decimal = points - Math.floor(points)
  if (decimal > 0.5)
    return Math.ceil(points)
  else if (decimal > 0)
    return Math.floor(points) + 0.5
  else
    return points
}

function relativeScoreRequired(pairings, target) {
  var remaining = toHalfPoints(requiredPoints(pairings, target)) - currentPoints(pairings)
  return remaining - pairings.filter(filterPlaying).length * 0.5
}

module.exports = function(ctrl) {
  var requiredScore = ctrl.data.targetPct && ctrl.toggleArbiter ? relativeScoreRequired(ctrl.data.pairings, ctrl.data.targetPct) : 0;
  if (ctrl.pref.draughtsResult) requiredScore *= 2;
  return [
    m('div.results', [
      m('div', trans(ctrl, 'nbPlaying', filterPlaying)),
      m('div', trans(ctrl, 'nbWins', filterWins)),
      m('div', trans(ctrl, 'nbDraws', filterDraws)),
      m('div', trans(ctrl, 'nbLosses', filterLosses))
    ]),
    ctrl.data.targetPct && !ctrl.toggleArbiter ? m('div.results.partial', [
      m('div.target', [
        m('div.number', Math.round(winningPercentage(ctrl.data.pairings) * 10) / 10 + '%'),
        m('div.text', ctrl.trans('winningPercentage'))
      ]),
      m('div.target', [
        m('div.number', ctrl.data.targetPct + '%'),
        m('div.text', ctrl.trans('targetPercentage'))
      ])
    ]) : null,
    !ctrl.data.targetPct ? null : !ctrl.toggleArbiter ?
      m('div.targets', [
        m('span', ctrl.trans('toReachTarget', '')),
        targetDistance(ctrl.data.pairings, ctrl.data.targetPct, ctrl.trans)
      ]) :
      m('div.results.partial', [
        m('div.targets', [
          m('span', ctrl.trans('winningPercentage') + ': '),
          Math.round(winningPercentage(ctrl.data.pairings) * 10) / 10 + '%'
        ]),
        m('div.targets', [
          m('span', ctrl.trans('relativeScoreRequired', '')),
          (requiredScore < 0 ? ' ' : ' +') + Math.round(requiredScore * 10) / 10
        ])
      ])
  ];
};
