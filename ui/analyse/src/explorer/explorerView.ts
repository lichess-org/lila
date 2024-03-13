import { VNode } from 'snabbdom';
import * as licon from 'common/licon';
import { numberFormat } from 'common/number';
import { perf } from 'game/perf';
import { bind, dataIcon, MaybeVNode, LooseVNodes, looseH as h } from 'common/snabbdom';
import { view as renderConfig } from './explorerConfig';
import { moveArrowAttributes, ucfirst } from './explorerUtil';
import AnalyseCtrl from '../ctrl';
import {
  isOpening,
  isTablebase,
  TablebaseCategory,
  OpeningData,
  OpeningMoveStats,
  OpeningGame,
  ExplorerDb,
} from './interfaces';
import ExplorerCtrl, { MAX_DEPTH } from './explorerCtrl';
import { showTablebase } from './tablebaseView';
import { FEN } from 'chessground/types';

function resultBar(move: OpeningMoveStats): VNode {
  const sum = move.white + move.draws + move.black;
  const section = (key: 'white' | 'black' | 'draws') => {
    const percent = (move[key] * 100) / sum;
    return h(
      'span.' + key,
      { attrs: { style: 'width: ' + Math.round((move[key] * 1000) / sum) / 10 + '%' } },
      percent > 12 ? Math.round(percent) + (percent > 20 ? '%' : '') : '',
    );
  };
  return h('div.bar', ['white', 'draws', 'black'].map(section));
}

function showMoveTable(ctrl: AnalyseCtrl, data: OpeningData): VNode | null {
  if (!data.moves.length) return null;
  const trans = ctrl.trans.noarg;
  const sumTotal = data.white + data.black + data.draws;
  const movesWithCurrent =
    data.moves.length > 1
      ? [
          ...data.moves,
          {
            white: data.white,
            black: data.black,
            draws: data.draws,
            uci: '',
            san: 'Σ',
          } as OpeningMoveStats,
        ]
      : data.moves;

  return h('table.moves', [
    h('thead', [
      h('tr', [
        h('th', trans('move')),
        h('th', { attrs: { colspan: 2 } }, trans('games')),
        h('th', trans('whiteDrawBlack')),
      ]),
    ]),
    h(
      'tbody',
      moveArrowAttributes(ctrl, { fen: data.fen, onClick: (_, uci) => uci && ctrl.explorerMove(uci) }),
      movesWithCurrent.map(move => {
        const total = move.white + move.draws + move.black;
        return h(`tr${move.uci ? '' : '.sum'}`, { key: move.uci, attrs: { 'data-uci': move.uci } }, [
          h('td', move.san[0] === 'P' ? move.san.slice(1) : move.san),
          h('td', ((total / sumTotal) * 100).toFixed(0) + '%'),
          h('td', numberFormat(total)),
          h('td', { attrs: { title: moveTooltip(ctrl, move) } }, resultBar(move)),
        ]);
      }),
    ),
  ]);
}

function moveTooltip(ctrl: AnalyseCtrl, move: OpeningMoveStats): string {
  if (!move.uci) return 'Total';
  if (move.game) {
    const g = move.game;
    const result = g.winner === 'white' ? '1-0' : g.winner === 'black' ? '0-1' : '½-½';
    return ctrl.explorer.opts.showRatings
      ? `${g.white.name} (${g.white.rating}) ${result} ${g.black.name} (${g.black.rating})`
      : `${g.white.name} ${result} ${g.black.name}`;
  }
  if (ctrl.explorer.opts.showRatings) {
    if (move.averageRating) return ctrl.trans('averageRatingX', move.averageRating);
    if (move.averageOpponentRating)
      return `Performance rating: ${move.performance}, average opponent: ${move.averageOpponentRating}`;
  }
  return '';
}

const showResult = (winner?: Color): VNode =>
  winner === 'white'
    ? h('result.white', '1-0')
    : winner === 'black'
    ? h('result.black', '0-1')
    : h('result.draws', '½-½');

