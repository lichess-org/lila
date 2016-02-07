var m = require('mithril');

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
  var data = ctrl.explorer.current(ctrl);
  if (data) {
    lastShow = data.moves.length ? m('div.data',
      m('table', [
        m('thead', [
          m('tr', [
            m('th', 'Move'),
            m('th', 'Games'),
            m('th', 'White / Draw / Black')
          ])
        ]),
        m('tbody', data.moves.map(function(move) {
          return m('tr', {
            'data-uci': move.uci
          }, [
            m('td', move.san),
            m('td', move.total),
            m('td', resultBar(move))
          ]);
        }))
      ])
    ) : m('div.data.empty', 'No game found');
  }
  return lastShow;
}

var overlay = m('div.overlay', m.trust(lichess.spinnerHtml));

module.exports = {
  renderExplorer: function(ctrl) {
    if (!ctrl.explorer.enabled()) return;
    var loading = ctrl.explorer.loading() || !ctrl.explorer.current(ctrl);
    return m('div', {
      class: 'explorer_box' + (loading ? ' loading' : ''),
      onclick: function(e) {
        var $tr = $(e.target).parents('tr');
        if ($tr.length) ctrl.explorerMove($tr[0].getAttribute('data-uci'));
      }
    }, [
      overlay,
      show(ctrl)
    ]);
  }
};
