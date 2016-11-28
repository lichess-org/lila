var m = require('mithril');

function yourTurn(ctrl) {
  var turnColor = ctrl.ground.data.turnColor;
  var puzzleColor = ctrl.data.puzzle.color;
  return m('div.your_turn.player.' + turnColor, [
    m('div.no-square', m('piece.king.' + turnColor)),
    m('div.instruction', [
      m('strong', ctrl.trans.noarg(turnColor == puzzleColor ? 'yourTurn' : 'waiting')),
      m('em', ctrl.trans.noarg(puzzleColor == 'white' ? 'findTheBestMoveForWhite' : 'findTheBestMoveForBlack'))
    ])
  ]);
}

module.exports = function(ctrl) {

  return m('div.feedback', yourTurn(ctrl));
};
