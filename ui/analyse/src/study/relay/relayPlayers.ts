import { type VNode, dataIcon, hl, onInsert, type MaybeVNodes, spinnerVdom as spinner } from 'lib/view';
import { json as xhrJson } from 'lib/xhr';
import * as licon from 'lib/licon';
import type {
  FideTC,
  Photo,
  RelayRound,
  RelayTeamName,
  RelayTour,
  RoundId,
  StatByFideTC,
} from './interfaces';
import { playerColoredResult } from './customScoreStatus';
import { playerFedFlag } from '../playerBars';
import { userLink, userTitle } from 'lib/view/userLink';
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
import { pubsub } from 'lib/pubsub';
import { teamLinkData } from './relayTeamLeaderboard';
import perfIcons from 'lib/game/perfIcons';

export type RelayPlayerId = FideId | string;

interface Tiebreak {
  extendedCode: string;
  description: string;
  points: number;
}

export interface RelayPlayer extends StudyPlayer {
  score?: number;
  played?: number;
  ratingsMap?: StatByFideTC;
  ratingDiffs?: StatByFideTC;
  performances?: StatByFideTC;
  tiebreaks?: Tiebreak[];
  rank?: number;
}

interface RelayPlayerGame {
  id: ChapterId;
  round: RoundId;
  roundObj?: RelayRound;
  opponent: RelayPlayer;
  color: Color;
  fideTC: FideTC;
  points?: PointsStr;
  customPoints?: number;
  ratingDiff?: number;
}

interface RelayPlayerWithGames extends RelayPlayer {
  games: RelayPlayerGame[];
  fide?: FidePlayer;
  user?: LightUser;
}

interface FidePlayer {
  ratings: StatByFideTC;
  year?: number;
  follow?: boolean;
}

interface PlayerToShow {
  id?: RelayPlayerId;
  player?: RelayPlayerWithGames;
}

export const playerId = (p: StudyPlayer) => p.fideId || p.name;

export default class RelayPlayers {
  loading = false;
  players?: RelayPlayer[];
  show?: PlayerToShow;

  constructor(
    readonly tour: RelayTour,
    readonly switchToPlayerTab: () => void,
    readonly isEmbed: boolean,
    private readonly federations: () => Federations | undefined,
    readonly hideResultsSinceRoundId: () => RoundId | undefined,
    readonly fidePhoto: (id: FideId) => Photo | undefined,
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
      `/broadcast/${this.tour.id}/players`,
    );
    this.players = players.map(p => convertPlayerFromServer(p, this.federations()));
    this.redraw();
  };

  loadPlayerWithGames = async (id: RelayPlayerId) => {
    const feds = this.federations();
    const full: RelayPlayerWithGames = await xhrJson(
      `/broadcast/${this.tour.id}/players/${encodeURIComponent(id)}`,
    ).then(p => convertPlayerFromServer(p, feds));
    full.games.forEach((g: RelayPlayerGame) => {
      g.opponent = convertPlayerFromServer(g.opponent as RelayPlayer & StudyPlayerFromServer, feds);
    });
    return full;
  };

  playerLinkConfig = (p: StudyPlayer): VNodeData | undefined => playerLinkConfig(this, p, true);
}

export const playersView = (ctrl: RelayPlayers): VNode =>
  ctrl.show ? playerView(ctrl, ctrl.show) : playersList(ctrl);

