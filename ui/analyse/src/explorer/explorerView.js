var m = require('mithril');
var partial = require('chessground').util.partial;
var renderConfig = require('./explorerConfig').view;

function resultBar(move) {
  var sum = move.white + move.draws + move.black;
  var section = function(key) {
    return move[key] === 0 ? null : m('span', {
      class: key,
      style: {
        width: (Math.round(move[key] * 1000 / sum) / 10) + '%'
      },
    }, Math.round(move[key] * 100 / sum) + '%');
  }
  return m('div.bar', ['white', 'draws', 'black'].map(section));
}

var lastShow = null;

function show(ctrl) {
  var data = ctrl.explorer.current();
  if (data) {
    lastShow = data.moves.length ? m('div.data', [
      m('table', [
        m('thead', [
          m('tr', [
            m('th', 'Move'),
            m('th', 'Games'),
            m('th', 'White / Draw / Black')
          ])
        ]),
        m('tbody', {
          onclick: function(e) {
            var $tr = $(e.target).parents('tr');
            if ($tr.length) ctrl.explorerMove($tr[0].getAttribute('data-uci'));
          }
        }, data.moves.map(function(move) {
          return m('tr', {
            key: move.uci,
            'data-uci': move.uci
          }, [
            m('td', move.san),
            m('td', move.total),
            m('td', resultBar(move))
          ]);
        }))
      ])
    ]) : m('div.data.empty', 'No game found');
  }
  return lastShow;
}

var overlay = m('div.overlay', m.trust(lichess.spinnerHtml));

module.exports = {
  renderExplorer: function(ctrl) {
    if (!ctrl.explorer.enabled()) return;
    var config = ctrl.explorer.config;
    var loading = !config.data.open() && (ctrl.explorer.loading() || !ctrl.explorer.current());
    return m('div', {
      class: 'explorer_box' + (loading ? ' loading' : '')
    }, [
      overlay,
      config.data.open() ? renderConfig(config) : show(ctrl),
      m('span.toconf', {
        'data-icon': config.data.open() ? 'L' : '%',
        onclick: config.toggleOpen
      })
    ]);
  }
};
