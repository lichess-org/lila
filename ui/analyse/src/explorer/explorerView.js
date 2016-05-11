var m = require('mithril');
var chessground = require('chessground');
var partial = chessground.util.partial;
var classSet = chessground.util.classSet;
var renderConfig = require('./explorerConfig').view;

function resultBar(move) {
  var sum = move.white + move.draws + move.black;
  var section = function(key) {
    var percent = move[key] * 100 / sum;
    return percent === 0 ? null : m('span', {
      class: key,
      style: {
        width: (Math.round(move[key] * 1000 / sum) / 10) + '%'
      },
    }, percent > 12 ? Math.round(percent) + (percent > 20 ? '%' : '') : null);
  }
  return m('div.bar', ['white', 'draws', 'black'].map(section));
}

var lastShow = null;

function $trUci($tr) {
  return $tr[0] ? $tr[0].getAttribute('data-uci') : null;
}

function moveTableAttributes(ctrl, fen) {
  return {
    config: function(el, isUpdate, ctx) {
      if (!isUpdate) {
        el.addEventListener('mouseover', function(e) {
          ctrl.explorer.setHoveringUci($trUci($(e.target).parents('tr')));
        });
        el.addEventListener('mouseout', function(e) {
          ctrl.explorer.setHoveringUci(null);
        });
        return;
      }
      if (ctx.lastFen === fen) return;
      ctx.lastFen = fen;
      setTimeout(function() {
        ctrl.explorer.setHoveringUci($trUci($(el).find('tr:hover')));
      }, 100);
    },
    onclick: function(e) {
      var $tr = $(e.target).parents('tr');
      if ($tr.length) ctrl.explorerMove($trUci($tr));
    }
  };
}

function showMoveTable(ctrl, moves, fen) {
  if (!moves.length) return null;
  return m('table.moves', [
    m('thead', [
      m('tr', [
        m('th', 'Move'),
        m('th', 'Games'),
        m('th', 'White / Draw / Black')
      ])
    ]),
    m('tbody', moveTableAttributes(ctrl, fen), moves.map(function(move) {
      return m('tr', {
        key: move.uci,
        'data-uci': move.uci,
        title: 'Average rating: ' + move.averageRating
      }, [
        m('td', move.san[0] === 'P' ? move.san.slice(1) : move.san),
        m('td', lichess.numberFormat(move.white + move.draws + move.black)),
        m('td', resultBar(move))
      ]);
    }))
  ]);
}

function showResult(winner) {
  if (winner === 'white') return m('result.white', '1-0');
  if (winner === 'black') return m('result.black', '0-1');
  return m('result.draws', '½-½');
}

function showGameTable(ctrl, type, games) {
  if (!ctrl.explorer.withGames || !games.length) return null;
  return m('table.games', [
    m('thead', [
      m('tr', [
        m('th[colspan=4]', type + ' games')
      ])
    ]),
    m('tbody', {
      onclick: function(e) {
        var $tr = $(e.target).parents('tr');
        if (!$tr.length) return;
        var orientation = ctrl.chessground.data.orientation;
        var fenParam = ctrl.vm.node.ply > 0 ? ('?fen=' + ctrl.vm.node.fen) : '';
        if (ctrl.explorer.config.data.db.selected() === 'lichess')
          window.open('/' + $tr.data('id') + '/' + orientation + fenParam, '_blank');
        else
          window.open('/import/master/' + $tr.data('id') + '/' + orientation + fenParam, '_blank');
      }
    }, games.map(function(game) {
      return m('tr', {
        key: game.id,
        'data-id': game.id
      }, [
        m('td', [game.white, game.black].map(function(p) {
          return m('span', p.rating);
        })),
        m('td', [game.white, game.black].map(function(p) {
          return m('span', p.name);
        })),
        m('td', showResult(game.winner)),
        m('td', game.year)
      ]);
    }))
  ]);
}

function showTablebase(ctrl, title, moves, fen) {
  var stm = fen.split(/\s/)[1];
  if (!moves.length) return null;
  return [
    m('div.title', title),
    m('table.tablebase', [
      m('tbody', moveTableAttributes(ctrl, fen), moves.map(function(move) {
        return m('tr', {
          key: move.uci,
          'data-uci': move.uci
        }, [
          m('td', move.san),
          m('td', [showDtz(stm, move), showDtm(stm, move)])
        ]);
      }))
    ])
  ];
}

function winner(stm, move) {
  if ((stm[0] == 'w' && move.wdl < 0) || (stm[0] == 'b' && move.wdl > 0))
    return 'white';
  else if ((stm[0] == 'b' && move.wdl < 0) || (stm[0] == 'w' && move.wdl > 0))
    return 'black';
}

function showDtm(stm, move) {
  if (move.dtm) return m('result.' + winner(stm, move), {
    title: 'Mate in ' + Math.abs(move.dtm) + ' half-moves (Depth To Mate)'
  }, 'DTM ' + Math.abs(move.dtm));
}

