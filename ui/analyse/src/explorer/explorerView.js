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
    }, percent > 12 ? Math.round(percent) + '%' : null);
  }
  return m('div.bar', ['white', 'draws', 'black'].map(section));
}

var lastShow = null;

function $trUci($tr) {
  return $tr[0] ? $tr[0].getAttribute('data-uci') : null;
}

function showMoveTable(ctrl, moves) {
  if (!moves.length) return null;
  return m('table.moves', [
    m('thead', [
      m('tr', [
        m('th', 'Move'),
        m('th', 'Games'),
        m('th', 'White / Draw / Black')
      ])
    ]),
    m('tbody', {
      config: function(el, isUpdate, ctx) {
        if (!isUpdate || ctx.lastFen === ctrl.vm.step.fen) return;
        ctx.lastFen = ctrl.vm.step.fen;
        setTimeout(function() {
          ctrl.explorer.setHoveringUci($trUci($(el).find('tr:hover')));
        }, 100);
      },
      onclick: function(e) {
        var $tr = $(e.target).parents('tr');
        if ($tr.length) ctrl.explorerMove($trUci($tr));
      },
      onmouseover: function(e) {
        var $tr = $(e.target).parents('tr');
        ctrl.explorer.setHoveringUci($trUci($tr));
      },
      onmouseout: function(e) {
        ctrl.explorer.setHoveringUci(null);
      }
    }, moves.map(function(move) {
      return m('tr', {
        key: move.uci,
        'data-uci': move.uci
      }, [
        m('td', move.san),
        m('td', lichess.numberFormat(move.white + move.draws + move.black)),
        m('td', resultBar(move))
      ]);
    }))
  ]);
}

function showResult(winner) {
  if (winner === 'white') return '1-0';
  if (winner === 'black') return '0-1';
  return '½-½';
}

function showGameTable(ctrl, type, games) {
  if (!games.length) return null;
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
        var baseUrl = ctrl.explorer.config.data.db.selected() === 'lichess' ? '/' : '/import/master/';
        window.open(baseUrl + $tr.data('id'), '_blank');
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

function show(ctrl) {
  var data = ctrl.explorer.current();
  if (data) {
    var db = ctrl.explorer.config.data.db.selected();
    var moveTable = showMoveTable(ctrl, data.moves);
    var recentTable = showGameTable(ctrl, 'recent', data['recentGames'] || []);
    var topTable = showGameTable(ctrl, 'top', data['topGames'] || []);
    if (moveTable || recentTable || topTable) lastShow = m('div.data', [moveTable, recentTable, topTable]);
    else lastShow = m('div.data.empty', 'No game found');
  }
  return lastShow;
}

function showConfig(ctrl) {
  return m('div.config', [
    m('div.title', ctrl.data.game.variant.name + ' opening explorer'),
    renderConfig(ctrl.explorer.config, ctrl.data.game.variant)
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
    var config = ctrl.explorer.config;
    var configOpened = config.data.open();
    var loading = !configOpened && (ctrl.explorer.loading() || (!ctrl.explorer.current() && !ctrl.explorer.failing()));
    return m('div', {
      class: classSet({
        explorer_box: true,
        loading: loading,
        config: configOpened
      })
    }, [
      overlay,
      configOpened ? showConfig(ctrl) : (ctrl.explorer.failing() ? failing() : show(ctrl)),
      m('span.toconf', {
        'data-icon': configOpened ? 'L' : '%',
        onclick: config.toggleOpen
      })
    ]);
  }
};
