var m = require('mithril');
var renderIndex = require('../treeView').renderIndex;
var renderMove = require('../treeView').renderMove;
var opposite = require('chessground').util.opposite;

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
  end: function(ctrl, flip) {
    var nothing = !ctrl.completion()[1];
    return [
      m('div.player', [
        m('div.no-square', m('piece.king.' + ctrl.color)),
        m('div.instruction', [
          m('em', nothing ?
            'No mistake found for ' + ctrl.color :
            'Done reviewing ' + ctrl.color + ' mistakes'),
          m('div.skip_view', [
            nothing ? null : m('a', {
              onclick: ctrl.reset
            }, 'Do it again'),
            m('a', {
              onclick: flip
            }, 'Review ' + opposite(ctrl.color) + ' mistakes')
          ])
        ])
      ])
    ];
  },
};

function renderFeedback(ctrl, fb, flip) {
  if (fb === 'find') {
    var current = ctrl.current();
    if (current) return feedback.find(ctrl, current);
    else return feedback.end(ctrl, flip);
  }
  if (fb === 'win') return feedback.win(ctrl);
  if (fb === 'fail') return feedback.fail(ctrl);
  if (fb === 'eval') return feedback.eval(ctrl);
  if (fb === 'view') return feedback.view(ctrl);
}

function renderTitle(ctrl, close) {
  var completion = ctrl.completion();
  return m('div.title', [
    m('span', 'Learn from your mistakes'),
    m('span', Math.min(completion[0] + 1, completion[1]) + ' / ' + completion[1]),
    m('span.close[data-icon=L]', {
      onclick: close
    })
  ]);
}

module.exports = function(root) {
  var ctrl = root.retro;
  if (!ctrl) return;
  var fb = ctrl.feedback();
  return m('div.retro_box', [
    renderTitle(ctrl, root.toggleRetro),
    m('div.feedback.' + fb, renderFeedback(ctrl, fb, root.flip))
  ]);
};
