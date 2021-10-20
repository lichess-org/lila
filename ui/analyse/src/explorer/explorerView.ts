import { h, VNode } from 'snabbdom';
import { numberFormat } from 'common/number';
import { perf } from 'game/perf';
import { bind, dataIcon, MaybeVNode } from 'common/snabbdom';
import { defined } from 'common';
import { view as renderConfig } from './explorerConfig';
import { moveTableAttributes, ucfirst } from './explorerUtil';
import AnalyseCtrl from '../ctrl';
import { isOpening, isTablebase, TablebaseCategory, OpeningData, OpeningMoveStats, OpeningGame } from './interfaces';
import ExplorerCtrl from './explorerCtrl';
import { showTablebase } from './tablebaseView';

function resultBar(move: OpeningMoveStats): VNode {
  const sum = move.white + move.draws + move.black;
  function section(key: 'white' | 'black' | 'draws') {
    const percent = (move[key] * 100) / sum;
    return h(
      'span.' + key,
      {
        attrs: { style: 'width: ' + Math.round((move[key] * 1000) / sum) / 10 + '%' },
      },
      percent > 12 ? Math.round(percent) + (percent > 20 ? '%' : '') : ''
    );
  }
  return h('div.bar', ['white', 'draws', 'black'].map(section));
}

function showMoveTable(ctrl: AnalyseCtrl, data: OpeningData): VNode | null {
  if (!data.moves.length) return null;
  const trans = ctrl.trans.noarg;
  const movesWithCurrent =
    data.moves.length > 1 && [data.white, data.black, data.draws].every(defined)
      ? [
          ...data.moves,
          {
            ...data,
            uci: '',
            san: 'Σ',
          } as OpeningMoveStats,
        ]
      : data.moves;

  return h('table.moves', [
    h('thead', [
      h('tr', [h('th.title', trans('move')), h('th.title', trans('games')), h('th.title', trans('whiteDrawBlack'))]),
    ]),
    h(
      'tbody',
      moveTableAttributes(ctrl, data.fen),
      movesWithCurrent.map(move =>
        h(
          `tr.expl-uci-${move.uci}`,
          {
            attrs: {
              'data-uci': move.uci,
              title: move.uci ? (move.averageRating ? ctrl.trans('averageRatingX', move.averageRating) : '') : 'Total',
            },
          },
          [
            h('td', move.san[0] === 'P' ? move.san.slice(1) : move.san),
            h('td', numberFormat(move.white + move.draws + move.black)),
            h('td', resultBar(move)),
          ]
        )
      )
    ),
  ]);
}

function showResult(winner?: Color): VNode {
  if (winner === 'white') return h('result.white', '1-0');
  if (winner === 'black') return h('result.black', '0-1');
  return h('result.draws', '½-½');
}

