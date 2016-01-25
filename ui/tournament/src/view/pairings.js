var m = require('mithril');
var partial = require('chessground').util.partial;
var util = require('./util');
var status = require('game').status;

function user(p, it) {
  return {
    tag: p.s === 0 ? 'playing' : (
      p.s === 1 ? 'draw' : (
        (p.s === 2) === (it === 0) ? 'win' : 'loss'
      )
    ),
    children: [p.u[it]]
  };
}

function featured(f, n) {
  return m('div.featured', [
    m('div.vstext.top', [
      m('strong', '#' + f.player2.rank),
      util.player(f.player2)
    ]),
    util.miniBoard(f),
    m('div.vstext.bottom', [
      m('strong', '#' + f.player1.rank),
      util.player(f.player1)
    ])
  ]);
}

module.exports = function(ctrl) {
  var pairing = function(p) {
    return {
      tag: 'a',
      attrs: {
        key: p.id,
        href: '/' + p.id
      },
      children: [
        user(p, 0),
        'vs',
        user(p, 1)
      ]
    };
  };
  return [
    ctrl.data.featured ? featured(ctrl.data.featured) : null,
    m('div.box.all_pairings.scroll-shadow-soft', {
      onclick: function() {
        return !ctrl.vm.disableClicks;
      }
    }, ctrl.data.pairings.map(pairing))
  ];
};
