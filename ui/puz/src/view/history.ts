import { initMiniBoardWith } from 'common/miniBoard';
import type { PuzCtrl } from '../interfaces';
import { Chess } from 'chessops/chess';
import { h, type VNode } from 'snabbdom';
import { parseFen, makeFen } from 'chessops/fen';
import { parseUci } from 'chessops/util';
import { onInsert } from 'common/snabbdom';
import type { Toggle } from 'common';

const slowPuzzleIds = (ctrl: PuzCtrl): Set<string> | undefined => {
  if (!ctrl.filters.slow() || !ctrl.run.history.length) return undefined;
  const mean = ctrl.run.history.reduce((a, r) => a + r.millis, 0) / ctrl.run.history.length;
  const threshold = mean * 1.5;
  return new Set(ctrl.run.history.filter(r => r.millis > threshold).map(r => r.puzzle.id));
};

const toggleButton = (prop: Toggle, title: string): VNode =>
  h(
    'button.puz-history__filter.button',
    {
      class: { active: prop(), 'button-empty': !prop },
      hook: onInsert(e => e.addEventListener('click', prop.toggle)),
    },
    title,
  );

export default (ctrl: PuzCtrl): VNode => {
  const slowIds = slowPuzzleIds(ctrl),
    filters = ctrl.filters,
    buttons: VNode[] = [
      toggleButton(filters.fail, i18n.storm.failedPuzzles),
      toggleButton(filters.slow, i18n.storm.slowPuzzles),
    ];
  if (filters.skip) buttons.push(toggleButton(filters.skip, i18n.storm.skippedPuzzle));
  return h('div.puz-history.box.box-pad', [
    h('div.box__top', [h('h2', i18n.storm.puzzlesPlayed), h('div.box__top__actions', buttons)]),
    h(
      'div.puz-history__rounds',
      ctrl.run.history
        .filter(
          r =>
            (!r.win || !filters.fail()) &&
            (!slowIds || slowIds.has(r.puzzle.id)) &&
            (!filters.skip || !filters.skip() || r.puzzle.id === ctrl.run.skipId),
        )
        .map(round =>
          h('div.puz-history__round', { key: round.puzzle.id }, [
            h('a.puz-history__round__puzzle.mini-board.cg-wrap.is2d', {
              attrs: {
                href: `/training/${round.puzzle.id}`,
                target: '_blank',
              },
              hook: onInsert(e => {
                const pos = Chess.fromSetup(parseFen(round.puzzle.fen).unwrap()).unwrap();
                const uci = round.puzzle.line.split(' ')[0];
                pos.play(parseUci(uci)!);
                initMiniBoardWith(e, makeFen(pos.toSetup()), pos.turn, uci);
              }),
            }),
            h('span.puz-history__round__meta', [
              h('span.puz-history__round__result', [
                h(round.win ? 'good' : 'bad', Math.round(round.millis / 1000) + 's'),
                ctrl.pref.ratings ? h('rating', round.puzzle.rating) : '',
              ]),
              h('span.puz-history__round__id', '#' + round.puzzle.id),
            ]),
          ]),
        ),
    ),
  ]);
};
