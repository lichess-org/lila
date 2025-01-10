import * as miniBoard from 'common/mini-board';
import { numberSpread } from 'common/number';
import { onInsert } from 'common/snabbdom';
import { i18n, i18nFormat } from 'i18n';
import { getNow } from 'puz/util';
import { toColor } from 'shogiops/util';
import { type VNode, h } from 'snabbdom';
import type StormCtrl from '../ctrl';

const renderEnd = (ctrl: StormCtrl): VNode[] => [...renderSummary(ctrl), renderHistory(ctrl)];

const newHighI18n = {
  day: i18n('storm:newDailyHighscore'),
  week: i18n('storm:newWeeklyHighscore'),
  month: i18n('storm:newMonthlyHighscore'),
  allTime: i18n('storm:newAllTimeHighscore'),
};

const renderSummary = (ctrl: StormCtrl): VNode[] => {
  const run = ctrl.runStats();
  const high = ctrl.vm.response?.newHigh;
  const accuracy = (100 * (run.moves - run.errors)) / run.moves;
  const scoreSteps = Math.min(run.score, 50);
  return [
    ...(high
      ? [
          h(
            'div.storm--end__high.storm--end__high-daily.bar-glider',
            h('div.storm--end__high__content', [
              h('div.storm--end__high__text', [
                h('strong', newHighI18n[high.key]),
                high.prev ? h('span', i18nFormat('storm:previousHighscoreWasX', high.prev)) : null,
              ]),
            ]),
          ),
        ]
      : []),
    h('div.storm--end__score', [
      h(
        'span.storm--end__score__number',
        {
          hook: onInsert(el =>
            numberSpread(el, scoreSteps, Math.round(scoreSteps * 50), 0)(run.score),
          ),
        },
        '0',
      ),
      h('p', i18n('storm:puzzlesSolved')),
    ]),
    h('div.storm--end__stats.box.box-pad', [
      h('table.slist', [
        h('tbody', [
          h('tr', [h('th', i18n('storm:moves')), h('td', h('number', run.moves))]),
          h('tr', [
            h('th', i18n('storm:accuracy')),
            h('td', [h('number', Number(accuracy).toFixed(1)), '%']),
          ]),
          h('tr', [h('th', i18n('storm:combo')), h('td', h('number', ctrl.run.combo.best))]),
          h('tr', [h('th', i18n('storm:time')), h('td', [h('number', Math.round(run.time)), 's'])]),
          h('tr', [
            h('th', i18n('storm:timePerMove')),
            h('td', [h('number', Number(run.time / run.moves).toFixed(2)), 's']),
          ]),
          h('tr', [h('th', i18n('storm:highestSolved')), h('td', h('number', run.highest))]),
        ]),
      ]),
    ]),
    h(
      'a.storm-play-again.button',
      {
        attrs: ctrl.run.endAt! < getNow() - 900 ? { href: '/storm' } : {},
      },
      i18n('storm:playAgain'),
    ),
  ];
};

const renderHistory = (ctrl: StormCtrl): VNode => {
  const slowIds = slowPuzzleIds(ctrl);
  return h('div.storm--end__history.box.box-pad', [
    h('div.box__top', [
      h('h2', i18n('storm:puzzlesPlayed')),
      h('div.box__top__actions', [
        h(
          'button.storm--end__history__filter.button',
          {
            class: {
              active: ctrl.vm.filterFailed,
              'button-empty': !ctrl.vm.filterFailed,
            },
            hook: onInsert(e => e.addEventListener('click', ctrl.toggleFilterFailed)),
          },
          'Failed puzzles',
        ),
        h(
          'button.storm--end__history__filter.button',
          {
            class: {
              active: ctrl.vm.filterSlow,
              'button-empty': !ctrl.vm.filterSlow,
            },
            hook: onInsert(e => e.addEventListener('click', ctrl.toggleFilterSlow)),
          },
          'Slow puzzles',
        ),
      ]),
    ]),
    h(
      'div.storm--end__history__rounds',
      ctrl.run.history
        .filter(r => (!r.win || !ctrl.vm.filterFailed) && (!slowIds || slowIds.has(r.puzzle.id)))
        .map(round =>
          h(
            'div.storm--end__history__round',
            {
              key: round.puzzle.id,
            },
            [
              h('a.storm--end__history__round__puzzle.mini-board', {
                attrs: {
                  href: `/training/${round.puzzle.id}`,
                  target: '_blank',
                },
                hook: onInsert(el => {
                  miniBoard.initOneWithState(el, {
                    variant: 'standard',
                    sfen: round.puzzle.sfen,
                    orientation: toColor(round.puzzle.sfen.split(' ')[1]),
                  });
                }),
              }),
              h('span.storm--end__history__round__meta', [
                h('span.storm--end__history__round__result', [
                  h(round.win ? 'good' : 'bad', Math.round(round.millis / 1000) + 's'),
                  h('rating', round.puzzle.rating),
                ]),
                h('span.storm--end__history__round__id', '#' + round.puzzle.id),
              ]),
            ],
          ),
        ),
    ),
  ]);
};

const slowPuzzleIds = (ctrl: StormCtrl): Set<string> | undefined => {
  if (!ctrl.vm.filterSlow || !ctrl.run.history.length) return undefined;
  const mean = ctrl.run.history.reduce((a, r) => a + r.millis, 0) / ctrl.run.history.length;
  const threshold = mean * 1.5;
  return new Set(ctrl.run.history.filter(r => r.millis > threshold).map(r => r.puzzle.id));
};

export default renderEnd;
