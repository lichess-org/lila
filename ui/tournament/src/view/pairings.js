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

function featured(f) {
  return m('div.featured', [
    m('div.vstext.clearfix', [
      m('div.left', [
        f.game.user1.name,
        m('br'),
        f.game.user1.title ? f.game.user1.title + ' ' : '',
        f.game.user1.rating
      ]),
      m('div.right', [
        f.game.user2.name,
        m('br'),
        f.game.user2.rating,
        f.game.user2.title ? ' ' + f.game.user2.title : ''
      ])
    ]),
    util.miniBoard(f.game)
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
