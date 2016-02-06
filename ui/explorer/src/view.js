var chessground = require('chessground');
var m = require('mithril');

function renderMoveList(moves) {
  var moves = Object.keys(moves).map(function(move) {
    return moves[move];
  }).sort(function(a, b) {
    return b.total - a.total;
  });

  return m('table', [
    m('thead', [
      m('tr', [
        m('th', 'Move'),
        m('th', 'Total games'),
        m('th', 'White wins'),
        m('th', 'Draws'),
        m('th', 'Black wins')
      ])
    ]),
    m('tbody', moves.map(function(move) {
      return m('tr', [
        m('td', move.san),
        m('td', move.total),
        m('td', move.white),
        m('td', move.draws),
        m('td', move.black)
      ]);
    }))
  ]);
}

module.exports = function(ctrl) {
  return m('div.explorer', [
    chessground.view(ctrl.chessground),
    m('br'),
    ctrl.api() ? renderMoveList(ctrl.api().moves) : []
  ]);
};
