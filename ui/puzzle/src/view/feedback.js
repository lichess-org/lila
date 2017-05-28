var m = require('mithril');
var afterView = require('./after');

function viewSolution(ctrl) {
  return m('div.view_solution', {
      class: ctrl.vm.canViewSolution ? 'show' : '',
    },
    m('a.button', {
      onclick: ctrl.viewSolution
    }, ctrl.trans.noarg('viewTheSolution'))
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
    viewSolution(ctrl)
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
    viewSolution(ctrl)
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
    viewSolution(ctrl)
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

function loading() {
  return m('div.feedback.loading', m.trust(lichess.spinnerHtml));
}

module.exports = function(ctrl) {
  if (ctrl.vm.loading) return loading();
  if (ctrl.vm.mode === 'view') return afterView(ctrl);
  if (ctrl.vm.lastFeedback === 'init') return initial(ctrl);
  if (ctrl.vm.lastFeedback === 'good') return good(ctrl);
  if (ctrl.vm.lastFeedback === 'retry') return retry(ctrl);
  if (ctrl.vm.lastFeedback === 'fail') return fail(ctrl);
};
