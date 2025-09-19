import { type VNode, bind, dataIcon, hl, onInsert } from 'lib/snabbdom';
import { json as xhrJson } from 'lib/xhr';
import * as licon from 'lib/licon';
import { spinnerVdom as spinner } from 'lib/view/controls';
import type { RelayTour, RoundId, TourId } from './interfaces';
import { playerFed } from '../playerBars';
import { userTitle } from 'lib/view/userLink';
import type {
  ChapterId,
  Federations,
  FideId,
  PointsStr,
  StudyPlayer,
  StudyPlayerFromServer,
} from '../interfaces';
import { sortTable, extendTablesortNumber } from 'lib/tablesort';
import { defined } from 'lib';
import { type Attrs, type Hooks, init as initSnabbdom, attributesModule, type VNodeData } from 'snabbdom';
import { convertPlayerFromServer } from '../studyChapters';
import { isTouchDevice } from 'lib/device';

export type RelayPlayerId = FideId | string;

interface Tiebreak {
  extendedCode: string;
  description: string;
  points: number;
}

interface RelayPlayer extends StudyPlayer {
  score?: number;
  played?: number;
  ratingDiff?: number;
  performance?: number;
  tiebreaks?: Tiebreak[];
  rank?: number;
}

interface RelayPlayerGame {
  id: ChapterId;
  round: RoundId;
  opponent: RelayPlayer;
  color: Color;
  points?: PointsStr;
  customPoints?: number;
  ratingDiff?: number;
}

interface RelayPlayerWithGames extends RelayPlayer {
  games: RelayPlayerGame[];
  fide?: FidePlayer;
}

interface FidePlayer {
  ratings: {
    [key: string]: number;
  };
  year?: number;
}

interface PlayerToShow {
  id?: RelayPlayerId;
  player?: RelayPlayerWithGames;
}

const playerId = (p: StudyPlayer) => p.fideId || p.name;

export default class RelayPlayers {
  loading = false;
  players?: RelayPlayer[];
  show?: PlayerToShow;

  constructor(
    private readonly tourId: TourId,
    readonly switchToPlayerTab: () => void,
    readonly isEmbed: boolean,
    private readonly federations: () => Federations | undefined,
    private readonly redraw: Redraw,
  ) {
    const locationPlayer = location.hash.startsWith('#players/') && location.hash.slice(9);
    if (locationPlayer) this.showPlayer(locationPlayer);
  }

  tabHash = () => (this.show ? `#players/${this.show.id}` : '#players');

  switchTabAndShowPlayer = async (id: RelayPlayerId) => {
    this.switchToPlayerTab();
    this.showPlayer(id);
    this.redraw();
  };

  showPlayer = async (id: RelayPlayerId) => {
    this.show = { id };
    const player = await this.loadPlayerWithGames(id);
    this.show = { id, player };
    this.redraw();
  };

  closePlayer = () => {
    this.show = undefined;
  };

  loadFromXhr = async (onInsert?: boolean) => {
    if (this.players && !onInsert) {
      this.loading = true;
      this.redraw();
    }
    const players: (RelayPlayer & StudyPlayerFromServer)[] = await xhrJson(
      `/broadcast/${this.tourId}/players`,
    );
    this.players = players.map(p => convertPlayerFromServer(p, this.federations()));
    this.redraw();
  };

  loadPlayerWithGames = async (id: RelayPlayerId) => {
    const feds = this.federations();
    const full: RelayPlayerWithGames = await xhrJson(
      `/broadcast/${this.tourId}/players/${encodeURIComponent(id)}`,
    ).then(p => convertPlayerFromServer(p, feds));
    full.games.forEach((g: RelayPlayerGame) => {
      g.opponent = convertPlayerFromServer(g.opponent as RelayPlayer & StudyPlayerFromServer, feds);
    });
    return full;
  };

  playerLinkConfig = (p: StudyPlayer): VNodeData | undefined => playerLinkConfig(this, p, true);
}

export const playersView = (ctrl: RelayPlayers, tour: RelayTour): VNode =>
  ctrl.show ? playerView(ctrl, ctrl.show, tour) : playersList(ctrl);

const ratingCategs = [
  ['standard', i18n.site.classical],
  ['rapid', i18n.site.rapid],
  ['blitz', i18n.site.blitz],
];

