import { Redraw, VNode, looseH as h } from 'common/snabbdom';
import * as xhr from 'common/xhr';
import { spinnerVdom as spinner } from 'common/spinner';
import { RoundId } from './interfaces';
import { playerFed } from '../playerBars';
import { userTitle } from 'common/userLink';
import { Federation, Federations, StudyPlayerFromServer } from '../interfaces';

interface RelayPlayer extends StudyPlayerFromServer {
  score: number;
  played: number;
}

export default class RelayPlayers {
  loading = false;
  players?: RelayPlayer[];

  constructor(
    private readonly roundId: RoundId,
    readonly showScores: boolean,
    private readonly federations: () => Federations | undefined,
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

  expandFederation = (p: RelayPlayer): Federation | undefined =>
    p.fed
      ? {
          id: p.fed,
          name: this.federations()?.[p.fed] || p.fed,
        }
      : undefined;
}

export const playersView = (ctrl: RelayPlayers): VNode =>
  h(
    'div.relay-tour__players',
    {
      class: { loading: ctrl.loading, nodata: !ctrl.players },
      hook: {
        insert: () => ctrl.loadFromXhr(true),
      },
    },
    ctrl.players ? renderPlayers(ctrl, ctrl.players) : [spinner()],
  );

const renderPlayers = (ctrl: RelayPlayers, players: RelayPlayer[]): VNode => {
  const withRating = !!players.find(p => p.rating);
  return h('table.relay-tour__players.slist.slist-invert.slist-pad', [
    h(
      'thead',
      h('tr', [
        h('th'),
        withRating ? h('th', 'Elo') : undefined,
        ctrl.showScores && h('th', 'Score'),
        h('th', 'Games'),
      ]),
    ),
    h(
      'tbody',
      players.map(player =>
        h('tr', [
          h(
            'th',
            player.fideId
              ? h('a', { attrs: { href: `/fide/${player.fideId}/redirect` } }, [
                  playerFed(ctrl.expandFederation(player)),
                  userTitle(player),
                  player.name,
                ])
              : player.name,
          ),
          h('td', withRating && player.rating ? `${player.rating}` : undefined),
          ctrl.showScores && h('td', `${player.score}`),
          h('td', `${player.played}`),
        ]),
      ),
    ),
  ]);
};
