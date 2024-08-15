import { Redraw, VNode, looseH as h } from 'common/snabbdom';
import * as xhr from 'common/xhr';
import { spinnerVdom as spinner } from 'common/spinner';
import { RoundId } from './interfaces';
// import { playerFed } from '../playerBars';
import { userTitle } from 'common/userLink';
import { ChapterPreviewPlayer } from '../interfaces';

interface RelayPlayerCard {
  player: ChapterPreviewPlayer;
  ratingDiff?: number;
  games: any[];
}

export default class RelayPlayerCards {
  loading = false;
  players?: RelayPlayerCard[];

  constructor(
    private readonly roundId: RoundId,
    // private readonly federations: () => Federations | undefined,
    private readonly redraw: Redraw,
  ) {}

  loadFromXhr = async (onInsert?: boolean) => {
    if (this.players && !onInsert) {
      this.loading = true;
      this.redraw();
    }
    this.players = await xhr.json(`/broadcast/${this.roundId}/players`);
    this.redraw();
  };

  // expandFederation = (p: ChapterPreviewPlayer): Federation | undefined =>
  //   p.fed
  //     ? {
  //         id: p.fed,
  //         name: this.federations()?.[p.fed as string] || (p.fed as string),
  //       }
  //     : undefined;
}

export const playerCardsView = (ctrl: RelayPlayerCards): VNode =>
  h(
    'div.relay-tour__leaderboard',
    {
      class: { loading: ctrl.loading, nodata: !ctrl.players },
      hook: {
        insert: () => ctrl.loadFromXhr(true),
      },
    },
    ctrl.players ? renderPlayers(ctrl.players) : [spinner()],
  );

const renderPlayers = (
  players: RelayPlayerCard[],
  // expandFederation: (p: LeadPlayer) => Federation | undefined,
): VNode => {
  const withRating = !!players.find(p => p.player.rating);
  return h('table.relay-tour__leaderboard.slist.slist-invert.slist-pad', [
    h(
      'thead',
      h('tr', [h('th'), withRating ? h('th', 'Elo') : undefined, h('th', 'Score'), h('th', 'Games')]),
    ),
    h(
      'tbody',
      players.map(p =>
        h('tr', [
          h(
            'th',
            p.player.fideId
              ? h('a', { attrs: { href: `/fide/${p.player.fideId}/redirect` } }, [
                  // playerFed(expandFederation(p.player)),
                  userTitle(p.player),
                  p.player.name,
                ])
              : p.player.name,
          ),
          h('td', withRating && p.player.rating ? `${p.player.rating}` : undefined),
        ]),
      ),
    ),
  ]);
};
