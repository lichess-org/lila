var m = require('mithril');
var bindOnce = require('common').bindOnce;

function play(ctrl) {
  var turnColor = ctrl.ground.data.turnColor;
  var puzzleColor = ctrl.data.puzzle.color;
  return m('div.feedback.play', [
    m('div.player.' + turnColor, [
      m('div.no-square', m('piece.king.' + turnColor)),
      m('div.instruction', [
        m('strong', ctrl.trans.noarg(turnColor == puzzleColor ? 'yourTurn' : 'waiting')),
        m('em', ctrl.trans.noarg(puzzleColor == 'white' ? 'findTheBestMoveForWhite' : 'findTheBestMoveForBlack'))
      ])
    ]),
    ctrl.vm.canViewSolution ? m('div.view_solution',
      m('a.button', {
        config: bindOnce('click', ctrl.viewSolution)
      }, 'View the solution')
    ) : null
  ]);
}

function view(ctrl) {
  return m('div.feedback.view', [
    'view'
  ]);
}

module.exports = function(ctrl) {

  if (ctrl.vm.mode === 'play') return play(ctrl);
  if (ctrl.vm.mode === 'view') return view(ctrl);
};