const playerView = (ctrl: RelayPlayers, show: PlayerToShow, tour: RelayTour): VNode => {
  const p = show.player;
  const year = (tour.dates?.[0] ? new Date(tour.dates[0]) : new Date()).getFullYear();
  const tc = tour.info.fideTc || 'standard';
  const age: number | undefined = p?.fide?.year && year - p.fide.year;
  const fidePageAttrs = p ? fidePageLinkAttrs(p, ctrl.isEmbed) : {};
  return hl(
    'div.relay-tour__player',
    {
      class: { loading: !show.player },
    },
    p
      ? [
          hl(
            'a.relay-tour__player__name.text',
            {
              attrs: {
                ...fidePageAttrs,
                ...dataIcon(licon.AccountCircle),
              },
            },
            [userTitle(p), p.name],
          ),
          p.team
            ? hl('div.relay-tour__player__team.text', { attrs: dataIcon(licon.Group) }, p.team)
            : undefined,
          hl('div.relay-tour__player__cards', [
            p.fide?.ratings &&
              ratingCategs.map(([key, name]) =>
                hl(`div.relay-tour__player__card${key === tc ? '.active' : ''}`, [
                  hl('em', name),
                  hl('span', [p.fide?.ratings[key] || '-']),
                ]),
              ),
            !!age &&
              hl('div.relay-tour__player__card', [hl('em', i18n.broadcast.ageThisYear), hl('span', [age])]),
            p.fed &&
              hl('div.relay-tour__player__card', [
                hl('em', i18n.broadcast.federation),
                hl('a.relay-tour__player__fed', { attrs: { href: `/fide/federation/${p.fed.name}` } }, [
                  hl('img.mini-game__flag', {
                    attrs: { src: site.asset.url(`images/fide-fed-webp/${p.fed.id}.webp`) },
                  }),
                  p.fed.name,
                ]),
              ]),
            !!p.fideId &&
              hl('div.relay-tour__player__card', [
                hl('em', 'FIDE ID'),
                hl('a', { attrs: fidePageAttrs }, p.fideId.toString()),
              ]),
            p.score !== undefined &&
              hl('div.relay-tour__player__card', [
                hl('em', i18n.broadcast.score),
                hl('span', [p.score, ' / ', p.played]),
              ]),
            !!p.performance &&
              hl('div.relay-tour__player__card', [
                hl('em', i18n.site.performance),
                hl('span', [p.performance, p.games.length < 4 ? '?' : '']),
              ]),
            p.ratingDiff !== undefined &&
              hl('div.relay-tour__player__card', [hl('em', i18n.broadcast.ratingDiff), ratingDiff(p)]),
          ]),
          hl('table.relay-tour__player__games.slist.slist-pad', [
            hl('thead', hl('tr', hl('td', { attrs: { colspan: 69 } }, i18n.broadcast.gamesThisTournament))),
            renderPlayerGames(ctrl, p, true),
          ]),
        ]
      : [spinner()],
  );
};

const playersList = (ctrl: RelayPlayers): VNode =>
  hl(
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
  const withScores = !!players.find(p => p.score !== undefined);
  const withRank = !!players.find(p => p.rank);
  const defaultSort = { attrs: { 'data-sort-default': 1 } };
  const tbs = players?.[0]?.tiebreaks;
  const sortByBoth = (x?: number, y?: number) => ({
    attrs: { 'data-sort': (x || 0) * 100000 + (y || 0) },
  });
  return hl('div.table', [
    withRank &&
      hl(
        'p.relay-tour__standings--disclaimer',
        { attrs: { 'data-icon': licon.InfoCircle } },
        'Standings are calculated using broadcasted games and may differ from official results.',
      ),
    hl(
      'table.relay-tour__players.slist.slist-invert.slist-pad',
      {
        hook: onInsert(tableAugment),
      },
      [
        hl(
          'thead',
          hl('tr', [
            withRank && hl('th.rank', i18n.site.rank),
            hl('th.player-name', i18n.site.player),
            withRating && hl('th', !withScores && defaultSort, 'Elo'),
            withScores && hl('th.score', defaultSort, i18n.broadcast.score),
            hl('th', i18n.site.games),
            tbs?.map(tb =>
              hl(
                'th.tiebreak',
                { attrs: { 'data-sort': tb.points, title: tb.description, 'aria-label': tb.description } },
                `${tb.extendedCode}`,
              ),
            ),
          ]),
        ),
        hl(
          'tbody',
          players.map(player =>
            hl('tr', [
              withRank && hl('td.rank', { attrs: { 'data-sort': player.rank || 0 } }, player.rank),
              hl(
                'td.player-name',
                { attrs: { 'data-sort': player.name || '' } },
                hl('a', playerLinkConfig(ctrl, player, true), [
                  playerFed(player.fed),
                  userTitle(player),
                  player.name,
                ]),
              ),
              withRating &&
                hl(
                  'td',
                  sortByBoth(player.rating, (player.score || 0) * 10),
                  !!player.rating && [`${player.rating}`, ratingDiff(player)],
                ),
              withScores &&
                hl(
                  'td.score',
                  {
                    attrs: {
                      'data-sort': player.rank
                        ? -player.rank // so that I don't have to insert a data-sort-reverse also
                        : sortByBoth((player.score || 0) * 10, player.rating)['attrs']['data-sort'],
                    },
                  },
                  `${player.score ?? 0}`,
                ),
              hl('td', sortByBoth(player.played, player.rating), `${player.played ?? 0}`),
              player.tiebreaks?.map(tb =>
                hl(
                  'td.tiebreak',
                  {
                    attrs: {
                      'data-sort': tb.points,
                      title: tb.description,
                      'aria-label': tb.description,
                    },
                  },
                  `${tb.points}`,
                ),
              ),
            ]),
          ),
        ),
      ],
    ),
  ]);
};

