var m = require('mithril');
var partial = require('chessground').util.partial;
var opposite = require('chessground').util.opposite;
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

function featuredPlayer(f, orientation) {
  var p = f[orientation === 'top' ? opposite(f.color) : f.color];
  return m('div.vstext.' + orientation, [
    p.berserk ? m('i[data-icon=`][title=Berserk]') : null,
    m('strong', '#' + p.rank),
    util.player(p)
  ])
}

function featured(f) {
  return m('div.featured', [
    featuredPlayer(f, 'top'),
    util.miniBoard(f),
    featuredPlayer(f, 'bottom')
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
