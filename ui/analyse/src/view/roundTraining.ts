import { h, thunk, type VNode } from 'snabbdom';

import { getPlayer } from 'lib/game';
import { licon } from 'lib/licon';
import { bind, dataIcon } from 'lib/view';
import { ratingDiff } from 'lib/view/userLink';

<<<<<<< feature/rolling-analysis-accuracy
import type AnalyseCtrl from '../ctrl';
import type { AnalysisSide, GamePhase } from '../interfaces';
import { findTag } from '../study/studyChapters';
=======
import type AnalyseCtrl from '@/ctrl';
import { findTag } from '@/study/studyChapters';
>>>>>>> master

type AdviceKind = 'inaccuracy' | 'mistake' | 'blunder';

interface Advice {
  kind: AdviceKind;
  i18n: I18nPlural;
  symbol: string;
}

const renderPlayer = ({ data, study }: AnalyseCtrl, color: Color): VNode => {
  const player = getPlayer(data, color);
  if (player.user)
    return h('a.user-link.ulpt', { attrs: { href: '/@/' + player.user.username } }, [
      player.user.username,
      ' ',
      ratingDiff(player),
    ]);
  return h(
    'span',
    player.name ||
      (player.ai && 'Stockfish level ' + player.ai) ||
      (study && findTag(study.data.chapter.tags, color)) ||
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

function playerTable(ctrl: AnalyseCtrl, color: Color): VNode {
  const sideData = ctrl.data.analysis![color];

  return h('div.advice-summary__side', [
    h('div.advice-summary__player', [h(`icon.is.color-icon.${color}`), renderPlayer(ctrl, color)]),
<<<<<<< feature/rolling-analysis-accuracy
    h('div.advice-summary__sections', [
      h('div.advice-summary__acpl', [
        ...advices.map(a => error(d.analysis![color][a.kind], color, a)),
        h('div', [h('strong', sideData.acpl), h('span', ` ${i18n.site.averageCentipawnLoss}`)]),
=======
    ...advices.map(a => error(sideData[a.kind], color, a)),
    h('div.advice-summary__acpl', [
      h('strong', sideData.acpl),
      h('span', ` ${i18n.site.averageCentipawnLoss}`),
    ]),
    h('div.advice-summary__accuracy', [
      h('strong', [sideData.accuracy, '%']),
      h('span', [
        i18n.site.accuracy,
        ' ',
        h('a', {
          attrs: { 'data-icon': licon.InfoCircle, href: '/page/accuracy', target: '_blank' },
        }),
>>>>>>> master
      ]),
      h('div.advice-summary__accuracy', [...renderPhases(sideData)]),
    ]),
  ]);
}

const accuracyClass = (accuracy: number): string =>
  accuracy >= 85 ? '.good' : accuracy >= 70 ? '.inaccuracy' : accuracy >= 50 ? '.mistake' : '.blunder';

const renderPhases = (side: AnalysisSide): VNode[] => {
  return [
    h(`div.advice-summary__phase${accuracyClass(side.accuracy)}`, [
      h('strong', [side.accuracy, '%']),
      h('span', i18n.site.accuracy),
    ]),
    ...phaseOrder
      .filter(phase => side.phases?.[phase] !== undefined)
      .map(phase =>
        h(`div.advice-summary__phase${accuracyClass(side.phases![phase]!)}`, [
          h('strong', `${side.phases![phase]}%`),
          h('span', phaseLabels[phase]),
        ]),
      ),
  ];
};

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
    ],
  );
};

export function puzzleLink(ctrl: AnalyseCtrl): VNode | undefined {
  const puzzle = ctrl.data.puzzle;
  if (!puzzle) return undefined;
  return h(
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
  );
}

export function render(ctrl: AnalyseCtrl): VNode | undefined {
  if (ctrl.study?.practice) return undefined;

  if (
    !ctrl.data.analysis ||
    !ctrl.settings.showStaticAnalysis ||
    (ctrl.study && ctrl.study.vm.toolTab() !== 'serverEval')
  )
    return h('div.analyse__round-training', puzzleLink(ctrl));

  // don't cache until the analysis is complete!
  const buster = ctrl.data.analysis.partial ? Math.random() : '';
  let cacheKey = String(buster) + !!ctrl.retro;
  if (ctrl.study) cacheKey += ctrl.study.data.chapter.id;

  return h('div.analyse__round-training', [
    h('div.analyse__acpl', thunk('div.advice-summary', doRender, [ctrl, cacheKey])),
    puzzleLink(ctrl),
  ]);
}
