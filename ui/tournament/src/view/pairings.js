var m = require('mithril');
var opposite = require('chessground').util.opposite;
var util = require('./util');

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

function featuredPlayer(f, orientation) {
  var p = f[orientation === 'top' ? opposite(f.color) : f.color];
  return m('div.vstext.' + orientation, [
    p.berserk ? m('i[data-icon=`][title=Berserk]') : null,
    m('strong', '#' + p.rank),
    util.player(p)
  ])
}

function featured(f) {
  if (f) return m('div.featured', [
    featuredPlayer(f, 'top'),
    util.miniBoard(f),
    featuredPlayer(f, 'bottom')
  ]);
}

function renderPairing(p) {
  return {
    tag: 'a',
    attrs: {
      key: p.id,
      href: '/' + p.id,
      class: 'glpt'
    },
    children: [
      user(p, 0),
      'vs',
      user(p, 1)
    ]
  };
}

module.exports = function(ctrl) {
  return [
    featured(ctrl.data.featured),
    m('div.box.all_pairings.scroll-shadow-soft', {
      onclick: function() {
        return !ctrl.vm.disableClicks;
      }
    }, ctrl.data.pairings.map(renderPairing))
  ];
};
