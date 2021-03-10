import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import { numberFormat } from 'common/number';
import { view as renderConfig } from './explorerConfig';
import { bind, dataIcon } from '../util';
import { winnerOf } from './explorerUtil';
import AnalyseCtrl from '../ctrl';
import {
  isOpening,
  isTablebase,
  TablebaseMoveStats,
  OpeningData,
  OpeningMoveStats,
  OpeningGame,
  Opening,
} from './interfaces';

function resultBar(move: OpeningMoveStats): VNode {
  const sum = move.white + move.draws + move.black;
  function section(key: 'white' | 'black' | 'draws') {
    const percent = (move[key] * 100) / sum;
    return percent === 0
      ? null
      : h(
          'span.' + key,
          {
            attrs: { style: 'width: ' + Math.round((move[key] * 1000) / sum) / 10 + '%' },
          },
          percent > 12 ? Math.round(percent) + (percent > 20 ? '%' : '') : ''
        );
  }
  return h('div.bar', ['white', 'draws', 'black'].map(section));
}

let lastShow: VNode;

function moveTableAttributes(ctrl: AnalyseCtrl, fen: Fen) {
  return {
    attrs: { 'data-fen': fen },
    hook: {
      insert(vnode: VNode) {
        const el = vnode.elm as HTMLElement;
        el.addEventListener('mouseover', e => {
          ctrl.explorer.setHovering(
            $(el).attr('data-fen')!,
            $(e.target as HTMLElement)
              .parents('tr')
              .attr('data-uci')
          );
        });
        el.addEventListener('mouseout', _ => {
          ctrl.explorer.setHovering($(el).attr('data-fen')!, null);
        });
        el.addEventListener('mousedown', e => {
          const uci = $(e.target as HTMLElement)
            .parents('tr')
            .attr('data-uci');
          if (uci) ctrl.explorerMove(uci);
        });
      },
      postpatch(old: VNode) {
        setTimeout(() => {
          const el = old.elm as HTMLElement;
          ctrl.explorer.setHovering($(el).attr('data-fen')!, $(el).find('tr:hover').attr('data-uci'));
        }, 100);
      },
    },
  };
}

