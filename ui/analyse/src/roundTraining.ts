import { h, thunk, VNode, VNodeData } from 'snabbdom';

import AnalyseCtrl from './ctrl';
import { findTag } from './study/studyChapters';
import * as game from 'game';
import { defined } from 'common';
import { bind, dataIcon } from 'common/snabbdom';

type AdviceKind = 'inaccuracy' | 'mistake' | 'blunder';

interface Advice {
  kind: AdviceKind;
  i18n: I18nKey;
  symbol: string;
}

const renderRatingDiff = (rd: number | undefined): VNode | undefined =>
  rd === 0 ? h('span', '±0') : rd && rd > 0 ? h('good', '+' + rd) : rd && rd < 0 ? h('bad', '−' + -rd) : undefined;

const renderPlayer = (ctrl: AnalyseCtrl, color: Color): VNode => {
  const p = game.getPlayer(ctrl.data, color);
  if (p.user)
    return h(
      'a.user-link.ulpt',
      {
        attrs: { href: '/@/' + p.user.username },
      },
      [p.user.username, ' ', renderRatingDiff(p.ratingDiff)]
    );
  return h(
    'span',
    p.name ||
      (p.ai && 'Stockfish level ' + p.ai) ||
      (ctrl.study && findTag(ctrl.study.data.chapter.tags, color)) ||
      'Anonymous'
  );
};

const advices: Advice[] = [
  { kind: 'inaccuracy', i18n: 'nbInaccuracies', symbol: '?!' },
  { kind: 'mistake', i18n: 'nbMistakes', symbol: '?' },
  { kind: 'blunder', i18n: 'nbBlunders', symbol: '??' },
];

const winPercentage = (centipawns: number) => 50 + 50 * (2 / (1 + Math.exp(-0.00368208 * centipawns)) - 1);
const moveAccuracy = (evaluationBefore: number, evaluationAfter: number) =>
  103.1668 * Math.exp(-0.04354 * (winPercentage(evaluationBefore) - winPercentage(evaluationAfter))) - 3.1669;
const harmonicMean = (numbers: number[]) => numbers.length / numbers.reduce((left, right) => left + 1 / right, 0);

function playerTable(ctrl: AnalyseCtrl, color: Color): VNode {
  const d = ctrl.data;
  const acpl = d.analysis![color].acpl;
  const positions = ctrl.mainline.filter(node => defined(node.eval)).map(node => node.eval!.cp);
  const moves = positions.slice(0, -1).map((evaluation, index) => [evaluation, positions[index + 1]]);
  const colorParity = color === 'white' ? 0 : 1;
  const colorMultiplier = color === 'white' ? 1 : -1;
  const playerMoves = moves
    .filter((_move, index) => index % 2 === colorParity)
    .filter(([pos1, pos2]) => defined(pos1) && defined(pos2));
  const moveAccuracies = playerMoves
    .map(([pos1, pos2]) => moveAccuracy(pos1! * colorMultiplier, pos2! * colorMultiplier))
    .filter(accuracy => accuracy > 0);
  const accuracy = Math.round(harmonicMean(moveAccuracies.map(accuracy => accuracy / 100)) * 100);

  return h('div.advice-summary__side', [
    h('div.advice-summary__player', [h(`i.is.color-icon.${color}`), renderPlayer(ctrl, color)]),
    ...advices.map(a => {
      const nb: number = d.analysis![color][a.kind];
      const style = nb ? `.symbol.${a.kind}` : '';
      const attrs: VNodeData = nb
        ? {
            'data-color': color,
            'data-symbol': a.symbol,
          }
        : {};
      return h(`div.advice-summary__error${style}`, { attrs }, ctrl.trans.vdomPlural(a.i18n, nb, h('strong', nb)));
    }),
    h('div.advice-summary__acpl', [
      h('strong', '' + (defined(acpl) ? acpl : '?')),
      h('span', ctrl.trans.noarg('averageCentipawnLoss')),
    ]),
    h('div.advice-summary__accuracy', [
      h('strong', '' + (defined(d.analysis) ? accuracy : '?')),
      h('span', ctrl.trans.noarg('accuracy')),
    ]),
  ]);
}

const doRender = (ctrl: AnalyseCtrl): VNode =>
  h(
    'div.advice-summary',
    {
      hook: {
        insert: vnode => {
          $(vnode.elm as HTMLElement).on('click', 'div.symbol', function (this: Element) {
            ctrl.jumpToGlyphSymbol($(this).data('color'), $(this).data('symbol'));
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
              attrs: dataIcon(''),
              hook: bind('click', ctrl.toggleRetro, ctrl.redraw),
            },
            ctrl.trans.noarg('learnFromYourMistakes')
          ),
      playerTable(ctrl, 'black'),
    ]
  );

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
            'data-icon': '',
            href: `/training/${puzzle.key}/${ctrl.bottomColor()}`,
          },
        },
        ['Recommended puzzle training', h('br'), puzzle.name]
      )
    )
  );
}

export function render(ctrl: AnalyseCtrl): VNode | undefined {
  if (ctrl.studyPractice || ctrl.embed) return;

  if (!ctrl.data.analysis || !ctrl.showComputer() || (ctrl.study && ctrl.study.vm.toolTab() !== 'serverEval'))
    return h('div.analyse__round-training', puzzleLink(ctrl));

  // don't cache until the analysis is complete!
  const buster = ctrl.data.analysis.partial ? Math.random() : '';
  let cacheKey = '' + buster + !!ctrl.retro;
  if (ctrl.study) cacheKey += ctrl.study.data.chapter.id;

  return h('div.analyse__round-training', [
    h('div.analyse__acpl', thunk('div.advice-summary', doRender, [ctrl, cacheKey])),
    puzzleLink(ctrl),
  ]);
}
