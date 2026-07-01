import { h, thunk, type VNode } from 'snabbdom';

import { getPlayer } from 'lib/game';
import {
  formatPhaseAccuracy,
  phaseAccuraciesDisplay,
  rollingAccuracy,
  type GamePhase,
  type PhaseAccuraciesDisplay,
} from 'lib/analyseAccuracy';
import { licon } from 'lib/licon';
import { bind, dataIcon } from 'lib/view';
import { ratingDiff } from 'lib/view/userLink';

import type AnalyseCtrl from '../ctrl';
import { findTag } from '../study/studyChapters';

type AdviceKind = 'inaccuracy' | 'mistake' | 'blunder';

interface Advice {
  kind: AdviceKind;
  i18n: I18nPlural;
  symbol: string;
}

const renderPlayer = (ctrl: AnalyseCtrl, color: Color): VNode => {
  const p = getPlayer(ctrl.data, color);
  if (p.user)
    return h('a.user-link.ulpt', { attrs: { href: '/@/' + p.user.username } }, [
      p.user.username,
      ' ',
      ratingDiff(p),
    ]);
  return h(
    'span',
    p.name ||
      (p.ai && 'Stockfish level ' + p.ai) ||
      (ctrl.study && findTag(ctrl.study.data.chapter.tags, color)) ||
      'Anonymous',
  );
};

const advices: Advice[] = [
  { kind: 'inaccuracy', i18n: i18n.site.numberInaccuracies, symbol: '?!' },
  { kind: 'mistake', i18n: i18n.site.numberMistakes, symbol: '?' },
  { kind: 'blunder', i18n: i18n.site.numberBlunders, symbol: '??' },
];

const phaseLabels: Record<GamePhase, string> = {
  opening: i18n.site.opening,
  middlegame: i18n.site.middlegame,
  endgame: i18n.site.endgame,
};

const phaseOrder: GamePhase[] = ['opening', 'middlegame', 'endgame'];

const renderPhaseRow = (phase: GamePhase, accuracies: PhaseAccuraciesDisplay): VNode =>
  h('div.advice-summary__phase', [
    h('strong', formatPhaseAccuracy(accuracies[phase].white)),
    h('span', phaseLabels[phase]),
    h('strong', formatPhaseAccuracy(accuracies[phase].black)),
  ]);

const renderPhases = (ctrl: AnalyseCtrl): VNode => {
  const { phases, showHint } = phaseAccuraciesDisplay(
    ctrl.mainline,
    ctrl.node.ply,
    ctrl.data.game,
    ctrl.data.analysis?.partial,
  );

  return h('div.advice-summary__phases', [
    ...phaseOrder.map(phase => renderPhaseRow(phase, phases)),
    showHint ? h('p.advice-summary__phases-hint', i18n.site.phaseAccuracyHint) : null,
  ]);
};

function playerTable(ctrl: AnalyseCtrl, color: Color): VNode {
  const d = ctrl.data,
    sideData = d.analysis![color],
    lastPly = ctrl.mainline[ctrl.mainline.length - 1]?.ply ?? ctrl.node.ply,
    atFinalPly = ctrl.node.ply >= lastPly,
    rolling = rollingAccuracy(ctrl.mainline, ctrl.node.ply, d.game),
    accuracy = rolling?.[color] ?? (atFinalPly ? sideData.accuracy : undefined),
    accuracyLabel = atFinalPly ? i18n.site.accuracy : i18n.site.accuracySoFar;

  return h('div.advice-summary__side', [
    h('div.advice-summary__player', [h(`icon.is.color-icon.${color}`), renderPlayer(ctrl, color)]),
    ...advices.map(a => error(d.analysis![color][a.kind], color, a)),
    h('div.advice-summary__acpl', [
      h('strong', sideData.acpl),
      h('span', ` ${i18n.site.averageCentipawnLoss}`),
    ]),
    accuracy !== undefined
      ? h('div.advice-summary__accuracy', [
          h('strong', [accuracy, '%']),
          h('span', [
            accuracyLabel,
            ' ',
            h('a', {
              attrs: { 'data-icon': licon.InfoCircle, href: '/page/accuracy', target: '_blank' },
            }),
          ]),
        ])
      : null,
  ]);
}

const error = (nb: number, color: Color, advice: Advice) =>
  h(
    'div.advice-summary__error' + (nb ? `.symbol.${advice.kind}` : ''),
    { attrs: nb ? { 'data-color': color, 'data-symbol': advice.symbol } : {} },
    advice.i18n.asArray(nb, h('strong', nb)),
  );

const doRender = (ctrl: AnalyseCtrl): VNode => {
  return h(
    'div.advice-summary',
    {
      hook: {
        insert: vnode => {
          $(vnode.elm as HTMLElement).on('click', 'div.symbol', function (this: HTMLElement) {
            ctrl.jumpToGlyphSymbol(this.dataset.color as Color, this.dataset.symbol!);
          });
        },
      },
    },
    [
      playerTable(ctrl, 'white'),
      ctrl.study
        ? null
        : h(
            'a.button.text',
            {
              class: { active: !!ctrl.retro },
              attrs: dataIcon(licon.PlayTriangle),
              hook: bind('click', ctrl.toggleRetro, ctrl.redraw),
            },
            i18n.site.learnFromYourMistakes,
          ),
      playerTable(ctrl, 'black'),
      renderPhases(ctrl),
    ],
  );
};

export function puzzleLink(ctrl: AnalyseCtrl): VNode | undefined {
  const puzzle = ctrl.data.puzzle;
  return (
    puzzle &&
    h(
      'div.analyse__puzzle',
      h(
        'a.button-link.text',
        {
          attrs: {
            ...dataIcon(licon.ArcheryTarget),
            href: `/training/${puzzle.key}/${ctrl.bottomColor()}`,
          },
        },
        ['Recommended puzzle training', h('br'), puzzle.name],
      ),
    )
  );
}

export function render(ctrl: AnalyseCtrl): VNode | undefined {
  if (ctrl.study?.practice) return;

  if (
    !ctrl.data.analysis ||
    !ctrl.settings.showStaticAnalysis ||
    (ctrl.study && ctrl.study.vm.toolTab() !== 'serverEval')
  )
    return h('div.analyse__round-training', puzzleLink(ctrl));

  // don't cache until the analysis is complete!
  const buster = ctrl.data.analysis.partial ? Math.random() : '';
  let cacheKey = String(buster) + !!ctrl.retro + ctrl.node.ply;
  if (ctrl.study) cacheKey += ctrl.study.data.chapter.id;

  return h('div.analyse__round-training', [
    h('div.analyse__acpl', thunk('div.advice-summary', doRender, [ctrl, cacheKey])),
    puzzleLink(ctrl),
  ]);
}