function showGameTable(ctrl: AnalyseCtrl, fen: FEN, title: string, games: OpeningGame[]): VNode | null {
  if (!ctrl.explorer.withGames || !games.length) return null;
  const openedId = ctrl.explorer.gameMenu(),
    isMasters = ctrl.explorer.db() == 'masters';
  return h('table.games', [
    h('thead', [h('tr', [h('th.title', { attrs: { colspan: isMasters ? 4 : 5 } }, title)])]),
    h(
      'tbody',
      moveArrowAttributes(ctrl, {
        fen,
        onClick: (e, _) => {
          const $tr = $(e.target as HTMLElement).parents('tr');
          if (!$tr.length) return;
          const id = $tr.data('id');
          if (ctrl.study && ctrl.study.members.canContribute()) {
            ctrl.explorer.gameMenu(id);
            ctrl.redraw();
          } else openGame(ctrl, id);
        },
      }),
      games.map(game => {
        return openedId === game.id
          ? gameActions(ctrl, game)
          : h('tr', { key: game.id, attrs: { 'data-id': game.id, 'data-uci': game.uci || '' } }, [
              ctrl.explorer.opts.showRatings &&
                h(
                  'td',
                  [game.white, game.black].map(p => h('span', '' + p.rating)),
                ),
              h(
                'td',
                [game.white, game.black].map(p => h('span', p.name)),
              ),
              h('td', showResult(game.winner)),
              h('td', game.month || game.year),
              !isMasters &&
                h(
                  'td',
                  game.speed &&
                    h('i', { attrs: { title: ucfirst(game.speed), ...dataIcon(perf.icons[game.speed]) } }),
                ),
            ]);
      }),
    ),
  ]);
}

function openGame(ctrl: AnalyseCtrl, gameId: string) {
  const orientation = ctrl.chessground.state.orientation,
    fenParam = ctrl.node.ply > 0 ? '?fen=' + ctrl.node.fen : '';
  let url = '/' + gameId + '/' + orientation + fenParam;
  if (ctrl.explorer.db() === 'masters') url = '/import/master' + url;
  window.open(url, '_blank', 'noopener');
}

function gameActions(ctrl: AnalyseCtrl, game: OpeningGame): VNode {
  const send = (insert: boolean) => {
    ctrl.study!.explorerGame(game.id, insert);
    ctrl.explorer.gameMenu(null);
    ctrl.redraw();
  };
  return h('tr', { key: game.id + '-m' }, [
    h('td.game_menu', { attrs: { colspan: ctrl.explorer.db() == 'masters' ? 4 : 5 } }, [
      h(
        'div.game_title',
        `${game.white.name} - ${game.black.name}, ${showResult(game.winner).text}, ${game.year}`,
      ),
      h('div.menu', [
        h(
          'a.text',
          { attrs: dataIcon(licon.Eye), hook: bind('click', _ => openGame(ctrl, game.id)) },
          'View',
        ),
        ctrl.study &&
          h(
            'a.text',
            { attrs: dataIcon(licon.BubbleSpeech), hook: bind('click', _ => send(false), ctrl.redraw) },
            'Cite',
          ),
        ctrl.study &&
          h(
            'a.text',
            { attrs: dataIcon(licon.PlusButton), hook: bind('click', _ => send(true), ctrl.redraw) },
            'Insert',
          ),
        h(
          'a.text',
          { attrs: dataIcon(licon.X), hook: bind('click', _ => ctrl.explorer.gameMenu(null), ctrl.redraw) },
          'Close',
        ),
      ]),
    ]),
  ]);
}

const closeButton = (ctrl: AnalyseCtrl): VNode =>
  h(
    'button.button.button-empty.text',
    { attrs: dataIcon(licon.X), hook: bind('click', ctrl.toggleExplorer, ctrl.redraw) },
    ctrl.trans.noarg('close'),
  );

