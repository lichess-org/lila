import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode';
import StormCtrl from '../ctrl';
import { onInsert } from '../util';
import { parseFen, makeFen } from 'chessops/fen';
import { Chess } from 'chessops/chess';
import { parseUci } from 'chessops/util';
import * as miniBoard from "common/mini-board";

const renderEnd = (ctrl: StormCtrl): VNode[] => [
  h('div.storm__summary', 'Game summary'),
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
            h('span.storm__history__round__result',
              h(round.win ? 'good' : 'bad', Math.round(round.millis / 1000) + 's')
            ),
            h('span.storm__history__round__id', '#' + round.puzzle.id)
          ])
        ])
      )
    )
  ])
];

export default renderEnd;
