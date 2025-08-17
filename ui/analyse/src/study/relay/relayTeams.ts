import { type MaybeVNodes, type VNode, onInsert, hl } from 'lib/snabbdom';
import { json as xhrJson } from 'lib/xhr';
import type { RoundId } from './interfaces';
import type { ChapterId, ChapterPreview, StudyPlayer, ChapterSelect, StatusStr } from '../interfaces';
import { type MultiCloudEval, renderScore } from '../multiCloudEval';
import { spinnerVdom as spinner } from 'lib/view/controls';
import { playerFed } from '../playerBars';
import { gameLinkAttrs, gameLinksListener, StudyChapters } from '../studyChapters';
import { userTitle } from 'lib/view/userLink';
import type RelayPlayers from './relayPlayers';

interface TeamWithPoints {
  name: string;
  points: number;
}
interface TeamGame {
  id: ChapterId;
  pov: Color;
}
interface TeamRow {
  teams: [TeamWithPoints, TeamWithPoints];
  games: TeamGame[];
}
type TeamTable = {
  table: TeamRow[];
};

export default class RelayTeams {
  loading = false;
  teams?: TeamTable;

  constructor(
    private readonly roundId: RoundId,
    readonly multiCloudEval: MultiCloudEval | undefined,
    readonly chapterSelect: ChapterSelect,
    readonly roundPath: () => string,
    private readonly redraw: Redraw,
  ) {}

  loadFromXhr = async (onInsert?: boolean) => {
    if (this.teams && !onInsert) {
      this.loading = true;
      this.redraw();
    }
    this.teams = await xhrJson(`/broadcast/${this.roundId}/teams`);
    this.redraw();
  };
}

export const teamsView = (ctrl: RelayTeams, chapters: StudyChapters, players: RelayPlayers) =>
  hl(
    'div.relay-tour__team-table',
    {
      class: { loading: ctrl.loading, nodata: !ctrl.teams },
      hook: {
        insert: vnode => {
          gameLinksListener(ctrl.chapterSelect)(vnode);
          ctrl.loadFromXhr(true);
        },
      },
    },
    ctrl.teams
      ? renderTeams(ctrl.teams, chapters, ctrl.roundPath(), players, ctrl.multiCloudEval?.thisIfShowEval())
      : [spinner()],
  );

const renderTeams = (
  teams: TeamTable,
  chapters: StudyChapters,
  roundPath: string,
  playersCtrl: RelayPlayers,
  cloudEval?: MultiCloudEval,
): MaybeVNodes =>
  teams.table.map(row => {
    const firstTeam = row.teams[0];
    return hl('div.relay-tour__team-match', [
      hl('div.relay-tour__team-match__teams', [
        hl('strong.relay-tour__team-match__team', row.teams[0].name),
        hl('span.relay-tour__team-match__team__points', [
          hl('points', firstTeam.points.toString()),
          hl('vs', 'vs'),
          hl('points', row.teams[1].points.toString()),
        ]),
        hl('strong.relay-tour__team', row.teams[1].name),
      ]),
      hl(
        'div.relay-tour__team-match__games',
        row.games.map(game => {
          const chap = chapters.get(game.id);
          const players = chap?.players;
          if (!players) return;
          const sortedPlayers =
            game.pov === 'white' ? [players.white, players.black] : [players.black, players.white];
          return (
            chap &&
            hl('a.relay-tour__team-match__game', { attrs: gameLinkAttrs(roundPath, chap) }, [
              playerView(playersCtrl, sortedPlayers[0]),
              statusView(chap, game.pov, chapters, cloudEval),
              playerView(playersCtrl, sortedPlayers[1]),
            ])
          );
        }),
      ),
    ]);
  });

const playerView = (players: RelayPlayers, p: StudyPlayer) =>
  hl('span.relay-tour__team-match__game__player', [
    hl('span.mini-game__user', players.playerLinkConfig(p), [
      playerFed(p.fed),
      hl('span.name', [userTitle(p), p.name]),
    ]),
    !!p.rating && hl('rating', `${p.rating}`),
  ]);

const statusView = (g: ChapterPreview, pov: Color, chapters: StudyChapters, cloudEval?: MultiCloudEval) => {
  const status = pov === 'white' ? g.status : (g.status?.split('').reverse().join('') as StatusStr);
  return hl(
    'span.relay-tour__team-match__game__status',
    status && status !== '*' ? status : cloudEval ? evalGauge(g, pov, chapters, cloudEval) : '*',
  );
};

const evalGauge = (
  game: ChapterPreview,
  pov: Color,
  chapters: StudyChapters,
  cloudEval: MultiCloudEval,
): VNode =>
  hl(
    `span.eval-gauge-horiz.pov-${pov}`,
    {
      attrs: { 'data-id': game.id },
      hook: onInsert(cloudEval.observe),
    },
    [
      hl(`span.eval-gauge-horiz__black`, {
        hook: {
          postpatch(old, vnode) {
            const prevNodeCloud = old.data?.cloud;
            const fen = chapters.get(game.id)?.fen;
            const cev = (fen && cloudEval.getCloudEval(fen)) || prevNodeCloud;
            if (cev?.chances != prevNodeCloud?.chances) {
              const elm = vnode.elm as HTMLElement;
              const gauge = elm.parentNode as HTMLElement;
              elm.style.width = `${((1 - (cev?.chances || 0)) / 2) * 100}%`;
              if (cev) {
                gauge.title = renderScore(cev);
                gauge.classList.add('eval-gauge-horiz--set');
              }
            }
            vnode.data!.cloud = cev;
          },
        },
      }),
      hl('tick.zero'),
    ],
  );
