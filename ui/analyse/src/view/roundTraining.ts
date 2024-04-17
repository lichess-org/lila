import { h, thunk, VNode } from 'snabbdom';
import AnalyseCtrl from '../ctrl';
import { findTag } from '../study/studyChapters';
import * as game from 'game';
import * as licon from 'common/licon';
import { bind, dataIcon } from 'common/snabbdom';
import { ratingDiff } from 'common/userLink';

type AdviceKind = 'inaccuracy' | 'mistake' | 'blunder';

interface Advice {
  kind: AdviceKind;
  i18n: I18nKey;
  symbol: string;
}

const renderPlayer = (ctrl: AnalyseCtrl, color: Color): VNode => {
  const p = game.getPlayer(ctrl.data, color);
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
  { kind: 'inaccuracy', i18n: 'nbInaccuracies', symbol: '?!' },
  { kind: 'mistake', i18n: 'nbMistakes', symbol: '?' },
  { kind: 'blunder', i18n: 'nbBlunders', symbol: '??' },
];

function playerTable(ctrl: AnalyseCtrl, color: Color): VNode {
  const d = ctrl.data,
    sideData = d.analysis![color];

  return h('div.advice-summary__side', [
    h('div.advice-summary__player', [h(`i.is.color-icon.${color}`), renderPlayer(ctrl, color)]),
    ...advices.map(a => error(ctrl, d.analysis![color][a.kind], color, a)),
    h('div.advice-summary__acpl', [
      h('strong', sideData.acpl),
      h('span', ` ${ctrl.trans.noarg('averageCentipawnLoss')}`),
    ]),
    h('div.advice-summary__accuracy', [
      h('strong', [sideData.accuracy, '%']),
      h('span', [
        ctrl.trans.noarg('accuracy'),
        ' ',
        h('a', {
          attrs: { 'data-icon': licon.InfoCircle, href: '/page/accuracy', target: '_blank' },
        }),
      ]),
    ]),
  ]);
}

const error = (ctrl: AnalyseCtrl, nb: number, color: Color, advice: Advice) =>
  h(
    'div.advice-summary__error' + (nb ? `.symbol.${advice.kind}` : ''),
    { attrs: nb ? { 'data-color': color, 'data-symbol': advice.symbol } : {} },
    ctrl.trans.vdomPlural(advice.i18n, nb, h('strong', nb)),
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
            ctrl.trans.noarg('learnFromYourMistakes'),
          ),
      playerTable(ctrl, 'black'),
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
            'data-icon': licon.ArcheryTarget,
            href: `/training/${puzzle.key}/${ctrl.bottomColor()}`,
          },
        },
        ['Recommended puzzle training', h('br'), puzzle.name],
      ),
    )
  );
}

export function render(ctrl: AnalyseCtrl): VNode | undefined {
  if (ctrl.studyPractice) return;

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
