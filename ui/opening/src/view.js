var chessground = require('chessground');
var m = require('mithril');

function renderTable(ctrl) {
  return m('div.table',
    m('div.table_inner', [])
  );
}

module.exports = function(ctrl) {
  return m('div#opening.training', [
    m('div.board_and_ground', [
      m('div', chessground.view(ctrl.chessground)),
      m('div.right', renderTable(ctrl))
    ]),
    m('div.center', [
    ])
  ]);
};
