import type { ChapterId, ChapterPreview, GamePointsStr } from '../interfaces';
import type { RelayRound } from './interfaces';

export interface TeamWithPoints {
  name: string;
  points: number;
}
export interface TeamGame {
  id: ChapterId;
  pov: Color;
}
export interface TeamRow {
  teams: [TeamWithPoints, TeamWithPoints];
  games: TeamGame[];
}
export type TeamTable = {
  table: TeamRow[];
};

type TeamScores = [number, number];
type PointPart = '1' | '0' | '½';

export const currentTeamTable = (
  teams: TeamTable,
  chapters: ChapterPreview[],
  round?: RelayRound,
): TeamTable => {
  const table = seededTeamTable(teams);

  for (const chapter of chapters) {
    if (
      !chapter.status ||
      chapter.status === '*' ||
      !chapter.players?.white.team ||
      !chapter.players.black.team
    )
      continue;

    const whiteTeam = chapter.players.white.team;
    const blackTeam = chapter.players.black.team;
    const key = teamPairKey(whiteTeam, blackTeam);
    let row = table.rows.get(key);

    if (!row) {
      row = {
        teams: [
          { name: whiteTeam, points: 0 },
          { name: blackTeam, points: 0 },
        ],
        games: [],
      };
      table.rows.set(key, row);
      table.keys.push(key);
    }

    const firstTeamColor = row.teams[0].name === whiteTeam ? 'white' : 'black';
    const scores = scoreByTeamOrder(chapter.status, firstTeamColor, round);
    row.teams[0].points += scores[0];
    row.teams[1].points += scores[1];
    row.games.push({ id: chapter.id, pov: firstTeamColor });
  }

  const currentRows = table.keys
    .map(key => {
      const row = table.rows.get(key)!;
      return row.games.length ? row : table.fetchedRows.get(key);
    })
    .filter((row): row is TeamRow => !!row);
  return currentRows.length ? { table: currentRows } : teams;
};

const seededTeamTable = (
  teams: TeamTable,
): { fetchedRows: Map<string, TeamRow>; keys: string[]; rows: Map<string, TeamRow> } => {
  const keys: string[] = [];
  const fetchedRows = new Map<string, TeamRow>();
  const rows = new Map<string, TeamRow>();

  for (const row of teams.table) {
    const key = teamPairKey(row.teams[0].name, row.teams[1].name);
    keys.push(key);
    fetchedRows.set(key, row);
    rows.set(key, {
      teams: [
        { name: row.teams[0].name, points: 0 },
        { name: row.teams[1].name, points: 0 },
      ],
      games: [],
    });
  }

  return { fetchedRows, keys, rows };
};

const teamPairKey = (first: string, second: string) => [first, second].sort().join('\0');

const scoreByTeamOrder = (status: GamePointsStr, firstTeamColor: Color, round?: RelayRound): TeamScores => {
  const firstScore = scoreForColor(status, firstTeamColor, round);
  const secondScore = scoreForColor(status, firstTeamColor === 'white' ? 'black' : 'white', round);
  return [firstScore, secondScore];
};

const scoreForColor = (status: GamePointsStr, color: Color, round?: RelayRound): number => {
  const point = pointPart(status.split('-')[color === 'white' ? 0 : 1]);
  if (point === undefined) return 0;
  const customScoring = round?.customScoring?.[color];
  return customScoring ? (point === 1 ? customScoring.win : point === 0.5 ? customScoring.draw : 0) : point;
};

const pointPart = (part: string): number | undefined => {
  switch (part as PointPart) {
    case '1':
      return 1;
    case '½':
      return 0.5;
    case '0':
      return 0;
    default:
      return undefined;
  }
};
