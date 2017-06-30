import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { view as renderConfig } from './explorerConfig';
import { bind, dataIcon } from '../util';
import AnalyseController from '../ctrl';

function resultBar(move): VNode {
  const sum = move.white + move.draws + move.black;
  function section(key) {
    const percent = move[key] * 100 / sum;
    return percent === 0 ? null : h('span.' + key, {
      attrs: { style: 'width: ' + (Math.round(move[key] * 1000 / sum) / 10) + '%' },
    }, percent > 12 ? Math.round(percent) + (percent > 20 ? '%' : '') : '');
  }
  return h('div.bar', ['white', 'draws', 'black'].map(section));
}

let lastShow: VNode;

function moveTableAttributes(ctrl: AnalyseController, fen: Fen) {
  return {
    attrs: { 'data-fen': fen },
    hook: {
      insert: vnode => {
        const el = vnode.elm as HTMLElement;
        el.addEventListener('mouseover', e => {
          ctrl.explorer.setHovering($(el).attr('data-fen'), $(e.target).parents('tr').attr('data-uci'));
        });
        el.addEventListener('mouseout', _ => {
          ctrl.explorer.setHovering($(el).attr('data-fen'), null);
        });
        el.addEventListener('mousedown', e => {
          const uci = $(e.target).parents('tr').attr('data-uci');
          if (uci) ctrl.explorerMove(uci);
        });
      },
      postpatch: (_, vnode) => {
        setTimeout(() => {
          const el = vnode.elm as HTMLElement;
          ctrl.explorer.setHovering($(el).attr('data-fen'), $(el).find('tr:hover').attr('data-uci'));
        }, 100);
      }
    }
  };
}

function showMoveTable(ctrl: AnalyseController, moves, fen: Fen): VNode | null {
  if (!moves.length) return null;
  return h('table.moves', [
    h('thead', [
      h('tr', [
        h('th', 'Move'),
        h('th', 'Games'),
        h('th', 'White / Draw / Black')
      ])
    ]),
    h('tbody', moveTableAttributes(ctrl, fen), moves.map(function(move) {
      return h('tr', {
        key: move.uci,
        attrs: {
          'data-uci': move.uci,
          title: 'Average rating: ' + move.averageRating
        }
      }, [
        h('td', move.san[0] === 'P' ? move.san.slice(1) : move.san),
        h('td', window.lichess.numberFormat(move.white + move.draws + move.black)),
        h('td', resultBar(move))
      ]);
    }))
  ]);
}

function showResult(winner: Color): VNode {
  if (winner === 'white') return h('result.white', '1-0');
  if (winner === 'black') return h('result.black', '0-1');
  return h('result.draws', '½-½');
}

function showGameTable(ctrl: AnalyseController, type: string, games): VNode | null {
  if (!ctrl.explorer.withGames || !games.length) return null;
  return h('table.games', [
    h('thead', [
      h('tr', [
        h('th', { attrs: { colspan: 4 } }, type + ' games')
      ])
    ]),
    h('tbody', {
      hook: bind('click', e => {
        const $tr = $(e.target).parents('tr');
        if (!$tr.length) return;
        const orientation = ctrl.chessground.state.orientation;
        const fenParam = ctrl.node.ply > 0 ? ('?fen=' + ctrl.node.fen) : '';
        if (ctrl.explorer.config.data.db.selected() === 'lichess')
          window.open('/' + $tr.data('id') + '/' + orientation + fenParam, '_blank');
        else window.open('/import/master/' + $tr.data('id') + '/' + orientation + fenParam, '_blank');
      })
    }, games.map(function(game) {
      return h('tr', {
        key: game.id,
        attrs: { 'data-id': game.id }
      }, [
        h('td', [game.white, game.black].map(function(p) {
          return h('span', p.rating);
        })),
        h('td', [game.white, game.black].map(function(p) {
          return h('span', p.name);
        })),
        h('td', showResult(game.winner)),
        h('td', game.year)
      ]);
    }))
  ]);
}

function showTablebase(ctrl: AnalyseController, title: string, moves, fen: Fen): VNode[] {
  if (!moves.length) return [];
  const stm = fen.split(/\s/)[1];
  return [
    h('div.title', title),
    h('table.tablebase', [
      h('tbody', moveTableAttributes(ctrl, fen), moves.map(function(move) {
        return h('tr', {
          key: move.uci,
          attrs: { 'data-uci': move.uci }
        }, [
          h('td', move.san),
          h('td', [showDtz(stm, move), showDtm(stm, move)])
        ]);
      }))
    ])
  ];
}

function showWatkins(ctrl: AnalyseController, moves, fen: Fen): VNode {
  return h('div.data.watkins', [
    h('div.title', 'Watkins antichess solution'),
    h('table.tablebase', [
      h('tbody', moveTableAttributes(ctrl, fen), moves.map(function(move) {
        return h('tr', {
          key: move.uci,
          attrs: { 'data-uci': move.uci }
        }, [
          h('td', move.san),
          h('td', [
            h('result.white', {
              attrs: { title: 'Proof tree size' }
            }, move.nodes + ' nodes')
          ])
        ]);
      }))
    ])
  ]);
}

function winner(stm, move): Color | undefined {
  if ((stm[0] == 'w' && move.wdl < 0) || (stm[0] == 'b' && move.wdl > 0))
    return 'white';
  if ((stm[0] == 'b' && move.wdl < 0) || (stm[0] == 'w' && move.wdl > 0))
    return 'black';
}

function showDtm(stm, move) {
  if (move.dtm) return h('result.' + winner(stm, move), {
    attrs: {
      title: 'Mate in ' + Math.abs(move.dtm) + ' half-moves (Depth To Mate)'
    }
  }, 'DTM ' + Math.abs(move.dtm));
}

