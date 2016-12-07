var m = require('mithril');
var bindOnce = require('common').bindOnce;
var treePath = require('tree').path;
var afterView = require('./after');

function viewSolution(ctrl) {
  return m('div.view_solution',
    m('a.button', {
      config: bindOnce('click', ctrl.viewSolution)
    }, 'View the solution')
  );
}

function initial(ctrl) {
  var puzzleColor = ctrl.getData().puzzle.color;
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

function retry(ctrl) {
  return m('div.feedback.retry', [
    m('div.player', [
      m('div.icon', '!'),
      m('div.instruction', [
        m('strong', ctrl.trans.noarg('goodMove')),
        m('em', ctrl.trans.noarg('butYouCanDoBetter'))
      ])
    ]),
    ctrl.vm.canViewSolution ? viewSolution(ctrl) : null
  ]);
}

function fail(ctrl) {
  return m('div.feedback.fail', [
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

function win(ctrl) {
  return m('div.feedback.win', [
    m('div.player', [
      m('div.icon', '✓'),
      m('div.instruction', [
        m('strong', ctrl.trans.noarg('victory'))
      ])
    ]),
    'Show next button'
  ]);
}

module.exports = function(ctrl) {
  if (ctrl.vm.mode === 'view') return afterView(ctrl);
  if (ctrl.vm.lastFeedback === 'init') return initial(ctrl);
  if (ctrl.vm.lastFeedback === 'good') return good(ctrl);
  if (ctrl.vm.lastFeedback === 'retry') return retry(ctrl);
  if (ctrl.vm.lastFeedback === 'fail') return fail(ctrl);
  if (ctrl.vm.lastFeedback === 'win') return win(ctrl);
};
