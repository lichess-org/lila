import { transWithColorName } from 'common/colorName';
import { bind, dataIcon } from 'common/snabbdom';
import spinner from 'common/spinner';
import { isHandicap } from 'shogiops/handicaps';
import { VNode, h } from 'snabbdom';
import AnalyseCtrl from '../ctrl';
import { renderIndexAndMove } from '../moveView';
import { RetroCtrl } from './retroCtrl';
import { opposite } from 'shogiops/util';

function skipOrViewSolution(ctrl: RetroCtrl) {
  return h('div.choices', [
    h(
      'a',
      {
        hook: bind('click', ctrl.viewSolution, ctrl.redraw),
      },
      ctrl.noarg('viewTheSolution')
    ),
    h(
      'a',
      {
        hook: bind('click', ctrl.skip),
      },
      ctrl.noarg('skipThisMove')
    ),
  ]);
}

function jumpToNext(ctrl: RetroCtrl) {
  return h(
    'a.half.continue',
    {
      hook: bind('click', ctrl.jumpToNext),
    },
    [h('i', { attrs: dataIcon('G') }), ctrl.noarg('next')]
  );
}

const minDepth = 8;
const maxDepth = 18;

function renderEvalProgress(node: Tree.Node): VNode {
  return h(
    'div.progress',
    h('div', {
      attrs: {
        style: `width: ${
          node.ceval ? (100 * Math.max(0, node.ceval.depth - minDepth)) / (maxDepth - minDepth) + '%' : 0
        }`,
      },
    })
  );
}

const feedback = {
  find(ctrl: RetroCtrl): VNode[] {
    return [
      h('div.player', [
        h('div.no-square', h('piece.king.' + ctrl.color)),
        h('div.instruction', [
          h(
            'strong',
            ctrl.trans.vdom(
              'xWasPlayed',
              h(
                'move',
                renderIndexAndMove(
                  {
                    variant: ctrl.variant,
                    withDots: true,
                    showGlyphs: true,
                    showEval: false,
                    offset: ctrl.offset,
                  },
                  ctrl.current().fault.node
                )!
              )
            )
          ),
          h(
            'em',
            transWithColorName(
              ctrl.trans,
              'findBetterMoveForX',
              ctrl.color,
              isHandicap({ rules: ctrl.variant, sfen: ctrl.initialSfen })
            )
          ),
          skipOrViewSolution(ctrl),
        ]),
      ]),
    ];
  },
  // user has browsed away from the move to solve
  offTrack(ctrl: RetroCtrl): VNode[] {
    return [
      h('div.player', [
        h('div.icon.off', '!'),
        h('div.instruction', [
          h('strong', ctrl.noarg('youBrowsedAway')),
          h('div.choices.off', [
            h(
              'a',
              {
                hook: bind('click', ctrl.jumpToNext),
              },
              ctrl.noarg('resumeLearning')
            ),
          ]),
        ]),
      ]),
    ];
  },
  fail(ctrl: RetroCtrl): VNode[] {
    return [
      h('div.player', [
        h('div.icon', '✗'),
        h('div.instruction', [
          h('strong', ctrl.noarg('youCanDoBetter')),
          h(
            'em',
            transWithColorName(
              ctrl.trans,
              'tryAnotherMoveForX',
              ctrl.color,
              isHandicap({ rules: ctrl.variant, sfen: ctrl.initialSfen })
            )
          ),
          skipOrViewSolution(ctrl),
        ]),
      ]),
    ];
  },
  win(ctrl: RetroCtrl): VNode[] {
    return [
      h(
        'div.half.top',
        h('div.player', [h('div.icon', '✓'), h('div.instruction', h('strong', ctrl.noarg('goodMove')))])
      ),
      jumpToNext(ctrl),
    ];
  },
  view(ctrl: RetroCtrl): VNode[] {
    return [
      h(
        'div.half.top',
        h('div.player', [
          h('div.icon', '✓'),
          h('div.instruction', [
            h('strong', ctrl.noarg('solution')),
            h(
              'em',
              ctrl.trans.vdom(
                'bestWasX',
                h(
                  'strong',
                  renderIndexAndMove(
                    {
                      variant: ctrl.variant,
                      withDots: true,
                      showEval: false,
                      offset: ctrl.offset,
                    },
                    ctrl.current().solution.node
                  )!
                )
              )
            ),
          ]),
        ])
      ),
      jumpToNext(ctrl),
    ];
  },
  eval(ctrl: RetroCtrl): VNode[] {
    return [
      h(
        'div.half.top',
        h('div.player.center', [
          h('div.instruction', [h('strong', ctrl.noarg('evaluatingYourMove')), renderEvalProgress(ctrl.node())]),
        ])
      ),
    ];
  },
  end(ctrl: RetroCtrl, hasFullComputerAnalysis: () => boolean): VNode[] {
    if (!hasFullComputerAnalysis())
      return [
        h(
          'div.half.top',
          h('div.player', [h('div.icon', spinner()), h('div.instruction', ctrl.noarg('waitingForAnalysis'))])
        ),
      ];
    const nothing = !ctrl.completion()[1],
      handicap = isHandicap({ rules: ctrl.variant, sfen: ctrl.initialSfen });
    return [
      h('div.player', [
        h('div.no-square', h('piece.king.' + ctrl.color)),
        h('div.instruction', [
          h(
            'em',
            nothing
              ? transWithColorName(ctrl.trans, 'noMistakesFoundForX', ctrl.color, handicap)
              : transWithColorName(ctrl.trans, 'doneReviewingXMistakes', ctrl.color, handicap)
          ),
          h('div.choices.end', [
            nothing
              ? null
              : h(
                  'a',
                  {
                    hook: bind('click', ctrl.reset),
                  },
                  ctrl.noarg('doItAgain')
                ),
            h(
              'a',
              {
                hook: bind('click', ctrl.flip),
              },
              transWithColorName(ctrl.trans, 'reviewXMistakes', opposite(ctrl.color), handicap)
            ),
          ]),
        ]),
      ]),
    ];
  },
};

function renderFeedback(root: AnalyseCtrl, fb) {
  const ctrl: RetroCtrl = root.retro!;
  const current = ctrl.current();
  if (ctrl.isSolving() && current && root.path !== current.prev.path) return feedback.offTrack(ctrl);
  if (fb === 'find') return current ? feedback.find(ctrl) : feedback.end(ctrl, root.hasFullComputerAnalysis);
  return feedback[fb](ctrl);
}

export default function (root: AnalyseCtrl): VNode | undefined {
  const ctrl = root.retro;
  if (!ctrl) return;
  const fb = ctrl.feedback(),
    completion = ctrl.completion();
  return h('div.retro-box.training-box.sub-box', [
    h('div.title', [
      h('span', ctrl.noarg('learnFromYourMistakes')),
      h('span', Math.min(completion[0] + 1, completion[1]) + ' / ' + completion[1]),
    ]),
    h('div.feedback.' + fb, renderFeedback(root, fb)),
  ]);
}
