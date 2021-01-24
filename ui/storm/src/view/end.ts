import * as miniBoard from "common/mini-board";
import StormCtrl from '../ctrl';
import { Chess } from 'chessops/chess';
import { h } from 'snabbdom'
import { onInsert } from '../util';
import { parseFen, makeFen } from 'chessops/fen';
import { parseUci } from 'chessops/util';
import { VNode } from 'snabbdom/vnode';
import { numberSpread } from 'common/number';

const renderEnd = (ctrl: StormCtrl): VNode[] => [
  renderSummary(ctrl),
  renderHistory(ctrl)
];

const renderSummary = (ctrl: StormCtrl): VNode => {
  const wins = ctrl.countWins();
  console.log(ctrl.trans.vdomPlural('xPuzzlesSolved', wins, h('strong', wins)));
  return h('div.storm__summary', [
    h('div.storm__summary__solved', [
      h('strong.storm__summary__solved__number', {
        hook: onInsert(el => numberSpread(el, wins, Math.round(wins * 50), 0)(wins))
      }, '0'),
      h('p', ctrl.trans('puzzlesSolved'))
    ]),
    h('div', [
      h('p', ['Failed: ', ctrl.vm.history.length - wins]),
      h('p', ['Best combo: ', ctrl.vm.comboBest])
    ])
  ]);
}

const renderHistory = (ctrl: StormCtrl): VNode =>
  h('div.storm__history', [
    h('h2', 'Puzzles played'),
    h('div.storm__history__rounds',
      ctrl.vm.history.map(round =>
        h('div.storm__history__round', [
          h('a.storm__history__round__puzzle.mini-board.cg-wrap.is2d', {
            attrs: { href: `/training/${round.puzzle.id}` },
            hook: onInsert(e => {
              const pos = Chess.fromSetup(parseFen(round.puzzle.fen).unwrap()).unwrap();
              const uci = round.puzzle.line.split(' ')[0];
              pos.play(parseUci(uci)!);
              miniBoard.initWith(e, makeFen(pos.toSetup()), pos.turn, uci);
            })
          }),
          h('span.storm__history__round__meta', [
            h('span.storm__history__round__result', [
              h(round.win ? 'good' : 'bad', Math.round(round.millis / 1000) + 's'),
              h('rating', round.puzzle.rating)
            ]),
            h('span.storm__history__round__id', '#' + round.puzzle.id)
          ])
        ])
      )
    )
  ]);

export default renderEnd;
