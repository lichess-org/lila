import * as miniBoard from "common/mini-board";
import StormCtrl from '../ctrl';
import { Chess } from 'chessops/chess';
import { get as dailyBestGet } from '../best';
import { h } from 'snabbdom'
import { numberSpread } from 'common/number';
import { onInsert } from '../util';
import { parseFen, makeFen } from 'chessops/fen';
import { parseUci } from 'chessops/util';
import { VNode } from 'snabbdom/vnode';

const renderEnd = (ctrl: StormCtrl): VNode[] => [
  ...renderSummary(ctrl),
  renderHistory(ctrl)
];

const renderSummary = (ctrl: StormCtrl): VNode[] => {
  const score = ctrl.countWins();
  const best = dailyBestGet();
  const run = ctrl.vm.run;
  const seconds = (run.endAt! - run.startAt) / 1000;
  return [
    ...(score > (best.prev || 0) ? [
      h('div.storm--end__high.storm--end__high-daily.bar-glider',
        h('div.storm--end__high__content', [
          h('div.storm--end__high__text', [
            h('strong', ctrl.trans('newDailyHighscore')),
            best.prev ? h('span', ctrl.trans('previousHighscoreWasX', best.prev)) : null
          ])
        ])
      )] : []),
    h('div.storm--end__score', [
      h('span.storm--end__score__number', {
        hook: onInsert(el => numberSpread(el, score, Math.round(score * 50), 0)(score))
      }, '0'),
      h('p', ctrl.trans('puzzlesSolved'))
    ]),
    h('div.storm--end__stats.box.box-pad', [
      h('table.slist', [
        h('tbody', [
          h('tr', [
            h('th', 'Moves played'),
            h('td', h('number', run.moves))
          ]),
          h('tr', [
            h('th', 'Accuracy'),
            h('td', [h('number', Number(100 * (run.moves - (ctrl.vm.history.length - score)) / run.moves).toFixed(1)), '%'])
          ]),
          h('tr', [
            h('th', 'Best combo'),
            h('td', h('number', ctrl.vm.comboBest))
          ]),
          h('tr', [
            h('th', 'Total time'),
            h('td', [h('number', Math.round(seconds)), 's'])
          ]),
          h('tr', [
            h('th', 'Time per move'),
            h('td', [h('number', Number(seconds / run.moves).toFixed(2)), 's'])
          ]),
          h('tr', [
            h('th', 'Highest solved'),
            h('td', h('number', ctrl.vm.history.reduce((h, r) => r.win && r.puzzle.rating > h ? r.puzzle.rating : h, 0)))
          ])
        ])
      ])
    ]),
    h('a.storm--end__play.button', {
      attrs: { href: '/storm' }
      // hook: onInsert(e => e.addEventListener('click', ctrl.restart))
    }, ctrl.trans('playAgain'))
  ];
}

const renderHistory = (ctrl: StormCtrl): VNode =>
  h('div.storm--end__history.box.box-pad', [
    h('h2', 'Puzzles played'),
    h('div.storm--end__history__rounds',
      ctrl.vm.history.map(round =>
        h('div.storm--end__history__round', [
          h('a.storm--end__history__round__puzzle.mini-board.cg-wrap.is2d', {
            attrs: { href: `/training/${round.puzzle.id}` },
            hook: onInsert(e => {
              const pos = Chess.fromSetup(parseFen(round.puzzle.fen).unwrap()).unwrap();
              const uci = round.puzzle.line.split(' ')[0];
              pos.play(parseUci(uci)!);
              miniBoard.initWith(e, makeFen(pos.toSetup()), pos.turn, uci);
            })
          }),
          h('span.storm--end__history__round__meta', [
            h('span.storm--end__history__round__result', [
              h(round.win ? 'good' : 'bad', Math.round(round.millis / 1000) + 's'),
              h('rating', round.puzzle.rating)
            ]),
            h('span.storm--end__history__round__id', '#' + round.puzzle.id)
          ])
        ])
      )
    )
  ]);

export default renderEnd;
