/// <reference types="../../../../bits/types/tablesort" />
import { Redraw, VNode, looseH as h, onInsert } from 'common/snabbdom';
import * as xhr from 'common/xhr';
import { spinnerVdom as spinner, spinnerVdom } from 'common/spinner';
import { RoundId, TourId } from './interfaces';
import { playerFed } from '../playerBars';
import { userTitle } from 'common/userLink';
import { ChapterId, Federations, FideId, StudyPlayer, StudyPlayerFromServer } from '../interfaces';
import tablesort from 'tablesort';
import extendTablesortNumber from 'common/tablesortNumber';
import { defined } from 'common';
import { Attrs, Hooks, init as initSnabbdom, attributesModule } from 'snabbdom';
import { Outcome } from 'chessops';
import { convertPlayerFromServer } from '../studyChapters';

type RelayPlayerId = FideId | string;

interface RelayPlayer extends StudyPlayer {
  id: RelayPlayerId; // fide ID or full name
  score: number;
  played: number;
  ratingDiff?: number;
}

interface RelayPlayerGame {
  id: ChapterId;
  round: RoundId;
  opponent: RelayPlayer;
  color: Color;
  outcome?: Outcome;
}

interface RelayPlayerWithGames extends RelayPlayer {
  games: RelayPlayerGame[];
}

export default class RelayPlayers {
  loading = false;
  players?: RelayPlayer[];

  constructor(
    private readonly tourId: TourId,
    readonly isEmbed: boolean,
    private readonly federations: () => Federations | undefined,
    private readonly redraw: Redraw,
  ) {}

  loadFromXhr = async (onInsert?: boolean) => {
    if (this.players && !onInsert) {
      this.loading = true;
      this.redraw();
    }
    const players: (RelayPlayer & StudyPlayerFromServer)[] = await xhr.json(
      `/broadcast/${this.tourId}/players`,
    );
    this.players = players.map(p => convertPlayerFromServer(p, this.federations()));
    this.redraw();
  };

  loadPlayerFull = async (id: RelayPlayerId) => {
    const full: RelayPlayerWithGames = await xhr
      .json(`/broadcast/${this.tourId}/players/${encodeURIComponent(id)}`)
      .then(convertPlayerFromServer);
    const feds = this.federations();
    full.games.forEach((g: RelayPlayerGame) => {
      g.opponent = convertPlayerFromServer(g.opponent as RelayPlayer & StudyPlayerFromServer, feds);
    });
    return full;
  };

  playerPowerTipHook = (p: StudyPlayer): Hooks | undefined => {
    const id = p.fideId || p.name;
    return id ? playerPowerTipHook(this, p, id) : undefined;
  };
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
  const withScores = !!players.find(p => p.score);
  const defaultSort = { attrs: { 'data-sort-default': 1 } };
  const sortByBoth = (x?: number, y?: number) => ({
    attrs: { 'data-sort': (x || 0) * 100000 + (y || 0) },
  });
  return h(
    'table.relay-tour__players.slist.slist-invert.slist-pad',
    {
      hook: onInsert(tableAugment),
    },
    [
      h(
        'thead',
        h('tr', [
          h('th', 'Player'),
          withRating ? h('th', defaultSort, 'Elo') : undefined,
          withScores ? h('th', !withRating && defaultSort, 'Score') : h('th', 'Games'),
        ]),
      ),
      h(
        'tbody',
        players.map(player =>
          h('tr', [
            h(
              'th',
              { attrs: { 'data-sort': player.name || '' } },
              h(
                player.fideId ? 'a' : 'span',
                {
                  attrs: playerLinkAttrs(player.fideId, ctrl.isEmbed),
                  hook: ctrl.playerPowerTipHook(player),
                },
                [playerFed(player.fed), userTitle(player), player.name],
              ),
            ),
            withRating
              ? h(
                  'td',
                  sortByBoth(player.rating, player.score * 10),
                  player.rating ? [`${player.rating}`, ratingDiff(player)] : undefined,
                )
              : undefined,
            withScores
              ? h('td', sortByBoth(player.score * 10, player.rating), `${player.score}/${player.played}`)
              : h('td', sortByBoth(player.played, player.rating), `${player.played}`),
          ]),
        ),
      ),
    ],
  );
};

