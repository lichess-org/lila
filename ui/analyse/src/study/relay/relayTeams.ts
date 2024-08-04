import { MaybeVNodes, Redraw, VNode, onInsert, looseH as h } from 'common/snabbdom';
import * as xhr from 'common/xhr';
import { RoundId } from './interfaces';
import { ChapterId, ChapterPreview, ChapterPreviewPlayer, ChapterSelect, StatusStr } from '../interfaces';
import { MultiCloudEval, renderScoreAtDepth } from '../multiCloudEval';
import { spinnerVdom as spinner } from 'common/spinner';
import { playerFed } from '../playerBars';
import { gameLinkAttrs, gameLinksListener, StudyChapters } from '../studyChapters';
import { userTitle } from 'common/userLink';

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
    readonly multiCloudEval: MultiCloudEval,
    readonly chapterSelect: ChapterSelect,
    readonly roundPath: () => string,
    private readonly redraw: Redraw,
  ) {}

  loadFromXhr = async (onInsert?: boolean) => {
    if (this.teams && !onInsert) {
      this.loading = true;
      this.redraw();
    }
    this.teams = await xhr.json(`/broadcast/${this.roundId}/teams`);
    this.redraw();
  };
}

export const teamsView = (ctrl: RelayTeams, chapters: StudyChapters) =>
  h(
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
      ? renderTeams(ctrl.teams, chapters, ctrl.roundPath(), ctrl.multiCloudEval.thisIfShowEval())
      : [spinner()],
  );

const renderTeams = (
  teams: TeamTable,
  chapters: StudyChapters,
  roundPath: string,
  cloudEval?: MultiCloudEval,
): MaybeVNodes =>
  teams.table.map(row => {
    const firstTeam = row.teams[0];
    return h('div.relay-tour__team-match', [
      h('div.relay-tour__team-match__teams', [
        h('strong.relay-tour__team-match__team', row.teams[0].name),
        h('span.relay-tour__team-match__team__points', [
          h('points', firstTeam.points.toString()),
          h('vs', 'vs'),
          h('points', row.teams[1].points.toString()),
        ]),
        h('strong.relay-tour__team', row.teams[1].name),
      ]),
      h(
        'div.relay-tour__team-match__games',
        row.games.map(game => {
          const chap = chapters.get(game.id);
          const players = chap?.players;
          if (!players) return;
          const sortedPlayers =
            game.pov == 'white' ? [players.white, players.black] : [players.black, players.white];
          return (
            chap &&
            h('a.relay-tour__team-match__game', { attrs: gameLinkAttrs(roundPath, chap) }, [
              playerView(sortedPlayers[0]),
              statusView(chap, game.pov, chapters, cloudEval),
              playerView(sortedPlayers[1]),
            ])
          );
        }),
      ),
    ]);
  });

const playerView = (p: ChapterPreviewPlayer) =>
  h('span.relay-tour__team-match__game__player', [
    h('span.mini-game__user', [playerFed(p.fed), h('span.name', [userTitle(p), p.name])]),
    p.rating && h('rating', `${p.rating}`),
  ]);

const statusView = (g: ChapterPreview, pov: Color, chapters: StudyChapters, cloudEval?: MultiCloudEval) => {
  const status = pov == 'white' ? g.status : (g.status?.split('').reverse().join('') as StatusStr);
  return h(
    'span.relay-tour__team-match__game__status',
    status && status != '*' ? status : cloudEval ? evalGauge(g, pov, chapters, cloudEval) : '*',
  );
};

const evalGauge = (
  game: ChapterPreview,
  pov: Color,
  chapters: StudyChapters,
  cloudEval: MultiCloudEval,
): VNode =>
  h(
    `span.eval-gauge-horiz.pov-${pov}`,
    {
      attrs: { 'data-id': game.id },
      hook: onInsert(cloudEval.observe),
    },
    [
      h(`span.eval-gauge-horiz__black`, {
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
                gauge.title = renderScoreAtDepth(cev);
                gauge.classList.add('eval-gauge-horiz--set');
              }
            }
            vnode.data!.cloud = cev;
          },
        },
      }),
      h('tick.zero'),
    ],
  );