const showEmpty = (ctrl: AnalyseCtrl, data?: OpeningData): VNode =>
  h('div.data.empty', [
    explorerTitle(ctrl.explorer),
    openingTitle(ctrl, data),
    h('div.message', [
      h(
        'strong',
        ctrl.trans.noarg(ctrl.explorer.root.node.ply >= MAX_DEPTH ? 'maxDepthReached' : 'noGameFound'),
      ),
      data?.queuePosition
        ? h('p.explanation', `Indexing ${data.queuePosition} other players first ...`)
        : !ctrl.explorer.config.fullHouse() &&
          h('p.explanation', ctrl.trans.noarg('maybeIncludeMoreGamesFromThePreferencesMenu')),
    ]),
  ]);

const showGameEnd = (ctrl: AnalyseCtrl, title: string): VNode =>
  h('div.data.empty', [
    h('div.title', ctrl.trans.noarg('gameOver')),
    h('div.message', [
      h('i', { attrs: dataIcon(licon.InfoCircle) }),
      h('h3', ctrl.trans.noarg(title)),
      closeButton(ctrl),
    ]),
  ]);

const openingTitle = (ctrl: AnalyseCtrl, data?: OpeningData) => {
  const opening = data?.opening;
  const title = opening ? `${opening.eco} ${opening.name}` : '';
  return h(
    'div.title',
    { attrs: opening ? { title } : {} },
    opening
      ? [h('a', { attrs: { href: `/opening/${opening.name}`, target: '_blank' } }, title)]
      : [showTitle(ctrl, ctrl.data.game.variant)],
  );
};

let lastShow: MaybeVNode;
export const clearLastShow = () => {
  lastShow = undefined;
};

function show(ctrl: AnalyseCtrl): MaybeVNode {
  const trans = ctrl.trans.noarg,
    data = ctrl.explorer.current();
  if (data && isOpening(data)) {
    const moveTable = showMoveTable(ctrl, data),
      recentTable = showGameTable(ctrl, data.fen, trans('recentGames'), data.recentGames || []),
      topTable = showGameTable(ctrl, data.fen, trans('topGames'), data.topGames || []);
    if (moveTable || recentTable || topTable)
      lastShow = h('div.data', [
        explorerTitle(ctrl.explorer),
        data?.opening && openingTitle(ctrl, data),
        moveTable,
        topTable,
        recentTable,
      ]);
    else lastShow = showEmpty(ctrl, data);
  } else if (data && isTablebase(data)) {
    const row = (category: TablebaseCategory, title: string, tooltip?: string) =>
      showTablebase(
        ctrl,
        data.fen,
        trans(title),
        tooltip && trans(tooltip),
        data.moves.filter(m => m.category == category),
      );
    if (data.moves.length)
      lastShow = h('div.data', [
        ...row('loss', 'winning'),
        ...row('unknown', 'unknown'),
        ...row('maybe-loss', 'winOr50MovesByPriorMistake', 'unknownDueToRounding'),
        ...row('blessed-loss', 'winPreventedBy50MoveRule'),
        ...row('draw', 'drawn'),
        ...row('cursed-win', 'lossSavedBy50MoveRule'),
        ...row('maybe-win', 'lossOr50MovesByPriorMistake', 'unknownDueToRounding'),
        ...row('win', 'losing'),
      ]);
    else if (data.checkmate) lastShow = showGameEnd(ctrl, 'checkmate');
    else if (data.stalemate) lastShow = showGameEnd(ctrl, 'stalemate');
    else if (data.variant_win || data.variant_loss) lastShow = showGameEnd(ctrl, 'variantEnding');
    else lastShow = showEmpty(ctrl);
  }
  return lastShow;
}

