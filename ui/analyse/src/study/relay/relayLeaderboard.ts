import { Redraw, VNode, looseH as h } from 'common/snabbdom';
import * as xhr from 'common/xhr';
import { spinnerVdom as spinner } from 'common/spinner';
import { RoundId } from './interfaces';
import { playerFed } from '../playerBars';
import { userTitle } from 'common/userLink';
import { Federation, Federations } from '../interfaces';

interface LeadPlayer {
  name: string;
  rating?: number;
  title?: string;
  fideId?: number;
  fed?: string;
  score: number;
  played: number;
}

export default class RelayLeaderboard {
  loading = false;
  leaderboard?: LeadPlayer[];

  constructor(
    private readonly roundId: RoundId,
    private readonly federations: () => Federations | undefined,
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

  expandFederation = (p: LeadPlayer): Federation | undefined =>
    p.fed
      ? {
          id: p.fed,
          name: this.federations()?.[p.fed] || p.fed,
        }
      : undefined;
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
    ctrl.leaderboard ? renderPlayers(ctrl.leaderboard, ctrl.expandFederation) : [spinner()],
  );

const renderPlayers = (
  players: LeadPlayer[],
  expandFederation: (p: LeadPlayer) => Federation | undefined,
): VNode => {
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
                  playerFed(expandFederation(player)),
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
