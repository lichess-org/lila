import { i18n } from 'i18n';
import { type VNode, h } from 'snabbdom';
import type { OpponentResult } from '../../types';
import { fixed } from '../../util';
import { section, winrateTable } from '../util';

export function opponents(data: OpponentResult): VNode {
  return h('div.opponents', [
    h('section.padding.half-wrap', [
      h('div.big-number-with-desc', [
        averageOpponentRating(data.avgOpponentRating),
        h('span.desc', i18n('insights:averageOpponentRating')),
      ]),
      h('div.big-number-with-desc', [
        averageOpponentRatingDiff(data.avgOpponentRatingDiff),
        h('span.desc', i18n('insights:averageOpponentRatingDiff')),
      ]),
    ]),
    section(i18n('insights:mostPlayedOpponents'), mostPlayedOpponentsTable(data)),
  ]);
}

function averageOpponentRating(nb: number): VNode {
  return h('div.big-number.rating', fixed(nb));
}
function averageOpponentRatingDiff(nb: number): VNode {
  return h('div.big-number.diff', (nb > 0 ? '+' : '') + fixed(nb));
}

function mostPlayedOpponentsTable(data: OpponentResult): VNode {
  return winrateTable(
    'most-played-opponents',
    [i18n('opponent'), i18n('games'), i18n('winRate')],
    data.winrateByMostPlayedOpponent,
    key =>
      h(
        'a.table-col1.user-link.ulpt',
        {
          attrs: { href: '/@/' + key },
          class: { small: key.length >= 16 },
        },
        key,
      ),
  );
}
