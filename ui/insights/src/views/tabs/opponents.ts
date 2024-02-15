import { VNode, h } from 'snabbdom';
import InsightCtrl from '../../ctrl';
import { section, winrateTable } from '../util';
import { OpponentResult } from '../../types';
import { fixed } from '../../util';

export function opponents(ctrl: InsightCtrl, data: OpponentResult): VNode {
  const noarg = ctrl.trans.noarg;
  return h('div.opponents', [
    h('section.padding.half-wrap', [
      h('div.big-number-with-desc', [
        averageOpponentRating(data.avgOpponentRating),
        h('span.desc', noarg('averageOpponentRating')),
      ]),
      h('div.big-number-with-desc', [
        averageOpponentRatingDiff(data.avgOpponentRatingDiff),
        h('span.desc', noarg('averageOpponentRatingDiff')),
      ]),
    ]),
    section(noarg('mostPlayedOpponents'), mostPlayedOpponentsTable(data, noarg)),
  ]);
}

function averageOpponentRating(nb: number): VNode {
  return h('div.big-number.rating', fixed(nb));
}
function averageOpponentRatingDiff(nb: number): VNode {
  return h('div.big-number.diff', (nb > 0 ? '+' : '') + fixed(nb));
}

function mostPlayedOpponentsTable(data: OpponentResult, noarg: TransNoArg): VNode {
  return winrateTable(
    'most-played-opponents',
    [noarg('opponent'), noarg('games'), noarg('winRate')],
    data.winrateByMostPlayedOpponent,
    key =>
      h(
        'a.table-col1.user-link.ulpt',
        {
          attrs: { href: '/@/' + key },
          class: { small: key.length >= 16 },
        },
        key
      )
  );
}