function showGameTable(ctrl: AnalyseCtrl, title: string, games: OpeningGame[]): VNode | null {
  if (!ctrl.explorer.withGames || !games.length) return null;
  const openedId = ctrl.explorer.gameMenu();
  return h('table.games', [
    h('thead', [h('tr', [h('th.title', { attrs: { colspan: 5 } }, title)])]),
    h(
      'tbody',
      {
        hook: bind('click', e => {
          const $tr = $(e.target as HTMLElement).parents('tr');
          if (!$tr.length) return;
          const id = $tr.data('id');
          if (ctrl.study && ctrl.study.members.canContribute()) {
            ctrl.explorer.gameMenu(id);
            ctrl.redraw();
          } else openGame(ctrl, id);
        }),
      },
      games.map(game => {
        return openedId === game.id
          ? gameActions(ctrl, game)
          : h(
              'tr',
              {
                key: game.id,
                attrs: { 'data-id': game.id },
              },
              [
                h(
                  'td',
                  [game.white, game.black].map(p => h('span', '' + p.rating))
                ),
                h(
                  'td',
                  [game.white, game.black].map(p => h('span', p.name))
                ),
                h('td', showResult(game.winner)),
                h('td', game.year || game.month),
                h(
                  'td',
                  game.speed &&
                    h('i', {
                      attrs: {
                        title: ucfirst(game.speed),
                        ...dataIcon(perf.icons[game.speed]),
                      },
                    })
                ),
              ]
            );
      })
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
  function send(insert: boolean) {
    ctrl.study!.explorerGame(game.id, insert);
    ctrl.explorer.gameMenu(null);
    ctrl.redraw();
  }
  return h(
    'tr',
    {
      key: game.id + '-m',
    },
    [
      h(
        'td.game_menu',
        {
          attrs: { colspan: 4 },
        },
        [
          h('div.game_title', `${game.white.name} - ${game.black.name}, ${showResult(game.winner).text}, ${game.year}`),
          h('div.menu', [
            h(
              'a.text',
              {
                attrs: dataIcon(''),
                hook: bind('click', _ => openGame(ctrl, game.id)),
              },
              'View'
            ),
            ...(ctrl.study
              ? [
                  h(
                    'a.text',
                    {
                      attrs: dataIcon(''),
                      hook: bind('click', _ => send(false), ctrl.redraw),
                    },
                    'Cite'
                  ),
                  h(
                    'a.text',
                    {
                      attrs: dataIcon(''),
                      hook: bind('click', _ => send(true), ctrl.redraw),
                    },
                    'Insert'
                  ),
                ]
              : []),
            h(
              'a.text',
              {
                attrs: dataIcon(''),
                hook: bind('click', _ => ctrl.explorer.gameMenu(null), ctrl.redraw),
              },
              'Close'
            ),
          ]),
        ]
      ),
    ]
  );
}

function closeButton(ctrl: AnalyseCtrl): VNode {
  return h(
    'button.button.button-empty.text',
    {
      attrs: dataIcon(''),
      hook: bind('click', ctrl.toggleExplorer, ctrl.redraw),
    },
    ctrl.trans.noarg('close')
  );
}

function showEmpty(ctrl: AnalyseCtrl, data?: OpeningData): VNode {
  return h('div.data.empty', [
    explorerTitle(ctrl.explorer),
    openingTitle(ctrl, data),
    h('div.message', [
      h('strong', ctrl.trans.noarg('noGameFound')),
      ctrl.explorer.config.fullHouse()
        ? null
        : h('p.explanation', ctrl.trans.noarg('maybeIncludeMoreGamesFromThePreferencesMenu')),
      closeButton(ctrl),
    ]),
  ]);
}

function showGameEnd(ctrl: AnalyseCtrl, title: string): VNode {
  return h('div.data.empty', [
    h('div.title', ctrl.trans.noarg('gameOver')),
    h('div.message', [h('i', { attrs: dataIcon('') }), h('h3', title), closeButton(ctrl)]),
  ]);
}

const openingTitle = (ctrl: AnalyseCtrl, data?: OpeningData) => {
  const opening = data?.opening;
  return h(
    'div.title',
    {
      attrs: opening ? { title: opening && `${opening.eco} ${opening.name}` } : {},
    },
    opening ? [h('strong', opening.eco), ' ', opening.name] : [showTitle(ctrl, ctrl.data.game.variant)]
  );
};

let lastShow: MaybeVNode;

function show(ctrl: AnalyseCtrl): MaybeVNode {
  const trans = ctrl.trans.noarg,
    data = ctrl.explorer.current();
  if (data && isOpening(data)) {
    const moveTable = showMoveTable(ctrl, data),
      recentTable = showGameTable(ctrl, trans('recentGames'), data.recentGames || []),
      topTable = showGameTable(ctrl, trans('topGames'), data.topGames || []);
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
        title,
        tooltip,
        data.moves.filter(m => m.category == category)
      );
    if (data.moves.length)
      lastShow = h('div.data', [
        ...row('loss', trans('winning')),
        ...row('unknown', trans('unknown')),
        ...row('maybe-loss', trans('winOr50MovesByPriorMistake'), trans('unknownDueToRounding')),
        ...row('blessed-loss', trans('winPreventedBy50MoveRule')),
        ...row('draw', trans('drawn')),
        ...row('cursed-win', trans('lossSavedBy50MoveRule')),
        ...row('maybe-win', trans('lossOr50MovesByPriorMistake'), trans('unknownDueToRounding')),
        ...row('win', trans('losing')),
      ]);
    else if (data.checkmate) lastShow = showGameEnd(ctrl, trans('checkmate'));
    else if (data.stalemate) lastShow = showGameEnd(ctrl, trans('stalemate'));
    else if (data.variant_win || data.variant_loss) lastShow = showGameEnd(ctrl, trans('variantEnding'));
    else lastShow = showEmpty(ctrl);
  }
  return lastShow;
}

const explorerTitle = (explorer: ExplorerCtrl) => {
  const db = explorer.db();
  return h('div.title.explorer-title', [
    h(
      'span.text',
      { attrs: dataIcon('') },
      db == 'masters'
        ? ['Masters database']
        : db == 'lichess'
        ? ['Lichess database']
        : [h('strong', explorer.config.data.playerName.value()), ' as ', explorer.config.data.color()]
    ),
    db == 'player' && explorer.isIndexing()
      ? h('span.indexing', [
          'Indexing ',
          h('strong', explorer.config.data.playerName.value()),
          ' games',
          h('i.ddloader'),
        ])
      : undefined,
  ]);
};

function showTitle(ctrl: AnalyseCtrl, variant: Variant) {
  if (variant.key === 'standard' || variant.key === 'fromPosition') return ctrl.trans.noarg('openingExplorer');
  return ctrl.trans('xOpeningExplorer', variant.name);
}

function showConfig(ctrl: AnalyseCtrl): VNode {
  return h(
    'div.config',
    [h('div.title', showTitle(ctrl, ctrl.data.game.variant))].concat(renderConfig(ctrl.explorer.config))
  );
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

let lastFen: Fen = '';

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
      class: {
        loading,
        reduced: !configOpened && (!!explorer.failing() || explorer.movesAway() > 2),
      },
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
          ...dataIcon(configOpened ? '' : ''),
        },
        hook: bind('click', () => ctrl.explorer.config.toggleOpen(), ctrl.redraw),
      }),
    ]
  );
}
