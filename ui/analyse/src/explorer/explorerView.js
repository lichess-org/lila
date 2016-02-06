var m = require('mithril');

function empty() {
  return m('div.empty', m.trust(lichess.spinnerHtml));
}

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

function show(data, ctrl) {
  var moves = Object.keys(data.moves).map(function(move) {
    return data.moves[move];
  }).filter(function(x) {
    return x.total > 0;
  }).sort(function(a, b) {
    return b.total - a.total;
  });
  return m('div.data',
    m('table', [
      // m('thead', [
      //   m('tr', [
      //     m('th', 'Move'),
      //     m('th', 'Games'),
      //     m('th', 'Result')
      //   ])
      // ]),
      m('tbody', {
        onclick: function(e) {
          ctrl.explorerMove($(e.target).parents('tr').data('uci'));
        }
      }, moves.map(function(move) {
        return m('tr', {
          'data-uci': move.uci
        }, [
          m('td', move.san),
          m('td', move.total),
          m('td', resultBar(move))
        ]);
      }))
    ])
  );
}

module.exports = {
  renderExplorer: function(ctrl) {
    if (!ctrl.explorer.enabled()) return;
    var data = ctrl.explorer.get(ctrl.vm.step.fen);
    return m('div.explorer_box', data ? show(data, ctrl) : empty());
  }
};
