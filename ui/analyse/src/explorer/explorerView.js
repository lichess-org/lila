var m = require('mithril');

function resultBar(move) {
  var sum = move.white + move.draws + move.black;
  var section = function(key) {
    var percent = Math.round(move[key] * 100 / sum) + '%';
    return percent === '0%' ? null : m('span', {
      class: key,
      style: {
        width: percent
      },
    }, percent);
  }
  return m('div.bar', ['white', 'draws', 'black'].map(section));
}

var lastShow = null;

function show(ctrl) {
  var data = ctrl.explorer.current(ctrl);
  if (data) {
    var moves = Object.keys(data.moves).map(function(move) {
      return data.moves[move];
    }).filter(function(x) {
      return x.total > 0;
    }).sort(function(a, b) {
      return b.total - a.total;
    });
    lastShow = moves.length ? m('div.data',
      m('table', [
        // m('thead', [
        //   m('tr', [
        //     m('th', 'Move'),
        //     m('th', 'Games'),
        //     m('th', 'Result')
        //   ])
        // ]),
        m('tbody', moves.map(function(move) {
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
    return m('div', {
      class: 'explorer_box' + (ctrl.explorer.current(ctrl) ? '' : ' loading'),
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