const explorerTitle = (explorer: ExplorerCtrl) => {
  const db = explorer.db();
  const otherLink = (name: string, title: string) =>
    h(
      'button.button-link',
      {
        key: name,
        attrs: { title },
        hook: bind('click', () => explorer.config.data.db(name.toLowerCase() as ExplorerDb), explorer.reload),
      },
      name,
    );
  const playerLink = () =>
    h(
      'button.button-link.player',
      {
        key: 'player',
        hook: bind(
          'click',
          () => {
            explorer.config.selectPlayer(playerName || 'me');
            if (explorer.db() != 'player') {
              explorer.config.data.db('player');
              explorer.config.data.open(true);
            }
          },
          explorer.reload,
        ),
      },
      explorer.root.trans('player'),
    );
  const active = (nodes: LooseVNodes, title: string) =>
    h(
      'span.active.text.' + db,
      {
        attrs: { title, ...dataIcon(licon.Book) },
        hook: db == 'player' ? bind('click', explorer.config.toggleColor, explorer.reload) : undefined,
      },
      nodes,
    );
  const playerName = explorer.config.data.playerName.value();
  const masterDbExplanation = explorer.root.trans('masterDbExplanation', 2200, '1952', '2022-10'),
    lichessDbExplanation = explorer.root.trans('lichessDbExplanation');
  const data = explorer.current();
  const queuePosition = data && isOpening(data) && data.queuePosition;
  return h('div.explorer-title', [
    db == 'masters'
      ? active([h('strong', 'Masters'), ' database'], masterDbExplanation)
      : explorer.config.allDbs.includes('masters') && otherLink('Masters', masterDbExplanation),
    db == 'lichess'
      ? active([h('strong', 'Lichess'), ' database'], lichessDbExplanation)
      : otherLink('Lichess', lichessDbExplanation),
    db == 'player'
      ? playerName
        ? active(
            [
              h(`strong${playerName.length > 14 ? '.long' : ''}`, playerName),
              ' ' + explorer.root.trans(explorer.config.data.color() == 'white' ? 'asWhite' : 'asBlack'),
              explorer.isIndexing() &&
                !explorer.config.data.open() &&
                h('i.ddloader', {
                  attrs: {
                    title: queuePosition
                      ? `Indexing ${queuePosition} other players first ...`
                      : 'Indexing ...',
                  },
                }),
            ],
            explorer.root.trans('switchSides'),
          )
        : active([h('strong', 'Player'), ' database'], '')
      : playerLink(),
  ]);
};

function showTitle(ctrl: AnalyseCtrl, variant: Variant) {
  if (variant.key === 'standard' || variant.key === 'fromPosition')
    return ctrl.trans.noarg('openingExplorer');
  return ctrl.trans('xOpeningExplorer', variant.name);
}

function showConfig(ctrl: AnalyseCtrl): VNode {
  return h('div.config', [explorerTitle(ctrl.explorer), ...renderConfig(ctrl.explorer.config)]);
}

function showFailing(ctrl: AnalyseCtrl) {
  return h('div.data.empty', [
    h('div.title', showTitle(ctrl, ctrl.data.game.variant)),
    h('div.failing.message', [
      h('h3', 'Oops, sorry!'),
      h('p.explanation', ctrl.explorer.failing()?.toString()),
      closeButton(ctrl),
    ]),
  ]);
}

let lastFen: FEN = '';

export default function (ctrl: AnalyseCtrl): VNode | undefined {
  const explorer = ctrl.explorer;
  if (!explorer.enabled()) return;
  const data = explorer.current(),
    config = explorer.config,
    configOpened = config.data.open(),
    loading = !configOpened && (explorer.loading() || (!data && !explorer.failing())),
    content = configOpened ? showConfig(ctrl) : explorer.failing() ? showFailing(ctrl) : show(ctrl);
  return h(
    `section.explorer-box.sub-box${configOpened ? '.explorer__config' : ''}`,
    {
      class: { loading, reduced: !configOpened && (!!explorer.failing() || explorer.movesAway() > 2) },
      hook: {
        insert: vnode => ((vnode.elm as HTMLElement).scrollTop = 0),
        postpatch(_, vnode) {
          if (!data || lastFen === data.fen) return;
          (vnode.elm as HTMLElement).scrollTop = 0;
          lastFen = data.fen;
        },
      },
    },
    [
      h('div.overlay'),
      content,
      h('button.fbt.toconf', {
        attrs: {
          'aria-label': configOpened ? 'Close configuration' : 'Open configuration',
          ...dataIcon(configOpened ? licon.X : licon.Gear),
        },
        hook: bind('click', () => ctrl.explorer.config.toggleOpen(), ctrl.redraw),
      }),
    ],
  );
}
