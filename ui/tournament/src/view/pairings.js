var m = require('mithril');
var partial = require('chessground').util.partial;
var util = require('./util');
var status = require('game').status;

var statusClasses = ['playing', 'draw', 'win', 'loss'];

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

module.exports = function(ctrl) {
  var pairing = function(p) {
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
  };
  return m('div.all_pairings.scroll-shadow-soft', ctrl.data.pairings.map(pairing));
};
