import { renderIndexAndMove } from '../view/components';
import type { RetroCtrl } from './retroCtrl';
import type AnalyseCtrl from '../ctrl';
import * as licon from 'lib/licon';
import { bind, dataIcon, hl, type VNode, spinnerVdom as spinner } from 'lib/view';
import type { TreeNode } from 'lib/tree/types';

function skipOrViewSolution(ctrl: RetroCtrl) {
  return hl('div.choices', [
    hl('a', { hook: bind('click', ctrl.viewSolution, ctrl.redraw) }, i18n.site.viewTheSolution),
    hl('a', { hook: bind('click', ctrl.skip) }, i18n.site.skipThisMove),
  ]);
}

function jumpToNext(ctrl: RetroCtrl) {
  return hl('a.half.continue', { hook: bind('click', ctrl.jumpToNext) }, [
    hl('i', { attrs: dataIcon(licon.PlayTriangle) }),
    i18n.site.next,
  ]);
}

const minDepth = 8;
const maxDepth = 18;

function renderEvalProgress(node: TreeNode): VNode {
  return hl(
    'div.progress',
    hl('div', {
      attrs: {
        style: `width: ${
          node.ceval ? (100 * Math.max(0, node.ceval.depth - minDepth)) / (maxDepth - minDepth) + '%' : 0
        }`,
      },
    }),
  );
}

const feedback = {
  find(ctrl: RetroCtrl): VNode[] {
    return [
      hl('div.player', [
        hl('div.no-square', hl('piece.king.' + ctrl.color)),
        hl('div.instruction', [
          hl(
            'strong',
            i18n.site.xWasPlayed.asArray(
              hl('move', renderIndexAndMove(ctrl.current()!.fault.node, false, true)),
            ),
          ),
          hl('em', i18n.site[ctrl.color === 'white' ? 'findBetterMoveForWhite' : 'findBetterMoveForBlack']),
          skipOrViewSolution(ctrl),
        ]),
      ]),
    ];
  },
  // user has browsed away from the move to solve
  offTrack(ctrl: RetroCtrl): VNode[] {
    return [
      hl('div.player', [
        hl('div.icon.off', '!'),
        hl('div.instruction', [
          hl('strong', i18n.site.youBrowsedAway),
          hl('div.choices.off', [
            hl('a', { hook: bind('click', ctrl.jumpToNext) }, i18n.site.resumeLearning),
          ]),
        ]),
      ]),
    ];
  },
  fail(ctrl: RetroCtrl): VNode[] {
    return [
      hl('div.player', [
        hl('div.icon', '✗'),
        hl('div.instruction', [
          hl('strong', i18n.site.youCanDoBetter),
          hl('em', i18n.site[ctrl.color === 'white' ? 'tryAnotherMoveForWhite' : 'tryAnotherMoveForBlack']),
          skipOrViewSolution(ctrl),
        ]),
      ]),
    ];
  },
  win(ctrl: RetroCtrl): VNode[] {
    return [
      hl(
        'div.half.top',
        hl('div.player', [hl('div.icon', '✓'), hl('div.instruction', hl('strong', i18n.study.goodMove))]),
      ),
      jumpToNext(ctrl),
    ];
  },
  view(ctrl: RetroCtrl): VNode[] {
    return [
      hl(
        'div.half.top',
        hl('div.player', [
          hl('div.icon', '✓'),
          hl('div.instruction', [
            hl('strong', i18n.site.solution),
            hl(
              'em',
              i18n.site.bestWasX.asArray(
                hl('strong', renderIndexAndMove(ctrl.current()!.solution.node, false, false)),
              ),
            ),
          ]),
        ]),
      ),
      jumpToNext(ctrl),
    ];
  },
  eval(ctrl: RetroCtrl): VNode[] {
    return [
      hl(
        'div.half.top',
        hl('div.player.center', [
          hl('div.instruction', [
            hl('strong', i18n.site.evaluatingYourMove),
            renderEvalProgress(ctrl.node()),
          ]),
        ]),
      ),
    ];
  },
  end(ctrl: RetroCtrl, hasFullComputerAnalysis: () => boolean): VNode[] {
    if (!hasFullComputerAnalysis())
      return [
        hl(
          'div.half.top',
          hl('div.player', [hl('div.icon', spinner()), hl('div.instruction', i18n.site.waitingForAnalysis)]),
        ),
      ];
    const nothing = !ctrl.completion()[1];
    return [
      hl('div.player', [
        hl('div.no-square', hl('piece.king.' + ctrl.color)),
        hl('div.instruction', [
          hl(
            'em',
            i18n.site[
              nothing
                ? ctrl.color === 'white'
                  ? 'noMistakesFoundForWhite'
                  : 'noMistakesFoundForBlack'
                : ctrl.color === 'white'
                  ? 'doneReviewingWhiteMistakes'
                  : 'doneReviewingBlackMistakes'
            ],
          ),
          hl('div.choices.end', [
            !nothing &&
              hl(
                'a',
                {
                  key: 'reset',
                  hook: bind('click', ctrl.reset),
                },
                i18n.site.doItAgain,
              ),
            hl(
              'a',
              {
                key: 'flip',
                hook: bind('click', ctrl.flip),
              },
              i18n.site[ctrl.color === 'white' ? 'reviewBlackMistakes' : 'reviewWhiteMistakes'],
            ),
          ]),
        ]),
      ]),
    ];
  },
};

function renderFeedback(root: AnalyseCtrl, fb: Exclude<keyof typeof feedback, 'end'>) {
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
  return hl('div.retro-box.training-box.sub-box', [
    hl('div.title', [
      hl('span', i18n.site.learnFromYourMistakes),
      hl('span', `${Math.min(completion[0] + 1, completion[1])} / ${completion[1]}`),
      hl('button.fbt', {
        hook: bind('click', root.toggleRetro, root.redraw),
        attrs: { 'data-icon': licon.X, 'aria-label': 'Close learn window' },
      }),
    ]),
    hl('div.feedback.' + fb, renderFeedback(root, fb)),
  ]);
}
