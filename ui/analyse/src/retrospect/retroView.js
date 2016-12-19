var m = require('mithril');
var renderIndex = require('../treeView').renderIndex;
var renderMove = require('../treeView').renderMove;

function skipOrViewSolution(ctrl) {
  return m('div.skip_view', [
    m('a', {
      onclick: ctrl.viewSolution
    }, ctrl.trans.noarg('viewTheSolution')),
    m('a', {
      onclick: ctrl.skip
    }, 'Skip this move')
  ]);
}

function jumpToNext(ctrl) {
  return m('a.half.continue', {
    onclick: ctrl.jumpToNext
  }, [
    m('i[data-icon=G]'),
    'Next'
  ]);
}

var feedback = {
  find: function(ctrl) {
    return [
      m('div.player', [
        m('div.no-square', m('piece.king.' + ctrl.color)),
        m('div.instruction', [
          m('strong', [
            renderIndex(ctrl.current().fault.node.ply, true),
            renderMove({
              showGlyphs: true,
              showEval: false
            }, ctrl.current().fault.node),
            ' was played'
          ]),
          m('em', 'Find a better move for ' + ctrl.color),
          skipOrViewSolution(ctrl)
        ])
      ])
    ];
  },
  fail: function(ctrl) {
    return [
      m('div.player', [
        m('div.icon', '✗'),
        m('div.instruction', [
          m('strong', 'You can do better'),
          m('em', 'Try another move for ' + ctrl.color),
          skipOrViewSolution(ctrl)
        ])
      ])
    ];
  },
  win: function(ctrl) {
    return [
      m('div.half.top',
        m('div.player', [
          m('div.icon', '✓'),
          m('div.instruction', m('strong', ctrl.trans.noarg('goodMove')))
        ])
      ),
      jumpToNext(ctrl)
    ];
  },
  view: function(ctrl) {
    return [
      m('div.half.top',
        m('div.player', [
          m('div.icon', '✓'),
          m('div.instruction', [
            m('strong', 'Solution:'),
            m('em', [
              'Best move was ',
              m('strong', [
                renderIndex(ctrl.current().solution.node.ply, true),
                renderMove({
                  showEval: false
                }, ctrl.current().solution.node)
              ])
            ])
          ])
        ])
      ),
      jumpToNext(ctrl)
    ];
  },
  eval: function(ctrl) {
    return [
      m('div.half.top',
        m('div.player', [
          m('div.icon', m.trust(lichess.spinnerHtml)),
          m('div.instruction', 'Evaluating your move...')
        ])
      )
    ];
  },
  find: function(ctrl) {
    return [
      m('div.player', [
        m('div.no-square', m('piece.king.' + ctrl.color)),
        m('div.instruction', [
          m('strong', [
            renderIndex(ctrl.current().fault.node.ply, true),
            renderMove({
              showGlyphs: true,
              showEval: false
            }, ctrl.current().fault.node),
            ' was played'
          ]),
          m('em', 'Find a better move for ' + ctrl.color),
          skipOrViewSolution(ctrl)
        ])
      ])
    ];
  },
};

function renderFeedback(ctrl, fb) {
  if (fb === 'find') {
    var current = ctrl.current();
    if (current) return feedback.find(ctrl, current);
    else return feedback.end(ctrl);
  }
  if (fb === 'win') return feedback.win(ctrl);
  if (fb === 'fail') return feedback.fail(ctrl);
  if (fb === 'eval') return feedback.eval(ctrl);
  if (fb === 'view') return feedback.view(ctrl);
}

module.exports = function(root) {
  var ctrl = root.retro;
  if (!ctrl || !ctrl.current()) return;
  var fb = ctrl.feedback();
  return m('div', {
    class: 'retro_box ' + fb
  }, renderFeedback(ctrl, fb));
};