export const playerLinkAttrs = (fideId: FideId | undefined, isEmbed: boolean): Attrs =>
  fideId
    ? {
        href: `/fide/${fideId}/redirect`,
        target: isEmbed ? '_blank' : '',
      }
    : {};

const playerTipId = 'tour-player-tip';

export const playerPowerTipHook = (ctrl: RelayPlayers, p: StudyPlayer, id: RelayPlayerId): Hooks => ({
  insert(vnode) {
    const el = vnode.elm as HTMLElement;
    $(el).powerTip({
      closeDelay: 200,
      popupId: playerTipId,
      preRender() {
        const tipEl = document.getElementById(playerTipId) as HTMLElement;
        const patch = initSnabbdom([attributesModule]);
        patch(tipEl, h(`div#${playerTipId}`, renderPlayerPreload(ctrl, p)));
        ctrl.loadPlayerFull(id).then((p: RelayPlayerWithGames) => {
          const vdom = renderPlayerWithGames(ctrl, p);
          tipEl.innerHTML = '';
          patch(tipEl, h(`div#${playerTipId}`, vdom));
          $.powerTip.reposition(el);
        });
      },
    });
  },
  destroy: vnode => $.powerTip.destroy(vnode.elm as HTMLElement),
});

const isRelayPlayer = (p: StudyPlayer | RelayPlayer): p is RelayPlayer => 'score' in p;

const renderPlayer = (ctrl: RelayPlayers, p: StudyPlayer | RelayPlayer): VNode =>
  h('div.tpp__player', [
    h(`${p.fideId ? 'a' : 'span'}.tpp__player__name`, { attrs: playerLinkAttrs(p.fideId, ctrl.isEmbed) }, [
      userTitle(p),
      p.name,
    ]),
    p.team ? h('div.tpp__player__team', p.team) : undefined,
    h('div.tpp__player__info', [
      h('div', [
        playerFed(p.fed),
        ...(p.rating ? [`${p.rating}`, isRelayPlayer(p) ? ratingDiff(p) : undefined] : []),
      ]),
      isRelayPlayer(p) ? h('div', [p.score, ' / ', p.played]) : undefined,
    ]),
  ]);

const renderPlayerPreload = (ctrl: RelayPlayers, p: StudyPlayer): VNode =>
  h('div.tpp', [renderPlayer(ctrl, p), h('div.tpp__preload', spinnerVdom())]);

const renderPlayerWithGames = (ctrl: RelayPlayers, p: RelayPlayerWithGames): VNode =>
  h('div.tpp', [
    renderPlayer(ctrl, p),
    h(
      'div.tpp__games',
      h(
        'table',
        h(
          'tbody',
          p.games.map((game, i) => {
            const op = game.opponent;
            return h(
              'tr',
              {
                hook: onInsert(el =>
                  el.addEventListener('click', (e: Event) => {
                    let tr = e.target as HTMLLinkElement;
                    while (tr && tr.tagName !== 'TR') tr = tr.parentNode as HTMLLinkElement;
                    const href = tr.querySelector('a')?.href;
                    if (href) location.href = href;
                  }),
                ),
              },
              [
                h('td', `${i + 1}`),
                h(
                  'td',
                  h('a', { attrs: { href: `/broadcast/-/-/${game.round}/${game.id}` } }, [
                    playerFed(op.fed),
                    userTitle(op),
                    op.name,
                  ]),
                ),
                h('td', op.rating?.toString()),
                h('td.is.color-icon.' + game.color),
                h(
                  'td.tpp__games__status',
                  game.outcome
                    ? game.outcome.winner
                      ? game.outcome.winner == game.color
                        ? h('good', '1')
                        : h('bad', '0')
                      : h('span', '½')
                    : '*',
                ),
              ],
            );
          }),
        ),
      ),
    ),
  ]);

const ratingDiff = (p: RelayPlayer) => {
  const rd = p.ratingDiff;
  return !defined(rd)
    ? undefined
    : rd > 0
    ? h('good.rp', '+' + rd)
    : rd < 0
    ? h('bad.rp', '−' + -rd)
    : h('span', ' ==');
};

const tableAugment = (el: HTMLTableElement) => {
  extendTablesortNumber();
  $(el).each(function (this: HTMLElement) {
    tablesort(this, {
      descending: true,
    });
  });
};
