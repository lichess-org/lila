import { Redraw, VNode, looseH as h } from 'common/snabbdom';
import * as xhr from 'common/xhr';
import { spinnerVdom as spinner } from 'common/spinner';
import { RelayPlayer, RoundId } from './interfaces';
import { playerFed } from '../playerBars';
import { userTitle } from 'common/userLink';

interface LeadPlayer extends RelayPlayer {
  score: number;
  played: number;
}

export default class RelayLeaderboard {
  loading = false;
  leaderboard?: LeadPlayer[];

  constructor(
    private readonly roundId: RoundId,
    private readonly redraw: Redraw,
  ) {}

  loadFromXhr = async (onInsert?: boolean) => {
    if (this.leaderboard && !onInsert) {
      this.loading = true;
      this.redraw();
    }
    this.leaderboard = await xhr.json(`/broadcast/${this.roundId}/leaderboard`);
    this.redraw();
  };
}

export const leaderboardView = (ctrl: RelayLeaderboard): VNode =>
  h(
    'div.relay-tour__leaderboard',
    {
      class: { loading: ctrl.loading, nodata: !ctrl.leaderboard },
      hook: {
        insert: () => ctrl.loadFromXhr(true),
      },
    },
    ctrl.leaderboard ? renderPlayers(ctrl.leaderboard) : [spinner()],
  );

const renderPlayers = (players: LeadPlayer[]): VNode => {
  const withRating = !!players.find(p => p.rating);
  return h('table.relay-tour__leaderboard.slist.slist-invert.slist-pad', [
    h(
      'thead',
      h('tr', [h('th'), withRating ? h('th', 'Elo') : undefined, h('th', 'Score'), h('th', 'Games')]),
    ),
    h(
      'tbody',
      players.map(player =>
        h('tr', [
          h(
            'th',
            player.fideId
              ? h('a', { attrs: { href: `/fide/${player.fideId}/redirect` } }, [
                  player.fed && playerFed(player.fed),
                  userTitle(player),
                  player.name,
                ])
              : player.name,
          ),
          h('td', withRating && player.rating ? `${player.rating}` : undefined),
          h('td', `${player.score}`),
          h('td', `${player.played}`),
        ]),
      ),
    ),
  ]);
};
