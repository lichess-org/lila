import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { view as renderConfig } from './explorerConfig';
import { bind, dataIcon } from '../util';
import { AnalyseController } from '../interfaces';

function resultBar(move) {
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

function moveTableAttributes(ctrl, fen) {
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

function showMoveTable(ctrl, moves, fen) {
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

function showResult(winner) {
  if (winner === 'white') return h('result.white', '1-0');
  if (winner === 'black') return h('result.black', '0-1');
  return h('result.draws', '½-½');
}

function showGameTable(ctrl, type, games) {
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
        const fenParam = ctrl.vm.node.ply > 0 ? ('?fen=' + ctrl.vm.node.fen) : '';
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

function showTablebase(ctrl, title, moves, fen) {
  var stm = fen.split(/\s/)[1];
  if (!moves.length) return null;
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

function showWatkins(ctrl, moves, fen) {
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

function winner(stm, move) {
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

function showDtz(stm, move) {
  if (move.checkmate) return h('result.' + winner(stm, move), 'Checkmate');
  else if (move.stalemate) return h('result.draws', 'Stalemate');
  else if (move.variant_win) return h('result.' + winner(stm, move), 'Variant loss');
  else if (move.variant_loss) return h('result.' + winner(stm, move), 'Variant win');
  else if (move.insufficient_material) return h('result.draws', 'Insufficient material');
  else if (move.dtz === null) return null;
  else if (move.dtz === 0) return h('result.draws', 'Draw');
  else if (move.zeroing) {
    var capture = move.san.indexOf('x') !== -1;
    if (capture) return h('result.' + winner(stm, move), 'Capture');
    else return h('result.' + winner(stm, move), 'Pawn move');
  } else return h('result.' + winner(stm, move), {
    attrs: {
      title: 'Next capture or pawn move in ' + Math.abs(move.dtz) + ' half-moves (Distance To Zeroing of the 50 move counter)'
    }
  }, 'DTZ ' + Math.abs(move.dtz));
}

function closeButton(ctrl) {
  return h('button.button.text', {
    attrs: dataIcon('L'),
    hook: bind('click', ctrl.toggleExplorer)
  }, 'Close');
}

function showEmpty(ctrl) {
  return h('div.data.empty', [
    h('div.title', showTitle(ctrl)),
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

function showGameEnd(ctrl, title) {
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
    var moves = data.moves;
    if (moves.length) lastShow = h('div.data', [
      showTablebase(ctrl, 'Winning', moves.filter(function(move) {
        return move.wdl === -2;
      }), data.fen),
      showTablebase(ctrl, 'Unknown', moves.filter(function(move) {
        return move.wdl === null;
      }), data.fen),
      showTablebase(ctrl, 'Win prevented by 50-move rule', moves.filter(function(move) {
        return move.wdl === -1;
      }), data.fen),
      showTablebase(ctrl, 'Drawn', moves.filter(function(move) {
        return move.wdl === 0;
      }), data.fen),
      showTablebase(ctrl, 'Loss saved by 50-move rule', moves.filter(function(move) {
        return move.wdl === 1;
      }), data.fen),
      showTablebase(ctrl, 'Losing', moves.filter(function(move) {
        return move.wdl === 2;
      }), data.fen)
    ])
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

function showTitle(ctrl) {
  if (ctrl.data.game.variant.key === 'standard' || ctrl.data.game.variant.key === 'fromPosition') {
    return 'Opening explorer';
  } else {
    return ctrl.data.game.variant.name + ' opening explorer';
  }
}

function showConfig(ctrl) {
  return h('div.config', [
    h('div.title', showTitle(ctrl)),
    renderConfig(ctrl.explorer.config)
  ]);
}

function showFailing(ctrl) {
  return h('div.data.empty', [
    h('div.title', showTitle(ctrl)),
    h('div.failing.message', [
      h('h3', 'Oops, sorry!'),
      h('p.explanation', 'The explorer is temporarily out of service. Try again soon!'),
      closeButton(ctrl)
    ])
  ]);
}

export default function(ctrl: AnalyseController) {
  var explorer = ctrl.explorer;
  if (!explorer.enabled()) return;
  var data = explorer.current();
  var config = explorer.config;
  var configOpened = config.data.open();
  var loading = !configOpened && (explorer.loading() || (!data && !explorer.failing()));
  var content = configOpened ? showConfig(ctrl) : (explorer.failing() ? showFailing(ctrl) : show(ctrl));
  return h('div.explorer_box', {
    class: {
      loading: loading,
      config: configOpened,
      reduced: !configOpened && (explorer.failing() || explorer.movesAway() > 2)
    },
    hook: {
      postpatch: (_, vnode) => {
        (vnode.elm as HTMLElement).scrollTop = 0;
      }
    }
  }, [
    h('div.overlay'),
    content, (!content || explorer.failing()) ? null : h('span.toconf', {
      attrs: dataIcon(configOpened ? 'L' : '%'),
      hook: bind('click', config.toggleOpen, ctrl.redraw)
    })
  ]);
};
