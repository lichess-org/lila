import { h, thunk, VNode } from 'snabbdom';
import AnalyseCtrl from '../ctrl';
import { findTag } from '../study/studyChapters';
import * as game from 'game';
import * as licon from 'common/licon';
import { bind, dataIcon } from 'common/snabbdom';

type AdviceKind = 'inaccuracy' | 'mistake' | 'blunder';

interface Advice {
  kind: AdviceKind;
  i18n: I18nKey;
  symbol: string;
}

const renderRatingDiff = (rd: number | undefined): VNode | undefined =>
  rd === 0
    ? h('span', '±0')
    : rd && rd > 0
    ? h('good', '+' + rd)
    : rd && rd < 0
    ? h('bad', '−' + -rd)
    : undefined;

const renderPlayer = (ctrl: AnalyseCtrl, color: Color): VNode => {
  const p = game.getPlayer(ctrl.data, color);
  if (p.user)
    return h(
      'a.user-link.ulpt',
      {
        attrs: { href: '/@/' + p.user.username },
      },
      [p.user.username, ' ', renderRatingDiff(p.ratingDiff)],
    );
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
      h('span', ctrl.trans.noarg('averageCentipawnLoss')),
    ]),
    h('div.advice-summary__accuracy', [
      h('strong', [sideData.accuracy, '%']),
      h('span', [
        ctrl.trans.noarg('accuracy'),
        ' ',
        h('a', {
          attrs: {
            'data-icon': licon.InfoCircle,
            href: '/page/accuracy',
            target: '_blank',
          },
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

const markerColorPrefix = (el: Element): string => {
  const symbol = el.getAttribute('data-symbol');
  const playerColorBit = el.getAttribute('data-color') == 'white' ? '1' : '0';
  // these 5 digit hex values are from the bottom of chart/acpl.ts
  if (symbol == '??') return '#db303' + playerColorBit;
  else if (symbol == '?') return '#cc9b0' + playerColorBit;
  else if (symbol == '?!') return '#1c9ae' + playerColorBit;
  else return '#000000';
};

const doRender = (ctrl: AnalyseCtrl): VNode => {
  const markers = $('g.highcharts-tracker');
  const showMarkers = (el: Element, visible: boolean) => {
    const prefix = markerColorPrefix(el);
    $(`path[stroke^='${prefix}']`, markers)
      .attr('fill', `${prefix}${visible ? 'ff' : '00'}`)
      .attr('stroke', `${prefix}${visible ? 'ff' : '00'}`);
  };

  return h(
    'div.advice-summary',
    {
      hook: {
        insert: vnode => {
          $(vnode.elm as HTMLElement)
            .on('click', 'div.symbol', function (this: HTMLElement) {
              ctrl.jumpToGlyphSymbol(this.dataset.color as Color, this.dataset.symbol!);
            })
            .on('mouseenter', 'div.symbol', function (this: HTMLElement) {
              showMarkers(this, true);
            })
            .on('mouseleave', 'div.symbol', function (this: HTMLElement) {
              showMarkers(this, false);
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