const ratingCategs: { [key in FideTC]: string } = {
  standard: i18n.site.classical,
  rapid: i18n.site.rapid,
  blitz: i18n.site.blitz,
};
const playerView = (ctrl: RelayPlayers, show: PlayerToShow): VNode => {
  const tour = ctrl.tour;
  const p = show.player;
  const year = (tour.dates?.[0] ? new Date(tour.dates[0]) : new Date()).getFullYear();
  const tc = tour.info.fideTc || 'standard';
  const age: number | undefined = p?.fide?.year && year - p.fide.year;
  const fidePageAttrs = p ? fidePageLinkAttrs(p, ctrl.isEmbed) : {};
  const photo = p?.fideId ? ctrl.fidePhoto(p.fideId) : undefined;
  return hl(
    'div.fide-player',
    {
      class: { loading: !show.player },
    },
    p
      ? [
          hl(
            'div.fide-player__header',
            {
              hook: onInsert(el => {
                site.asset.loadEsm('fidePlayerFollow');
                pubsub.emit('content-loaded', el);
              }),
            },
            [
              photo && playerPhotoOrFallback(p, photo, 'medium', 'fide-player__photo'),
              hl('div.fide-player__header__info', [
                hl('a.fide-player__header__name', { attrs: fidePageAttrs }, [
                  hl('span', [userTitle(p), p.name]),
                  p.user && userLink({ ...p.user, title: undefined }),
                ]),
                p.fide &&
                  hl('label.fide-player__follow', [
                    hl(`input#fide-follow-${p.fideId}.cmn-favourite`, {
                      attrs: {
                        type: 'checkbox',
                        'data-action': `/fide/${p.fideId}/follow?follow=true`,
                        checked: !!p.fide?.follow,
                      },
                    }),
                    hl('label', { attrs: { for: `fide-follow-${p.fideId}` } }),
                    i18n.site.follow,
                  ]),
                hl('table.fide-player__header__table', [
                  hl('tbody', [
                    p.fed &&
                      hl('tr', [
                        hl('th', i18n.broadcast.federation),
                        hl(
                          'td',
                          hl(
                            'a.fide-player__federation',
                            { attrs: { href: `/fide/federation/${p.fed.name}` } },
                            [playerFedFlag(p.fed), p.fed.name],
                          ),
                        ),
                      ]),
                    p.team &&
                      hl('tr', [
                        hl('th', 'Team'),
                        hl(
                          'td.text',
                          { attrs: dataIcon(licon.Group) },
                          hl('a', matchOrResultsTeamLink(ctrl, p.team), p.team),
                        ),
                      ]),
                    age && hl('tr', [hl('th', i18n.broadcast.age), hl('td', age.toString())]),
                  ]),
                ]),
              ]),
            ],
          ),
          hl('div.fide-player__cards', [
            p.fide?.ratings &&
              Object.entries(ratingCategs).map(([key, name]: [FideTC, string]) =>
                hl(`div.fide-player__card${key === tc ? '.active' : ''}`, [
                  hl('em', fideTCAttrs(key), name),
                  hl('span', [p.fide?.ratings[key] || '-']),
                ]),
              ),
            p.score !== undefined &&
              hl('div.fide-player__card', [
                hl('em', i18n.broadcast.score),
                hl('span', [p.score, ' / ', p.played]),
              ]),
            p.performances &&
              hl('div.fide-player__card', [
                hl('em', i18n.site.performance),
                Object.entries(p.performances).map(([tc, value]: [FideTC, number]) =>
                  hl(
                    'div',
                    fideTCAttrs(tc),
                    `${value}${p.games.filter(g => g.fideTC === tc).length < 4 ? '?' : ''}`,
                  ),
                ),
              ]),
            p.ratingDiffs &&
              hl('div.fide-player__card', [hl('em', i18n.broadcast.ratingDiff), ratingDiff(p)]),
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

export const renderPlayers = (
  ctrl: RelayPlayers,
  players: RelayPlayer[],
  forceEloSort = false,
): MaybeVNodes => {
  const withRating = players.some(p => defined(p.rating));
  const withScores = players.some(p => defined(p.score));
  const withRank = players.some(p => defined(p.rank));
  const defaultSort = { attrs: { 'data-sort-default': 1 } };
  const tbs = players?.[0]?.tiebreaks;
  const sortByBoth = (x?: number, y?: number) => ({
    attrs: { 'data-sort': (x || 0) * 100000 + (y || 0) },
  });
  return [
    withRank
      ? hl(
          'p.relay-tour__standings--disclaimer.text',
          { attrs: dataIcon(licon.InfoCircle) },
          'Standings are calculated using broadcasted games and may differ from official results.',
        )
      : undefined,
    hl(
      'table.relay-tour__players__table.fide-players-table.slist.slist-invert.slist-pad',
      {
        hook: onInsert(tableAugment),
      },
      [
        hl(
          'thead',
          hl('tr', [
            withRank && hl('th.rank', { attrs: { ...defaultSort['attrs'], ...dataIcon(licon.Trophy) } }),
            hl('th.player-name', { attrs: { 'data-sort-reverse': true } }, i18n.site.player),
            withRating && hl('th', ((!withScores && !withRank) || forceEloSort) && defaultSort, 'Elo'),
            withScores && hl('th.score', !withRank && !forceEloSort && defaultSort, i18n.broadcast.score),
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
              withRank &&
                hl('td.rank', { attrs: { 'data-sort': player.rank ? -player.rank : 0 } }, player.rank),
              playerTd(player, ctrl, true),
              withRating &&
                hl(
                  'td',
                  sortByBoth(player.rating, (player.score || 0) * 10),
                  player.rating && ratingDiff(player),
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
  ];
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
                ctrl.loadPlayerWithGames(id).then(p => {
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
    playerPhoto(p, ctrl, 'medium'),
    hl('div.tpp__player__info', [
      hl(`a.tpp__player__name`, playerLinkConfig(ctrl, p, false), [userTitle(p), p.name]),
      hl('div.tpp__player__details', [
        p.team && hl('a.tpp__player__team', matchOrResultsTeamLink(ctrl, p.team), p.team),
        hl('div', [
          playerFedFlag(p.fed),
          !!p.rating && isRelayPlayer(p) && !ctrl.hideResultsSinceRoundId() && ratingDiff(p),
        ]),
        isRelayPlayer(p) &&
          !ctrl.hideResultsSinceRoundId() &&
          p.score !== undefined &&
          hl('div', [i18n.broadcast.score, ' ', hl('strong', p.score)]),
      ]),
    ]),
  ]);

const renderPlayerTipWithGames = (ctrl: RelayPlayers, p: RelayPlayerWithGames): VNode =>
  hl('div.tpp', [
    renderPlayerTipHead(ctrl, p),
    hl('div.tpp__games', hl('table', renderPlayerGames(ctrl, p, false))),
  ]);

const renderPlayerGames = (ctrl: RelayPlayers, p: RelayPlayerWithGames, withTips: boolean): VNode => {
  const hideResultsSinceRoundId = ctrl.hideResultsSinceRoundId();
  const hideResultsSinceIndex =
    (hideResultsSinceRoundId && p.games.findIndex(g => g.round === hideResultsSinceRoundId)) || 999;
  return hl(
    'tbody.fide-players-table',
    p.games.map((game, i) => {
      const op = game.opponent;
      const points = game.points;
      const customPoints = game.customPoints;
      const coloredPoint = (points: PointsStr): VNode | undefined => {
        if (hideResultsSinceIndex <= i) return hl('span', '?');
        const povResultStr =
          points === '1/2' ? '½-½' : (points === '1') === (game.color === 'white') ? '1-0' : '0-1';
        const coloredResult = playerColoredResult(povResultStr, game.color, game.roundObj);
        if (!coloredResult) return;
        const displayValue =
          customPoints !== undefined && points.replace('1/2', '0.5') !== customPoints.toString()
            ? customPoints
            : coloredResult.points;
        return hl(`${coloredResult.tag}`, displayValue);
      };
      return hl('tr', [
        hl(
          'td',
          hl(
            'a.game-link.text',
            { attrs: { ...dataIcon(licon.StudyBoard), href: `/broadcast/-/-/${game.round}/${game.id}` } },
            `${i + 1}`,
          ),
        ),
        playerTd(op, ctrl, withTips),
        hl('td', op.rating?.toString()),
        hl('td.is.color-icon.' + game.color),
        hl('td.tpp__games__status', points !== undefined ? coloredPoint(points) : '*'),
        hl(
          'td',
          defined(game.ratingDiff) &&
            hideResultsSinceIndex > i &&
            ratingDiff(game, p.ratingsMap && Object.keys(p.ratingsMap).length > 1),
        ),
      ]);
    }),
  );
};

const playerPhoto = (player: StudyPlayer, ctrl: RelayPlayers, which: 'small' | 'medium' = 'small'): VNode =>
  playerPhotoOrFallback(
    player,
    player.fideId ? ctrl.fidePhoto(player.fideId) : undefined,
    which,
    'fide-players__photo',
  );

export const playerPhotoOrFallback = (
  player: StudyPlayer,
  photo: Photo | undefined,
  which: 'small' | 'medium',
  cls: string,
): VNode =>
  photo
    ? hl(`img.${cls}`, { attrs: { src: photo[which] } })
    : hl(`img.${cls}.${cls}--fallback`, {
        attrs: { src: site.asset.url(`images/anon-${player.title === 'BOT' ? 'engine' : 'face'}.webp`) },
      });

const playerTd = (player: RelayPlayer, ctrl: RelayPlayers, withTips: boolean): VNode => {
  const linkCfg = playerLinkConfig(ctrl, player, withTips);
  return hl(
    'td.player-intro-td',
    { attrs: { 'data-sort': player.name || '' } },
    hl('span.player-intro', [
      hl('a.player-intro__photo', linkCfg, playerPhoto(player, ctrl)),
      hl('span.player-intro__info', [
        hl('a.player-intro__name', linkCfg, [userTitle(player), player.name]),
        player.fed &&
          hl('span.player-intro__fed', [
            hl('img.mini-game__flag', {
              attrs: { src: site.asset.fideFedSrc(player.fed.id) },
            }),
            player.fed.name,
          ]),
      ]),
    ]),
  );
};

const ratingDiff = (p: RelayPlayer | RelayPlayerGame, showIcons: boolean = true) =>
  isRelayPlayerGame(p)
    ? hl('div', showIcons && fideTCAttrs(p.fideTC), diffNode(p.ratingDiff))
    : (p.ratingDiffs &&
        Object.entries(p.ratingDiffs).map(([tc, diff]: [FideTC, number]) =>
          hl('div', fideTCAttrs(tc), [p.ratingsMap?.[tc], diffNode(diff)]),
        )) ||
      p.rating;

const diffNode = (rd: number | undefined) =>
  !defined(rd)
    ? undefined
    : rd > 0
      ? hl('good.rp', '+' + rd)
      : rd < 0
        ? hl('bad.rp', '−' + -rd)
        : hl('span.rp--same', ' ==');

const isRelayPlayerGame = (p: RelayPlayer | RelayPlayerGame): p is RelayPlayerGame =>
  'round' in p && 'opponent' in p;

const fideTCAttrs = (tc: FideTC): VNodeData => ({
  attrs: {
    'data-icon': perfIcons[tc === 'standard' ? 'classical' : tc],
    title: ratingCategs[tc],
  },
});

export const tableAugment = (el: HTMLTableElement) => {
  extendTablesortNumber();
  $(el).each(function (this: HTMLElement) {
    sortTable(this, {
      descending: true,
    });
  });
};

const matchOrResultsTeamLink = (ctrl: RelayPlayers, teamName: RelayTeamName): VNodeData =>
  ctrl.tour.showTeamScores ? teamLinkData(teamName) : { attrs: { href: '#teams' } };
