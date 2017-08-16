import { renderIndexAndMove } from '../moveView';
import { opposite } from 'chessground/util';
import { RetroCtrl } from './retroCtrl';
import AnalyseCtrl from '../ctrl';
import { bind, dataIcon, spinner } from '../util';
import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

function skipOrViewSolution(ctrl: RetroCtrl) {
  return h('div.choices', [
    h('a', {
      hook: bind('click', ctrl.viewSolution, ctrl.redraw)
    }, ctrl.trans.noarg('viewTheSolution')),
    h('a', {
      hook: bind('click', ctrl.skip)
    }, 'Skip this move')
  ]);
}

function jumpToNext(ctrl: RetroCtrl) {
  return h('a.half.continue', {
    hook: bind('click', ctrl.jumpToNext)
  }, [
    h('i', { attrs: dataIcon('G') }),
    'Next'
  ]);
}

const minDepth = 8;
const maxDepth = 18;

function renderEvalProgress(node: Tree.Node): VNode {
  return h('div.progress', h('div', {
    attrs: {
      style: `width: ${node.ceval ? (100 * Math.max(0, node.ceval.depth - minDepth) / (maxDepth - minDepth)) + '%' : 0}`
    }
  }));
}

const feedback = {
  find(ctrl: RetroCtrl): VNode[] {
    return [
      h('div.player', [
        h('div.no-square', h('piece.king.' + ctrl.color)),
        h('div.instruction', [
          h('strong', [
            ...renderIndexAndMove({
              withDots: true,
              showGlyphs: true,
              showEval: false
            }, ctrl.current().fault.node),
            ' was played'
          ]),
          h('em', 'Find a better move for ' + ctrl.color),
          skipOrViewSolution(ctrl)
        ])
      ])
    ];
  },
  // user has browsed away from the move to solve
  offTrack(ctrl: RetroCtrl): VNode[] {
    return [
      h('div.player', [
        h('div.icon.off', '!'),
        h('div.instruction', [
          h('strong', 'You browsed away'),
          h('div.choices.off', [
            h('a', {
              hook: bind('click', ctrl.jumpToNext)
            }, 'Resume learning')
          ])
        ])
      ])
    ];
  },
  fail(ctrl: RetroCtrl): VNode[] {
    return [
      h('div.player', [
        h('div.icon', '✗'),
        h('div.instruction', [
          h('strong', 'You can do better'),
          h('em', 'Try another move for ' + ctrl.color),
          skipOrViewSolution(ctrl)
        ])
      ])
    ];
  },
  win(ctrl: RetroCtrl): VNode[] {
    return [
      h('div.half.top',
        h('div.player', [
          h('div.icon', '✓'),
          h('div.instruction', h('strong', ctrl.trans.noarg('goodMove')))
        ])
      ),
      jumpToNext(ctrl)
    ];
  },
  view(ctrl: RetroCtrl): VNode[] {
    return [
      h('div.half.top',
        h('div.player', [
          h('div.icon', '✓'),
          h('div.instruction', [
            h('strong', 'Solution:'),
            h('em', [
              'Best move was ',
              h('strong', renderIndexAndMove({
                withDots: true,
                showEval: false
              }, ctrl.current().solution.node)
              )
            ])
          ])
        ])
      ),
      jumpToNext(ctrl)
    ];
  },
  eval(ctrl: RetroCtrl): VNode[] {
    return [
      h('div.half.top',
        h('div.player.center', [
          h('div.instruction', [
            h('strong', 'Evaluating your move'),
            renderEvalProgress(ctrl.node())
          ])
        ])
      )
    ];
  },
  end(ctrl: RetroCtrl, flip: () => void, hasFullComputerAnalysis: () => boolean): VNode[] {
    if (!hasFullComputerAnalysis()) return [
      h('div.half.top',
        h('div.player', [
          h('div.icon', spinner()),
          h('div.instruction', 'Waiting for analysis...')
        ])
      )
    ];
    const nothing = !ctrl.completion()[1];
    return [
      h('div.player', [
        h('div.no-square', h('piece.king.' + ctrl.color)),
        h('div.instruction', [
          h('em', nothing ?
            'No mistakes found for ' + ctrl.color :
            'Done reviewing ' + ctrl.color + ' mistakes'),
          h('div.choices.end', [
            nothing ? null : h('a', {
              hook: bind('click', ctrl.reset)
            }, 'Do it again'),
            h('a', {
              hook: bind('click', flip)
            }, 'Review ' + opposite(ctrl.color) + ' mistakes')
          ])
        ])
      ])
    ];
  },
};

function renderFeedback(root: AnalyseCtrl, fb) {
  const ctrl: RetroCtrl = root.retro!;
  const current = ctrl.current();
  if (ctrl.isSolving() && current && root.path !== current.prev.path)
  return feedback.offTrack(ctrl);
  if (fb === 'find') return current ? feedback.find(ctrl) :
    feedback.end(ctrl, root.flip, root.hasFullComputerAnalysis);
  return feedback[fb](ctrl);
}

function renderTitle(ctrl: RetroCtrl): VNode {
  var completion = ctrl.completion();
  return h('div.title', [
    h('span', 'Learn from your mistakes'),
    h('span', Math.min(completion[0] + 1, completion[1]) + ' / ' + completion[1]),
    h('span.close', {
      attrs: dataIcon('L'),
      hook: bind('click', ctrl.close, ctrl.redraw)
    })
  ]);
}

export default function(root: AnalyseCtrl): VNode | undefined {
  const ctrl = root.retro;
  if (!ctrl) return;
  const fb = ctrl.feedback();
  return h('div.retro_box', [
    renderTitle(ctrl),
    h('div.feedback.' + fb, renderFeedback(root, fb))
  ]);
};
