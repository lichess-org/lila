var m = require('mithril');
var bindOnce = require('common').bindOnce;
var treePath = require('tree').path;

function viewSolution(ctrl) {
  return m('div.view_solution',
    m('a.button', {
      config: bindOnce('click', ctrl.viewSolution)
    }, 'View the solution')
  );
}

function initial(ctrl) {
  var puzzleColor = ctrl.data.puzzle.color;
  return m('div.feedback.play', [
    m('div.player', [
      m('div.no-square', m('piece.king.' + puzzleColor)),
      m('div.instruction', [
        m('strong', ctrl.trans.noarg('yourTurn')),
        m('em', ctrl.trans.noarg(puzzleColor === 'white' ? 'findTheBestMoveForWhite' : 'findTheBestMoveForBlack'))
      ])
    ]),
    ctrl.vm.canViewSolution ? viewSolution(ctrl) : null
  ]);
}

function good(ctrl) {
  return m('div.feedback.good', [
    m('div.player', [
      m('div.icon', '✓'),
      m('div.instruction', [
        m('strong', ctrl.trans.noarg('bestMove')),
        m('em', ctrl.trans.noarg('keepGoing'))
      ])
    ]),
    ctrl.vm.canViewSolution ? viewSolution(ctrl) : null
  ]);
}

function view(ctrl) {
  return m('div.feedback.view', [
    'view'
  ]);
}

function failed(ctrl) {
  return m('div.feedback.try', [
    m('div.player', [
      m('div.icon', '✗'),
      m('div.instruction', [
        m('strong', ctrl.trans.noarg('puzzleFailed')),
        m('em', ctrl.trans.noarg('butYouCanKeepTrying'))
      ])
    ]),
    viewSolution(ctrl)
  ]);
}

module.exports = function(ctrl) {

  if (ctrl.vm.mode === 'play') {
    if (ctrl.vm.initialNode.children.filter(function(node) {
      return node.puzzle === 'good';
    }).length) return good(ctrl);
    return initial(ctrl);
  }
  if (ctrl.vm.mode === 'try') return failed(ctrl);
  if (ctrl.vm.mode === 'view') return view(ctrl);
};
