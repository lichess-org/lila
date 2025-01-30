import { bind, dataIcon } from 'common/snabbdom';
import spinner from 'common/spinner';
import { i18n, i18nFormatCapitalized, i18nVdom } from 'i18n';
import { colorName } from 'shogi/color-name';
import { isHandicap } from 'shogiops/handicaps';
import { opposite } from 'shogiops/util';
import { type VNode, h } from 'snabbdom';
import type AnalyseCtrl from '../ctrl';
import { renderIndexAndMove } from '../move-view';
import type { RetroCtrl } from './retro-ctrl';

function skipOrViewSolution(ctrl: RetroCtrl) {
  return h('div.choices', [
    h(
      'a',
      {
        hook: bind('click', ctrl.viewSolution, ctrl.redraw),
      },
      i18n('viewTheSolution'),
    ),
    h(
      'a',
      {
        hook: bind('click', ctrl.skip),
      },
      i18n('skipThisMove'),
    ),
  ]);
}

function jumpToNext(ctrl: RetroCtrl) {
  return h(
    'a.half.continue',
    {
      hook: bind('click', ctrl.jumpToNext),
    },
    [h('i', { attrs: dataIcon('G') }), i18n('next')],
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
          node.ceval
            ? `${(100 * Math.max(0, node.ceval.depth - minDepth)) / (maxDepth - minDepth)}%`
            : 0
        }`,
      },
    }),
  );
}

const feedback = {
  find(ctrl: RetroCtrl): VNode[] {
    return [
      h('div.player', [
        h('div.no-square', h(`piece.king.${ctrl.color}`)),
        h('div.instruction', [
          h(
            'strong',
            i18nVdom(
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
                  ctrl.current().fault.node,
                )!,
              ),
            ),
          ),
          h(
            'em',
            i18nFormatCapitalized(
              'findBetterMoveForX',
              colorName(ctrl.color, isHandicap({ rules: ctrl.variant, sfen: ctrl.initialSfen })),
            ),
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
        h('div.icon.off', { attrs: { 'data-icon': '!' } }),
        h('div.instruction', [
          h('strong', i18n('youBrowsedAway')),
          h('div.choices.off', [
            h(
              'a',
              {
                hook: bind('click', ctrl.jumpToNext),
              },
              i18n('resumeLearning'),
            ),
          ]),
        ]),
      ]),
    ];
  },
  fail(ctrl: RetroCtrl): VNode[] {
    return [
      h('div.player', [
        h('div.icon.bad', { attrs: { 'data-icon': 'L' } }),
        h('div.instruction', [
          h('strong', i18n('youCanDoBetter')),
          h(
            'em',
            i18nFormatCapitalized(
              'tryAnotherMoveForX',
              colorName(ctrl.color, isHandicap({ rules: ctrl.variant, sfen: ctrl.initialSfen })),
            ),
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
          h('div.icon', { attrs: { 'data-icon': 'K' } }),
          h('div.instruction', h('strong', i18n('goodMove'))),
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
          h('div.icon', { attrs: { 'data-icon': 'K' } }),
          h('div.instruction', [
            h('strong', i18n('solution')),
            h(
              'em',
              i18nVdom(
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
                    ctrl.current().solution.node,
                  )!,
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
            h('strong', i18n('evaluatingYourMove')),
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
            h('div.icon', spinner()),
            h('div.instruction', i18n('waitingForAnalysis')),
          ]),
        ),
      ];
    const nothing = !ctrl.completion()[1];
    const handicap = isHandicap({ rules: ctrl.variant, sfen: ctrl.initialSfen });
    return [
      h('div.player', [
        h('div.no-square', h(`piece.king.${ctrl.color}`)),
        h('div.instruction', [
          h(
            'em',
            nothing
              ? i18nFormatCapitalized('noMistakesFoundForX', colorName(ctrl.color, handicap))
              : i18nFormatCapitalized('doneReviewingXMistakes', colorName(ctrl.color, handicap)),
          ),
          h('div.choices.end', [
            nothing
              ? null
              : h(
                  'a',
                  {
                    hook: bind('click', ctrl.reset),
                  },
                  i18n('doItAgain'),
                ),
            h(
              'a',
              {
                hook: bind('click', ctrl.flip),
              },
              i18nFormatCapitalized('reviewXMistakes', colorName(opposite(ctrl.color), handicap)),
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
  if (ctrl.isSolving() && current && root.path !== current.prev.path)
    return feedback.offTrack(ctrl);
  if (fb === 'find')
    return current ? feedback.find(ctrl) : feedback.end(ctrl, root.hasFullComputerAnalysis);
  return feedback[fb](ctrl);
}

export default function (root: AnalyseCtrl): VNode | undefined {
  const ctrl = root.retro;
  if (!ctrl) return;
  const fb = ctrl.feedback();
  const completion = ctrl.completion();
  return h('div.retro-box.training-box.sub-box', [
    h('div.title', [
      h('span', i18n('learnFromYourMistakes')),
      h('span', `${Math.min(completion[0] + 1, completion[1])} / ${completion[1]}`),
    ]),
    h(`div.feedback.${fb}`, renderFeedback(root, fb)),
  ]);
}