function showDtz(stm, move): VNode | null {
  if (move.checkmate) return h('result.' + winner(stm, move), 'Checkmate');
  else if (move.stalemate) return h('result.draws', 'Stalemate');
  else if (move.variant_win) return h('result.' + winner(stm, move), 'Variant loss');
  else if (move.variant_loss) return h('result.' + winner(stm, move), 'Variant win');
  else if (move.insufficient_material) return h('result.draws', 'Insufficient material');
  else if (move.dtz === null) return null;
  else if (move.dtz === 0) return h('result.draws', 'Draw');
  else if (move.zeroing) return move.san.indexOf('x') !== -1 ?
  h('result.' + winner(stm, move), 'Capture') :
  h('result.' + winner(stm, move), 'Pawn move');
  return h('result.' + winner(stm, move), {
    attrs: {
      title: 'Next capture or pawn move in ' + Math.abs(move.dtz) + ' half-moves (Distance To Zeroing of the 50 move counter)'
    }
  }, 'DTZ ' + Math.abs(move.dtz));
}

function closeButton(ctrl: AnalyseController): VNode {
  return h('button.button.text', {
    attrs: dataIcon('L'),
    hook: bind('click', ctrl.toggleExplorer)
  }, 'Close');
}

function showEmpty(ctrl: AnalyseController): VNode {
  return h('div.data.empty', [
    h('div.title', showTitle(ctrl.data.game.variant)),
    h('div.message', [
      h('h3', "No game found"),
      h('p.explanation',
        ctrl.explorer.config.fullHouse() ?
        "Already searching through all available games." :
        "Maybe include more games from the preferences menu?"),
      closeButton(ctrl)
    ])
  ]);
}

function showGameEnd(ctrl: AnalyseController, title: string): VNode {
  return h('div.data.empty', [
    h('div.title', "Game over"),
    h('div.message', [
      h('i', { attrs: dataIcon('') }),
      h('h3', title),
      closeButton(ctrl)
    ])
  ]);
}

function show(ctrl) {
  var data = ctrl.explorer.current();
  if (data && data.opening) {
    var moveTable = showMoveTable(ctrl, data.moves, data.fen);
    var recentTable = showGameTable(ctrl, 'recent', data['recentGames'] || []);
    var topTable = showGameTable(ctrl, 'top', data['topGames'] || []);
    if (moveTable || recentTable || topTable) lastShow = h('div.data', [moveTable, topTable, recentTable]);
    else lastShow = showEmpty(ctrl);
  } else if (data && data.tablebase) {
    const moves = data.moves;
    if (moves.length) lastShow = h('div.data', [
      ['Winning', m => m.wdl === -2],
      ['Unknown', m => m.wdl === null],
      ['Win prevented by 50-move rule', m => m.wdl === -1],
      ['Drawn', m => m.wdl === 0],
      ['Loss saved by 50-move rule', m => m.wdl === 1],
      ['Losing', m => m.wdl === 2],
    ].map(a => showTablebase(ctrl, a[0] as string, moves.filter(a[1]), data.fen))
      .reduce(function(a, b) { return a.concat(b); }, []))
    else if (data.checkmate) lastShow = showGameEnd(ctrl, 'Checkmate')
      else if (data.stalemate) lastShow = showGameEnd(ctrl, 'Stalemate')
        else if (data.variant_win || data.variant_loss) lastShow = showGameEnd(ctrl, 'Variant end');
      else lastShow = showEmpty(ctrl);
  } else if (data && data.watkins) {
    if (data.game_over) lastShow = showGameEnd(ctrl, 'Antichess win');
    else if (data.moves && data.moves.length) lastShow = showWatkins(ctrl, data.moves, data.fen);
    else lastShow = showEmpty(ctrl);
  }
  return lastShow;
}

function showTitle(variant: Variant) {
  if (variant.key === 'standard' || variant.key === 'fromPosition') return 'Opening explorer';
  return variant.name + ' opening explorer';
}

function showConfig(ctrl: AnalyseController): VNode {
  return h('div.config', [
    h('div.title', showTitle(ctrl.data.game.variant))
  ].concat(renderConfig(ctrl.explorer.config)));
}

function showFailing(ctrl) {
  return h('div.data.empty', [
    h('div.title', showTitle(ctrl.data.game.variant)),
    h('div.failing.message', [
      h('h3', 'Oops, sorry!'),
      h('p.explanation', 'The explorer is temporarily out of service. Try again soon!'),
      closeButton(ctrl)
    ])
  ]);
}

export default function(ctrl: AnalyseController): VNode | undefined {
  const explorer = ctrl.explorer;
  if (!explorer.enabled()) return;
  const data = explorer.current();
  const config = explorer.config;
  const configOpened = config.data.open();
  const loading = !configOpened && (explorer.loading() || (!data && !explorer.failing()));
  const content = configOpened ? showConfig(ctrl) : (explorer.failing() ? showFailing(ctrl) : show(ctrl));
  return h('div.explorer_box', {
    class: {
      loading: loading,
      config: configOpened,
      reduced: !configOpened && (explorer.failing() || explorer.movesAway() > 2)
    },
    hook: {
      postpatch: (_, vnode) => (vnode.elm as HTMLElement).scrollTop = 0
    }
  }, [
    h('div.overlay'),
    content, (!content || explorer.failing()) ? null : h('span.toconf', {
      attrs: dataIcon(configOpened ? 'L' : '%'),
      hook: bind('click', config.toggleOpen, ctrl.redraw)
    })
  ]);
};
