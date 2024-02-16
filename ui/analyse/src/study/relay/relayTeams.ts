import { Redraw, bind, looseH as h } from 'common/snabbdom';
import * as xhr from 'common/xhr';
import { RoundId } from './interfaces';
import { ChapterId } from '../interfaces';
import { Color } from 'chessops';

interface TeamWithPoints {
  name: string;
  points: number;
}
interface TeamPlayer {
  name: string;
  title?: string;
  rating?: number;
}
interface TeamGame {
  id: ChapterId;
  players: [TeamPlayer, TeamPlayer];
  p0Color: Color;
  outcome?: Color | 'draw';
}
interface TeamRow {
  teams: [TeamWithPoints, TeamWithPoints];
  games: TeamGame[];
}
type TeamTable = {
  table: TeamRow[];
};

export default class RelayTeams {
  teams?: TeamTable;

  constructor(
    private readonly roundId: RoundId,
    readonly setChapter: (id: ChapterId) => void,
    readonly roundPath: () => string,
    private readonly redraw: Redraw,
  ) {
    this.reload();
  }

  reload = async () => {
    this.teams = await xhr.json(`/broadcast/${this.roundId}/teams`);
    this.redraw();
  };
}

export const teamsView = (ctrl: RelayTeams) =>
  ctrl.teams &&
  h(
    'div.relay-tour__team-table',
    ctrl.teams.table.map(row =>
      h('div.relay-tour__team-match', [
        h('div.relay-tour__team-match__teams', [
          h('strong.relay-tour__team-match__team', row.teams[0].name),
          h('span.relay-tour__team-match__team__points', [
            h('points', row.teams[0].points.toString()),
            h('vs', 'vs'),
            h('points', row.teams[1].points.toString()),
          ]),
          h('strong.relay-tour__team', row.teams[1].name),
        ]),
        h(
          'div.relay-tour__team-match__games',
          row.games.map(game =>
            h(
              'a.relay-tour__team-match__game',
              {
                attrs: { href: `${ctrl.roundPath()}/${game.id}` },
                hook: bind(
                  'click',
                  () => {
                    ctrl.setChapter(game.id);
                    return false;
                  },
                  undefined,
                  false,
                ),
              },
              [playerView(game.players[0]), statusView(game), playerView(game.players[1])],
            ),
          ),
        ),
      ]),
    ),
  );

const statusView = (g: TeamGame) =>
  h(
    'span.relay-tour__team-match__game__status',
    !g.outcome ? '*' : g.outcome === 'draw' ? '½-½' : g.outcome === g.p0Color ? '1-0' : '0-1',
  );

const playerView = (p: TeamPlayer) =>
  h('span.relay-tour__team-match__game__player', [
    `${p.title ? p.title + ' ' : ''}${p.name}`,
    p.rating && h('rating', `${p.rating}`),
  ]);