function showMoveTable(ctrl: AnalyseCtrl, data: OpeningData): VNode | null {
  if (!data.moves.length) return null;
  const trans = ctrl.trans.noarg;
  return h('table.moves', [
    h('thead', [
      h('tr', [h('th.title', trans('move')), h('th.title', trans('games')), h('th.title', trans('whiteDrawBlack'))]),
    ]),
    h(
      'tbody',
      moveTableAttributes(ctrl, data.fen),
      data.moves.map(move => {
        return h(
          'tr',
          {
            key: move.uci,
            attrs: {
              'data-uci': move.uci,
              title: ctrl.trans('averageRatingX', move.averageRating),
            },
          },
          [
            h('td', move.san[0] === 'P' ? move.san.slice(1) : move.san),
            h('td', numberFormat(move.white + move.draws + move.black)),
            h('td', resultBar(move)),
          ]
        );
      })
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
    h('thead', [h('tr', [h('th.title', { attrs: { colspan: 4 } }, title)])]),
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
                h('td', [game.year]),
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
  if (ctrl.explorer.config.data.db.selected() === 'masters') url = '/import/master' + url;
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
                attrs: dataIcon('v'),
                hook: bind('click', _ => openGame(ctrl, game.id)),
              },
              'View'
            ),
            ...(ctrl.study
              ? [
                  h(
                    'a.text',
                    {
                      attrs: dataIcon('c'),
                      hook: bind('click', _ => send(false), ctrl.redraw),
                    },
                    'Cite'
                  ),
                  h(
                    'a.text',
                    {
                      attrs: dataIcon('O'),
                      hook: bind('click', _ => send(true), ctrl.redraw),
                    },
                    'Insert'
                  ),
                ]
              : []),
            h(
              'a.text',
              {
                attrs: dataIcon('L'),
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

function showTablebase(ctrl: AnalyseCtrl, title: string, moves: TablebaseMoveStats[], fen: Fen): VNode[] {
  if (!moves.length) return [];
  return [
    h('div.title', title),
    h('table.tablebase', [
      h(
        'tbody',
        moveTableAttributes(ctrl, fen),
        moves.map(move => {
          return h(
            'tr',
            {
              key: move.uci,
              attrs: { 'data-uci': move.uci },
            },
            [h('td', move.san), h('td', [showDtz(ctrl, fen, move), showDtm(ctrl, fen, move)])]
          );
        })
      ),
    ]),
  ];
}

function showDtm(ctrl: AnalyseCtrl, fen: Fen, move: TablebaseMoveStats) {
  if (move.dtm)
    return h(
      'result.' + winnerOf(fen, move),
      {
        attrs: {
          title: ctrl.trans.plural('mateInXHalfMoves', Math.abs(move.dtm)) + ' (Depth To Mate)',
        },
      },
      'DTM ' + Math.abs(move.dtm)
    );
  return undefined;
}

function showDtz(ctrl: AnalyseCtrl, fen: Fen, move: TablebaseMoveStats): VNode | null {
  const trans = ctrl.trans.noarg;
  if (move.checkmate) return h('result.' + winnerOf(fen, move), trans('checkmate'));
  else if (move.stalemate) return h('result.draws', trans('stalemate'));
  else if (move.variant_win) return h('result.' + winnerOf(fen, move), trans('variantLoss'));
  else if (move.variant_loss) return h('result.' + winnerOf(fen, move), trans('variantWin'));
  else if (move.insufficient_material) return h('result.draws', trans('insufficientMaterial'));
  else if (move.dtz === null) return null;
  else if (move.dtz === 0) return h('result.draws', trans('draw'));
  else if (move.zeroing)
    return move.san.includes('x')
      ? h('result.' + winnerOf(fen, move), trans('capture'))
      : h('result.' + winnerOf(fen, move), trans('pawnMove'));
  return h(
    'result.' + winnerOf(fen, move),
    {
      attrs: {
        title: ctrl.trans.plural('nextCaptureOrPawnMoveInXHalfMoves', Math.abs(move.dtz)),
      },
    },
    'DTZ ' + Math.abs(move.dtz)
  );
}

function closeButton(ctrl: AnalyseCtrl): VNode {
  return h(
    'button.button.button-empty.text',
    {
      attrs: dataIcon('L'),
      hook: bind('click', ctrl.toggleExplorer, ctrl.redraw),
    },
    ctrl.trans.noarg('close')
  );
}

function showEmpty(ctrl: AnalyseCtrl, opening?: Opening): VNode {
  return h('div.data.empty', [
    h(
      'div.title',
      h(
        'span',
        {
          attrs: opening ? { title: opening && `${opening.eco} ${opening.name}` } : {},
        },
        opening ? [h('strong', opening.eco), ' ', opening.name] : [showTitle(ctrl, ctrl.data.game.variant)]
      )
    ),
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

function show(ctrl: AnalyseCtrl) {
  const trans = ctrl.trans.noarg,
    data = ctrl.explorer.current();
  if (data && isOpening(data)) {
    const moveTable = showMoveTable(ctrl, data),
      recentTable = showGameTable(ctrl, trans('recentGames'), data.recentGames || []),
      topTable = showGameTable(ctrl, trans('topGames'), data.topGames || []);
    if (moveTable || recentTable || topTable)
      lastShow = h('div.data', [
        data &&
          data.opening &&
          h(
            'div.title',
            h(
              'span',
              {
                attrs: data.opening ? { title: data.opening && `${data.opening.eco} ${data.opening.name}` } : {},
              },
              [h('strong', data.opening.eco), ' ', data.opening.name]
            )
          ),
        moveTable,
        topTable,
        recentTable,
      ]);
    else lastShow = showEmpty(ctrl, data && data.opening);
  } else if (data && isTablebase(data)) {
    const halfmoves = parseInt(data.fen.split(' ')[4], 10) + 1;
    const zeroed = halfmoves === 1;
    const moves = data.moves;
    const dtz = m => (m.checkmate || m.variant_win || m.variant_loss || m.zeroing ? 0 : m.dtz);
    if (moves.length)
      lastShow = h(
        'div.data',
        ([
          [trans('winning'), m => m.wdl === -2 && m.dtz !== null && (zeroed || dtz(m) - halfmoves > -100)],
          [trans('unknown'), m => m.wdl === null || m.dtz === null],
          [
            'Winning or 50 moves by prior mistake',
            m => m.wdl === -2 && m.dtz !== null && !zeroed && dtz(m) - halfmoves === -100,
          ],
          [
            trans('winPreventedBy50MoveRule'),
            m => m.dtz !== null && (m.wdl === -1 || (m.wdl === -2 && !zeroed && dtz(m) - halfmoves < -100)),
          ],
          [trans('drawn'), m => m.wdl === 0],
          [
            trans('lossSavedBy50MoveRule'),
            m => m.dtz !== null && (m.wdl === 1 || (m.wdl === 2 && !zeroed && dtz(m) + halfmoves > 100)),
          ],
          [
            'Losing or 50 moves by prior mistake',
            m => m.wdl === 2 && m.dtz !== null && !zeroed && dtz(m) + halfmoves === 100,
          ],
          [trans('losing'), m => m.wdl === 2 && m.dtz !== null && (zeroed || dtz(m) + halfmoves < 100)],
        ] as [string, (move: TablebaseMoveStats) => boolean][])
          .map(a => showTablebase(ctrl, a[0] as string, moves.filter(a[1]), data.fen))
          .reduce(function (a, b) {
            return a.concat(b);
          }, [])
      );
    else if (data.checkmate) lastShow = showGameEnd(ctrl, trans('checkmate'));
    else if (data.stalemate) lastShow = showGameEnd(ctrl, trans('stalemate'));
    else if (data.variant_win || data.variant_loss) lastShow = showGameEnd(ctrl, trans('variantEnding'));
    else lastShow = showEmpty(ctrl);
  }
  return lastShow;
}

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
      h('h3', 'The explorer will be back in a few days.'),
      h('p.explanation', [
        'Due to a ',
        h(
          'a',
          {
            attrs: {
              href:
                'https://france3-regions.francetvinfo.fr/grand-est/bas-rhin/strasbourg-0/strasbourg-gros-incendie-dans-un-data-center-du-port-du-rhin-pas-de-blesse-1990768.html',
            },
          },
          'fire in the SBG2 datacenter'
        ),
        ', power to the server (in SBG3) has been cut. We expect it to be restored before reindexing games on a new server would complete.',
      ]),
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
    'section.explorer-box.sub-box',
    {
      class: {
        loading,
        config: configOpened,
        reduced: !configOpened && (explorer.failing() || explorer.movesAway() > 2),
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
      !content || explorer.failing()
        ? null
        : h('span.toconf', {
            attrs: dataIcon(configOpened ? 'L' : '%'),
            hook: bind('click', () => ctrl.explorer.config.toggleOpen(), ctrl.redraw),
          }),
    ]
  );
}