function showDtz(stm, move) {
  if (move.checkmate) return m('result.' + winner(stm, move), 'Checkmate');
  else if (move.stalemate) return m('result.draws', 'Stalemate');
  else if (move.insufficient_material) return m('result.draws', 'Insufficient material');
  else if (move.dtz === null) return null;
  else if (move.dtz === 0) return m('result.draws', 'Draw');
  else if (move.zeroing) {
    var capture = move.san.indexOf('x') !== -1;
    if (capture) return m('result.' + winner(stm, move), 'Capture');
    else return m('result.' + winner(stm, move), 'Pawn move');
  }
  else return m('result.' + winner(stm, move), {
    title: 'Next capture or pawn move in ' + Math.abs(move.dtz) + ' half-moves (Distance To Zeroing of the 50 move counter)'
  }, 'DTZ ' + Math.abs(move.dtz));
}

function showEmpty(ctrl) {
  return m('div.data.empty', [
    m('div.title', showTitle(ctrl)),
    m('div.message', [
      m('i[data-icon=]'),
      m('h3', "No game found"),
      m('p',
        ctrl.explorer.config.fullHouse() ?
        "Already searching through all available games." :
        "Maybe include more games from the preferences menu?"),
      m('br'),
      m('button.button.text[data-icon=L]', {
        onclick: ctrl.explorer.toggle
      }, 'Close')
    ])
  ]);
}

function showGameEnd(ctrl, title) {
  return m('div.data.empty', [
    m('div.title', "Game over"),
    m('div.message', [
      m('i[data-icon=]'),
      m('h3', title),
      m('button.button.text[data-icon=L]', {
        onclick: ctrl.explorer.toggle
      }, 'Close')
    ])
  ]);
}

function show(ctrl) {
  var data = ctrl.explorer.current();
  if (data && data.opening) {
    var db = ctrl.explorer.config.data.db.selected();
    var moveTable = showMoveTable(ctrl, data.moves, data.fen);
    var recentTable = showGameTable(ctrl, 'recent', data['recentGames'] || []);
    var topTable = showGameTable(ctrl, 'top', data['topGames'] || []);
    if (moveTable || recentTable || topTable) lastShow = m('div.data', [moveTable, topTable, recentTable]);
    else lastShow = showEmpty(ctrl);
  } else if (data && data.tablebase) {
    var moves = data.moves;
    if (moves.length) lastShow = m('div.data', [
      showTablebase(ctrl, 'Winning', moves.filter(function(move) { return move.real_wdl === -2; }), data.fen),
      showTablebase(ctrl, 'Win prevented by 50-move rule', moves.filter(function(move) { return move.real_wdl === -1; }), data.fen),
      showTablebase(ctrl, 'Drawn', moves.filter(function(move) { return move.real_wdl === 0; }), data.fen),
      showTablebase(ctrl, 'Loss saved by 50-move rule', moves.filter(function(move) { return move.real_wdl === 1; }), data.fen),
      showTablebase(ctrl, 'Losing', moves.filter(function(move) { return move.real_wdl === 2; }), data.fen)
    ])
    else if (data.checkmate) lastShow = showGameEnd(ctrl, 'Checkmate')
    else if (data.stalemate) lastShow = showGameEnd(ctrl, 'Stalemate')
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
  return m('div.config', [
    m('div.title', showTitle(ctrl)),
    renderConfig(ctrl.explorer.config)
  ]);
}


var overlay = m('div.overlay', m.trust(lichess.spinnerHtml));

function failing() {
  return m('div.failing.message', [
    m('i[data-icon=,]'),
    m('h3', 'Oops, sorry!'),
    m('p', 'The explorer is temporarily'),
    m('p', 'out of service. Try again soon!')
  ]);
}

module.exports = {
  renderExplorer: function(ctrl) {
    if (!ctrl.explorer.enabled()) return;
    var data = ctrl.explorer.current();
    var config = ctrl.explorer.config;
    var configOpened = config.data.open();
    var loading = !configOpened && (ctrl.explorer.loading() || (!data && !ctrl.explorer.failing()));
    var content = configOpened ? showConfig(ctrl) : (ctrl.explorer.failing() ? failing() : show(ctrl));
    return m('div', {
      class: classSet({
        explorer_box: true,
        loading: loading,
        config: configOpened
      }),
      config: function(el, isUpdate, ctx) {
        if (!isUpdate || !data || ctx.lastFen === data.fen) return;
        ctx.lastFen = data.fen;
        el.scrollTop = 0;
      }
    }, [
      overlay,
      content,
      (!content || ctrl.explorer.failing()) ? null : m('span.toconf', {
        'data-icon': configOpened ? 'L' : '%',
        onclick: config.toggleOpen
      })
    ]);
  }
};
