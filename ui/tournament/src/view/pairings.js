var m = require('mithril');
var partial = require('chessground').util.partial;
var util = require('./util');
var status = require('game').status;

function user(pairing, username) {
  var id = username.toLowerCase();
  return {
    tag: pairing.st >= status.ids.mate ? (
      pairing.wi ? (pairing.wi === id ? 'win' : 'loss') : 'draw'
    ) : 'playing',
    attrs: {},
    children: [username]
  };
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
        user(p, p.u1),
        'vs',
        user(p, p.u2)
      ]
    };
  };
  return ctrl.data.pairings.map(pairing);
};
