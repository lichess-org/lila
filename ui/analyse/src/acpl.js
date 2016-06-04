var m = require('mithril');
var getPlayer = require('game').game.getPlayer;

function renderPlayer(data, color) {
  var p = getPlayer(data, color);
  return m('a.user_link', {
    href: '/@/' + p.user.username
  }, p.user.username);
}

var advices = [
  ['inaccuracy', 'Inaccuracies', '?!'],
  ['mistake', 'Mistakes', '?'],
  ['blunder', 'Blunders', '??']
];

var cached = false;

module.exports = function(ctrl) {
  var d = ctrl.data;
  if (!d.analysis) return;

  var first = ctrl.vm.mainline[0].eval;
  if (first.cp || first.mate) {
    if (cached) return {
      subtree: 'retain'
    };
    else cached = true;
  }

  return m('div.advice_summary', {
    config: function(el, isUpdate) {
      if (!isUpdate)
        $(el).on('click', 'tr.symbol', function() {
          ctrl.jumpToGlyphSymbol($(this).data('color'), $(this).data('symbol'));
        });
    }
  }, ['white', 'black'].map(function(color) {
    return m('table', [
      m('thead', m('tr', [
        m('td', m('i.is.color-icon.' + color)),
        m('th', renderPlayer(d, color))
      ])),
      m('tbody', [
        advices.map(function(a) {
          var nb = d.analysis[color][a[0]];
          attrs = nb ? {
            class: 'symbol',
            'data-color': color,
            'data-symbol': a[2]
          } : {};
          return m('tr', attrs, [
            m('td', nb),
            m('th', a[1])
          ]);
        }),
        m('tr', [
          m('td', d.analysis[color].acpl),
          m('th', 'Average centipawn loss')
        ])
      ])
    ]);
  }));
};
