import { renderIndexAndMove } from '../view/moveView';
import { onInsertHandler } from '../plugins/analyse.nvui';
import type { RetroCtrl } from './retroCtrl';
import type AnalyseCtrl from '../ctrl';
import * as licon from 'lib/licon';
import { dataIcon, onInsert } from 'lib/snabbdom';
import { spinnerVdom as spinner } from 'lib/view/controls';
import { h, type VNode } from 'snabbdom';

function skipOrViewSolution(ctrl: RetroCtrl) {
  return h('div.choices', [
    h(
      'button',
      {
        hook: onInsert((el: HTMLButtonElement) => {
          const viewSolution = () => {
            ctrl.viewSolution();
            ctrl.redraw();
          };
          onInsertHandler(viewSolution, el);
        }),
        attrs: { tabindex: '0' },
      },
      i18n.site.viewTheSolution,
    ),
    h(
      'button',
      {
        hook: onInsert((el: HTMLButtonElement) => {
          const skipThisMove = () => {
            ctrl.skip(), ctrl.redraw();
          };
          onInsertHandler(skipThisMove, el);
        }),
        attrs: { tabindex: '0' },
      },
      i18n.site.skipThisMove,
    ),
  ]);
}

function jumpToNext(ctrl: RetroCtrl) {
  return h(
    'button.half.continue',
    {
      hook: onInsert((el: HTMLButtonElement) => {
        const jumpToNext = () => {
          ctrl.jumpToNext(), ctrl.redraw();
        };
        onInsertHandler(jumpToNext, el);
      }),
      attrs: { 'aria-label': 'Jump to next', tabindex: '0' },
    },
    [h('i', { attrs: dataIcon(licon.PlayTriangle) }), i18n.site.next],
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
    }),
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
            i18n.site.xWasPlayed.asArray(
              h(
                'move',
                { attrs: { tabindex: '0', 'aria-live': 'assertive' } },
                renderIndexAndMove(
                  { withDots: true, showGlyphs: true, showEval: false },
                  ctrl.current()!.fault.node,
                ),
              ),
            ),
          ),
          h(
            'em',
            { attrs: { 'aria-live': 'polite' } },
            i18n.site[ctrl.color === 'white' ? 'findBetterMoveForWhite' : 'findBetterMoveForBlack'],
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
        h('div.icon.off', { attrs: { 'aria-label': i18n.site.resumeLearning } }, '!'),

        h('div.instruction', [
          h('strong', { 'aria-live': 'assertive' }, i18n.site.youBrowsedAway),
          h('div.choices.off', [
            h(
              'button',
              {
                tabindex: '0',
                hook: onInsert((el: HTMLButtonElement) => {
                  const jumpToNext = () => {
                    ctrl.jumpToNext();
                  };
                  onInsertHandler(jumpToNext, el);
                }),
              },
              i18n.site.resumeLearning,
            ),
          ]),
        ]),
      ]),
    ];
  },
  fail(ctrl: RetroCtrl): VNode[] {
    return [
      h('div.player', [
        h('div.icon', { attrs: { 'aria-label': i18n.site.youCanDoBetter } }, '✗'),
        h('div.instruction', [
          h('strong', { attrs: { 'aria-live': 'assertive' } }, i18n.site.youCanDoBetter),
          h(
            'em',
            { attrs: { 'aria-live': 'assertive' } },
            i18n.site[ctrl.color === 'white' ? 'tryAnotherMoveForWhite' : 'tryAnotherMoveForBlack'],
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
        h('div.player', [
          h('div.icon', { attrs: { 'aria-label': i18n.study.goodMove } }, '✓'),
          h('div.instruction', h('strong', { attrs: { 'aria-live': 'assertive' } }, i18n.study.goodMove)),
        ]),
      ),
      jumpToNext(ctrl),
    ];
  },
  view(ctrl: RetroCtrl): VNode[] {
    return [
      h(
        'div.half.top',
        h('div.player', [
          h('div.icon', { attrs: { 'aria-label': i18n.site.solution } }, '✓'),
          h('div.instruction', { attrs: { 'tab-index': '0' } }, [
            h('strong', { attrs: { 'aria-live': 'assertive' } }, i18n.site.solution),
            h(
              'em',
              i18n.site.bestWasX.asArray(
                h(
                  'strong',
                  { attrs: { 'aria-live': 'assertive' } },
                  renderIndexAndMove({ withDots: true, showEval: false }, ctrl.current()!.solution.node),
                ),
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
      h(
        'div.half.top',
        h('div.player.center', [
          h('div.instruction', [
            h('strong', { attrs: { 'aria-live': 'assertive' } }, i18n.site.evaluatingYourMove),
            renderEvalProgress(ctrl.node()),
          ]),
        ]),
      ),
    ];
  },
  end(ctrl: RetroCtrl, hasFullComputerAnalysis: () => boolean): VNode[] {
    if (!hasFullComputerAnalysis())
      return [
        h(
          'div.half.top',
          h('div.player', [
            h('div.icon', { attrs: { 'aria-label': i18n.site.waitingForAnalysis } }, spinner()),
            h('div.instruction', { attrs: { 'aria-live': 'polite' } }, i18n.site.waitingForAnalysis),
          ]),
        ),
      ];
    const nothing = !ctrl.completion()[1];
    return [
      h('div.player', [
        h('div.no-square', h('piece.king.' + ctrl.color)),
        h('div.instruction', [
          h(
            'em',
            { attrs: { 'aria-live': 'polite' } },
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
          h('div.choices.end', [
            nothing
              ? null
              : h(
                  'button',
                  {
                    attrs: {
                      'tab-index': '0',
                    },
                    key: 'reset',
                    hook: onInsert((el: HTMLButtonElement) => {
                      const doItAgain = () => {
                        ctrl.reset();
                      };
                      onInsertHandler(doItAgain, el);
                    }),
                  },
                  i18n.site.doItAgain,
                ),
            h(
              'button',
              {
                attrs: {
                  'tab-index': '0',
                },
                key: 'flip',
                hook: onInsert((el: HTMLButtonElement) => {
                  const flipBoard = () => {
                    ctrl.flip();
                  };
                  onInsertHandler(flipBoard, el);
                }),
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

  return h('div.retro-box.training-box.sub-box', [
    h('div.title', [
      h('h3', { attrs: { 'aria-live': 'assertive' } }, i18n.site.learnFromYourMistakes),
      h(
        'p',
        { attrs: { 'aria-label': 'mistake number' } },
        `${Math.min(completion[0] + 1, completion[1])} / ${completion[1]}`,
      ),
      h('button.fbt', {
        hook: onInsert((el: HTMLButtonElement) => {
          const toggleLFYM = () => {
            root.toggleRetro();
            root.redraw();
          };
          onInsertHandler(toggleLFYM, el);
        }),
        attrs: { 'data-icon': licon.X, 'aria-label': 'toggle learn from your mistakes' },
      }),
    ]),
    h('div.feedback.' + fb, { attrs: { 'aria-live': 'assertive' } }, renderFeedback(root, fb)),
  ]);
}