const playerTipId = 'tour-player-tip';
export const playerLinkHook = (ctrl: RelayPlayers, player: RelayPlayer, withTip: boolean): Hooks => {
  const id = playerId(player);
  withTip = withTip && !isTouchDevice();
  return id
    ? {
        ...onInsert(el => {
          el.addEventListener('click', e => {
            e.preventDefault();
            ctrl.switchTabAndShowPlayer(id);
          });
          if (withTip)
            $(el).powerTip({
              closeDelay: 200,
              popupId: playerTipId,
              preRender() {
                const tipEl = document.getElementById(playerTipId) as HTMLElement;
                const patch = initSnabbdom([attributesModule]);
                tipEl.style.display = 'none';
                ctrl.loadPlayerWithGames(id).then((p: RelayPlayerWithGames) => {
                  const vdom = renderPlayerTipWithGames(ctrl, p);
                  tipEl.innerHTML = '';
                  patch(tipEl, hl(`div#${playerTipId}`, vdom));
                  $.powerTip.reposition(el);
                });
              },
            });
        }),
        ...(withTip ? { destroy: vnode => $.powerTip.destroy(vnode.elm as HTMLElement) } : {}),
      }
    : {};
};

export const playerLinkConfig = (ctrl: RelayPlayers, player: StudyPlayer, withTip: boolean): VNodeData => {
  const id = playerId(player);
  return id
    ? {
        attrs: {
          href: `#players/${playerId(player)}`,
        },
        hook: playerLinkHook(ctrl, player, withTip),
      }
    : {};
};

export const fidePageLinkAttrs = (p: StudyPlayer, blank?: boolean): Attrs | undefined =>
  p.fideId ? { href: `/fide/${p.fideId}/redirect`, ...(blank ? { target: '_blank' } : {}) } : undefined;

const isRelayPlayer = (p: StudyPlayer | RelayPlayer): p is RelayPlayer => 'score' in p;

const renderPlayerTipHead = (ctrl: RelayPlayers, p: StudyPlayer | RelayPlayer): VNode =>
  hl('div.tpp__player', [
    hl(`a.tpp__player__name`, playerLinkConfig(ctrl, p, false), [userTitle(p), p.name]),
    p.team && hl('div.tpp__player__team', p.team),
    hl('div.tpp__player__info', [
      hl('div', [playerFed(p.fed), !!p.rating && [`${p.rating}`, isRelayPlayer(p) && ratingDiff(p)]]),
      isRelayPlayer(p) && p.score !== undefined && hl('div', `${p.score}`),
    ]),
  ]);

const renderPlayerTipWithGames = (ctrl: RelayPlayers, p: RelayPlayerWithGames): VNode =>
  hl('div.tpp', [
    renderPlayerTipHead(ctrl, p),
    hl('div.tpp__games', hl('table', renderPlayerGames(ctrl, p, false))),
  ]);

const renderPlayerGames = (ctrl: RelayPlayers, p: RelayPlayerWithGames, withTips: boolean): VNode =>
  hl(
    'tbody',
    p.games.map((game, i) => {
      const op = game.opponent;
      const points = game.points;
      const customPoints = game.customPoints;
      const formatPointsStr = (points: PointsStr): string => points.replace('1/2', '½');
      const formatPoints = (points: PointsStr, customPoints: number | undefined): string =>
        customPoints === undefined || points.replace('1/2', '0.5') === customPoints.toString()
          ? formatPointsStr(points)
          : `${formatPointsStr(points)} (${customPoints})`;
      const pointsVnode = (points: PointsStr, customPoints: number | undefined): VNode =>
        hl(points === '1' ? 'good' : points === '0' ? 'bad' : 'span', formatPoints(points, customPoints));
      return hl(
        'tr',
        {
          hook: bind('click', e => {
            let tr = e.target as HTMLLinkElement;
            while (tr && tr.tagName !== 'TR') tr = tr.parentNode as HTMLLinkElement;
            const href = tr.querySelector('a')?.href;
            if (href) location.href = href;
          }),
        },
        [
          hl('td', `${i + 1}`),
          hl(
            'td',
            hl(
              'a',
              {
                hook: withTips ? playerLinkHook(ctrl, op, true) : {},
                attrs: { href: `/broadcast/-/-/${game.round}/${game.id}` },
              },
              [playerFed(op.fed), userTitle(op), op.name],
            ),
          ),
          hl('td', op.rating?.toString()),
          hl('td.is.color-icon.' + game.color),
          hl('td.tpp__games__status', points !== undefined ? pointsVnode(points, customPoints) : '*'),
          hl('td', defined(game.ratingDiff) ? ratingDiff(game) : undefined),
        ],
      );
    }),
  );

const ratingDiff = (p: RelayPlayer | RelayPlayerGame) => {
  const rd = p.ratingDiff;
  return !defined(rd)
    ? undefined
    : rd > 0
      ? hl('good.rp', '+' + rd)
      : rd < 0
        ? hl('bad.rp', '−' + -rd)
        : hl('span.rp--same', ' ==');
};

const tableAugment = (el: HTMLTableElement) => {
  extendTablesortNumber();
  $(el).each(function (this: HTMLElement) {
    sortTable(this, {
      descending: true,
    });
  });
};
