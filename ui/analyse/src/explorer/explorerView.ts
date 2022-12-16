import { bind, dataIcon } from 'common/snabbdom';
import { VNode, h } from 'snabbdom';
import AnalyseCtrl from '../ctrl';
import { view as renderConfig } from './explorerConfig';
import { winnerOf } from './explorerUtil';
import {
  Opening,
  OpeningData,
  OpeningGame,
  OpeningMoveStats,
  TablebaseMoveStats,
  isOpening,
  isTablebase,
} from './interfaces';

function resultBar(move: OpeningMoveStats): VNode {
  const sum = move.sente + move.draws + move.gote;
  function section(key: 'sente' | 'gote' | 'draws') {
    const percent = (move[key] * 100) / sum;
    return percent === 0
      ? null
      : h(
          'span.' + key,
          {
            attrs: {
              style: 'width: ' + Math.round((move[key] * 1000) / sum) / 10 + '%',
            },
          },
          percent > 12 ? Math.round(percent) + (percent > 20 ? '%' : '') : ''
        );
  }
  return h('div.bar', ['sente', 'draws', 'gote'].map(section));
}

let lastShow: VNode;

function moveTableAttributes(ctrl: AnalyseCtrl, sfen: Sfen) {
  return {
    attrs: { 'data-sfen': sfen },
    hook: {
      insert: vnode => {
        const el = vnode.elm as HTMLElement;
        el.addEventListener('mouseover', e => {
          ctrl.explorer.setHovering(
            $(el).attr('data-sfen'),
            $(e.target as HTMLElement)
              .parents('tr')
              .attr('data-usi')
          );
        });
        el.addEventListener('mouseout', _ => {
          ctrl.explorer.setHovering($(el).attr('data-sfen'), null);
        });
        el.addEventListener('mousedown', e => {
          const usi = $(e.target as HTMLElement)
            .parents('tr')
            .attr('data-usi');
          if (usi) ctrl.explorerMove(usi);
        });
      },
      postpatch: (_, vnode) => {
        setTimeout(() => {
          const el = vnode.elm as HTMLElement;
          ctrl.explorer.setHovering($(el).attr('data-sfen'), $(el).find('tr:hover').attr('data-usi'));
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
      moveTableAttributes(ctrl, data.sfen),
      data.moves.map(move => {
        return h(
          'tr',
          {
            key: move.usi,
            attrs: {
              'data-usi': move.usi,
              title: ctrl.trans('averageRatingX', move.averageRating),
            },
          },
          [
            h('td', move.usi),
            h('td', window.lishogi.numberFormat(move.sente + move.draws + move.gote)),
            h('td', resultBar(move)),
          ]
        );
      })
    ),
  ]);
}

function showResult(winner?: Color): VNode {
  if (winner === 'sente') return h('result.sente', '1-0');
  if (winner === 'gote') return h('result.gote', '0-1');
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
                  [game.sente, game.gote].map(p => h('span', '' + p.rating))
                ),
                h(
                  'td',
                  [game.sente, game.gote].map(p => h('span', p.name))
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
  const orientation = ctrl.shogiground.state.orientation,
    sfenParam = ctrl.node.ply > 0 ? '?sfen=' + ctrl.node.sfen : '';
  let url = '/' + gameId + '/' + orientation + sfenParam;
  if (ctrl.explorer.config.data.db.selected() === 'masters') url = '/import/master' + url;
  window.open(url, '_blank');
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
          h('div.game_title', `${game.sente.name} - ${game.gote.name}, ${showResult(game.winner).text}, ${game.year}`),
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

function showTablebase(ctrl: AnalyseCtrl, title: string, moves: TablebaseMoveStats[], sfen: Sfen): VNode[] {
  if (!moves.length) return [];
  return [
    h('div.title', title),
    h('table.tablebase', [
      h(
        'tbody',
        moveTableAttributes(ctrl, sfen),
        moves.map(move => {
          return h(
            'tr',
            {
              key: move.usi,
              attrs: { 'data-usi': move.usi },
            },
            [h('td', move.usi), h('td', [showDtz(ctrl, sfen, move), showDtm(ctrl, sfen, move)])]
          );
        })
      ),
    ]),
  ];
}

function showDtm(_ctrl: AnalyseCtrl, sfen: Sfen, move: TablebaseMoveStats) {
  if (move.dtm) return h('result.' + winnerOf(sfen, move), {}, 'DTM ' + Math.abs(move.dtm));
  return undefined;
}

function showDtz(ctrl: AnalyseCtrl, sfen: Sfen, move: TablebaseMoveStats): VNode | null {
  const trans = ctrl.trans.noarg;
  if (move.checkmate) return h('result.' + winnerOf(sfen, move), trans('checkmate'));
  else if (move.stalemate) return h('result.draws', trans('stalemate'));
  else if (move.variant_win) return h('result.' + winnerOf(sfen, move), trans('variantLoss'));
  else if (move.variant_loss) return h('result.' + winnerOf(sfen, move), trans('variantWin'));
  else if (move.insufficient_material) return h('result.draws', trans('insufficientMaterial'));
  else if (move.dtz === null) return null;
  else if (move.dtz === 0) return h('result.draws', trans('draw'));
  return h('result.' + winnerOf(sfen, move), {}, 'DTZ ' + Math.abs(move.dtz));
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
          attrs: opening ? { title: opening && `${opening.japanese} ${opening.english}` } : {},
        },
        opening ? [h('strong', opening.english), ' ', opening.english] : [showTitle(ctrl, ctrl.data.game.variant)]
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
                attrs: data.opening
                  ? {
                      title: data.opening && `${data.opening.japanese} ${data.opening.english}`,
                    }
                  : {},
              },
              [h('strong', data.opening.japanese), ' ', data.opening.english]
            )
          ),
        moveTable,
        topTable,
        recentTable,
      ]);
    else lastShow = showEmpty(ctrl, data && data.opening);
  } else if (data && isTablebase(data)) {
    const halfmoves = parseInt(data.sfen.split(' ')[4], 10) + 1;
    const zeroed = halfmoves === 1;
    const moves = data.moves;
    const dtz = m => (m.checkmate || m.variant_win || m.variant_loss ? 0 : m.dtz);
    if (moves.length)
      lastShow = h(
        'div.data',
        (
          [
            [trans('winning'), m => m.wdl === -2 && m.dtz !== null && (zeroed || dtz(m) - halfmoves > -100)],
            [trans('unknown'), m => m.wdl === null || m.dtz === null],
            [
              'Winning or 50 moves by prior mistake',
              m => m.wdl === -2 && m.dtz !== null && !zeroed && dtz(m) - halfmoves === -100,
            ],
            [
              'winPreventedBy50MoveRule',
              m => m.dtz !== null && (m.wdl === -1 || (m.wdl === -2 && !zeroed && dtz(m) - halfmoves < -100)),
            ],
            [trans('drawn'), m => m.wdl === 0],
            [
              'lossSavedBy50MoveRule',
              m => m.dtz !== null && (m.wdl === 1 || (m.wdl === 2 && !zeroed && dtz(m) + halfmoves > 100)),
            ],
            [
              'Losing or 50 moves by prior mistake',
              m => m.wdl === 2 && m.dtz !== null && !zeroed && dtz(m) + halfmoves === 100,
            ],
            [trans('losing'), m => m.wdl === 2 && m.dtz !== null && (zeroed || dtz(m) + halfmoves < 100)],
          ] as [string, (move: TablebaseMoveStats) => boolean][]
        )
          .map(a => showTablebase(ctrl, a[0] as string, moves.filter(a[1]), data.sfen))
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
  if (variant.key === 'standard') return ctrl.trans.noarg('openingExplorer');
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
      h('p.explanation', 'The explorer is temporarily out of service. Try again soon!'),
      closeButton(ctrl),
    ]),
  ]);
}

let lastSfen: Sfen = '';

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
          if (!data || lastSfen === data.sfen) return;
          (vnode.elm as HTMLElement).scrollTop = 0;
          lastSfen = data.sfen;
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
