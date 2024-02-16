import { Redraw, looseH as h } from 'common/snabbdom';
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
  rating: number;
}
interface TeamGame {
  id: ChapterId;
  players: [TeamPlayer, TeamPlayer];
  p0Color: Color;
  outcome: Color | 'draw';
}
interface TeamRow {
  teams: [TeamWithPoints, TeamWithPoints];
  games: TeamGame[];
}
type TeamTable = TeamRow[];

export default class RelayTeams {
  teams?: TeamTable;

  constructor(
    private readonly roundId: RoundId,
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
    'div.relay-tour__teams',
    ctrl.teams.map(row =>
      h(
        'div.relay-tour__team__vs',
        row.teams.map(team =>
          h('div.relay-tour__team', [
            h('strong.relay-tour__team__name', team.name),
            h('span.relay-tour__team__points', `${team.points}`),
          ]),
        ),
      ),
    ),
  );
