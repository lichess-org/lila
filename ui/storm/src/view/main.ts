import * as miniBoard from "common/mini-board";
import chessground from './chessground';
import renderClock from './clock';
import StormCtrl from '../ctrl';
import { Chess } from 'chessops/chess';
import { h } from 'snabbdom'
import { onInsert } from '../util';
import { parseFen, makeFen } from 'chessops/fen';
import { parseUci } from 'chessops/util';
import { VNode } from 'snabbdom/vnode';

export default function(ctrl: StormCtrl): VNode {
  return h('main.storm.storm--' + ctrl.vm.mode,
    ctrl.vm.mode == 'play' ? renderPlay(ctrl) : renderEnd(ctrl)
  );
}

const renderPlay = (ctrl: StormCtrl): VNode[] => [
  h('div.storm__board.main-board', [
    chessground(ctrl),
    ctrl.promotion.view()
  ]),
  h('div.storm__side', [
    renderClock(ctrl)
  ])
];

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
