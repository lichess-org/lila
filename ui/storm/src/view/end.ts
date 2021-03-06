import * as miniBoard from 'common/mini-board';
import StormCtrl from '../ctrl';
import { Chess } from 'chessops/chess';
import { getNow, onInsert } from 'puz/util';
import { h } from 'snabbdom';
import { numberSpread } from 'common/number';
import { parseFen, makeFen } from 'chessops/fen';
import { parseUci } from 'chessops/util';
import { VNode } from 'snabbdom/vnode';

const renderEnd = (ctrl: StormCtrl): VNode[] => [...renderSummary(ctrl), renderHistory(ctrl)];

const newHighI18n = {
  day: 'newDailyHighscore',
  week: 'newWeeklyHighscore',
  month: 'newMonthlyHighscore',
  allTime: 'newAllTimeHighscore',
};

const renderSummary = (ctrl: StormCtrl): VNode[] => {
  const run = ctrl.runStats();
  const high = ctrl.vm.response?.newHigh;
  const accuracy = (100 * (run.moves - run.errors)) / run.moves;
  const noarg = ctrl.trans.noarg;
  const scoreSteps = Math.min(run.score, 50);
  return [
    ...(high
      ? [
          h(
            'div.storm--end__high.storm--end__high-daily.bar-glider',
            h('div.storm--end__high__content', [
              h('div.storm--end__high__text', [
                h('strong', noarg(newHighI18n[high.key])),
                high.prev ? h('span', ctrl.trans('previousHighscoreWasX', high.prev)) : null,
              ]),
            ])
          ),
        ]
      : []),
    h('div.storm--end__score', [
      h(
        'span.storm--end__score__number',
        {
          hook: onInsert(el => numberSpread(el, scoreSteps, Math.round(scoreSteps * 50), 0)(run.score)),
        },
        '0'
      ),
      h('p', noarg('puzzlesSolved')),
    ]),
    h('div.storm--end__stats.box.box-pad', [
      h('table.slist', [
        h('tbody', [
          h('tr', [h('th', noarg('moves')), h('td', h('number', run.moves))]),
          h('tr', [h('th', noarg('accuracy')), h('td', [h('number', Number(accuracy).toFixed(1)), '%'])]),
          h('tr', [h('th', noarg('combo')), h('td', h('number', ctrl.run.combo.best))]),
          h('tr', [h('th', noarg('time')), h('td', [h('number', Math.round(run.time)), 's'])]),
          h('tr', [
            h('th', noarg('timePerMove')),
            h('td', [h('number', Number(run.time / run.moves).toFixed(2)), 's']),
          ]),
          h('tr', [h('th', noarg('highestSolved')), h('td', h('number', run.highest))]),
        ]),
      ]),
    ]),
    h(
      'a.storm-play-again.button',
      {
        attrs: ctrl.run.endAt! < getNow() - 900 ? { href: '/storm' } : {},
      },
      noarg('playAgain')
    ),
  ];
};

const renderHistory = (ctrl: StormCtrl): VNode => {
  const slowIds = slowPuzzleIds(ctrl);
  return h('div.storm--end__history.box.box-pad', [
    h('div.box__top', [
      h('h2', ctrl.trans('puzzlesPlayed')),
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
          'Failed puzzles'
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
          'Slow puzzles'
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
              h('a.storm--end__history__round__puzzle.mini-board.cg-wrap.is2d', {
                attrs: {
                  href: `/training/${round.puzzle.id}`,
                  target: '_blank',
                },
                hook: onInsert(e => {
                  const pos = Chess.fromSetup(parseFen(round.puzzle.fen).unwrap()).unwrap();
                  const uci = round.puzzle.line.split(' ')[0];
                  pos.play(parseUci(uci)!);
                  miniBoard.initWith(e, makeFen(pos.toSetup()), pos.turn, uci);
                }),
              }),
              h('span.storm--end__history__round__meta', [
                h('span.storm--end__history__round__result', [
                  h(round.win ? 'good' : 'bad', Math.round(round.millis / 1000) + 's'),
                  h('rating', round.puzzle.rating),
                ]),
                h('span.storm--end__history__round__id', '#' + round.puzzle.id),
              ]),
            ]
          )
        )
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
