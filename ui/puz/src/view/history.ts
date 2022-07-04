import * as miniBoard from 'common/mini-board';
import { PuzCtrl } from '../interfaces';
import { Chess } from 'chessops/chess';
import { h, VNode } from 'snabbdom';
import { parseFen, makeFen } from 'chessops/fen';
import { parseUci } from 'chessops/util';
import { onInsert } from 'common/snabbdom';

const slowPuzzleIds = (ctrl: PuzCtrl): Set<string> | undefined => {
  if (!ctrl.vm.filterSlow || !ctrl.run.history.length) return undefined;
  const mean = ctrl.run.history.reduce((a, r) => a + r.millis, 0) / ctrl.run.history.length;
  const threshold = mean * 1.5;
  return new Set(ctrl.run.history.filter(r => r.millis > threshold).map(r => r.puzzle.id));
};

export default (ctrl: PuzCtrl): VNode => {
  const slowIds = slowPuzzleIds(ctrl);
  const noarg = ctrl.trans.noarg;
  return h('div.puz-history.box.box-pad', [
    h('div.box__top', [
      h('h2', ctrl.trans('puzzlesPlayed')),
      h('div.box__top__actions', [
        h(
          'button.puz-history__filter.button',
          {
            class: {
              active: ctrl.vm.filterFailed,
              'button-empty': !ctrl.vm.filterFailed,
            },
            hook: onInsert(e => e.addEventListener('click', ctrl.toggleFilterFailed)),
          },
          noarg('failedPuzzles')
        ),
        h(
          'button.puz-history__filter.button',
          {
            class: {
              active: ctrl.vm.filterSlow,
              'button-empty': !ctrl.vm.filterSlow,
            },
            hook: onInsert(e => e.addEventListener('click', ctrl.toggleFilterSlow)),
          },
          noarg('slowPuzzles')
        ),
      ]),
    ]),
    h(
      'div.puz-history__rounds',
      ctrl.run.history
        .filter(r => (!r.win || !ctrl.vm.filterFailed) && (!slowIds || slowIds.has(r.puzzle.id)))
        .map(round =>
          h(
            'div.puz-history__round',
            {
              key: round.puzzle.id,
            },
            [
              h('a.puz-history__round__puzzle.mini-board.cg-wrap.is2d', {
                attrs: {
                  href: `/training/${round.puzzle.id}`,
                  target: '_blank',
                  rel: 'noopener',
                },
                hook: onInsert(e => {
                  const pos = Chess.fromSetup(parseFen(round.puzzle.fen).unwrap()).unwrap();
                  const uci = round.puzzle.line.split(' ')[0];
                  pos.play(parseUci(uci)!);
                  miniBoard.initWith(e, makeFen(pos.toSetup()), pos.turn, uci);
                }),
              }),
              h('span.puz-history__round__meta', [
                h('span.puz-history__round__result', [
                  h(round.win ? 'good' : 'bad', Math.round(round.millis / 1000) + 's'),
                  h('rating', round.puzzle.rating),
                ]),
                h('span.puz-history__round__id', '#' + round.puzzle.id),
              ]),
            ]
          )
        )
    ),
  ]);
};
