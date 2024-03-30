import { Redraw, VNode, looseH as h } from 'common/snabbdom';
import * as xhr from 'common/xhr';
import { spinnerVdom as spinner } from 'common/spinner';
import { RoundId } from './interfaces';
import { userTitle } from 'common/userLink';

interface LeadPlayer {
  // TODO, refactoring opportunity.
  // This interface share many fields with RelayPlayer,
  // but has a `fed` field with different type.
  // RelayPlayer has type type `Federation` (with `string` components `id` and `name`),
  // but for LeadPlayer we read json data from /broadcast/<roundId>/leaderboard endpoint,
  // where the `fed` field is `string` (`id`)

  // shared with RelayPlayer
  name: string;
  rating?: number;
  title?: string;
  fideId?: number;

  // conflicting with RelayPlayer
  fed?: string;

  // unique for LeadPlayer
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
                  player.fed && playerFedId(player.fed),
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

const playerFedId = (fedId: string) =>
  h('img.mini-game__flag', {
    attrs: { src: site.asset.url(`images/fide-fed/${fedId}.svg`), title: `Federation: ${fedId}` },
  });
